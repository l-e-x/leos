/*
 * Copyright 2019 European Commission
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


import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.model.xml.Element;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlHelper;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.*;

@Service
public class BillProcessorImpl implements BillProcessor {

    protected XmlContentProcessor xmlContentProcessor;
    protected NumberProcessor numberProcessor;

    @Autowired
    BillProcessorImpl(XmlContentProcessor xmlContentProcessor,
            NumberProcessor numberProcessor) {
        this.xmlContentProcessor = xmlContentProcessor;
        this.numberProcessor = numberProcessor;
    }

    public byte[] insertNewElement(Bill document, String elementId, boolean before, String tagName) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        final String template;
        byte[] updatedContent;

        switch (tagName) {
            case CITATION:
                template = XmlHelper.getCitationTemplate();
                updatedContent = insertNewElement(document, elementId, before, tagName, template);
                break;
            case RECITAL:
                template = XmlHelper.getRecitalTemplate("#");
                updatedContent = insertNewElement(document, elementId, before, tagName, template);
                updatedContent = numberProcessor.renumberRecitals(updatedContent);
                break;
            case ARTICLE:
                template = XmlHelper.getArticleTemplate("#", "Article heading...");
                updatedContent = insertNewElement(document, elementId, before, tagName, template);
                final BillMetadata metadata = document.getMetadata().getOrError(() -> "Document metadata is required!");
                updatedContent = numberProcessor.renumberArticles(updatedContent, metadata.getLanguage());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }

        updatedContent = xmlContentProcessor.doXMLPostProcessing(updatedContent);
        return updatedContent;
    }

    public byte[] deleteElement(Bill document, String elementId, String tagName, User user) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");
        byte[] updatedContent;
        switch (tagName) {
            case CLAUSE:
            case CITATION:
                updatedContent = deleteElement(document, elementId, tagName);
                break;
            case RECITAL:
                updatedContent = deleteElement(document, elementId, tagName);
                updatedContent = numberProcessor.renumberRecitals(updatedContent);
                break;
            case ARTICLE:
                updatedContent = deleteElement(document, elementId, tagName);
                final BillMetadata metadata = document.getMetadata().getOrError(() -> "Document metadata is required!");
                updatedContent = numberProcessor.renumberArticles(updatedContent, metadata.getLanguage());
                break;
            default:
                throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }

        updatedContent = xmlContentProcessor.doXMLPostProcessing(updatedContent);
        return updatedContent;
    }

    private byte[] insertNewElement(Bill document, String elementId, boolean before, String tagName, String template) {
        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final byte[] contentBytes = content.getSource().getBytes();
        byte[] updatedBytes = xmlContentProcessor.insertElementByTagNameAndId(contentBytes, template, tagName, elementId, before);
        return updatedBytes;
    }

    private byte[] deleteElement(Bill document, String elementId, String tagName) {
        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final byte[] bytes = content.getSource().getBytes();
        byte[] updatedBytes = xmlContentProcessor.deleteElementByTagNameAndId(bytes, tagName, elementId);
        return updatedBytes;
    }

    @Override
    public byte[] mergeElement(Bill document, String elementContent, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementContent, "ElementContent is required.");
        Validate.notNull(elementName, "ElementName is required.");
        Validate.notNull(elementId, "ElementId is required.");

        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final byte[] contentBytes = content.getSource().getBytes();
        byte[] updatedContent = xmlContentProcessor.mergeElement(contentBytes, elementContent, elementName, elementId);
        if (updatedContent != null) {
            final BillMetadata metadata = document.getMetadata().getOrError(() -> "Document metadata is required!");
            updatedContent = numberProcessor.renumberArticles(updatedContent, metadata.getLanguage());
            updatedContent = xmlContentProcessor.doXMLPostProcessing(updatedContent);
        }
        return updatedContent;
    }

    @Override
    public Element getSplittedElement(Bill document, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementName, "ElementName is required.");
        Validate.notNull(elementId, "ElementId is required.");

        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final byte[] contentBytes = content.getSource().getBytes();
        String[] element = xmlContentProcessor.getSplittedElement(contentBytes, elementName, elementId);
        return element != null ? new Element(element[0], element[1], element[2]) : null;
    }

    @Override
    public Element getMergeOnElement(Bill document, String elementContent, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementContent, "ElementContent is required.");
        Validate.notNull(elementName, "ElementName is required.");
        Validate.notNull(elementId, "ElementId is required.");

        final Content content = document.getContent().getOrError(() -> "Document content is required!");
        final byte[] contentBytes = content.getSource().getBytes();
        String[] element = xmlContentProcessor.getMergeOnElement(contentBytes, elementContent, elementName, elementId);
        return element != null ? new Element(element[0], element[1], element[2]) : null;
    }
}
