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
package eu.europa.ec.leos.annotate.model.search;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.AnnotationComparator;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.IncomingSearchOptions;
import eu.europa.ec.leos.annotate.model.web.helper.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
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
    @SuppressWarnings("PMD.LongVariable")
    public static final String DEFAULT_SEARCH_SEPARATE_REPLIES = "false";
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

    // list of sets of metadata with statuses requested
    @SuppressWarnings("PMD.LongVariable")
    private List<SimpleMetadataWithStatuses> metadataMapsWithStatusesList;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    // constructor with mandatory search parameters
    public AnnotationSearchOptions(final String uri, final String group,
            final boolean separateReplies,
            final int limit, final int offset,
            final String order, final String sort) {

        this.separateReplies = separateReplies;

        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot search as given URI is invalid", e);
        }

        this.group = group;

        setItemLimitAndOffset(limit, offset);

        setSortColumnIntern(sort);
        this.order = Direction.valueOf(order.toUpperCase(Locale.ENGLISH));
    }

    /**
     * creation of new search options based on {@link IncomingSearchOptions} instance
     * 
     * @param inOpts instance of {@link IncomingSearchOptions}
     * @param separate_replies flag indicating whether separate replies are wanted (separate options since it isn't properly mapped by Spring)
     * @return initialised {@link AnnotationSearchOptions} instance
     * @throws IllegalArgumentException if input search options are {@literal null} 
     */
    public static AnnotationSearchOptions fromIncomingSearchOptions(final IncomingSearchOptions incOpts, final boolean separate_replies) {
        
        Assert.notNull(incOpts, "Valid IncomingSearchOptions required");
        
        final AnnotationSearchOptions options = new AnnotationSearchOptions(incOpts.getUri(),
                incOpts.getGroup(), separate_replies,
                incOpts.getLimit(), incOpts.getOffset(),
                incOpts.getOrder(), incOpts.getSort());
        
        if (incOpts.getUrl() != null && !incOpts.getUrl().isEmpty()) {
            options.setUri(incOpts.getUrl());
        }
        // optional parameters
        options.setUser(incOpts.getUser());

        String metadataProcessed = incOpts.getMetadatasets();
        if (!StringUtils.isEmpty(metadataProcessed)) {
            // at least during test scenarios, we experienced problems when sending JSON metadata
            // with only one entry - therefore, we had encoded the curly brackets URL-conform,
            // and have to decode this again here
            metadataProcessed = metadataProcessed.replace("%7B", "{").replace("%7D", "}").replace("\\", "");
        }
        
        final List<SimpleMetadataWithStatuses> converted = JsonConverter.convertJsonToSimpleMetadataWithStatusesList(metadataProcessed);
        options.setMetadataMapsWithStatusesList(converted);
        
        return options;
    }
    
    // -------------------------------------
    // Getters & setters
    // -------------------------------------

    public boolean isSeparateReplies() {
        return separateReplies;
    }

    public void setSeparateReplies(final boolean separateReplies) {
        this.separateReplies = separateReplies;
    }

    public int getItemLimit() {
        return itemLimit;
    }

    public void setItemLimit(final int itemLimit) {
        this.itemLimit = itemLimit;
    }

    // function to set limit and offset at once, as they are related (offset depends on limit)
    public void setItemLimitAndOffset(final int limit, final int offset) {

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

    public void setItemOffset(final int itemOffset) {
        this.itemOffset = itemOffset;
    }

    public Direction getOrder() {
        return order;
    }

    public void setOrder(final Direction order) {
        this.order = order;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(final String sortColumn) {

        setSortColumnIntern(sortColumn);
    }
    
    // moved to separate function to avoid being overridden (PMD notification)
    private void setSortColumnIntern(final String sortColumn) {

        // check that given sort criteria is valid; ignore if invalid
        if (!StringUtils.isEmpty(sortColumn) && !AnnotationComparator.SORTABLE_COLUMN_NAMES.contains(sortColumn)) {
            LOG.error("Given sorting column '" + sortColumn + "' cannot be used for sorting; no sorting will be applied");
            this.sortColumn = "";
            return;
        }
        this.sortColumn = sortColumn;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public void setUri(final String uri) {
        try {
            this.uri = new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Given search URI is invalid", e);
        }
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(final String group) {
        this.group = group;
    }

    public List<SimpleMetadataWithStatuses> getMetadataMapsWithStatusesList() {
        return metadataMapsWithStatusesList;
    }

    public void setMetadataMapsWithStatusesList(final List<SimpleMetadataWithStatuses> metaList) {
        this.metadataMapsWithStatusesList = metaList;
    }
    
    // sets the statuses of all metadata sets defined
    public void setStatuses(final List<AnnotationStatus> statuses) {
        if(this.metadataMapsWithStatusesList == null) {
            this.metadataMapsWithStatusesList = new ArrayList<SimpleMetadataWithStatuses>();
        }
        if(CollectionUtils.isEmpty(this.metadataMapsWithStatusesList)) {
            this.metadataMapsWithStatusesList.add(new SimpleMetadataWithStatuses(null, null));
        }
        this.metadataMapsWithStatusesList.forEach(mmwsl -> mmwsl.setStatuses(statuses));
    }
    
    /**
     * @return the {@link Sort} object to be used for sorting database content
     * note: {@literal null} is returned when specified sorting criterion was invalid
     */
    public Sort getSort() {

        if (StringUtils.isEmpty(this.sortColumn)) {
            return null;
        }
        return new Sort(this.order, this.sortColumn);
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(separateReplies, itemLimit, itemOffset, order, sortColumn, uri, user, 
                group, metadataMapsWithStatusesList);
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

        final AnnotationSearchOptions other = (AnnotationSearchOptions) obj;
        return Objects.equals(this.separateReplies, other.separateReplies) &&
                Objects.equals(this.itemLimit, other.itemLimit) &&
                Objects.equals(this.itemOffset, other.itemOffset) &&
                Objects.equals(this.order, other.order) &&
                Objects.equals(this.sortColumn, other.sortColumn) &&
                Objects.equals(this.uri, other.uri) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.metadataMapsWithStatusesList, other.metadataMapsWithStatusesList);
    }
}
