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

import eu.europa.ec.leos.annotate.model.GroupComparator;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.GroupAlreadyExistingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for managing user groups 
 */
@Service
public class GroupServiceImpl implements GroupService {

    private static final Logger LOG = LoggerFactory.getLogger(GroupServiceImpl.class);

    // we currently only support one single group; this property is injected its default name from configuration
    @Value("${defaultgroup.name}")
    private String DEFAULT_GROUP_NAME;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Service functionality
    // -------------------------------------
    
    /**
     * create a new group with a given name and visibility
     * 
     * @param name the group's name, will also be used for display name and other properties
     * @param isPublic flag indicating if it is a public group
     * @throws GroupAlreadyExistingException if the group is already registered with the given name, this exception is thrown
     * 
     * @return the created {@link Group} object
     */
    @Override
    public Group createGroup(String name, boolean isPublic) throws GroupAlreadyExistingException {
        
        if(StringUtils.isEmpty(name)) {
            LOG.error("Cannot create group without a given name!");
            throw new IllegalArgumentException("Group name undefined!");
        }
        
        LOG.info("Save group with name '{}' in the database", name);
        
        Group newGroup = new Group(name, isPublic);
        try {
            groupRepos.save(newGroup);
            LOG.debug("Group '{}' created with id {}", name, newGroup.getId());
        } catch(DataIntegrityViolationException dive) {
            LOG.error("The group '{}' already exists", name);
            throw new GroupAlreadyExistingException(dive);
        } catch (Exception ex) {
            LOG.error("Exception while creating group", ex);
            throw new GroupAlreadyExistingException(ex);
        }
        return newGroup;
    }
    
    /**
     * find a group based on its internal group name
     * 
     * @param groupName the internal group name, e.g. "__world__"
     * 
     * @return found {@link Group}, or {@literal null}
     */
    @Override
    public Group findGroupByName(String groupName) {

        Group foundGroup = groupRepos.findByName(groupName);
        LOG.debug("Found group based on group name: " + (foundGroup != null));
        return foundGroup;
    }

    /**
     * search the database for the configured default group
     */
    private Group findDefaultGroup() {
        return findGroupByName(DEFAULT_GROUP_NAME);
    }

    /**
     * add a user as member of the default group
     * 
     * @param user the {@link user} to be assigned
     * 
     * @throws DefaultGroupNotFoundException exception thrown in case default group is unavailable
     */
    @Override
    public void assignUserToDefaultGroup(User user) throws DefaultGroupNotFoundException {

        Group defaultGroup = findDefaultGroup();
        if (defaultGroup == null) {
            LOG.error("Cannot assign user to the default group; seems not to be configured in database");
            throw new DefaultGroupNotFoundException();
        }

        assignUserToGroup(user, defaultGroup);
    }

    /**
     * add a user as a member of a given group
     * 
     * @param user the {@link User} to be assigned to a group
     * @param group the {@link Group} the user is to be assigned to
     * 
     * @return flag indicating if user is a group member (in the end)
     */
    @Override
    public boolean assignUserToGroup(User user, Group group) {

        if(user == null || group == null) {
            throw new IllegalArgumentException("user and group must both be defined to assign user to group");
        }
        
        // check if already assigned
        if(isUserMemberOfGroup(user, group)) {
            LOG.info("User '{}' is already member of group '{}' - nothing to do", user.getLogin(), group.getName());
            return true;
        }
        
        long userId = user.getId(), groupId = group.getId();
        UserGroup foundUserGroup = new UserGroup(userId, groupId);
        userGroupRepos.save(foundUserGroup);
        LOG.info("Saved user '{}' (id {}) as member of group '{}' (id {})", user.getLogin(), userId, group.getName(), groupId);

        return true;
    }


    /**
     * check if the default group is configured; throw exception if not
     * 
     * @throws DefaultGroupNotFoundException exception thrown in case default group is unavailable
     */
    @Override
    public void throwIfNotExistsDefaultGroup() throws DefaultGroupNotFoundException {

        if (findDefaultGroup() == null) {
            LOG.error("Default group seems not to be configured in database; throw DefaultGroupNotFoundException");
            throw new DefaultGroupNotFoundException();
        }
    }

