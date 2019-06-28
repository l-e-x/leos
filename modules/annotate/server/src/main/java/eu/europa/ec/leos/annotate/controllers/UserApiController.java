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

import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserPreferences;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
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

/**
 * API functions related to users
 */
@RestController
@RequestMapping("/api")
public class UserApiController {

    private static final Logger LOG = LoggerFactory.getLogger(UserApiController.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticatedUserStore authUser;

    // -------------------------------------
    // Endpoints
    // -------------------------------------
    /**
     * Endpoint for getting user profile
     *
     * @param request Incoming request
     * @param response Outgoing response, containing user profile as JSON body
     * @param authority the authority, as request parameter (ignored)
     *
     * @return 
     * in case of success: HTTP status 200, JSON based response containing current user profile 
     * in case of failure: HTTP status 404, JSON based response with error description
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/profile", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public ResponseEntity<Object> getProfile(final HttpServletRequest request, final HttpServletResponse response,
            @RequestParam(value = "authority", required = false) final String authority)
            throws IOException, ServletException {

        JsonUserProfile profile = null;
        String errorMsg = "";

        try {
            profile = userService.getUserProfile(authUser.getUserInfo());
            return new ResponseEntity<Object>(profile, HttpStatus.OK);

        } catch (UserNotFoundException unfe) {
            errorMsg = unfe.getMessage();
            LOG.error(errorMsg);

        } catch (Exception e) {
            errorMsg = e.getMessage();
            LOG.error("Error retrieving user profile", e);
        }

        LOG.warn("Profile not found, return Http status 404 and failure notice");
        return new ResponseEntity<Object>(new JsonFailureResponse("Profile not found: " + errorMsg), HttpStatus.NOT_FOUND);
    }

    /**
     * Endpoint for updating user preferences
     *
     * @param request Incoming request, containing user preference to be changed as JSON body
     * @param response Outgoing response, containing updated user profile as JSON body
     * @param jsonUserPref JSON preferences metadata ({@link JsonUserPreferences}), extracted from request body
     *
     * @return 
     *  in case of success: HTTP status 200, JSON based response containing current user profile
     *  in case of failure: HTTP status 404, JSON based response with error description
     *
     * @throws IOException
     * @throws ServletException
     */
    @RequestMapping(value = "/profile", method = RequestMethod.PATCH, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> updateUserPreferences(final HttpServletRequest request, final HttpServletResponse response,
            @RequestBody final JsonUserPreferences jsonUserPref)
            throws IOException, ServletException {

        String errorMsg = "";

        try {
            userService.updateSidebarTutorialVisible(authUser.getUserInfo().getLogin(), jsonUserPref.getPreferences().isShow_sidebar_tutorial());

        } catch (UserNotFoundException e) {

            errorMsg = e.getMessage();
            LOG.error("Error updating user settings", e);

        } catch (Exception e) {
            errorMsg = e.getMessage();
            LOG.error("Unexpected error retrieving user profile", e);
        }

        if (!StringUtils.isEmpty(errorMsg)) {
            LOG.warn("User setting not saved, return Http status 404 and failure notice");
            return new ResponseEntity<Object>(new JsonFailureResponse("Profile not found: " + errorMsg), HttpStatus.NOT_FOUND);
        }

        // return the (updated) user profile
        return getProfile(request, response, null);
    }

}
