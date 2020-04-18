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

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.model.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.search.Consts.SearchUserType;
import eu.europa.ec.leos.annotate.model.web.PublishContributionsRequest;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.services.*;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for annotation response status update 
 */
@Service
public class StatusUpdateServiceImpl implements StatusUpdateService {

    private static final Logger LOG = LoggerFactory.getLogger(StatusUpdateServiceImpl.class);
    private static final String ERROR_USERINFO_MISSING = "Required user information missing.";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;
    
    @Autowired
    private AnnotationPermissionService annotPermService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private MetadataMatchingService metadataMatchingService;

    // -------------------------------------
    // Service functionality
    // -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    @SuppressWarnings("PMD.ConfusingTernary")
    public ResponseStatusUpdateResult updateAnnotationResponseStatus(final StatusUpdateRequest updateRequest, final UserInformation userInfo)
            throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        Assert.notNull(userInfo, ERROR_USERINFO_MISSING);
        Assert.notNull(updateRequest, "Required information on how to update the response status is missing");

        final List<Metadata> metadataUpdated = metadataService.updateMetadata(updateRequest, userInfo);
        final boolean metadataItemsUpdated = !metadataUpdated.isEmpty();

        // we send a websocket update for all annotations associated to this metadata
        final List<Long> metadataIds = metadataUpdated.stream().map(Metadata::getId).collect(Collectors.toList());
        final List<Annotation> annots = annotService.findByMetadataAndStatus(metadataIds, AnnotationStatus.NORMAL);

        // now we need to take a closer look at the annotations being updated:
        // if they are linked to another annotation, we have to
        // - soft-delete the linked annotation
        // - remove the link from the updated annotation to the linked annotation (other link direction is kept)
        final List<Annotation> annotsToSave = new ArrayList<Annotation>();
        final List<String> annotsToDelete = new ArrayList<String>();

        checkLinkedAnnotations(annots, annotsToSave, annotsToDelete);

        // if no items were found for updating, we create an artificial item in order to evaluate the "sentDeleted" items
        if (metadataUpdated.isEmpty()) {
            LOG.debug("There was no metadata to update; add dummy record to check for sentDeleted items to be processed");
            final Metadata origMeta = metadataService.createMetadataFromStatusUpdateRequest(updateRequest);
            metadataUpdated.add(origMeta);
        }

        // check "sentDeleted" annotations that have same metadata (except version), but response status "SENT" already
        // as they have "sentDeleted" flag set, they now should be deleted unless already being DELETED
        // this means that we use the metadata sets given above, but adapt their response status and remove the version
        // -> this should deliver us the metadata (and then the annotations) to be finally DELETED now
        // note: this should work in if all older annotations are always DELETED in this way
        // (ref: ANOT-97)
        final List<Long> metaCandIdsSentDelete = new ArrayList<Long>();

        for (final Metadata metaUpdatedItem : metadataUpdated) {

            // create a copy in order to avoid the changed item being saved (which can occur since it is linked to Annotation objects)
            final Metadata metaUpdated = new Metadata(metaUpdatedItem);

            metaUpdated.setResponseStatus(Metadata.ResponseStatus.SENT);
            metaUpdated.removeResponseVersion();

            final List<Long> foundMetaIds = metadataMatchingService.findExactMetadataWithoutResponseVersion(metaUpdated.getDocument(), metaUpdated.getGroup(),
                    metaUpdated.getSystemId(), metaUpdated);
            if (!CollectionUtils.isEmpty(foundMetaIds)) {
                metaCandIdsSentDelete.addAll(foundMetaIds);
            }
        }

        // from those metadata candidates, we search all associate annotations having "sentDeleted=true" and not DELETED status
        final List<Annotation> annotsSentDeletedToDelete = annotService.findSentDeletedByMetadataIdAndStatus(
                metaCandIdsSentDelete, AnnotationStatus.getNonDeleted());
        if (!CollectionUtils.isEmpty(annotsSentDeletedToDelete)) {
            // add found items to the list of annotations to be deleted
            annotsSentDeletedToDelete.forEach(ann -> annotsToDelete.add(ann.getId()));
        } else if (!metadataItemsUpdated) {
            // no items to delete and no metadata items updated -> throw error (as we did in the past)
            LOG.warn("No items to update, no items to be logically deleted");
            throw new CannotUpdateAnnotationStatusException("No data to update");
        }

        // persist the changes
        try {
            annotService.saveAll(annotsToSave);

            for (final String annId : annotsToDelete) {
                final Annotation ann = annotService.findAnnotationById(annId);
                annotService.softDeleteAnnotation(ann, userInfo.getUser().getId());
            }
        } catch (IllegalArgumentException | CannotDeleteAnnotationException e) {
            throw new CannotUpdateAnnotationStatusException("Error updating dependent annotations", e);
        }

