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

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnnotationPermissionService {

    private static final Logger LOG = LoggerFactory.getLogger(AnnotationPermissionService.class);

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserService userService;


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
    public boolean hasUserPermissionToSeeAnnotation(final Annotation annot, final User user) {

        if (user == null || annot == null) {
            LOG.warn("Checking if user has permission to view annotation fails since annot or user unavailable. Deny permission.");
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
        return groupService.isUserMemberOfGroup(user, annot.getGroup());
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
    public boolean hasUserPermissionToUpdateAnnotation(final Annotation annot, final String userlogin) {

        return isAnnotationOfUser(annot, userlogin);
    }

    /**
     * check if user may accept a suggestion based on his user name (needs to be member of the group to which the annotation belongs)
     * 
     * @param sugg
     *        the suggestion to be accepted
     * @param user
     *        user requesting to accept the suggestion
     *        
     * @return true if user may accept the suggestion
     */
    public boolean hasUserPermissionToAcceptSuggestion(final Annotation sugg, final User user) {

        // note: currently, any user of the group may accept a suggestion
        if (user == null) {
            LOG.debug("No user given for checking permission to accept suggestion");
            return false;
        }
        return groupService.isUserMemberOfGroup(user, sugg.getGroup());
    }

    /**
     * check if user may reject a suggestion based on his user name (needs to be member of the group to which the annotation belongs)
     * 
     * @param sugg
     *        the suggestion to be rejected
     * @param user
     *        the user requesting to reject the suggestion
     *        
     * @return true if user may reject the suggestion
     */
    public boolean hasUserPermissionToRejectSuggestion(final Annotation sugg, final User user) {

        // note: currently, any user of the group may reject a suggestion
        if (user == null) {
            LOG.debug("No user given for checking permission to reject suggestion");
            return false;
        }
        return groupService.isUserMemberOfGroup(user, sugg.getGroup());
    }

    /**
     * checks if it is allowed to update a given annotation
     *  
     * @param annotation the annotation that should be updated
     * @return flag indicating if updating is permitted (in general; user permission has to be checked in a further step)
     */
    public boolean canAnnotationBeUpdated(final Annotation annotation) {

        if (annotation == null) {
            LOG.error("Annotation to be checked for being updateable is invalid!");
            return false;
        }

        return !annotation.isResponseStatusSent();
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

}
