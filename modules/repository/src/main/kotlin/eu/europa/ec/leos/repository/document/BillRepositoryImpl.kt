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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Bill Repository implementation.
 *
 * @constructor Creates a specific Bill Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class BillRepositoryImpl(
        private val leosRepository: LeosRepository
) : BillRepository {

    companion object : KLogging()

    override fun createBill(templateId: String, path: String, name: String, metadata: BillMetadata): Bill {
        logger.debug { "Creating Bill... [template=$templateId, path=$path, name=$name]" }
        return leosRepository.createDocument(templateId, path, name, metadata, Bill::class)
    }

    override fun updateBill(id: String, metadata: BillMetadata, content: ByteArray, major: Boolean, comment: String?): Bill {
        logger.debug { "Updating Bill metadata and content... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, content, major, comment, Bill::class)
    }

    override fun findBillById(id: String, latest: Boolean): Bill {
        logger.debug { "Finding Bill by ID... [id=$id, latest=$latest]" }
        return leosRepository.findDocumentById(id, Bill::class, latest)
    }

    override fun findBillVersions(id: String, fetchContent: Boolean): List<Bill> {
        logger.debug { "Finding Bill versions... [id=$id]" }
        return leosRepository.findDocumentVersionsById(id, Bill::class, fetchContent)
    }

}
