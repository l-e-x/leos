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
package eu.europa.ec.leos.annotate.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Class containing user detail information, which was e.g. retrieved from external user repository
 */
public class UserDetails {

    private String login; // user login name
    private String authority; // authority to which user is associated, e.g. "ecas"
    private String email; // email address
    private String entity; // associated entity, e.g. "DIGIT"
    private String displayName; // nice name to be used

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    @JsonCreator
    public UserDetails(@JsonProperty("login") String login, @JsonProperty("perId") Long perId,
            @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
            @JsonProperty("entity") String entity, @JsonProperty("email") String email, @JsonProperty("roles") List<String> roles) {

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

    public void setLogin(String login) {
        this.login = login;
    }

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(login, authority, email, displayName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UserDetails other = (UserDetails) obj;
        return Objects.equals(this.authority, other.authority) &&
                Objects.equals(this.login, other.login) &&
                Objects.equals(this.email, other.email) &&
                Objects.equals(this.displayName, other.displayName);
    }
}
