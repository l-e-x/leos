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
import eu.europa.ec.leos.annotate.model.GroupComparator;
import eu.europa.ec.leos.annotate.model.UserInformation;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
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

    // this property is injected its default name from configuration
    @Value("${defaultgroup.name}")
    private String defaultGroupName;

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
     * return the name of the default group - instead of hard-coding it...
     */
    public String getDefaultGroupName() {
        return defaultGroupName;
    }

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
    public Group createGroup(final String name, final boolean isPublic) throws GroupAlreadyExistingException {

        Assert.isTrue(!StringUtils.isEmpty(name), "Cannot create group without a given name!");

        LOG.info("Save group with name '{}' in the database", name);

        final Group newGroup = new Group(getInternalGroupName(name), name, isPublic);
        try {
            groupRepos.save(newGroup);
            LOG.debug("Group '{}' created with id {}", name, newGroup.getId());
        } catch (DataIntegrityViolationException dive) {
            LOG.error("The group '{}' already exists", name);
            throw new GroupAlreadyExistingException(dive);
        } catch (Exception ex) {
            LOG.error("Exception while creating group", ex);
            throw new GroupAlreadyExistingException(ex);
        }
        return newGroup;
    }

    /**
     * generate an internal name for the group, which is URL-safe
     * 
     * @param groupDisplayName nice name of the group
     * @return URL-safe internal group name
     */
    @Override
    public String getInternalGroupName(final String groupDisplayName) {

        // idea: drop all characters not contained in any of the above ranges - this implicitly also removes spaces
        final StringBuilder simpleName = new StringBuilder();
        for (int i = 0; i < groupDisplayName.length(); i++) {

            final int codePoint = Character.codePointAt(groupDisplayName, i);
            if(isUnreservedCharacter(codePoint)) {
                simpleName.append(groupDisplayName.charAt(i));
            }
        }

        return simpleName.toString();
    }
    
    /**
     * we want to keep it simple and allow those characters denoted "unreserved characters" in section 2.3 of RFC 3986:
     * ALPHA / DIGIT / "-" / "." / "_" / "~"
     * (where we dropped "~")
     * corresponds to character ranges:
     * ALPHA: [a-z] = 97-122, [A-Z] = 65-90
     * DIGIT: [0-9] = 48-57
     * "-" = 45, "." = 46, "_" = 95
     * @param codePoint the character code to analyse
     * @return flag indicating if it is an "allowed character"
     */
    private boolean isUnreservedCharacter(final int codePoint) {
        
        return 97 <= codePoint && codePoint <= 122 ||
                65 <= codePoint && codePoint <= 90 || 
                48 <= codePoint && codePoint <= 57 || 
                codePoint == 45 || codePoint == 46 || codePoint == 95;
    }

    /**
     * find a group based on its internal group name
     * 
     * @param groupName the internal group name, e.g. "__world__"
     * 
     * @return found {@link Group}, or {@literal null}
     */
    @Override
    public Group findGroupByName(final String groupName) {

        Group foundGroup = groupRepos.findByName(groupName);
        LOG.debug("Found group based on group name: {}", foundGroup != null);
        if(foundGroup == null) {
            // try again with URL-conform internal name
            foundGroup = groupRepos.findByName(getInternalGroupName(groupName));
            LOG.debug("Found group based on internal group name: {}", foundGroup != null);
        }
        return foundGroup;
    }

    /**
     * search the database for the configured default group
     * 
     * @return found default {@link Group}, or {@literal null}
     */
    public Group findDefaultGroup() {
        return findGroupByName(defaultGroupName);
    }

    /**
     * add a user as member of the default group
     * 
     * @param user the {@link user} to be assigned
     * 
     * @throws DefaultGroupNotFoundException exception thrown in case default group is unavailable
     */
    @Override
    public void assignUserToDefaultGroup(final User user) throws DefaultGroupNotFoundException {

        final Group defaultGroup = findDefaultGroup();
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
    public boolean assignUserToGroup(final User user, final Group group) {

        Assert.notNull(user, "User must be defined to assign user to group");
        Assert.notNull(group, "Group must be defined to assign user to group");

        // check if already assigned
        if (isUserMemberOfGroup(user, group)) {
            LOG.info("User '{}' is already member of group '{}' - nothing to do", user.getLogin(), group.getName());
            return true;
        }

        final long userId = user.getId();
        final long groupId = group.getId();
        final UserGroup foundUserGroup = new UserGroup(userId, groupId);
        userGroupRepos.save(foundUserGroup);
        LOG.info("Saved user '{}' (id {}) as member of group '{}' (id {})", user.getLogin(), userId, group.getName(), groupId);

        return true;
    }

    /**
     * remove a user as member of a given group
     * 
     * @param user the {@link User} to be removed from a group
     * @param group the {@link Group} from which the user is to be removed
     * 
     * @return flag indicating if the user was a member of the group and removed
     */
    @Override
    @Transactional
    public boolean removeUserFromGroup(final User user, final Group group) {
        
        Assert.notNull(user, "User must be defined to remove user to group");
        Assert.notNull(group, "Group must be defined to remove user to group");

        // check if already assigned
        if (!isUserMemberOfGroup(user, group)) {
            LOG.info("User '{}' is no member of group '{}' - nothing to do", user.getLogin(), group.getName());
            return false;
        }

        final long userId = user.getId();
        final long groupId = group.getId();
        userGroupRepos.deleteByUserIdAndGroupId(userId, groupId);
        LOG.info("Removed user '{}' (id {}) as member of group '{}' (id {})", user.getLogin(), userId, group.getName(), groupId);

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
    public boolean isUserMemberOfGroup(final User user, final Group group) {

        Assert.notNull(user, "Cannot check if user is group member when no user is given");
        Assert.notNull(group, "Cannot check if user is group member when no group is given");

        final UserGroup membership = userGroupRepos.findByUserIdAndGroupId(user.getId(), group.getId());
        LOG.debug("User '{}' (id {}) is member of group '{}' (id {}): {}", user.getLogin(), user.getId(), group.getName(), group.getId(), membership != null);
        return membership != null;
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
    public List<Group> getGroupsOfUser(final User user) {

        Assert.notNull(user, "Cannot search for groups of undefined User (null)");

        final List<UserGroup> foundUserGroups = userGroupRepos.findByUserId(user.getId());
        LOG.debug("Found {} groups in which user '{}' is member", foundUserGroups == null ? 0 : foundUserGroups.size(), user.getLogin());

        if (foundUserGroups == null) {
            return null;
        }

        // extract groupIds of found assignments and get corresponding groups
        return groupRepos.findByIdIn(foundUserGroups.stream().map(usergroup -> usergroup.getGroupId()).collect(Collectors.toList()));
    }

    /**
     * find all groups a user is member of and provide their group IDs
     * 
     *  @param user the user whose groups are wanted
     *  @return list of group IDs, or {@literal null}
     */
    @Override
    public List<Long> getGroupIdsOfUser(final User user) {

        if (user == null) {
            LOG.warn("Cannot retrieve group IDs from undefined user");
            return null;
        }

        final List<UserGroup> userGroups = userGroupRepos.findByUserId(user.getId());
        if (userGroups == null) return null;

        // extract the groupIds
        return userGroups.stream().map(UserGroup::getGroupId).distinct().collect(Collectors.toList());
    }

    /**
     * find all users being member of the group and provide their user IDs
     * 
     *  @param group the group whose users are wanted
     *  @return list of user IDs, or {@literal null}
     */
    @Override
    public List<Long> getUserIdsOfGroup(final Group group) {

        if (group == null) {
            LOG.warn("Cannot retrieve user IDs from undefined group");
            return null;
        }

        final List<UserGroup> userGroups = userGroupRepos.findByGroupId(group.getId());
        if (userGroups == null) return null;

        // extract the userId
        return userGroups.stream().map(UserGroup::getUserId).collect(Collectors.toList());
    }

    /**
     * find all users being member of the group and provide their user IDs
     * 
     *  @param groupName the name of the group whose users are wanted
     *  @return list of user IDs, or {@literal null}
     */
    @Override
    public List<Long> getUserIdsOfGroup(final String groupName) {

        final Group group = findGroupByName(groupName);
        return getUserIdsOfGroup(group);
    }

    /**
     * find all groups in which a user is member and provide their details in the JSON format 
     * that should be returned by the groups API
     * 
     *  @param userinfo {@link UserInformation} of the user for which all groups are wanted; ISC users are treated slightly different than others
     *  @return list of found groups, wrapped in JSON objects ({@link JsonGroupWithDetails}); 
     *          if no user is given (=not logged in), only the default group is returned
     */
    @Override
    public List<JsonGroupWithDetails> getUserGroupsAsJson(final UserInformation userinfo) {

        List<Group> allGroups = null;

        if (userinfo == null ||
                userinfo.getUser() == null || 
                StringUtils.isEmpty(userinfo.getAuthority())) {
            LOG.info("Groups retrieval request received without user - return default group only");
            allGroups = new ArrayList<Group>();
            allGroups.add(findDefaultGroup());
        } else {

            final User user = userinfo.getUser();
            allGroups = getGroupsOfUser(user);
            if (allGroups == null) {
                LOG.warn("Did not receive a valid result from querying groups of user");
                return null;
            }
            LOG.debug("Found {} groups for user '{}'", allGroups.size(), user.getLogin());
            
            if(Authorities.isIsc(userinfo.getAuthority())) {
                // for ISC, we should not return the default group -> filter out
                LOG.debug("Remove default group for ISC user {}", user.getLogin());
                allGroups.remove(findDefaultGroup());
            }
        }

        // sort the groups as desired
        allGroups.sort(new GroupComparator(defaultGroupName));

        final List<JsonGroupWithDetails> results = new ArrayList<JsonGroupWithDetails>();
        for (int i = 0; i < allGroups.size(); i++) {
            final Group currentGroup = allGroups.get(i);
            results.add(new JsonGroupWithDetails(currentGroup.getDisplayName(), currentGroup.getName(), currentGroup.isPublicGroup()));
        }

        return results;
    }
}
