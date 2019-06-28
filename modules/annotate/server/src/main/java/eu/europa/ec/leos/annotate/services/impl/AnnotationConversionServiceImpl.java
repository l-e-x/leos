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

import eu.europa.ec.leos.annotate.model.AnnotationComparator;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import eu.europa.ec.leos.annotate.model.search.*;
import eu.europa.ec.leos.annotate.model.web.annotation.*;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserInfo;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * class containing functionality for converting annotations into their JSON representation  
 */
@Service
public class AnnotationConversionServiceImpl implements AnnotationConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationConversionService.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // note: should not be declared final, as otherwise mocking it will give problems
    @Autowired
    private UserService userService;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public AnnotationConversionServiceImpl(final UserService userServ) {
        this.userService = userServ;
    }

    // -------------------------------------
    // Service functionality
    // -------------------------------------

    /**
    * convert a given {@link Annotation} object into {@link JsonAnnotation} format
    * 
    * @param annot
    *        the Annotation object to be converted
    * @param userInfo
    *        user information about the user requesting the action
    * @return the wrapped JsonAnnotation object
    */
    @Override
    public JsonAnnotation convertToJsonAnnotation(final Annotation annot, final UserInformation userInfo) {

        return convertToJsonAnnotation(annot, null, userInfo);
    }

    /**
     * convert a given Annotation object into JsonAnnotation format, taking rules specific to some search model into account
     * 
     * @param annot
     *        the Annotation object to be converted
     * @param searchModel
     *        the search model used - might e.g. influence anonymisation of user-related data (creator, etc.)
     * @param userInfo
     *        user information about the user requesting the action
     * @return the wrapped JsonAnnotation object
     */
    @Override
    public JsonAnnotation convertToJsonAnnotation(final Annotation annot, final SearchModel searchModel, final UserInformation userInfo) {

        if (annot == null) {
            LOG.error("Received null for annotation to be converted to JsonAnnotation");
            return null;
        }

        URI docUri = null;
        try {
            docUri = new URI(annot.getDocument().getUri()); // document is mandatory property of annotation (thus not null)
        } catch (URISyntaxException e) {
            LOG.error("Unexpected error converting document URI '" + annot.getDocument().getUri() + "'", e);
        }

        final JsonAnnotation result = new JsonAnnotation();
        result.setId(annot.getId());
        result.setText(annot.getText());
        result.setUpdated(annot.getUpdated());
        result.setUri(docUri);

        // document info
        final JsonAnnotationDocument doc = new JsonAnnotationDocument();
        doc.setTitle(annot.getDocument().getTitle());

        final JsonAnnotationDocumentLink docLink = new JsonAnnotationDocumentLink();
        docLink.setHref(docUri);
        doc.setLink(Arrays.asList(docLink));

        // metadata is appended as a child of document
        final String annotAuthority = annot.getMetadata().getSystemId(); // metadata is mandatory property of annotation (thus not null)
        doc.setMetadata(annot.getMetadata().getAllMetadataAsSimpleMetadata());
        result.setDocument(doc);

        // check if we are to anonymise the creator of the annotation - this is when the annotation is SENT
        final boolean isResponseStatusSent = annot.isResponseStatusSent();
        final boolean anonymizeUser = isResponseStatusSent;

        // targets
        final JsonAnnotationTargets targets = new JsonAnnotationTargets(null);
        targets.setDeserializedSelectors(annot.getTargetSelectors());
        targets.setSource(docUri);
        result.setTarget(Arrays.asList(targets));

        // group info
        final String groupName = annot.getGroup().getName(); // group is mandatory property of annotation (thus not null) - accessed via metadata
        result.setGroup(groupName);

        // user info
        final String userAccountForHypo = userService.getHypothesisUserAccountFromUser(annot.getUser(), annotAuthority);
        if (anonymizeUser) {
            String entityName = "";
            final UserDetails userDetails = userService.getUserDetailsFromUserRepo(annot.getUser().getLogin());
            if (userDetails == null) {
                entityName = "unknown";
            } else {
                entityName = userDetails.getEntity();
            }

            String iscReference = annot.getMetadata().getIscReference(); // metadata is mandatory property of annotation (thus not null)
            if (StringUtils.isEmpty(iscReference)) {
                iscReference = "unknown ISC reference";
            }

            // we use the entity for the user as well as for the display name of the user
            // note: setting it only in the "entity name" instead of display name makes the appearance
            // become distorted
            result.setUser(entityName);

            final JsonUserInfo jsUserInfo = new JsonUserInfo();
            jsUserInfo.setDisplay_name(iscReference);
            jsUserInfo.setEntity_name(entityName);
            result.setUser_info(jsUserInfo);
            
            // finally, we overwrite the "created date" with the date at which the response status was updated
            result.setCreated(annot.getMetadata().getResponseStatusUpdated());
        } else {
            // show "real" user information, if available
            result.setUser(userAccountForHypo);

            // retrieve user's display name to have "nice names" being displayed for each annotation
            String displayName = "";
            String entityName = "";
            final UserDetails userDetails = userService.getUserDetailsFromUserRepo(annot.getUser().getLogin());
            if (userDetails == null) {
                // usually, all users should be found in the UD repo, so this case is unlikely to occur...
                // ... but the network could be down or whatever, therefore we use a fallback
                // -> why? because annotate/hypothes.is client starts acting weird if the information is missing, and
                // might even use the "display_name" provided in the current user's profile FOR ALL USERS, which is totally wrong!!!
                // ... and that simply must be avoided!
                displayName = annot.getUser().getLogin();
                // skip setting entity name as we don't have any reasonable fallback for this

            } else {
                displayName = userDetails.getDisplayName();
                entityName = userDetails.getEntity();
            }
            final JsonUserInfo jsUserInfo = new JsonUserInfo();
            jsUserInfo.setDisplay_name(displayName);
            jsUserInfo.setEntity_name(entityName);
            result.setUser_info(jsUserInfo);

            // show real date of creating the annotation
            result.setCreated(annot.getCreated());
        }

        result.setPermissions(setJsonAnnotationPermissions(annot, groupName, isResponseStatusSent, userAccountForHypo));

        // tags
        if (annot.getTags() != null && !annot.getTags().isEmpty()) {
            // convert from List<Tag> to List<String>
            result.setTags(annot.getTags().stream().map(Tag::getName).collect(Collectors.toList()));
        }

        // references
        result.setReferences(annot.getReferencesList());

        // annotation status
        result.setStatus(getJsonAnnotationStatus(annot, annotAuthority));

        return result;
    }

    /**
     * convert a given list of Annotation objects into JsonSearchResult format, taking search options into account
     * (e.g. whether replies should be listed separately)
     * 
     * @param annotationResult
     *        the wrapper object containing the list of Annotations objects to be converted
     * @param replies the replies belonging to the found annotations
     * @param options search options that might influence the result, e.g. whether replies should be listed separately
     * @return the wrapped JsonSearchResult object
     */
    @Override
    public JsonSearchResult convertToJsonSearchResult(final AnnotationSearchResult annotationResult, final List<Annotation> replies,
            final AnnotationSearchOptions options, final UserInformation userInfo) {

        if (annotationResult == null) {
            LOG.info("There is no search result to be converted to JSON response format");
            return null;
        }

        if (options == null) {
            LOG.info("No search options given for creating search result");
            return null;
        }

        final SearchModel searchModel = annotationResult.getSearchModelUsed();
        // depending on how the results were requested, we create one result type or another
        if (options.isSeparateReplies()) {

            final List<JsonAnnotation> annotationsAsJson = annotationResult.getItems().stream()
                    .map(ann -> convertToJsonAnnotation(ann, searchModel, userInfo))
                    .collect(Collectors.toList());
            final List<JsonAnnotation> repliesAsJson = (replies == null ? new ArrayList<JsonAnnotation>()
                    : replies.stream().map(ann -> convertToJsonAnnotation(ann, searchModel, userInfo)).collect(Collectors.toList()));
            return new JsonSearchResultWithSeparateReplies(annotationsAsJson, repliesAsJson, annotationResult.getTotalItems());

        } else {
            // merge replies into annotations, re-sort according to options
            // note: the annotations list might be implemented by an unmodifiable collection; therefore, we have to copy the items...
            final List<Annotation> mergedList = new ArrayList<Annotation>();
            mergedList.addAll(annotationResult.getItems());
            if (replies != null) {
                mergedList.addAll(replies);
            }
            mergedList.sort(new AnnotationComparator(options.getSort()));

            final List<JsonAnnotation> annotationsAsJson = mergedList.stream().map(ann -> convertToJsonAnnotation(ann, searchModel, userInfo))
                    .collect(Collectors.toList());
            return new JsonSearchResult(annotationsAsJson, annotationResult.getTotalItems());
        }
    }

    /**
     * set the permissions
     * currently kept simple:
     * - private annotation -> read permission for user only
     * - public annotation -> read permission for group
     * admin, delete and update always for user only - unless the annotation has responseStatus=SENT
     * -> in that case, we don't provide any permissions there, i.e. the annotation cannot be modified
     * 
     * @param annot annotation for which permissions are to be computed
     * @param groupName associate group
     * @param isResponseStatusSent flag indicating whether the annotation's response status is set to "SENT"
     * @param userAccountForHypo hypothes.is account name of the annotation's author
     * @return assembled {@link JsonAnnotationPermissions}
     */
    private JsonAnnotationPermissions setJsonAnnotationPermissions(final Annotation annot, final String groupName, final boolean isResponseStatusSent,
            final String userAccountForHypo) {

        final JsonAnnotationPermissions permissions = new JsonAnnotationPermissions();
        final String permisAdmDelUpd = isResponseStatusSent ? "" : userAccountForHypo;
        permissions.setAdmin(Arrays.asList(permisAdmDelUpd));
        permissions.setDelete(Arrays.asList(permisAdmDelUpd));
        permissions.setUpdate(Arrays.asList(permisAdmDelUpd));
        permissions.setRead(Arrays.asList(annot.isShared() ? "group:" + groupName : userAccountForHypo));

        return permissions;
    }

    /**
     * assemble the status information of an annotation in JSON format
     * 
     * @param annot Annotation for which to assemble the information
     * @return filled-in {@link JsonAnnotationStatus} object
     */
    public JsonAnnotationStatus getJsonAnnotationStatus(final Annotation annot) {
        return getJsonAnnotationStatus(annot, "");
    }

    /**
     * assemble the status information of an annotation in JSON format
     * 
     * @param annot Annotation for which to assemble the information
     * @param authority the user's authority; if empty, the authority will be read from metadata
     * @return filled-in {@link JsonAnnotationStatus} object
     */
    private JsonAnnotationStatus getJsonAnnotationStatus(final Annotation annot, final String authority) {

        final JsonAnnotationStatus status = new JsonAnnotationStatus();
        status.setStatus(annot.getStatus());
        status.setUpdated(annot.getStatusUpdated());

        String annotAuthority = authority;
        if (StringUtils.isEmpty(annotAuthority)) {
            annotAuthority = annot.getMetadata().getSystemId();
        }
        status.setUpdated_by(
                annot.getStatusUpdatedBy() == null ? null : userService.getHypothesisUserAccountFromUserId(annot.getStatusUpdatedBy(), annotAuthority));
        return status;
    }

}
