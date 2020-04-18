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
 * Class representing user information, e.g. display name (or more information in the future) 
 */
public class JsonUserInfo {

    // simple property representing the display name for a user
    private String display_name;
    private String entity_name;

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------
    
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public JsonUserInfo() {
        // default constructor required by JSON deserialisation
    }
    
    public JsonUserInfo(final JsonUserInfo orig) {
        // copy constructor
        this.display_name = orig.display_name;
        this.entity_name = orig.entity_name;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    
    @Generated
    public String getDisplay_name() {
        return display_name;
    }

    @Generated
    public void setDisplay_name(final String display_name) {
        this.display_name = display_name;
    }

    @Generated
    public String getEntity_name() {
        return entity_name;
    }

    @Generated
    public void setEntity_name(final String entity_name) {
        this.entity_name = entity_name;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(display_name, entity_name);
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
        final JsonUserInfo other = (JsonUserInfo) obj;
        return Objects.equals(this.display_name, other.display_name) &&
                Objects.equals(this.entity_name, other.entity_name);
    }
}
