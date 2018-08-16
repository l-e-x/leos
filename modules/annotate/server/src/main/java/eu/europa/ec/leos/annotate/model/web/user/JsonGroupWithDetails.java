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

/**
 * Class representing a group membership reported during user's group retrieval
 * contains an extended property set compared to {@link JsonGroup}
 */
public class JsonGroupWithDetails extends JsonGroup {

    private static final String PRIVATE_GROUP_INDICATOR = "private";
    private static final String PUBLIC_GROUP_INDICATOR = "open";

    private boolean scoped = false; // currently hard-coded, as no further support available yet
    private String type;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    
    // default constructor required for JSON deserialisation
    public JsonGroupWithDetails() {
    }

    public JsonGroupWithDetails(String displayName, String internalName, boolean publicGroup) {

        super(displayName, internalName, publicGroup);
        this.type = publicGroup ? PUBLIC_GROUP_INDICATOR : PRIVATE_GROUP_INDICATOR;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public boolean isScoped() {
        return scoped;
    }

    public void setScoped(boolean scoped) {
        this.scoped = scoped;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
