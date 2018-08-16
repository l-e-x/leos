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
package eu.europa.ec.leos.annotate.services.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.Serializable;
import java.util.Objects;

/**
 * Implementation of a {@link Pageable} interface that can be used to retrieve specific items from a database query;
 * in contrast to other Pageable interface implementations like e.g. {@link PageRequest}, this one allows to specify an offset
 * 
 * idea is based on https://stackoverflow.com/questions/25008472/pagination-in-spring-data-jpa-limit-and-offset
 */
public class OffsetBasedPageRequest implements Pageable, Serializable {

    private static final long serialVersionUID = -5889264988682884883L;

    private int limit; // the number of items to return
    private int offset; // number of items to skip
    private final Sort sort; // any sorting to be applied to the data

    // -------------------------------------
    // constructors
    // -------------------------------------
    /**
     * Creates a new {@link OffsetBasedPageRequest} with sort parameters applied.
     *
     * @param offset zero-based offset.
     * @param limit  the size of the elements to be returned.
     * @param sort   can be {@literal null}.
     */
    public OffsetBasedPageRequest(int offset, int limit, Sort sort) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset index must not be less than zero!");
        }

        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one!");
        }
        this.limit = limit;
        this.offset = offset;
        this.sort = sort;
    }

    // -------------------------------------
    // Pageable interface functions
    // -------------------------------------
    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public int getOffset() {
        // this is the important part as this property cannot be set through other Pageable implementations
        // like e.g. PageRequest
        return offset;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetBasedPageRequest(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    public OffsetBasedPageRequest previous() {
        return hasPrevious() ? new OffsetBasedPageRequest(getOffset() - getPageSize(), getPageSize(), getSort()) : this;
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new OffsetBasedPageRequest(0, getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return offset > limit;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------
    @Override
    public int hashCode() {
        return Objects.hash(limit, offset, sort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final OffsetBasedPageRequest other = (OffsetBasedPageRequest) obj;
        return Objects.equals(this.limit, other.limit) &&
                Objects.equals(this.offset, other.offset) &&
                Objects.equals(this.sort, other.sort);
    }
}
