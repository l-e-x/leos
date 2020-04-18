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
import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationPermissions;
import eu.europa.ec.leos.annotate.services.AnnotationPermissionServiceWithTestFunctions;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@Service
public class AnnotationPermissionServiceImpl implements AnnotationPermissionServiceWithTestFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPermissionServiceImpl.class);

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;

    // default group name, injected from properties
    @Value("${defaultgroup.name}")
    private String defaultGroupName;

    // -------------------------------------
    // Methods from test interface part
    // -------------------------------------

    @Override
    public void setDefaultGroupName(final String groupName) {

        this.defaultGroupName = groupName;
    }

    /* (non-Javadoc)
     * @see eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionService#hasUserPermissionToSeeAnnotation(eu.europa.ec.leos.annotate.model.entity.Annotation, eu.europa.ec.leos.annotate.model.entity.User)
     */
    @Override
    public boolean hasUserPermissionToSeeAnnotation(final Annotation annot, final User user) {

        Assert.notNull(annot, "Checking if user has permission to view annotation fails since annotation unavailable.");
        Assert.notNull(user, "Checking if user has permission to view annotation fails since user unavailable.");

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
        return groupService.isUserMemberOfGroup(user, annot.getGroup());
    }

    /* (non-Javadoc)
     * @see eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionService#hasUserPermissionToUpdateAnnotation(eu.europa.ec.leos.annotate.model.entity.Annotation, eu.europa.ec.leos.annotate.model.UserInformation)
     */
    @Override
    public boolean hasUserPermissionToUpdateAnnotation(final Annotation annot, final UserInformation userinfo) {

        Assert.notNull(annot, "Annotation to be checked for being updateable is invalid!");
        Assert.notNull(userinfo, "User to be checked for being able to update annotation is invalid!");

        final boolean isSent = annot.isResponseStatusSent();

        if (Authorities.isLeos(userinfo.getAuthority())) {

            // EdiT users may not update SENT annotations, and otherwise only update their own annotations
            return !isSent && isAnnotationOfUser(annot, userinfo.getLogin());

        } else {

            // when in ISC, users of same entity (group) may update SENT annotations...
            if (isSent) {
                return groupService.isUserMemberOfGroup(userinfo.getUser(), annot.getGroup());
            } else {
                // but other annotations may be updated if they are of the user
                return isAnnotationOfUser(annot, userinfo.getLogin());
            }
        }
    }

    /* (non-Javadoc)
     * @see eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionService#hasUserPermissionToAcceptSuggestion(eu.europa.ec.leos.annotate.model.entity.Annotation, eu.europa.ec.leos.annotate.model.entity.User)
     */
    @Override
    public boolean hasUserPermissionToAcceptSuggestion(final Annotation sugg, final User user) {

        // note: currently, any user of the group may accept a suggestion
        Assert.notNull(user, "No user given for checking permission to accept suggestion");

        return groupService.isUserMemberOfGroup(user, sugg.getGroup());
    }

    /* (non-Javadoc)
     * @see eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionService#hasUserPermissionToRejectSuggestion(eu.europa.ec.leos.annotate.model.entity.Annotation, eu.europa.ec.leos.annotate.model.entity.User)
     */
    @Override
    public boolean hasUserPermissionToRejectSuggestion(final Annotation sugg, final User user) {

        // note: currently, any user of the group may reject a suggestion
        Assert.notNull(user, "No user given for checking permission to reject suggestion");

        return groupService.isUserMemberOfGroup(user, sugg.getGroup());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean userMayPublishContributions(final UserInformation userInfo) {

        Assert.notNull(userInfo, "No valid user data given for checking publishing permissions");

        return Authorities.isIsc(userInfo.getAuthority());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonAnnotationPermissions getJsonAnnotationPermissions(final Annotation annot, final String groupName,
            final String userAccountForHypo, final UserInformation userInfo) {

        Assert.notNull(userInfo, "User information not available");

        final PossiblePermissions possiblePerms = new PossiblePermissions(userAccountForHypo, "group:" + groupName, defaultGroupName);

        final JsonAnnotationPermissions permissions = new JsonAnnotationPermissions();

        setAdminPermissions(annot, possiblePerms, userInfo, permissions);
        setDeletePermissions(annot, possiblePerms, userInfo, permissions);
        setEditPermissions(annot, possiblePerms, userInfo, permissions);
        setReadPermissions(annot, possiblePerms, permissions);

        return permissions;
    }

    // administrate (whatever that means in the client...)
    @SuppressWarnings("PMD.TooFewBranchesForASwitchStatement")
    private void setAdminPermissions(final Annotation annot, final PossiblePermissions possiblePerms, final UserInformation userInfo,
            final JsonAnnotationPermissions permissions) {

        switch (userInfo.getSearchUser()) {

            case Contributor:

                final boolean isContributorAnnotation = !annot.isShared() && annot.getUserId() == userInfo.getUser().getId();

                if (isContributorAnnotation) {
                    permissions.setAdmin(possiblePerms.getUserPermission());
                } else {
                    permissions.setAdmin(possiblePerms.getNoPermission());
                }
                break;

            default:
                // standard EdiT or ISC user

                if (annot.isResponseStatusSent()) {
                    permissions.setAdmin(possiblePerms.getNoPermission());
                } else {
                    permissions.setAdmin(possiblePerms.getUserPermission());
                }
                break;
        }
    }

    // deleting the annotation
    private void setDeletePermissions(final Annotation annot, final PossiblePermissions possiblePerms, final UserInformation userInfo,
            final JsonAnnotationPermissions permissions) {

        switch (userInfo.getSearchUser()) {

            case Contributor:
                permissions.setDelete(getContributorDeletePermissions(annot, possiblePerms, userInfo));
                break;

            case EdiT:
                permissions.setDelete(getLeosDeletePermissions(annot, possiblePerms));
                break;

            case ISC:
                permissions.setDelete(getIscDeletePermissions(annot, possiblePerms, userInfo));
                break;

            default:
                // this case should not occur
                LOG.error("Asked for delete permissions for unknown user type.");
                permissions.setDelete(null);
                break;
        }
    }

    private List<String> getIscDeletePermissions(final Annotation annot, final PossiblePermissions possiblePerms,
            final UserInformation userInfo) {

        if (annot.isResponseStatusSent()) {

            final String responseId = annot.getMetadata().getResponseId(); // note: Metadata cannot be null
            final boolean userIsFromSameEntity = isResponseFromUsersEntity(userInfo, responseId);

            // ISC: if the user is from the same group as the annotation, he may delete it; unless it was already deleted
            if (annot.getStatus() == AnnotationStatus.DELETED) {
                return possiblePerms.getNoPermission();
            } else if (userIsFromSameEntity) {
                return possiblePerms.getUserPermission();
            } else {
                // otherwise, it is read-only
                return possiblePerms.getNoPermission();
            }
        } else {
            return possiblePerms.getUserPermission();
        }
    }

    private List<String> getLeosDeletePermissions(final Annotation annot, final PossiblePermissions possiblePerms) {

        if (annot.isResponseStatusSent()) {
            // all users may delete: in order to simulate "everybody", we assign the default group
            // in which all users are members
            return possiblePerms.getEverybodyPermission();
        } else {
            return possiblePerms.getUserPermission();
        }
    }

    private List<String> getContributorDeletePermissions(final Annotation annot, final PossiblePermissions possiblePerms,
            final UserInformation userInfo) {

        final boolean isContributorAnnotation = !annot.isShared() && annot.getUserId() == userInfo.getUser().getId();

        if (annot.getStatus() == AnnotationStatus.DELETED) {
            return possiblePerms.getNoPermission();
        } else if (isContributorAnnotation) {
            // contributor may delete his own annotation
            return possiblePerms.getUserPermission();
        } else {
            // contributor may not delete other people's annotation
            return possiblePerms.getNoPermission();
        }
    }

    // editing
    private void setEditPermissions(final Annotation annot, final PossiblePermissions possiblePerms, final UserInformation userInfo,
            final JsonAnnotationPermissions permissions) {

        switch (userInfo.getSearchUser()) {

            case Contributor:
                permissions.setUpdate(getContributorEditPermissions(annot, possiblePerms, userInfo));
                break;

            case EdiT:
                permissions.setUpdate(getLeosEditPermissions(annot, possiblePerms));
                break;

            case ISC:
                permissions.setUpdate(getIscEditPermissions(annot, possiblePerms, userInfo));
                break;

            default:
                // this case should not occur
                LOG.error("Asked for edit permissions for unknown user type.");
                permissions.setUpdate(null);
                break;
        }
    }

    private List<String> getIscEditPermissions(final Annotation annot, final PossiblePermissions possiblePerms,
            final UserInformation userInfo) {

        if (annot.isResponseStatusSent()) {

            final String responseId = annot.getMetadata().getResponseId(); // note: Metadata cannot be null
            final boolean userIsFromSameEntity = isResponseFromUsersEntity(userInfo, responseId);

            // while in ISC, SENT annotations may be edited by group members; unless it was already deleted
            if (annot.getStatus() == AnnotationStatus.DELETED) {
                return possiblePerms.getNoPermission();
            } else if (userIsFromSameEntity) {
                return possiblePerms.getUserPermission();
            } else {
                // otherwise, it is read-only
                return possiblePerms.getNoPermission();
            }
        } else {
            return possiblePerms.getUserPermission();
        }
    }

    private List<String> getLeosEditPermissions(final Annotation annot, final PossiblePermissions possiblePerms) {

        if (annot.isResponseStatusSent()) {

            // in EdiT, SENT annotations may not be edited
            return possiblePerms.getNoPermission();
        } else {
            return possiblePerms.getUserPermission();
        }
    }

    private List<String> getContributorEditPermissions(final Annotation annot, final PossiblePermissions possiblePerms,
            final UserInformation userInfo) {

        final boolean isContributorAnnotation = !annot.isShared() && annot.getUserId() == userInfo.getUser().getId();

        if (annot.getStatus() == AnnotationStatus.DELETED) {
            return possiblePerms.getNoPermission();
        } else if (isContributorAnnotation) {
            // contributor may update his own annotation
            return possiblePerms.getUserPermission();
        } else {
            // contributor may not update other people's annotation
            return possiblePerms.getNoPermission();
        }
    }

    // reading
    private void setReadPermissions(final Annotation annot, final PossiblePermissions possiblePerms,
            final JsonAnnotationPermissions permissions) {

        permissions.setRead(annot.isShared() ? possiblePerms.getGroupPermission() : possiblePerms.getUserPermission());
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
    private boolean isAnnotationOfUser(final Annotation annot, final String userlogin) {

        final User user = userService.findByLogin(userlogin);

        // verify that the user associated to the annotation is the given user
        try {
            return annot.getUser().getId().equals(user.getId());
        } catch (Exception e) {
            LOG.error("Error checking if annotation belongs to user", e);
        }

        return false;
    }

    /**
     * check if the annotation belongs to the user's entity (i.e. if the user's connectedEntity equals the responseId of ISC)
     * 
     * @param userInfo
     *        given {@link UserInformation} with user details, e.g. the connectedEntity
     * @param responseId
     *        response Id of an ISC response (usually DG name)
     * @return flag indicating if both are filled and coincide
     */
    private boolean isResponseFromUsersEntity(final UserInformation userInfo, final String responseId) {

        if (StringUtils.isEmpty(responseId)) {
            return false;
        }

        if (userInfo == null || StringUtils.isEmpty(userInfo.getConnectedEntity())) {
            return false;
        }

        return responseId.equals(userInfo.getConnectedEntity());
    }

    /**
     * local helper class administrating the individual permissions that might be assigned
     */
    private final static class PossiblePermissions {

        private final String defaultGroup;
        private static final String NO_PERMISSION = "";
        private final String USER_PERMISSION;
        private final String ANNOT_GROUP_PERMISSION;
        private final String EVERYBODY_PERMISSION;

        public PossiblePermissions(final String userOnly, final String group, final String defaultGroupName) {

            this.defaultGroup = defaultGroupName;

            USER_PERMISSION = userOnly;
            ANNOT_GROUP_PERMISSION = group;
            EVERYBODY_PERMISSION = "group:" + this.defaultGroup;
        }

        @Generated
        public List<String> getNoPermission() {
            return Arrays.asList(NO_PERMISSION);
        }

        @Generated
        public List<String> getUserPermission() {
            return Arrays.asList(USER_PERMISSION);
        }

        @Generated
        public List<String> getGroupPermission() {
            return Arrays.asList(ANNOT_GROUP_PERMISSION);
        }

        @Generated
        public List<String> getEverybodyPermission() {
            return Arrays.asList(EVERYBODY_PERMISSION);
        }
    }
}
