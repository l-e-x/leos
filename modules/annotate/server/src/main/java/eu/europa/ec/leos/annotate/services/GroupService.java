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

import java.util.List;

public interface GroupService {

    // finding group
    Group findGroupByName(String groupName);

    // creating a new group
    Group createGroup(String entity, boolean isPublic) throws GroupAlreadyExistingException;

    String getDefaultGroupName();
    
    Group findDefaultGroup();
    
    /**
     * group assignments
     */
    // assignment to user to default group and default group availability check
    void throwIfNotExistsDefaultGroup() throws DefaultGroupNotFoundException;

    // assignment of users to specific groups, or to default group
    boolean assignUserToGroup(User user, Group entityGroup);
    void assignUserToDefaultGroup(User user) throws DefaultGroupNotFoundException;

    // retrieval of group memberships
    boolean isUserMemberOfGroup(User user, Group group);

    // retrieve all groups in which a user is member
    List<Group> getGroupsOfUser(User user);

    // return all IDs of the groups that the user belongs to
    List<Long> getGroupIdsOfUser(User user);
    
    // return all IDs of the users belonging to a given group
    List<Long> getUserIdsOfGroup(Group group);

    List<Long> getUserIdsOfGroup(String groupName);

    // returns the groups associated as a user, in send-ready JSON objects
    List<JsonGroupWithDetails> getUserGroupsAsJson(UserInformation userinfo);

}
