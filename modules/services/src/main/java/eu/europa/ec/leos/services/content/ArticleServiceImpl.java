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
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.support.xml.XmlHelper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ArticleServiceImpl implements ArticleService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    @Override
    public String getArticleTemplate() {
        return XmlHelper.getArticleTemplate("Article #", "Article heading...");

    }

    @Override
    public String getArticle(LeosDocument document, String articleId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(articleId, "Article id is required.");

        String article;
        try {
            article = xmlContentProcessor.getElementByNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    "article", articleId);
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve the article.");
        }
        return article;
    }

    @Override
    public LeosDocument saveArticle(LeosDocument document, String userLogin, String article, String articleId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(articleId, "Article id is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    article, "article", articleId);

            // save document into repository
            document = documentService.updateDocumentContent(document.getLeosId(), userLogin, updatedXmlContent, "operation.article.updated");
        } catch (IOException e) {
            throw new RuntimeException("Unable to save the article.");
        }
        return document;
    }

    @Override
    public LeosDocument deleteArticle(LeosDocument document, String userlogin, String articleId) {

        Validate.notNull(document, "Document is required.");
        Validate.notNull(articleId, "Article id is required.");

        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.deleteElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()), "article", articleId);
            updatedXmlContent = xmlContentProcessor.renumberArticles(updatedXmlContent, document.getLanguage());
            document = documentService.updateDocumentContent(document.getLeosId(), userlogin, updatedXmlContent,"operation.article.deleted");

        } catch (IOException e) {
            throw new RuntimeException("Unable to delete the article.");
        }
        return document;
    }

    @Override
    public LeosDocument insertNewArticle(LeosDocument document, String sessionId, String articleId, boolean before) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(articleId, "Article id is required.");

        String articleTemplate = getArticleTemplate();

        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.insertElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()), articleTemplate,
                    "article",
                    articleId, before);

            updatedXmlContent = xmlContentProcessor.renumberArticles(updatedXmlContent, document.getLanguage());
        } catch (IOException e) {
            throw new RuntimeException("Unable to insert the article.");
        }

        return documentService.updateDocumentContent(document.getLeosId(), sessionId, updatedXmlContent,"operation.article.inserted");

    }
}
