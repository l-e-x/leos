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
package eu.europa.ec.leos.annotate.model.web.annotation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true) // required to avoid deserialisation failures for constant field 'accepted'
public class JsonSuggestionAcceptSuccessResponse extends JsonSuccessResponseBase {

    /**
     * Class representing the simple structure transmitted as response in case of successfully accepting of a suggestion 
     */
    
    private static final boolean accepted = true;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    
    // default constructor required for deserialisation
    public JsonSuggestionAcceptSuccessResponse() {
        super();
    }
    
    public JsonSuggestionAcceptSuccessResponse(String annotationId) {
        super(annotationId);
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    
    public boolean getAccepted() {
        return accepted;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------
    
    @Override
    public int hashCode() {
        return Objects.hash(id, accepted);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonSuggestionAcceptSuccessResponse other = (JsonSuggestionAcceptSuccessResponse) obj;
        return Objects.equals(this.id, other.id); // static field left out
    }
}
