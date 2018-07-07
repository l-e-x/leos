/**
 * Copyright 2015 European Commission
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

public interface PreambleService {
    /**
     * Retrieves the Citations from the given document
     * @param document The document containing citations
     * @param citationsId The citations id
     * @return the xml string representation of the citations
     */
    String getCitations(LeosDocument document, String citationsId);

    /**
     * Saves the new citations content of an citations tag to the given document 
     * @param document The document to update
     * @param citations The new content
     * @param citationsId The id of the citations
     * @return The updated document 
     */
    LeosDocument saveCitations(LeosDocument document, String citations, String citationsId);
    /**
     * Retrieves the recitals from the given document
     * @param document The document containing recitals
     * @param recitalsId The citations id
     * @return the xml string representation of the recitals
     */
    String getRecitals(LeosDocument document, String recitalsId);

    /**
     * Saves the new recitals content of an recital tag to the given document 
     * @param document The document to update
     * @param recitals The new content
     * @param recitalsId The id of the citations
     * @return The updated document 
     */
    LeosDocument saveRecitals(LeosDocument document, String recitals, String recitalsId);
}