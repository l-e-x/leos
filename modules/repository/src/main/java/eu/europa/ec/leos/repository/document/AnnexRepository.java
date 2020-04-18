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
package eu.europa.ec.leos.repository.document;

import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import org.springframework.security.access.prepost.PostAuthorize;

import java.util.List;

/**
 * Annex Repository interface.
 * <p>
 * Represents collections of *Annex* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [Annex] and [AnnexMetadata].
 */
public interface AnnexRepository {
    /**
     * Creates an [Annex] document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the annex.
     * @param path       the path where to create the annex.
     * @param name       the name of the annex.
     * @param metadata   the metadata of the annex.
     * @return the created annex document.
     */
    Annex createAnnex(String templateId, String path, String name, AnnexMetadata metadata);

    /**
     * Creates an [Annex] document from a given content and with the specified characteristics.
     *
     * @param path     the path where to create the annex.
     * @param name     the name of the annex.
     * @param metadata the metadata of the annex.
     * @param content  the content of the annex.
     * @return the created annex document.
     */
    Annex createAnnexFromContent(String path, String name, AnnexMetadata metadata, byte[] content);

    /**
     * Updates an [Annex] document with the given metadata.
     *
     * @param id       the ID of the annex document to update.
     * @param metadata the metadata of the annex.
     * @return the updated annex document.
     */
    Annex updateAnnex(String id, AnnexMetadata metadata);

    /**
     * Updates an [Annex] document with the given content.
     *
     * @param id      the ID of the annex document to update.
     * @param content the content of the annex.
     * @param versionType  the version type to be created
     * @param comment the comment of the update, optional.
     * @return the updated annex document.
     */
    Annex updateAnnex(String id, byte[] content, VersionType versionType, String comment);

    /**
     * Updates a [Annex] document with the given metadata and content.
     *
     * @param id       the ID of the annex document to update.
     * @param metadata the metadata of the annex.
     * @param content  the content of the annex.
     * @param versionType  the version type to be created
     * @param comment  the comment of the update, optional.
     * @return the updated annex document.
     */
    Annex updateAnnex(String id, AnnexMetadata metadata, byte[] content, VersionType versionType, String comment);

    Annex updateMilestoneComments(String id, List<String> milestoneComments, byte[] content, VersionType versionType, String comment);

    Annex updateMilestoneComments(String id, List<String> milestoneComments);

    /**
     * Finds a [Annex] document with the specified characteristics.
     *
     * @param id     the ID of the annex document to retrieve.
     * @param latest retrieves the latest version of the proposal document, when *true*.
     * @return the found annex document.
     */
    Annex findAnnexById(String id, boolean latest);

    /**
     * Deletes an [Annex] document with the specified characteristics.
     *
     * @param id the ID of the annex document to delete.
     */
    void deleteAnnex(String id);

    /**
     * Finds all versions of a [Annex] document with the specified characteristics.
     *
     * @param id           the ID of the Annex document to retrieve.
     * @param fetchContent streams the content
     * @return the list of found Annex document versions or empty.
     */
    List<Annex> findAnnexVersions(String id, boolean fetchContent);

    /**
     * Finds a [Annex] document with the specified characteristics.
     *
     * @param ref the reference metadata of the annex document to retrieve.
     * @return the found annex document.
     */
    @PostAuthorize("hasPermission(returnObject, 'CAN_READ')")
    Annex findAnnexByRef(String ref);
    
    List<Annex> findAllMinorsForIntermediate(String docRef, String curr, String prev, int startIndex, int maxResults);
    
    int findAllMinorsCountForIntermediate(String docRef, String currIntVersion, String prevIntVersion);
    
    Integer findAllMajorsCount(String docRef);
    
    List<Annex> findAllMajors(String docRef, int startIndex, int maxResult);
    
    List<Annex> findRecentMinorVersions(String documentId, String documentRef, int startIndex, int maxResults);
    
    Integer findRecentMinorVersionsCount(String documentId, String documentRef);
}
