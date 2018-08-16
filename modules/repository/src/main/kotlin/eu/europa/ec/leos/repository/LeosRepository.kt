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
package eu.europa.ec.leos.repository

import eu.europa.ec.leos.domain.common.LeosAuthority
import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosMetadata
import eu.europa.ec.leos.domain.document.LeosPackage
import kotlin.reflect.KClass

/**
 * LEOS Repository *generic* interface.
 *
 * Represents collections of *generic* document or package entities, independently of how they are stored.
 * Provides *generic* methods to persist and retrieve entity related data to and from a storage system.
 * Allows CRUD operations based on strongly typed Business Entities: [LeosDocument], [LeosMetadata] and [LeosPackage].
 */
interface LeosRepository {

    /**
     * Creates a document from a given template and with the specified characteristics.
     *
     * @param D the document type
     * @param M the metadata type
     * @param templateId the ID of the template for the document.
     * @param path the path where to create the document.
     * @param name the name of the document.
     * @param metadata the metadata of the document.
     * @param type the type class of the document.
     * @return the created document.
     */
    fun <D : LeosDocument, M : LeosMetadata> createDocument(templateId: String, path: String, name: String, metadata: M, type: KClass<out D>): D

    /**
     * Updates a document with the given metadata.
     *
     * @param D the document type
     * @param M the metadata type
     * @param id the ID of the document to update.
     * @param metadata the metadata of the document.
     * @param type the type class of the document.
     * @return the updated document.
     */
    fun <D : LeosDocument, M : LeosMetadata> updateDocument(id: String, metadata: M, type: KClass<out D>): D

    /**
     * Updates a document with the given content.
     *
     * @param D the document type
     * @param id the ID of the document to update.
     * @param content the content of the document.
     * @param major creates a *major version* of the document, when *true*.
     * @param comment the comment of the update, optional.
     * @param type the type class of the document.
     * @return the updated document.
     */
    fun <D : LeosDocument> updateDocument(id: String, content: ByteArray, major: Boolean, comment: String?, type: KClass<out D>): D

    /**
     * Updates a document with the given metadata and content.
     *
     * @param D the document type
     * @param M the metadata type
     * @param id the ID of the document to update.
     * @param metadata the metadata of the document.
     * @param content the content of the document.
     * @param major creates a *major version* of the document, when *true*.
     * @param comment the comment of the update, optional.
     * @param type the type class of the document.
     * @return the updated document.
     */
    fun <D : LeosDocument, M : LeosMetadata> updateDocument(id: String, metadata: M, content: ByteArray, major: Boolean, comment: String?, type: KClass<out D>): D

    /**
     * Updates a document with the given collaborators.
     *
     * @param D the document type
     * @param id the ID of the document to update.
     * @param collaborators the map of users to authorities.
     * @param type the type class of the document.
     * @return the updated document.
     */
    fun <D : LeosDocument> updateDocument(id: String, collaborators: Map<String, LeosAuthority>, type: KClass<out D>): D

    /**
     * Finds a document with the specified characteristics.
     *
     * @param D the document type
     * @param id the ID of the document to retrieve.
     * @param type the type class of the document.
     * @param latest retrieves the *latest version* of the document, when *true*.
     * @return the found document.
     */
    fun <D : LeosDocument> findDocumentById(id: String, type: KClass<out D>, latest: Boolean): D

    /**
     * Finds a document with the specified characteristics.
     *
     * @param D the document type
     * @param path the path where to find the document.
     * @param name the name of the document to retrieve.
     * @param type the type class of the document.
     * @return the found document.
     */
    fun <D : LeosDocument> findDocumentByParentPath(path: String, name: String, type: KClass<out D>): D

    /**
     * Finds documents with the specified characteristics.
     *
     * @param D the document type
     * @param path the path where to find the document.
     * @param type the type class of the document.
     * @return the list of found documents or empty.
     */
    fun <D : LeosDocument> findDocumentsByParentPath(path: String, type: KClass<out D>, descendants: Boolean, fetchContent: Boolean): List<D>

    /**
     * Finds all versions of a document with the specified characteristics.
     *
     * @param D the document type
     * @param id the ID of the document to retrieve.
     * @param type the type class of the document.
     * @return the list of found document versions or empty.
     */
    fun <D : LeosDocument> findDocumentVersionsById(id: String, type: KClass<out D>, fetchContent: Boolean): List<D>

    /**
     * Deletes a document with the specified characteristics.
     *
     * @param id the ID of the document to delete.
     */
    fun deleteDocumentById(id: String): Unit

    /**
     * Creates a [LeosPackage] with the specified characteristics.
     *
     * @param path the path where to create the package.
     * @param name the name of the package.
     * @return the created package.
     */
    fun createPackage(path: String, name: String): LeosPackage

    /**
     * Deletes a [LeosPackage] with the specified characteristics.
     *
     * @param path the path of the package to be deleted.
     */
    fun deletePackage(path: String): Unit

    /**
     * Finds a [LeosPackage] with the specified characteristics.
     *
     * @param documentId the ID of a document inside the package.
     * @return the found package.
     */
    fun findPackageByDocumentId(documentId: String): LeosPackage
}
