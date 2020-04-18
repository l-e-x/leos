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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class JsonConverter {

    private final static Logger LOG = LoggerFactory.getLogger(JsonConverter.class);

    private JsonConverter() {
        // private constructor to have class be considered an utility class
    }

    // perform a JSON deserialisation of a list of {@link SimpleMetadataWithStatuses} objects
    public static List<SimpleMetadataWithStatuses> convertJsonToSimpleMetadataWithStatusesList(final String dataAsJson) {

        List<SimpleMetadataWithStatuses> map = new ArrayList<SimpleMetadataWithStatuses>();
        if (StringUtils.isEmpty(dataAsJson)) {
            return map;
        }

        try {
            // register the custom deserializer
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
            final SimpleModule module = new SimpleModule();
            module.addDeserializer(SimpleMetadataWithStatuses.class, new SimpleMetadataWithStatusesDeserializer());
            mapper.registerModule(module);

            // idea from: https://www.baeldung.com/jackson-collection-array
            final JavaType customClassCollection = mapper.getTypeFactory().constructCollectionType(List.class, SimpleMetadataWithStatuses.class);
            map = mapper.readValue(dataAsJson, customClassCollection);

            // the custom deserialiser might return {@literal null} in case of error
            // -> filter out
            map = map.stream().filter(smws -> smws != null).collect(Collectors.toList());
        } catch (IOException e) {
            LOG.error(String.format("Error deserialising JSON input '%s'", dataAsJson), e);
        }

        return map;
    }

    // perform a JSON deserialisation of a {@link JsonAnnotationStatuses} object into a list of {@link AnnotationStatus} items
    // note: also called by our custom deserializer for {@link SimpleMetadataWithStatuses}
    public static List<AnnotationStatus> convertJsonToAnnotationStatusList(final String statuses) {

        if (StringUtils.isEmpty(statuses)) {
            return AnnotationStatus.getDefaultStatus();
        }

        List<AnnotationStatus> statusObject = null;

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final JavaType customClassCollection = mapper.getTypeFactory().constructCollectionType(List.class, AnnotationStatus.class);
            statusObject = mapper.readValue(statuses, customClassCollection);
        } catch (IOException e) {
            LOG.error(String.format("Error deserialising JSON input '%s'", statuses), e);
        }

        if (statusObject == null || CollectionUtils.isEmpty(statusObject)) {
            return AnnotationStatus.getDefaultStatus();
        }

        // if the "ALL" value was set, others can be dropped
        if (statusObject.contains(AnnotationStatus.ALL)) {
            return AnnotationStatus.getAllValues();
        }
        return statusObject;
    }

}
