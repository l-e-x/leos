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
package eu.europa.ec.leos.annotate.model;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;

import java.util.List;
import java.util.Objects;

/**
 * simple POJO hosting a list of {@link Metadata} IDs and a list of {@link AnnotationStatus} items
 * required for search
 */
public class MetadataIdsAndStatuses {

    // -------------------------------------
    // private properties
    // -------------------------------------
    private final List<Long> metadataIds;
    private final List<AnnotationStatus> statuses;

    // -------------------------------------
    // Constructor
    // -------------------------------------

    public MetadataIdsAndStatuses(final List<Long> metadataIds, final List<AnnotationStatus> statuses) {
        this.metadataIds = metadataIds;
        this.statuses = statuses;
    }

    // -------------------------------------
    // Getters
    // -------------------------------------

    @Generated
    public List<Long> getMetadataIds() {
        return metadataIds;
    }

    @Generated
    public List<AnnotationStatus> getStatuses() {
        return statuses;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(metadataIds, statuses);
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
        final MetadataIdsAndStatuses other = (MetadataIdsAndStatuses) obj;
        return Objects.equals(this.metadataIds, other.metadataIds) &&
                Objects.equals(this.statuses, other.statuses);
    }
}
