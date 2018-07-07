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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElementProcessorImpl<T extends XmlDocument> implements ElementProcessor<T> {

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    @Override
    public String getElement(T document, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        String element;
            element = xmlContentProcessor.getElementByNameAndId(document.getContent().get().getSource().getByteString().toByteArray(),
                    elementName, elementId);
        return element;
    }

    @Override
    public byte[] updateElement(T document, String elementContent, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(document.getContent().get().getSource().getByteString().toByteArray(),
                    elementContent, elementName, elementId);
        return updatedXmlContent;
    }

    @Override
    public byte[] deleteElement(T document, String elementId, String elementDype) {

        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        byte[]  updatedXmlContent = xmlContentProcessor.deleteElementByTagNameAndId(document.getContent().get().getSource().getByteString().toByteArray(), elementDype, elementId);
        return updatedXmlContent;
    }
}
