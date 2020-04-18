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
import eu.europa.ec.leos.annotate.model.AnnotationComparator;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.search.Consts;
import eu.europa.ec.leos.annotate.model.web.annotation.*;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserInfo;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationPermissionService;
import eu.europa.ec.leos.annotate.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * class containing functionality for converting annotations into their JSON representation  
 */
@Service
public class AnnotationConversionServiceImpl implements AnnotationConversionService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationConversionService.class);

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationPermissionService annotPermService;

    // note: should not be declared final, as otherwise mocking it will give problems
    @SuppressWarnings("PMD.ImmutableField")
    @Autowired
    private UserService userService;

    // -------------------------------------
    // Constructors
    // -------------------------------------

    public AnnotationConversionServiceImpl() {
        // required default constructor for autowired instantiation
    }
    
    public AnnotationConversionServiceImpl(final UserService userServ) {
        this.userService = userServ;
    }

    public AnnotationConversionServiceImpl(final UserService userServ, final AnnotationPermissionService permServ) {
        this.userService = userServ;
        this.annotPermService = permServ;
    }
    
    // required for mocking
    public void setPermissionService(final AnnotationPermissionService permServ) {
        this.annotPermService = permServ;
    }

    // -------------------------------------
    // Service functionality
    // -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonAnnotation convertToJsonAnnotation(final Annotation annot, final UserInformation userInfo) {

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
        result.setLinkedAnnotationId(annot.getLinkedAnnotationId());
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
        final boolean anonymizeUser = annot.isResponseStatusSent();

        // targets
        final JsonAnnotationTargets targets = new JsonAnnotationTargets(null);
        targets.setDeserializedSelectors(annot.getTargetSelectors());
        targets.setSource(docUri);
        result.setTarget(Arrays.asList(targets));

        // group info
        final String groupName = annot.getGroup().getName(); // group is mandatory property of annotation (thus not null) - accessed via metadata
        result.setGroup(groupName);

        final String userAccountForHypo = userService.getHypothesisUserAccountFromUser(annot.getUser(), annotAuthority);
        setUserInfo(result, annot, anonymizeUser, userAccountForHypo);

        result.setPermissions(annotPermService.getJsonAnnotationPermissions(annot, groupName, userAccountForHypo, userInfo));

        // tags
        if (!CollectionUtils.isEmpty(annot.getTags())) {
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
     * setting the user information for the converted {@link JsonAnnotation}
     * 
     * @param result
     *        the converted {@link JsonAnnotation}
     * @param annot
     *        the {@link Annotation} to be converted
     * @param anonymizeUser
     *        flag indicating whether user name etc. should be anonymised
     * @param userAccountForHypo
     *        the user account (used when user is not to be anonymised)
     */
    private void setUserInfo(final JsonAnnotation result, final Annotation annot, final boolean anonymizeUser, final String userAccountForHypo) {

        // user info
        if (anonymizeUser) {
            String entityName = "";
            final UserDetails userDetails = userService.getUserDetailsFromUserRepo(annot.getUser().getLogin());
            if (userDetails == null) {
                entityName = "unknown";
            } else {
                entityName = userDetails.getEntities().get(0).getName();
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
                entityName = userDetails.getEntities().get(0).getName();
            }
            final JsonUserInfo jsUserInfo = new JsonUserInfo();
            jsUserInfo.setDisplay_name(displayName);
            jsUserInfo.setEntity_name(entityName);
            result.setUser_info(jsUserInfo);

            // show real date of creating the annotation
            result.setCreated(annot.getCreated());
        }
    }

    /**
     * {@inheritDoc}
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

        // hand over the information which type of user is requesting the search
        Assert.notNull(userInfo, "Required user information missing");
        if (options.getSearchUser() == Consts.SearchUserType.Contributor) {
            userInfo.setSearchUser(Consts.SearchUserType.Contributor);
        } else if (Authorities.isLeos(userInfo.getAuthority())) {
            userInfo.setSearchUser(Consts.SearchUserType.EdiT);
        } else if (Authorities.isIsc(userInfo.getAuthority())) {
            userInfo.setSearchUser(Consts.SearchUserType.ISC);
        } else {
            LOG.error("Unknown user type found; cannot assign permissions!");
            return null;
        }

        // depending on how the results were requested, we create one result type or another
        if (options.isSeparateReplies()) {

            final List<JsonAnnotation> annotationsAsJson = annotationResult.getItems().stream()
                    .map(ann -> convertToJsonAnnotation(ann, userInfo))
                    .collect(Collectors.toList());
            final List<JsonAnnotation> repliesAsJson = (replies == null ? new ArrayList<JsonAnnotation>()
                    : replies.stream().map(ann -> convertToJsonAnnotation(ann, userInfo)).collect(Collectors.toList()));
            return new JsonSearchResultWithSeparateReplies(annotationsAsJson, repliesAsJson, annotationResult.getTotalItems());

        } else {
            // merge replies into annotations, re-sort according to options
            // note: the annotations list might be implemented by an unmodifiable collection; therefore, we have to copy the items...
            final List<Annotation> mergedList = Stream.of(annotationResult.getItems(), replies).flatMap(item -> item == null ? null : item.stream())
                    .collect(Collectors.toList());
            mergedList.sort(new AnnotationComparator(options.getSort()));

            final List<JsonAnnotation> annotationsAsJson = mergedList.stream().map(ann -> convertToJsonAnnotation(ann, userInfo))
                    .collect(Collectors.toList());
            return new JsonSearchResult(annotationsAsJson, annotationResult.getTotalItems());
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonAnnotationStatus getJsonAnnotationStatus(final Annotation annot) {
        return getJsonAnnotationStatus(annot, "");
    }

    /**
     * assemble the status information of an annotation in JSON format
     * 
     * @param annot 
     *        {@link Annotation} for which to assemble the information
     * @param authority 
     *        the user's authority; if empty, the authority will be read from metadata
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

        if (annot.getStatusUpdatedBy() == null) {
            status.setUser_info(null);
            status.setUpdated_by(null);
        } else {
            final User modifyingUser = userService.getUserById(annot.getStatusUpdatedBy());
            if (modifyingUser == null) {
                LOG.warn("Could not resolved user by his ID ({})", annot.getStatusUpdatedBy());
            } else {
                status.setUser_info(userService.getHypothesisUserAccountFromUser(modifyingUser, annotAuthority));

                try {
                    final UserDetails userDetails = userService.getUserDetailsFromUserRepo(modifyingUser.getLogin());
                    if (userDetails != null) {
                        // use main entity of user
                        status.setUpdated_by(userDetails.getEntities().get(0).getName());
                    }
                } catch (Exception ex) {
                    LOG.error("Error getting user Details from UD repo (for retrieving user's main entity)", ex);
                }
            }
        }

        status.setSentDeleted(annot.isSentDeleted());

        return status;
    }

}
