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
package eu.europa.ec.leos.web.support;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;

import java.util.EnumSet;
import java.util.Properties;
import java.util.regex.Pattern;

@WebListener
public class CorsFilterRegisterListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(CorsFilterRegisterListener.class);

    /*
     * CORS register listener.
     * 
     * It register CORS filter base on the values provided through following properties.
     *
     *  - "leos.cors.filter.url.mappings": LEOS application context paths for which the filter will be applied. Several context paths
     *  can be provided separated with commas.
     *  
     *  - "leos.cors.filter.allowed.domains": Domains allowed to access the resources behind the filter. Several domains can be provided
     *  separated with commas or * for no restriction. 
     *  
     * ie: leos.cors.filter.url.mappings=/example/css/*,/example2/css/*,...
     *     leos.cors.filter.allowed.domains=http://www.example.com,http://www.example2.com,...
     *  
     *  If some of properties are empty CORS filter is not registered.
     *
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.debug("Registering CORS filter...");
        ServletContext ctx = sce.getServletContext();
        ApplicationContext appCtx = WebApplicationContextUtils.getWebApplicationContext(ctx);

        Properties applicationProperties = (Properties) appCtx.getBean("applicationProperties");
        String urlMappings = applicationProperties.getProperty("leos.cors.filter.url.mappings");
        String allowedDomains = applicationProperties.getProperty("leos.cors.filter.allowed.domains");

        if ((!StringUtils.isEmpty(urlMappings)) && (!StringUtils.isEmpty(allowedDomains))) {
            FilterRegistration fr = ctx.addFilter("CorsFilter", CorsFilter.class);
            fr.setInitParameter("allowedDomains", allowedDomains.trim());
            Pattern.compile(",").splitAsStream(urlMappings).forEach(x -> {
                fr.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD), true, x.trim());
                LOG.debug("CORS filter registered url mapping: " + x.trim());
            });
        } else {
            LOG.debug("CORS filter not registered!");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

}
