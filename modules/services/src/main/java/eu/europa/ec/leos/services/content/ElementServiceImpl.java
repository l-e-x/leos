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
import eu.europa.ec.leos.support.xml.XmlContentProcessor;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ElementServiceImpl implements ElementService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    private static final Logger LOG = LoggerFactory.getLogger(ElementServiceImpl.class);

    @Override
    public String getElement(LeosDocument document, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        String element;
        try {
            element = xmlContentProcessor.getElementByNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    elementName, elementId);
        } catch (IOException e) {
            throw new RuntimeException("Unable to retrieve the " + elementName + ":" + elementId);
        }
        return element;
    }

    @Override
    public LeosDocument saveElement(LeosDocument document, String userLogin, String elementContent, String elementName, String elementId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    elementContent, elementName, elementId);

            // save document into repository
            document = documentService.updateDocumentContent(document.getLeosId(), userLogin, updatedXmlContent, "operation." + elementName + ".updated");
        } catch (IOException e) {
            throw new RuntimeException("Unable to save the " + elementName + ":" + elementId);
        }
        return document;
    }

    @Override
    public LeosDocument deleteElement(LeosDocument document, String userlogin, String elementId, String elementType) {

        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");

        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.deleteElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()), elementType, elementId);
            updatedXmlContent = xmlContentProcessor.renumberArticles(updatedXmlContent, document.getLanguage());
            document = documentService.updateDocumentContent(document.getLeosId(), userlogin, updatedXmlContent,"operation."+ elementType +".deleted");

        } catch (IOException e) {
            throw new RuntimeException("Unable to delete the element.");
        }
        return document;
    }
}
