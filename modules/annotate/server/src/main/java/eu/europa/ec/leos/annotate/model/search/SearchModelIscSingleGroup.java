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

import eu.europa.ec.leos.annotate.model.MetadataIdsAndStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Search model class for search model ISC.1 and ISC.2: search for ISC system with single, specific group
 * 
 * note: meanwhile, this class is identical to its base class; could be combined once search model development has settled
 */
public class SearchModelIscSingleGroup extends SearchModel {

    private static final Logger LOG = LoggerFactory.getLogger(SearchModelIscSingleGroup.class);

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public SearchModelIscSingleGroup(final ResolvedSearchOptions rso, final List<MetadataIdsAndStatuses> metadataIds) {
        super(rso, metadataIds, Consts.SearchModelMode.ConsiderUserMembership);
        this.hasPostFiltering = true;
        this.addDeletedHistoryItems = true;
    }

    /**
     *  for this search model, we have to filter out annotations:
     *  - if the items with highest responseVersion v_high of the user's group have responseStatus IN_PREPARATION,
     *    we have to filter out linked annotations having responseVersion v_high-1 and responseStatus SENT
     *  - if the items with highest responseVersion v_high of the user's group have responseStatus SENT, 
     *    we don't need to do anything as this case is working by design
     */
    @Override
    public List<Annotation> postFilterSearchResults(final List<Annotation> foundItems) {

        // extract the responseVersions and find their maximum
        final Optional<Long> maxVersionOpt = foundItems.stream().map(ann -> ann.getMetadata().getResponseVersion())
                .max(Comparator.comparing(Long::valueOf));

        long maxVersion;
        if (maxVersionOpt.isPresent()) {
            maxVersion = maxVersionOpt.get();
        } else {
            LOG.info("No need to do post-filtering on ISC search model: no responseVersion(s) found");
            return foundItems;
        }

        if (maxVersion <= 0) { // sometimes -1 is used by ISC for querying, but shouldn't be in the database
            LOG.info("No need to do post-filtering on ISC search model: maximum responseVersion is -1");
            return foundItems;
        }

        final List<String> itemsToRemove = getItemIdsToRemove(foundItems, maxVersion);

        if (itemsToRemove.isEmpty()) {
            // nothing to be filtered out
            return foundItems;
        }

        // now we really have to filter out
        int filteredOut = 0;
        final List<Annotation> filteredItems = new ArrayList<Annotation>();
        for (final Annotation ann : foundItems) {

            if (itemsToRemove.contains(ann.getId())) {
                // this particular item is to be filtered out - do nothing
                filteredOut++;
                continue;
            } else {
                // item passes filter -> add to result list
                filteredItems.add(ann);
            }
        }

        LOG.info("{} items filtered out", filteredOut);
        return filteredItems;
    }

    // identify the items that should be removed (=filtered out)
    private List<String> getItemIdsToRemove(final List<Annotation> foundItems, final long maxVersion) {

        final List<String> itemsToRemove = new ArrayList<String>();

        // now we look for items of this highest version, having responseStatus IN_PREPARATION and a linkedAnnot set
        for (final Annotation ann : foundItems) {

            final Metadata meta = ann.getMetadata();

            if (meta.getResponseVersion() == maxVersion &&
                    meta.getResponseStatus() == Metadata.ResponseStatus.IN_PREPARATION &&
                    !StringUtils.isEmpty(ann.getLinkedAnnotationId())) {

                // match: the linked annotation should be removed
                itemsToRemove.add(ann.getLinkedAnnotationId());
            }
        }

        return itemsToRemove;
    }
}