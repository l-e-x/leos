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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * custom serialisation of {@link SimpleMetadataWithStatuses} objects
 */
public class SimpleMetadataWithStatusesSerializer extends StdSerializer<SimpleMetadataWithStatuses> {

    private static final long serialVersionUID = -6490537196614908863L;
    private final static Logger LOG = LoggerFactory.getLogger(SimpleMetadataWithStatusesSerializer.class);

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public SimpleMetadataWithStatusesSerializer() {
        this(null);
    }

    public SimpleMetadataWithStatusesSerializer(final Class<SimpleMetadataWithStatuses> smwt) {
        super(smwt);
    }

    // -----------------------------------------------------------
    // Methods
    // -----------------------------------------------------------

    /**
     * custom serialisation of a {@link SimpleMetadataWithStatuses} object
     */
    @Override
    public void serialize(final SimpleMetadataWithStatuses value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException {
        
        // should become something like: {\"ISC\":\"ISC/1/2\", \"status\":[\"NORMAL\"]},
        // i.e. the metadata key-value pairs are written directly, while the statuses are written as an array

        jgen.writeStartObject();

        // write the metadata
        for (final String metaKey : value.getMetadata().keySet()) {
            jgen.writeStringField(metaKey, value.getMetadata().get(metaKey));
        }
        LOG.trace("Serialised {} metadata items", value.getMetadata().keySet().size());

        // write the status
        jgen.writeArrayFieldStart("status");
        for (final AnnotationStatus stat : value.getStatuses()) {
            jgen.writeString(stat.name());
        }
        LOG.trace("Serialised {} status items", value.getStatuses().size());
        jgen.writeEndArray();

        jgen.writeEndObject();
    }

}
