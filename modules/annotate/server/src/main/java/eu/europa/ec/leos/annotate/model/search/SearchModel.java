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
import eu.europa.ec.leos.annotate.model.MetadataIdsAndStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.search.Consts.SearchModelMode;
import eu.europa.ec.leos.annotate.repository.impl.AnnotationSearchSpec;
import org.springframework.data.jpa.domain.Specification;

import javax.annotation.Nonnull;

import java.util.List;
import java.util.Objects;

/**
 * simple class for our search models - holds the most important properties like resolved search options and metadata sets
 * also provides access to the (technical) search specification to be used by applying the search model
 * 
 * note: previously, this was an abstract class and the derived search models classes were initialising their own associate search specification class;
 *       however, they all ended up being identical, so the SearchModel was transformed to be a wrapper around the (remaining generic) search specification 
 */
public class SearchModel {

    // -------------------------------------
    // fields, accessible by derived classes
    // -------------------------------------

    private final ResolvedSearchOptions rso;
    private final Specification<Annotation> searchSpec;
    private final List<MetadataIdsAndStatuses> metaAndStatus;
    
    // flag indicating if the respective derived model class uses post-filtering functionality
    protected boolean hasPostFiltering;

    // flag indicating if the respective derived model class should add deleted items in historical sets
    protected boolean addDeletedHistoryItems;
    
    // -------------------------------------
    // Constructors
    // -------------------------------------
    public SearchModel(final ResolvedSearchOptions rso, final List<MetadataIdsAndStatuses> metaAndStatus,
            final SearchModelMode searchModeToUse) {

        this.rso = rso;
        this.metaAndStatus = metaAndStatus;

        this.searchSpec = new AnnotationSearchSpec(
                rso.getExecutingUser().getId(),
                rso.getFilterUser() == null ? null : rso.getFilterUser().getId(),
                metaAndStatus,
                rso.getGroup(), searchModeToUse);
        this.hasPostFiltering = false;
    }

    /**
     * apply a post-filtering on a given list of annotations
     * not needed by default, will be overridden only in search models requiring it
     * 
     * @param foundItems
     *        list of items to be post-filtered
     * @return filtered list
     */
    @Nonnull
    public List<Annotation> postFilterSearchResults(final List<Annotation> foundItems) {
        
        return foundItems;
    }
    
    // -------------------------------------
    // Getters
    // -------------------------------------
    @Generated
    public Specification<Annotation> getSearchSpecification() {
        return this.searchSpec;
    }

    @Generated
    public List<MetadataIdsAndStatuses> getMetadataAndStatusesList() {
        return this.metaAndStatus;
    }

    @Generated
    public boolean isHasPostFiltering() {
        return this.hasPostFiltering;
    }
    
    @Generated
    public boolean isAddDeletedHistoryItems() {
        return this.addDeletedHistoryItems;
    }
    
    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(rso, searchSpec, metaAndStatus, hasPostFiltering);
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
        final SearchModel other = (SearchModel) obj;
        return Objects.equals(this.rso, other.rso) &&
                Objects.equals(this.searchSpec, other.searchSpec) &&
                Objects.equals(this.metaAndStatus, other.metaAndStatus) &&
                Objects.equals(this.hasPostFiltering, other.hasPostFiltering);
    }
}