        // we return the list of annotation IDs affected by the response status transition
        // (in order to be able to publish updates via websockets)
        final List<String> updatedIds = annots.stream().map(Annotation::getId).collect(Collectors.toList());
        LOG.info("{} annotations updated, {} annotations logically deleted", updatedIds.size(), annotsToDelete.size());
        return new ResponseStatusUpdateResult(updatedIds, annotsToDelete);
    }

    /**
     * check a given list of annotations for the presence of links to other annotations
     * if so, break the link and delete the linked item
     * 
     * @param annots
     *        list of {@link Annotation}s to be checked
     * @param annotsToSave
     *        returned list of {@link Annotation}s modified and requiring to be saved
     * @param annotsToDelete
     *        returned list of {@link Annotation}s to be deleted
     */
    private void checkLinkedAnnotations(final List<Annotation> annots,
            final List<Annotation> annotsToSave,
            final List<String> annotsToDelete) {

        for (final Annotation annCheck : annots) {

            if (!StringUtils.isEmpty(annCheck.getLinkedAnnotationId())) {

                // soft-delete the linked item
                annotsToDelete.add(annCheck.getLinkedAnnotationId());

                // break the link and save
                annCheck.setLinkedAnnotationId(null);
                annotsToSave.add(annCheck);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PublishContributionsResult publishContributions(final PublishContributionsRequest publishRequest,
            final UserInformation userInfo)
            throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        if (publishRequest == null) {
            throw new CannotPublishContributionsException("Cannot publish contributor annotations without information on which ones to publish");
        }

        Assert.notNull(userInfo, "No valid user information given");

        if (!annotPermService.userMayPublishContributions(userInfo)) {
            throw new MissingPermissionException("User " + userInfo.getLogin() +
                    " has no permission to publish annotations of user " + publishRequest.getUserid());
        }

        /**
         *  idea: 
         *  1) get all matching annotations of the user, make them become public (shared)
         *  2) then examine all associate metadata and see whether further annotations of other users 
         *     are assigned to them; if so, create and assign a metadata copy to the annotations of step 1)
         *  3) finally, for all relevant metadata, add the new "originMode" property
         */

        List<Metadata> metaCandidates = metadataService.findMetadataOfDocumentGroupSystemid(publishRequest.getDocUri(), publishRequest.getGroup(),
                Authorities.ISC);
        if (CollectionUtils.isEmpty(metaCandidates)) {
            throw new CannotPublishContributionsException("No matching metadata found");
        }

        metaCandidates = metadataMatchingService.filterByIscReference(metaCandidates, publishRequest.getIscReference());
        if (CollectionUtils.isEmpty(metaCandidates)) {
            throw new CannotPublishContributionsException(String.format("No matching metadata having ISCReference %s found", publishRequest.getIscReference()));
        }

        final AnnotationSearchOptions options = new AnnotationSearchOptions(publishRequest.getDocUri(), publishRequest.getGroup(), true, -1, 0, "ASC",
                "created");
        options.setSearchUser(SearchUserType.ISC);
        options.setUser(publishRequest.getUserid());

        final List<SimpleMetadataWithStatuses> metaStatusList = new ArrayList<SimpleMetadataWithStatuses>();
        for (final Metadata meta : metaCandidates) {
            final SimpleMetadataWithStatuses smws = new SimpleMetadataWithStatuses(meta.getKeyValuePropertyAsSimpleMetadata(),
                    Arrays.asList(AnnotationStatus.NORMAL));
            metaStatusList.add(smws);
        }
        options.setMetadataMapsWithStatusesList(metaStatusList);
        final AnnotationSearchResult searchRes = annotService.searchAnnotations(options, userInfo);
        if (searchRes.getTotalItems() == 0) {
            throw new CannotPublishContributionsException("No annotations found matching given metadata");
        }

        // filter for private items
        searchRes.setItems(searchRes.getItems().stream().filter(ann -> !ann.isShared()).collect(Collectors.toList()));
        searchRes.setTotalItems(searchRes.getItems().size());

        if (!CollectionUtils.isEmpty(searchRes.getItems())) {

            // make the annotations become publicly visible
            annotService.makeShared(searchRes.getItems());
        }

        final PublishContributionsResult publishResult = new PublishContributionsResult(
                searchRes.getItems().stream().map(annot -> annot.getId()).collect(Collectors.toList()));

        // now examine all related metadata items: if any is associated to more annotations than the ones just updated,
        // we have to create new metadata item for these and link the annotations to it
        final List<Metadata> allMetadata = searchRes.getItems().stream().map(annot -> annot.getMetadata()).distinct().collect(Collectors.toList());

        for (final Metadata meta : allMetadata) {

            // retrieve all annotations assigned to this metadata item - considering all statuses!
            final List<Annotation> linkedAnnots = annotService.findByMetadata(Arrays.asList(meta.getId()));
            final List<Annotation> assignedAnnots = linkedAnnots.stream().filter(
                    annot -> publishResult.getUpdatedAnnotIds().contains(annot.getId())).collect(Collectors.toList());

            if (assignedAnnots.size() == linkedAnnots.size()) {

                // all linked annotations have been published - just add new metadata property and we are done
                addOriginModeAndSave(meta);
            } else {

                // not all of the linked annotations have been published - i.e. that those contained in assignedAnnots must be reassigned
                final Metadata newMetadata = new Metadata(meta);
                addOriginModeAndSave(newMetadata);

                for (final Annotation annot : assignedAnnots) {
                    annot.setMetadata(newMetadata);
                }
                annotService.saveAll(assignedAnnots);
            }
        }

        return publishResult;
    }

    /**
     * sets the "originMode" parameter to a given {@link Metadata} item
     * 
     * @param meta
     *        {@link Metadata} to be changed
     * @return changed {@link Metadata} item
     * @throws CannotCreateMetadataException
     *         thrown when persisting the metadata fails
     */
    private Metadata addOriginModeAndSave(final Metadata meta) throws CannotCreateMetadataException { 

        final SimpleMetadata keyvalues = meta.getKeyValuePropertyAsSimpleMetadata();
        keyvalues.put("originMode", "private");
        meta.setKeyValuePropertyFromSimpleMetadata(keyvalues);

        metadataService.saveMetadata(meta);
        
        return meta;
    }

    
}