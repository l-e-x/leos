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
import eu.europa.ec.leos.annotate.model.search.Consts;

import java.util.Objects;

public class IncomingSearchOptions {

    // -----------------------------------------------------------
    // Fields
    // -----------------------------------------------------------

    // note: due to the preceding "_", the following property is not properly mapped by Spring
    // so it remains a separate incoming request parameter
    // private boolean _separate_replies = Boolean.parseBoolean(AnnotationSearchOptions.DEFAULT_SEARCH_SEPARATE_REPLIES);

    private int limit = Consts.DEFAULT_SEARCH_LIMIT;
    private int offset = Consts.DEFAULT_SEARCH_OFFSET;
    private String sort = Consts.DEFAULT_SEARCH_SORT;
    private String order = Consts.DEFAULT_SEARCH_ORDER;
    private String uri;
    private String url;
    private String user;
    private String group;
    private String tag;
    private String any;
    private String metadatasets;
    private String connectedEntity;
    private String mode;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    
    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public IncomingSearchOptions() {
        // default constructor
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public int getLimit() {
        return limit;
    }

    @Generated
    public void setLimit(final int limit) {
        this.limit = limit;
    }

    @Generated
    public int getOffset() {
        return offset;
    }

    @Generated
    public void setOffset(final int offset) {
        this.offset = offset;
    }

    @Generated
    public String getSort() {
        return sort;
    }

    @Generated
    public void setSort(final String sort) {
        this.sort = sort;
    }

    @Generated
    public String getOrder() {
        return order;
    }

    @Generated
    public void setOrder(final String order) {
        this.order = order;
    }

    @Generated
    public String getUri() {
        return uri;
    }

    @Generated
    public void setUri(final String uri) {
        this.uri = uri;
    }

    @Generated
    public String getUrl() {
        return url;
    }

    @Generated
    public void setUrl(final String url) {
        this.url = url;
    }

    @Generated
    public String getUser() {
        return user;
    }

    @Generated
    public void setUser(final String user) {
        this.user = user;
    }

    @Generated
    public String getGroup() {
        return group;
    }

    @Generated
    public void setGroup(final String group) {
        this.group = group;
    }

    @Generated
    public String getTag() {
        return tag;
    }

    @Generated
    public void setTag(final String tag) {
        this.tag = tag;
    }

    @Generated
    public String getAny() {
        return any;
    }

    @Generated
    public void setAny(final String any) {
        this.any = any;
    }

    @Generated
    public String getMetadatasets() {
        return metadatasets;
    }

    @Generated
    public void setMetadatasets(final String metadataSets) {
        this.metadatasets = metadataSets;
    }
    
    @Generated
    public String getConnectedEntity() {
        return connectedEntity;
    }
    
    @Generated
    public void setConnectedEntity(final String entity) {
        this.connectedEntity = entity;
    }
    
    @Generated
    public String getMode() {
        return mode;
    }
    
    @Generated
    public void setMode(final String mode) {
        this.mode = mode;
    }
    
    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(limit, offset, sort, order, uri, url, user, group,
                tag, any, metadatasets, connectedEntity, mode);
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
                Objects.equals(this.metadatasets, other.metadatasets) &&
                Objects.equals(this.connectedEntity, other.connectedEntity) &&
                Objects.equals(this.mode, other.mode);
    }
}
