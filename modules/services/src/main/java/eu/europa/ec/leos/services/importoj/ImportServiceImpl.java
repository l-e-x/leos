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
package eu.europa.ec.leos.services.importoj;

import eu.europa.ec.leos.integration.ExternalDocumentProvider;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.i18n.MessageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
class ImportServiceImpl implements ImportService {

    private ExternalDocumentProvider externalDocumentProvider;
    private ConversionHelper conversionHelper;
    private XmlContentProcessor xmlContentProcessor;
    private NumberProcessor numberProcessor;
    private static final String RECITAL = "recital";
    private static final String ARTICLE = "article";

    @Autowired
    ImportServiceImpl(ExternalDocumentProvider externalDocumentProvider, ConversionHelper conversionHelper, XmlContentProcessor xmlContentProcessor,
            NumberProcessor numberProcessor) {
        this.externalDocumentProvider = externalDocumentProvider;
        this.conversionHelper = conversionHelper;
        this.xmlContentProcessor = xmlContentProcessor;
        this.numberProcessor = numberProcessor;
    }

    @Autowired
    private MessageHelper messageHelper;

    @Override
    public String getFormexDocument(String type, int year, int number) {
        String formexDocument = null;
        try {
            formexDocument = externalDocumentProvider.getFormexDocument(type, year, number);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get the document in formex format", e);
        }
        return formexDocument;
    }

    @Override
    @Cacheable(value = "aknCache", cacheManager = "cacheManager")
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
            String xPath = "//" + element[1] + "[last()]";
            String elementId = xmlContentProcessor.getElementIdByPath(documentContent, xPath);
            String elementType = element[1];
            if (elementId != null) {
                // do pre-processing on the selected elements
                String updatedElement = xmlContentProcessor.doImportedElementPreProcessing(element[2], elementType);
                if (elementType.equalsIgnoreCase(ARTICLE)) {
                    updatedElement = this.numberProcessor.renumberImportedArticle(updatedElement,language);
                } else if (elementType.equalsIgnoreCase(RECITAL)) {
                    updatedElement = this.numberProcessor.renumberImportedRecital(updatedElement);
                }

                if (updatedElement != null) {
                    // insert selected element to the document
                    documentContent = xmlContentProcessor.insertElementByTagNameAndId(documentContent, updatedElement,
                            element[1],
                            elementId, checkIfLastArticleIsEntryIntoForce(documentContent, element, elementId, language));
                }
            } else {
                // TODO:handle case when no desired element exists in the document.
            }
        }
        // renumber
        documentContent = this.numberProcessor.renumberRecitals(documentContent);
        documentContent = this.numberProcessor.renumberArticles(documentContent, language);
        documentContent = xmlContentProcessor.doXMLPostProcessing(documentContent);
        return documentContent;
    }

    // check if the last article in the document has heading Entry into force, if yes articles imported before EIF article
    private boolean checkIfLastArticleIsEntryIntoForce(byte[] documentContent, String[] element, String elementId,
            String language) {
        boolean isLastElementEIF = false;
        String lastElement = xmlContentProcessor.getElementByNameAndId(documentContent, element[1], elementId);
        String headingElementValue = xmlContentProcessor.getElementValue(lastElement.getBytes(StandardCharsets.UTF_8),
                "//heading[1]", false); // Disable namespace parsing for VTD as xml fragment passed may not contain namespace information
        if (checkIfHeadingIsEntryIntoForce(headingElementValue, language)) {
            isLastElementEIF = true;
        }
        return isLastElementEIF;
    }

    // Gets the heading message from locale
    private boolean checkIfHeadingIsEntryIntoForce(String headingElementValue, String language) {
        boolean isHeadingMatched = false;
        if (headingElementValue != null && !headingElementValue.isEmpty()) {
            isHeadingMatched = messageHelper.getMessage("legaltext.article.entryintoforce.heading").replaceAll("\\h+", "")
                    .equalsIgnoreCase(StringUtils.trimAllWhitespace(headingElementValue.replaceAll("\\h+", "")));
        }
        return isHeadingMatched;
    }
}
