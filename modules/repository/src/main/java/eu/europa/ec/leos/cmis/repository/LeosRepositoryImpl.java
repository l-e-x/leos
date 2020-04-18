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
import eu.europa.ec.leos.cmis.extensions.CmisFolderExtensions;
import eu.europa.ec.leos.cmis.extensions.LeosMetadataExtensions;
import eu.europa.ec.leos.cmis.mapping.CmisMapper;
import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.LeosMetadata;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.repository.LeosRepository;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.europa.ec.leos.cmis.support.RepositoryUtil.updateDocumentProperties;
import static eu.europa.ec.leos.cmis.support.RepositoryUtil.updateMilestoneCommentsProperties;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * LEOS Repository implementation.
 *
 * @constructor Creates a generic LEOS Repository, injected with a CMIS Repository and a Security Context.
 */
@Repository
public class LeosRepositoryImpl implements LeosRepository {

    private static final Logger logger = LoggerFactory.getLogger(LeosRepositoryImpl.class);

    private final String legMimeType;
    private final String leosDocMimeType;
    private final CmisRepository cmisRepository;
    private final SecurityContext securityContext;
    private final LeosPermissionAuthorityMapHelper authorityMapHelper;

    public LeosRepositoryImpl(CmisRepository cmisRepository, SecurityContext securityContext, LeosPermissionAuthorityMapHelper authorityMapHelper) {
        this.cmisRepository = cmisRepository;
        this.securityContext = securityContext;
        this.authorityMapHelper = authorityMapHelper;
        legMimeType = "application/octet-stream";
        leosDocMimeType = "application/akn+xml";
    }

    @Override
    public <D extends LeosDocument, M extends LeosMetadata> D createDocument(String templateId, String path, String name,
                                                                             M metadata, Class<? extends D> type) {
        logger.trace("Creating document... [template=" + templateId + ", path=" + path + ", name=" + name + ']');

        long startTimeNanos = System.nanoTime();
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, name);
        properties.putAll(LeosMetadataExtensions.toCmisProperties(metadata));
        properties.put(CmisProperties.COLLABORATORS.getId(), singletonList(getAccessRecord(securityContext.getUser().getLogin(), authorityMapHelper.getRoleForDocCreation())));
        properties.put(CmisProperties.INITIAL_CREATED_BY.getId(), securityContext.getUser().getLogin());
        properties.put(CmisProperties.INITIAL_CREATION_DATE.getId(), Date.from(Instant.now()));

        Document doc = cmisRepository.createDocumentFromSource(templateId, path, properties);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document creation took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to create document! [template=" + templateId + ", path=" + path + ", name=" + name + ']'));
    }

    @Override
    public <D extends LeosDocument, M extends LeosMetadata> D createDocumentFromContent(String path, String name, M metadata, Class<? extends D> type, String leosCategory, byte[] contentBytes) {
        logger.trace("Creating document From Content... [path=" + path + ", name=" + name + ']');
        long startTimeNanos = System.nanoTime();

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, name);
        properties.put(PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
        properties.put(PropertyIds.OBJECT_TYPE_ID, CmisMapper.cmisPrimaryType(XmlDocument.class));
        properties.put(CmisProperties.DOCUMENT_CATEGORY.getId(), leosCategory);
        properties.putAll(LeosMetadataExtensions.toCmisProperties(metadata));
        properties.put(CmisProperties.COLLABORATORS.getId(), singletonList(getAccessRecord(securityContext.getUser().getLogin(), authorityMapHelper.getRoleForDocCreation())));
        properties.put(CmisProperties.INITIAL_CREATED_BY.getId(), securityContext.getUser().getLogin());
        properties.put(CmisProperties.INITIAL_CREATION_DATE.getId(), Date.from(Instant.now()));

        Document doc = cmisRepository.createDocumentFromContent(path, name, properties, leosDocMimeType, contentBytes);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document creation took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to create document! [path=" + path + ", name=" + name + ']'));
    }

    @Override
    public LegDocument createLegDocumentFromContent(String path, String name, String jobId, List<String> milestoneComments, byte[] contentBytes, LeosLegStatus status,
                                                    List<String> containedDocuments) {
        logger.trace("Creating leg document from content... [path=" + path + ", name=" + name + ']');
        long startTimeNanos = System.nanoTime();

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, name);
        properties.put(PropertyIds.BASE_TYPE_ID, BaseTypeId.CMIS_DOCUMENT.value());
        properties.put(PropertyIds.OBJECT_TYPE_ID, CmisMapper.cmisPrimaryType(LegDocument.class));
        properties.put(CmisProperties.DOCUMENT_CATEGORY.getId(), LeosCategory.LEG.name());
        properties.put(CmisProperties.JOB_ID.getId(), jobId);
        properties.put(CmisProperties.JOB_DATE.getId(), Date.from(Instant.now()));
        properties.put(CmisProperties.MILESTONE_COMMENTS.getId(), milestoneComments);
        properties.put(CmisProperties.STATUS.getId(), status.name());
        properties.put(CmisProperties.INITIAL_CREATED_BY.getId(), securityContext.getUser().getLogin());
        properties.put(CmisProperties.INITIAL_CREATION_DATE.getId(), Date.from(Instant.now()));
        properties.put(CmisProperties.CONTAINED_DOCUMENTS.getId(), containedDocuments);

        Document doc = cmisRepository.createDocumentFromContent(path, name, properties, legMimeType, contentBytes);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository Leg document creation from content took " + time + " milliseconds.");

        return toLeosDocument(doc, LegDocument.class, true)
                .orElseThrow(() -> new IllegalStateException("Unable to create leg document from content! [path=" + path + ", name=" + name + ']'));
    }

