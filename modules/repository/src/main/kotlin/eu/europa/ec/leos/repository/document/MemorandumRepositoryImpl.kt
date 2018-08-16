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
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Memorandum Repository implementation.
 *
 * @constructor Creates a specific Memorandum Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class MemorandumRepositoryImpl(
        private val leosRepository: LeosRepository
) : MemorandumRepository {

    companion object : KLogging()

    override fun createMemorandum(templateId: String, path: String, name: String, metadata: MemorandumMetadata): Memorandum {
        logger.debug { "Creating Memorandum... [template=$templateId, path=$path, name=$name]" }
        return leosRepository.createDocument(templateId, path, name, metadata, Memorandum::class)
    }

    override fun updateMemorandum(id: String, metadata: MemorandumMetadata): Memorandum {
        logger.debug { "Updating Memorandum metadata... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, Memorandum::class)
    }

    override fun updateMemorandum(id: String, content: ByteArray, major: Boolean, comment: String?): Memorandum {
        logger.debug { "Updating Memorandum content... [id=$id]" }
        return leosRepository.updateDocument(id, content, major, comment, Memorandum::class)
    }

    override fun updateMemorandum(id: String, metadata: MemorandumMetadata, content: ByteArray, major: Boolean, comment: String?): Memorandum {
        logger.debug { "Updating Memorandum metadata and content... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, content, major, comment, Memorandum::class)
    }

    override fun findMemorandumById(id: String, latest: Boolean): Memorandum {
        logger.debug { "Finding Memorandum by ID... [id=$id, latest=$latest]" }
        return leosRepository.findDocumentById(id, Memorandum::class, latest)
    }

    override fun findMemorandumVersions(id: String, fetchContent: Boolean): List<Memorandum> {
        logger.debug { "Finding Memorandum versions... [id=$id]" }
        return leosRepository.findDocumentVersionsById(id, Memorandum::class, fetchContent)
    }
}
