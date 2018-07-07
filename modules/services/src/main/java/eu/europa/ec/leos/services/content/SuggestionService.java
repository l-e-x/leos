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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface SuggestionService {

    /** Returns the suggestion with the given id. If suggestionId is not given, a suggestion structure<popup><p></p></popup> is returned with content of original Id
     * @param document The document to read
     * @param  originalElementId containing element
     * @param suggestionId  The id of suggestion to be fetched. If null, a new suggestion is created and returned
     * @return The xml string representation of the suggestion
     */
    String getSuggestion(LeosDocument document, @Nonnull String originalElementId, @Nullable String suggestionId);

    /** The suggestion is saved with the given id. And saves the document in repository
     * @param document The document to update
     * @param originalElementId  The id of the element inside which suggestion to be saved
     * @param suggestionId  The id of the suggestion to be inserted/updated
     * @param newSuggestedContent text of the content in format <popup><p></p></popup>
     * @return The updated document
     */
    LeosDocument saveSuggestion(LeosDocument document, @Nonnull String originalElementId, @Nonnull String suggestionId, String newSuggestedContent);
}
