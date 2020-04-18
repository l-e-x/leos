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
package eu.europa.ec.leos.repository;

import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.domain.cmis.metadata.LeosMetadata;
import eu.europa.ec.leos.model.filter.QueryFilter;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * LEOS Repository *generic* interface.
 * <p>
 * Represents collections of *generic* document or package entities, independently of how they are stored.
 * Provides *generic* methods to persist and retrieve entity related data to and from a storage system.
 * Allows CRUD operations based on strongly typed Business Entities: [LeosDocument], [LeosMetadata] and [LeosPackage].
 */
public interface LeosRepository {

    /**
     * Creates a document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the document.
     * @param path       the path where to create the document.
     * @param name       the name of the document.
     * @param metadata   the metadata of the document.
     * @param type       the type class of the document.
     * @return the created document.
     */
    <D extends LeosDocument, M extends LeosMetadata> D createDocument(String templateId, String path, String name, M metadata, Class<? extends D> type);

    /**
     * Creates a document from a given template and with the specified characteristics.
     *
     * @param path     the path where to create the document.
     * @param name     the name of the document.
     * @param metadata the metadata of the document.
     * @param type     the type class of the document.
     * @return the created document.
     */
    <D extends LeosDocument, M extends LeosMetadata> D createDocumentFromContent(String path, String name, M metadata, Class<? extends D> type, String leosCategory, byte[] contentBytes);

    /**
     * Creates a leg document from a given content and with the specified characteristics.
     *
     * @param path              the path where to create the leg document.
     * @param name              the name of the leg document.
     * @param jobId             the ToolBox Service jobId
     * @param milestoneComments the milestone comments list
     * @param contentBytes      the leg document content
     * @param status            the leg document status
     * @return the created leg document.
     */
    LegDocument createLegDocumentFromContent(String path, String name, String jobId, List<String> milestoneComments, byte[] contentBytes, LeosLegStatus status,
                                             List<String> containedDocuments);

    /**
     * Updating Leg document status and content.
     *
     * @param id           the leg document id.
     * @param status       the updated status.
     * @param contentBytes the leg document content
     * @param versionType  the version type to be created
     * @param comment      the updated version comment
     * @return the updated Leg document
     */
    LegDocument updateLegDocument(String id, LeosLegStatus status, byte[] contentBytes, VersionType versionType, String comment);

    /**
     * Updating Leg document status.
     *
     * @param id     the leg document id.
     * @param status the updated status.
     * @return the updated Leg document
     */
    LegDocument updateLegDocument(String id, LeosLegStatus status);

    /**
     * Updates a document with the given metadata.
     *
     * @param id       the ID of the document to update.
     * @param metadata the metadata of the document.
     * @param type     the type class of the document.
     * @return the updated document.
     */
    <D extends LeosDocument, M extends LeosMetadata> D updateDocument(String id, M metadata, Class<? extends D> type);

    /**
     * Updates a document with the given content.
     *
     * @param id      the ID of the document to update.
     * @param content the content of the document.
     * @param versionType the version type to be created
     * @param comment the comment of the update, optional.
     * @param type    the type class of the document.
     * @return the updated document.
     */
    <D extends LeosDocument> D updateDocument(String id, byte[] content, VersionType versionType, String comment, Class<? extends D> type);

    <D extends LeosDocument> D updateMilestoneComments(String id, byte[] content, List<String> milestoneComments, VersionType versionType, String comment, Class<? extends D> type);

    <D extends LeosDocument> D updateMilestoneComments(String id, List<String> milestoneComments, Class<? extends D> type);

    /**
     * Updates a document with the given metadata and content.
     *
     * @param id       the ID of the document to update.
     * @param metadata the metadata of the document.
     * @param content  the content of the document.
     * @param versionType  the version type to be created
     * @param comment  the comment of the update, optional.
     * @param type     the type class of the document.
     * @return the updated document.
     */
    <D extends LeosDocument, M extends LeosMetadata> D updateDocument(String id, M metadata, byte[] content, VersionType versionType, String comment, Class<? extends D> type);

    /**
     * Updates a document with the given collaborators.
     *
     * @param id            the ID of the document to update.
     * @param collaborators the map of users to authorities.
     * @param type          the type class of the document.
     * @return the updated document.
     */
    <D extends LeosDocument> D updateDocument(String id, Map<String, String> collaborators, Class<? extends D> type);

