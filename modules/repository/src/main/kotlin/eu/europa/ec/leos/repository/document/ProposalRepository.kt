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

/**
 * Proposal Repository interface.
 *
 * Represents collections of *Proposal* documents, with specific methods to persist and retrieve.
 * Allows CRUD operations based on strongly typed Business Entities: [Proposal] and [ProposalMetadata].
 */
interface ProposalRepository {

    /**
     * Creates a [Proposal] document from a given template and with the specified characteristics.
     *
     * @param templateId the ID of the template for the proposal.
     * @param path the path where to create the proposal.
     * @param name the name of the proposal.
     * @param metadata the metadata of the proposal.
     * @return the created proposal document.
     */
    fun createProposal(templateId: String, path: String, name: String, metadata: ProposalMetadata): Proposal

    /**
     * Updates a [Proposal] document with the given metadata.
     *
     * @param id the ID of the proposal document to update.
     * @param metadata the metadata of the proposal.
     * @return the updated proposal document.
     */
    fun updateProposal(id: String, metadata: ProposalMetadata): Proposal

    /**
     * Updates a [Proposal] document with the given content.
     *
     * @param id the ID of the proposal document to update.
     * @param content the content of the proposal.
     * @return the updated proposal document.
     */
    fun updateProposal(id: String, content: ByteArray): Proposal

    /**
     * Updates a [Proposal] document with the given metadata and content.
     *
     * @param id the ID of the proposal document to update.
     * @param metadata the metadata of the proposal.
     * @param content the content of the proposal.
     * @return the updated proposal document.
     */
    fun updateProposal(id: String, metadata: ProposalMetadata, content: ByteArray): Proposal

    /**
     * Finds a [Proposal] document with the specified characteristics.
     *
     * @param id the ID of the proposal document to retrieve.
     * @param latest retrieves the latest version of the proposal document, when *true*.
     * @return the found proposal document.
     */
    fun findProposalById(id: String, latest: Boolean): Proposal

}
