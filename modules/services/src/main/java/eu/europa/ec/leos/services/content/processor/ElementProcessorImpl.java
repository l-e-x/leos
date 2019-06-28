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


import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.model.xml.Element;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ElementProcessorImpl<T extends XmlDocument> implements ElementProcessor<T> {

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    @Override
    public String getElement(T document, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        String element;
        element = xmlContentProcessor.getElementByNameAndId(document.getContent().get().getSource().getBytes(),
                    elementName, elementId);
        return element;
    }

    @Override
    public Element getSiblingElement(T document, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementName, "ElementName id is required.");
        Validate.notNull(elementId, "Element id is required.");

        String[] element = xmlContentProcessor.getSiblingElement(document.getContent().get().getSource().getBytes(),
        		elementName, elementId, Collections.emptyList(), false);
        return element != null ? new Element(element[0], element[1], element[2]) : null;
    }

    @Override
    public Element getChildElement(T document, String elementName, String elementId, List<String> elementTags, int position) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementName, "ElementName id is required.");
        Validate.notNull(elementId, "Element id is required.");

        String[] element = xmlContentProcessor.getChildElement(document.getContent().get().getSource().getBytes(),
        		elementName, elementId, elementTags, position);
        return element != null ? new Element(element[0], element[1], element[2]) : null;
    }

    @Override
    public byte[] updateElement(T document, String elementContent, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(document.getContent().get().getSource().getBytes(),
                    elementContent, elementName, elementId);
        return updatedXmlContent;
    }

    @Override
    public byte[] deleteElement(T document, String elementId, String elementDype) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        byte[] updatedXmlContent = xmlContentProcessor.deleteElementByTagNameAndId(document.getContent().get().getSource().getBytes(),
        		elementDype, elementId);
        return updatedXmlContent;
    }

    @Override
    public byte[] replaceTextInElement(T document, String origText, String newText, String elementId, int startOffset, int endOffset) {
        Validate.notNull(document, "Document is required.");
        Validate.notEmpty(origText, "Orginal Text is required");
        Validate.notNull(elementId, "Element Id is required");
        Validate.notNull(newText, "New Text is required");

        byte[] byteXmlContent = document.getContent().get().getSource().getBytes();
        return xmlContentProcessor.replaceTextInElement(byteXmlContent, origText, newText, elementId, startOffset, endOffset);
    }
}
