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

public class JsonSearchResult {

    /**
     * Class representing the result of a search for annotations 
     */

    // the 'rows' are the individual Annotation objects found
    protected List<JsonAnnotation> rows;

    // the total lists the amount of annotations found (without replies in case replies are to be provided separately)
    private long total;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    public JsonSearchResult() {
        // default constructor required for deserialisation
    }

    public JsonSearchResult(final List<JsonAnnotation> items, final long totalItems) {
        this.rows = items;
        this.total = totalItems;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public List<JsonAnnotation> getRows() {
        return rows;
    }

    public void setRows(final List<JsonAnnotation> rows) {
        this.rows = rows;
    }

    public long getTotal() {
        return this.total;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(total, rows);
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
        final JsonSearchResult other = (JsonSearchResult) obj;
        return Objects.equals(this.total, other.total) &&
                Objects.equals(this.rows, other.rows);
    }
}
