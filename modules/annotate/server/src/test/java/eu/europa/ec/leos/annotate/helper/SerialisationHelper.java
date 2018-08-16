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
package eu.europa.ec.leos.annotate.helper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.*;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.model.web.token.JsonTokenResponse;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserPreferences;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;

import java.io.IOException;
import java.util.List;

/**
 * helper functions for (de)serialisation of various Json wrap objects
 */
public class SerialisationHelper {

    // -------------------------------------
    // JSON serialisation functions
    // -------------------------------------
    public static String serialize(JsonAnnotation annot) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(annot);
    }

    public static String serialize(JsonUserPreferences prefs) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(prefs);
    }

    // -------------------------------------
    // JSON deserialisation functions
    // -------------------------------------
    public static JsonAnnotation deserializeJsonAnnotation(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonAnnotation.class);
    }

    public static JsonFailureResponse deserializeJsonFailureResponse(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonFailureResponse.class);
    }

    public static JsonDeleteSuccessResponse deserializeJsonDeleteSuccessResponse(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonDeleteSuccessResponse.class);
    }

    public static JsonSearchResult deserializeJsonSearchResult(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSearchResult.class);
    }

    public static JsonSearchResultWithSeparateReplies deserializeJsonSearchResultWithSeparateReplies(String input)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSearchResultWithSeparateReplies.class);
    }

    public static JsonUserProfile deserializeJsonUserProfile(String input)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonUserProfile.class);
    }

    public static JsonAuthenticationFailure deserializeJsonAuthenticationFailure(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonAuthenticationFailure.class);
    }

    public static JsonTokenResponse deserializeJsonTokenResponse(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonTokenResponse.class);
    }

    public static List<JsonGroupWithDetails> deserializeJsonGroupWithDetails(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, new TypeReference<List<JsonGroupWithDetails>>() {});
    }

    public static JsonSuggestionAcceptSuccessResponse deserializeJsonSuggestionAcceptSuccessResponse(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSuggestionAcceptSuccessResponse.class);
    }

    public static JsonSuggestionRejectSuccessResponse deserializeJsonSuggestionRejectSuccessResponse(String input) throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(input, JsonSuggestionRejectSuccessResponse.class);
    }

}
