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

import eu.europa.ec.leos.cmis.extensions.CmisDocumentExtensions;
import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.cmis.search.SearchStrategy;
import eu.europa.ec.leos.cmis.search.SearchStrategyProvider;
import eu.europa.ec.leos.cmis.support.OperationContextProvider;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.model.filter.QueryFilter;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ObjectId;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.UnfileObject;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static eu.europa.ec.leos.cmis.support.OperationContextProvider.getMinimalContext;

@Repository
public class CmisRepository {

    private static final Logger logger = LoggerFactory.getLogger(CmisRepository.class);

    private final Session cmisSession;
    private static final Map<String, Long> synchronizedKeys = new ConcurrentHashMap<>();

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

    Document createDocumentFromContent(final String path, final String name, Map<String, ?> properties, final String mimeType, byte[] contentBytes) {
        logger.trace("Creating document... [path=" + path + ", name=" + name + ", mimeType=" + mimeType + "]");
        OperationContext context = getMinimalContext(cmisSession);

        Folder targetFolder = findFolderByPath(path, context);
        ByteArrayInputStream byteStream = new ByteArrayInputStream(contentBytes);
        ContentStream contentStream = cmisSession.getObjectFactory().createContentStream(name, (long) contentBytes.length, mimeType, byteStream);

        Map<String, Object> updatedProperties = new LinkedHashMap<>();
        updatedProperties.putAll(properties);
        updatedProperties.put(CmisProperties.VERSION_TYPE.getId(), VersionType.MINOR.value());
        updatedProperties.put(CmisProperties.VERSION_LABEL.getId(), getNextVersionLabel(VersionType.MINOR, null));

        return targetFolder.createDocument(updatedProperties, contentStream, VersioningState.MINOR);
    }

