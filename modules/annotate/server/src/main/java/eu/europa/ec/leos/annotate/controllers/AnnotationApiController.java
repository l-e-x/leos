/*
 * Copyright 2018 European Commission
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
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonDeleteSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
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
    private AuthenticationService authService;

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
    public ResponseEntity<Object> addAnnotation(HttpServletRequest request, HttpServletResponse response, @RequestBody JsonAnnotation jsonAnnotation)
            throws IOException, ServletException {

        LOG.debug("Received request to add new annotation");

        String errorMsg = "";

        try {
            JsonAnnotation myResponseJson = annotService.createAnnotation(jsonAnnotation, authService.getAuthenticatedUser().getLogin());

            LOG.debug("Annotation was saved, return Http status 200 and annotation metadata; annotation id: '" + myResponseJson.getId() + "'");
            return new ResponseEntity<Object>(myResponseJson, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while saving annotation", e);
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
    public ResponseEntity<Object> getAnnotation(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String annotationId)
            throws IOException, ServletException {

        LOG.debug("Received request to retrieve existing annotation");

        String errorMsg = "";

        try {
            Annotation ann = annotService.findAnnotationById(annotationId, authService.getAuthenticatedUser().getLogin());
            JsonAnnotation foundAnn = annotService.convertToJsonAnnotation(ann);

            LOG.debug("Annotation found, return Http status 200 and annotation metadata; annotation id: '" + annotationId + "'");
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
     * - HTTP status 400, JSON based response with error description (e.g. if annotation data incomplete or other problem during update) 
     * - HTTP status 404, JSON based response with error description (e.g. if annotation not found or user may not update it)
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateAnnotation(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String annotationId,
            @RequestBody JsonAnnotation jsonAnnotation)
            throws IOException, ServletException {

        LOG.debug("Received request to update an existing annotation");

        String errorMsg = "";
        HttpStatus httpStatusToSent;

        try {
            JsonAnnotation myResponseJson = annotService.updateAnnotation(annotationId, jsonAnnotation, authService.getAuthenticatedUser().getLogin());

            LOG.debug("Annotation updated, return Http status 200 and annotation metadata; annotation id: '" + annotationId + "'");
            return new ResponseEntity<Object>(myResponseJson, HttpStatus.OK);

        } catch (MissingPermissionException | CannotUpdateAnnotationException e) {

            httpStatusToSent = HttpStatus.NOT_FOUND;
            LOG.error("Error while updating annotation", e);
            errorMsg = e.getMessage();

        } catch (Exception e) {

            httpStatusToSent = HttpStatus.BAD_REQUEST;
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
     * in case of failure: HTTP status 404, JSON based response with error description (e.g. if annotation not found)
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/annotations/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> deleteAnnotation(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String annotationId)
            throws IOException, ServletException {

        LOG.debug("Received request to delete an existing annotation");

        String errorMsg = "";

        try {
            annotService.deleteAnnotationById(annotationId, authService.getAuthenticatedUser().getLogin());

            LOG.info("Annotation deleted, return Http status 200 and annotation id: '{}'", annotationId);
            return new ResponseEntity<Object>(new JsonDeleteSuccessResponse(annotationId), HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while deleting annotation", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("Annotation could not be deleted, return Http status 404 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotation could not be deleted: " + errorMsg), HttpStatus.NOT_FOUND);
    }

    /**
     * Endpoint for searching for annotations matching given criteria
     *
     * @param request Incoming request
     * @param response Outgoing response
     * @param _separate_replies (request parameter)
     *        search flag indicating whether replies should be mixed with
     *        annotations or be returned as separate group; default: {@value #DEFAULT_SEARCH_SEARCH_REPLIES}
     * @param limit (request parameter) 
     *        the maximum number of annotations to return; default: {@value #DEFAULT_SEARCH_LIMIT}
     * @param offset (request parameter)
     *        minimum number of initial annotations to skip; default: {@value #DEFAULT_SEARCH_OFFSET}
     * @param sort (request parameter) 
     *        field by which annotations should be sorted; default: {@value #DEFAULT_SEARCH_SORT}
     * @param order (request parameter) 
     *        order in which the results should be sorted; default: {@value #DEFAULT_SEARCH_ORDER}; allows "asc" and "desc"
     * @param uri (request parameter) 
     *        URI to be used for the search - usually the URL of the annotated page
     * @param url (request parameter) 
     *        URL of the annotated page; alias for URI
     * @param user (request parameter) 
     *        limit the results to annotations made by the specified user (login)
     * @param group (request parameter) 
     *        limit the results to annotations made in the specified group
     * @param tag (request parameter) 
     *        limit the results to annotations tagged with the specified value
     * @param any (request parameter) 
     *        limit the results to annotations in which one of a number of common fields contain the passed value
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
    public ResponseEntity<Object> searchAnnotations(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "_separate_replies", defaultValue = AnnotationSearchOptions.DEFAULT_SEARCH_SEARCH_REPLIES) boolean separate_replies,
            @RequestParam(value = "limit", defaultValue = AnnotationSearchOptions.DEFAULT_SEARCH_LIMIT) int limit,
            @RequestParam(value = "offset", defaultValue = AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET) int offset,
            @RequestParam(value = "sort", defaultValue = AnnotationSearchOptions.DEFAULT_SEARCH_SORT) String sort,
            @RequestParam(value = "order", defaultValue = AnnotationSearchOptions.DEFAULT_SEARCH_ORDER) String order,
            @RequestParam(value = "uri") String uri,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "user", required = false) String user,
            @RequestParam(value = "group") String group,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "any", required = false) String any)
            throws IOException, ServletException {

        LOG.debug("Received request to search for annotations");

        String errorMsg = "";

        AnnotationSearchOptions options = new AnnotationSearchOptions(uri, group, separate_replies, limit, offset, order, sort);
        if (url != null && !url.isEmpty()) {
            options.setUri(url);
        }
        // optional parameters
        options.setUser(user);
        options.setTag(tag);
        options.setAny(any);

        try {
            String userLogin = authService.getAuthenticatedUser().getLogin();
            List<Annotation> searchResult = annotService.searchAnnotations(options, userLogin);
            List<Annotation> replies = null;
            if (searchResult != null && searchResult.size() > 0) {
                replies = annotService.searchRepliesForAnnotations(searchResult, options, userLogin);
            }
            JsonSearchResult result = annotService.convertToJsonSearchResult(searchResult, replies, options);

            LOG.debug("Annotation search successful, return Http status 200 and result: '" + (result == null ? "0" : result.getTotal()) + "'");
            return new ResponseEntity<Object>(result, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error("Error while searching for annotations", e);
            errorMsg = e.getMessage();
        }

        LOG.warn("There was a problem during annotation search, return Http status 400 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("The annotations could not be searched for: " + errorMsg), HttpStatus.BAD_REQUEST);
    }

    // configuration of the JSON deserialisation: allowing fields without quotes
    // e.g. used in document/metadata entries
    @NoAuthAnnotation
    @Bean
    public ObjectMapper objectMapper() {

        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.featuresToEnable(Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        return builder.build();
    }
}
