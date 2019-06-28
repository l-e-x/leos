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

import java.util.Objects;

/**
 * JSON object returned in case of authentication problems or problems during token generation
 */
@SuppressWarnings("PMD.LongVariable")
public class JsonAuthenticationFailure {

    private String error;
    private String error_description;

    // -------------------------------------
    // Available error texts
    // -------------------------------------
    private static final String ERROR_UNAUTHORIZED = "authentication error";
    private static final String ERROR_UNKNOWN_CLIENT = "invalid_client";
    private static final String ERROR_UNKNOWN_USER = "invalid_client";
    private static final String ERROR_ACCESS_TOKEN_EXPIRED = "invalid_grant";
    private static final String ERROR_REFRESH_TOKEN_EXPIRED = "invalid_grant";
    private static final String ERROR_INVALID_CALL = "invalid_grant";
    private static final String ERROR_INVALID_REQUEST = "invalid_request";

    private static final String ERROR_DESC_UNAUTHORIZED = "Not authorized";
    private static final String ERROR_DESC_UNKNOWN_CLIENT = "token from unknown client";
    private static final String ERROR_DESC_TOKEN_EXPIRED = "token expired";
    private static final String ERROR_DESC_UNKNOWN_USER = "invalid authentication information given";
    private static final String ERROR_DESC_INVALID_REFRESH_TOKEN = "invalid refresh token given";
    private static final String ERROR_DESC_INVALID_REQUEST = "invalid request given";
    private static final String ERROR_DESC_INVALID_GRANT_TYPE = "invalid/unsupported grant type given";
    private static final String ERROR_DESC_AUTHORITY_NOT_ALLOWED = "token from client that is not permitted for authority";

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public JsonAuthenticationFailure() {
        // default constructor required for JSON deserialisation
    }

    private JsonAuthenticationFailure(final String error, final String description) {
        this.error = error;
        this.error_description = description;
    }

    public static JsonAuthenticationFailure getUnknownUserResult() {
        return new JsonAuthenticationFailure(ERROR_UNKNOWN_USER, ERROR_DESC_UNKNOWN_USER);
    }

    public static JsonAuthenticationFailure getAccessTokenExpiredResult() {
        return new JsonAuthenticationFailure(ERROR_ACCESS_TOKEN_EXPIRED, ERROR_DESC_TOKEN_EXPIRED);
    }

    public static JsonAuthenticationFailure getRefreshTokenExpiredResult() {
        return new JsonAuthenticationFailure(ERROR_REFRESH_TOKEN_EXPIRED, ERROR_DESC_TOKEN_EXPIRED);
    }

    public static JsonAuthenticationFailure getInvalidRefreshTokenResult() {
        return new JsonAuthenticationFailure(ERROR_INVALID_CALL, ERROR_DESC_INVALID_REFRESH_TOKEN);
    }

    public static JsonAuthenticationFailure getUnsupportedGrantTypeResult() {
        return new JsonAuthenticationFailure(ERROR_INVALID_CALL, ERROR_DESC_INVALID_GRANT_TYPE);
    }

    public static JsonAuthenticationFailure getInvalidRequestResult() {
        return new JsonAuthenticationFailure(ERROR_INVALID_REQUEST, ERROR_DESC_INVALID_REQUEST);
    }

    public static JsonAuthenticationFailure getAuthenticationErrorResult() {
        return new JsonAuthenticationFailure(ERROR_UNAUTHORIZED, ERROR_DESC_UNAUTHORIZED);
    }

    public static JsonAuthenticationFailure getUnknownClientResult() {
        return new JsonAuthenticationFailure(ERROR_UNKNOWN_CLIENT, ERROR_DESC_UNKNOWN_CLIENT);
    }

    public static JsonAuthenticationFailure getTokenInvalidForClientAuthorityResult() {
        return new JsonAuthenticationFailure(ERROR_UNKNOWN_CLIENT, ERROR_DESC_AUTHORITY_NOT_ALLOWED);
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public String getError_description() {
        return error_description;
    }

    public void setError_description(final String error_description) {
        this.error_description = error_description;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(error, error_description);
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
        final JsonAuthenticationFailure other = (JsonAuthenticationFailure) obj;
        return Objects.equals(this.error, other.error) &&
                Objects.equals(this.error_description, other.error_description);
    }
}
