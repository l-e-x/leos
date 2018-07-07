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
package eu.europa.ec.leos.services.importoj;

import eu.europa.ec.leos.integration.ExternalDocumentProvider;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class ImportServiceImpl implements ImportService {

    private ExternalDocumentProvider externalDocumentProvider;
    private ConversionHelper conversionHelper;
    private XmlContentProcessor xmlContentProcessor;

    @Autowired
    ImportServiceImpl(ExternalDocumentProvider externalDocumentProvider, ConversionHelper conversionHelper, XmlContentProcessor xmlContentProcessor){
        this.externalDocumentProvider = externalDocumentProvider;
        this.conversionHelper = conversionHelper;
        this.xmlContentProcessor = xmlContentProcessor;
    }

    @Override
    public String getFormexDocument(String type, int year, int number) {
        String formexDocument=null;
        try {
            formexDocument = externalDocumentProvider.getFormexDocument(type, year, number);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get the document in formex format", e);
        }
        return formexDocument;
    }

    @Override
    @Cacheable(value="aknCache", cacheManager = "cacheManager")
    public String getAknDocument(String type, int year, int number) {
        String aknDocument = null;
        try {
            aknDocument = conversionHelper.convertFormexToAKN(getFormexDocument(type, year, number));
        } catch (Exception e) {
            throw new RuntimeException("Unable to convert to AKN", e);
        }
        return aknDocument;
    }

    @Override
    public byte[] insertSelectedElements(byte[] documentContent, byte[] importedContent, List<String> elementIds, String language) {
        for (String id : elementIds) {
            String[] element = xmlContentProcessor.getElementById(importedContent, id);
            
            // Get id of the last element in the document
            String xPath = "//" + element[0] + "[last()]";
            String elementId = xmlContentProcessor.getElementIdByPath(documentContent, xPath);
            
            if (elementId != null) {
                // do pre-processing on the selected elements
                String updatedElement = xmlContentProcessor.doImportedElementPreProcessing(element[1]);
                if (updatedElement != null) {
                    // insert selected element to the document
                    documentContent = xmlContentProcessor.insertElementByTagNameAndId(documentContent, updatedElement,
                            element[0],
                            elementId, false);
                }
            } else {
                // TODO:handle case when no desired element exists in the document.
            }
        }
        // renumber
        documentContent = xmlContentProcessor.renumberArticles(xmlContentProcessor.renumberRecitals(documentContent), language);
        
        return documentContent;
    }
}
