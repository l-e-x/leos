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
package eu.europa.ec.leos.integration.rest;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import eu.europa.ec.leos.integration.AnnotationProvider;

@Component
public class AnnotationClientImpl implements AnnotationProvider {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AnnotationAuthProvider authenticationProvider;

	@Override
	public String searchAnnotations(URI uri, String jwtToken) {
		HttpHeaders headers = new HttpHeaders();
		TokenJson tokenJson = authenticationProvider.getToken(jwtToken);
		// FIXME In ticket LEOS-2862 Annotations: improve authentication provider
		headers.set("Authorization", "Bearer " + tokenJson.getAccessToken());
		headers.set("Accept", "application/json");
		HttpEntity<?> request = new HttpEntity<>(headers);
		return restTemplate.exchange(uri, HttpMethod.GET, request, String.class).getBody().toString();
	}

}