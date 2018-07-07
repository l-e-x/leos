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
package eu.europa.ec.leos.repository.store

import eu.europa.ec.leos.domain.common.LeosAuthority
import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Workspace Repository implementation.
 *
 * @constructor Creates a specific Workspace Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class WorkspaceRepositoryImpl(
        private val leosRepository: LeosRepository
) : WorkspaceRepository {

    companion object : KLogging()

    override fun <D : LeosDocument> findDocumentsByParentPath(path: String, type: Class<out D>): List<D> {
        logger.debug{ "Finding document by parent path... [path=$path, type=${type.simpleName}]" }
        return leosRepository.findDocumentsByParentPath(path, type.kotlin)
    }

    override fun <T : LeosDocument> findDocumentById(id: String, type: Class<out T>, latest: Boolean): T {
        logger.debug { "Finding document by ID... [id=$id, type=${type.simpleName}, latest=$latest]" }
        return leosRepository.findDocumentById(id, type.kotlin, latest)
    }

    override fun <D : XmlDocument> updateDocumentCollaborators(id: String, collaborators: Map<String, LeosAuthority>, type: Class<out D>): D {
        logger.debug { "Updating document collaborators... [id=$id, collaborators=$collaborators]" }
        return leosRepository.updateDocument(id, collaborators, type.kotlin)
    }
}
