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
package eu.europa.ec.leos.annotate.aspects;

import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.exceptions.AccessTokenExpiredException;

import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Aspect acting on all controller methods having a HttpServletRequest as parameter
 * Takes care to verify access tokens; denies access in case of invalid access tokens
 */
@Aspect
@Component
public class AuthAspect {

    private static final Logger LOG = LoggerFactory.getLogger(AuthAspect.class);
    
    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    private AuthenticationService authenticationService;

    @Autowired
    public AuthAspect(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    // -------------------------------------
    // Scope definition of annotations
    // -------------------------------------
    
    // @Pointcut("execution(public org.springframework.http.ResponseEntity eu.europa.ec.leos.annotate.controllers.*.*(..))")
    @Pointcut("execution(* eu.europa.ec.leos.annotate.controllers.*.*(..))")
    public void allControllerMethods() {
    }

    // the NoAuthAnnotation is to be used if no authentication needs to be verified for a controller method
    @Pointcut("@annotation(eu.europa.ec.leos.annotate.aspects.NoAuthAnnotation)")
    public void hasNoAuthAnnotation() {
    }

    /**
     * Authenticate a request by analysing the access token received in the request
     * 
     * @param joinPoint aspect joinpoint from which we are called
     * @param request the HTTP request containing the access token
     * @return proceeds if authentication is passed, returns a {@link JsonFailureResponse} with HTTP 401 otherwise
     */
    @Around("allControllerMethods() && !hasNoAuthAnnotation() && args(request, ..)")
    public Object authenticate(final ProceedingJoinPoint joinPoint, HttpServletRequest request) throws Throwable {
        
        if (request == null || authenticationService == null) {
            LOG.error("Either the request was null or required authentication service; cannot continue...");
            return new ResponseEntity<Object>(1, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String login = null;
        
        try {
            login = authenticationService.getUserLogin(request);
        } catch (AccessTokenExpiredException atee) {
            // according to OAuth specification, section 5.2, "invalid_grant" is to be replied for expired tokens
            return new ResponseEntity<Object>(JsonAuthenticationFailure.getAccessTokenExpiredResult(), HttpStatus.UNAUTHORIZED);
        }

        if (login == null) {
            return new ResponseEntity<Object>(JsonAuthenticationFailure.getAuthenticationErrorResult(), HttpStatus.UNAUTHORIZED);
        }

        return joinPoint.proceed();
    }
}
