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
package eu.europa.ec.leos.annotate.services.impl;

import eu.europa.ec.leos.annotate.model.entity.Metadata;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * collection of dumb helper methods around lists of {@link Metadata}
 */
public final class MetadataListHelper {

    private MetadataListHelper() {
        // private constructor in order to make class be considered a Utility class
    }
    
    /**
     * from a given list of {@link Metadata} objects, return their IDs
     * 
     * @param metaList 
     *        list of {@link Metadata} objects, may even contain {@literal null} entries
     * 
     * @return list of the valid items' IDs without duplicates; returns {@literal null} if there are no results
     */
    public static List<Long> getMetadataSetIds(final List<Metadata> metaList) {

        if (metaList == null || metaList.isEmpty()) {
            return null;
        }

        final List<Long> idList = metaList.stream().filter(meta -> meta != null)
                .map(Metadata::getId).distinct().collect(Collectors.toList());
        if (!idList.isEmpty()) {
            return idList;
        }

        return null;
    }

    /**
     * wrapper around the {@link getMetadataSetIds} method, but makes sure to always return a non-null result
     * 
     * @param metaList 
     *        list of Metadata objects
     * 
     * @return list of the valid items' IDs; in worst case, returns an empty list
     */
    @Nonnull
    public static List<Long> getNonNullMetadataSetIds(final List<Metadata> metaList) {

        final List<Long> result = getMetadataSetIds(metaList);
        if (result != null) {
            return result;
        }
        return new ArrayList<Long>();
    }

    /**
     * merges two given list of {@link Metadata} objects and returns the disjunct set of IDs
     * 
     * @param metaList1 
     *        first list of {@link Metadata} objects, may even contain {@literal null} entries
     * @param metaList2 
     *        second list of {@link Metadata} objects, may even contain {@literal null} entries
     * 
     * @return list of the valid items' IDs without duplicates; returns {@literal null} if there are no results
     */
    public static List<Long> getMetadataSetIds(final List<Metadata> metaList1, final List<Metadata> metaList2) {

        final List<Long> firstIds = getNonNullMetadataSetIds(metaList1);
        final List<Long> secondIds = getNonNullMetadataSetIds(metaList2);

        // merge the streams - by construction, their elements should be disjoint, but be sure...
        final List<Long> merged = Stream.concat(firstIds.stream(), secondIds.stream()).distinct().collect(Collectors.toList());

        if (merged.isEmpty()) {
            return null;
        }
        return merged;
    }

}
