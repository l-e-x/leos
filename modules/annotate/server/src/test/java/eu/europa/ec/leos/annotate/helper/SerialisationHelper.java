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
package eu.europa.ec.leos.annotate.helper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.*;
import eu.europa.ec.leos.annotate.model.web.status.StatusUpdateSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.model.web.token.JsonTokenResponse;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserPreferences;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * helper functions for (de)serialisation of various JSON-wrapped objects
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class SerialisationHelper {

    private SerialisationHelper() {
        // utility class -> private constructor
    }

    // -------------------------------------
    // JSON serialisation functions
    // -------------------------------------
    public static String serialize(final JsonAnnotation annot) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(annot);
    }

    public static String serialize(final JsonUserPreferences prefs) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(prefs);
    }

    public static String serialize(final Map<String, String> map) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(map);
    }

    public static String serialize(final List<SimpleMetadata> map) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final String asJson = mapper.writeValueAsString(map);

        // serialisation puts quotation marks around array being a value, e.g.
        // {"status":["ALL"]} becomes {"status":"["ALL"]"}
        // this gives problems upon deserialisation, therefore we remove this superfluous quotes
        return asJson.replace("\"[", "[").replace("]\"", "]");
    }

    public static String serializeSimpleMetadataWithStatusesList(final List<SimpleMetadataWithStatuses> requestedMetadata) throws JsonProcessingException {
        
        // note: uses the {@link SimpleMetadataWithStatusesSerializer}
        final ObjectMapper mapper = new ObjectMapper();
        final String asJson = mapper.writeValueAsString(requestedMetadata);

        // encode curly brackets
        return asJson.replace("{", "%7B").replace("}", "%7D");
    }

    public static String serialize(final JsonIdList idList) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(idList);
    }

    // -------------------------------------
    // JSON deserialisation functions
    // -------------------------------------
    public static JsonAnnotation deserializeJsonAnnotation(final String input) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonAnnotation.class);
    }

    public static JsonFailureResponse deserializeJsonFailureResponse(final String input) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonFailureResponse.class);
    }

    public static JsonDeleteSuccessResponse deserializeJsonDeleteSuccessResponse(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonDeleteSuccessResponse.class);
    }

    public static JsonBulkDeleteSuccessResponse deserializeJsonBulkDeleteSuccessResponse(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonBulkDeleteSuccessResponse.class);
    }

    public static JsonSearchResult deserializeJsonSearchResult(final String input) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSearchResult.class);
    }

    public static JsonSearchResultWithSeparateReplies deserializeJsonSearchResultWithSeparateReplies(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSearchResultWithSeparateReplies.class);
    }

    public static JsonUserProfile deserializeJsonUserProfile(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonUserProfile.class);
    }

    public static JsonAuthenticationFailure deserializeJsonAuthenticationFailure(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonAuthenticationFailure.class);
    }

    public static JsonTokenResponse deserializeJsonTokenResponse(final String input) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonTokenResponse.class);
    }

    public static List<JsonGroupWithDetails> deserializeJsonGroupWithDetails(final String input) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, new TypeReference<List<JsonGroupWithDetails>>() {
        });
    }

    public static JsonSuggestionAcceptSuccessResponse deserializeJsonSuggestionAcceptSuccessResponse(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSuggestionAcceptSuccessResponse.class);
    }

    public static JsonSuggestionRejectSuccessResponse deserializeJsonSuggestionRejectSuccessResponse(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSuggestionRejectSuccessResponse.class);
    }

    public static StatusUpdateSuccessResponse deserializeJsonStatusUpdateSuccessResponse(final String input)
            throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, StatusUpdateSuccessResponse.class);
    }

    public static JsonSearchCount deserializeJsonSearchCount(final String input) throws JsonParseException, JsonMappingException, IOException {
        final ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSearchCount.class);
    }

}
