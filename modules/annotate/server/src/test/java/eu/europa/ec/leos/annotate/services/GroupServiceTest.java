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
package eu.europa.ec.leos.annotate.services;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.GroupAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.exceptions.UserAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.impl.UUIDGeneratorServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GroupServiceTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that group creation fails if no name is given
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testCreateGroupFails() {

        try {
            groupService.createGroup("", true);
            Assert.fail("Group creation should throw exception when intended group name is missing; did not!");
        } catch (Exception e) {
            // OK
        }
    }

    /**
     * Test creation of random groups
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testCreateManyGroups() throws GroupAlreadyExistingException {

        final int numberOfGroups = 100;

        List<String> groupNames = new ArrayList<String>();
        UUIDGeneratorService uuidService = new UUIDGeneratorServiceImpl();

        // arrange
        for (int i = 0; i < numberOfGroups; i++) {
            groupNames.add(uuidService.generateUrlSafeUUID());
        }

        // verify before: no groups
        Assert.assertEquals(0, groupRepos.count());

        // act: create groups
        for (int i = 0; i < numberOfGroups; i++) {
            groupService.createGroup(groupNames.get(i), false);
        }

        // verify: <numberOfGroups> groups
        Assert.assertEquals(numberOfGroups, groupRepos.count());

        // trying to create again throws exceptions, but doesn't create supplementary items
        for (int i = 0; i < numberOfGroups; i++) {
            try {
                groupService.createGroup(groupNames.get(i), false);
                Assert.fail("Trying to create existing group again should throw exception - did not!");
            } catch (Exception e) {
                // OK
            }
        }
        Assert.assertEquals(numberOfGroups, groupRepos.count()); // number of groups did not change

        // verify details
        for (int i = 0; i < numberOfGroups; i++) {

            Group foundGroup = groupService.findGroupByName(groupNames.get(i));
            Assert.assertNotNull(foundGroup);
            Assert.assertEquals(groupNames.get(i), foundGroup.getName());
            Assert.assertEquals(groupNames.get(i), foundGroup.getDisplayName());
            Assert.assertEquals(groupNames.get(i), foundGroup.getDescription());
        }
    }

    /**
     * Test that assigning user to default group fails when default group is not configured
     */
    @Test
    public void testDefaultGroupRequired() throws UserAlreadyExistingException {

        User u = new User("username");
        userRepos.save(u);

        // in test setup, all groups were removed - i.e. that there is no default group entry available
        try {
            groupService.assignUserToDefaultGroup(u);
            Assert.fail("Expected exception about missing default group entries not received");
        } catch (DefaultGroupNotFoundException dgne) {
        }
    }

    /**
     * test that assigning user twice to the default group doesn't actually do anything
     * (and does not throw exceptions either) 
     */
    @Test
    public void testNoDoubleAssignmentToDefaultGroup() throws DefaultGroupNotFoundException {

        User theUser = userRepos.save(new User("demo"));
        TestDbHelper.insertDefaultGroup(groupRepos);
        Assert.assertEquals(0, userGroupRepos.count()); // empty

        groupService.assignUserToDefaultGroup(theUser);
        Assert.assertEquals(1, userGroupRepos.count()); // assignment entry was created

        // doing it a second time should still not throw any exception
        groupService.assignUserToDefaultGroup(theUser);

        Assert.assertEquals(1, userGroupRepos.count()); // and there is still only one assignment entry
    }

    /**
     * check that assignment of users to groups throws exceptions when required parameters are missing
     */
    @Test
    public void testAssignUserToGroupInvalidParameters() {

        try {
            groupService.assignUserToGroup(null, null);
            Assert.fail("Method should throw exception due to missing parameters; did not");
        } catch (IllegalArgumentException iae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception: " + e.toString());
        }

        try {
            User dummyUser = new User("me");
            groupService.assignUserToGroup(dummyUser, null);
            Assert.fail("Method should throw exception due to missing parameters; did not");
        } catch (IllegalArgumentException iae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception: " + e.toString());
        }
    }

    /**
     * test assigning user to a group works, and does not complain when called a second time (but doesn't do anything)
     */
    @Test
    public void testAssignUserToGroup_New_And_AlreadyAssigned() {

        // prepare: create group and user
        User theUser = userRepos.save(new User("demo"));
        Assert.assertEquals(0, userGroupRepos.count()); // empty

        Group testGroup = new Group("testgroup", false);
        groupRepos.save(testGroup);

        // test
        Assert.assertEquals(0, userGroupRepos.count()); // no assignments before
        Assert.assertTrue(groupService.assignUserToGroup(theUser, testGroup));
        Assert.assertEquals(1, userGroupRepos.count()); // assignment entry was created
        Assert.assertTrue(groupService.isUserMemberOfGroup(theUser, testGroup));

        // doing it a second time should work without protest
        Assert.assertTrue(groupService.assignUserToGroup(theUser, testGroup));
    }

    /**
     * test that a user's group membership is found correctly
     */
    @Test
    public void testGroupMembership() {

        User theUser = userRepos.save(new User("demo"));
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        Assert.assertFalse(groupService.isUserMemberOfGroup(theUser, defaultGroup));

        // assign user to group now
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        Assert.assertTrue(groupService.isUserMemberOfGroup(theUser, defaultGroup));
    }

    /**
     * test that a user's group membership throws exceptions if required parameters are missing
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testGroupMembershipCheckFailures() {

        try {
            groupService.isUserMemberOfGroup(null, new Group());
            Assert.fail("Expected exception about invalid argument not received!");
        } catch (Exception e) {
            // OK
        }

        try {
            groupService.isUserMemberOfGroup(new User(), null);
            Assert.fail("Expected exception about invalid argument not received!");
        } catch (Exception e) {
            // OK
        }
    }

    /**
     * test search for user's group memberships
     */
    @Test
    public void testGroupsOfUser() {

        User theUser = userRepos.save(new User("demo"));
        User anotherUser = userRepos.save(new User("other"));
        User grouplessUser = userRepos.save(new User("lonesomeuser"));
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        Group anotherGroup = groupRepos.save(new Group("internalGroupId", "group nicename", "Group description", false));

        // empty lists received so far as users are not associated to groups yet
        Assert.assertEquals(0, groupService.getGroupsOfUser(theUser).size());
        Assert.assertEquals(0, groupService.getGroupsOfUser(anotherUser).size());

        // assign users to groups now
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), anotherGroup.getId()));
        userGroupRepos.save(new UserGroup(anotherUser.getId(), defaultGroup.getId()));

        // retrieve
        List<Group> groupsOfTheUser = groupService.getGroupsOfUser(theUser);
        List<Group> groupsOfAnotherUser = groupService.getGroupsOfUser(anotherUser);
        List<Group> groupsOfLonesomeUser = groupService.getGroupsOfUser(grouplessUser);

        // verify assignments
        Assert.assertEquals(2, groupsOfTheUser.size());
        Assert.assertEquals(1, groupsOfAnotherUser.size());
        Assert.assertEquals(0, groupsOfLonesomeUser.size());

        Assert.assertTrue(groupsOfTheUser.stream().filter(group -> group.equals(defaultGroup)).findAny().isPresent());
        Assert.assertTrue(groupsOfTheUser.stream().filter(group -> group.equals(anotherGroup)).findAny().isPresent());
        Assert.assertTrue(groupsOfAnotherUser.stream().filter(group -> group.equals(defaultGroup)).findAny().isPresent());

        // make sure that group visibility is correct - check the "groupsOfTheUser": one is public, other one isn't
        Assert.assertTrue(groupsOfTheUser.stream().filter(group -> group.equals(defaultGroup)).collect(Collectors.toList()).get(0).isPublicGroup());
        Assert.assertFalse(groupsOfTheUser.stream().filter(group -> group.equals(anotherGroup)).collect(Collectors.toList()).get(0).isPublicGroup());
    }

    /**
     * test the retrieval of a user's group as Json result
     * (used by groups API)
     */
    @Test
    public void testGroupsOfUserAsJson() {

        java.util.Random r = new java.util.Random();

        final String privateGroupName1 = "group nicename", privateGroupName2 = "other private group nicename";
        final String publicGroupName1 = "the public group", publicGroupName2 = "a public group";

        // no groups set for the user
        User theUser = userRepos.save(new User("demo"));
        Assert.assertEquals(0, groupService.getUserGroupsAsJson(theUser).size());

        // assign user to groups: the public default group, two private ones, two public ones
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        Group firstPrivateGroup = groupRepos.save(new Group(Integer.toString(r.nextInt()), privateGroupName1, "Group description", false));
        Group secondPrivateGroup = groupRepos.save(new Group(Integer.toString(r.nextInt()), privateGroupName2, "Group description", false));
        Group firstPublicGroup = groupRepos.save(new Group(Integer.toString(r.nextInt()), publicGroupName1, "Group description", true));
        Group secondPublicGroup = groupRepos.save(new Group(Integer.toString(r.nextInt()), publicGroupName2, "Group description", true));

        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), firstPrivateGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), secondPrivateGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), firstPublicGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), secondPublicGroup.getId()));

        // retrieve
        List<JsonGroupWithDetails> userGroups = groupService.getUserGroupsAsJson(theUser);
        Assert.assertNotNull(userGroups);
        Assert.assertEquals(5, userGroups.size());

        // verify
        // world public group first
        JsonGroupWithDetails extractedDefaultGroup = userGroups.get(0);
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());

        // two public groups next, sorted by display name
        Assert.assertEquals(userGroups.get(1).getName(), publicGroupName2);
        Assert.assertEquals(userGroups.get(2).getName(), publicGroupName1);

        // two private groups at the end, sorted by display name
        Assert.assertEquals(userGroups.get(3).getName(), privateGroupName1);
        Assert.assertEquals(userGroups.get(4).getName(), privateGroupName2);

        JsonGroupWithDetails extractedPrivateGroup = userGroups.get(3);
        Assert.assertNotNull(extractedPrivateGroup);
        Assert.assertEquals("private", extractedPrivateGroup.getType());
        Assert.assertFalse(extractedPrivateGroup.isPublic());
        Assert.assertFalse(extractedPrivateGroup.isScoped());
    }

    /**
     * test that the default group is returned if no user is specified
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testGroupsOfNullUser() {

        // when no user given, we expect the default group to be returned
        // a) if no default group is defined, there may be an exception
        try {
            groupService.getUserGroupsAsJson(null);
            Assert.fail("Expected exception for unavailable default group not found");
        } catch (Exception e) {
            // ok
        }

        // b) define default group, but don't assign any user to it
        // then ask again -> default group should be returned anyway
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        List<JsonGroupWithDetails> details = groupService.getUserGroupsAsJson(null);
        Assert.assertNotNull(details);
        Assert.assertEquals(1, details.size());

        // verify
        // world public group (=default group) is returned
        JsonGroupWithDetails extractedDefaultGroup = details.get(0);
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals(defaultGroup.getDisplayName(), extractedDefaultGroup.getName());
        Assert.assertEquals(defaultGroup.getName(), extractedDefaultGroup.getId());
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());

        // c) check again for an unknown user - empty result
        details = groupService.getUserGroupsAsJson(new User("unknownUser"));
        Assert.assertNotNull(details);
        Assert.assertEquals(0, details.size());
    }

    /**
     * test search for users of a group
     */
    @Test
    public void testUsersOfGroup() {

        User theUser = userRepos.save(new User("demo"));
        User anotherUser = userRepos.save(new User("other"));
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        Group anotherGroup = groupRepos.save(new Group("internalGroupId", "group nicename", "Group description", false));

        Assert.assertNull(groupService.getUserIdsOfGroup(""));
        Assert.assertEquals(0, groupService.getUserIdsOfGroup(new Group()).size()); // test with undefined group
        Assert.assertEquals(0, groupService.getUserIdsOfGroup(defaultGroup).size());
        Assert.assertEquals(0, groupService.getUserIdsOfGroup(anotherGroup).size());

        // assign users to groups now
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), anotherGroup.getId()));
        userGroupRepos.save(new UserGroup(anotherUser.getId(), defaultGroup.getId()));

        // retrieve
        List<Long> usersOfDefaultGroup = groupService.getUserIdsOfGroup(defaultGroup);
        List<Long> userssOfAnotherGroup = groupService.getUserIdsOfGroup(anotherGroup.getName()); // test alternative interface

        // verify assignments
        Assert.assertEquals(2, usersOfDefaultGroup.size());
        Assert.assertEquals(1, userssOfAnotherGroup.size());

        Assert.assertTrue(usersOfDefaultGroup.stream().filter(userId -> userId.equals(theUser.getId())).findAny().isPresent());
        Assert.assertTrue(usersOfDefaultGroup.stream().filter(userId -> userId.equals(anotherUser.getId())).findAny().isPresent());
        Assert.assertTrue(userssOfAnotherGroup.stream().filter(userId -> userId.equals(theUser.getId())).findAny().isPresent());
    }

    /**
     * test that a user's group membership search throws exceptions if required parameters are missing
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testGroupsOfUserFailure() {

        try {
            groupService.getGroupsOfUser(null);
            Assert.fail("Expected exception about invalid argument not received!");
        } catch (Exception e) {
            // OK
        }
    }

}
