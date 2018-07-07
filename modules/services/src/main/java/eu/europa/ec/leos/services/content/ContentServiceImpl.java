/**
 * Copyright 2015 European Commission
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.net.MediaType;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.repositories.content.ContentRepository;
import eu.europa.ec.leos.services.exception.LeosDocumentLockException;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlHelper;
import eu.europa.ec.leos.support.xml.XmlMetaDataProcessor;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;

@Service
public class ContentServiceImpl implements WorkspaceService, DocumentService  {

    private static final Logger LOG = LoggerFactory.getLogger(ContentServiceImpl.class);

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private LockingService lockingService;

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    @Autowired
    private XmlMetaDataProcessor xmlMetaDataProcessor;

    @Value("${leos.templates.path}")
    private String templatesPath;

    @Value("${leos.workspaces.path}")
    private String workspacesPath;

    @Value("${leos.samples.path}")
    private String samplesPath;
    /* ********************************WorkspaceService****************************************************************/
    @Override
    public List<LeosObjectProperties> browseUserWorkspace() {
        String userWsPath = generateUserWorkspacePath();
        LOG.trace("Browsing user workspace... [path={}]", userWsPath);
        return contentRepository.browse(userWsPath);
    }

    private String generateUserWorkspacePath() {
        // TODO working directory should be defined per user (workspacesPath/userLogin)
        return samplesPath;
    }

    /* ****************************************************************************************************************/
    @Override
    public LeosDocument getDocument(String leosId) {
        LOG.trace("Getting document... [leosId={}]", leosId);
        Validate.notNull(leosId, "The document id must not be null!");

        return contentRepository.retrieveById(leosId,LeosDocument.class);
    }
    
    @Override
    public LeosDocument getDocument(String leosId, String versionId) {
        LOG.trace("Getting document version... [leosId={}, verisonId={}]", leosId,versionId);
        Validate.notNull(versionId, "The versionId must not be null!");

        return contentRepository.retrieveVersion(versionId, LeosDocument.class);
    }
    
    public @Nonnull
    LeosDocument updateDocumentContent(@Nonnull String leosId, String userLogin, @Nonnull byte[] content, String checkinComment) {

        Validate.notNull(leosId, "The document id must not be null!");
        Validate.notNull(content, "The document content must not be null!");

        return contentRepository.updateContent(leosId, null, (long) content.length, MediaType.XML_UTF_8,
                new ByteArrayInputStream(content),checkinComment, LeosDocument.class);
    }

    public List<LeosDocumentProperties> getDocumentVersions(String leosId){
            Validate.notNull(leosId, "The document leosId must not be null!");
            return contentRepository.getVersions(leosId);
    }
    

    @Override
    public LeosDocument createDocumentFromTemplate(String templateId, MetaDataVO metaDataVO) {
        LOG.trace("Creating document from template... [templateId={}]", templateId);
        Validate.isTrue((templateId != null), "The template id must not be null!");
        Validate.isTrue((metaDataVO != null), "The document metadata must not be null!");
        String templatePath = templatesPath + "/" + templateId + ".xml";
        LOG.trace("Template path = {}", templatePath);
        String documentName = generateDocumentName(metaDataVO);
        LOG.trace("Document name = {}", documentName);
        String workspacePath = generateUserWorkspacePath();
        LOG.trace("Workspace path = {}", workspacePath);
        LeosDocument document = contentRepository.copy(templatePath, documentName, workspacePath,"operation.intial.version");
        document = updateMetaData(document, metaDataVO);
        return document;
    }

    @Override
    public LeosDocument updateMetaData(LeosDocument document, String userLogin, MetaDataVO metaDataVO) {
        Validate.notNull(document, "No document given to update meta data to.");
        Validate.notNull(metaDataVO, "No meta data given to update in the document.");

        checkDocumentLock(document.getLeosId(), userLogin);

        return updateMetaData(document, metaDataVO);
    }

    @Override
    public MetaDataVO getMetaData(LeosDocument document) {
        Validate.notNull(document, "Document is required");

        try {
            String metaData = xmlContentProcessor.getElementByNameAndId(IOUtils.toByteArray(document.getContentStream()), "proprietary", null);
            return xmlMetaDataProcessor.createMetaDataVOFromXml(metaData);
        } catch (IOException e) {
            throw new RuntimeException("Unable to fetch the metadata");
        }
    }

    @Override
    public List<TableOfContentItemVO> getTableOfContent(LeosDocument document) {
        Validate.notNull(document, "Document is required");

        try {
            return xmlContentProcessor.buildTableOfContent(IOUtils.toByteArray(document.getContentStream()));
        } catch (IOException e) {
            throw new RuntimeException("Unable to fetch the table of content");
        }
    }

    @Override
    public LeosDocument saveTableOfContent(LeosDocument document, String userLogin, List<TableOfContentItemVO> tocList) {
        Validate.notNull(document, "Document is required");
        Validate.notNull(tocList, "Table of content list is required");

        checkDocumentLock(document.getLeosId(), userLogin);

        byte[] content;
        try {
            content = xmlContentProcessor
                    .createDocumentContentWithNewTocList(tocList, IOUtils.toByteArray(document.getContentStream()));
            // TODO validate bytearray for being valid xml/AKN content

            content = xmlContentProcessor.renumberArticles(content, document.getLanguage());
            document = contentRepository.updateContent(
                        document.getLeosId(), 
                        null, 
                        (long) content.length, 
                        MediaType.XML_UTF_8,
                        new ByteArrayInputStream(content),"operation.toc.updated",LeosDocument.class); 
        } catch (IOException e) {
            throw new RuntimeException("Unable to save the table of content");
        }
        return document;
    }

    private String generateDocumentName(MetaDataVO metadataVO) {
        StringBuffer sb = new StringBuffer();
        sb.append(metadataVO.getDocType());
        sb.append(" ");
        sb.append(metadataVO.getDocPurpose());
        return sb.toString();
    }

    private void checkDocumentLock(String leosId, String userLogin ) {
        if (!lockingService.isDocumentLockedFor(leosId, userLogin, null)) {//will not check for session equality
            throw new LeosDocumentLockException();
        }
    }

    private LeosDocument updateMetaData(LeosDocument document, MetaDataVO metaDataVO) {
        try {
            byte[] updatedContent;
            byte[] documentContent = IOUtils.toByteArray(document.getContentStream());
            String proprietary = xmlMetaDataProcessor.createXmlForProprietary(metaDataVO);
            try {
                updatedContent = xmlContentProcessor.replaceElementWithTagName(documentContent, "proprietary", proprietary);
            } catch (IllegalArgumentException e) {
                try {
                    updatedContent = xmlContentProcessor.appendElementToTag(documentContent, "meta", proprietary);
                } catch (IllegalArgumentException e1) {
                    String metaTag = XmlHelper.buildTag("meta", proprietary);
                    updatedContent = xmlContentProcessor.appendElementToTag(documentContent, "bill", metaTag);
                }
            }

            // KLUGE hack to sync document content with metadata
            LOG.trace("Updating document title = {}", metaDataVO.getDocPurpose());
            String docTitleTag = "docPurpose";
            String docTitleXml = XmlHelper.buildTag(docTitleTag, metaDataVO.getDocPurpose());
            updatedContent = xmlContentProcessor.replaceElementWithTagName(updatedContent, docTitleTag, docTitleXml);//TODO replace all
            
            updatedContent=xmlContentProcessor.doXMLPostProcessing(updatedContent);
            
            LeosDocument updatedDocument = contentRepository.updateContent(
                    document.getLeosId(), 
                    null, 
                    (long) updatedContent.length, 
                    MediaType.XML_UTF_8,
                    new ByteArrayInputStream(updatedContent),"operation.metadata.updated",LeosDocument.class); 

            // KLUGE hack to sync document name with metadata
            String updatedName = generateDocumentName(metaDataVO);
            if (updatedDocument.getName().equals(updatedName)) {
                LOG.trace("Update document name is not required.");
            } else {
                LOG.trace("Updating document name = {}", updatedName);
                updatedDocument = contentRepository.rename(updatedDocument.getLeosId(), updatedName,"operation.document.renamed");
            }

            return updatedDocument;
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException("Unable to save the metadata");
        }
    }
}
