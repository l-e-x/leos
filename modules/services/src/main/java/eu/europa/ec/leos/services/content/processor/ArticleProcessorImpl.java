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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlHelper;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArticleProcessorImpl implements ArticleProcessor {

    @Autowired
    private XmlContentProcessor xmlContentProcessor;
    
    @Autowired
    private NumberProcessor articleNumberProcessor;

    @Override
    public String getArticleTemplate() {
        return XmlHelper.getArticleTemplate("#", "Article heading...");
    }

    @Override
    public byte[] insertNewArticle(Bill document, String articleId, boolean before) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(articleId, "Article id is required.");

        String articleTemplate = getArticleTemplate();

        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final BillMetadata metadata = document.getMetadata().getOrError(() -> "Document metadata is required!");
        final byte[] contentBytes = content.getSource().getByteString().toByteArray();
        byte[] updatedXmlContent = xmlContentProcessor.insertElementByTagNameAndId(contentBytes, articleTemplate,
                "article",
                articleId, before);

        updatedXmlContent = articleNumberProcessor.renumberArticles(updatedXmlContent, metadata.getLanguage());
        updatedXmlContent = xmlContentProcessor.doXMLPostProcessing(updatedXmlContent);

        return updatedXmlContent;
    }

    @Override
    public byte[] deleteArticle(Bill document, String elementId) {

        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Article id is required.");

        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final BillMetadata metadata = document.getMetadata().getOrError(() -> "Document metadata is required!");
        final byte[] contentBytes = content.getSource().getByteString().toByteArray();
        byte[] updatedXmlContent = xmlContentProcessor.deleteElementByTagNameAndId(contentBytes, "article", elementId);
        updatedXmlContent = articleNumberProcessor.renumberArticles(updatedXmlContent, metadata.getLanguage());
        updatedXmlContent = xmlContentProcessor.doXMLPostProcessing(updatedXmlContent);
        return updatedXmlContent;
    }
}
