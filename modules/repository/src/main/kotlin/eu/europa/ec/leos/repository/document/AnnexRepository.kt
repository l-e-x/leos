/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.repository.document

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata

/**
 * Annex Repository interface.
 *
 * Represents collections of *Annex* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [Annex] and [AnnexMetadata].
 */
interface AnnexRepository {

    /**
     * Creates an [Annex] document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the annex.
     * @param path the path where to create the annex.
     * @param name the name of the annex.
     * @param metadata the metadata of the annex.
     * @return the created annex document.
     */
    fun createAnnex(templateId: String, path: String, name: String, metadata: AnnexMetadata): Annex

    /**
     * Updates an [Annex] document with the given metadata.
     *
     * @param id the ID of the annex document to update.
     * @param metadata the metadata of the annex.
     * @return the updated annex document.
     */
    fun updateAnnex(id: String, metadata: AnnexMetadata): Annex

    /**
     * Updates an [Annex] document with the given content.
     *
     * @param id the ID of the annex document to update.
     * @param content the content of the annex.
     * @param major creates a *major version* of the annex, when *true*.
     * @param comment the comment of the update, optional.
     * @return the updated annex document.
     */
    fun updateAnnex(id: String, content: ByteArray, major: Boolean, comment: String?): Annex

    /**
     * Updates a [Annex] document with the given metadata and content.
     *
     * @param id the ID of the annex document to update.
     * @param metadata the metadata of the annex.
     * @param content the content of the annex.
     * @param major creates a *major version* of the annex, when *true*.
     * @param comment the comment of the update, optional.
     * @return the updated annex document.
     */
    fun updateAnnex(id: String, metadata: AnnexMetadata, content: ByteArray, major: Boolean, comment: String?): Annex

    /**
     * Finds a [Annex] document with the specified characteristics.
     *
     * @param id the ID of the annex document to retrieve.
     * @param latest retrieves the latest version of the proposal document, when *true*.
     * @return the found annex document.
     */
    fun findAnnexById(id: String, latest: Boolean): Annex

    /**
     * Deletes an [Annex] document with the specified characteristics.
     *
     * @param id the ID of the annex document to delete.
     */
    fun deleteAnnex(id: String): Unit
    
    /**
     * Finds all versions of a [Annex] document with the specified characteristics.
     *
     * @param id the ID of the Annex document to retrieve.
     * @return the list of found Annex document versions or empty.
     */
    fun findAnnexVersions(id: String): List<Annex>
}
