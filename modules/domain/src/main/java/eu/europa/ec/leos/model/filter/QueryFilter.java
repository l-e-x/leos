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
package eu.europa.ec.leos.model.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class QueryFilter {

    public static final String SORT_DESCENDING = "DESC";
    public static final String SORT_ASCENDING = "ASC";

    private final List<Filter> filters = new ArrayList<>();
    private final List<SortOrder> sortOrders = new ArrayList<>();

    public List<Filter> getFilters() {
        return Collections.unmodifiableList(filters);
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public boolean removeFilter(String filterKey) {
        return filters.removeIf(filter -> Objects.equals(filter.key, filterKey));
    }

    public void removeAllFilters() {
        filters.clear();
    }

    public List<SortOrder> getSortOrders() {
        return Collections.unmodifiableList(sortOrders);
    }

    public void addSortOrder(SortOrder sortOrder) {
        sortOrders.add(sortOrder);
    }

    public boolean removeSortOrder(String key) {
        return sortOrders.removeIf(sortOrder -> Objects.equals(sortOrder.key, key));
    }

    public enum FilterType {
        Root("root"),//NO CMIS COLUMN
        actType("metadata:actType"),
        procedureType("metadata:procedureType"),
        docType("metadata:docType"),
        ref("metadata:ref"),
        template("leos:template"),
        docTemplate("leos:docTemplate"),

        category("leos:category"),
        language("leos:language"),
        role("leos:collaborators", "ANY"),
        title("leos:title"),
        versionLabel("leos:versionLabel"),
        versionType("leos:versionType"),
        containedDocuments("leos:containedDocuments", "ANY"),
        
        cmisVersionLabel("cmis:versionLabel"),
        creationDate("cmis:creationDate"),
        lastModificationDate("cmis:lastModificationDate");

        private String cmisColumnName;
        private String multiColumnType;

        FilterType(String columnName) {
            this.cmisColumnName = columnName;
        }

        FilterType(String columnName, String multiColumnType) {
            this.cmisColumnName = columnName;
            this.multiColumnType = multiColumnType;
        }

        public static String getColumnType(String uiName) {
            for (FilterType column : values()) {
                if (column.name().equals(uiName)) {
                    return column.multiColumnType;
                }
            }
            throw new IllegalArgumentException("No Column Name found for the UI Name : " + uiName);
        }

        public static String getColumnName(String uiName) {
            for (FilterType column : values()) {
                if (column.name().equals(uiName)) {
                    return column.cmisColumnName;
                }
            }
            throw new IllegalArgumentException("No Column Name found for the UI Name : " + uiName);
        }
    }

    public static class Filter {
        public final String key;
        public final String[] value;
        public final String operator;
        public final boolean nullCheck;

        public Filter(String key, String operator, boolean nullCheck,  String... value) {
            this.key = key;
            this.value = value;
            this.operator = operator;
            this.nullCheck = nullCheck;
        }
        
        public String getKey() {
            return key;
        }
        
        public String getValue() {
            return value[0];
        }
    }

    public static class SortOrder {
        public final String key;
        public final String direction;

        public SortOrder(String key, String direction) {
            this.key = key;
            this.direction = direction;
        }
    }
}
