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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;

import javax.annotation.Nonnull;

import java.util.List;

/**
 * interface for matching functionality around {@link Metadata}
 */
public interface MetadataMatchingService {

    /**
     * find an exact match for a {@link Metadata} item in the database
     * 
     * @param document 
     *        associated {@link Document}
     * @param group 
     *        associated {@link Group}
     * @param systemId 
     *        associated system ID
     * @param otherMetadata 
     *        {@link Metadata} object containing key-value properties to be matched
     *        
     * @return found item, or {@literal null}
     */
    Metadata findExactMetadata(Document document, Group group, String systemId, Metadata otherMetadata);

    /**
     * find an exact match for a {@link Metadata} item in the database
     * 
     * @param document 
     *        associated {@link Document}
     * @param group 
     *        associated {@link Group}
     * @param systemId 
     *        associated system ID
     * @param otherMetadataProps 
     *        map of other key-value {@link Metadata} properties
     *        
     * @return found item, or {@literal null}
     */
    Metadata findExactMetadata(Document document, Group group, String systemId, SimpleMetadata otherMetadata);

    /**
     * find an exact match for a {@link Metadata} item in the database, however ignoring the "responseVersion"
     * 
     * @param document 
     *        associated {@link Document}
     * @param group 
     *        associated {@link Group}
     * @param systemId 
     *        associated system ID
     * @param otherMetadata 
     *        other {@link Metadata} properties to be matched
     *        
     * @return found item, or {@literal null}
     */
    List<Long> findExactMetadataWithoutResponseVersion(final Document document, final Group group, final String systemId,
            final Metadata otherMetadata);

    // helper functions
    /**
     * check if all items of a given set of metadata key/values are contained in a given database {@link Metadata} object
     * 
     * @param metadataRequired 
     *        set of required key/value pairs ({@link SimpleMetadata})
     * @param candidateMeta 
     *        {@link Metadata} instance to be inspected whether it contains all requested key/value pairs
     * @param checkVersion 
     *        flag indicating if versions don't have to be checked
     * 
     * @return flag indicating if candidate element contains (at least) the requested items
     */
    boolean areAllMetadataContainedInDbMetadata(SimpleMetadata metadataRequired, Metadata candidateMeta,
            boolean skipVersionCheck);

    /**
     * check if all items of a given set of metadata key/values are contained in a given database {@link Metadata} object
     * applies a comparison on the version
     * 
     * @param metadataRequired 
     *        set of required key/value pairs ({@link SimpleMetadata})
     * @param candidateMeta 
     *        {@link Metadata} instance to be inspected whether it contains all requested key/value pairs
     * 
     * @return flag indicating if candidate element contains (at least) the requested items
     */
    boolean areAllMetadataContainedInDbMetadata(SimpleMetadata metadataRequired, Metadata candidateMeta);

    /**
     * filter a given list of {@link Metadata} objects by a list of requested items ("OR filtering")
     *  
     * @param candidates 
     *        set of candidate {@link Metadata} items
     * @param requested
     *        list of requested sets of metadata that must match at least
     * 
     * @return {@literal null} if no candidates are given; list of IDs of the filtered candidates 
     */
    List<Long> getIdsOfMatchingMetadatas(List<Metadata> candidates, List<SimpleMetadata> requested);

    /**
     * filter a given list of {@link Metadata} objects by a list of requested items ("OR filtering")
     *  
     * @param candidates 
     *        set of candidate {@link Metadata} items
     * @param requested
     *        requested set of metadata that must match at least
     * 
     * @return {@literal null} if no candidates are given; list of IDs of the filtered candidates 
     */
    List<Long> getIdsOfMatchingMetadatas(List<Metadata> candidates, SimpleMetadata requested);

    /**
     * determines the current highest response version assigned to an annotation' ISC procedure
     * 
     * @param annot
     *        {@link Annotation} for which to determine the highest response version
     * @return found response version, or -1
     */
    long getHighestResponseVersion(Annotation annot);
    
    /**
     * checks that an IN_PREPARATION version is already existing for the given {@link Annotation} which is to be deleted,
     * or creates one
     * 
     * @param annot
     *        {@link Annotation} to be deleted
     * @return found (or newly created) IN_PREPARATION item
     */
    Metadata findOrCreateInPrepItemForAnnotToDelete(final Annotation annot);

    /**
     * filters a given list of {@link Metadata} items for those featuring a given ISC reference
     * 
     * @param metaCandidates
     *        {@link Metadata} list to be filtered
     * @return filtered list of items, at least an empty list
     */
    @Nonnull
    List<Metadata> filterByIscReference(final List<Metadata> metaCandidates, final String iscReference);
}
