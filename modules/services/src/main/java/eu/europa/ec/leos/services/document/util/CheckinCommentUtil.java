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
package eu.europa.ec.leos.services.document.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import eu.europa.ec.leos.model.action.CheckinCommentVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class CheckinCommentUtil {
    
    private static final Logger LOG = LoggerFactory.getLogger(CheckinCommentUtil.class);
    
    public static CheckinCommentVO getJavaObjectFromJson(String json) {
        CheckinCommentVO obj;
        ObjectMapper mapper = new ObjectMapper();
        try {
            obj = mapper.readValue(Strings.nullToEmpty(json), CheckinCommentVO.class);
        } catch (IOException e) {
            obj = new CheckinCommentVO(Strings.nullToEmpty(json)); // show the (non json) string as title
            LOG.trace("Cannot convert to a CheckinComment java object from json: " + json);
        }
        return obj;
    }
    
    public static String getJsonObject(CheckinCommentVO obj) {
        String json = "";
        ObjectMapper mapper = new ObjectMapper();
        try {
            json = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot build JSON string for CheckinCommentVO object: " + obj);
        }
        
        return json;
    }
}
