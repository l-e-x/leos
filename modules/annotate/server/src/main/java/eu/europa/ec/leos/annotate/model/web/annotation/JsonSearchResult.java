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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.List;

public class JsonSearchResult {

    /**
     * Class representing the result of a search for annotations 
     */

    // the 'rows' are the individual Annotation objects found
    protected List<JsonAnnotation> rows;

    // the total lists the amount of annotations found (without replies in case replies are to be provided separately)
    @SuppressFBWarnings
    private int total;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    // default constructor required for deserialisation
    public JsonSearchResult() {
    }

    public JsonSearchResult(List<JsonAnnotation> items) {
        this.rows = items;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public List<JsonAnnotation> getRows() {
        return rows;
    }

    public void setRows(List<JsonAnnotation> rows) {
        this.rows = rows;
    }

    public int getTotal() {
        if (this.rows == null) {
            return 0;
        }
        return this.rows.size();
    }

}
