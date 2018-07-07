/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.support.web;

import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
public class UrlBuilder {

    public String getBaseUrl(final HttpServletRequest httpServletRequest) {
        StringBuffer baseUrl = new StringBuffer();
        baseUrl.append(httpServletRequest.getScheme()).append("://");
        baseUrl.append(httpServletRequest.getServerName()).append(":");
        baseUrl.append(httpServletRequest.getServerPort());
        return  baseUrl.toString();
    }

    public  String getContextPath(final HttpServletRequest httpServletRequest) {
        return httpServletRequest.getContextPath();
    }

    public  String getDocumentHtmlUrl(final HttpServletRequest httpServletRequest, final String leosId) {
        return getDocumentRestUrl(httpServletRequest, "/rest/document/html/", leosId);
    }

    public  String getDocumentPdfUrl(final HttpServletRequest httpServletRequest, final String leosId) {
        return getDocumentRestUrl(httpServletRequest, "/rest/document/pdf/", leosId);
    }

    public  String getLocalPath(final HttpServletRequest httpServletRequest, final String relativePath) {
        return httpServletRequest.getContextPath() + relativePath;
    }
    
    public  String getWebAppPath(final HttpServletRequest httpServletRequest) {
        return  getBaseUrl(httpServletRequest)+ httpServletRequest.getContextPath() ;
    }
    
    private  String getDocumentRestUrl(final HttpServletRequest httpServletRequest, final String restPath, final String leosId) {
        return getBaseUrl(httpServletRequest) + getLocalPath(httpServletRequest, restPath) + leosId;
    }
}
