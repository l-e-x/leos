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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Proposal
import eu.europa.ec.leos.domain.document.LeosMetadata.ProposalMetadata
import eu.europa.ec.leos.repository.LeosRepository
import mu.KLogging
import org.springframework.stereotype.Repository

/**
 * Proposal Repository implementation.
 *
 * @constructor Creates a specific Proposal Repository, injected with a generic LEOS Repository.
 */
@Repository
internal class ProposalRepositoryImpl(
        private val leosRepository: LeosRepository
) : ProposalRepository {

    companion object : KLogging()

    override fun createProposal(templateId: String, path: String, name: String, metadata: ProposalMetadata): Proposal {
        logger.debug { "Creating Proposal... [template=$templateId, path=$path, name=$name]" }
        return leosRepository.createDocument(templateId, path, name, metadata, Proposal::class)
    }

    override fun updateProposal(id: String, metadata: ProposalMetadata): Proposal {
        logger.debug { "Updating Proposal metadata... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, Proposal::class)
    }

    override fun updateProposal(id: String, content: ByteArray): Proposal {
        logger.debug { "Updating Proposal content... [id=$id]" }
        return leosRepository.updateDocument(id, content, false, "Content updated.", Proposal::class)
    }

    override fun updateProposal(id: String, metadata: ProposalMetadata, content: ByteArray): Proposal {
        logger.debug { "Updating Proposal metadata and content... [id=$id]" }
        return leosRepository.updateDocument(id, metadata, content, false, "Metadata updated.", Proposal::class)
    }

    override fun findProposalById(id: String, latest: Boolean): Proposal {
        logger.debug { "Finding Proposal by ID... [id=$id, latest=$latest]" }
        return leosRepository.findDocumentById(id, Proposal::class, latest)
    }

}
