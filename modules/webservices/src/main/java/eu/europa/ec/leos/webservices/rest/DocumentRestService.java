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
package eu.europa.ec.leos.webservices.rest;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.format.FormatterService;
import eu.europa.ec.leos.support.web.UrlBuilder;

@Component
@Path("/document")
public class DocumentRestService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentRestService.class);
    private static final MediaType HTML_MEDIA_TYPE = MediaType.TEXT_HTML_TYPE;
    private static final MediaType PDF_MEDIA_TYPE = new MediaType("application", "pdf");

    @Autowired
    private FormatterService formatterService;
    
    @Autowired
    private DocumentService docService;
    
    @GET
    @Path("/html/{leosId}")
    public Response getHtmlDocument(
            final @Context HttpServletRequest servletRequest,
            final @PathParam("leosId") String leosId,
            final @QueryParam("rev") String revisionId)
            throws WebApplicationException {
        LOG.debug("Handling HTML document request (leosId={})(revisionId={})...", leosId, revisionId);

        // Using Streaming Output to send document to browser as a
        StreamingOutput htmlStream = new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                try {
                    LeosDocument leosDocument = docService.getDocument(leosId);
                    formatterService.formatToHtml(leosDocument.getContentStream(), outputStream, new UrlBuilder().getWebAppPath(servletRequest));
                    outputStream.flush();
                    outputStream.close();
                }
                catch (Exception e) {
                    LOG.error("REST service error when generating HTML (leosId={})!", leosId);
                    throw new WebApplicationException(e);
                }
            }
        };
        
        try {
            return Response.ok(htmlStream, HTML_MEDIA_TYPE).build();
        } catch (Exception e) {
            LOG.error("REST service error when generating HTML (leosId={})!", leosId);
            throw new WebApplicationException(e);
        }
    }

    @GET
    @Path("/pdf/{leosId}")
    public Response getPdfDocument(
            final @Context HttpServletRequest servletRequest,
            final @PathParam("leosId") String leosId,
            final @QueryParam("rev") String revisionId) throws WebApplicationException {
        LOG.debug("Handling PDF document request (leosId={})(revisionId={})...", leosId, revisionId);

        StreamingOutput pdfStream = new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                try {
                    LeosDocument leosDocument = docService.getDocument(leosId);
                    formatterService.formatToPdf(leosDocument.getContentStream(), outputStream, new UrlBuilder().getWebAppPath(servletRequest));
                    outputStream.flush();
                    outputStream.close();
                }
                catch (Exception e) {
                    LOG.error("REST service error when generating PDF (leosId={})!", leosId);
                    throw new WebApplicationException(e);
                }
            }
        };
        
        try {
            return Response.ok(pdfStream, PDF_MEDIA_TYPE).build();
        } catch (Exception e) {
            LOG.error("REST service error when generating PDF (leosId={})!", leosId);
            throw new WebApplicationException(e);
        }
    }
}
