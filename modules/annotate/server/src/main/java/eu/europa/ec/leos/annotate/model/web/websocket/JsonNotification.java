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
package eu.europa.ec.leos.annotate.model.web.websocket;

import eu.europa.ec.leos.annotate.Generated;

import java.util.*;

public class JsonNotification {

    private String type = "annotation-notification";
    private Map<String, String> options = new HashMap<String, String>();
    private List<Object> payload = new ArrayList<>();

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public JsonNotification(final String action) {
        options.put("action", action);
    }

    public JsonNotification(final String type, final Map<String, String> options, final List<Object> payload) {
        this.type = type;
        this.options = options;
        this.payload = payload;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public String getType() {
        return type;
    }

    @Generated
    public void setType(final String type) {
        this.type = type;
    }

    @Generated
    public Map<String, String> getOptions() {
        return options;
    }

    @Generated
    public void setOptions(final Map<String, String> options) {
        this.options = options;
    }

    @Generated
    public List<Object> getPayload() {
        return payload;
    }

    @Generated
    public void setPayload(final List<Object> payload) {
        this.payload = payload;
    }

    @Generated
    public void addPayload(final Object payload) {
        this.payload.add(payload);
    }

    // -----------------------------------------------------------
    // equals and hashCode
    // -----------------------------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(type, options, payload);
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
        final JsonNotification other = (JsonNotification) obj;
        return Objects.equals(this.type, other.type) &&
                Objects.equals(this.options, other.options) &&
                Objects.equals(this.payload, other.payload);

    }
}