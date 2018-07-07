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

import com.google.common.base.Stopwatch;
import com.google.common.net.MediaType;
import eu.europa.ec.leos.model.content.*;
import eu.europa.ec.leos.model.content.LeosDocumentProperties.OwnerSystem;
import eu.europa.ec.leos.model.content.LeosDocumentProperties.Stage;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.Permission;
import eu.europa.ec.leos.repositories.content.ContentRepository;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties;
import eu.europa.ec.leos.repositories.support.cmis.StorageProperties.EditableProperty;
import eu.europa.ec.leos.services.exception.LeosDocumentLockException;
import eu.europa.ec.leos.services.exception.LeosPermissionDeniedException;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.services.user.PermissionService;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlMetaDataProcessor;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.UserVO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private PermissionService permissionService;

    @Autowired
    protected SecurityContext leosSecurityContext;

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
        return contentRepository.getUserLeosObjectProperties(userWsPath, leosSecurityContext.getUser());
    }

    @Override
    public String generateUserWorkspacePath() {
        // TODO working directory should be defined per user (workspacesPath/userLogin)
        return samplesPath;
    }

    /* ****************************************************************************************************************/
    @Override
    public LeosDocument getDocument(String leosId) {
        LOG.trace("Getting document... [leosId={}]", leosId);
        Validate.notNull(leosId, "The document id must not be null!");
        LeosDocument leosDocument=contentRepository.retrieveById(leosId, LeosDocument.class);
        checkPermission(leosDocument);
        return leosDocument;
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
        StorageProperties properties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        properties.set(EditableProperty.CHECKIN_COMMENT, checkinComment);
        
        return contentRepository.updateContent(leosId, (long) content.length, MediaType.XML_UTF_8,
                new ByteArrayInputStream(content), properties, LeosDocument.class);
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
        
        StorageProperties properties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        properties.set(EditableProperty.NAME, documentName);
        properties.set(EditableProperty.STAGE, Stage.DRAFT.getValue());
        properties.set(EditableProperty.SYSTEM, OwnerSystem.LEOS.getValue());
        properties.set(EditableProperty.TEMPLATE, templateId);
        properties.set(EditableProperty.CHECKIN_COMMENT, "operation.initial.version");
        properties.set(EditableProperty.AUTHOR_ID, leosSecurityContext.getUser().getLogin());
        properties.set(EditableProperty.AUTHOR_NAME, leosSecurityContext.getUser().getName());
        
        LeosDocument document = contentRepository.copy(templatePath, workspacePath, properties);
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
            String metaData = xmlContentProcessor.getElementByNameAndId(IOUtils.toByteArray(document.getContentStream()), "meta", null);
            return xmlMetaDataProcessor.fromXML(metaData);
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
            content = xmlContentProcessor.createDocumentContentWithNewTocList(tocList, IOUtils.toByteArray(document.getContentStream()));
            // TODO validate bytearray for being valid xml/AKN content

            content = xmlContentProcessor.renumberArticles(content, document.getLanguage());
            
            StorageProperties properties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
            properties.set(EditableProperty.CHECKIN_COMMENT, "operation.toc.updated");
            
            document = contentRepository.updateContent(
                        document.getLeosId(), 
                        (long) content.length, 
                        MediaType.XML_UTF_8,
                        new ByteArrayInputStream(content),
                        properties,
                        LeosDocument.class); 
        } catch (IOException e) {
            throw new RuntimeException("Unable to save the table of content");
        }
        return document;
    }

    private String generateDocumentName(MetaDataVO metadataVO) {
        StringBuffer sb = new StringBuffer();
        if (StringUtils.isNotBlank(metadataVO.getDocStage())) {
            sb.append(StringUtils.appendIfMissing(metadataVO.getDocStage(), StringUtils.SPACE));
        }
        if (StringUtils.isNotBlank(metadataVO.getDocType())) {
            sb.append(StringUtils.appendIfMissing(metadataVO.getDocType(), StringUtils.SPACE));
        }
        sb.append(metadataVO.getDocPurpose());
        return sb.toString();
    }

    private void checkDocumentLock(String leosId, String userLogin ) {
        if (!lockingService.isDocumentLockedFor(leosId, userLogin, null)) {//will not check for session equality
            throw new LeosDocumentLockException();
        }
    }

    private void checkPermission(LeosDocument leosDocument ) {
        if (!permissionService.getPermissions(leosSecurityContext.getUser(), leosDocument).contains(Permission.WRITE)) {
            //TODO rethink on check permission impl
            throw new LeosPermissionDeniedException("Permission denied for this document.Contact Owner " + leosDocument.getAuthor().getName());
        }
    }

    private LeosDocument updateMetaData(LeosDocument document, MetaDataVO metaDataVO) {
        try {
            byte[] updatedContent;
            byte[] documentContent = IOUtils.toByteArray(document.getContentStream());
            Stopwatch stopwatch=Stopwatch.createStarted();

            String existinMetaTag=xmlContentProcessor.getElementByNameAndId(documentContent, "meta", null);
            String metaFragment = xmlMetaDataProcessor.toXML(metaDataVO, existinMetaTag);
            LOG.trace("XML created for meta in :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

            try {
                updatedContent = xmlContentProcessor.replaceElementsWithTagName(documentContent, "meta", metaFragment);
            } catch (IllegalArgumentException e) {
                    updatedContent = xmlContentProcessor.appendElementToTag(documentContent, "bill", metaFragment);
            }
            LOG.trace("Meta Tag updated in XML  :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));

            Map<String, String> referencesValueMap =new HashMap<String, String>();
            referencesValueMap.put( XmlMetaDataProcessor.DEFAULT_DOC_PURPOSE_ID, metaDataVO.getDocPurpose());
            updatedContent = xmlContentProcessor.updateReferedAttributes(updatedContent, referencesValueMap);
            LOG.trace("Refered attributes injected in doc XML  :{} ms",stopwatch.elapsed(TimeUnit.MILLISECONDS));
            
            updatedContent=xmlContentProcessor.doXMLPostProcessing(updatedContent);
            LOG.trace("XML post processing done in :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            
            String updatedTitle = generateDocumentName(metaDataVO);
            StorageProperties properties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
            properties.set(EditableProperty.CHECKIN_COMMENT, "operation.metadata.updated");
            properties.set(EditableProperty.TITLE, updatedTitle);
            properties.set(EditableProperty.TEMPLATE, metaDataVO.getTemplate());
            properties.set(EditableProperty.LANGUAGE, metaDataVO.getLanguage());

            LeosDocument updatedDocument = contentRepository.updateContent(
                    document.getLeosId(), 
                    (long) updatedContent.length, 
                    MediaType.XML_UTF_8,
                    new ByteArrayInputStream(updatedContent), properties, LeosDocument.class); 
            
            LOG.trace("XML updated in repository :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            return updatedDocument;
        } catch (IOException e) {
            throw new RuntimeException("Unable to save the metadata");
        }
    }

    @Override
    public List<String> getAncestorsIdsForElementId(LeosDocument leosDocument,
            String elementId) {
        Validate.notNull(leosDocument, "Document is required");
        Validate.notNull(elementId, "Element id is required");

        try {
            return xmlContentProcessor.getAncestorsIdsForElementId(
                    IOUtils.toByteArray(leosDocument.getContentStream()),
                    elementId);
        } catch (IOException e) {
            String errorMsg = String.format(
                    "Unable to fetch encestors ids for element id: {}",
                    elementId);
            LOG.error(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    @Override
    public void setContributors (String leosId, List<UserVO> contributors) {
        LOG.trace("set contributors on ... [leosId={}]", leosId);
        Stopwatch stopwatch=Stopwatch.createStarted();
        contentRepository.updateProperties(leosId, getContributorStorageProperties(contributors));
        LOG.trace("update contributors from CMIS! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @Override
    public LeosDocument updateStage(String leosId, LeosDocumentProperties.Stage newStage){
        StorageProperties storageProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        storageProperties.set(EditableProperty.STAGE, newStage.getValue());
        storageProperties.set(EditableProperty.CHECKIN_COMMENT,"operation.stage.updated");
        return contentRepository.updateProperties(leosId, storageProperties);
    }

    private StorageProperties getContributorStorageProperties (List<UserVO> contributors) {
        StorageProperties storageProperties = new StorageProperties(LeosTypeId.LEOS_DOCUMENT);
        List<String> contributorIds = new ArrayList<>();
        List<String> contributorNames = new ArrayList<>();
        for (UserVO userVO : contributors) {
            if (userVO.getId() != null && userVO.getName() != null) {
                contributorIds.add(userVO.getId());
                contributorNames.add(userVO.getName());
            }
        }
        storageProperties.set(EditableProperty.CONTRIBUTOR_IDS,contributorIds);
        storageProperties.set(EditableProperty.CONTRIBUTOR_NAMES,contributorNames);

        storageProperties.set(EditableProperty.CHECKIN_COMMENT, "operation.contributor.updated");

        return storageProperties;
    }

    @Override
    public void deleteDocument(String leosId){
        Validate.notNull(leosId, "leosId is required");
        Stopwatch stopwatch=Stopwatch.createStarted();
        contentRepository.delete(leosId, LeosFile.class);
        LOG.trace("Document deleted in repository :{} ms", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
