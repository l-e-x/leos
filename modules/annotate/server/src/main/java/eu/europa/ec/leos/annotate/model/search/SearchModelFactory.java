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
import eu.europa.ec.leos.annotate.services.MetadataMatchingService;
import eu.europa.ec.leos.annotate.services.MetadataService;
import eu.europa.ec.leos.annotate.services.impl.MetadataListHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    @Autowired
    private MetadataMatchingService metadataMatchingService;

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

                // metadata search should be used if there is _real_ metadata available
                // (normally, the list contains an empty default entry - which we don't count as _real_ data)
                boolean useMetadataSearch = !CollectionUtils.isEmpty(rso.getMetadataWithStatusesList());
                if (useMetadataSearch) {
                    // check that the list does not only consist of one default entry
                    // note: if there is more than one entry, we think they are valuable
                    useMetadataSearch = !(rso.getMetadataWithStatusesList().size() == 1 &&
                            rso.getMetadataWithStatusesList().get(0).isEmptyDefaultEntry());
                }

                if (useMetadataSearch) {

                    // -------------------------------------
                    // case EdiT with metadata
                    // -------------------------------------
                    resultingModel = getSearchModelEdiTByMetadata(rso);

                } else {

                    // -------------------------------------
                    // case EdiT.1
                    // -------------------------------------
                    resultingModel = getSearchModelEdiT1(rso);
                }
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

    // provide a search model like EdiT.1, but consider given metadata sets
    private SearchModel getSearchModelEdiTByMetadata(final ResolvedSearchOptions rso) {

        final List<Long> groupIds = groupService.getGroupIdsOfUser(rso.getExecutingUser()); // won't be empty as at least the default group is present

        // desired annotations may
        // a) belong to LEOS system, any group of the user
        final List<Metadata> metaLeos = metadataService.findMetadataOfDocumentSystemidGroupIds(rso.getDocument(), Authorities.EdiT, groupIds);

        // b) belong to ISC system, any group (independent of the user), and have responseStatus SENT
        final List<Metadata> metaIsc = metadataService.findMetadataOfDocumentSystemidSent(rso.getDocument(), Authorities.ISC);

        final List<MetadataIdsAndStatuses> resolvedCandidates = new ArrayList<MetadataIdsAndStatuses>();

        // note: we want all LEOS and all ISC items matching the given metadata sets, so we have to multiply them all
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {
            final List<Long> metadataIdsLeos = metadataMatchingService.getIdsOfMatchingMetadatas(metaLeos, smws.getMetadata());
            if (metadataIdsLeos != null) { // our function returns {@literal null} or a list with content, but no empty list
                resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIdsLeos, smws.getStatuses()));
            }

            final List<Long> metadataIdsIsc = metadataMatchingService.getIdsOfMatchingMetadatas(metaIsc, smws.getMetadata());
            if (metadataIdsIsc != null) { // our function returns {@literal null} or a list with content, but no empty list
                resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIdsIsc, smws.getStatuses()));
            }
        }

        return new SearchModelLeosAllGroups(rso, resolvedCandidates);
    }

    private SearchModel getSearchModelEdiT1(final ResolvedSearchOptions rso) {

        final List<Long> groupIds = groupService.getGroupIdsOfUser(rso.getExecutingUser()); // won't be empty as at least the default group is present

        // desired annotations may
        // a) belong to LEOS system, any group of the user
        final List<Metadata> metaLeos = metadataService.findMetadataOfDocumentSystemidGroupIds(rso.getDocument(), Authorities.EdiT, groupIds);

        // b) belong to ISC system, any group (independent of the user), and have responseStatus SENT
        final List<Metadata> metaIsc = metadataService.findMetadataOfDocumentSystemidSent(rso.getDocument(), Authorities.ISC);

        final List<Long> metadataIds = MetadataListHelper.getMetadataSetIds(metaLeos, metaIsc);
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

        final List<Long> metadataIds = MetadataListHelper.getMetadataSetIds(metaLeos, metaIsc);
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

        // we need:
        // a) all SENT items to this document/System ID having NORMAL status
        // a1) without those sentDeleted=true of the searched group if the user is member of the group
        // a2) without linked ones of the searched group if the user is member of the group
        // b) all IN_PREPARATION of the searched group if the user is member
        //
        // example for "of the searched group if the user is member": DIGIT user runs search for DIGIT; DIGIT user runs search for AGRI and is member of AGRI
        // counterexample: DIGIT user runs search for EMPL, but is not member of EMPL
        
        // find matching metadata - search for candidates associated to document and authority and being SENT
        final List<Metadata> metaCandidatesSent = metadataService.findMetadataOfDocumentSystemidSent(rso.getDocument(), Authorities.ISC);

        // retrieve the same, but being IN_PREPARATION
        List<Metadata> metaCandidatesInPrep = new ArrayList<Metadata>();
        if(rso.isUserIsMemberOfGroup()) {
            metaCandidatesInPrep = metadataService.findMetadataOfDocumentGroupSystemidInPreparation(rso.getDocument(), rso.getGroup(), Authorities.ISC);
        }
        
        if (metaCandidatesSent.isEmpty() && metaCandidatesInPrep.isEmpty()) {
            LOG.info("No corresponding metadata fulfilling search model ISC.1/ISC.2 found in DB (based on document/group/systemId");
            return null;
        }
        
        // there are candidates -> merge the lists
        final List<Metadata> metaCandidates = Stream.of(metaCandidatesSent, metaCandidatesInPrep).flatMap(item -> item == null ? null : item.stream())
                .collect(Collectors.toList());

        final List<MetadataIdsAndStatuses> resolvedCandidates = new ArrayList<MetadataIdsAndStatuses>();
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {

            // now check if these candidates have at least the metadata received via the search options
            final List<Long> metadataIds = metadataMatchingService.getIdsOfMatchingMetadatas(metaCandidates, smws.getMetadata());
            if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
                continue;
            }
            resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        // when this point is reached, all requested metadata matches - feed the search model
        return new SearchModelIscSingleGroup(rso, resolvedCandidates);
    }

    // note: probably this search model is not being used any more in production; could maybe be removed in the future
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
            final List<Long> metadataIds = metadataMatchingService.getIdsOfMatchingMetadatas(metaIsc, smws.getMetadata());
            if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
                continue;
            }
            resolvedCandidates.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        return new SearchModelIscAllGroups(rso, resolvedCandidates);
    }
}
