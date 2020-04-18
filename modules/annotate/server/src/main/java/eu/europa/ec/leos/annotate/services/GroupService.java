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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.GroupAlreadyExistingException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GroupService {

    /**
     * finding a group given its name
     * 
     * @param groupName
     *        name to be matched; note: if not found, matching an internal name is tried also
     * @return found {@link Group}, or {@literal null}
     */
    Group findGroupByName(String groupName);

    /**
     * create a new group
     *  
     * @param entity
     *        name of the new group
     * @param isPublic
     *        flag indicating if this group is public (not used yet)
     * @return created {@link Group}
     * @throws GroupAlreadyExistingException
     *         thrown if a group with the same name already exists
     */
    Group createGroup(String entity, boolean isPublic) throws GroupAlreadyExistingException;

    /**
     * computes the internal name of a group
     * 
     * @param groupDisplayName
     *        the display name of the group
     * @return computed internal name, URL-conform
     */
    String getInternalGroupName(final String groupDisplayName);
    
    /**
     * gives the name of the default group
     * 
     * @return default group name
     */
    String getDefaultGroupName();
    
    /**
     * looks up the default group
     * 
     * @return found {@link Group}, or {@literal null} 
     */
    Group findDefaultGroup();
    
    /**
     * group assignments
     */
    
    /**
     * assignment of user to default group and default group availability check
     * 
     * @throws DefaultGroupNotFoundException
     *         thrown when no default group can be found in the database
     */
    void throwIfNotExistsDefaultGroup() throws DefaultGroupNotFoundException;

    /**
     * assignment of a user to specific group, or to default group
     *  
     * @param user
     *        {@link User} to be assigned to a group
     * @param entityGroup
     *        {@link Group} to which the user is to be assigned to
     * @return flag indicating success
     */
    boolean assignUserToGroup(User user, Group entityGroup);
    
    /**
     * assignment of a user to the default group
     *  
     * @param user
     *        {@link User} to be assigned to a group
     * @throws DefaultGroupNotFoundException
     *         thrown when no default group can be found in the database
     */
    void assignUserToDefaultGroup(User user) throws DefaultGroupNotFoundException;

    /**
     * removal of a user from a group
     * 
     * @param user
     *        {@link User} to be removed from a group
     * @param group
     *        {@link Group} from which the user is to be removed
     * @return flag indicating success
     */
    @Transactional
    boolean removeUserFromGroup(User user, Group group);

    /**
     * retrieval of group memberships
     * 
     * @param user
     *        {@link User} for which we want to know if he is member of a group
     * @param group
     *        {@link Group} into which a user is possibly member
     * @return flag indicating if the user is member of the given group
     */
    boolean isUserMemberOfGroup(User user, Group group);

    /**
     * retrieve all groups in which a user is member
     * 
     * @param user
     *        {@link User} for which all group memberships are wanted
     * @return list of {@link Group}s in which the user is member
     */
    List<Group> getGroupsOfUser(User user);

    /**
     * return all IDs of the groups that the user belongs to
     * 
     * @param user
     *        {@link User} for which all group memberships are wanted
     * @return list of {@link Group} IDs in which the user is member
     */
    List<Long> getGroupIdsOfUser(User user);
    
    /**
     * return all IDs of the users belonging to a given group
     * 
     * @param group
     *        {@link Group} for which all members are wanted
     * @return list of user IDs being member of the given group
     */
    List<Long> getUserIdsOfGroup(Group group);

    /**
     * return all IDs of the users belonging to a given group
     * 
     * @param groupName
     *        name of the group for which all members are wanted
     * @return list of user IDs being member of the given group
     */
    List<Long> getUserIdsOfGroup(String groupName);

    /**
     * returns the groups associated as a user, in send-ready JSON objects
     * 
     * @param userinfo
     *        information about the user for whom all group information is wanted
     * @return list of {@link JsonGroupWithDetails} objects containing all group information
     */
    List<JsonGroupWithDetails> getUserGroupsAsJson(UserInformation userinfo);

}
