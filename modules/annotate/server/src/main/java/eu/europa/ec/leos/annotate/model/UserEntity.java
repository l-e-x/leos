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

import java.util.Objects;

/**
 * class representing the entities assigned to a user in UD-repo
 */
public class UserEntity {

    @SuppressWarnings("PMD.ShortVariable")
    private String id;
    private String name;
    private String organizationName;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    @JsonCreator
    public UserEntity(@JsonProperty("id") final String idParam,
            @JsonProperty("name") final String name, @JsonProperty("organizationName") final String organizationName) {

        this.id = idParam;
        this.name = name;
        this.organizationName = organizationName;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    
    @Generated
    public String getId() {
        return id;
    }

    @Generated
    public void setId(final String idParam) {
        this.id = idParam;
    }

    @Generated
    public String getName() {
        return name;
    }

    @Generated
    public void setName(final String name) {
        this.name = name;
    }

    @Generated
    public String getOrganizationName() {
        return organizationName;
    }

    @Generated
    public void setOrganizationName(final String organizationName) {
        this.organizationName = organizationName;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, name, organizationName);
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
        final UserEntity other = (UserEntity) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.organizationName, other.organizationName);
    }
}
