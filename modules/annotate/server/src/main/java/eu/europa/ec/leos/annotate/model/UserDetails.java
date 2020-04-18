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
    private List<UserEntity> entities; // associated "main entities" to the user
    private String displayName; // nice name to be used
    private List<UserEntity> allEntities; // total list of associated entities (including hierarchy paths)

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    @SuppressWarnings("PMD.UnusedFormalParameter")
    @JsonCreator
    public UserDetails(@JsonProperty("login") final String login, @JsonProperty("perId") final Long perId,
            @JsonProperty("firstName") final String firstName, @JsonProperty("lastName") final String lastName,
            @JsonProperty("entities") final List<UserEntity> entities, @JsonProperty("email") final String email,
            @JsonProperty("roles") final List<String> roles) {

        this.login = login;
        this.displayName = String.format("%s %s", lastName, firstName);
        this.email = email;
        this.entities = entities;

        // note: the total entity list is only set afterwards, since it is retrieved using
        // a different UD-repo request
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    @Generated
    public String getLogin() {
        return login;
    }

    @Generated
    public void setLogin(final String login) {
        this.login = login;
    }

    @Generated
    public String getEmail() {
        return email;
    }

    @Generated
    public void setEmail(final String email) {
        this.email = email;
    }

    @Generated
    public List<UserEntity> getEntities() {
        return entities;
    }

    @Generated
    public void setEntities(final List<UserEntity> entities) {
        this.entities = entities;
    }

    @Generated
    public String getDisplayName() {
        return displayName;
    }

    @Generated
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Generated
    public List<UserEntity> getAllEntities() {
        return allEntities;
    }

    @Generated
    public void setAllEntities(final List<UserEntity> entities) {
        this.allEntities = entities;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(login, email, entities, displayName, allEntities);
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
        return Objects.equals(this.entities, other.entities) &&
                Objects.equals(this.login, other.login) &&
                Objects.equals(this.email, other.email) &&
                Objects.equals(this.displayName, other.displayName) &&
                Objects.equals(this.allEntities, other.allEntities);
    }
}