    /**
     * Finds a document with the specified characteristics.
     *
     * @param id     the ID of the document to retrieve.
     * @param type   the type class of the document.
     * @param latest retrieves the *latest version* of the document, when *true*.
     * @return the found document.
     */
    <D extends LeosDocument> D findDocumentById(String id, Class<? extends D> type, boolean latest);

    /**
     * Finds a document with the specified characteristics.
     *
     * @param path the path where to find the document.
     * @param name the name of the document to retrieve.
     * @param type the type class of the document.
     * @return the found document.
     */
    <D extends LeosDocument> D findDocumentByParentPath(String path, String name, Class<? extends D> type);

    /**
     * Finds documents with the specified characteristics.
     *
     * @param path the path where to find the document.
     * @param type the type class of the document.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByParentPath(String path, Class<? extends D> type, boolean descendants, boolean fetchContent);

    /**
     * Finds all versions of a document with the specified characteristics.
     *
     * @param id   the ID of the document to retrieve.
     * @param type the type class of the document.
     * @return the list of found document versions or empty.
     */
    <D extends LeosDocument> List<D> findDocumentVersionsById(String id, Class<? extends D> type, boolean fetchContent);

    /**
     * Deletes a document with the specified characteristics.
     *
     * @param id the ID of the document to delete.
     */
    void deleteDocumentById(String id);

    /**
     * Creates a [LeosPackage] with the specified characteristics.
     *
     * @param path the path where to create the package.
     * @param name the name of the package.
     * @return the created package.
     */
    LeosPackage createPackage(String path, String name);

    /**
     * Deletes a [LeosPackage] with the specified characteristics.
     *
     * @param path the path of the package to be deleted.
     */
    void deletePackage(String path);

    /**
     * Finds a [LeosPackage] with the specified characteristics.
     *
     * @param documentId the ID of a document inside the package.
     * @return the found package.
     */
    LeosPackage findPackageByDocumentId(String documentId);

    /**
     * Finds leg documents with the specified characteristics.
     *
     * @param id   the path where to find the document.
     * @param type the type class of the document.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByPackageId(String id, Class<? extends D> type, boolean allVersion, boolean fetchContent);

    /**
     * Finds leg documents with the specified status.
     *
     * @param status the status of the document.
     * @param type   the type class of the document.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByStatus(LeosLegStatus status, Class<? extends D> type);

    /**
     * Finds a document with the specified characteristics.
     *
     * @param userId the ID of the User.
     * @param type   the type class of the document.
     * @return the found document.
     */
    <D extends LeosDocument> List<D> findDocumentsByUserId(String userId, Class<? extends D> type, String leosAuthority);

    <D extends LeosDocument> Stream<D> findPagedDocumentsByParentPath(String path, Class<? extends D> type, boolean descendants, boolean fetchContent,
                                                                      int startIndex, int maxResults, QueryFilter workspaceFilter);

    <D extends LeosDocument> int findDocumentCountByParentPath(String path, Class<? extends D> type, boolean descendants, QueryFilter workspaceFilter);

    /**
     * Finds a document with the specified metadata reference.
     *
     * @param ref the metadata reference of the document.
     * @param type the type class of the document.
     * @return the found document.
     */
    <D extends LeosDocument> D findDocumentByRef(String ref, Class<? extends D> type);
    
    <D extends LeosDocument> List<D> findAllMinorsForIntermediate(Class<? extends D> type, String docRef, String currIntVersion, String prevIntVersion, int startIndex, int maxResults);
    
    <D extends LeosDocument> int findAllMinorsCountForIntermediate(Class<? extends D> type, String docRef, String currIntVersion, String prevIntVersion);

    <D extends LeosDocument> Integer findAllMajorsCount(Class<? extends D> type, String docRef);

    <D extends LeosDocument> List<D> findAllMajors(Class<? extends D> type, String docRef, int startIndex, int maxResult);
    
    <D extends LeosDocument> D findLatestMajorVersionById(Class<? extends D> type, String documentId);

    <D extends LeosDocument> List<D> findRecentMinorVersions(Class<? extends D> type, String documentRef, String versionLabel, int startIndex, int maxResults);

    <D extends LeosDocument> Integer findRecentMinorVersionsCount(Class<? extends D> type, String documentRef, String versionLabel);

}
