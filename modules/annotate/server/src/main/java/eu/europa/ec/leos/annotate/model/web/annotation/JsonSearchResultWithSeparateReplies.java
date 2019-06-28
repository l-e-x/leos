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
package eu.europa.ec.leos.annotate.model.web.annotation;

import eu.europa.ec.leos.annotate.Generated;

import java.util.List;
import java.util.Objects;

public class JsonSearchResultWithSeparateReplies extends JsonSearchResult {

    /**
     * Class representing the result of a search for annotations, however with replies being listed separately
     */

    // list of annotation replies
    private List<JsonAnnotation> replies;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    public JsonSearchResultWithSeparateReplies() {
        // default constructor required for deserialisation
        super();
    }

    public JsonSearchResultWithSeparateReplies(final List<JsonAnnotation> annotations, final List<JsonAnnotation> replies, final long totalItems) {
        super(annotations, totalItems);

        this.replies = replies;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public List<JsonAnnotation> getReplies() {
        return replies;
    }

    public void setReplies(final List<JsonAnnotation> replies) {
        this.replies = replies;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(getTotal(), getRows(), replies);
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
        final JsonSearchResultWithSeparateReplies other = (JsonSearchResultWithSeparateReplies) obj;
        return Objects.equals(this.getTotal(), other.getTotal()) &&
                Objects.equals(this.getRows(), other.getRows()) &&
                Objects.equals(this.replies, other.replies);
    }
}
