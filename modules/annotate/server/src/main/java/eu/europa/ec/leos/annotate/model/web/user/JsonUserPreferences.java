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
 * Class representing the top-level structure received by the hypothesis client for updating user preferences 
 */
public class JsonUserPreferences {

    private JsonUserShowSideBarPreference preferences;

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------

    public JsonUserPreferences() {
        // default constructor required by JSON deserialisation
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    public JsonUserShowSideBarPreference getPreferences() {
        return preferences;
    }

    public void setPreferences(final JsonUserShowSideBarPreference preferences) {
        this.preferences = preferences;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(preferences);
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
        final JsonUserPreferences other = (JsonUserPreferences) obj;
        return Objects.equals(this.preferences, other.preferences);
    }
}
