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
package eu.europa.ec.leos.annotate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.ec.leos.annotate.Generated;

import java.util.List;
import java.util.Objects;

/**
 * Class containing user detail information, which was e.g. retrieved from external user repository
 */
public class UserDetails {

    private String login; // user login name
    private String email; // email address
    private String entity; // associated entity, e.g. "DIGIT"
    private String displayName; // nice name to be used

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @JsonCreator
    public UserDetails(@JsonProperty("login") final String login, @JsonProperty("perId") final Long perId,
            @JsonProperty("firstName") final String firstName, @JsonProperty("lastName") final String lastName,
            @JsonProperty("entity") final String entity, @JsonProperty("email") final String email, @JsonProperty("roles") final List<String> roles) {

        this.login = login;
        this.displayName = String.format("%s %s", lastName, firstName);
        this.email = email;
        this.entity = entity;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public String getLogin() {
        return login;
    }

    public void setLogin(final String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(final String email) {
        this.email = email;
    }
    
    public String getEntity() {
        return entity;
    }

    public void setEntity(final String entity) {
        this.entity = entity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(login, email, entity, displayName);
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
        final UserDetails other = (UserDetails) obj;
        return Objects.equals(this.entity, other.entity) &&
                Objects.equals(this.login, other.login) &&
                Objects.equals(this.email, other.email) &&
                Objects.equals(this.displayName, other.displayName);
    }
}