    @Override
    public LegDocument updateLegDocument(String id, LeosLegStatus status) {
        logger.trace("Updating Leg document status... [id=" + id + ", status=" + status.name() + ']');
        long startTimeNanos = System.nanoTime();

        Map<String, Object> properties = new HashMap<>();
        properties.put(CmisProperties.STATUS.getId(), status.name());

        Document doc = cmisRepository.updateDocument(id, properties);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository Leg document status update took " + time + " milliseconds.");

        return toLeosDocument(doc, LegDocument.class, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update leg document status! [id=" + id + ", status=" + status.name() + ']'));
    }

    @Override
    public LegDocument updateLegDocument(String id, LeosLegStatus status, byte[] contentBytes, VersionType versionType, String comment) {
        logger.debug("Updating Leg document status and content... [id=" + id + ", status=" + status.name() + ", content size=" + contentBytes.length + ", versionType=" + versionType + ", comment=" + comment + ']');
        long startTimeNanos = System.nanoTime();

        Map<String, Object> properties = new HashMap<>();
        properties.put(CmisProperties.STATUS.getId(), status.name());

        Document doc = cmisRepository.updateDocument(id, properties, contentBytes, versionType, comment);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository Leg document status and content update took " + time + " milliseconds.");

        return toLeosDocument(doc, LegDocument.class, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update leg document! [id=" + id + ", status=" + status.name() + ']'));
    }

    @Override
    public <D extends LeosDocument, M extends LeosMetadata> D updateDocument(String id, M metadata, Class<? extends D> type) {
        logger.trace("Updating document metadata... [id=" + id + ']');

        long startTimeNanos = System.nanoTime();
        Document doc = cmisRepository.updateDocument(id, updateDocumentProperties(metadata));
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document update took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update document! [id=" + id + ']'));
    }

    @Override
    public <D extends LeosDocument, M extends LeosMetadata> D updateDocument(String id, M metadata, byte[] content, VersionType versionType, String comment, Class<? extends D> type) {
        logger.trace("Updating document metadata and content... [id=" + id + ", comment=" + comment + ']');

        long startTimeNanos = System.nanoTime();

        Document doc = cmisRepository.updateDocument(id, updateDocumentProperties(metadata), content, versionType, comment);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document update took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update document! [id=" + id + ", comment=" + comment + ']'));
    }

    @Override
    public <D extends LeosDocument> D updateDocument(String id, byte[] content, VersionType versionType, String comment, Class<? extends D> type) {
        logger.trace("Updating document content... [id=" + id + ", comment=" + comment + ']');

        long startTimeNanos = System.nanoTime();

        Document doc = cmisRepository.updateDocument(id, updateMilestoneCommentsProperties(emptyList()), content, versionType, comment);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document update took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update document! [id=" + id + ", comment=" + comment + ']'));
    }

    @Override
    public <D extends LeosDocument> D updateDocument(String id, Map<String, String> collaborators, Class<? extends D> type) {
        logger.trace("Updating document collaborators... [id=" + id + ']');
        long startTimeNanos = System.nanoTime();

        Map<String, Object> properties = new HashMap<>(updateMilestoneCommentsProperties(emptyList()));

        List<String> collaboratorUsers = collaborators.entrySet()
                .stream()
                .map(clEntry -> getAccessRecord(clEntry.getKey(), clEntry.getValue()))
                .collect(toList());

        properties.put(CmisProperties.COLLABORATORS.getId(), collaboratorUsers);

        Document doc = cmisRepository.updateDocument(id, properties);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document update took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update document! [id=" + id + ']'));
    }

    @Override
    public <D extends LeosDocument> D updateMilestoneComments(String id, byte[] content, List<String> milestoneComments, VersionType versionType, String comment, Class<? extends D> type) {
        logger.trace("Updating document metadata and content... [id=" + id + ", comment=" + comment + ']');

        long startTimeNanos = System.nanoTime();
        Map<String, List<String>> properties = updateMilestoneCommentsProperties(milestoneComments);

        Document doc = cmisRepository.updateDocument(id, properties, content, versionType, comment);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document update took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update document! [id=" + id + ", comment=" + comment + ']'));
    }

    @Override
    public <D extends LeosDocument> D updateMilestoneComments(String id, List<String> milestoneComments, Class<? extends D> type) {
        logger.trace("Updating document metadata... [id=" + id + ']');
        long startTimeNanos = System.nanoTime();

        Map<String, List<String>> properties = updateMilestoneCommentsProperties(milestoneComments);

        Document doc = cmisRepository.updateDocument(id, properties);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document update took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalStateException("Unable to update document! [id=" + id + ']'));
    }

    @Override
    public <D extends LeosDocument> D findDocumentById(String id, Class<? extends D> type, boolean latest) {
        logger.trace("Finding document by ID... [id=" + id + ", latest=" + latest + ']');

        long startTimeNanos = System.nanoTime();
        Document doc = cmisRepository.findDocumentById(id, latest);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalArgumentException("Document not found! [id=" + id + ", latest=" + latest + ']'));
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByUserId(String userId, Class<? extends D> type, String leosAuthority) {
        logger.trace("Finding documents for user... userId=" + userId + ']');

        long startTimeNanos = System.nanoTime();
        String primaryType = CmisMapper.cmisPrimaryType(type);
        List<Document> docs = cmisRepository.findDocumentsByUserId(userId, primaryType, leosAuthority);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        return toLeosDocuments(docs, type, false);
    }

    @Override
    public <D extends LeosDocument> D findDocumentByParentPath(String path, String name, Class<? extends D> type) {
        logger.trace("Finding document by parent path... [path=" + path + ", name=" + name + ']');

        long startTimeNanos = System.nanoTime();
        Document doc = cmisRepository.findDocumentByParentPath(path, name);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        return toLeosDocument(doc, type, true)
                .orElseThrow(() -> new IllegalArgumentException("Document not found! [path=" + path + ", name=" + name + ']'));
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByParentPath(String path, Class<? extends D> type, boolean descendants, boolean fetchContent) {
        logger.trace("Finding documents by parent path... [path=" + path + ", type=" + type.getSimpleName() + ']');

        long startTimeNanos = System.nanoTime();
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Set<LeosCategory> categories = CmisMapper.cmisCategories(type);

        List<Document> docs = cmisRepository.findDocumentsByParentPath(path, primaryType, categories, descendants);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        return toLeosDocuments(docs, type, fetchContent);
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentVersionsById(String id, Class<? extends D> type, boolean fetchContent) {
        logger.trace("Finding document versions by ID... [id=" + id + ']');

        long startTimeNanos = System.nanoTime();
        List<Document> docs = cmisRepository.findAllVersions(id);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository versions search took " + time + " milliseconds.");

        return toLeosDocuments(docs, type, fetchContent);
    }
    
    @Override
    public void deleteDocumentById(String id) {
        logger.trace("Deleting Document... [id=" + id + ']');
        long startTimeNanos = System.nanoTime();
        cmisRepository.deleteDocumentById(id);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document deletion took " + time + " milliseconds.");
    }

    @Override
    public LeosPackage createPackage(String path, String name) {
        logger.trace("Creating package... [path=" + path + ", name=" + name + ']');

        long startTimeNanos = System.nanoTime();
        Folder folder = cmisRepository.createFolder(path, name);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository package creation took " + time + " milliseconds.");
        if (folder != null) {
            return CmisFolderExtensions.toLeosPackage(folder);
        }

        throw new IllegalStateException("Unable to create Package! [path=" + path + ", name=" + name + ']');
    }

    @Override
    public void deletePackage(String path) {
        logger.trace("Deleting package... [path=" + path + ']');
        long startTimeNanos = System.nanoTime();
        cmisRepository.deleteFolder(path);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository package deletion took " + time + " milliseconds.");
    }

    @Override
    public LeosPackage findPackageByDocumentId(String documentId) {
        logger.trace("Finding package by document ID... [documentId=" + documentId + ']');

        long startTimeNanos = System.nanoTime();
        Document doc = cmisRepository.findDocumentById(documentId, false);
        Folder folder = doc.getParents().stream().findFirst().orElse(null);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository package search took " + time + " milliseconds.");
        if (folder != null) {
            return CmisFolderExtensions.toLeosPackage(folder);
        }

        throw new IllegalStateException("Package not found! [documentId=" + documentId + ']');
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByPackageId(String id, Class<? extends D> type, boolean allVersion, boolean fetchContent) {
        logger.trace("Finding documents by parent id... [pkgId=" + id + ", type=" + type.getSimpleName() + ']');

        long startTimeNanos = System.nanoTime();
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Set<LeosCategory> categories = CmisMapper.cmisCategories(type);
        List<Document> docs = cmisRepository.findDocumentsByPackageId(id, primaryType, categories, allVersion);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        return toLeosDocuments(docs, type, fetchContent);
    }

    @Override
    public <D extends LeosDocument> List<D> findDocumentsByStatus(LeosLegStatus status, Class<? extends D> type) {
        logger.trace("Finding documents for status... status=" + status + ']');

        long startTimeNanos = System.nanoTime();
        String primaryType = CmisMapper.cmisPrimaryType(type);
        List<Document> docs = cmisRepository.findDocumentsByStatus(status, primaryType);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        return toLeosDocuments(docs, type, false);
    }

    private String getAccessRecord(String userLogin, String authority) {
        return userLogin + "::" + authority;
    }

    private <D extends LeosDocument> Optional<D> toLeosDocument(Document doc, Class<? extends D> type, boolean fetchContent) {
        D leosDocument = null;
        if (doc != null) {
            leosDocument = CmisDocumentExtensions.toLeosDocument(doc, type, fetchContent);
        }
        return Optional.ofNullable(leosDocument);
    }

    private <D extends LeosDocument> List<D> toLeosDocuments(List<Document> docs, Class<? extends D> type, boolean fetchContent) {
        List<D> leosDocuments = emptyList();
        if (docs != null) {
            leosDocuments = docs.stream()
                    .map(doc -> CmisDocumentExtensions.toLeosDocument(doc, type, fetchContent))
                    .collect(toList());
        }
        return leosDocuments;
    }

    @Override
    public <D extends LeosDocument> Stream<D> findPagedDocumentsByParentPath(String path, Class<? extends D> type, boolean descendants, boolean fetchContent,
                                                                             int startIndex, int maxResults, QueryFilter workspaceFilter) {
        logger.trace("Finding documents by parent path... [path=$path, type=${type.simpleName}]");
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Set<LeosCategory> categories = CmisMapper.cmisCategories(type);
        Stream<Document> docs = cmisRepository.findPagedDocumentsByParentPath(path, primaryType, categories, descendants, startIndex, maxResults, workspaceFilter);

        logger.trace("CMIS Repository document search took $time milliseconds.");
        return docs.map(doc -> CmisDocumentExtensions.toLeosDocument(doc, type, fetchContent));
    }

    @Override
    public <D extends LeosDocument> int findDocumentCountByParentPath(String path, Class<? extends D> type, boolean descendants, QueryFilter workspaceFilter) {
        logger.trace("Finding documents by parent path... [path=$path, type=${type.simpleName}]");
        int docCount = 0;
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Set<LeosCategory> categories = CmisMapper.cmisCategories(type);
        docCount = cmisRepository.findDocumentCountByParentPath(path, primaryType, categories, descendants, workspaceFilter);

        logger.trace("CMIS Repository document search took $time milliseconds.");
        return docCount;
    }

    @Override
    public <D extends LeosDocument> D findDocumentByRef(String ref, Class<? extends D> type) {
        logger.trace("Finding document with ref... [ref=" + ref + ']');

        long startTimeNanos = System.nanoTime();
        String primaryType = CmisMapper.cmisPrimaryType(type);
        List<Document> docs = cmisRepository.findDocumentsByRef(ref, primaryType);
        long time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTimeNanos);
        logger.trace("CMIS Repository document search took " + time + " milliseconds.");

        if (docs.isEmpty() || (docs.size() > 1)) {
            throw new IllegalStateException("Error occurred retrieving document! [=" + ref + ']');
        } else {
            return toLeosDocument(docs.get(0), type, true)
                .orElseThrow(() -> new IllegalStateException("Error occurred retrieving document! [=" + ref + ']'));
        }
    }
    
    @Override
    public <D extends LeosDocument> List<D> findAllMinorsForIntermediate(Class<? extends D> type, String docRef, String currIntVersion, String prevIntVersion, int startIndex, int maxResults) {
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Stream<Document> documents = cmisRepository.findAllMinorsForIntermediate(primaryType, docRef, currIntVersion, prevIntVersion, startIndex, maxResults);
        return documents.map(doc -> CmisDocumentExtensions.toLeosDocument(doc, type, false))
                .collect(Collectors.toList());
    }
    
    @Override
    public <D extends LeosDocument> int findAllMinorsCountForIntermediate(Class<? extends D> type, String docRef, String currIntVersion, String prevIntVersion) {
     String primaryType = CmisMapper.cmisPrimaryType(type);
     return cmisRepository.findAllMinorsCountForIntermediate(primaryType, docRef, currIntVersion, prevIntVersion);
    }
    
    @Override
    public <D extends LeosDocument> Integer findAllMajorsCount(Class<? extends D> type, String docRef) {
        String primaryType = CmisMapper.cmisPrimaryType(type);
        return cmisRepository.findAllMajorsCount(primaryType, docRef);
    }
    
    @Override
    public <D extends LeosDocument> List<D> findAllMajors(Class<? extends D> type, String docRef, int startIndex, int maxResult) {
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Stream<Document> documents = cmisRepository.findAllMajors(primaryType, docRef, startIndex, maxResult);
        return documents.map(doc -> CmisDocumentExtensions.toLeosDocument(doc, type, false))
                .collect(Collectors.toList());
    }
    
    @Override
    public <D extends LeosDocument> D findLatestMajorVersionById(Class<? extends D> type, String documentId) {
        Document doc = cmisRepository.findLatestMajorVersionById(documentId);
        return CmisDocumentExtensions.toLeosDocument(doc, type, false);
    }
    
    @Override
    public <D extends LeosDocument> List<D> findRecentMinorVersions(Class<? extends D> type, String documentRef, String lastMajorId, int startIndex, int maxResults) {
        String primaryType = CmisMapper.cmisPrimaryType(type);
        Stream<Document> documents = cmisRepository.findRecentMinorVersions(primaryType, documentRef, lastMajorId, startIndex, maxResults);
        return documents.map(doc -> CmisDocumentExtensions.toLeosDocument(doc, type, false))
                .collect(Collectors.toList());
    }
    
    @Override
    public <D extends LeosDocument> Integer findRecentMinorVersionsCount(Class<? extends D> type, String documentRef, String versionLabel) {
        String primaryType = CmisMapper.cmisPrimaryType(type);
        return cmisRepository.findRecentMinorVersionsCount(primaryType, documentRef, versionLabel);
    }
    
}
