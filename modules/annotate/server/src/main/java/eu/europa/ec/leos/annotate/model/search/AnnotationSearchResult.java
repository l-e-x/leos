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
import eu.europa.ec.leos.annotate.model.entity.Annotation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * class for hosting various results from running a search for annotations
 */
public class AnnotationSearchResult {

    private List<Annotation> items;
    private SearchModel searchModelUsed;
    private long totalItems;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    public AnnotationSearchResult() {
        items = new ArrayList<Annotation>();
    }

    // -------------------------------------
    // Shortcut functions for found items
    // -------------------------------------
    public boolean isEmpty() {

        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    // -------------------------------------
    // Getters & setters
    // -------------------------------------
    public List<Annotation> getItems() {
        return items;
    }

    public void setItems(final List<Annotation> items) {
        this.items = items;
    }

    public SearchModel getSearchModelUsed() {
        return searchModelUsed;
    }

    public void setSearchModelUsed(final SearchModel searchModelUsed) {
        this.searchModelUsed = searchModelUsed;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(final long totalItems) {
        this.totalItems = totalItems;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(items, searchModelUsed);
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
        final AnnotationSearchResult other = (AnnotationSearchResult) obj;
        return Objects.equals(this.items, other.items) &&
                Objects.equals(this.searchModelUsed, other.searchModelUsed);
    }
}
