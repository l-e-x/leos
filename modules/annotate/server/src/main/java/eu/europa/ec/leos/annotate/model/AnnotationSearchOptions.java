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
package eu.europa.ec.leos.annotate.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Objects;

public class AnnotationSearchOptions {

    /**
     *  class representing the options available for searching for annotations
     */

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationSearchOptions.class);

    // -------------------------------------
    // Default values for parameters, (also) used by controller
    // -------------------------------------
    public static final String DEFAULT_SEARCH_SEARCH_REPLIES = "false";
    public static final String DEFAULT_SEARCH_LIMIT = "20";
    public static final String DEFAULT_SEARCH_OFFSET = "0";
    public static final String DEFAULT_SEARCH_SORT = "updated";
    public static final String DEFAULT_SEARCH_ORDER = "desc";

    // -------------------------------------
    // Available properties
    // -------------------------------------

    // should replies be merged into the top-level annotations, or be listed separately
    private boolean separateReplies;

    // number of items and start index to be returned
    private int itemLimit = Integer.parseInt(DEFAULT_SEARCH_LIMIT), itemOffset = Integer.parseInt(DEFAULT_SEARCH_OFFSET);

    // sorting: column to be sorted, sorting order
    private Direction order = Direction.DESC;
    private String sortColumn;

    // URI of the document for which annotations are wanted
    private URI uri;

    // user login and group name being originators for the annotations
    private String user;
    private String group;

    // search for tags
    private String tag;

    // free parameter - available in hypothes.is API, but currently not used
    private String any;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    // constructor with mandatory search parameters
    public AnnotationSearchOptions(String uri, String group, boolean separateReplies, int limit, int offset, String order, String sort) {

        this.separateReplies = separateReplies;

        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot search as given URI is invalid", e);
        }

        this.group = group;

        setItemLimitAndOffset(limit, offset);

        setSortColumn(sort);
        this.order = Direction.valueOf(order.toUpperCase(Locale.ENGLISH));
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    public boolean isSeparateReplies() {
        return separateReplies;
    }

    public void setSeparateReplies(boolean separateReplies) {
        this.separateReplies = separateReplies;
    }

    public int getItemLimit() {
        return itemLimit;
    }

    public void setItemLimit(int itemLimit) {
        this.itemLimit = itemLimit;
    }

    // function to set limit and offset at once, as they are related (offset depends on limit)
    public void setItemLimitAndOffset(int limit, int offset) {

        if (limit < 0) {
            // negative value: we want to return all items - do this by specifying biggest possible value...
            this.itemLimit = Integer.MAX_VALUE;

            // ... and setting offset to 0 (thus avoid receiving only a page)
            this.itemOffset = 0;

        } else if (limit == 0) {

            // zero: use default values
            this.itemLimit = Integer.parseInt(DEFAULT_SEARCH_LIMIT);
            this.itemOffset = (offset < 0 ? Integer.parseInt(DEFAULT_SEARCH_OFFSET) : offset);

        } else {
            // any other positive value is accepted - no upper limit!
            this.itemLimit = limit;

            // take over the given offset; set it positive, if need be
            this.itemOffset = (offset < 0 ? Integer.parseInt(DEFAULT_SEARCH_OFFSET) : offset);
        }
    }

    public int getItemOffset() {
        return itemOffset;
    }

    public void setItemOffset(int itemOffset) {
        this.itemOffset = itemOffset;
    }

    public Direction getOrder() {
        return order;
    }

    public void setOrder(Direction order) {
        this.order = order;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {

        // check that given sort criteria is valid; ignore if invalid
        if (sortColumn != null && !sortColumn.isEmpty()) {
            if (!AnnotationComparator.SORTABLE_COLUMN_NAMES.contains(sortColumn)) {
                LOG.error("Given sorting column '" + sortColumn + "' cannot be used for sorting; no sorting will be applied");
                this.sortColumn = "";
                return;
            }
        }
        this.sortColumn = sortColumn;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public void setUri(String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Given search URI is invalid", e);
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getAny() {
        return any;
    }

    public void setAny(String any) {
        this.any = any;
    }

    /**
     * @return the {@link Sort} object to be used for sorting database content
     * note: {@literal null} is returned when specified sorting criterion was invalid
     */
    public Sort getSort() {

        if (this.sortColumn == null || this.sortColumn.isEmpty()) {
            return null;
        }
        return new Sort(this.order, this.sortColumn);
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(separateReplies, itemLimit, itemOffset, order, sortColumn, uri, user, group, tag, any);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final AnnotationSearchOptions other = (AnnotationSearchOptions) obj;
        return Objects.equals(this.separateReplies, other.separateReplies) &&
                Objects.equals(this.itemLimit, other.itemLimit) &&
                Objects.equals(this.itemOffset, other.itemOffset) &&
                Objects.equals(this.order, other.order) &&
                Objects.equals(this.sortColumn, other.sortColumn) &&
                Objects.equals(this.uri, other.uri) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.tag, other.tag) &&
                Objects.equals(this.any, other.any);
    }
}
