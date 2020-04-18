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
package eu.europa.ec.leos.annotate.model.web.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.ec.leos.annotate.Generated;

import java.util.Objects;

/**
 * Class representing a group membership reported during user profile retrieval
 */
public class JsonGroup {

    @JsonProperty("public")
    private boolean publicGroup; // flag indicating if group is public
    private String name; // corresponds to our group display name
    
    @SuppressWarnings("PMD.ShortVariable")
    private String id; // internally, this represents the unique group name

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------

    public JsonGroup() {
        // default constructor required for JSON deserialisation
    }

    public JsonGroup(final String name, final String newId, final boolean isPublic) {
        this.publicGroup = isPublic;
        this.name = name;
        this.id = newId;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    
    @Generated
    public boolean isPublic() {
        return publicGroup;
    }

    @Generated
    public void setPublic(final boolean isPublic) {
        this.publicGroup = isPublic;
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
    public String getId() {
        return id;
    }

    @Generated
    public void setId(final String newId) {
        this.id = newId;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(publicGroup, name, id);
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
        final JsonGroup other = (JsonGroup) obj;
        return Objects.equals(this.publicGroup, other.publicGroup) &&
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.id, other.id);

    }
}
