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

import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.model.web.status.StatusUpdateSuccessResponse;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.MetadataService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationStatusException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
import eu.europa.ec.leos.annotate.websockets.MessageBroker;
//import eu.europa.ec.leos.annotate.websockets.MessageBroker;
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
import java.util.List;

@RestController
@RequestMapping("/api")
public class StatusApiController {

    private static final Logger LOG = LoggerFactory.getLogger(StatusApiController.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private MetadataService metadataService;

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
     * Endpoint for updating the response status of annotations matching given criteria
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param uri (request parameter)
     *        URI of the annotations' document
     * @param group (request parameter)
     *        group in which the annotations are published
     * @param responseStatus (request parameter)
     *        new response status to be assigned to the matching annotations
     * @param metadata (request body)
     *        map (string/string) of further metadata that the annotations to be updated must have at least
     *
     * @return
     * in case of success: HTTP status 200, JSON based response success response ({@link StatusUpdateSuccessResponse})
     * in case of failure due to missing permissions or other unfulfilled conditions: HTTP status 404 with error description
     * in case of other failures: HTTP status 400, JSON based response with error description
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateAnnotationStatus(final HttpServletRequest request, final HttpServletResponse response,
            final StatusUpdateRequest updateRequest,
            @RequestBody final SimpleMetadata metadataToMatch)
            throws IOException, ServletException {

        LOG.debug("Received request to update metadata status for annotations");

        String errorMsg = "";
        HttpStatus httpStatusToSend;
        updateRequest.setMetadataToMatch(metadataToMatch); // note: even if the expected parameters were not given, a non-null object is given!

        try {
            final UserInformation userInfo = authUser.getUserInfo();
            final List<Long> metadataIds = metadataService.updateMetadata(updateRequest, userInfo);

            // we send an update for all annotations associated to this metadata
            final List<String> annotIds = annotService.getAnnotationIdsOfMetadata(metadataIds);
            final String header = request.getHeader("x-client-id");

            // publish changes via websockets
            annotIds.stream().forEach(annotId -> messageBroker.publish(annotId, MessageBroker.ACTION.UPDATE, header));

            LOG.debug("Annotation metadata status update successful, return Http status 200");
            return new ResponseEntity<Object>(new StatusUpdateSuccessResponse(), HttpStatus.OK);

        } catch (CannotUpdateAnnotationStatusException | MissingPermissionException e) {

            httpStatusToSend = HttpStatus.NOT_FOUND;
            LOG.error("The annotation/metadata status could not be updated", e);
            errorMsg = e.getMessage();

        } catch (Exception e) {

            httpStatusToSend = HttpStatus.BAD_REQUEST;
            LOG.error("Error while trying to update status of annotations", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("There was a problem while updating annotation status via metadata, return and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotation status could not be updated: " + errorMsg), httpStatusToSend);
    }

}
