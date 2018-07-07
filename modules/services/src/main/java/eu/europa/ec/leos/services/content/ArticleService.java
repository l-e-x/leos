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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ArticleService {

    /**
     * Returns the new article template to be inserted with a random article id
     * @return the empty article template
     */
    @Nonnull
    String getArticleTemplate();

    /**
     * Retrieves the article from the given document
     * @param document The document containing the article
     * @param articleId The article id
     * @return the xml string representation of the article
     */
    @Nullable
    String getArticle(LeosDocument document, String articleId);

    /**
     * Saves the new article content of an existing article to the given document 
     * or deletes the article if the given article is null
     * @param document The document to update
     * @param sessionId The login name of the user trying to save the article
     * @param article The new article content, or null to delete the article
     * @param articleId The id of the article
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument saveArticle(LeosDocument document, String userLoginName, @Nullable String article, String articleId);

    /**
     * Inserts a new article before or after the article with the given id. And saved the document
     * @param document The document to update
     * @param userLoginName The login name of the user trying to insert the article
     * @param articleId  The id of the article before or after the new article
     * @param before true if the new article needs to be inserted before the given article, false if it needs to be inserted after.
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument insertNewArticle(LeosDocument document, String userLoginName, String articleId, boolean before);

    /**
     * Deletes an article with the given article id and saves the document.
     * @param document The document to update
     * @param userLoginName The login name of the user trying to delete the article
     * @param articleId The id of the article which is to be deleted.
     * @return The updated document or throws a LeosDocumentNotLockedByCurrentUserException if the given user doesn't have a lock on the document
     */
    LeosDocument deleteArticle(LeosDocument document, String userLoginName, String articleId);

}