    /**
     * check if a given user is member of a given group
     * 
     * @param user 
     *        the user to be checked for group membership
     * @param group
     *        the group which is to be checked if user is member
     *        
     * @return flag indicating if user is member of the group
     */
    @Override
    public boolean isUserMemberOfGroup(User user, Group group) {

        if (user == null) {
            LOG.error("Cannot check if user is group member when no user is given");
            throw new IllegalArgumentException("User is null");
        }

        if (group == null) {
            LOG.error("Cannot check if user is group member when no group is given");
            throw new IllegalArgumentException("Group is null");
        }

        UserGroup membership = userGroupRepos.findByUserIdAndGroupId(user.getId(), group.getId());
        LOG.debug("User '{}' (id {}) is member of group '{}' (id {}): {}", user.getLogin(), user.getId(), group.getName(), group.getId(), membership != null);
        return (membership != null);
    }

    /**
     * retrieve all groups a user is member in
     * 
     * @param user
     *        the user for which groups are to be looked up
     * 
     * @return list of groups the user is registered in or {@literal null} when none are found
     */
    @Override
    public List<Group> getGroupsOfUser(User user) {

        if (user == null) {
            LOG.error("Cannot search for groups of undefined User (null)");
            throw new IllegalArgumentException("User is null");
        }

        List<UserGroup> foundUserGroups = userGroupRepos.findByUserId(user.getId());
        LOG.debug("Found {} groups in which user '{}' is member", foundUserGroups != null ? foundUserGroups.size() : 0, user.getLogin());

        if (foundUserGroups == null) {
            return null;
        }

        // extract groupIds of found assignments and get corresponding groups
        List<Group> foundGroups = groupRepos.findByIdIn(foundUserGroups.stream().map(ug -> ug.getGroupId()).collect(Collectors.toList()));
        return foundGroups;
    }

    /**
     * find all users being member of the group and provide their user IDs
     * 
     *  @param group the group whose users are wanted
     *  @return list of user IDs, or {@literal null}
     */
    @Override
    public List<Long> getUserIdsOfGroup(Group group) {

        if (group == null) {
            LOG.warn("Cannot retrieve user IDs from undefined group");
            return null;
        }

        List<UserGroup> userGroups = userGroupRepos.findByGroupId(group.getId());
        if (userGroups == null) return null;

        // extract the userId
        List<Long> userIds = userGroups.stream().map(UserGroup::getUserId).collect(Collectors.toList());

        return userIds;
    }

    /**
     * find all users being member of the group and provide their user IDs
     * 
     *  @param groupName the name of the group whose users are wanted
     *  @return list of user IDs, or {@literal null}
     */
    @Override
    public List<Long> getUserIdsOfGroup(String groupName) {

        Group group = findGroupByName(groupName);
        return getUserIdsOfGroup(group);
    }

    /**
     * find all groups in which a user is member and provide their details in the Json format 
     * that should be returned by the groups API
     * 
     *  @param user the user for which all groups are wanted
     *  @return list of found groups, wrapped in Json objects ({@link JsonGroupWithDetails}); 
     *          if no user is given (=not logged in), only the default group is returned
     */
    @Override
    public List<JsonGroupWithDetails> getUserGroupsAsJson(User user) {

        List<Group> allGroups = null;
        
        if (user == null) {
            LOG.info("Groups retrieval request received without user - return default group only");
            allGroups = new ArrayList<Group>();
            allGroups.add(findDefaultGroup());
        } else {

            allGroups = getGroupsOfUser(user);
            if (allGroups == null) {
                LOG.warn("Did not receive a valid result from querying groups of user");
                return null;
            }
            LOG.debug("Found {} groups for user '{}'", allGroups.size(), user.getLogin());
        }
        
        // sort the groups as desired
        allGroups.sort(new GroupComparator(DEFAULT_GROUP_NAME));

        List<JsonGroupWithDetails> results = new ArrayList<JsonGroupWithDetails>();
        for (int i = 0; i < allGroups.size(); i++) {
            Group currentGroup = allGroups.get(i);
            results.add(new JsonGroupWithDetails(currentGroup.getDisplayName(), currentGroup.getName(), currentGroup.isPublicGroup()));
        }

        return results;
    }
}
