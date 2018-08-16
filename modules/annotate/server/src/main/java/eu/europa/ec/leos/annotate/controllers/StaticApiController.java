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
import eu.europa.ec.leos.annotate.services.StaticContentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * API functions providing static content
 */
@RestController
@RequestMapping("/api")
public class StaticApiController {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private StaticContentService contentService;

    // -------------------------------------
    // API endpoints
    // -------------------------------------
    /**
     * Endpoint for API root
     *
     * @param request Incoming request
     * @param response Outgoing response, containing API description as JSON body
     *
     * @return HTTP status 200, JSON based response containing API endpoints description
     *
     * @throws IOException
     * @throws ServletException
     */
    @NoAuthAnnotation
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public String getApi(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        return contentService.getApi();
    }

    /**
     * Endpoint for API links
     *
     * @param request Incoming request
     * @param response Outgoing response, containing API description as JSON body
     *
     * @return HTTP status 200, JSON based response containing further API links
     *
     * @throws IOException
     * @throws ServletException
     */
    @NoAuthAnnotation
    @RequestMapping(value = {"/links"}, produces = {MediaType.APPLICATION_JSON_VALUE})
    @ResponseBody
    public String getLinks(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        return contentService.getLinks();
    }

}
