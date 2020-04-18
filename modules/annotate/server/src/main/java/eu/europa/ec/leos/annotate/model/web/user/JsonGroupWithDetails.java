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

import eu.europa.ec.leos.annotate.Generated;

import java.util.Objects;

/**
 * Class representing a group membership reported during user's group retrieval
 * contains an extended property set compared to {@link JsonGroup}
 */
public class JsonGroupWithDetails extends JsonGroup {

    private static final String PRIVATE_GROUP_INDICATOR = "private";
    private static final String PUBLIC_GROUP_INDICATOR = "open";

    private boolean scoped; // currently hard-coded, as no further support available yet
    private String type;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public JsonGroupWithDetails() {
        // default constructor required for JSON deserialisation
        super();
    }

    public JsonGroupWithDetails(final String displayName, final String internalName, final boolean publicGroup) {

        super(displayName, internalName, publicGroup);
        this.type = publicGroup ? PUBLIC_GROUP_INDICATOR : PRIVATE_GROUP_INDICATOR;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    
    @Generated
    public boolean isScoped() {
        return scoped;
    }

    @Generated
    public void setScoped(final boolean scoped) {
        this.scoped = scoped;
    }

    @Generated
    public String getType() {
        return type;
    }

    @Generated
    public void setType(final String type) {
        this.type = type;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(scoped, type);
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
        final JsonGroupWithDetails other = (JsonGroupWithDetails) obj;
        return Objects.equals(this.scoped, other.scoped) &&
                Objects.equals(this.type, other.type);
    }
}
