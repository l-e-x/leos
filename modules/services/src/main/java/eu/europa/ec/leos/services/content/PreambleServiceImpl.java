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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.support.xml.XmlContentProcessor;

@Service
public class PreambleServiceImpl implements PreambleService {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private XmlContentProcessor xmlContentProcessor;

    private static final Logger LOG = LoggerFactory.getLogger(PreambleServiceImpl.class);

    @Override
    public String getCitations(LeosDocument document, String citationsId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(citationsId, "Citations Id is required.");

        String citations;
        try {
            citations = xmlContentProcessor.getElementByNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    "citations", citationsId);
        } catch (IOException e) {
            LOG.error("Unable to retrieve citations", e);
            throw new RuntimeException("Unable to retrieve citations.", e);
        }
        
        return citations;    
    }

    @Override
    public LeosDocument saveCitations(LeosDocument document, String newCitations, String citationsId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(citationsId, "Citations Id is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    newCitations, "citations", citationsId);

            // save document into repository
            document = documentService.updateDocumentContent(document.getLeosId(), null, updatedXmlContent, "operation.citations.updated");
        } catch (IOException e) {
            LOG.error("Unable to save citations", e);
            throw new RuntimeException("Unable to save the citations.", e);
        }
        return document;
    }
   
    @Override
    public String getRecitals(LeosDocument document, String recitalsId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(recitalsId, "recitals Id is required.");

        String recitals;
        try {
            recitals = xmlContentProcessor.getElementByNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    "recitals", recitalsId);
        } catch (IOException e) {
            LOG.error("Unable to retrieve recitals", e);
            throw new RuntimeException("Unable to retrieve recitals.", e);
        }
        
        return recitals;    
    }

    @Override
    public LeosDocument saveRecitals(LeosDocument document, String newRecitals, String recitalsId) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(recitalsId, "recitalsId is required.");

        // merge the updated content with the actual document and return updated document
        byte[] updatedXmlContent;
        try {
            updatedXmlContent = xmlContentProcessor.replaceElementByTagNameAndId(IOUtils.toByteArray(document.getContentStream()),
                    newRecitals, "recitals", recitalsId);

            // save document into repository
            document = documentService.updateDocumentContent(document.getLeosId(), null, updatedXmlContent,"operation.recitals.updated");
        } catch (IOException e) {
            LOG.error("Unable to save recitals", e);
            throw new RuntimeException("Unable to save the recitals.",e);
        }
        return document;
    }
}
