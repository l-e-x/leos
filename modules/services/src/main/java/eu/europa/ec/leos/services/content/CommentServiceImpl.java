/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.content;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.support.xml.IdGenerator;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.vo.CommentVO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    @Autowired
    private SecurityContext leosSecurityContext;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    private static final Logger LOG = LoggerFactory.getLogger(CommentServiceImpl.class);

    @Override
    public List<CommentVO> getAllComments(LeosDocument document) {
        Validate.notNull(document, "Document is required.");
        List<CommentVO> comments;
        try {
            comments= xmlContentProcessor.getAllComments(IOUtils.toByteArray(document.getContentStream()));
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve comments.");
        }
        
        return comments ;
    }

    @Override
    public LeosDocument updateComment(LeosDocument document, String commentId, String newContent) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(commentId, "Comment id is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent;
        try {
            String commentXml = (newContent!=null)
                                    ?getCommentTag(commentId, newContent)
                                    :null;
            updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    commentXml, "popup", commentId);

            // save document into repository
            document = documentService.updateDocumentContent(document.getLeosId(), leosSecurityContext.getUser().getLogin(), updatedXmlContent, "operation.comment.updated");
        } catch (IOException e) {
            throw new RuntimeException("Unable to save the article.");
        }
        return document;
    }

    @Override
    public LeosDocument deleteComment(LeosDocument document, String commentId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(commentId, "commentId id is required.");

        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.deleteElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()), "popup", commentId);
            document = documentService.updateDocumentContent(document.getLeosId(), leosSecurityContext.getUser().getLogin(), updatedXmlContent,"operation.comment.deleted");

        } catch (IOException e) {
            throw new RuntimeException("Unable to delete the comment.",e);
        }
        return document;
    }
    
    @Override
    public LeosDocument insertNewComment(LeosDocument document, String elementId, String commentId, String commentContent, boolean start) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element Id is required.");
        Validate.notNull(commentId, "Comment Id is required.");

        byte[] updatedXmlContent;
        try {
            String commentXml = getCommentTag(commentId, commentContent);
            updatedXmlContent = xmlContentProcessor.insertCommentInElement(
                    IOUtils.toByteArray(document.getContentStream()),
                    elementId,
                    commentXml,
                    start);

        } catch (Exception e) {
            throw new RuntimeException("Unable to insert the comment.",e);
        }

        return documentService.updateDocumentContent(document.getLeosId(), leosSecurityContext.getUser().getLogin() , updatedXmlContent,"operation.comment.inserted");

    }
    
    //creating XML representation of comment
    //note: doing it here as it would be faster and simpler than groovy
    public String getCommentTag(String commentId, String noteContent) {
        User leosUser= leosSecurityContext.getUser();
        
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        return String.format("<popup id=\"%1$s\" refersTo=\"~leosComment\"" +
                " leos:userid=\"%2$s\"" +
                " leos:username=\"%3$s\"" +
                " leos:dg=\"%8$s\"" +
                " leos:datetime=\"%4$s\">" +
                "<p id=\"%5$s_%6$s\">" +
                "%7$s" +
                "</p></popup>",
                commentId,//1
                leosUser.getLogin(),//2
                leosUser.getName(),//3
                sdf.format(new Date()),//4
                commentId,//5
                IdGenerator.generateId(3),//6
                noteContent,//7
                (leosUser.getDepartment().getDepartmentId() == null) ? "" : leosUser.getDepartment().getDepartmentId()
        );
        
    }
    @Override
    public byte[] aggregateComments(byte[] baseXml, byte[] addendXml) throws Exception {

        List<CommentVO> existingComments = xmlContentProcessor.getAllComments(baseXml);
        List<CommentVO> newComments = xmlContentProcessor.getAllComments(addendXml);

        Comparator<CommentVO> commentIdComparator = new Comparator<CommentVO>() {
            @Override
            public int compare(CommentVO o1, CommentVO o2) {
                return o1.getId().compareTo(o2.getId());
            }
        };
        //sorting required for binary search in lists
        Collections.sort(newComments, commentIdComparator);
        Collections.sort(existingComments, commentIdComparator);

        LOG.debug("Comments in BaseXml:{},in addendedXML:{}", existingComments.size(), newComments.size());

        byte[] aggregateXmlBytes = baseXml;
        for (CommentVO comment : newComments) {
            //check corresponding element in existingComments
            //case 1. found
            //  Content equal -- Do nothing . move to next iteration.
            //  Content not equal --update the comment
            //case 2..not found
            //  Add comment in baseXml
            //case 3..delete the comments in baseXml..not handled as of now

            int index = Collections.binarySearch(existingComments, comment, commentIdComparator);
            if (index >= 0) {//found
                if (!(comment.getEnclosingElementId().equals(existingComments.get(index).getEnclosingElementId())
                        && comment.getComment().equals(existingComments.get(index).getComment()))) {
                    //found but not equal ..so update
                    String commentXml = xmlContentProcessor.getElementByNameAndId(addendXml, "popup", comment.getId());
                    aggregateXmlBytes = xmlContentProcessor.deleteElementByTagNameAndId(aggregateXmlBytes, "popup", comment.getId());
                    aggregateXmlBytes = xmlContentProcessor.insertCommentInElement(aggregateXmlBytes, comment.getEnclosingElementId(), commentXml, true);
                }
            }//end found if block
            else { //insert if not found
                String commentXml = xmlContentProcessor.getElementByNameAndId(addendXml, "popup", comment.getId());
                aggregateXmlBytes = xmlContentProcessor.insertCommentInElement(aggregateXmlBytes, comment.getEnclosingElementId(), commentXml, true);
            }//end not found else

                /*---Delete case to be handled --*/
        }//end for

        return aggregateXmlBytes;
    }
}
