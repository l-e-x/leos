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
package eu.europa.ec.leos.annotate.model.web.websocket;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;

public class Clause {

    private static final Logger LOG = LoggerFactory.getLogger(Clause.class);

    private String field;
    private String operator;
    private Set<String> value;
    private boolean case_sensitive;
    private Object options;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public Clause() {
        // default constructor
    }

    public Clause(final String field, final String operator, final Set<String> value, final boolean case_sensitive, final Object options) {
        this.field = field;
        this.operator = operator;
        this.value = value;
        this.case_sensitive = case_sensitive;
        this.options = options;
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    
    @Generated
    public String getField() {
        return field;
    }

    @Generated
    public void setField(final String field) {
        this.field = field;
    }

    @Generated
    public String getOperator() {
        return operator;
    }

    @Generated
    public void setOperator(final String operator) {
        this.operator = operator;
    }

    @Generated
    public Set<String> getValue() {
        return value;
    }

    @Generated
    public void setValue(final Set<String> value) {
        this.value = value;
    }

    @Generated
    public boolean isCase_sensitive() {
        return case_sensitive;
    }

    @Generated
    public void setCase_sensitive(final boolean case_sensitive) {
        this.case_sensitive = case_sensitive;
    }

    @Generated
    public Object getOptions() {
        return options;
    }

    @Generated
    public void setOptions(final Object options) {
        this.options = options;
    }

    // -------------------------------------
    // useful methods
    // -------------------------------------

    public boolean matches(final Annotation annotation) {
        if (this.getField().equalsIgnoreCase("/uri")) {
            final String operand = annotation.getDocument().getUri();
            switch (operator) {
                case "one_of":
                    return this.getValue().contains(operand);
                // Other case are not implemented as client does not send those
                // equals", "matches", "lt", "le", "gt", "ge", "one_of", "first_of", "match_of",lene", "leng", "lenge", "lenl", "lenle"
                default:
                    return false;
            }

        } else {
            LOG.error("No other clause supported except uri = %");
        }
        return false;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final Clause clause = (Clause) obj;
        return Objects.equals(getField(), clause.getField()) &&
                Objects.equals(getOperator(), clause.getOperator()) &&
                Objects.equals(getValue(), clause.getValue());
    }

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(getField(), getOperator(), getValue());
    }

}