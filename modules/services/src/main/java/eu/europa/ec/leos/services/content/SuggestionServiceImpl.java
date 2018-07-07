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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Service
public class SuggestionServiceImpl implements SuggestionService {

    @Autowired
    private CommentService commentService;

    @Autowired
    private ElementService elementService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    @Autowired
    private SecurityContext leosSecurityContext;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    private static final Logger LOG = LoggerFactory.getLogger(SuggestionServiceImpl.class);

    @Override
    public LeosDocument saveSuggestion(LeosDocument document, @Nonnull String originalElementId, @Nonnull String suggestionId, String newSuggestedContent) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(originalElementId, "Original element id is required.");
        Validate.notNull(suggestionId, "Suggestion id is required.");
        byte[] updatedXmlContent;

        try {
            String suggestion = elementService.getElement(document, "popup", suggestionId);

            //if the commnent exist in xml.. update it
            //if it doesnt exist, insert it
            if (suggestion != null) {
                updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(
                        IOUtils.toByteArray(document.getContentStream()),
                        newSuggestedContent, "popup", suggestionId);
            } else {
                updatedXmlContent = xmlContentProcessor.insertCommentInElement(
                        IOUtils.toByteArray(document.getContentStream()),
                        originalElementId,
                        newSuggestedContent,
                        true);
            }
            // save document into repository
            document = documentService.updateDocumentContent(document.getLeosId(), leosSecurityContext.getUser().getLogin(), updatedXmlContent,
                    "operation.suggestion.updated");
        } catch (Exception e) {
            throw new RuntimeException("Unable to save the article.");
        }
        return document;
    }

    @Override
    public String getSuggestion(LeosDocument document, @Nonnull String originalElementId, @Nullable String suggestionId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(originalElementId, "Original element id is required.");

        String suggestion = null;
        if (suggestionId != null) {
            suggestion = elementService.getElement(document, "popup", suggestionId);
        }
        else{
            suggestion = constructNewSuggestion(document, originalElementId);
        }
        return suggestion;
    }

    private String constructNewSuggestion(LeosDocument document, @Nonnull String originalElementId) {
        String suggestion;

        try {
            byte[] documentContent = IOUtils.toByteArray(document.getContentStream());
            String existingTag = xmlContentProcessor.getElementById(documentContent, originalElementId);

            //remove all tags with suggestions
            String processedContent = new String(xmlContentProcessor.removeElements(existingTag.getBytes(Charset.forName("UTF-8")),
                    "//*[@refersTo='~leosSuggestion' or @refersto='~leosSuggestion']"), StandardCharsets.UTF_8);
          
            //remove all tags with comments
            processedContent = new String(xmlContentProcessor.removeElements(processedContent.getBytes(Charset.forName("UTF-8")),
                    "//*[@refersTo='~leosComment' or @refersto='~leosComment']"), StandardCharsets.UTF_8);
            
            processedContent = processedContent.replaceAll(" id(\\s*?)=(\\s*?)\"", " original-id=\"");
            suggestion = wrapContentInSuggestionTag(processedContent);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get the original text.", e);
        }
        return suggestion;
    }

    //creating XML representation of suggestion
    private String wrapContentInSuggestionTag(String content) {
        User leosUser = leosSecurityContext.getUser();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return String.format("<popup id=\"%1$s\" refersTo=\"~leosSuggestion\"" +
                        " leos:userid=\"%2$s\"" +
                        " leos:username=\"%3$s\"" +
                        " leos:dg=\"%4$s\"" +
                        " leos:datetime=\"%5$s\">" +
                        "%6$s" +
                        "</popup>",
                IdGenerator.generateId("sug", 10),//1
                leosUser.getLogin(),//2
                leosUser.getName(),//3
                (leosUser.getDepartment().getDepartmentId() == null) ? "" : leosUser.getDepartment().getDepartmentId(),//4
                sdf.format(new Date()),//5
                content//6
        );
    }
}
