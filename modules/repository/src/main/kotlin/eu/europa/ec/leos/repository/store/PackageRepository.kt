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
package eu.europa.ec.leos.repository.store

import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosPackage

/**
 * LEOS Package Repository interface.
 *
 * Represents collections of *packages*, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [LeosPackage].
 */
interface PackageRepository {

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

    /**
     * Finds documents with the specified characteristics.
     *
     * @param path the path of the package where to find the documents.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    fun <D : LeosDocument> findDocumentsByPackagePath(path: String, type: Class<out D>, fetchContent: Boolean): List<D>

    /**
     * Finds documents with the specified characteristics.
     *
     * @param path the path of the package where to find the documents
     * @param name the file name of the document to find.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    fun <D : LeosDocument> findDocumentByPackagePathAndName(path: String, name: String, type: Class<out D>): D
}
