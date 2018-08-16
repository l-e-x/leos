/*
 * Copyright 2018 European Commission
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

import eu.europa.ec.leos.annotate.model.AnnotationComparator;
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.*;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserInfo;
import eu.europa.ec.leos.annotate.repository.AnnotationRepository;
import eu.europa.ec.leos.annotate.repository.impl.AnnotationReplySearchSpec;
import eu.europa.ec.leos.annotate.repository.impl.AnnotationSearchSpec;
import eu.europa.ec.leos.annotate.services.*;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import org.hibernate.collection.internal.PersistentBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.activation.UnknownGroupException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for annotation administration functionality 
 */
@Service
public class AnnotationServiceImpl implements AnnotationService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationServiceImpl.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationRepository annotRepos;

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
    private UUIDGeneratorService uuidService;

    @Value("${default.systemId}")
    private String DEFAULT_SYSTEM_ID;

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
     * @return returns the found annotation object, or null
     */
    @Override
    public Annotation findAnnotationById(String annotId) {

        return annotRepos.findById(annotId);
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
    public Annotation findAnnotationById(String annotId, String userlogin) throws MissingPermissionException {

        if (StringUtils.isEmpty(annotId)) {
            LOG.error("Cannot search for annotation, no annotation ID specified.");
            throw new IllegalArgumentException("Required annotation ID missing");
        }

        // retrieve the annotation first...
        Annotation resultAnnotation = findAnnotationById(annotId);
        if (resultAnnotation == null) {
            LOG.error("The wanted annotation with id '{}' could not be found.", annotId);
            return null;
        }

        // ... then check the permissions
        // note: we did not combine the permission check directly with the database query here in order to be able
        // to easily distinguish between annotation being missing and annotation not being permitted to be viewed
        User user = userService.findByLogin(userlogin);
        if (!hasUserPermissionToSeeAnnotation(resultAnnotation, user)) {
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
     * @param userLogin
     *        the user's login
     * @throws CannotCreateAnnotationException 
     *         the exception is thrown when the annotation cannot be created due to unfulfilled constraints
     *         (e.g. missing document or user information)
     */
    @Override
    public JsonAnnotation createAnnotation(JsonAnnotation webAnnot, String userLogin) throws CannotCreateAnnotationException {

        // find belonging user in DB
        User registeredUser = userService.findByLogin(userLogin);
        if (registeredUser == null) {
            // user was already created during client's initialisation (i.e. token exchange/profile retrieval)
            // so it should be known here unless an error had occurred before
            // therefore we don't do anything if user is still missing now
            throw new CannotCreateAnnotationException(new UserNotFoundException(userLogin));
        }

        // find belonging group in DB
        Group group = groupService.findGroupByName(webAnnot.getGroup());
        if (group == null) {
            LOG.error("Cannot create annotation as associate group is unknown");
            throw new CannotCreateAnnotationException(new UnknownGroupException(webAnnot.getGroup()));
        }

        // search if document is already contained in DB
        Document document = documentService.findDocumentByUri(webAnnot.getUri());
        if (document == null) {

            // register new document
            try {
                if (webAnnot.getDocument() != null) {
                    // new top-level annotations have a document object containing URI and title
                    document = documentService.createNewDocument(webAnnot.getDocument());
                } else {
                    // replies to annotations do not have the document object, but still feature the URI
                    document = documentService.createNewDocument(webAnnot.getUri());
                }
            } catch (CannotCreateDocumentException e) {
                LOG.error("Cannot create annotation as associate document could not be registered");
                throw new CannotCreateAnnotationException(e);
            }
        }

        // determine system ID
        String systemId = DEFAULT_SYSTEM_ID;
        if (webAnnot.getDocument() != null && webAnnot.getDocument().getMetadata() != null) {
            Metadata helpMeta = new Metadata();
            helpMeta.setKeyValuePropertyFromHashMap(webAnnot.getDocument().getMetadata());
            if (!StringUtils.isEmpty(helpMeta.getSystemId())) {
                systemId = helpMeta.getSystemId();
            }
        }

        // search if there is already a metadata set for the group+document+systemId combination
        Metadata metadata = metadataService.findByDocumentAndGroupAndSystemId(document, group, systemId);
        if (metadata == null) {

            // register the new metadata
            try {
                metadata = new Metadata(document, group, systemId);
                metadata.setKeyValuePropertyFromHashMap(webAnnot.getDocument().getMetadata());

                metadata = metadataService.saveMetadata(metadata);
            } catch (CannotCreateMetadataException ccme) {
                LOG.error("Metadata could not be persisted while creating annotation");
                throw new CannotCreateAnnotationException(ccme);
            } catch (Exception e) {
                LOG.error("Received unexpected exception when trying to persist metadata during creation of annotation", e);
                throw new CannotCreateAnnotationException(e);
            }
        }

        // save the annotation with all required reference IDs
        Annotation annot = new Annotation();
        annot.setCreated(webAnnot.getCreated());
        annot.setDocument(document);
        annot.setGroup(group);
        annot.setMetadata(metadata);
        annot.setUser(registeredUser);
        annot.setId(uuidService.generateUrlSafeUUID()); // as a new annotation is saved, we set an ID, regardless whether one was present before!
        annot.setReferences(webAnnot.getReferences()); // take over all referenced annotations (is 'null' for new top-level annotations, filled for replies)
        annot.setShared(!isPrivateAnnotation(webAnnot));
        annot.setText(webAnnot.getText());
        annot.setTargetSelectors(webAnnot.getSerializedTargets());
        annot.setUpdated(LocalDateTime.now());

        // save tags, if present
        if (webAnnot.getTags() != null && webAnnot.getTags().size() > 0) {
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
     * update an existing annotation in the database based on an incoming, JSON-deserialized annotation
     * 
     * @param webAnnot
     *        the incoming annotation
     * @throws CannotUpdateAnnotationException 
     *         the exception is thrown when the annotation cannot be updated (e.g. when it is not existing)
     * @throws MissingPermissionException 
     *         the exception is thrown when the user lacks permissions for updating the annotation
     */
    @Override
    public JsonAnnotation updateAnnotation(String annotationId, JsonAnnotation webAnnot, String username)
            throws CannotUpdateAnnotationException, MissingPermissionException {

        if (StringUtils.isEmpty(annotationId)) {
            throw new IllegalArgumentException("Required annotation ID missing.");
        }

        final Annotation ann = findAnnotationById(annotationId);
        if (ann == null) {
            throw new CannotUpdateAnnotationException("Annotation not found");
        }

        // check permissions
        if (!hasUserPermissionToUpdateAnnotation(ann, username)) {
            LOG.warn("User '{}' does not have permission to update annotation with id '{}'.", username, annotationId);
            throw new MissingPermissionException(username);
        }

        // only the following properties of the annotation can be updated:
        // - text
        // - shared
        // - updated (current timestamp)
        // - tags
        ann.setText(webAnnot.getText());
        ann.setShared(!isPrivateAnnotation(webAnnot));
        ann.setUpdated(LocalDateTime.now());

        // update the tags - due to hibernate mapping involved, we need to be more careful with this
        boolean oldAnnotHasTags = ann.getTags() != null && ann.getTags().size() > 0;
        boolean newAnnotHasTags = webAnnot.getTags() != null && webAnnot.getTags().size() > 0;

        // keep simple cases simple
        if (!oldAnnotHasTags && !newAnnotHasTags) {

            // nothing to do

        } else if (!oldAnnotHasTags && newAnnotHasTags) {

            // store all new tags
            ann.setTags(tagsService.getTagList(webAnnot.getTags(), ann));

        } else if (oldAnnotHasTags && !newAnnotHasTags) {

            // remove all existing tags
            tagsService.removeTags(ann.getTags());
            ann.setTags(null);

        } else {
            // there were tags before and now, so we have to check more closely; comparing the total number of tags is not sufficient!
            // idea: check which ones to remove, which ones to add

            // retrieve those present in old annotation, but not contained in new annotation
            List<Tag> tagsToRemove = ann.getTags().stream().filter(tag -> !webAnnot.getTags().contains(tag.getName())).collect(Collectors.toList());

            // retrieve those present in new annotation, but not contained in old annotation
            List<String> tagsToAdd = webAnnot.getTags().stream().filter(tagString -> !ann.getTags().stream().anyMatch(tag -> tag.getName().equals(tagString)))
                    .collect(Collectors.toList());

            if (tagsToRemove.size() > 0) {
                ann.getTags().removeAll(tagsToRemove);

                // note: we also need to remove the tags from the internally stored list of items cached by hibernate
                // alternative could be two first remove all items, save, add new items, save again - could be overhead
                try {
                    PersistentBag pb = (PersistentBag) ann.getTags();
                    if (pb.getStoredSnapshot() instanceof ArrayList) {
                        @SuppressWarnings("unchecked")
                        List<Tag> snapshotList = (ArrayList<Tag>) (pb.getStoredSnapshot());
                        snapshotList.removeAll(tagsToRemove);
                    }
                } catch (Exception e) {
                    LOG.error("Error removing cleaned tags: ", e);
                }

                // remove from database
                tagsService.removeTags(tagsToRemove);
            }

            // add new items
            if (tagsToAdd.size() > 0) {
                ann.getTags().addAll(tagsService.getTagList(tagsToAdd, ann));
            }
        }

        try {
            annotRepos.save(ann);
        } catch (Exception e) {
            throw new CannotUpdateAnnotationException(e);
        }

        // update newly set properties for the response object - e.g. "Updated"
        webAnnot.setUpdated(ann.getUpdated());

        return webAnnot;
    }

    /**
     * delete an annotation in the database based on an annotation ID
     * 
     * @param annotationId
     *        the ID of the annotation to be deleted
     * @param userlogin
     *        login of the user requesting to delete an annotation
     * @throws CannotDeleteAnnotationException 
     *         the exception is thrown when the annotation cannot be deleted, e.g. when it is not existing or due to unexpected database error)
     */
    @Override
    public void deleteAnnotationById(String annotationId, String userlogin) throws CannotDeleteAnnotationException {

        if (StringUtils.isEmpty(annotationId)) {
            throw new IllegalArgumentException("Required annotation ID missing.");
        }

        Annotation ann = findAnnotationById(annotationId);
        if (ann == null) {
            throw new CannotDeleteAnnotationException("Annotation not found");
        }

        deleteAnnotationAndOrphanedDocument(ann);
    }

    /**
     * search for annotations meeting certain criteria and for a given user
     * (user influences which other users' annotations are included)
     * 
     * NOTE: search for tags is currently not supported - seems a special functionality from hypothesis client, but does not respect documented API
     * 
     * @param options
     *        the {@link AnnotationSearchOptions} detailing search criteria like group, number of results, ...
     * @param userlogin
     *        the login of the user for which the search is being executed
     *        
     * @return returns a list of Annotation objects meeting the search criteria
     *         returns an empty list in case search could not be run due to unfulfilled requirements  
     */
    @Override
    public List<Annotation> searchAnnotations(AnnotationSearchOptions options, String userlogin) {

        final List<Annotation> emptyResult = new ArrayList<Annotation>();

        if (StringUtils.isEmpty(userlogin)) {
            LOG.error("Cannot search without user!");
            throw new IllegalArgumentException("User login empty");
        }

        if (options == null) {
            LOG.error("Cannot search without valid search parameters!");
            throw new IllegalArgumentException("Options empty");
        }

        // 1) URI
        Document doc = documentService.findDocumentByUri(options.getUri());
        if (doc == null) {
            LOG.debug("No document registered yet for given search URI: " + options.getUri());
            return emptyResult;
        }

        // 2) group
        Group group = groupService.findGroupByName(options.getGroup());
        if (group == null) {
            LOG.debug("No group registered yet with given search group name: " + options.getGroup());
            return emptyResult;
        }

        // 3) user (optional)
        User user = null;
        if (options.getUser() != null && !options.getUser().isEmpty()) {
            user = userService.findByLogin(options.getUser());
            if (user == null) {
                // break instead of ignoring user (which would produce more search results and open
                // an information leak)
                LOG.debug("No user registered yet with given user name: {}, so there cannot be any matches", options.getUser());
                return emptyResult;
            }
        }

        // 4) sorting, ordering, limit and offset
        // our own Pageable implementation allows handing over sorting, limit and especially offset requirements
        Pageable pageable = new OffsetBasedPageRequest(options.getItemOffset(), options.getItemLimit(), options.getSort());

        List<Annotation> result = null;
        try {

            User executingUser = userService.findByLogin(userlogin);
            Long optionalUserIdToFilter = (user == null ? null : user.getId());

            // the users belonging to the group are required for knowing which other users' public
            // annotations are visible for the executing user
            List<Long> userIdsOfGroup = groupService.getUserIdsOfGroup(group);

            Page<Annotation> resultPage = annotRepos.findAll(
                    new AnnotationSearchSpec(executingUser.getId(), doc.getId(), group.getId(), optionalUserIdToFilter, userIdsOfGroup), pageable);
            result = resultPage.getContent();

        } catch (Exception ex) {
            LOG.error("Search in annotation repository produced unexpected error!");
            throw ex;
        }
        return result;
    }

    /**
     * search for the replies belonging to a given set of annotations
     * (given user influences which other users' replies are included)
     * 
     * NOTE: search for tags is currently not supported - seems a special functionality from hypothesis client, but does not respect documented API
     * 
     * @param annotations
     *        the set of {@link Annotation}s whose replies are wanted
     * @param options
     *        the search options - required for sorting and ordering
     * @param userlogin
     *        the login of the user for which the search is being executed
     *        
     * @return returns a list of Annotation objects meeting belonging to the annotations and visible for the user
     */
    @Override
    public List<Annotation> searchRepliesForAnnotations(List<Annotation> annotations, AnnotationSearchOptions options, String userlogin) {

        if (annotations == null || annotations.size() == 0) {
            LOG.debug("No annotations received to search for belonging answers");
            return annotations;
        }

        if (StringUtils.isEmpty(userlogin)) {
            LOG.error("Cannot search for replies without user!");
            throw new IllegalArgumentException("User login empty");
        }

        // extract all the IDs of the given annotations
        List<String> annotationIds = annotations.stream().map(Annotation::getId).collect(Collectors.toList());

        // notes:
        // - we want to retrieve ALL replies, no matter how many there are!
        // - in the database, replies have ALL their parent IDs filled in the "References" field;
        // the top-most parent element is contained in the computed "Root" field
        // -> so in order to retrieve all replies contained in the subtree of an annotation A,
        // we have to find all annotations having A's ID set in the "Root" field
        // we keep the sorting and ordering options

        Pageable pageable = new OffsetBasedPageRequest(0, Integer.MAX_VALUE, options.getSort()); // hand over sorting

        List<Annotation> result = null;
        try {

            User executingUser = userService.findByLogin(userlogin);

            // the users belonging to the group are required for knowing which other users' public
            // annotations are visible for the executing user
            List<Long> userIdsOfGroup = groupService.getUserIdsOfGroup(options.getGroup());

            Page<Annotation> resultPage = annotRepos.findAll(
                    new AnnotationReplySearchSpec(annotationIds, executingUser.getId(), userIdsOfGroup), pageable);
            result = resultPage.getContent();
        } catch (Exception ex) {
            LOG.error("Search for replies in annotation repository produced unexpected error!");
            throw ex;
        }
        return result;
    }

    /**
     * accept a suggestion, taking permissions into account
     * 
     * @param suggestionId
     *        the ID of the suggestion (annotation) to be accepted
     * @param userlogin
     *        the login of the user requesting to accept the suggestion
     *        
     * @return returns the found annotation object, or null
     * @throws CannotAcceptSuggestionException
     *         this exception is thrown when the referenced suggestion does not exist
     * @throws NoSuggestionException
     *         this exception is thrown when the referenced annotation is not a suggestion, but a different kind of annotation
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to accept the suggestion
     * @throws CannotDeleteAnnotationException
     *         this exception is thrown when the technical deletion encounters an unexpected error
     */
    @Override
    public void acceptSuggestionById(String suggestionId, String userlogin)
            throws CannotAcceptSuggestionException, NoSuggestionException, MissingPermissionException, CannotDeleteAnnotationException {

        if (StringUtils.isEmpty(suggestionId)) {
            throw new IllegalArgumentException("Required suggestion/annotation ID missing.");
        }

        Annotation ann = findAnnotationById(suggestionId);
        if (ann == null) {
            throw new CannotAcceptSuggestionException("Suggestion not found");
        }

        if (!isSuggestion(ann)) {
            throw new NoSuggestionException("Given ID '" + suggestionId + "' does not represent a suggestion");
        }

        if (!hasUserPermissionToAcceptSuggestion(ann, userlogin)) {
            LOG.warn("User '{}' does not have permission to accept suggestion/annotation with id '{}'.", userlogin, suggestionId);
            throw new MissingPermissionException(userlogin);
        }

        // NOTE: currently, accepting suggestions is done by deleting them; this will change in the future
        deleteAnnotationAndOrphanedDocument(ann);
    }

    /**
     * reject a suggestion, taking permissions into account
     * 
     * @param suggestionId
     *        the ID of the suggestion (annotation) to be rejected
     * @param userlogin
     *        the login of the user requesting to reject the suggestion
     *        
     * @return returns the found annotation object, or null
     * @throws CannotAcceptSuggestionException
     *         this exception is thrown when the referenced suggestion does not exist
     * @throws NoSuggestionException
     *         this exception is thrown when the referenced annotation is not a suggestion, but a different kind of annotation
     * @throws MissingPermissionException 
     *         this exception is thrown when the user does not have the permission to reject the suggestion
     * @throws CannotDeleteAnnotationException
     *         this exception is thrown when the technical deletion encounters an unexpected error
     */
    @Override
    public void rejectSuggestionById(String suggestionId, String userlogin)
            throws CannotRejectSuggestionException, NoSuggestionException, MissingPermissionException, CannotDeleteAnnotationException {

        if (StringUtils.isEmpty(suggestionId)) {
            throw new IllegalArgumentException("Required suggestion/annotation ID missing.");
        }

        Annotation ann = findAnnotationById(suggestionId);
        if (ann == null) {
            throw new CannotRejectSuggestionException("Suggestion not found");
        }

        if (!isSuggestion(ann)) {
            throw new NoSuggestionException("Given ID '" + suggestionId + "' does not represent a suggestion");
        }

        if (!hasUserPermissionToRejectSuggestion(ann, userlogin)) {
            LOG.warn("User '{}' does not have permission to reject suggestion/annotation with id '{}'.", userlogin, suggestionId);
            throw new MissingPermissionException(userlogin);
        }

        // NOTE: currently, rejecting suggestions is done by deleting them; this will change in the future
        deleteAnnotationAndOrphanedDocument(ann);
    }

    /**
     * check whether a given annotation represents a suggestion
     * 
     * @param sugg the annotation to be checked
     * 
     * @return true if the annotation was identified as a suggestion
     */
    @Override
    public boolean isSuggestion(Annotation sugg) {

        if (sugg == null) {
            throw new IllegalArgumentException("Required suggestion missing");
        }

        return tagsService.hasSuggestionTag(sugg.getTags());
    }

    /**
     * convert a given Annotation object into JsonAnnotation format
     * 
     * @param annot
     *        the Annotation object to be converted
     * @return the wrapped JsonAnnotation object
     */
    @Override
    public JsonAnnotation convertToJsonAnnotation(Annotation annot) {

        if (annot == null) {
            LOG.error("Received null for annotation to be converted to JsonAnnotation");
            return null;
        }

        URI docUri = null;
        try {
            if (annot.getDocument() != null) {
                docUri = new URI(annot.getDocument().getUri());
            }
        } catch (URISyntaxException e) {
            LOG.error("Unexpected error converting document URI '" + annot.getDocument().getUri() + "'", e);
        }

        JsonAnnotation result = new JsonAnnotation();
        result.setCreated(annot.getCreated());
        result.setId(annot.getId());
        result.setText(annot.getText());
        result.setUpdated(annot.getUpdated());
        result.setUri(docUri);

        // document info
        JsonAnnotationDocument doc = new JsonAnnotationDocument();
        doc.setTitle(annot.getDocument().getTitle());

        JsonAnnotationDocumentLink docLink = new JsonAnnotationDocumentLink();
        docLink.setHref(docUri);
        doc.setLink(Arrays.asList(docLink));

        // metadata is appended as a child of document
        if (annot.getMetadata() != null) {
            doc.setMetadata(annot.getMetadata().getKeyValuePropertyAsHashMap());
        }
        result.setDocument(doc);

        // targets
        JsonAnnotationTargets targets = new JsonAnnotationTargets(null);
        targets.setDeserializedSelectors(annot.getTargetSelectors());
        targets.setSource(docUri);
        result.setTarget(Arrays.asList(targets));

        // group info
        String groupName = "";
        if (annot.getGroup() != null) {
            groupName = annot.getGroup().getName();
            result.setGroup(groupName);
        }

        // user info
        String userAccountForHypo = "";
        if (annot.getUser() != null) {
            userAccountForHypo = userService.getHypothesisUserAccountFromUser(annot.getUser());
            result.setUser(userAccountForHypo);

            // retrieve user's display name to have "nice names" being displayed for each annotation
            String displayName = "";
            UserDetails userDetails = userService.getUserDetailsFromUserRepo(annot.getUser().getLogin());
            if (userDetails != null) {
                displayName = userDetails.getDisplayName();
            } else {
                // usually, all users should be found in the UD repo, so this case is unlikely to occur...
                // ... but the network could be down or whatever, therefore we use a fallback
                // -> why? because annotate/hypothes.is client starts acting weird if the information is missing, and
                // might even use the "display_name" provided in the current user's profile FOR ALL USERS, which is totally wrong!!!
                // ... and that simply must be avoided!
                displayName = annot.getUser().getLogin();
            }
            JsonUserInfo userInfo = new JsonUserInfo();
            userInfo.setDisplay_name(displayName);
            result.setUser_info(userInfo);
        }

        // permissions
        // currently kept simple:
        // - private annotation -> read permission for user only
        // - public annotation -> read permission for group
        // admin, delete and update always for user only
        JsonAnnotationPermissions permissions = new JsonAnnotationPermissions();
        permissions.setAdmin(Arrays.asList(userAccountForHypo));
        permissions.setDelete(Arrays.asList(userAccountForHypo));
        permissions.setUpdate(Arrays.asList(userAccountForHypo));
        permissions.setRead(Arrays.asList(annot.isShared() ? "group:" + groupName : userAccountForHypo));
        result.setPermissions(permissions);

        // tags
        if (annot.getTags() != null && annot.getTags().size() > 0) {
            // convert from List<Tag> to List<String>
            result.setTags(annot.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        }

        // references
        result.setReferences(annot.getReferencesList());

        return result;
    }

    /**
     * convert a given list of Annotation objects into JsonSearchResult format, taking search options into account
     * (e.g. whether replies should be listed separately)
     * 
     * @param annotations
     *        the list of Annotations objects to be converted
     * @param replies the replies belonging to the found annotations
     * @param options search options that might influence the result, e.g. whether replies should be listed separately
     * @return the wrapped JsonSearchResult object
     */
    @Override
    public JsonSearchResult convertToJsonSearchResult(List<Annotation> annotations, List<Annotation> replies, AnnotationSearchOptions options) {

        if (annotations == null) {
            LOG.info("There is no search result to be converted to JSON response format");
            return null;
        }

        if (options == null) {
            LOG.info("No search options given for creating search result");
            return null;
        }

        // depending on how the results were requested, we create one result type or another
        if (!options.isSeparateReplies()) {

            // merge replies into annotations, re-sort according to options
            // note: the annotations list might be implemented by an unmodifiable collection; therefore, we have to copy the items...
            List<Annotation> mergedList = new ArrayList<Annotation>();
            mergedList.addAll(annotations);
            if (replies != null) {
                mergedList.addAll(replies);
            }
            mergedList.sort(new AnnotationComparator(options.getSort()));

            List<JsonAnnotation> annotationsAsJson = mergedList.stream().map(ann -> convertToJsonAnnotation(ann))
                    .collect(Collectors.toList());
            return new JsonSearchResult(annotationsAsJson);

        } else {
            List<JsonAnnotation> annotationsAsJson = annotations.stream().map(ann -> convertToJsonAnnotation(ann))
                    .collect(Collectors.toList());
            List<JsonAnnotation> repliesAsJson = (replies == null ? new ArrayList<JsonAnnotation>()
                    : replies.stream().map(ann -> convertToJsonAnnotation(ann)).collect(Collectors.toList()));
            return new JsonSearchResultWithSeparateReplies(annotationsAsJson, repliesAsJson);
        }
    }

    /**
     * technical deletion of an annotation; also takes care to remove a document entry if it becomes orphaned due to the deletion
     *  
     * @param annot the annotation to be deleted
     * @throws CannotDeleteAnnotationException
     *         the exception is thrown when the annotation cannot be deleted, e.g. when it is not existing or due to unexpected database error)
     */
    private void deleteAnnotationAndOrphanedDocument(Annotation annot) throws CannotDeleteAnnotationException {

        // we remove orphaned documents - therefore we now save the document information before removing the annotation
        // notes:
        // - we do not delete the user, the group, or the user's group membership
        // - we do not have to delete tags, as they are deleted by their foreign key constraint to the annotation ID
        Document doc = annot.getDocument();

        // note: due to the database schema construction, there is a foreign key constraint on the 'root' column
        // this makes all replies be deleted in case the thread's root annotation is deleted
        try {
            annotRepos.delete(annot);
        } catch (Exception e) {
            LOG.error("Error deleting annotation with ID '" + annot.getId() + "'", e);
            throw new CannotDeleteAnnotationException(e);
        }

        // check if document is orphaned (i.e. no more annotations are stored for the document) -> delete
        if (annotRepos.existsByDocumentId(doc.getId())) {
            LOG.debug("There are still annotations assigned to document '{}'. We do not delete the document.", doc.getId());
        } else {
            LOG.info("Document '" + doc.getId() + "' is no longer referenced by any annotation. We will remove the document now.");
            try {
                documentService.deleteDocument(doc);
            } catch (CannotDeleteDocumentException cdde) {
                LOG.error("Document '" + doc.getId() + "' could not be deleted, remains in database without being related to any annotation", cdde);
            } catch (Exception e) {
                LOG.error("Unexpected error upon deleting document '{}'; remains in database without being related to any annotation", doc.getId());
            }
        }
    }

    /**
     * checks if an annotation is visible for "only me" this is done based on the read permissions set: 
     *  if it is readable for the user only, it is private
     * 
     * @param annotation
     *        JsonAnnotation object to be checked
     * @return flag indicating whether annotation is meant to be visible for "only me"
     */
    private boolean isPrivateAnnotation(JsonAnnotation annotation) {

        if (annotation == null) {
            LOG.error("Annotation to be checked for privacy is invalid!");
            return false;
        }

        JsonAnnotationPermissions perms = annotation.getPermissions();
        if (perms == null) {
            LOG.error("Annotation to be checked for privacy does not contain any permission information!");
            return false;
        }

        List<String> readPerms = perms.getRead();
        if (readPerms == null || readPerms.size() == 0) {
            LOG.error("Annotation to be checked for privacy does not contain any read permissions!");
            return false;
        }

        // the annotation is private if and only if the user itself has read permissions
        return readPerms.size() == 1 && readPerms.get(0).equals(annotation.getUser());
    }

    /**
     * check if user may see the annotation based on
     * - his user name (if the annotation is private and he is the user that created the annotation)
     * - his group membership (if the annotation is public and he is member of the group the annotation is published in)
     * 
     * @param annot
     *        the annotation to be retrieved
     * @param user
     *        the user requesting to view the annotation
     *        
     * @return true if user may see the annotation
     */
    private boolean hasUserPermissionToSeeAnnotation(Annotation annot, User user) {

        if (user == null || annot == null) {
            LOG.warn("Checking if user has permission to view annotation fails: annot available: {}, user available: {}. Deny permission.",
                    annot != null, user != null);
            return false;
        }

        // if the requesting user created the annotation, then he may see it
        if (annot.getUser().getId().equals(user.getId())) {
            return true;
        }

        // otherwise, if annotation must be
        // a) shared
        if (!annot.isShared()) {
            return false;
        }

        // b) user must be member of the group in which the annotation was published
        if (!groupService.isUserMemberOfGroup(user, annot.getGroup())) {
            return false;
        }

        return true;
    }

    /**
     * check if user may update the annotation based on
     * - his user name (if he is the user that created the annotation)
     * - his group membership (if the annotation is public and he is member of the group the annotation is published in)
     * 
     * @param annot
     *        the annotation to be updated
     * @param userlogin
     *        login of the user requesting to update the annotation
     *        
     * @return true if user may update the annotation
     */
    private boolean hasUserPermissionToUpdateAnnotation(Annotation annot, String userlogin) {

        return isAnnotationOfUser(annot, userlogin);
    }

    /**
     * check if user may accept a suggestion based on his user name (needs to be member of the group to which the annotation belongs)
     * 
     * @param sugg
     *        the suggestion to be accepted
     * @param userlogin
     *        login of the user requesting to accept the suggestion
     *        
     * @return true if user may accept the suggestion
     */
    private boolean hasUserPermissionToAcceptSuggestion(Annotation sugg, String userlogin) {

        if (StringUtils.isEmpty(userlogin)) {
            LOG.warn("No user name given for checking permission to accept suggestion");
            return false;
        }

        // note: currently, any user of the group may accept a suggestion
        User user = null;

        if ((user = userService.findByLogin(userlogin)) == null) {
            LOG.debug("No user registered yet with given user name: {}, so there cannot be any matches", userlogin);
            return false;
        }
        return groupService.isUserMemberOfGroup(user, sugg.getGroup());
    }

    /**
     * check if user may reject a suggestion based on his user name (needs to be member of the group to which the annotation belongs)
     * 
     * @param sugg
     *        the suggestion to be rejected
     * @param userlogin
     *        login of the user requesting to accept the suggestion
     *        
     * @return true if user may reject the suggestion
     */
    private boolean hasUserPermissionToRejectSuggestion(Annotation sugg, String userlogin) {

        if (StringUtils.isEmpty(userlogin)) {
            LOG.warn("No user name given for checking permission to reject suggestion");
            return false;
        }

        // note: currently, any user of the group may reject a suggestion
        User user = null;

        if ((user = userService.findByLogin(userlogin)) == null) {
            LOG.debug("No user registered yet with given user name: {}, so there cannot be any matches", userlogin);
            return false;
        }
        return groupService.isUserMemberOfGroup(user, sugg.getGroup());
    }

    /**
     * check if a given annotation belongs to a user
     * 
     * @param annot
     *        the annotation to be checked
     * @param userlogin
     *        login of the user to be checked
     *        
     * @return true if user created the annotation
     */
    private boolean isAnnotationOfUser(Annotation annot, String userlogin) {

        User user = userService.findByLogin(userlogin);

        // verify that the user associated to the annotation is the given user
        try {
            return (annot.getUser().getId().equals(user.getId()));
        } catch (Exception e) {
            LOG.error("Error checking if annotation belongs to user", e);
        }

        return false;
    }

}