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

import eu.europa.ec.leos.annotate.aspects.NoAuthAnnotation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/")
public class AppController {

    @Value("${annotate.client.url}")
    private String clientUrl;

    @NoAuthAnnotation
    @RequestMapping(value = {"app.html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView getAppHtml(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
        final ModelAndView modelAndView = new ModelAndView("app");
        final ModelMap model = new ModelMap();
        model.addAttribute("clientUrl", clientUrl);
        modelAndView.addAllObjects(model);
        return modelAndView;
    }
}