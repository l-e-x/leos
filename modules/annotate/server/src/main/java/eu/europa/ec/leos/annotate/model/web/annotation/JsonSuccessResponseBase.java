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

public abstract class JsonSuccessResponseBase {

    /**
     * Base class representing the simple structure transmitted as response in case of successful execution of requests
     */
    
    protected String id;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    
    // default constructor required for deserialisation
    public JsonSuccessResponseBase() {
    }
    
    public JsonSuccessResponseBase(String annotationId) {

        this.id = annotationId;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // -------------------------------------
    // equals and hashCode
    // (need to be defined in sub classes)
    // -------------------------------------
    
    public abstract int hashCode();
    public abstract boolean equals(Object obj);
}
