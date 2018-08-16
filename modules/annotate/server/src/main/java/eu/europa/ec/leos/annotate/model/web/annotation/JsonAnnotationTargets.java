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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;

public class JsonAnnotationTargets {

    private static final Logger LOG = LoggerFactory.getLogger(JsonAnnotationTargets.class);

    /**
     * Class representing the structure received by the hypothesis client for the path to the annotated text 
     */

    private URI source;
    private Object selector;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    @JsonCreator
    public JsonAnnotationTargets(@JsonProperty("selector") Object selectors) {

        this.selector = selectors;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    public Object getSelector() {
        return selector;
    }

    public void setSelector(Object selector) {
        this.selector = selector;
    }

    /**
     * Deserialize a given String representing the target selectors list and the source
     * 
     * @param serialized serialized string
     */
    @JsonIgnore
    public void setDeserializedSelectors(String serialized) {

        try {
            @SuppressWarnings("unchecked")
            ArrayList<LinkedHashMap<String, Object>> deserialized = new ObjectMapper().readValue(serialized, ArrayList.class);
            if (deserialized != null && deserialized.size() > 0) {
                LinkedHashMap<String, Object> lhm = deserialized.get(0);
                this.selector = lhm.get("selector");
                if (lhm.get("source") != null) {
                    try {
                        this.source = new URI(lhm.get("source").toString());
                    } catch (URISyntaxException e) {
                        LOG.error("Serialized URI from targets could not be deserialized", e);
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Annotation target selectors could not be serialized to JSON", e);
        }
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(source, selector);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final JsonAnnotationTargets other = (JsonAnnotationTargets) obj;
        return Objects.equals(this.source, other.source) &&
                Objects.equals(this.selector, other.selector);
    }
}
