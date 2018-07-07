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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import org.springframework.security.access.prepost.PreAuthorize;

import javax.annotation.Nonnull;

public interface ArticleProcessor {

    /**
     * Returns the new article template to be inserted with a random article id
     * @return the empty article template
     */
    @Nonnull
    String getArticleTemplate();

    /**
     * Inserts a new article before or after the article with the given id. And saved the document
     * @param document The document to update
     * @param articleId  The id of the article before or after the new article
     * @param before true if the new article needs to be inserted before the given article, false if it needs to be inserted after.
     * @return The updated document
     */
    @PreAuthorize("hasPermission(#document, 'CAN_UPDATE')")
    byte[] insertNewArticle(Bill document, String articleId, boolean before);

    /**
     * delete Article and returns updated Legal Text
     * @param document The document to update
     * @return The updated document
     */
    @PreAuthorize("hasPermission(#document, 'CAN_UPDATE')")
    byte[] deleteArticle(Bill document, String elementId);
}
