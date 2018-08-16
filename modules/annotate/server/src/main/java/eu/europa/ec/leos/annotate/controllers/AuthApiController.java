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
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.model.web.token.JsonTokenResponse;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.exceptions.TokenFromUnknownClientException;
import eu.europa.ec.leos.annotate.services.exceptions.TokenInvalidForClientAuthorityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

/**
 * API functions related to authentication
 */
@RestController
@RequestMapping("/api")
public class AuthApiController {

    private static final Logger LOG = LoggerFactory.getLogger(AuthApiController.class);

    private static final String GRANT_TYPE = "grant_type";
    private static final String BEARER_GRANT_TYPE = "jwt-bearer";
    private static final String BEARER_PARAMETER = "assertion";
    private static final String REFRESH_GRANT_TYPE = "refresh_token";
    private static final String REFRESH_GRANT_PARAMETER = "refresh_token";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AuthenticationService authService;

    @Autowired
    private UserService userService;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    // note: custom constructor in order to ease testability by benefiting from dependency injection
    @NoAuthAnnotation
    @Autowired
    public AuthApiController(UserService userService, AuthenticationService authService) {
        if (this.userService == null) {
            this.userService = userService;
        }
        if (this.authService == null) {
            this.authService = authService;
        }
    }

    // -------------------------------------
    // API endpoints
    // -------------------------------------

    /**
     * Endpoint for token exchange 
     * 
     * transforms an id_token to an access token
     * verifies the id_token on that way
     *
     * @param request Incoming request
     * @param response Outgoing response, containing API new tokens (access, refresh) as JSON body
     * 
     * @throws Exception
     * @throws ServletException
     */
    @NoAuthAnnotation
    @RequestMapping(value = {"/token"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Object> getToken(HttpServletRequest request, HttpServletResponse response) throws Exception, ServletException {

        // all responses are supposed to be without caching
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        JsonAuthenticationFailure errorResult = null;

        String grantType = request.getParameter(GRANT_TYPE);

        if (!StringUtils.isEmpty(grantType)) {

            // request access token
            if (grantType.contains(BEARER_GRANT_TYPE)) {

                User user = null;
                UserDetails userDetails = null;
                String login = null;
                try {
                    login = authService.getUserLoginFromToken(request.getParameter(BEARER_PARAMETER));

                    LOG.debug("User '{}' requesting for login with jwt-token", login);

                    // create the user in our database, if not yet existing
                    try {
                        user = userService.createUserIfNotExists(login);
                    } catch (Exception ex) {
                        LOG.warn("Exception occurred while creating user, cannot create user! {}", ex.getMessage());
                    }

                    // check if user details are available in UD-REPO
                    try {
                        userDetails = userService.getUserDetailsFromUserRepo(login);
                    } catch (Exception ex) {
                        LOG.warn("Exception occurred while fetching user details from ud-repo: {}", ex.getMessage());
                    }

                    if (userDetails == null) {
                        // user is not known in UD-REPO, so we deny access
                        LOG.error("User '{}' was not found in UD-REPO, deny access!", login);
                        errorResult = JsonAuthenticationFailure.getUnknownUserResult();
                    } else {
                        // user was found, generate tokens and allow access

                        // add user to group identified by his entity
                        userService.addUserToEntityGroup(user, userDetails);

                        // store access token / refresh token in database
                        Token token = authService.generateAndSaveTokensForUser(user);
                        return new ResponseEntity<Object>(
                                new JsonTokenResponse(token.getAccessToken(), token.getRefreshToken(), token.getAccessTokenLifetimeSeconds()),
                                HttpStatus.OK);
                    }
                } catch (TokenFromUnknownClientException tfuce) {
                    LOG.error("Error decoding JWT token", tfuce);
                    errorResult = JsonAuthenticationFailure.getUnknownClientResult();
                } catch (TokenInvalidForClientAuthorityException tifcae) {
                    LOG.error("JWT token received from authority not authorized to authenticate user", tifcae);
                    errorResult = JsonAuthenticationFailure.getTokenInvalidForClientAuthorityResult();
                }

                // request refresh of access token
            } else if (grantType.equals(REFRESH_GRANT_TYPE)) {
                String refreshTokenParameter = request.getParameter(REFRESH_GRANT_PARAMETER);

                // get user with matching refresh token
                AtomicReference<Token> foundToken = new AtomicReference<Token>();
                User user = authService.findUserByRefreshToken(refreshTokenParameter, foundToken);
                if (user == null) {

                    // note on test coverage: if token found and not expired, then a user is returned -> missed branch noted here cannot occur
                    if (foundToken.get() != null && foundToken.get().isRefreshTokenExpired()) {
                        // token found, but expired
                        errorResult = JsonAuthenticationFailure.getRefreshTokenExpiredResult();
                    } else {
                        // no token found -> unknown token received
                        errorResult = JsonAuthenticationFailure.getInvalidRefreshTokenResult();
                    }
                } else {

                    // store access token / refresh token in database
                    Token token = authService.generateAndSaveTokensForUser(user);
                    return new ResponseEntity<Object>(new JsonTokenResponse(
                            token.getAccessToken(), token.getRefreshToken(), token.getRefreshTokenLifetimeSeconds()), HttpStatus.OK);
                }
                // grant type not supported
            } else {
                errorResult = JsonAuthenticationFailure.getUnsupportedGrantTypeResult();
            }

        } else { // grant type empty
            errorResult = JsonAuthenticationFailure.getInvalidRequestResult();
        }

        return new ResponseEntity<Object>(errorResult, HttpStatus.BAD_REQUEST);
    }
}
