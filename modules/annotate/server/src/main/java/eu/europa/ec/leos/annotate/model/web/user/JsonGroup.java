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
package eu.europa.ec.leos.annotate.model.web.user;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Class representing a group membership reported during user profile retrieval
 */
public class JsonGroup {

    @JsonProperty("public")
    private boolean isPublic; // flag indicating if group is public
    private String name; // corresponds to our group display name
    private String id; // internally, this represents the unique group name

    // -----------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------
    // default constructor required for JSON deserialisation
    public JsonGroup() {
    }

    public JsonGroup(String name, String id, boolean isPublic) {
        this.isPublic = isPublic;
        this.name = name;
        this.id = id;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
