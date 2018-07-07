/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.content;

import eu.europa.ec.leos.model.content.LeosDocument;

public interface ElementService {
    /**
     * Retrieves the element from the given document
     * @param document The document containing the article
     * @param elementId The  id of element
     * @return the xml string representation of the element
     */
    String getElement(LeosDocument document, String elementName, String elementId);

    /**
     * Saves the new elemenContent of an existing element to the given document
     * or deletes the element if the given elementContent is null
     * @param document The document to update
     * @param userLogin The login name of the user trying to save the element
     * @param elementContent The new article content, or null to delete the element
     * @param elementName The element Tag Name
     * @param elementId The id of the element
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument saveElement(LeosDocument document, String userLogin, String elementContent, String elementName, String elementId);
    /**
     * Deletes an element with the given id and saves the document.
     * @param document The document to update
     * @param userlogin The login name of the user trying to delete the element
     * @param elementId The id of the element which is to be deleted.
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument deleteElement(LeosDocument document, String userlogin, String elementId, String elementType);
}