    Document createDocumentFromSource(final String sourceId, String path, Map<String, ?> properties) {
        logger.trace("Creating document from source... [sourceId=" + sourceId + "]");
        OperationContext context = getMinimalContext(cmisSession);

        Folder targetFolder = findFolderByPath(path, context);
        Document sourceDoc = findDocumentById(sourceId, false, context);

        Map<String, Object> updatedProperties = new LinkedHashMap<>();
        updatedProperties.putAll(properties);
        updatedProperties.put(CmisProperties.VERSION_TYPE.getId(), VersionType.MINOR.value());
        updatedProperties.put(CmisProperties.VERSION_LABEL.getId(), getNextVersionLabel(VersionType.MINOR, null));

        return sourceDoc.copy(targetFolder, updatedProperties, VersioningState.MINOR, null, null, null, context);
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

    public Document updateDocument(String id, Map<String, ?> properties, byte[] updatedDocumentBytes, VersionType versionType, String comment) {
        try {
            synchronized (getSyncKey(id)) {
                logger.trace("Updating document properties and content... [id=" + id + ']');
                Document pwc = checkOutWorkingCopy(id);
                Document udpatedDocument = checkInWorkingCopy(pwc, properties, updatedDocumentBytes, versionType, comment);
                logger.trace("Updated document properties and content...");
                if (udpatedDocument == null) {
                    throw new IllegalStateException("Update not successful for document:" + id);
                } else {
                    return udpatedDocument;
                }
            }
        } finally {
            cleanSyncKey(id);
        }
    }

    private Document checkInWorkingCopy(Document pwc, Map<String, ?> properties, byte[] updatedDocumentBytes, VersionType versionType, String comment) {
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

                updatedProperties.put(CmisProperties.VERSION_TYPE.getId(), versionType.value());
                updatedProperties.put(CmisProperties.VERSION_LABEL.getId(),
                        getNextVersionLabel(versionType, pwc.getAllVersions().size() > 1 ? CmisDocumentExtensions.getLeosVersionLabel(pwc.getAllVersions().get(1)) : null));

                updatedDocId = pwc.checkIn(versionType.equals(VersionType.MAJOR) || versionType.equals(VersionType.INTERMEDIATE), updatedProperties, contentStream, comment);
                logger.trace("Document checked-in successfully...[updated document id:" + updatedDocId.getId() + ']');
            } catch (CmisBaseException e) {
                logger.error("Document update failed, trying to cancel the checkout", e);
                pwc.cancelCheckOut();
                throw e;
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

    private static synchronized Long getSyncKey(String key) {
        if (!synchronizedKeys.containsKey(key) ) {
            synchronizedKeys.put(key, new Long(System.currentTimeMillis()));
        }
        return synchronizedKeys.get(key);
    }

    private static void cleanSyncKey(String key) {
        synchronizedKeys.remove(key);
    }

    private String getNextVersionLabel(VersionType versionType, String oldVersion) {
        if (StringUtils.isEmpty(oldVersion)) {
            if (versionType.equals(VersionType.MAJOR)) {
                return "1.0.0";
            } else if (versionType.equals(VersionType.INTERMEDIATE)) {
                return "0.1.0";
            } else {
                return "0.0.1";
            }
        }

        String[] newVersion = oldVersion.split("\\.");
        if (versionType.equals(VersionType.MAJOR)) {
            newVersion[0] = Integer.parseInt(newVersion[0]) + 1 + "";
            newVersion[1] = "0";
            newVersion[2] = "0";
        } else if (versionType.equals(VersionType.INTERMEDIATE)) {
            newVersion[1] = Integer.parseInt(newVersion[1]) + 1 + "";
            newVersion[2] = "0";
        } else {
            newVersion[2] = Integer.parseInt(newVersion[2]) + 1 + "";
        }
        return newVersion[0] + "." + newVersion[1] + "." + newVersion[2];
    }

    // Pagination changes
    Stream<Document> findPagedDocumentsByParentPath(String path, String primaryType, Set<LeosCategory> categories, boolean descendants, int startIndex, int maxResults, QueryFilter workspaceFilter) {
        logger.trace("Finding documents by parent path... [path=$path, primaryType=$primaryType, categories=$categories, descendants=$descendants]");
        OperationContext context = OperationContextProvider.getOperationContext(cmisSession, maxResults);

        Folder folder = findFolderByPath(path, context);
        Stream<Document> documents = getSearchStrategy().findDocumentPage(folder, primaryType, categories, descendants, false, context, startIndex, workspaceFilter);

        logger.trace("Found ${documents.count()} CMIS document(s).");
        return documents;
    }

    int findDocumentCountByParentPath(String path, String primaryType, Set<LeosCategory> categories, boolean descendants, QueryFilter workspaceFilter) {
        logger.trace("Finding documents by parent path... [path=$path, primaryType=$primaryType, categories=$categories, descendants=$descendants]");
        OperationContext context = OperationContextProvider.getMinimalContext(cmisSession);

        Folder folder = findFolderByPath(path, context);
        int documentCount = getSearchStrategy().findDocumentCount(folder, primaryType, categories, descendants, false, context, workspaceFilter);

        logger.trace("Found ${documentCount} CMIS document(s).");
        return documentCount;
    }

    List<Document> findDocumentsByRef(String ref, String primaryType) {
        OperationContext context = getMinimalContext(cmisSession);
        List<Document> documents = getSearchStrategy().findDocumentsByRef(ref, primaryType, context);
        logger.trace("Found " + documents.size() + " documents for " + ref);
        return documents;
    }
    
    public Stream<Document> findAllMinorsForIntermediate(String primaryType, String docRef, String currIntVersion, String prevIntVersion, int startIndex, int maxResults) {
        OperationContext context = OperationContextProvider.getOperationContext(cmisSession, maxResults);
        Stream<Document> documents = getSearchStrategy().findAllMinorsForIntermediate(primaryType, docRef, currIntVersion, prevIntVersion, startIndex, context);
        return documents;
    }
    
    public int findAllMinorsCountForIntermediate(String primaryType, String docRef, String currIntVersion, String prevIntVersion) {
        OperationContext context = getMinimalContext(cmisSession);
        return getSearchStrategy().findAllMinorsCountForIntermediate(primaryType, docRef, currIntVersion, prevIntVersion, context);
    }

    public Integer findAllMajorsCount(String primaryType, String docRef) {
        OperationContext context = getMinimalContext(cmisSession);
        return getSearchStrategy().findAllMajorsCount(primaryType, docRef, context);
    }

    public Stream<Document> findAllMajors(String primaryType, String docRef, int startIndex, int maxResult) {
        OperationContext context = OperationContextProvider.getOperationContext(cmisSession, maxResult);
        return getSearchStrategy().findAllMajors(primaryType, docRef, startIndex, context);
    }

    public Stream<Document> findRecentMinorVersions(String primaryType, String documentRef, String lastMajorId, int startIndex, int maxResults) {
        OperationContext context = OperationContextProvider.getOperationContext(cmisSession, maxResults);
        return getSearchStrategy().findRecentMinorVersions(primaryType, documentRef, lastMajorId, startIndex, context);
    }

    public Integer findRecentMinorVersionsCount(String primaryType, String documentRef, String versionLabel) {
        OperationContext context = getMinimalContext(cmisSession);
        return getSearchStrategy().findRecentMinorVersionsCount(primaryType, documentRef, versionLabel, context);
    }
    
    public Document findLatestMajorVersionById(String id) {
        OperationContext context = getMinimalContext(cmisSession);
        CmisObject cmisObject = cmisSession.getLatestDocumentVersion(id, true, context);
        require(cmisObject instanceof Document, "CMIS object referenced by id [" + id + "] is not a Document!");
        return (Document) cmisObject;
    }
}
