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

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.model.MetadataIdsAndStatuses;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.MetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class SearchModelFactory {

    private static final Logger LOG = LoggerFactory.getLogger(SearchModelFactory.class);

    // -------------------------------------
    // Required services
    // -------------------------------------
    @Autowired
    private GroupService groupService;

    @Autowired
    private MetadataService metadataService;

    // -------------------------------------
    // Main factory function
    // -------------------------------------

    /**
     * given a set of validated search options, determine which search model is to be used
     * 
     * @param rso a {@link ResolvedSearchOptions} instance containing all search parameters
     * 
     * @return matching {@link SearchModel} instance, or {@literal null} (e.g. when no related {@link Metadata} is found)
     */
    public SearchModel getSearchModel(final ResolvedSearchOptions rso) {

        Assert.notNull(rso, "No resolved search options supplied to determine search model");

        // note: when calling from the AnnotationService, the token is set; unit/integration tests have to make sure to set it
        String authority = "";
        if (rso.getExecutingUserToken() == null) {
            LOG.warn("Cannot determine search model without knowing the authority - will result in fallback");
        } else {
            authority = rso.getExecutingUserToken().getAuthority().toUpperCase(Locale.ENGLISH);
        }

        final String defaultGroupName = groupService.getDefaultGroupName();
        SearchModel resultingModel = null;

        if (Authorities.isLeos(authority)) {

            if (defaultGroupName.equals(rso.getGroup().getName())) {

                // -------------------------------------
                // case EdiT.1
                // -------------------------------------
                resultingModel = getSearchModelEdiT1(rso);

            } else {

                // -------------------------------------
                // case EdiT.2
                // -------------------------------------
                resultingModel = getSearchModelEdiT2(rso);
            }

        } else if (Authorities.isIsc(authority)) {

            if (defaultGroupName.equals(rso.getGroup().getName())) {

                // -------------------------------------
                // case ISC.3
                // -------------------------------------
                resultingModel = getSearchModelIsc3(rso);

            } else {

                // -------------------------------------
                // cases ISC.1 and ISC.2
                // -------------------------------------
                resultingModel = getSearchModelIsc1And2(rso);
            }

        } else {

            LOG.warn("No search model defined for unexpected authority {}", authority);
        }
        return resultingModel;
    }

    private SearchModel getSearchModelEdiT1(final ResolvedSearchOptions rso) {

        final List<Long> groupIds = groupService.getGroupIdsOfUser(rso.getExecutingUser()); // won't be empty as at least the default group is present

        // desired annotations may
        // a) belong to LEOS system, any group of the user
        final List<Metadata> metaLeos = metadataService.findMetadataOfDocumentSystemidGroupIds(rso.getDocument(), Authorities.EdiT, groupIds);

        // b) belong to ISC system, any group (independent of the user), and have responseStatus SENT
        final List<Metadata> metaIsc = metadataService.findMetadataOfDocumentSystemidSent(rso.getDocument(), Authorities.ISC);

        final List<Long> metadataIds = metadataService.getMetadataSetIds(metaLeos, metaIsc);
        if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
            LOG.info("No corresponding metadata fulfilling search model EdiT.1 found in DB");
            return null;
        }

        final List<MetadataIdsAndStatuses> resolvedCandidates = new ArrayList<MetadataIdsAndStatuses>();
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {
            resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        return new SearchModelLeosAllGroups(rso, resolvedCandidates);
    }

    private SearchModel getSearchModelEdiT2(final ResolvedSearchOptions rso) {

        // desired annotations may
        // a) belong to LEOS system
        final List<Metadata> metaLeos = metadataService.findMetadataOfDocumentGroupSystemid(rso.getDocument(), rso.getGroup(), Authorities.EdiT);

        // b) belong to ISC system and have responseStatus SENT
        final List<Metadata> metaIsc = metadataService.findMetadataOfDocumentGroupSystemidSent(rso.getDocument(),
                rso.getGroup(), Authorities.ISC);

        final List<Long> metadataIds = metadataService.getMetadataSetIds(metaLeos, metaIsc);
        if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
            LOG.info("No corresponding metadata fulfilling search model EdiT.2 found in DB");
            return null;
        }

        final List<MetadataIdsAndStatuses> resolvedCandidates = new ArrayList<MetadataIdsAndStatuses>();
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {
            resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        return new SearchModelLeosSingleGroup(rso, resolvedCandidates);
    }

    private SearchModel getSearchModelIsc1And2(final ResolvedSearchOptions rso) {

        // find matching metadata - search for candidates associated to document, group and authority
        final List<Metadata> metaCandidates = metadataService.findMetadataOfDocumentGroupSystemid(rso.getDocument(), rso.getGroup(), Authorities.ISC);
        if (metaCandidates.isEmpty()) {
            LOG.info("No corresponding metadata fulfilling search model ISC.1/ISC.2 found in DB (based on document/group/systemId");
            return null;
        }

        final List<MetadataIdsAndStatuses> resolvedCandidates = new ArrayList<MetadataIdsAndStatuses>();
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {

            // now check if these candidates have at least the metadata received via the search options
            final List<Long> metadataIds = metadataService.getIdsOfMatchingMetadatas(metaCandidates, smws.getMetadata());
            if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
                continue;
            }
            resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        // when this point is reached, all requested metadata matches - feed the search model
        return new SearchModelIscSingleGroup(rso, resolvedCandidates);
    }

    private SearchModel getSearchModelIsc3(final ResolvedSearchOptions rso) {

        // desired annotations belong to ISC system, any group of the user, and have responseStatus SENT
        // -> retrieve all groups of the user...
        final List<Long> groupIds = groupService.getGroupIdsOfUser(rso.getExecutingUser()); // won't be empty as at least the default group is present

        // ... find all associate metadata sets...
        final List<Metadata> metaIsc = metadataService.findMetadataOfDocumentSystemidGroupIds(rso.getDocument(),
                Authorities.ISC, groupIds);
        if (metaIsc.isEmpty()) { // service returns empty list when nothing is found, but not {@literal null}
            LOG.info("No metadata to the document of any of the user's groups found");
            return null;
        }

        final List<MetadataIdsAndStatuses> resolvedCandidates = new ArrayList<MetadataIdsAndStatuses>();
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {

            // ... filter out those sets that match the given criteria...
            final List<Long> metadataIds = metadataService.getIdsOfMatchingMetadatas(metaIsc, smws.getMetadata());
            if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
                continue;
            }
            resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        return new SearchModelIscAllGroups(rso, resolvedCandidates);
    }
}
