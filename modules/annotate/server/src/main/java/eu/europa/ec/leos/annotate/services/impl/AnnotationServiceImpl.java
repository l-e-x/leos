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
import eu.europa.ec.leos.annotate.model.MetadataIdsAndStatuses;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.search.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationPermissions;
import eu.europa.ec.leos.annotate.repository.AnnotationRepository;
import eu.europa.ec.leos.annotate.repository.impl.AnnotationCountSearchSpec;
import eu.europa.ec.leos.annotate.repository.impl.AnnotationReplySearchSpec;
import eu.europa.ec.leos.annotate.services.*;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import org.hibernate.collection.internal.PersistentBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Nonnull;

import java.rmi.activation.UnknownGroupException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for annotation administration functionality 
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
@Service
public class AnnotationServiceImpl implements AnnotationService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationServiceImpl.class);
    private static final String ERROR_USERINFO_MISSING = "Required user information missing.";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    @Qualifier("annotationRepos")
    private AnnotationRepository annotRepos;

    @Autowired
    private AnnotationPermissionService annotPermService;

    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private TagsService tagsService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private AnnotationConversionService conversionService;

    @Autowired
    private UUIDGeneratorService uuidService;

    @Autowired
    private SearchModelFactory searchModelFactory;

    // -------------------------------------
    // Constructors
    // -------------------------------------
    public AnnotationServiceImpl() {
        // default constructor
    }

    // constructor e.g. used for testing
    public AnnotationServiceImpl(final UserService userService) {
        if (this.userService == null) {
            this.userService = userService;
        }
    }

    // -------------------------------------
    // Service functionality
    // -------------------------------------

    /**
     * simple function that just looks up an annotation based on its ID WITHOUT permission checks
     * to be used only from tests or from services layer
     *  
     * @param annotId
     *        the ID of the wanted annotation
     *        
     * @return returns the found annotation object, or {@literal null}; note: only non-deleted annotations are returned
     */
    @Override
    public Annotation findAnnotationById(final String annotId) {

        return annotRepos.findByIdAndStatus(annotId, AnnotationStatus.NORMAL);
    }

    /**
     * look up an annotation based on its ID, taking permissions into account
     * 
     * @param annotId
     *        the ID of the wanted annotation
     * @param userlogin
     *        the login of the user requesting to get the annotation
     *        
     * @return returns the found annotation object, or null
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to view this annotation
     */
    @Override
    public Annotation findAnnotationById(final String annotId, final String userlogin) throws MissingPermissionException {

        Assert.isTrue(!StringUtils.isEmpty(annotId), "Cannot search for annotation, no annotation ID specified.");

        // retrieve the annotation first...
        final Annotation resultAnnotation = findAnnotationById(annotId);
        if (resultAnnotation == null) {
            LOG.error("The wanted annotation with id '{}' could not be found.", annotId);
            return null;
        }

        // ... then check the permissions
        // note: we did not combine the permission check directly with the database query here in order to be able
        // to easily distinguish between annotation being missing and annotation not being permitted to be viewed
        final User user = userService.findByLogin(userlogin);
        if (!annotPermService.hasUserPermissionToSeeAnnotation(resultAnnotation, user)) {
            LOG.warn("User '{}' does not have permission to see annotation with id '{}'.", userlogin, annotId);
            throw new MissingPermissionException(userlogin);
        }

        return resultAnnotation;
    }

    /**
     * create an annotation in the database based on an incoming, JSON-deserialized annotation
     * 
     * @param webAnnot
     *        the incoming annotation
     * @param userInfo
     *        information about the user wanting to create an annotation
     * @throws CannotCreateAnnotationException 
     *         the exception is thrown when the annotation cannot be created due to unfulfilled constraints
     *         (e.g. missing document or user information)
     */
    @Override
    public JsonAnnotation createAnnotation(final JsonAnnotation webAnnot, final UserInformation userInfo) throws CannotCreateAnnotationException {

        if (userInfo == null) {
            throw new CannotCreateAnnotationException(new IllegalArgumentException("userInfo is null"));
        }
        LOG.debug("createAnnotation: authUser='{}'", userInfo.getLogin());

        // find belonging user in DB
        // NOTE: we could access the user using the authentication user's information retrieved above - but for currently unknown reasons,
        // the User object received thereby might not be contained in the database in test scenarios - although it was explicitly saved
        // therefore we have to re-retrieve the User object fresh from the database
        final User registeredUser = userService.findByLogin(userInfo.getLogin());
        if (registeredUser == null) {
            // user was already created during client's initialisation (i.e. token exchange/profile retrieval)
            // so it should be known here unless an error had occurred before
            // therefore we don't do anything if user is still missing now
            throw new CannotCreateAnnotationException(new UserNotFoundException(userInfo.getLogin()));
        }

        // find belonging group in DB
        final Group group = groupService.findGroupByName(webAnnot.getGroup());
        if (group == null) {
            LOG.error("Cannot create annotation as associate group is unknown");
            throw new CannotCreateAnnotationException(new UnknownGroupException(webAnnot.getGroup()));
        }

        // search if document is already contained in DB
        final Document document = findOrCreateDocument(webAnnot);

        final Metadata metadata = prepareMetadata(webAnnot, document, group, userInfo);

        // save the annotation with all required reference IDs
        Annotation annot = new Annotation();
        annot.setCreated(webAnnot.getCreated());
        annot.setMetadata(metadata);
        annot.setUser(registeredUser);
        annot.setId(uuidService.generateUrlSafeUUID()); // as a new annotation is saved, we set an ID, regardless whether one was present before!
        annot.setReferences(webAnnot.getReferences()); // take over all referenced annotations (is 'null' for new top-level annotations, filled for replies)
        annot.setShared(!isPrivateAnnotation(webAnnot));
        annot.setText(webAnnot.getText());
        annot.setTargetSelectors(webAnnot.getSerializedTargets());
        annot.setUpdated(LocalDateTime.now());

        // save tags, if present
        if (webAnnot.getTags() != null && !webAnnot.getTags().isEmpty()) {
            annot.setTags(tagsService.getTagList(webAnnot.getTags(), annot));
        }

        try {
            annot = annotRepos.save(annot);
        } catch (Exception e) {
            throw new CannotCreateAnnotationException(e);
        }

        // update newly set properties for the response object
        webAnnot.setId(annot.getId());
        webAnnot.setUpdated(annot.getUpdated());

        return webAnnot;
    }

    /**
     * looks up if a document already exists, or tries to create it
     * 
     * @param webAnnot incoming annotation (JSON-based)
     * @return found {@link Document}
     * @throws CannotCreateAnnotationException thrown when document cannot be created
     */
    @Nonnull
    private Document findOrCreateDocument(final JsonAnnotation webAnnot)
            throws CannotCreateAnnotationException {

        Document document = documentService.findDocumentByUri(webAnnot.getUri());
        if (document == null) {

            // register new document
            try {
                if (webAnnot.getDocument() == null) {
                    // replies to annotations do not have the document object, but still feature the URI
                    document = documentService.createNewDocument(webAnnot.getUri());
                } else {
                    // new top-level annotations have a document object containing URI and title
                    document = documentService.createNewDocument(webAnnot.getDocument());
                }
            } catch (CannotCreateDocumentException e) {
                LOG.error("Cannot create annotation as associate document could not be registered");
                throw new CannotCreateAnnotationException(e);
            }
        }
        return document;
    }

    // prepare the metadata to be associated to an annotation
    // this can either be an existing metadata set, or a new one that is created
    @Nonnull
    private Metadata prepareMetadata(final JsonAnnotation webAnnot,
            final Document document, final Group group,
            final UserInformation userInfo) throws CannotCreateAnnotationException {

        // determine system ID
        String systemId = userInfo.getAuthority();
        Metadata receivedMetadata = null;
        if (webAnnot.hasMetadata()) {
            receivedMetadata = new Metadata();
            receivedMetadata.setKeyValuePropertyFromSimpleMetadata(webAnnot.getDocument().getMetadata());

            // if we received a system ID, we use it; otherwise, we propagate the system ID of the user
            // note: usually, they should be identical anyway!
            if (StringUtils.isEmpty(receivedMetadata.getSystemId())) {
                receivedMetadata.setSystemId(systemId);
            } else {
                systemId = receivedMetadata.getSystemId();
            }
        }

        if (receivedMetadata != null && receivedMetadata.isResponseStatusSent()) {
            throw new CannotCreateAnnotationException("Cannot create new annotations having response status SENT already");
        }

        // search if there is already a metadata set for the group+document+systemId combination
        Metadata metadata = null;
        if (webAnnot.isReply()) {
            metadata = findOrCreateReplyMetadata(webAnnot, document, group, userInfo.getAuthority(), receivedMetadata);

            // no reply, but a new annotation
        } else {

            metadata = metadataService.findExactMetadata(document, group, systemId, receivedMetadata);
            if (metadata == null) {

                // register the new metadata
                try {
                    metadata = new Metadata(document, group, systemId);
                    metadata.setKeyValuePropertyFromSimpleMetadata(webAnnot.getDocument().getMetadata());

                    metadata = metadataService.saveMetadata(metadata);
                } catch (CannotCreateMetadataException ccme) {
                    LOG.error("Metadata could not be persisted while creating annotation");
                    throw new CannotCreateAnnotationException(ccme);
                } catch (Exception e) {
                    LOG.error("Received unexpected exception when trying to persist metadata during creation of annotation", e);
                    throw new CannotCreateAnnotationException(e);
                }
            }
        }

        return metadata;
    }

    /**
     * finds existing metadata that will be associated to a reply; or creates appropriate new metadata
     * 
     * @param webAnnot incoming annotation (JSON-based)
     * @param document associate document
     * @param group associate group
     * @param authority system from which the reply is being created 
     * @param receivedMetadata incoming metadata
     * 
     * @return found or newly created {@link Metadata} object to be used for the reply
     * 
     * @throws CannotCreateAnnotationException thrown when no parent annotation found, parent is SENT, or other error
     */
    private Metadata findOrCreateReplyMetadata(final JsonAnnotation webAnnot, final Document document,
            final Group group, final String authority, final Metadata receivedMetadata)
            throws CannotCreateAnnotationException {

        Metadata metadata = null;

        // for replies, the annotation does not contain metadata; we reuse the metadata of the thread's root
        final Annotation rootAnnot = findAnnotationById(webAnnot.getRootAnnotationId());
        if (rootAnnot == null) {
            throw new CannotCreateAnnotationException("No root annotation found for reply");
        } else {
            if (rootAnnot.isResponseStatusSent()) {
                throw new CannotCreateAnnotationException("Replies on SENT annotations are not allowed");
            }
            metadata = rootAnnot.getMetadata();
            if (!metadata.getSystemId().equals(authority)) {
                // user is commenting on annotation created by from different system
                // (e.g. LEOS/EdiT user is commenting on ISC annotation)
                // in that case, we must find/create different metadata!
                metadata = metadataService.findExactMetadata(document, group, authority, receivedMetadata); // receivedMetadata is null!
                if (metadata == null) {

                    // register the new metadata
                    try {
                        metadata = new Metadata(document, group, authority);
                        metadata = metadataService.saveMetadata(metadata);
                    } catch (CannotCreateMetadataException ccme) {
                        LOG.error("Metadata could not be persisted while creating annotation reply");
                        throw new CannotCreateAnnotationException(ccme);
                    } catch (Exception e) {
                        LOG.error("Received unexpected exception when trying to persist metadata during creation of annotation reply", e);
                        throw new CannotCreateAnnotationException(e);
                    }
                }
            }
        }
        return metadata;
    }

    /**
     * update an existing annotation in the database based on an incoming, JSON-deserialized annotation
     * 
     * @param annotationId 
     *        id of the annotation to be updated
     * @param webAnnot 
     *        the incoming annotation
     * @param userInfo
     *        information about the user wanting to create an annotation
     *        
     * @throws CannotUpdateAnnotationException 
     *         the exception is thrown when the annotation cannot be updated (e.g. when it is not existing)
     * @throws CannotUpdateSentAnnotationException
     *         exception thrown when an annotation with response status SENT is tried to be updated
     * @throws MissingPermissionException 
     *         the exception is thrown when the user lacks permissions for updating the annotation
     */
    @Override
    public JsonAnnotation updateAnnotation(final String annotationId, final JsonAnnotation webAnnot, final UserInformation userInfo)
            throws CannotUpdateAnnotationException, CannotUpdateSentAnnotationException, MissingPermissionException {

        Assert.isTrue(!StringUtils.isEmpty(annotationId), "Required annotation ID missing.");

        if (userInfo == null) {
            throw new CannotUpdateAnnotationException(new IllegalArgumentException("userInfo is null"));
        }

        final Annotation ann = findAnnotationById(annotationId);
        if (ann == null) {
            throw new CannotUpdateAnnotationException("Annotation not found");
        }

        // check if the annotation is final already and may not be updated any longer (e.g. when having ResponseStatus SENT)
        if (!annotPermService.canAnnotationBeUpdated(ann)) {
            LOG.warn("Annotation with id '{}' is final (SENT) and cannot be updated.", annotationId);
            throw new CannotUpdateSentAnnotationException(
                    String.format("Annotation with id '%s' is final (responseStatus=SENT) and cannot be updated.", annotationId));
        }

        // check user permissions
        if (!annotPermService.hasUserPermissionToUpdateAnnotation(ann, userInfo.getLogin())) {
            LOG.warn("User '{}' does not have permission to update annotation with id '{}'.", userInfo.getLogin(), annotationId);
            throw new MissingPermissionException(userInfo.getLogin());
        }

        // only the following properties of the annotation can be updated:
        // - text
        // - shared
        // - updated (current timestamp)
        // - tags
        ann.setText(webAnnot.getText());
        ann.setShared(!isPrivateAnnotation(webAnnot));
        ann.setUpdated(LocalDateTime.now());

        final long originalMetadataId = ann.getMetadata().getId();
        updateTags(webAnnot, ann);
        updateGroup(webAnnot, ann, userInfo.getAuthority());

        try {
            annotRepos.save(ann);
        } catch (Exception e) {
            throw new CannotUpdateAnnotationException(e);
        }

        // update newly set properties for the response object - e.g. "Updated"
        webAnnot.setUpdated(ann.getUpdated());

        // if new metadata was assigned and the original metadata set is no longer referenced by any annotation, we can remove it
        // note: retrieving the ID of the associate metadata object is more reliable than asking for metadataId of annotation object!
        if (ann.getMetadata().getId() != originalMetadataId &&
                annotRepos.countByMetadataId(originalMetadataId) == 0) {
            metadataService.deleteMetadataById(originalMetadataId);
        }

        // add/update the status information
        webAnnot.setStatus(conversionService.getJsonAnnotationStatus(ann));

        return webAnnot;
    }

    /**
    * update the tags associated to an annotation
    * 
    * @param webAnnot incoming annotation containing updated tags
    * @param annot database annotation, tags may have to be updated
    */
    private void updateTags(final JsonAnnotation webAnnot, final Annotation annot) {

        // update the tags - due to hibernate mapping involved, we need to be more careful with this
        final boolean oldAnnotHasTags = annot.getTags() != null && !annot.getTags().isEmpty();
        final boolean newAnnotHasTags = webAnnot.getTags() != null && !webAnnot.getTags().isEmpty();

        if (oldAnnotHasTags && newAnnotHasTags) {
            // there were tags before and now, so we have to check more closely; comparing the total number of tags is not sufficient!
            // idea: check which ones to remove, which ones to add

            // retrieve those present in old annotation, but not contained in new annotation
            final List<Tag> tagsToRemove = annot.getTags().stream()
                    .filter(tag -> !webAnnot.getTags().contains(tag.getName()))
                    .collect(Collectors.toList());

            // retrieve those present in new annotation, but not contained in old annotation
            final List<String> tagsToAdd = webAnnot.getTags().stream()
                    .filter(tagString -> !annot.getTags().stream().anyMatch(tag -> tag.getName().equals(tagString)))
                    .collect(Collectors.toList());

            if (!tagsToRemove.isEmpty()) {
                annot.getTags().removeAll(tagsToRemove);

                // note: we also need to remove the tags from the internally stored list of items cached by hibernate
                // alternative could be two first remove all items, save, add new items, save again - could be overhead
                try {
                    final PersistentBag persBag = (PersistentBag) annot.getTags();
                    if (persBag.getStoredSnapshot() instanceof ArrayList) {
                        @SuppressWarnings("unchecked")
                        final List<Tag> snapshotList = (ArrayList<Tag>) (persBag.getStoredSnapshot());
                        snapshotList.removeAll(tagsToRemove);
                    }
                } catch (Exception e) {
                    LOG.error("Error removing cleaned tags: ", e);
                }

                // remove from database
                tagsService.removeTags(tagsToRemove);
            }

            // add new items
            if (!tagsToAdd.isEmpty()) {
                annot.getTags().addAll(tagsService.getTagList(tagsToAdd, annot));
            }

            // keep simple cases simple
        } else if (!oldAnnotHasTags && newAnnotHasTags) {

            // store all new tags
            annot.setTags(tagsService.getTagList(webAnnot.getTags(), annot));

        } else if (oldAnnotHasTags && !newAnnotHasTags) {

            // remove all existing tags
            tagsService.removeTags(annot.getTags());
            annot.setTags(null);
        }
        // last case (= !oldAnnotHasTags && !newAnnotHasTags): nothing to do
    }

    /**
     * update the group associated to an annotation, if necessary
     * technically, this means assigning a different metadata set
     * 
     * @param webAnnot incoming annotation possibly containing a new group
     * @param annot database annotation whose group may have to be updated
     * @param systemId the authority of the user requesting the update
     */
    private void updateGroup(final JsonAnnotation webAnnot, final Annotation annot, final String systemId) throws CannotUpdateAnnotationException {

        if (webAnnot.getGroup().equals(annot.getGroup().getName())) {
            LOG.trace("No need to update annotation's group, new and old are identical");
            return;
        }

        final Group newGroup = groupService.findGroupByName(webAnnot.getGroup());

        // search if there is already a metadata set for the group+document+systemId combination
        Metadata metadata = metadataService.findExactMetadata(annot.getDocument(), newGroup, systemId, annot.getMetadata());
        if (metadata == null) {

            // register the new metadata
            try {
                metadata = new Metadata(annot.getDocument(), newGroup, systemId);
                metadata.setKeyValuePropertyFromSimpleMetadata(webAnnot.getDocument().getMetadata());

                metadata = metadataService.saveMetadata(metadata);
            } catch (CannotCreateMetadataException ccme) {
                LOG.error("Metadata could not be persisted while creating annotation");
                throw new CannotUpdateAnnotationException(ccme);
            } catch (Exception e) {
                LOG.error("Received unexpected exception when trying to persist metadata during creation of annotation", e);
                throw new CannotUpdateAnnotationException(e);
            }
        }
        annot.setMetadata(metadata);
    }

    /**
     * delete an annotation in the database based on an annotation ID
     * 
     * @param annotationId
     *        the ID of the annotation to be deleted
     * @param userInfo
     *        information about the user requesting to delete an annotation
     * @throws CannotDeleteAnnotationException 
     *         the exception is thrown when the annotation cannot be deleted, e.g. when it is not existing or due to unexpected database error)
     * @throws CannotDeleteSentAnnotationException
     *         the exception is thrown when trying to delete a SENT annotation
     */
    @Override
    public void deleteAnnotationById(final String annotationId, final UserInformation userInfo)
            throws CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        Assert.isTrue(!StringUtils.isEmpty(annotationId), "Required annotation ID missing.");
        Assert.notNull(userInfo, ERROR_USERINFO_MISSING);

        final Annotation ann = findAnnotationById(annotationId);
        if (ann == null) {
            throw new CannotDeleteAnnotationException("Annotation not found");
        }

        if (ann.isResponseStatusSent() && !Authorities.isLeos(userInfo.getAuthority())) {
            LOG.info("Annotation '{}' has response status SENT and thus cannot be deleted in {}", ann.getId(), userInfo.getAuthority());
            throw new CannotDeleteSentAnnotationException("Annotation has response status SENT, cannot be deleted in " + userInfo.getAuthority());
        }

        final User user = userService.findByLogin(userInfo.getLogin());
        deleteAnnotation(ann, user.getId());
    }

    /**
     * delete a set of annotations in the database based on their IDs
     * 
     * @param annotationIds
     *        the list of IDs of the annotation to be deleted
     * @param userInfo
     *        information about the user requesting to delete annotations
     * @return returns a list of annotations that were successfully deleted
     */
    @Override
    public List<String> deleteAnnotationsById(final List<String> annotationIds, final UserInformation userInfo) {

        Assert.notNull(userInfo, ERROR_USERINFO_MISSING);

        final List<String> deleted = new ArrayList<String>();
        if (annotationIds == null || annotationIds.isEmpty()) {
            LOG.warn("No annotations for bulk deletion received.");
            return deleted;
        }

        final List<String> errors = new ArrayList<String>();

        // simply call the method for deleting a single annotation and keep track of success and errors
        for (final String annotationId : annotationIds) {
            try {
                deleteAnnotationById(annotationId, userInfo);
                deleted.add(annotationId);
            } catch (RuntimeException e) {
                LOG.warn("Error while deleting one of several annotations", e);
                throw e;
            } catch (Exception e) {
                errors.add(annotationId);
            }
        }
        LOG.info("Annotation bulk deletion: {} annotations deleted successfully, {} errors", deleted.size(), errors.size());

        return deleted;
    }

    /** 
     * search for annotations meeting certain criteria and for a given user
     * (user influences which other users' annotations are included)
     * 
     * NOTE: search for tags is currently not supported - seems a special functionality from hypothesis client, but does not respect documented API
     * 
     * @param options
     *        the {@link AnnotationSearchOptions} detailing search criteria like group, number of results, ...
     * @param userInfo
     *        information about the user for which the search is being executed
     *        
     * @return returns a list of {@link Annotation} objects meeting the search criteria
     *         returns an empty list in case search could not be run due to unfulfilled requirements  
     */
    @Override
    public AnnotationSearchResult searchAnnotations(final AnnotationSearchOptions options, final UserInformation userInfo) {

        Assert.notNull(userInfo, "User information not available");

        LOG.debug("User authenticated while searching for annotations: '{}'", userInfo.getLogin());

        Assert.notNull(options, "Cannot search without valid search parameters!");

        final AnnotationSearchResult emptyResult = new AnnotationSearchResult();

        // 1) URI / document
        final Document doc = documentService.findDocumentByUri(options.getUri());
        if (doc == null) {
            LOG.debug("No document registered yet for given search URI: {}", options.getUri());
            return emptyResult;
        }

        // 2) group
        final Group group = groupService.findGroupByName(options.getGroup());
        if (group == null) {
            LOG.debug("No group registered yet with given search group name: {}", options.getGroup());
            return emptyResult;
        }

        // 3) check that the user requesting the search actually is member of the requested group
        // if not, he will not see any result
        final User executingUser = getExecutingUser(userInfo, group);
        if (executingUser == null) {
            LOG.warn("Unable to determine user running search query");
            return emptyResult;
        }

        // 4) user (optional)
        User user = null;
        if (!StringUtils.isEmpty(options.getUser())) {
            user = userService.findByLogin(options.getUser());
            if (user == null) {
                // break instead of ignoring user (which would produce more search results and open an information leak)
                LOG.debug("No user registered yet with given user name: {}, so there cannot be any matches", options.getUser());
                return emptyResult;
            }
        }

        // 5) sorting, ordering, limit and offset
        // our own Pageable implementation allows handing over sorting, limit and especially offset requirements
        final Pageable pageable = new OffsetBasedPageRequest(options.getItemOffset(), options.getItemLimit(), options.getSort());

        try {
            // wrap up all available information in order to retrieve matching search model
            final ResolvedSearchOptions rso = new ResolvedSearchOptions();
            rso.setDocument(doc);
            rso.setGroup(group);
            rso.setExecutingUserToken(userInfo.getCurrentToken());
            rso.setExecutingUser(executingUser);
            rso.setFilterUser(user);
            rso.setMetadataWithStatusesList(options.getMetadataMapsWithStatusesList());

            return executeSearch(rso, pageable);

        } catch (Exception ex) {
            LOG.error("Search in annotation repository produced unexpected error!");
            throw ex;
        }
    }

    /**
     * determine the {@link User} running the search - and check if he is member of the requested group at all
     * 
     * @param userInfo {@link UserInformation} containing user details
     * @param group {@link Group} for which the user requested to retrieve information
     * @return {@link User} object being member of given group, or {@literal null}
     */
    private User getExecutingUser(final UserInformation userInfo, final Group group) {

        User executingUser = userInfo.getUser();
        if (executingUser == null) {
            executingUser = userService.findByLogin(userInfo.getLogin());
        }

        if (!groupService.isUserMemberOfGroup(executingUser, group)) {
            LOG.info("User {} is not member of group {}, so he may not see any content", executingUser.getLogin(), group.getName());
            return null;
        }

        return executingUser;
    }

    /**
     * method that actually executes the search given the final set of options
     * 
     * @param rso {@link ResolvedSearchOptions} containing all query parameters
     * @param pageable {@link OffsetBasedPageRequest} containing search parameters (limit, offset, sorting)
     * 
     * @return {@link AnnotationSearchResult} containing found results
     */
    private AnnotationSearchResult executeSearch(final ResolvedSearchOptions rso, final Pageable pageable) {

        final SearchModel searchModel = searchModelFactory.getSearchModel(rso);
        if (searchModel == null) {
            LOG.warn("No suitable search model found or no matching DB content found");
            return new AnnotationSearchResult(); // empty result
        }

        final AnnotationSearchResult result = new AnnotationSearchResult();
        result.setSearchModelUsed(searchModel);

        // search
        final Page<Annotation> resultPage = annotRepos.findAll(searchModel.getSearchSpecification(), pageable);
        result.setItems(resultPage.getContent());
        result.setTotalItems(resultPage.getTotalElements());

        return result;
    }

    /**
     * search for the replies belonging to a given set of annotations
     * (given user influences which other users' replies are included)
     * 
     * NOTE: search for tags is currently not supported - seems a special functionality from hypothesis client, but does not respect documented API
     * 
     * @param annotSearchRes
     *        the {@link AnnotationSearchResult} resulting from a previously executed search for (root) annotations
     * @param options
     *        the search options - required for sorting and ordering
     * @param userInfo
     *        information about the user requesting the search ({@link UserInformation})
     *        
     * @return returns a list of Annotation objects meeting belonging to the annotations and visible for the user
     */
    @Override
    public List<Annotation> searchRepliesForAnnotations(final AnnotationSearchResult annotSearchRes,
            final AnnotationSearchOptions options,
            final UserInformation userInfo) {

        if (annotSearchRes == null) {
            LOG.debug("No annotation search result received to search for answers");
            return null;
        }

        if (CollectionUtils.isEmpty(annotSearchRes.getItems())) {
            LOG.debug("No annotations received to search for belonging answers");
            return annotSearchRes.getItems();
        }

        Assert.notNull(options, "Cannot search for replies without search options!");
        Assert.notNull(userInfo, "Cannot search for replies without authenticated user!");

        // extract all the IDs of the given annotations
        final List<String> annotationIds = annotSearchRes.getItems().stream().map(Annotation::getId).collect(Collectors.toList());

        // notes:
        // - we want to retrieve ALL replies, no matter how many there are!
        // - in the database, replies have ALL their parent IDs filled in the "References" field;
        // the top-most parent element is contained in the computed "Root" field
        // -> so in order to retrieve all replies contained in the subtree of an annotation A,
        // we have to find all annotations having A's ID set in the "Root" field
        // we keep the sorting and ordering options

        final Pageable pageable = new OffsetBasedPageRequest(0, Integer.MAX_VALUE, options.getSort()); // hand over sorting

        List<Annotation> result = null;
        try {

            final User executingUser = userService.findByLogin(userInfo.getLogin());

            // the users belonging to the group are required for knowing which other users' public
            // annotations are visible for the executing user
            final List<Long> userIdsOfGroup = groupService.getUserIdsOfGroup(options.getGroup());

            final Page<Annotation> resultPage = annotRepos.findAll(
                    new AnnotationReplySearchSpec(annotationIds, executingUser.getId(), userIdsOfGroup), pageable);
            result = resultPage.getContent();

            // now we need to filter out such replies which should not be found according to their status
            // note: this would be too complicated to do on the database level, therefore it is performed as a postprocessing step
            final List<Annotation> filteredList = new ArrayList<Annotation>();
            final List<MetadataIdsAndStatuses> metaIdsStats = annotSearchRes.getSearchModelUsed().getMetadataAndStatusesList();
            for (final Annotation rep : result) {
                final String parentId = rep.getRootAnnotationId();
                final Annotation parentAnnot = annotSearchRes.getItems().stream().filter(ann -> ann.getId().equals(parentId)).findFirst().get(); // must be
                                                                                                                                                 // there by
                                                                                                                                                 // construction
                final long parentMetaId = parentAnnot.getMetadataId();

                final List<AnnotationStatus> allowedStatus = metaIdsStats.stream()
                        .filter(mis -> mis.getMetadataIds().contains(parentMetaId))
                        .map(MetadataIdsAndStatuses::getStatuses)
                        .flatMap(list -> list.stream()) // removes nested lists
                        .distinct() // filter out duplicates
                        .collect(Collectors.toList());

                // now finally if the found reply has one of the found allowed status, if might pass
                if (allowedStatus.contains(rep.getStatus())) {
                    filteredList.add(rep);
                }
            }
            result = filteredList;
        } catch (Exception ex) {
            LOG.error("Search for replies in annotation repository produced unexpected error!");
            throw ex;
        }
        return result;
    }

    /**
     * retrieve the number of annotations for a given document/group/metadata
     * hereby, only public annotations are counted, and highlights are ignored
     * 
     * @param options the options for the retrieval of annotations
     * @param userInfo information about the user requesting the number of annotations
     * 
     * @throws MissingPermissionException
     *         this exception is thrown when requesting user is no ISC user, 
     *         or when other than ISC annotations are wanted
     * @return number of annotations, or -1 when retrieval could not even be launched
     *         due to failing precondition checks
     */
    @Override
    public int getAnnotationsCount(final AnnotationSearchCountOptions options, final UserInformation userInfo)
            throws MissingPermissionException {

        Assert.notNull(userInfo, "User information not available");

        LOG.debug("User authenticated while searching for annotations: '{}'", userInfo.getLogin());
        if (!Authorities.isIsc(userInfo.getAuthority())) {
            throw new MissingPermissionException("Only permitted for ISC users");
        }

        Assert.notNull(options, "Cannot search without valid search parameters!");

        final int emptyResult = -1;

        // 1) URI / document
        final Document doc = documentService.findDocumentByUri(options.getUri());
        if (doc == null) {
            LOG.debug("No document registered yet for given search URI: {}", options.getUri());
            return emptyResult;
        }

        // 2) group
        final Group group = groupService.findGroupByName(options.getGroup());
        if (group == null) {
            LOG.debug("No group registered yet with given search group name: {}", options.getGroup());
            return emptyResult;
        }

        // 3) check that the user requesting the search actually is member of the requested group
        // if not, he will not see any result
        final User executingUser = getExecutingUser(userInfo, group);
        if (executingUser == null) {
            LOG.warn("Unable to determine user running search query");
            return emptyResult;
        }

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setDocument(doc);
        rso.setGroup(group);

        final List<MetadataIdsAndStatuses> metadataIdsAndStatuses = getMetadataIdsForAnnotationsCount(options.getMetadatasets(), rso);
        if (metadataIdsAndStatuses == null) {
            return 0; // reason was logged already
        }

        // finally search
        try {
            return (int) annotRepos.count(new AnnotationCountSearchSpec(metadataIdsAndStatuses));

        } catch (Exception ex) {
            LOG.error("Search in annotation repository for number of annotations produced unexpected error!");
            throw ex;
        }
    }

    /**
     * processing of the metadata for counting annotations (externalised to reduce complexity)
     * 
     * @param options given JSON-encoded metadata sets to match
     * @param rso {@link ResolvedSearchOptions} containing document and group; 
     * @return list of retrieved IDs of metadata sets matching
     * @throws MissingPermissionException if non-ISC annotations are requested
     */
    private List<MetadataIdsAndStatuses> getMetadataIdsForAnnotationsCount(final String metadataSets,
            final ResolvedSearchOptions rso) throws MissingPermissionException {

        rso.setMetadataWithStatusesList(metadataSets);

        // check that ISC authority is requested, or no authority (and we thus assume ISC)
        if (!rso.getMetadataWithStatusesList().isEmpty()) {
            // creating a {@link Metadata} instance by using the given map, the system Id of the map is used
            final Metadata metadataHelp = new Metadata(rso.getDocument(), rso.getGroup(), Authorities.ISC);
            throwIfNonIscRequested(metadataHelp, rso);
        }

        // note: we do no longer perform an exact match, but instead AT LEAST all given metadata must match
        // so first look for candidates associated to document, group and system ID
        final List<Metadata> metaCandidates = metadataService.findMetadataOfDocumentGroupSystemid(rso.getDocument(), rso.getGroup(), Authorities.ISC);
        if (metaCandidates.isEmpty()) {
            LOG.info("No corresponding metadata found for document/group/ISC");
            return null;
        }

        final List<MetadataIdsAndStatuses> metaAndStatus = new ArrayList<MetadataIdsAndStatuses>();
        for (final SimpleMetadataWithStatuses smws : rso.getMetadataWithStatusesList()) {

            // now check if these candidates have at least the metadata received via the search options
            final List<Long> metadataIds = metadataService.getIdsOfMatchingMetadatas(metaCandidates, smws.getMetadata());
            if (metadataIds == null) { // our function returns {@literal null} or a list with content, but no empty list
                continue;
            }
            metaAndStatus.add(new MetadataIdsAndStatuses(metadataIds, smws.getStatuses()));
        }

        if (CollectionUtils.isEmpty(metaAndStatus)) {
            LOG.info("No corresponding metadata fulfilling search criteria found in DB");
            return null;
        }

        return metaAndStatus;
    }

    /**
     * checks if non-ISC data was requested in search options; if so: throws exception
     * 
     * @param metadataHelp dummy object having main metadata properties already (time saver)
     * @param rso filled {@link ResolvedSearchOptions} containing requested metadata sets
     * 
     * @throws MissingPermissionException thrown if any of the metadata sets requests non-ISC data
     */
    private void throwIfNonIscRequested(final Metadata metadataHelp, final ResolvedSearchOptions rso)
            throws MissingPermissionException {

        for (final SimpleMetadataWithStatuses requested : rso.getMetadataWithStatusesList()) {
            metadataHelp.setKeyValuePropertyFromSimpleMetadata(requested.getMetadata());
            if (!Authorities.isIsc(metadataHelp.getSystemId())) {
                // other authority queried -> refuse
                throw new MissingPermissionException("Only querying ISC is allowed");
            }
        }
    }

    /**
     * accept a suggestion, taking permissions and authority into account
     * 
     * @param suggestionId
     *        the ID of the suggestion (annotation) to be accepted
     * @param userInfo
     *        information about the user requesting to accept the suggestion
     *        
     * @return returns the found annotation object, or {@literal null}
     * @throws CannotAcceptSuggestionException
     *         this exception is thrown when the referenced suggestion does not exist
     * @throws CannotAcceptSentSuggestionException
     *         this exception is thrown when the annotation already has response status SENT
     * @throws NoSuggestionException
     *         this exception is thrown when the referenced annotation is not a suggestion, but a different kind of annotation
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to accept the suggestion 
     */
    @Override
    public void acceptSuggestionById(final String suggestionId, final UserInformation userInfo)
            throws CannotAcceptSuggestionException, CannotAcceptSentSuggestionException,
            NoSuggestionException, MissingPermissionException {

        Assert.isTrue(!StringUtils.isEmpty(suggestionId), "Required suggestion/annotation ID missing.");
        Assert.notNull(userInfo, ERROR_USERINFO_MISSING);
        
        final Annotation ann = findAnnotationById(suggestionId);
        if (ann == null) {
            throw new CannotAcceptSuggestionException("Suggestion not found");
        }

        if (!isSuggestion(ann)) {
            throw new NoSuggestionException("Given ID '" + suggestionId + "' does not represent a suggestion");
        }

        // in ISC, accepting a suggestion is not allowed - but in LEOS!
        if (ann.isResponseStatusSent() && !Authorities.isLeos(userInfo.getAuthority())) {
            LOG.info("Annotation/suggestion '{}' has response status SENT and thus cannot be accepted in {}", ann.getId(), userInfo.getAuthority());
            throw new CannotAcceptSentSuggestionException("Annotation/suggestion has response status SENT, cannot be accepted in " + userInfo.getAuthority());
        }

        final User user = userInfo.getUser();
        if (!annotPermService.hasUserPermissionToAcceptSuggestion(ann, user)) {
            final String login = user == null ? "unknown user" : user.getLogin();
            LOG.warn("User '{}' does not have permission to accept suggestion/annotation with id '{}'.", login, suggestionId);
            throw new MissingPermissionException(login);
        }

        acceptAnnotation(ann, user.getId());
    }

    /**
     * reject a suggestion, taking permissions and authority into account
     * 
     * @param suggestionId
     *        the ID of the suggestion (annotation) to be rejected
     * @param userInfo
     *        information about the user requesting to reject the suggestion
     *        
     * @return returns the found annotation object, or {@literal null}
     * @throws CannotRejectSuggestionException
     *         this exception is thrown when the referenced suggestion does not exist
     * @throws NoSuggestionException
     *         this exception is thrown when the referenced annotation is not a suggestion, but a different kind of annotation
     * @throws CannotRejectSentSuggestionException
     *         this exception is thrown when the annotation has response status SENT
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to reject the suggestion
     */
    @Override
    public void rejectSuggestionById(final String suggestionId, final UserInformation userInfo)
            throws CannotRejectSuggestionException, NoSuggestionException,
            MissingPermissionException, CannotRejectSentSuggestionException {

        Assert.isTrue(!StringUtils.isEmpty(suggestionId), "Required suggestion/annotation ID missing.");
        Assert.notNull(userInfo, ERROR_USERINFO_MISSING);
        
        final Annotation ann = findAnnotationById(suggestionId);
        if (ann == null) {
            throw new CannotRejectSuggestionException("Suggestion not found");
        }

        if (!isSuggestion(ann)) {
            throw new NoSuggestionException("Given ID '" + suggestionId + "' does not represent a suggestion");
        }

        // in ISC, rejecting a suggestion is not allowed - but in LEOS!
        if (ann.isResponseStatusSent() && !Authorities.isLeos(userInfo.getAuthority())) {
            LOG.info("Annotation/suggestion '{}' has response status SENT and thus cannot be rejected in {}", ann.getId(), userInfo.getAuthority());
            throw new CannotRejectSentSuggestionException("Annotation/suggestion has response status SENT, cannot be rejected in " + userInfo.getAuthority());
        }

        final User user = userInfo.getUser();
        if (!annotPermService.hasUserPermissionToRejectSuggestion(ann, user)) {
            final String login = user == null ? "unknown user" : user.getLogin();
            LOG.warn("User '{}' does not have permission to reject suggestion/annotation with id '{}'.", login, suggestionId);
            throw new MissingPermissionException(login);
        }

        rejectAnnotation(ann, user.getId());
    }

    /**
     * check whether a given annotation represents a suggestion
     * 
     * @param sugg the annotation to be checked
     * 
     * @return true if the annotation was identified as a suggestion
     */
    @Override
    public boolean isSuggestion(final Annotation sugg) {

        Assert.notNull(sugg, "Required suggestion missing");

        return tagsService.hasSuggestionTag(sugg.getTags());
    }

    /**
     * deletion of an annotation
     * (recursive if annotation is a root annotation)
     *  
     * @param annot the annotation to be deleted
     * @param userId ID of the user requesting rejection
     * 
     * @throws CannotDeleteAnnotationException
     *         the exception is thrown when the annotation cannot be deleted, e.g. when it is not existing or due to unexpected database error)
     */
    private void deleteAnnotation(final Annotation annot, final long userId) throws CannotDeleteAnnotationException {

        /**
         * note:
         * due to the database schema construction, there is a foreign key constraint on the 'root' column;
         * this makes all replies be deleted in case the thread's root annotation is deleted;
         * however, with our soft delete, the "DELETED" information is not automatically propagated
         *
         * to propagate the information, a trigger would be a good option, but this fails as the trigger would
         *  change other data of the table and thus again activate the trigger (ORA-04091)
         * 
         * so we have to do it ourselves: if a thread root is deleted, we delete all children;
         *  if it is not the root, only the particular annotation is removed, but NO child
         */

        try {
            updateAnnotationStatus(annot, AnnotationStatus.DELETED, userId);
        } catch (CannotUpdateAnnotationException cuae) {
            // wrap into more specific exception
            throw new CannotDeleteAnnotationException(cuae);
        }
    }

    /**
     * accepting an annotation
     * (recursive if annotation is a root annotation)
     *  
     * @param annot the annotation to be accepted
     * @param userId ID of the user requesting rejection
     * 
     * @throws CannotAcceptSuggestionException
     *         the exception is thrown when the annotation cannot be accepted, e.g. when it is not existing or due to unexpected database error)
     */
    private void acceptAnnotation(final Annotation annot, final long userId) throws CannotAcceptSuggestionException {

        // cfr. comment in {@link deleteAnnotation}

        try {
            updateAnnotationStatus(annot, AnnotationStatus.ACCEPTED, userId);
        } catch (CannotUpdateAnnotationException cuae) {
            // wrap into more specific exception
            throw new CannotAcceptSuggestionException(cuae);
        }
    }

    /**
     * rejecting an annotation
     * (recursive if annotation is a root annotation)
     *  
     * @param annot the annotation to be rejected
     * @param userId ID of the user requesting rejection
     * 
     * @throws CannotRejectSuggestionException
     *         the exception is thrown when the annotation cannot be rejected, e.g. when it is not existing or due to unexpected database error)
     */
    private void rejectAnnotation(final Annotation annot, final long userId) throws CannotRejectSuggestionException {

        // cfr. comment in {@link deleteAnnotation}

        try {
            updateAnnotationStatus(annot, AnnotationStatus.REJECTED, userId);
        } catch (CannotUpdateAnnotationException cuae) {
            // wrap into more specific exception
            throw new CannotRejectSuggestionException(cuae);
        }
    }

    /**
     * method for updating the status of an annotation (recursive, if needed, e.g. for root annotations)
     * 
     * @param annot annotation (root) to be updated
     * @param newStatus the new status to be applied
     * @param userId internal DB id of the user requesting status change
     * 
     * @throws CannotUpdateAnnotationException
     *         exception thrown when saving the updated annotation fails
     */
    private void updateAnnotationStatus(final Annotation annot, final AnnotationStatus newStatus, final long userId) throws CannotUpdateAnnotationException {

        try {
            annot.setStatus(newStatus);
            annot.setStatusUpdated(LocalDateTime.now());
            annot.setStatusUpdatedBy(userId);
            annotRepos.save(annot);
        } catch (Exception e) {
            LOG.error("Error updating annotation status for annotation with ID '" + annot.getId() + "' to status '" + newStatus.toString() + '"', e);
            throw new CannotUpdateAnnotationException(e);
        }

        final boolean isThreadRoot = StringUtils.isEmpty(annot.getRootAnnotationId());

        if (isThreadRoot) {
            // get all children - fortunately, they all have the same root, no matter how many layers might be between the root and a successor
            // note: we only adapt those items still being in "NORMAL" state (since e.g. we don't want to change an already DELETED item to ACCEPTED)
            final List<Annotation> children = annotRepos.findByRootAnnotationIdIsInAndStatus(Arrays.asList(annot.getId()), AnnotationStatus.NORMAL, null);
            for (final Annotation child : children) {
                child.setStatus(newStatus);
                child.setStatusUpdated(LocalDateTime.now());
                child.setStatusUpdatedBy(userId);
                try {
                    annotRepos.save(child);
                } catch (Exception e) {
                    LOG.error("Error soft-deleting child '" + child.getId() + "' of annotation '" + annot.getId() + "'", e);
                }
            }
        }
    }

    /**
     * checks if an annotation is visible for "only me" this is done based on the read permissions set: 
     *  if it is readable for the user only, it is private
     * 
     * @param annotation
     *        {@link JsonAnnotation} object to be checked
     * @return flag indicating whether annotation is meant to be visible for "only me"
     */
    private boolean isPrivateAnnotation(final JsonAnnotation annotation) {

        if (annotation == null) {
            LOG.error("Annotation to be checked for privacy is invalid!");
            return false;
        }

        final JsonAnnotationPermissions perms = annotation.getPermissions();
        if (perms == null) {
            LOG.error("Annotation to be checked for privacy does not contain any permission information!");
            return false;
        }

        final List<String> readPerms = perms.getRead();
        if (readPerms == null || readPerms.isEmpty()) {
            LOG.error("Annotation to be checked for privacy does not contain any read permissions!");
            return false;
        }

        // the annotation is private if and only if the user itself has read permissions
        return readPerms.size() == 1 &&
                readPerms.get(0).equals(annotation.getUser());
    }

    /**
    * get a list of all {@link Annotation} objects that have certain metadata sets associated
    * 
    * @param metadataIds the IDs of the metadata sets associated to the annotations
    * 
    * @return non-empty list of String
    */
    @Override
    @Nonnull
    public List<String> getAnnotationIdsOfMetadata(final List<Long> metadataIds) {

        final List<Annotation> annots = annotRepos.findByMetadataIdIsInAndStatus(metadataIds, AnnotationStatus.NORMAL);
        if (annots.isEmpty()) { // repository returns empty list if no matches are found
            return new ArrayList<String>();
        }

        return annots.stream().map(Annotation::getId).collect(Collectors.toList());
    }

}