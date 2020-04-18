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
package eu.europa.ec.leos.annotate.model.web.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

/**
 * custom deserializer: deserialize statuses using existing deserializer, collect all other metadata
 * 
 * based on description presented at https://www.baeldung.com/jackson-collection-array
 */
public class SimpleMetadataWithStatusesDeserializer extends StdDeserializer<SimpleMetadataWithStatuses> {

    private static final long serialVersionUID = 8132671298135915420L;

    private final static Logger LOG = LoggerFactory.getLogger(SimpleMetadataWithStatusesDeserializer.class);

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public SimpleMetadataWithStatusesDeserializer() {
        this(null);
    }

    public SimpleMetadataWithStatusesDeserializer(final Class<?> valueClass) {
        super(valueClass);
    }

    // -----------------------------------------------------------
    // Methods
    // -----------------------------------------------------------

    /**
     * custom deserializer: deserialize statuses using existing deserializer, collect all other metadata
     * 
     * receives a single metadata set like: {\"ISC\":\"ISC/1/2\", \"status\":[\"NORMAL\"]}
     * construction of a list of such items is done using the caller
     */
    @Override
    public SimpleMetadataWithStatuses deserialize(final JsonParser jparser,
            final DeserializationContext ctxt) throws IOException, JsonProcessingException {

        try {

            final JsonNode metadatasetNode = jparser.getCodec().readTree(jparser);

            List<AnnotationStatus> statuses = null;
            final SimpleMetadata simpMeta = new SimpleMetadata();

            final java.util.Iterator<Entry<String, JsonNode>> iterField = metadatasetNode.fields();
            while (iterField.hasNext()) {

                final Entry<String, JsonNode> currentField = iterField.next();
                if (currentField.getKey().equals("status")) {
                    statuses = JsonConverter.convertJsonToAnnotationStatusList(currentField.getValue().toString());
                } else {
                    simpMeta.put(currentField.getKey(), currentField.getValue().asText());
                }
            }

            return new SimpleMetadataWithStatuses(simpMeta, statuses);
        } catch (IOException ioe) {
            LOG.error("Exception parsing SimpleMetadataWithStatuses", ioe);
        }
        return null;
    }

}
