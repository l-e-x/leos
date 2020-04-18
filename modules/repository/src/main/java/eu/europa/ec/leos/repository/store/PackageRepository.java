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
package eu.europa.ec.leos.repository.store;

import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;

import java.util.List;

/**
 * LEOS Package Repository interface.
 * <p>
 * Represents collections of *packages*, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [LeosPackage].
 */

public interface PackageRepository {

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
     * Finds Leg document with specified id.
     *
     * @param id     the leg document id.
     * @param latest true if the latest version is requested.
     */
    LegDocument findLegDocumentById(String id, boolean latest);

    /**
     * Updating Leg document status.
     *
     * @param id     the leg document id.
     * @param status the updated status.
     */
    LegDocument updateLegDocument(String id, LeosLegStatus status);

    /**
     * Updating Leg document status and content.
     *
     * @param id      the leg document id.
     * @param status  the updated status.
     * @param content the leg document content
     * @param versionType the version type to be created
     * @param comment the updated version comment
     */
    LegDocument updateLegDocument(String id, LeosLegStatus status, byte[] content, VersionType versionType, String comment);

    /**
     * Finds a [LeosPackage] with the specified characteristics.
     *
     * @param documentId the ID of a document inside the package.
     * @return the found package.
     */
    LeosPackage findPackageByDocumentId(String documentId);

    /**
     * Finds documents with the specified characteristics.
     *
     * @param path the path of the package where to find the documents.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByPackagePath(String path, Class<? extends D> type, boolean fetchContent);

    /**
     * Finds documents with the specified characteristics.
     *
     * @param path the path of the package where to find the documents
     * @param name the file name of the document to find.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> D findDocumentByPackagePathAndName(String path, String name, Class<? extends D> type);


    /**
     * Finds leg most recent leg document that contains a specific version of a document.
     *
     * @param path the path of the package where to find the documents
     * @param versionedReference the document reference with the version.
     * @return A leg document that contains the reference
     */
    LegDocument findLastLegByVersionedReference(String path, String versionedReference);

    /**
     * Finds documents with the specified characteristics.
     *
     * @param id   the id of the package where to find the documents.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByPackageId(String id, Class<? extends D> type, boolean allVersion, boolean fetchContent);

    /**
     * Finds documents with the specified characteristics.
     *
     * @param userId the userId of the user
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByUserId(String userId, Class<? extends D> type, String leosAuthority);

    /**
     * Finds leg documents with the specified status.
     *
     * @param status the status of the document.
     * @param type   the type class of the document.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> List<D> findDocumentsByStatus(LeosLegStatus status, Class<? extends D> type);
}
