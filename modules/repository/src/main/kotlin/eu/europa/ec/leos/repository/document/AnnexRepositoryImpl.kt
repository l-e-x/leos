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
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Annex Repository implementation.
 *
 * @constructor Creates a specific Annex Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class AnnexRepositoryImpl(
        private val leosRepository: LeosRepository
) : AnnexRepository {

    companion object : KLogging()

    override fun createAnnex(templateId: String, path: String, name: String, metadata: AnnexMetadata): Annex {
        logger.debug { "Creating Annex... [template=$templateId, path=$path, name=$name]" }
        return leosRepository.createDocument(templateId, path, name, metadata, Annex::class)
    }

    override fun updateAnnex(id: String, metadata: AnnexMetadata): Annex {
        logger.debug { "Updating Annex metadata... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, Annex::class)
    }

    override fun updateAnnex(id: String, content: ByteArray, major: Boolean, comment: String?): Annex {
        logger.debug { "Updating Annex content... [id=$id]" }
        return leosRepository.updateDocument(id, content, major, comment, Annex::class)
    }

    override fun updateAnnex(id: String, metadata: AnnexMetadata, content: ByteArray, major: Boolean, comment: String?): Annex {
        logger.debug { "Updating Annex metadata and content... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, content, major, comment, Annex::class)
    }

    override fun findAnnexById(id: String, latest: Boolean): Annex {
        logger.debug { "Finding Annex by ID... [id=$id, latest=$latest]" }
        return leosRepository.findDocumentById(id, Annex::class, latest)
    }

    override fun deleteAnnex(id: String) {
        logger.debug { "Deleting Annex... [id=$id]" }
        return leosRepository.deleteDocumentById(id)
    }

    override fun findAnnexVersions(id: String): List<Annex> {
        logger.debug { "Finding Annex versions... [id=$id]" }
        return leosRepository.findDocumentVersionsById(id, Annex::class)
    }
}
