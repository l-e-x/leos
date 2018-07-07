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
package eu.europa.ec.leos.services.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.support.xml.TransformationManager;

@Service
public class FormatterServiceImpl implements FormatterService {

    private static final Logger LOG = LoggerFactory.getLogger(FormatterServiceImpl.class);

    @Autowired
    private TransformationManager transformationManager;

    @Autowired
    private DocumentService documentService;

    @Override
    public void formatToHtml(InputStream inputStream, OutputStream outputStream, String contextPath) {
        LOG.debug("formatToHtml service invoked for Stream");

        try {
            // 1. get seperate parts of doc.
            // 2. transform if required
            String strHtml = transformationManager.toHtmlForPreview(inputStream, contextPath);
            // 3. join it and return

            outputStream.write(strHtml.getBytes(Charset.forName("UTF-8")));
        } catch (IOException e) {
            throw new RuntimeException("Unable to format to HTML");
        }
    }

    
    @Override
    public String formatToHtml(String versionId, String contextPath) {
        LOG.debug("formatToHtml service invoked for version id:{})", versionId);
        
        try {
            // 1. get document in xml format
            LeosDocument leosDocument = documentService.getDocument(null, versionId);
            // 2. get seperate parts of doc..Rigth now working with Legal body only
            // 3. transform if required
            String strHtml = transformationManager.toHtmlForPreview(leosDocument.getContentStream(),contextPath);
            // 4. join it and return

            return strHtml;
        } catch (Exception e) {
            throw new RuntimeException("Unable to format to HTML");
        }
    }
    
    @Override
    public void formatToPdf(InputStream inputStream, OutputStream outputStream, String contextPath) {
        LOG.debug("Creating PDF service invoked for Stream");
        
        try {
            String xhtml = transformationManager.toHtmlForPreview(inputStream,contextPath);

            ITextRenderer iTextRenderer = new ITextRenderer();
            // plugging in custom UserAgentCallback to find CSSIn local directory/authenticated way
            LocalLoadingUserAgent localAgent=new LocalLoadingUserAgent(iTextRenderer.getOutputDevice());
            //Shared context is required for super class of the loading agent.
            localAgent.setSharedContext(iTextRenderer.getSharedContext());
            iTextRenderer.getSharedContext().setUserAgentCallback(localAgent);
            iTextRenderer.setDocumentFromString(xhtml);
            iTextRenderer.layout();

            iTextRenderer.createPDF(outputStream, true);
            LOG.debug("...PDF should be ready now!!!");
            
        } catch (Exception e) {
            LOG.error("Exception generating PDF...", e);
            throw new RuntimeException("PDF generation error!", e);
        }
    }
}
