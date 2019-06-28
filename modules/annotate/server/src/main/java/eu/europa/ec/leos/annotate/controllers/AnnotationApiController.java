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

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.annotate.aspects.NoAuthAnnotation;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchCountOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.IncomingSearchOptions;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.*;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
import eu.europa.ec.leos.annotate.websockets.MessageBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AnnotationApiController {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationApiController.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private AnnotationConversionService conversionService;

    @Autowired
    private AuthenticatedUserStore authUser;

    @Autowired
    private MessageBroker messageBroker;

    // -------------------------------------
    // API endpoints
    // -------------------------------------

    /**
     * Endpoint for adding a new annotation
     *
     * @param request Incoming request, containing annotation to be added as JSON body
     * @param response Outgoing response, containing persisted annotation as JSON body
     * @param jsonAnnotation JSON annotation metadata ({@link JsonAnnotation}), extracted from request body
     *
     * @return
     * in case of success: HTTP status 200, JSON based response containing received annotation with some properties updated
     * in case of failure: HTTP status 400, JSON based response with error description
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> addAnnotation(final HttpServletRequest request, final HttpServletResponse response,
            @RequestBody final JsonAnnotation jsonAnnotation)
            throws IOException, ServletException {

        LOG.debug("Received request to add new annotation");

        String errorMsg = "";

        try {
            final UserInformation userInfo = authUser.getUserInfo();
            final JsonAnnotation myResponseJson = annotService.createAnnotation(jsonAnnotation, userInfo);
            messageBroker.publish(myResponseJson.getId(), MessageBroker.ACTION.CREATE, request.getHeader("x-client-id"));

            LOG.debug("Annotation was saved, return Http status 200 and annotation metadata; annotation id: '{}'", myResponseJson.getId());
            return new ResponseEntity<Object>(myResponseJson, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while creating annotation", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("Annotation was not saved, return Http status 400 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotation could not be created: " + errorMsg), HttpStatus.BAD_REQUEST);
    }

    /**
     * Endpoint for retrieving an annotation with a given ID
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param annotationId Id of the wanted annotation; contained in request parameter
     *
     * @return
     * in case of success: HTTP status 200, JSON based response containing found annotation
     * in case of failure: HTTP status 404, JSON based response with error description (e.g. if annotation not found or user may not view it)
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getAnnotation(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable("id") final String annotationId)
            throws IOException, ServletException {

        LOG.debug("Received request to retrieve existing annotation");

        String errorMsg = "";

        try {
            final UserInformation userInfo = authUser.getUserInfo();
            final Annotation ann = annotService.findAnnotationById(annotationId, userInfo.getLogin());
            if (ann == null) {
                throw new Exception("Annotation '" + annotationId + "' not found.");
            }
            final JsonAnnotation foundAnn = conversionService.convertToJsonAnnotation(ann, userInfo);

            LOG.debug("Annotation found, return Http status 200 and annotation metadata; annotation id: '{}'", annotationId);
            return new ResponseEntity<Object>(foundAnn, HttpStatus.OK);

        } catch (MissingPermissionException mpe) {
            errorMsg = mpe.getMessage();

        } catch (Exception e) {
            LOG.error("Error while retrieving annotation", e);
            errorMsg = "The annotation '" + annotationId + "' could not be found: " + e.getMessage();
        }

        LOG.warn("Annotation could not be found or error occured, return Http status 404 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse(errorMsg), HttpStatus.NOT_FOUND);
    }

    /**
     * Endpoint for updating an existing annotation with a given ID
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param annotationId Id of the annotation to be updated; contained in request parameter
     *
     * @return
     * in case of success: HTTP status 200, JSON based response containing updated annotation
     * in case of failure:
     * - HTTP status 400, JSON based response with error description (e.g. if annotation data incomplete, annotation has SENT status or other problem during update)
     * - HTTP status 404, JSON based response with error description (e.g. if annotation not found or user may not update it)
     * - HTTP status 500, JSON based response with error description for any unforeseen error
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateAnnotation(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable("id") final String annotationId,
            @RequestBody final JsonAnnotation jsonAnnotation)
            throws IOException, ServletException {

        LOG.debug("Received request to update an existing annotation");

        String errorMsg = "";
        HttpStatus httpStatusToSent;

        try {
            final JsonAnnotation myResponseJson = annotService.updateAnnotation(annotationId, jsonAnnotation, authUser.getUserInfo());
            messageBroker.publish(myResponseJson.getId(), MessageBroker.ACTION.UPDATE, request.getHeader("x-client-id"));

            LOG.debug("Annotation updated, return Http status 200 and annotation metadata; annotation id: '{}'", annotationId);
            return new ResponseEntity<Object>(myResponseJson, HttpStatus.OK);

        } catch (MissingPermissionException | CannotUpdateAnnotationException e) {

            httpStatusToSent = HttpStatus.NOT_FOUND;
            LOG.error("Error while updating annotation", e);
            errorMsg = e.getMessage();

        } catch (CannotUpdateSentAnnotationException e) {

            httpStatusToSent = HttpStatus.BAD_REQUEST;
            LOG.error("Error: cannot update SENT annotation", e);
            errorMsg = e.getMessage();

        } catch (Exception e) {

            httpStatusToSent = HttpStatus.INTERNAL_SERVER_ERROR;
            LOG.error("Unexpected error while updating annotation", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("Annotation could not be updated, return Http status " + httpStatusToSent.value() + " and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotation '" + annotationId + "' could not be updated: " + errorMsg),
                httpStatusToSent);
    }

    /**
     * Endpoint for deleting an annotation with a given ID
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param annotationId Id of the annotation to be deleted; contained in request parameter
     *
     * @return
     * in case of success: HTTP status 200, JSON based response containing success information and ID of the deleted annotation
     * in case of failure:
     * - HTTP status 400, JSON based response with error description (e.g. when annotation has SENT status and cannot be deleted) 
     * - HTTP status 404, JSON based response with error description (e.g. if annotation not found)
     * - HTTP status 500, JSON based response with error description for any unforeseen error
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> deleteAnnotation(final HttpServletRequest request, final HttpServletResponse response,
            @PathVariable("id") final String annotationId)
            throws IOException, ServletException {

        LOG.debug("Received request to delete an existing annotation");

        String errorMsg = "";
        HttpStatus statusToReturn = HttpStatus.OK;

        try {
            annotService.deleteAnnotationById(annotationId, authUser.getUserInfo());
            messageBroker.publish(annotationId, MessageBroker.ACTION.DELETE, request.getHeader("x-client-id"));

            LOG.info("Annotation deleted, return Http status 200 and annotation id: '{}'", annotationId);
            return new ResponseEntity<Object>(new JsonDeleteSuccessResponse(annotationId), HttpStatus.OK);

        } catch (CannotDeleteAnnotationException cdae) {

            LOG.error("Error while deleting annotation", cdae);
            errorMsg = cdae.getMessage();
            statusToReturn = HttpStatus.NOT_FOUND;

        } catch (CannotDeleteSentAnnotationException cdsae) {

            LOG.error("Error: trying to delete SENT annotation");
            errorMsg = cdsae.getMessage();
            statusToReturn = HttpStatus.BAD_REQUEST;

        } catch (Exception e) {

            LOG.error("Unexpected error while deleting annotation", e);
            errorMsg = e.getMessage();
            statusToReturn = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        LOG.warn("Annotation could not be deleted, return Http status {} and failure notice", statusToReturn.value());
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotation could not be deleted: " + errorMsg), statusToReturn);
    }

    /**
     * Endpoint for deleting a whole set of annotation with given IDs
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param body the message body contains an array "ids" that contains the IDs of all annotations
     *             that should be deleted
     *
     * @return
     * in case of success: HTTP status 200, JSON based response containing success information
     * in case of failure:
     * - HTTP status 500, JSON based response with error description for any unforeseen error
     * 
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> bulkDeleteAnnotations(final HttpServletRequest request, final HttpServletResponse response,
            @RequestBody final JsonIdList annotationList)
            throws IOException, ServletException {

        LOG.debug("Received request to do bulk deletion of annotations");

        String errorMsg = "";

        try {
            final List<String> deleted = annotService.deleteAnnotationsById(annotationList.getIds(), authUser.getUserInfo());
            for (final String annotationId : deleted) {
                messageBroker.publish(annotationId, MessageBroker.ACTION.DELETE, request.getHeader("x-client-id"));
            }

            LOG.info("Annotations deleted, return Http status 200 and success");
            return new ResponseEntity<Object>(new JsonBulkDeleteSuccessResponse(), HttpStatus.OK);

        } catch (Exception e) {

            LOG.error("Unexpected error while deleting annotation", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("Annotations could not all be deleted, return Http status {} and failure notice", HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotations could not all be deleted: " + errorMsg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Endpoint for searching for annotations matching given criteria
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param _separate_replies (request parameter)
     *        search flag indicating whether replies should be mixed with
     *        annotations or be returned as separate group; default: {@value #DEFAULT_SEARCH_SEARCH_REPLIES}
     * @param limit (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        the maximum number of annotations to return; default: {@value #DEFAULT_SEARCH_LIMIT}
     * @param offset (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        minimum number of initial annotations to skip; default: {@value #DEFAULT_SEARCH_OFFSET}
     * @param sort (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        field by which annotations should be sorted; default: {@value #DEFAULT_SEARCH_SORT}
     * @param order (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        order in which the results should be sorted; default: {@value #DEFAULT_SEARCH_ORDER}; allows "asc" and "desc"
     * @param uri (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        URI to be used for the search - usually the URL of the annotated page
     * @param url (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        URL of the annotated page; alias for URI
     * @param user (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        limit the results to annotations made by the specified user (login)
     * @param group (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        limit the results to annotations made in the specified group
     * @param tag (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        limit the results to annotations tagged with the specified value - currently ignored
     * @param any (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        limit the results to annotations in which one of a number of common fields contain the passed value - currently ignored
     * @param metadatasets (request parameter, wrapped in {@link IncomingSearchOptions} object) 
     *        array of {@link SimpleMetadata} maps of metadata to be matched; logical OR matching applied
     * @param status (request parameter, wrapped in {@link IncomingSearchOptions} object)
     *        list of {@link AnnotationStatus} items to be matched, wrapped as a {@link JsonAnnotationStatuses}) 
     *
     * @return
     * in case of success: HTTP status 200, JSON based response containing search results
     * in case of failure: HTTP status 400, JSON based response with error description (e.g. if search failed)
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/search", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> searchAnnotations(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam(value = "_separate_replies", defaultValue = AnnotationSearchOptions.DEFAULT_SEARCH_SEPARATE_REPLIES) final boolean separate_replies,
            final IncomingSearchOptions incomingOptions)
            throws IOException, ServletException {

        // note: _separate_replies needs to remain separate request parameter,
        // as it cannot be mapped directly due to "_" prefix
        LOG.debug("Received request to search for annotations");

        // note the IncomingSearchOptions object is never null, as at least its default values are set

        String errorMsg = "";

        try {
            // convert received parameters into internal object, which contains some more logic,
            // e.g. validity checks
            final AnnotationSearchOptions options = AnnotationSearchOptions.fromIncomingSearchOptions(incomingOptions, separate_replies);
            final UserInformation userInfo = authUser.getUserInfo();
            final AnnotationSearchResult searchResult = annotService.searchAnnotations(options, userInfo);
            List<Annotation> replies = null;
            if (searchResult != null && !searchResult.isEmpty()) {
                replies = annotService.searchRepliesForAnnotations(searchResult, options, userInfo);
            }
            final JsonSearchResult result = conversionService.convertToJsonSearchResult(searchResult, replies, options, userInfo);

            LOG.debug("Annotation search successful, return Http status 200 and result: '{}'", (result == null ? "0" : result.getTotal()));
            return new ResponseEntity<Object>(result, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while searching for annotations", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("There was a problem during annotation search, return Http status 400 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotations could not be searched for: " + errorMsg), HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/count", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> countAnnotations(final HttpServletRequest request, final HttpServletResponse response,
            final AnnotationSearchCountOptions options)
            throws IOException, ServletException {

        LOG.debug("Received request to search for annotations");

        String errorMsg = "";

        try {
            final UserInformation userInfo = authUser.getUserInfo();

            // at least during test scenarios, we experienced problems when sending JSON metadata
            // with only one entry - therefore, we had encoded the curly brackets URL-conform,
            // and have to decode this again here
            options.decodeEscapedBrackets();
            final int resultVal = annotService.getAnnotationsCount(options, userInfo);

            LOG.debug("Annotation counting successful, return Http status 200 and result: '{}'", resultVal);
            return new ResponseEntity<Object>(new JsonSearchCount(resultVal), HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while searching for annotations", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("There was a problem during annotation count search, return Http status 400 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotations could not be counted: " + errorMsg), HttpStatus.BAD_REQUEST);
    }

    // configuration of the JSON deserialisation: allowing fields without quotes
    // e.g. used in document/metadata entries
    @NoAuthAnnotation
    @Bean
    public ObjectMapper objectMapper() {

        final Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToEnable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        return builder.build();
    }
}
