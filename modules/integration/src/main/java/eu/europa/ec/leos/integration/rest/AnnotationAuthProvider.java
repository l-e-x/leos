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
package eu.europa.ec.leos.integration.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Provides authentication token for annotation
 *
 */
@Component
class AnnotationAuthProvider {

    @Autowired
    private RestTemplate restTemplate;

    @Value("#{integrationProperties['annotate.api.internal.host']}")
    private String annotationHost;

    /**
     * @param jwtToken
     * @return Access token
     */
    public TokenJson getToken(String jwtToken) {
        String tokenURI = annotationHost + "token";
        MultiValueMap<String, String> requestPayload = new LinkedMultiValueMap<String, String>();
        requestPayload.add("grant_type", "jwt-bearer");
        requestPayload.add("assertion", jwtToken);
        return restTemplate.postForObject(tokenURI, requestPayload, TokenJson.class);
    }

}
