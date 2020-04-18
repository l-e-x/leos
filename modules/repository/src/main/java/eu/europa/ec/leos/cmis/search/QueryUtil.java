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
package eu.europa.ec.leos.cmis.search;

import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.model.filter.QueryFilter.Filter;
import eu.europa.ec.leos.model.filter.QueryFilter.FilterType;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

class QueryUtil {

    private static final Logger LOG = LoggerFactory.getLogger(QueryUtil.class);

    static String formFilterClause(QueryFilter workspaceFilter) {
        LOG.trace("Form where clause for filter...");
        StringBuilder whereClauseFilter = new StringBuilder();
        for (QueryFilter.Filter filter : workspaceFilter.getFilters()) {
            if (whereClauseFilter.length() != 0) {
                whereClauseFilter.append(" AND ");
            }
            if(filter.nullCheck) {
                whereClauseFilter.append("(");
                whereClauseFilter.append(QueryFilter.FilterType.getColumnName(filter.key) );
                whereClauseFilter.append(" IS NULL OR ");
            }
            StringBuilder value = new StringBuilder("'");
            value.append(StringUtils.join(filter.value, "', '"));
            value.append("'");

            String operation;
            if ("ANY".equals(QueryFilter.FilterType.getColumnType(filter.key))) {
                if ("IN".equalsIgnoreCase(filter.operator)) {
                    operation = String.format("ANY %s IN (%s)",
                            QueryFilter.FilterType.getColumnName(filter.key),
                            value);
                } else {
                    operation = String.format("%s %s ANY %s",
                            value,
                            filter.operator,
                            QueryFilter.FilterType.getColumnName(filter.key)
                    );
                }
            } else {
                if ("IN".equalsIgnoreCase(filter.operator)) {
                    operation = String.format("%s IN (%s)",
                            QueryFilter.FilterType.getColumnName(filter.key),
                            value);
                } else {
                    operation = String.format("%s %s %s",
                            QueryFilter.FilterType.getColumnName(filter.key),
                            filter.operator,
                            value);
                }
            }
            whereClauseFilter.append(operation);
            if(filter.nullCheck) {
                whereClauseFilter.append(")");
            }
        }
        return whereClauseFilter.toString();
    }

    static String formSortClause(QueryFilter workspaceFilter) {
        return workspaceFilter.getSortOrders().stream()
                .map(sortOrder -> QueryFilter.FilterType.getColumnName(sortOrder.key) + " " + sortOrder.direction)
                .collect(Collectors.joining(" ,"));
    }

    static String getQuery(Folder folder, Set<LeosCategory> categories, boolean descendants, QueryFilter workspaceFilter) {
        String categoryStr = categories.stream()
                .map(a -> "'" + a.name()+"'")
                .collect(Collectors.joining(","));

        StringBuilder whereClause = new StringBuilder(
                String.format("leos:category IN (%s) AND %s('%s')",
                        categoryStr,
                        descendants? "IN_TREE": "IN_FOLDER",
                        folder.getId()));

        String filterClause = QueryUtil.formFilterClause(workspaceFilter);
        if(!filterClause.isEmpty()){
            whereClause.append(" AND ").append(filterClause);
        }
        return whereClause.toString();
    }
    
    static String getMajorVersionQueryString(String docRef) {
       StringBuilder queryBuilder =  new StringBuilder(CmisProperties.METADATA_REF.getId()).append(" = '").append(docRef)
               .append("' ")
               .append(" AND cmis:isMajorVersion = true ")
               .append(" order by cmis:creationDate DESC");
       return queryBuilder.toString();
    }
    
    static QueryFilter getRecentVersionsQuery(String docRef, String versionLabel) {
        QueryFilter filter = new QueryFilter();
        Filter f1 = new QueryFilter.Filter(FilterType.ref.name(), "=", false, docRef);
        filter.addFilter(f1);
        Filter f2 = new QueryFilter.Filter(FilterType.cmisVersionLabel.name(), ">", false, versionLabel);
        filter.addFilter(f2);
        filter.addSortOrder(new QueryFilter.SortOrder(QueryFilter.FilterType.creationDate.name(), QueryFilter.SORT_DESCENDING));
        return filter;
    }

    public static QueryFilter getMinorVersionsQueryFilter(String docRef, String currIntVersion, String prevIntVersion) {
        QueryFilter filter = new QueryFilter();
        QueryFilter.Filter f1 = new QueryFilter.Filter(QueryFilter.FilterType.ref.name(), "=", false, docRef);
        filter.addFilter(f1);
        QueryFilter.Filter f2 = new QueryFilter.Filter(QueryFilter.FilterType.cmisVersionLabel.name(), "<", false, currIntVersion);
        filter.addFilter(f2);
        QueryFilter.Filter f3 = new QueryFilter.Filter(QueryFilter.FilterType.cmisVersionLabel.name(), ">", false, prevIntVersion);
        filter.addFilter(f3);
        filter.removeSortOrder(FilterType.creationDate.name());
        filter.addSortOrder(new QueryFilter.SortOrder(QueryFilter.FilterType.creationDate.name(), QueryFilter.SORT_DESCENDING));
        return filter;
    }

}
