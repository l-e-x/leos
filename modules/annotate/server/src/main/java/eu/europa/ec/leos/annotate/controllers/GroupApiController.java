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

import eu.europa.ec.leos.annotate.aspects.NoAuthAnnotation;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * API functions related to groups
 */
@RestController
@RequestMapping("/api")
public class GroupApiController {

    private static final Logger LOG = LoggerFactory.getLogger(GroupApiController.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AuthenticationService authenticationService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private GroupService groupService;

    // -------------------------------------
    // Endpoints
    // -------------------------------------

    /**
     * Endpoint for getting groups of the user
     * - for logged-in user, all his groups are returned
     * - for non-logged-in user, only default group is returned
     *
     * @param request      Incoming request
     * @param response     Outgoing response, containing user groups as JSON body
     * @param authority    (currently ignored) the authority, as request parameter
     * @param document_uri (currently ignored) the URI of the document, used for scoped groups that only apply to certain URLs
     * @param expand       (currently ignored) parameter indicating that certain elements should provide more ('expanded') information - according
     *                     to documentation, only supported value is "organization", which we don't support at the moment
     *
     * @return 
     * in case of success: HTTP status 200, JSON based response containing (user's) groups
     * in case of failure: HTTP status 400, JSON based response with error description
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/groups", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    @NoAuthAnnotation
    public ResponseEntity<Object> getGroups(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "authority", required = false) String authority,
            @RequestParam(value = "document_uri", required = false) String documentUri,
            @RequestParam(value = "expand", required = false) String expand)
            throws IOException, ServletException {

        List<JsonGroupWithDetails> groups = null;
        String errorMsg = "";

        try {
            String userlogin = authenticationService.getUserLogin(request);
            User user = null;
            
            if(!StringUtils.isEmpty(userlogin)) {
                user = userService.findByLogin(userlogin);
            }
            
            groups = groupService.getUserGroupsAsJson(user);
            
            if(groups == null) {
                groups = new ArrayList<JsonGroupWithDetails>();
            }
            return new ResponseEntity<Object>(groups, HttpStatus.OK);

        } catch (Exception e) {
            errorMsg = e.getMessage();
            LOG.error("Error finding user's groups", e);
        }

        LOG.warn("Error finding user's groups, return Http status 400 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("Error finding groups: " + errorMsg), HttpStatus.BAD_REQUEST);
    }
}
