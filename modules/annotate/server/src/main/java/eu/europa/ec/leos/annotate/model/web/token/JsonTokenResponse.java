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
package eu.europa.ec.leos.annotate.model.web.token;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.entity.Token;

import java.util.Objects;

/**
 * Class denoting the token exchange response
 */
public class JsonTokenResponse {

    private String access_token, refresh_token;

    private String token_type = "bearer";

    private String scope = null;
    private String state = null;

    private int expires_in;

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------

    public JsonTokenResponse() {
        // default constructor required for JSON deserialisation
    }

    public JsonTokenResponse(final Token token) {

        this.access_token = token.getAccessToken();
        this.refresh_token = token.getRefreshToken();
        this.expires_in = token.getAccessTokenLifetimeSeconds();
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(final String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(final String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getToken_type() {
        return token_type;
    }

    public void setToken_type(final String token_type) {
        this.token_type = token_type;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public int getExpires_in() {
        return expires_in;
    }

    public void setExpires_in(final int expires_in) {
        this.expires_in = expires_in;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(access_token, refresh_token, token_type, scope, state, expires_in);
    }

    @Generated
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonTokenResponse other = (JsonTokenResponse) obj;
        return Objects.equals(this.access_token, other.access_token) &&
                Objects.equals(this.refresh_token, other.refresh_token) &&
                Objects.equals(this.token_type, other.token_type) &&
                Objects.equals(this.scope, other.scope) &&
                Objects.equals(this.state, other.state) &&
                Objects.equals(this.expires_in, other.expires_in);
    }
}
