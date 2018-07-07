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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata

/**
 * Bill Repository interface.
 *
 * Represents collections of *Bill* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [Bill] and [BillMetadata].
 */
interface BillRepository {

    /**
     * Creates a [Bill] document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the bill.
     * @param path the path where to create the bill.
     * @param name the name of the bill.
     * @param metadata the metadata of the bill.
     * @return the created bill document.
     */
    fun createBill(templateId: String, path: String, name: String, metadata: BillMetadata): Bill

    /**
     * Updates a [Bill] document with the given metadata and content.
     *
     * @param id the ID of the bill document to update.
     * @param metadata the metadata of the bill.
     * @param content the content of the bill.
     * @param major creates a *major version* of the bill, when *true*.
     * @param comment the comment of the update, optional.
     * @return the updated bill document.
     */
    fun updateBill(id: String, metadata: BillMetadata, content: ByteArray, major: Boolean, comment: String?): Bill

    /**
     * Finds a [Bill] document with the specified characteristics.
     *
     * @param id the ID of the bill document to retrieve.
     * @param latest retrieves the latest version of the proposal document, when *true*.
     * @return the found bill document.
     */
    fun findBillById(id: String, latest: Boolean): Bill

    /**
     * Finds all versions of a [Bill] document with the specified characteristics.
     *
     * @param id the ID of the bill document to retrieve.
     * @return the list of found bill document versions or empty.
     */
    fun findBillVersions(id: String): List<Bill>

}
