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
package eu.europa.ec.leos.annotate.model.web;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;

import java.util.Objects;

public class IncomingSearchOptions {

    // -----------------------------------------------------------
    // Fields
    // -----------------------------------------------------------

    // note: due to the preceding "_", the following property is not properly mapped by Spring
    // so it remains a separate incoming request parameter
    // private boolean _separate_replies = Boolean.parseBoolean(AnnotationSearchOptions.DEFAULT_SEARCH_SEPARATE_REPLIES);

    private int limit = Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_LIMIT);
    private int offset = Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET);
    private String sort = AnnotationSearchOptions.DEFAULT_SEARCH_SORT;
    private String order = AnnotationSearchOptions.DEFAULT_SEARCH_ORDER;
    private String uri;
    private String url;
    private String user;
    private String group;
    private String tag;
    private String any;
    private String metadatasets;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public IncomingSearchOptions() {
        // default constructor
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    public int getLimit() {
        return limit;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(final String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(final String order) {
        this.order = order;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
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

    public String getTag() {
        return tag;
    }

    public void setTag(final String tag) {
        this.tag = tag;
    }

    public String getAny() {
        return any;
    }

    public void setAny(final String any) {
        this.any = any;
    }

    public String getMetadatasets() {
        return metadatasets;
    }

    public void setMetadatasets(final String metadataSets) {
        this.metadatasets = metadataSets;
    }
    
    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(limit, offset, sort, order, uri, url, user, group,
                tag, any, metadatasets);
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
        final IncomingSearchOptions other = (IncomingSearchOptions) obj;
        return Objects.equals(this.limit, other.limit) &&
                Objects.equals(this.offset, other.offset) &&
                Objects.equals(this.sort, other.sort) &&
                Objects.equals(this.order, other.order) &&
                Objects.equals(this.uri, other.uri) &&
                Objects.equals(this.url, other.url) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.group, other.group) &&
                Objects.equals(this.tag, other.tag) &&
                Objects.equals(this.any, other.any) &&
                Objects.equals(this.metadatasets, other.metadatasets);
    }
}
