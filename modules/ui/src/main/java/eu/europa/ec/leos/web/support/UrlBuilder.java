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

import com.vaadin.server.Page;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

import java.net.URI;

@Component
public class UrlBuilder {

    public String getBaseUrl(final HttpServletRequest httpServletRequest) {
        StringBuffer baseUrl = new StringBuffer();
        baseUrl.append(httpServletRequest.getScheme()).append("://");
        baseUrl.append(httpServletRequest.getServerName()).append(":");
        baseUrl.append(httpServletRequest.getServerPort());
        return  baseUrl.toString();
    }

    public  String getWebAppPath(final HttpServletRequest httpServletRequest) {
        return  getBaseUrl(httpServletRequest)+ httpServletRequest.getContextPath() ;
    }
    
    public String getDocumentUrl(final Page page) {
        URI location = page.getLocation();
        StringBuilder URL = new StringBuilder();
        URL.append(location.getScheme()).append("://");
        URL.append(location.getAuthority());
        URL.append(location.getPath()).append("#");
        URL.append(location.getFragment());
        return URL.toString();
    }
}
