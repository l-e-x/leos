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

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Filter {

    private static final Logger LOG = LoggerFactory.getLogger(Filter.class);

    private String match_policy;
    private Map<String, Boolean> actions;
    private List<Clause> clauses;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public Filter() {
        // default constructor
    }

    public Filter(final String match_policy, final Map<String, Boolean> actions, final List<Clause> clauses) {
        this.match_policy = match_policy;
        this.actions = actions;
        this.clauses = clauses;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public String getMatch_policy() {
        return match_policy;
    }

    @Generated
    public void setMatch_policy(final String match_policy) {
        this.match_policy = match_policy;
    }

    @Generated
    public Map<String, Boolean> getActions() {
        return actions;
    }

    @Generated
    public void setActions(final Map<String, Boolean> actions) {
        this.actions = actions;
    }

    @Generated
    public List<Clause> getClauses() {
        return clauses;
    }

    @Generated
    public void setClauses(final List<Clause> clauses) {
        this.clauses = clauses;
    }

    // -----------------------------------------------------------
    // Useful methods
    // -----------------------------------------------------------

    public boolean matches(final Annotation annotation) {
        if (annotation == null) {
            return false;
        }

        // Here, We could also use action to match but we are not supporting every scenario
        for (final Clause c : getClauses()) {
            final boolean match = c.matches(annotation);
            switch (match_policy) {
                case "include_any":
                    if (match) {
                        return true;
                    }
                    break;
                case "include_all":
                    if (!match) {
                        return false;
                    }
                    break;
                case "exclude_any":
                case "exclude_all":
                default:
                    LOG.error("Unsupported clause {}", match_policy);
                    break;
            }
        }
        return false;
    }

    // -----------------------------------------------------------
    // equals and hashCode
    // -----------------------------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(match_policy, actions, clauses);
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
        final Filter other = (Filter) obj;
        return Objects.equals(this.match_policy, other.match_policy) &&
                Objects.equals(this.actions, other.actions) &&
                Objects.equals(this.clauses, other.clauses);
    }

}
/* Schema of object
    "type": "object",
    "properties": {
        "name": {"type": "string", "optional": True},
        "match_policy": {
            "type": "string",
            "enum": ["include_any", "include_all",
                     "exclude_any", "exclude_all"]
        },
        "actions": {
            "create": {"type": "boolean", "default":  True},
            "update": {"type": "boolean", "default":  True},
            "delete": {"type": "boolean", "default":  True},
        },
        "clauses": {
            "type": "array",
            "items": {
                "field": {"type": "string", "format": "json-pointer"},
                "operator": {
                    "type": "string",
                    "enum": ["equals", "matches", "lt", "le", "gt", "ge",
                             "one_of", "first_of", "match_of",
                             "lene", "leng", "lenge", "lenl", "lenle"]
                },
                "value": "object",
                "options": {"type": "object", "default": {}}
            }
        },
    },
    "required": ["match_policy", "clauses", "actions"]
}
 */
