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

import java.util.List;

public class JsonSearchResultWithSeparateReplies extends JsonSearchResult {

    /**
     * Class representing the result of a search for annotations, however with replies being listed separately
     */

    // list of annotation replies
    private List<JsonAnnotation> replies;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    // default constructor required for deserialisation
    public JsonSearchResultWithSeparateReplies() {
    }
    
    public JsonSearchResultWithSeparateReplies(List<JsonAnnotation> annotations, List<JsonAnnotation> replies) {
        super(annotations);
        
        this.replies = replies;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public List<JsonAnnotation> getReplies() {
        return replies;
    }

    public void setReplies(List<JsonAnnotation> replies) {
        this.replies = replies;
    }
}
