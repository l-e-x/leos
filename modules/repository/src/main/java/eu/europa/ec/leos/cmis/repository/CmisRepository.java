/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.cmis.repository;

import eu.europa.ec.leos.cmis.search.SearchStrategy;
import eu.europa.ec.leos.cmis.search.SearchStrategyProvider;
import eu.europa.ec.leos.cmis.support.OperationContextProvider;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.util.*;

import static eu.europa.ec.leos.cmis.support.OperationContextProvider.getMinimalContext;

@Repository
public class CmisRepository {

    private static final Logger logger = LoggerFactory.getLogger(CmisRepository.class);

    private final Session cmisSession;

    CmisRepository(Session cmisSession) {
        this.cmisSession = cmisSession;
    }

    private SearchStrategy getSearchStrategy() {
        return SearchStrategyProvider.getSearchStrategy(cmisSession);
    }

    Folder createFolder(final String path, final String name) {
        logger.trace("Creating folder... [path=" + path + ", name=" + name + "]");
        OperationContext context = getMinimalContext(cmisSession);
        Folder parentFolder = findFolderByPath(path, context);

        Map<String, String> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, BaseTypeId.CMIS_FOLDER.value());
        properties.put(PropertyIds.NAME, name);

        return parentFolder.createFolder(properties, null, null, null, context);
    }

    void deleteFolder(final String path) {
        logger.trace("Deleting folder... [path=" + path + "]");
        OperationContext context = getMinimalContext(cmisSession);
        Folder folder = findFolderByPath(path, context);
        folder.deleteTree(true, UnfileObject.DELETE, true);
    }

    Document createDocumentFromContent(final String path, final String name, Map<String, ?> properties, final String mimeType, byte[] contentBytes, VersioningState versioningState) {
        logger.trace("Creating document... [path=" + path + ", name=" + name + ", mimeType=" + mimeType + "]");
        OperationContext context = getMinimalContext(cmisSession);

        Folder targetFolder = findFolderByPath(path, context);
        ByteArrayInputStream byteStream = new ByteArrayInputStream(contentBytes);
        ContentStream contentStream = cmisSession.getObjectFactory().createContentStream(name, (long) contentBytes.length, mimeType, byteStream);
        return targetFolder.createDocument(properties, contentStream, versioningState);
    }

    Document createDocumentFromSource(final String sourceId, String path, Map<String, ?> properties) {
        logger.trace("Creating document from source... [sourceId=" + sourceId + "]");
        OperationContext context = getMinimalContext(cmisSession);

        Folder targetFolder = findFolderByPath(path, context);
        Document sourceDoc = findDocumentById(sourceId, false, context);

        return sourceDoc.copy(targetFolder, properties, VersioningState.MAJOR, null, null, null, context);
    }

    void deleteDocumentById(final String id) {
        logger.trace("Deleting document... [id=" + id + "]");
        OperationContext context = getMinimalContext(cmisSession);
        CmisObject cmisObject = cmisSession.getObject(id, context);
        require(cmisObject instanceof Document, "CMIS object referenced by id [" + id + "] is not a Document!");
        cmisObject.delete(true);
    }

    Document updateDocument(final String id, Map<String, ?> properties) {
        logger.trace("Updating document properties... [id=" + id + "]");
        OperationContext context = getMinimalContext(cmisSession);
        Document document = findDocumentById(id, true, context);
        return (Document) document.updateProperties(properties);
    }

    Document updateDocument(String id, Map<String, ?> properties, byte[] updatedDocumentBytes, boolean major, String comment) {
        logger.trace("Updating document properties and content... [id=" + id + ']');
        Document pwc = checkOutWorkingCopy(id);
        Document udpatedDocument = checkInWorkingCopy(pwc, properties, updatedDocumentBytes, major, comment);
        logger.trace("Updated document properties and content...");
        if (udpatedDocument == null) {
            throw new IllegalStateException("Update not successful for document:" + id);
        } else {
            return udpatedDocument;
        }
    }

    private Document checkInWorkingCopy(Document pwc, Map<String, ?> properties, byte[] updatedDocumentBytes, boolean major, String comment) {
        Map<String, Object> updatedProperties = new LinkedHashMap<>();
        // KLUGE LEOS-2408 workaround for issue related to reset properties values with OpenCMIS In-Memory server
        logger.trace("KLUGE LEOS-2408 workaround for reset properties values...");

        pwc.getProperties().forEach(property -> {
            if (Updatability.READWRITE == property.getDefinition().getUpdatability()) {
                updatedProperties.put(property.getId(), property.getValue());
            }
        });

        // add input properties to existing properties map,
        // eventually overriding old properties values
        updatedProperties.putAll(properties);

        OperationContext context = getMinimalContext(cmisSession);
        ObjectId updatedDocId;

        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(updatedDocumentBytes)) {

            try {
                ContentStream pwcContentStream = pwc.getContentStream();

                ContentStream contentStream = cmisSession.getObjectFactory().createContentStream(pwcContentStream.getFileName(),
                        updatedDocumentBytes.length, pwcContentStream.getMimeType(), byteStream);
                updatedDocId = pwc.checkIn(major, updatedProperties, contentStream, comment);
                logger.trace("Document checked-in successfully...[updated document id:" + updatedDocId.getId() + ']');
            } catch (CmisBaseException e) {
                logger.error("Document update failed, trying to cancel the checkout", e);
                pwc.cancelCheckOut();
                throw e;
            }

            // If major version, remove previous version
            // FIXME: Couldn't update major version flag and comment in CMIS without checking out and checking in
            if (pwc.getAllVersions().size() > 1) {
                Document beforeLastVersion = pwc.getAllVersions().get(1);
                if (major && !beforeLastVersion.isMajorVersion() && beforeLastVersion.getLastModifiedBy().equals(pwc.getLastModifiedBy())) {
                    logger.trace("Major version - Removing the before last version...");
                    beforeLastVersion.delete(false);

                }
            }

            return findDocumentById(updatedDocId.getId(), true, context);
        } catch (Throwable e) {
            throw new IllegalStateException("unexpected exception", e);
        }
    }

    private Document checkOutWorkingCopy(String id) {
        OperationContext context = getMinimalContext(cmisSession);
        Document document = findDocumentById(id, true, context);

        String pwcId;
        if (document.isVersionSeriesCheckedOut()) {
            pwcId = document.getVersionSeriesCheckedOutId();
            logger.trace("Document already check out ... [id=" + id + ", pwc id=" + document.getVersionSeriesCheckedOutId());
        } else {
            pwcId = document.checkOut().getId();
        }

        Document pwc = findDocumentById(pwcId, false, context);
        logger.trace("Document checked-out... [id=" + id + ", pwc id=" + pwc.getId() + ']');
        return pwc;
    }

    // FIXME replace primaryType string with some enum value
    List<Document> findDocumentsByParentPath(final String path, final String primaryType, final Set<LeosCategory> categories, final boolean descendants) {
        logger.trace("Finding documents by parent path... [path=" + path + ", primaryType=" + primaryType + ", categories=" + categories + ", descendants=" + descendants + ']');
        OperationContext context = OperationContextProvider.getOperationContext(cmisSession, "cmis:lastModificationDate DESC");
        Folder folder = findFolderByPath(path, context);
        List<Document> documents = getSearchStrategy().findDocuments(folder, primaryType, categories, descendants, false, context);
        logger.trace("Found " + documents.size() + " CMIS document(s).");
        return documents;
    }

    List<Document> findDocumentsByPackageId(final String id, final String primaryType, final Set<LeosCategory> categories, final boolean allVersion) {
        logger.trace("Finding documents by package Id... [pkgId=" + id + ", primaryType=" + primaryType + ", categories=" + categories + ", allVersion=" + allVersion + ']');
        OperationContext context = getMinimalContext(cmisSession);
        Folder folder = findFolderById(id, context);
        Boolean isCmisRepoSearchable = cmisSession.getRepositoryInfo().getCapabilities().isAllVersionsSearchableSupported();

        List<Document> documents = getSearchStrategy().findDocuments(folder, primaryType, categories, false, allVersion, context);
        if (allVersion && !isCmisRepoSearchable && !documents.isEmpty()) {
            documents = findAllVersions(documents.get(0).getId());
        }

        logger.trace("Found " + documents.size() + " CMIS document(s).");
        return documents;
    }

    Document findDocumentByParentPath(final String path, final String name) {
        logger.trace("Finding document by parent path... [path=" + path + ", name=" + name + ']');
        OperationContext context = getMinimalContext(cmisSession);
        return (Document) cmisSession.getObjectByPath(path, name, context);
    }

    Document findDocumentById(final String id, final boolean latest) {
        logger.trace("Finding document by id... [id=" + id + ", latest=" + latest + ']');
        OperationContext context = getMinimalContext(cmisSession);
        return findDocumentById(id, latest, context);
    }


    List<Document> findDocumentsByUserId(final String userId, String primaryType, String leosAuthority) {
        logger.trace("Finding document by user id... userId=" + userId);
        return findDocumentsForUser(userId, primaryType, leosAuthority);
    }


    List<Document> findDocumentsByStatus(LeosLegStatus status, String primaryType) {
        OperationContext context = getMinimalContext(cmisSession);
        return getSearchStrategy().findDocumentsByStatus(status, primaryType, context);
    }


    List<Document> findAllVersions(final String id) {
        logger.trace("Finding all document versions... [id=" + id + ']');
        OperationContext context = getMinimalContext(cmisSession);
        Document document = findDocumentById(id, false, context);
        final List<Document> versions = document.getAllVersions();
        logger.trace("Found " + versions.size() + " CMIS version(s).");
        return versions;
    }

    private Document findDocumentById(String id, boolean latest, OperationContext context) {
        CmisObject cmisObject = latest ? cmisSession.getLatestDocumentVersion(id, context) : cmisSession.getObject(id, context);
        require(cmisObject instanceof Document, "CMIS object referenced by id [" + id + "] is not a Document!");
        return (Document) cmisObject;
    }

    private List<Document> findDocumentsForUser(final String userId, String primaryType, String leosAuthority) {
        OperationContext context = OperationContextProvider.getOperationContext(cmisSession, "cmis:lastModificationDate DESC");
        final List<Document> documents = getSearchStrategy().findDocumentsForUser(userId, primaryType, leosAuthority, context);
        logger.trace("Found " + documents.size() + " docuemnts for " + userId);
        return documents;
    }

    private Folder findFolderByPath(String path, OperationContext context) {
        boolean pathAvailable = cmisSession.existsPath(path);
        require(pathAvailable, "Path [" + path + "] is not available in CMIS repository!");
        CmisObject cmisObject = cmisSession.getObjectByPath(path, context);
        require(cmisObject instanceof Folder, "CMIS object referenced by path [" + path + "] is not a Folder!");
        return (Folder) cmisObject;
    }

    private Folder findFolderById(String id, OperationContext context) {
        boolean idAvailable = cmisSession.exists(id);
        require(idAvailable, "Id [" + id + "] is not available in CMIS repository!");
        CmisObject cmisObject = cmisSession.getObject(id, context);
        require(cmisObject instanceof Folder, "CMIS object referenced by id [" + id + "] is not a Folder!");
        return (Folder) cmisObject;
    }

    private void require(boolean requiredCondition, String message) {
        if (!requiredCondition) {
            throw new IllegalArgumentException(message);
        }
    }
}
