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

import eu.europa.ec.leos.domain.common.LeosAuthority
import eu.europa.ec.leos.domain.document.LeosDocument
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument
import org.springframework.security.access.prepost.PostAuthorize

/**
 * Workspace Repository interface.
 *
 * Represents collections of *documents*, with specific methods to retrieve them.
 */
interface WorkspaceRepository {

    /**
     * Finds documents with the specified characteristics.
     *
     * @param D the document type
     * @param path the path where to find the documents.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    fun <D : LeosDocument> findDocumentsByParentPath(path: String, type: Class<out D>, fetchContent: Boolean): List<D>

    // SMELL security should probably be enforced at service level...
    @PostAuthorize("hasPermission(returnObject , 'CAN_READ')")
    fun <T : LeosDocument> findDocumentById(id: String, type: Class<out T>, latest: Boolean): T

    /**
     * Updates the collaborators of the specified document.
     *
     * @param D the document type
     * @param id the ID of the document to update.
     * @param collaborators the map of users to authorities.
     * @param type the type class of the document.
     * @return the updated document.
     */
    fun <D : XmlDocument> updateDocumentCollaborators(id: String, collaborators: Map<String, LeosAuthority>, type: Class<out D>): D

}
