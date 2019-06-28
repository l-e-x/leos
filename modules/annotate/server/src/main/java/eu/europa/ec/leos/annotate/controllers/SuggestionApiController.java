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
package eu.europa.ec.leos.annotate.controllers;

import eu.europa.ec.leos.annotate.websockets.MessageBroker;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSuggestionAcceptSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSuggestionRejectSuccessResponse;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class SuggestionApiController {

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionApiController.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private AuthenticatedUserStore authUser;

    @Autowired
    private MessageBroker messageBroker;
    // -------------------------------------
    // API endpoints
    // -------------------------------------

    /**
     * Endpoint for accepting a suggestion (annotation with specific tag) with a given ID
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param annotationId Id of the suggestion to be accepted; contained in request parameter
     *
     * @return 
     * in case of success: HTTP status 200, JSON based response containing success information and ID of the accepted annotation 
     * in case of failure: HTTP status 400, JSON based response with error description 
     *                     (if referenced annotation is not a suggestion or its response status is SENT)
     * in case of failure: HTTP status 404, JSON based response with error description (e.g. if suggestion not found or user may not accept it)
     * in case of failure: HTTP status 500, JSON based response with error description (unforeseen error)
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}/accept", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> acceptSuggestion(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable("id") final String annotationId)
            throws IOException, ServletException {

        LOG.debug("Received request to accept an existing suggestion");

        String errorMsg = "";
        HttpStatus httpStatusToSent;

        try {
            annotService.acceptSuggestionById(annotationId, authUser.getUserInfo());
            messageBroker.publish(annotationId, MessageBroker.ACTION.DELETE, request.getHeader("x-client-id"));

            LOG.info("Suggestion accepted, return Http status 200 and suggestion (annotation) id: '{}'", annotationId);
            return new ResponseEntity<Object>(new JsonSuggestionAcceptSuccessResponse(annotationId), HttpStatus.OK);

        } catch (MissingPermissionException | CannotAcceptSuggestionException mpcase) {
            httpStatusToSent = HttpStatus.NOT_FOUND;
            errorMsg = mpcase.getMessage();

        } catch (NoSuggestionException | CannotAcceptSentSuggestionException nse) {
            httpStatusToSent = HttpStatus.BAD_REQUEST;
            errorMsg = nse.getMessage();

        } catch (Exception e) {
            LOG.error("Error while accepting suggestion", e);
            errorMsg = "The annotation could not be accepted: " + e.getMessage();
            httpStatusToSent = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        LOG.warn("Suggestion could not be accepted, return Http status {} and failure notice", httpStatusToSent);
        return new ResponseEntity<Object>(new JsonFailureResponse(errorMsg), httpStatusToSent);
    }

    /**
     * Endpoint for rejecting a suggestion (annotation with specific tag) with a given ID
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param annotationId Id of the suggestion to be rejected; contained in request parameter
     *
     * @return 
     * in case of success: HTTP status 200, JSON based response containing success information and ID of the rejected annotation 
     * in case of failure: HTTP status 400, JSON based response with error description 
     *                     (e.g. if referenced annotation is not a suggestion or its response status is SENT)
     * in case of failure: HTTP status 404, JSON based response with error description (e.g. if suggestion not found or user may not reject it)
     * in case of failure: HTTP status 500, JSON based response with error description (unforeseen error)
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}/reject", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> rejectSuggestion(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable("id") final String annotationId)
            throws IOException, ServletException {

        LOG.debug("Received request to reject an existing suggestion");

        String errorMsg = "";
        HttpStatus httpStatusToSent;

        try {
            annotService.rejectSuggestionById(annotationId, authUser.getUserInfo());
            messageBroker.publish(annotationId, MessageBroker.ACTION.DELETE, request.getHeader("x-client-id"));

            LOG.info("Suggestion rejected, return Http status 200 and suggestion (annotation) id: '{}'", annotationId);
            return new ResponseEntity<Object>(new JsonSuggestionRejectSuccessResponse(annotationId), HttpStatus.OK);

        } catch (MissingPermissionException | CannotRejectSuggestionException mpcase) {
            httpStatusToSent = HttpStatus.NOT_FOUND;
            errorMsg = mpcase.getMessage();

        } catch (NoSuggestionException | CannotRejectSentSuggestionException nse) {
            httpStatusToSent = HttpStatus.BAD_REQUEST;
            errorMsg = nse.getMessage();

        } catch (Exception e) {
            LOG.error("Error while rejecting suggestion", e);
            errorMsg = "The annotation could not be rejected: " + e.getMessage();
            httpStatusToSent = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        LOG.warn("Suggestion could not be rejected, return Http status {} and failure notice", httpStatusToSent);
        return new ResponseEntity<Object>(new JsonFailureResponse(errorMsg), httpStatusToSent);
    }
}
