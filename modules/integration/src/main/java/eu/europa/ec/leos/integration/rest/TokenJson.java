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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class TokenJson {

	private String accessToken;
	private String tokenType;
	private String refreshToken;
	private Long expiresIn;
	private String scope;
	private String state;

	@JsonCreator
	public TokenJson(@JsonProperty("access_token") String accessToken,
			@JsonProperty("refresh_token") String refreshToken, @JsonProperty("token_type") String tokenType,
			@JsonProperty("scope") String scope, @JsonProperty("state") String state,
			@JsonProperty("expires_in") long expiresIn) {
		this.accessToken = accessToken;
		this.tokenType = tokenType;
		this.refreshToken = refreshToken;
		this.expiresIn = expiresIn;
		this.scope = scope;
		this.state = state;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public Long getExpiresIn() {
		return expiresIn;
	}

	public String getScope() {
		return scope;
	}

	public String getState() {
		return state;
	}

}