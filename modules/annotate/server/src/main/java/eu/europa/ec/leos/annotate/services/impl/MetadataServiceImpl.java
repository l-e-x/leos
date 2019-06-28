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
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.repository.MetadataRepository;
import eu.europa.ec.leos.annotate.services.DocumentService;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.MetadataService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationStatusException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MetadataServiceImpl implements MetadataService {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataServiceImpl.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private DocumentService documentService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private MetadataRepository metadataRepos;

    private static final String SYSTEM_ID = "systemId";
    private static final String RESPONSE_STATUS = "responseStatus";

    // -------------------------------------
    // Service functionality
    // -------------------------------------

    /**
     * find {@link Metadata} objects given their linked {@link Document}, {@link Group} and system ID
     * 
     * @param document the linked document
     * @param group    the associate group
     * @param systemId the system ID via which the metadata was given
     * 
     * @return found Metadata objects, or empty list
     */
    @Override
    @Nonnull
    public List<Metadata> findMetadataOfDocumentGroupSystemid(final Document document, final Group group, final String systemId) {

        return metadataRepos.findByDocumentAndGroupAndSystemId(document, group, systemId);
    }

    /**
     * find {@link Metadata} objects given their linked {@link Document}, {@link Group}, system ID, and having response status SENT
     * 
     * @param document the linked document
     * @param group    the associate group
     * @param systemId the system ID via which the metadata was given
     * 
     * @return found Metadata objects, or empty list
     */
    @Override
    @Nonnull
    public List<Metadata> findMetadataOfDocumentGroupSystemidSent(final Document document, final Group group, final String systemId) {

        return metadataRepos.findByDocumentAndGroupAndSystemIdAndResponseStatus(document, group, systemId, ResponseStatus.SENT);
    }

    /**
     * find all {@link Metadata} objects given linked {@link Document}, system ID and a list of allowed linked {@link Group}s
     * 
     * @param document the linked document
     * @param systemId the system ID via which the metadata was given
     * @param groupIds the list of associate groups' IDs
     * 
     * @return found Metadata objects, or empty list
     * @throws IllegalArgumentException if groupIds are {@literal null}
     */
    @Override
    @Nonnull
    public List<Metadata> findMetadataOfDocumentSystemidGroupIds(final Document document, final String systemId, final List<Long> groupIds) {

        // null will provoke a Hibernate exception - we return an exception before, which is better understandable
        Assert.notNull(groupIds, "groupIds must not be null");

        return metadataRepos.findByDocumentAndSystemIdAndGroupIdIsIn(document, systemId, groupIds);
    }

    /**
     * find all {@link Metadata} objects given linked {@link Document}, system ID, and having response status SENT
     * 
     * @param document the linked document
     * @param systemId the system ID via which the metadata was given
     * 
     * @return found Metadata objects, or empty list
     */
    @Override
    @Nonnull
    public List<Metadata> findMetadataOfDocumentSystemidSent(final Document document, final String systemId) {

        return metadataRepos.findByDocumentAndSystemIdAndResponseStatus(document, systemId, ResponseStatus.SENT);
    }

    /**
     * check if all items of a given set of metadata key/values are contained in a given database {@link Metadata} object
     * 
     * @param metadataRequired set of required key/value pairs ({@link SimpleMetadata})
     * @param candidateMeta {@link Metadata} instance to be inspected whether it contains all requested key/value pairs
     * 
     * @return flag indicating if candidate element contains (at least) the requested items
     */
    @Override
    public boolean areAllMetadataContainedInDbMetadata(
            final SimpleMetadata metadataRequired,
            final Metadata candidateMeta) {

        // no requirements -> ok
        if (metadataRequired == null) {
            LOG.debug("Did not get any metadata that should be searched -> approve");
            return true;
        }

        if (candidateMeta == null && metadataRequired.isEmpty()) {
            // empty requirements, no DB content -> ok
            LOG.debug("Got empty metadata that should be searched -> approve");
            return true;
        } else if (candidateMeta == null) {
            // requirements present, but no DB content -> fail
            LOG.debug("Cannot compare when one or more of comparees are null");
            return false;
        }

        // note: the preprocessing checks may remove entries from the given map
        // but the callee shall not notice; therefore, we copy the map and operate on the copy
        final SimpleMetadata metaReqCopy = new SimpleMetadata(metadataRequired);

        // preprocessing for systemId (has its own field in DB entity)
        if (!checkSystemId(metaReqCopy, candidateMeta)) {
            return false;
        }

        // similar preprocessing for response status (has its own field in DB entity)
        if (!checkResponseStatus(metaReqCopy, candidateMeta)) {
            return false;
        }

        return checkKeyValueProperties(metaReqCopy, candidateMeta);
    }

    /**
     *  moved the system ID comparison to a separate function
     *  
     * @param metadataRequired set of required key/value pairs ({@link SimpleMetadata})
     * @param candidateMeta {@link Metadata} instance to be inspected whether it contains all requested key/value pairs
     * 
     * @return {@literal false} in case of discrepancy, {@literal true} otherwise
     */
    private boolean checkSystemId(final SimpleMetadata metadataRequired, final Metadata candidateMeta) {

        if (metadataRequired.containsKey(SYSTEM_ID)) {
            if (StringUtils.isEmpty(candidateMeta.getSystemId())) {
                LOG.debug("SystemId does not match: candidate item has no systemId set");
                return false;
            }
            if (!metadataRequired.get(SYSTEM_ID).equals(candidateMeta.getSystemId())) {
                LOG.debug("SystemId does not match");
                return false;
            }
            metadataRequired.remove(SYSTEM_ID);
        }

        return true;
    }

    /**
     *  moved the response status comparison to a separate function
     *  
     * @param metadataRequired set of required key/value pairs ({@link SimpleMetadata})
     * @param candidateMeta {@link Metadata} instance to be inspected whether it contains all requested key/value pairs
     * 
     * @return {@literal false} in case of discrepancy, {@literal true} otherwise
     */
    private boolean checkResponseStatus(final SimpleMetadata metadataRequired, final Metadata candidateMeta) {

        if (metadataRequired.containsKey(RESPONSE_STATUS)) {
            final ResponseStatus expectedResponseStatus = candidateMeta.getResponseStatus();
            if (expectedResponseStatus == null) {
                LOG.debug("ResponseStatus does not match: candidate item has no response status set");
                return false;
            }
            if (!metadataRequired.get(RESPONSE_STATUS).equals(candidateMeta.getResponseStatus().toString())) {
                LOG.debug("ResponseStatus does not match");
                return false;
            }
            metadataRequired.remove(RESPONSE_STATUS);
        }

        return true;
    }

    /**
     * comparison of key-value properties with DB object values
     * note: comparison of systemId and responseStatus properties should have been done before (separate function)
     *  
     * @param metadataRequired set of required key/value pairs ({@link SimpleMetadata})
     * @param candidateMeta {@link Metadata} instance to be inspected whether it contains all requested key/value pairs
     * 
     * @return {@literal false} in case of discrepancy, {@literal true} otherwise
     */
    private boolean checkKeyValueProperties(final SimpleMetadata metadataRequired, final Metadata candidateMeta) {

        final SimpleMetadata foundMeta = candidateMeta.getKeyValuePropertyAsSimpleMetadata();
        for (final Map.Entry<String, String> requiredEntry : metadataRequired.entrySet()) {

            // note: response status and systemId were already matched before and removed
            final String requiredEntryKey = requiredEntry.getKey();
            final String foundValue = foundMeta.get(requiredEntryKey);
            if (foundValue == null) {
                LOG.debug("Required metadata field '{}' not stored in DB.", requiredEntry.getKey());
                return false;
            }
            if (!foundValue.equals(requiredEntry.getValue())) {
                LOG.debug("Value of field '{}' different in DB.", requiredEntryKey);
                return false;
            }
        }

        // when reaching this point, all required metadata is available and matches
        return true;
    }

    /**
     * filter a given list of {@link Metadata} objects by a list of requested items ("OR filtering")
     *  
     * @param candidates set of candidate {@link Metadata} items
     * @param requested  list of requested sets of metadata that must match at least
     * 
     * @return {@literal null} if no candidates are given; list of IDs of the filtered candidates 
     */
    @Override
    public List<Long> getIdsOfMatchingMetadatas(
            final List<Metadata> candidates,
            final List<SimpleMetadata> requested) {

        // no candidates -> no matches!
        if (candidates == null) {
            return null;
        }

        // no restriction given -> all match!
        if (requested == null || requested.isEmpty() ||
                requested.size() == 1 && requested.get(0).isEmpty()) {
            return getMetadataSetIds(candidates);
        }

        final List<Metadata> semifilteredMetadata = new ArrayList<Metadata>();
        for (final SimpleMetadata requestedMeta : requested) {
            semifilteredMetadata.addAll(candidates.stream()
                    .filter(meta -> areAllMetadataContainedInDbMetadata(requestedMeta, meta))
                    .collect(Collectors.toList()));
        }
        return getMetadataSetIds(semifilteredMetadata);
    }
    
    /**
     * filter a given list of {@link Metadata} objects by a list of requested items ("OR filtering")
     *  
     * @param candidates set of candidate {@link Metadata} items
     * @param requested  requested set of metadata that must match at least
     * 
     * @return {@literal null} if no candidates are given; list of IDs of the filtered candidates 
     */
    @Override
    public List<Long> getIdsOfMatchingMetadatas(
            final List<Metadata> candidates,
            final SimpleMetadata requested) {

        // no candidates -> no matches!
        if (candidates == null) {
            return null;
        }

        // no restriction given -> all match!
        if (requested == null || requested.isEmpty()) {
            return getMetadataSetIds(candidates);
        }

        return getIdsOfMatchingMetadatas(candidates, Arrays.asList(requested));
    }

    /**
     * from a given list of {@link Metadata} objects, return their IDs
     * 
     * @param metaList list of {@link Metadata} objects, may even contain {@literal null} entries
     * 
     * @return list of the valid items' IDs without duplicates; returns {@literal null} if there are no results
     */
    @Override
    public List<Long> getMetadataSetIds(final List<Metadata> metaList) {

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
     * merges two given list of {@link Metadata} objects and returns the disjunct set of IDs
     * 
     * @param metaList1 first list of {@link Metadata} objects, may even contain {@literal null} entries
     * @param metaList2 second list of {@link Metadata} objects, may even contain {@literal null} entries
     * 
     * @return list of the valid items' IDs without duplicates; returns {@literal null} if there are no results
     */
    @Override
    public List<Long> getMetadataSetIds(final List<Metadata> metaList1, final List<Metadata> metaList2) {

        final List<Long> firstIds = getNonNullMetadataSetIds(metaList1);
        final List<Long> secondIds = getNonNullMetadataSetIds(metaList2);

        // merge the streams - by construction, their elements should be disjoint, but be sure...
        final List<Long> merged = Stream.concat(firstIds.stream(), secondIds.stream()).distinct().collect(Collectors.toList());

        if (merged.isEmpty()) {
            return null;
        }
        return merged;
    }

    /**
     * wrapper around the {@link getMetadataSetIds} method, but makes sure to always return a non-null result
     * 
     * @param metaList list of Metadata objects
     * 
     * @return list of the valid items' IDs; in worst case, returns an empty list
     */
    @Override
    @Nonnull
    public List<Long> getNonNullMetadataSetIds(final List<Metadata> metaList) {

        final List<Long> result = getMetadataSetIds(metaList);
        if (result != null) {
            return result;
        }
        return new ArrayList<Long>();
    }

    /**
    * save a given {@link Metadata} set
    * 
    * @param metadata the metadata set to be saved
    * 
    * @return saved metadata, with IDs filled in
    * @throws CannotCreateMetadataException 
    */
    @Override
    public Metadata saveMetadata(final Metadata metadata) throws CannotCreateMetadataException {

        if (metadata == null) {
            LOG.error("Received metadata for saving is null!");
            throw new CannotCreateMetadataException(new IllegalArgumentException("Metadata is null"));
        }

        Metadata modifiedMetadata = null;
        try {
            modifiedMetadata = metadataRepos.save(metadata);
        } catch (Exception e) {
            LOG.error("Exception upon saving metadata");
            throw new CannotCreateMetadataException(e);
        }

        return modifiedMetadata;
    }

    /**
     * find an exact match for a {@link Metadata} item in the database
     * 
     * @param document associated document
     * @param group associated group
     * @param systemId associated system ID
     * @param otherMetadata {@link Metadata} object containing key-value properties to be matched
     */
    @Override
    public Metadata findExactMetadata(final Document document, final Group group, final String systemId, final Metadata otherMetadata) {

        SimpleMetadata metadataMap = null;
        if (otherMetadata != null) {
            metadataMap = otherMetadata.getKeyValuePropertyAsSimpleMetadata();
        }

        return findExactMetadata(document, group, systemId, metadataMap);
    }

    /**
     * find an exact match for a {@link Metadata} item in the database
     * 
     * @param document associated document
     * @param group associated group
     * @param systemId associated system ID
     * @param otherMetadataProps map of other key-value metadata properties
     */
    @Override
    public Metadata findExactMetadata(final Document document, final Group group, final String systemId, final SimpleMetadata otherMetadataProps) {

        if (document == null || group == null || StringUtils.isEmpty(systemId)) {
            LOG.warn("Cannot search for metadata item when document, group or systemId is missing");
            return null;
        }

        final List<Metadata> candidates = findMetadataOfDocumentGroupSystemid(document, group, systemId);
        if (candidates.isEmpty()) {
            LOG.debug("Did not find any metadata sets matching given document/group/systemId");
            return null;
        }

        // find equality by matching inclusion in both directions (A includes B, B includes A)
        final List<Metadata> candidatesOneDirection = candidates.stream().filter(meta -> areAllMetadataContainedInDbMetadata(otherMetadataProps, meta))
                .collect(Collectors.toList());

        final Metadata metaHelp = new Metadata(document, group, systemId);
        metaHelp.setKeyValuePropertyFromSimpleMetadata(otherMetadataProps);

        for (final Metadata candidateMeta : candidatesOneDirection) {
            if (areAllMetadataContainedInDbMetadata(candidateMeta.getKeyValuePropertyAsSimpleMetadata(), metaHelp)) {
                return candidateMeta; // perfect match
            }
        }

        return null;
    }

    /**
     * removal of given metadata set identified by its ID
     * 
     * @param metadataId ID of the metadata set to be removed
     */
    @Override
    public void deleteMetadataById(final long metadataId) {

        try {
            metadataRepos.delete(metadataId);
        } catch (Exception e) {
            LOG.error("Could not delete metadata set with ID " + metadataId, e);
        }
    }

    /**
     * update the response status of metadata sets
     * note: "at least" matching is applied, i.e. all metadata having *at least* the requested ones are updated
     * 
     * @param updateRequest {@link StatusUpdateRequest} object containing the group and document URI whose metadata is to be updated;
     *                      in addition, it contains the new responseStatus and further metadata properties for identifying target metadata
     * @param userInfo information about the user requesting metadata update
     * 
     * @return returns the list of ID of the metadata sets that were updated; at least an empty list is returned, but not {@literal null}
     * 
     * @throws CannotUpdateAnnotationStatusException whenever required information cannot be identified (e.g. related group/document)
     * @throws MissingPermissionException thrown if user does not have required permissions
     */
    @Override
    @Nonnull
    public List<Long> updateMetadata(final StatusUpdateRequest updateRequest, final UserInformation userInfo)
            throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        if (userInfo == null) {
            throw new CannotUpdateAnnotationStatusException(new IllegalArgumentException("userInfo is null"));
        }

        Assert.notNull(updateRequest, "information about the metadata to be updated is required");
        Assert.isTrue(!StringUtils.isEmpty(updateRequest.getGroup()), "group assigned to metadata to be updated is required");
        Assert.isTrue(!StringUtils.isEmpty(updateRequest.getUri()), "document URI assigned to metadata to be updated is required");

        if (!userMayUpdateMetadata(userInfo)) {
            throw new MissingPermissionException(String.format("%s (%s)", userInfo.getLogin(), userInfo.getAuthority()));
        }

        final Document foundDocument = documentService.findDocumentByUri(updateRequest.getUri());
        if (foundDocument == null) {
            LOG.error("No document found with given URI; thus no metadata can be updated");
            throw new CannotUpdateAnnotationStatusException("Given URI is unknown");
        }

        final Group foundGroup = groupService.findGroupByName(updateRequest.getGroup());
        if (foundGroup == null) {
            LOG.error("No group with given name found; thus no metadata can be updated");
            throw new CannotUpdateAnnotationStatusException("Given group is unknown");
        }

        // finally search for the items requiring to be updated
        // first step: get all candidates associated to document, group and system ID
        final String authority = userInfo.getAuthority();
        final List<Metadata> metaCandidates = findMetadataOfDocumentGroupSystemid(foundDocument, foundGroup, authority);
        if (metaCandidates.isEmpty()) {
            LOG.error("No annotations are assigned to the group, document, authority/systemId; thus no metadata can be updated");
            throw new CannotUpdateAnnotationStatusException("No metadata found");
        }

        // keep only those having at least the required metadata
        final List<Metadata> filteredMetadata = metaCandidates.stream()
                .filter(meta -> areAllMetadataContainedInDbMetadata(updateRequest.getMetadataToMatch(), meta))
                .collect(Collectors.toList());
        if (filteredMetadata.isEmpty()) {
            LOG.error("No annotations assigned to the group, document, authority/systemId have the required properties; thus no metadata can be updated");
            throw new CannotUpdateAnnotationStatusException("No metadata found having required properties");
        }

        final List<Long> metadataIdsUpdated = new ArrayList<Long>();
        for (final Metadata metaFound : filteredMetadata) {

            // update, write auditing information and save
            metaFound.setResponseStatus(updateRequest.getResponseStatus());
            metaFound.setResponseStatusUpdated(LocalDateTime.now());
            metaFound.setResponseStatusUpdatedBy(userInfo.getUser().getId());

            try {
                saveMetadata(metaFound);
            } catch (CannotCreateMetadataException e) {
                throw new CannotUpdateAnnotationStatusException(
                        String.format("Metadata with id '%s' could not be updated: %s", metaFound.getId(), e.getMessage()), e);
            }
            // keep track of successfully updated metadata sets
            metadataIdsUpdated.add(metaFound.getId());
        }
        return metadataIdsUpdated;
    }

    /**
     * check for a given user whether he is permitted to trigger a metadata status update
     * 
     * @param userInfo information about the user requesting the status update
     * @return flag indicating whether he is allowed to execute the update
     */
    private boolean userMayUpdateMetadata(final UserInformation userInfo) {

        Assert.notNull(userInfo, "No valid user data given for checking permissions");

        return Authorities.isIsc(userInfo.getAuthority());
    }

}
