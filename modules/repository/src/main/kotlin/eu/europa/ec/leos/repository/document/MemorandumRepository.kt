/*
 * Copyright 2018 European Commission
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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum
import eu.europa.ec.leos.domain.document.LeosMetadata.MemorandumMetadata

/**
 * Memorandum Repository interface.
 *
 * Represents collections of *Memorandum* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [Memorandum] and [MemorandumMetadata].
 */
interface MemorandumRepository {

    /**
     * Creates a [Memorandum] document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the memorandum.
     * @param path the path where to create the memorandum.
     * @param name the name of the memorandum.
     * @param metadata the metadata of the memorandum.
     * @return the created memorandum document.
     */
    fun createMemorandum(templateId: String, path: String, name: String, metadata: MemorandumMetadata): Memorandum

    /**
     * Updates a [Memorandum] document with the given metadata.
     *
     * @param id the ID of the memorandum document to update.
     * @param metadata the metadata of the memorandum.
     * @return the updated memorandum document.
     */
    fun updateMemorandum(id: String, metadata: MemorandumMetadata): Memorandum

    /**
     * Updates a [Memorandum] document with the given content.
     *
     * @param id the ID of the memorandum document to update.
     * @param content the content of the memorandum.
     * @param major creates a *major version* of the memorandum, when *true*.
     * @param comment the comment of the update, optional.
     * @return the updated memorandum document.
     */
    fun updateMemorandum(id: String, content: ByteArray, major: Boolean, comment: String?): Memorandum

    /**
     * Updates a [Memorandum] document with the given metadata and content.
     *
     * @param id the ID of the memorandum document to update.
     * @param metadata the metadata of the memorandum.
     * @param content the content of the memorandum.
     * @param major creates a *major version* of the memorandum, when *true*.
     * @param comment the comment of the update, optional.
     * @return the updated memorandum document.
     */
    fun updateMemorandum(id: String, metadata: MemorandumMetadata, content: ByteArray, major: Boolean, comment: String?): Memorandum

    /**
     * Finds a [Memorandum] document with the specified characteristics.
     *
     * @param id the ID of the memorandum document to retrieve.
     * @param latest retrieves the latest version of the proposal document, when *true*.
     * @return the found memorandum document.
     */
    fun findMemorandumById(id: String, latest: Boolean): Memorandum

    /**
     * Finds all versions of a [Memorandum] document with the specified characteristics.
     *
     * @param id the ID of the Memorandum document to retrieve.
     * @param  fetchContent streams the content
     * @return the list of found Memorandum document versions or empty.
     */
    fun findMemorandumVersions(id: String, fetchContent: Boolean): List<Memorandum>
}
