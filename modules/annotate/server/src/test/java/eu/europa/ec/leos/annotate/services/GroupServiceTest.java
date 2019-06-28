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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserInformation;
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
@SpringBootTest(properties = "spring.config.name=anot")
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
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that group creation fails if no name is given
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testCreateGroupFails() throws GroupAlreadyExistingException {

        groupService.createGroup("", true);
        Assert.fail("Group creation should throw exception when intended group name is missing; did not!");
    }

    /**
     * Test creation of random groups
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testCreateManyGroups() throws GroupAlreadyExistingException {

        final int numberOfGroups = 100;

        final List<String> groupNames = new ArrayList<String>();
        final UUIDGeneratorService uuidService = new UUIDGeneratorServiceImpl();

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

            final Group foundGroup = groupService.findGroupByName(groupNames.get(i));
            Assert.assertNotNull(foundGroup);
            Assert.assertEquals(groupNames.get(i), foundGroup.getName());
            Assert.assertEquals(groupNames.get(i), foundGroup.getDisplayName());
            Assert.assertEquals(groupNames.get(i), foundGroup.getDescription());
        }
    }

    /**
     * Test that group names are generated with URL-safe letters only
     */
    @Test
    public void testGroupNameGeneration() throws GroupAlreadyExistingException {

        final String inputName = "§Th1s Is-Th€.Gr0\\up/_N@me>";
        final String simplifiedInputName = "Th1sIs-Th.Gr0up_Nme";

        // so we expect that 'simplifiedInputName' would be chosen
        final Group created = groupService.createGroup(inputName, true);
        Assert.assertNotNull(created);
        Assert.assertEquals(simplifiedInputName, created.getName());
    }

    /**
     * Test that assigning user to default group fails when default group is not configured
     */
    @Test(expected = DefaultGroupNotFoundException.class)
    public void testDefaultGroupRequired() throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        final User user = new User("username");
        userRepos.save(user);

        // in test setup, all groups were removed - i.e. that there is no default group entry available
        groupService.assignUserToDefaultGroup(user);
        Assert.fail("Expected exception about missing default group entries not received");
    }

    /**
     * test that assigning user twice to the default group doesn't actually do anything
     * (and does not throw exceptions either) 
     */
    @Test
    public void testNoDoubleAssignmentToDefaultGroup() throws DefaultGroupNotFoundException {

        final User theUser = userRepos.save(new User("demo"));
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
    @Test(expected = IllegalArgumentException.class)
    public void testAssignUserToGroupInvalidParameters1() {

        groupService.assignUserToGroup(null, null);
    }

    /**
     * check that assignment of users to groups throws exceptions when required parameters are missing
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAssignUserToGroupInvalidParameters2() {

        final User dummyUser = new User("me");
        groupService.assignUserToGroup(dummyUser, null);
    }

    /**
     * test assigning user to a group works, and does not complain when called a second time (but doesn't do anything)
     */
    @Test
    public void testAssignUserToGroup_New_And_AlreadyAssigned() {

        // prepare: create group and user
        final User theUser = userRepos.save(new User("demo"));
        Assert.assertEquals(0, userGroupRepos.count()); // empty

        final Group testGroup = new Group("testgroup", false);
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

        final User theUser = userRepos.save(new User("demo"));
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        Assert.assertFalse(groupService.isUserMemberOfGroup(theUser, defaultGroup));

        // assign user to group now
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        Assert.assertTrue(groupService.isUserMemberOfGroup(theUser, defaultGroup));
    }

    /**
     * test that a user's group membership throws exceptions if required parameters are missing
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupMembershipCheckFailures1() {

        groupService.isUserMemberOfGroup(null, new Group());
    }

    /**
     * test that a user's group membership throws exceptions if required parameters are missing
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupMembershipCheckFailures2() {

        groupService.isUserMemberOfGroup(new User(), null);
    }

    /**
     * test search for user's group memberships
     */
    @Test
    public void testGroupsOfUser() {

        final User theUser = userRepos.save(new User("demo"));
        final User anotherUser = userRepos.save(new User("other"));
        final User grouplessUser = userRepos.save(new User("lonesomeuser"));
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group anotherGroup = groupRepos.save(new Group("internalGroupId", "group nicename", "Group description", false));

        // empty lists received so far as users are not associated to groups yet
        Assert.assertEquals(0, groupService.getGroupsOfUser(theUser).size());
        Assert.assertEquals(0, groupService.getGroupsOfUser(anotherUser).size());

        // assign users to groups now
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), anotherGroup.getId()));
        userGroupRepos.save(new UserGroup(anotherUser.getId(), defaultGroup.getId()));

        // retrieve
        final List<Group> groupsOfTheUser = groupService.getGroupsOfUser(theUser);
        final List<Group> groupsOfAnotherUser = groupService.getGroupsOfUser(anotherUser);
        final List<Group> groupsOfLonesomeUser = groupService.getGroupsOfUser(grouplessUser);

        // verify assignments
        Assert.assertEquals(2, groupsOfTheUser.size());
        Assert.assertEquals(1, groupsOfAnotherUser.size());
        Assert.assertEquals(0, groupsOfLonesomeUser.size());

        Assert.assertTrue(groupsOfTheUser.stream().anyMatch(group -> group.equals(defaultGroup)));
        Assert.assertTrue(groupsOfTheUser.stream().anyMatch(group -> group.equals(anotherGroup)));
        Assert.assertTrue(groupsOfAnotherUser.stream().anyMatch(group -> group.equals(defaultGroup)));

        // make sure that group visibility is correct - check the "groupsOfTheUser": one is public, other one isn't
        Assert.assertTrue(groupsOfTheUser.stream().filter(group -> group.equals(defaultGroup)).collect(Collectors.toList()).get(0).isPublicGroup());
        Assert.assertFalse(groupsOfTheUser.stream().filter(group -> group.equals(anotherGroup)).collect(Collectors.toList()).get(0).isPublicGroup());

        // check retrieval of the user's group IDs
        final List<Long> groupIdsOfTheUser = groupService.getGroupIdsOfUser(theUser);
        final List<Long> groupIdsOfAnotherUser = groupService.getGroupIdsOfUser(anotherUser);
        final List<Long> groupIdsOfLonesomeUser = groupService.getGroupIdsOfUser(grouplessUser);

        // verify received IDs
        Assert.assertTrue(groupIdsOfTheUser.stream().anyMatch(groupId -> groupId.equals(defaultGroup.getId())));
        Assert.assertTrue(groupIdsOfTheUser.stream().anyMatch(groupId -> groupId.equals(anotherGroup.getId())));
        Assert.assertTrue(groupIdsOfAnotherUser.stream().anyMatch(groupId -> groupId.equals(defaultGroup.getId())));
        Assert.assertEquals(0, groupIdsOfLonesomeUser.size());
    }

    /**
     * test the retrieval of a user's group as JSON result for a LEOS/EdiT user
     * (used by groups API)
     */
    @Test
    public void testGroupsOfLeosUserAsJson() {

        final java.util.Random rand = new java.util.Random();

        final String privateGroupName1 = "group nicename";
        final String privateGroupName2 = "other private group nicename";
        final String publicGroupName1 = "the public group";
        final String publicGroupName2 = "a public group";

        // no groups set for the user
        final User theUser = userRepos.save(new User("demo"));
        final UserInformation userinfo = new UserInformation(theUser, Authorities.EdiT);
        Assert.assertEquals(0, groupService.getUserGroupsAsJson(userinfo).size());

        // assign user to groups: the public default group, two private ones, two public ones
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group firstPrivateGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), privateGroupName1, "Group description", false));
        final Group secondPrivateGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), privateGroupName2, "Group description", false));
        final Group firstPublicGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), publicGroupName1, "Group description", true));
        final Group secondPublicGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), publicGroupName2, "Group description", true));

        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), firstPrivateGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), secondPrivateGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), firstPublicGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), secondPublicGroup.getId()));

        // retrieve
        final List<JsonGroupWithDetails> userGroupsLeos = groupService.getUserGroupsAsJson(userinfo);
        Assert.assertNotNull(userGroupsLeos);
        Assert.assertEquals(5, userGroupsLeos.size());

        // verify
        // world public group first
        final JsonGroupWithDetails extractedDefaultGroup = userGroupsLeos.get(0);
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());

        // two public groups next, sorted by display name
        Assert.assertEquals(userGroupsLeos.get(1).getName(), publicGroupName2);
        Assert.assertEquals(userGroupsLeos.get(2).getName(), publicGroupName1);

        // two private groups at the end, sorted by display name
        Assert.assertEquals(userGroupsLeos.get(3).getName(), privateGroupName1);
        Assert.assertEquals(userGroupsLeos.get(4).getName(), privateGroupName2);

        final JsonGroupWithDetails extractedPrivateGroup = userGroupsLeos.get(3);
        Assert.assertNotNull(extractedPrivateGroup);
        Assert.assertEquals("private", extractedPrivateGroup.getType());
        Assert.assertFalse(extractedPrivateGroup.isPublic());
        Assert.assertFalse(extractedPrivateGroup.isScoped());
    }

    /**
     * test the retrieval of a user's group as JSON result for an ISC user (should not show the default group)
     * (used by groups API)
     */
    @Test
    public void testGroupsOfIscUserAsJson() {

        final java.util.Random rand = new java.util.Random();

        final String privateGroupName1 = "group nicename";
        final String privateGroupName2 = "other private group nicename";
        final String publicGroupName1 = "the public group";
        final String publicGroupName2 = "a public group";

        // no groups set for the user
        final User theUser = userRepos.save(new User("demo"));
        final UserInformation userinfo = new UserInformation(theUser, Authorities.ISC);
        Assert.assertEquals(0, groupService.getUserGroupsAsJson(userinfo).size());

        // assign user to groups: the public default group, two private ones, two public ones
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group firstPrivateGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), privateGroupName1, "Group description", false));
        final Group secondPrivateGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), privateGroupName2, "Group description", false));
        final Group firstPublicGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), publicGroupName1, "Group description", true));
        final Group secondPublicGroup = groupRepos.save(new Group(Integer.toString(rand.nextInt()), publicGroupName2, "Group description", true));

        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), firstPrivateGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), secondPrivateGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), firstPublicGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), secondPublicGroup.getId()));

        // retrieve - the default group should not be among the returned groups
        final List<JsonGroupWithDetails> userGroupsIsc = groupService.getUserGroupsAsJson(userinfo);
        Assert.assertNotNull(userGroupsIsc);
        Assert.assertEquals(4, userGroupsIsc.size());

        // verify
        // two public groups first, sorted by display name
        Assert.assertEquals(userGroupsIsc.get(0).getName(), publicGroupName2);
        Assert.assertEquals(userGroupsIsc.get(1).getName(), publicGroupName1);

        // two private groups at the end, sorted by display name
        Assert.assertEquals(userGroupsIsc.get(2).getName(), privateGroupName1);
        Assert.assertEquals(userGroupsIsc.get(3).getName(), privateGroupName2);

        // check that the default group is not contained
        Assert.assertEquals(0, userGroupsIsc.stream().filter(group -> group.getName().equals(defaultGroup.getName())).count());
    }

    /**
     * test that an exception is thrown when groups of undefined user are requested and not default group is defined
     */
    @Test(expected = NullPointerException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupsOfNullIsNull() {

        // when no user given, we expect the default group to be returned
        // if no default group is defined, there may be an exception
        groupService.getUserGroupsAsJson(null);
    }

    /**
     * test that the default group is returned if no user is specified, but default group is defined
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupsOfNullUserinfo() {

        // define default group, but don't assign any user to it
        // ask -> default group should be returned anyway
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final List<JsonGroupWithDetails> details = groupService.getUserGroupsAsJson(null);
        Assert.assertNotNull(details);
        Assert.assertEquals(1, details.size());

        // verify
        // world public group (=default group) is returned
        final JsonGroupWithDetails extractedDefaultGroup = details.get(0);
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals(defaultGroup.getDisplayName(), extractedDefaultGroup.getName());
        Assert.assertEquals(defaultGroup.getName(), extractedDefaultGroup.getId());
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());
    }

    /**
     * test that the default group is returned if no particular user is specified, but default group is defined
     */
    @Test
    @SuppressFBWarnings(value = {SpotBugsAnnotations.ExceptionIgnored,
            SpotBugsAnnotations.KnownNullValue}, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupsOfEmptyUserinfo() {

        // define default group, but don't assign any user to it
        // ask -> default group should be returned anyway
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final User nullUser = null;
        final List<JsonGroupWithDetails> details = groupService.getUserGroupsAsJson(new UserInformation(nullUser, ""));
        Assert.assertNotNull(details);
        Assert.assertEquals(1, details.size());

        // verify
        // world public group (=default group) is returned
        final JsonGroupWithDetails extractedDefaultGroup = details.get(0);
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals(defaultGroup.getDisplayName(), extractedDefaultGroup.getName());
        Assert.assertEquals(defaultGroup.getName(), extractedDefaultGroup.getId());
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());
    }

    /**
     * test that the default group is returned when no authority is specified
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupsOfUndefinedAuthority() {

        // define default group, but don't assign any user to it
        // ask -> default group should be returned anyway
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final List<JsonGroupWithDetails> details = groupService.getUserGroupsAsJson(new UserInformation(new User("me"), ""));
        Assert.assertNotNull(details);
        Assert.assertEquals(1, details.size());

        // verify
        // world public group (=default group) is returned
        final JsonGroupWithDetails extractedDefaultGroup = details.get(0);
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals(defaultGroup.getDisplayName(), extractedDefaultGroup.getName());
        Assert.assertEquals(defaultGroup.getName(), extractedDefaultGroup.getId());
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());
    }

    /**
     * test that no group are returned if user is unknown
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupsOfUnknownUser() {

        // define default group, but don't assign any user to it
        TestDbHelper.insertDefaultGroup(groupRepos);

        // check for an unknown user - empty result
        final List<JsonGroupWithDetails> details = groupService.getUserGroupsAsJson(new UserInformation(new User("unknownUser"), "someauthority"));
        Assert.assertNotNull(details);
        Assert.assertEquals(0, details.size());
    }

    /**
     * test that no group IDs are received when no user is given
     */
    @Test
    public void testGroupIdsOfNullUser() {

        // when no user is given, we expect null to be returned
        Assert.assertNull(groupService.getGroupIdsOfUser(null));
    }

    /**
     * test search for users of a group
     */
    @Test
    public void testUsersOfGroup() {

        final User theUser = userRepos.save(new User("demo"));
        final User anotherUser = userRepos.save(new User("other"));
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group anotherGroup = groupRepos.save(new Group("internalGroupId", "group nicename", "Group description", false));

        Assert.assertNull(groupService.getUserIdsOfGroup(""));
        Assert.assertEquals(0, groupService.getUserIdsOfGroup(new Group()).size()); // test with undefined group
        Assert.assertEquals(0, groupService.getUserIdsOfGroup(defaultGroup).size());
        Assert.assertEquals(0, groupService.getUserIdsOfGroup(anotherGroup).size());

        // assign users to groups now
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(theUser.getId(), anotherGroup.getId()));
        userGroupRepos.save(new UserGroup(anotherUser.getId(), defaultGroup.getId()));

        // retrieve
        final List<Long> usersOfDefaultGroup = groupService.getUserIdsOfGroup(defaultGroup);
        final List<Long> usersOfAnotherGroup = groupService.getUserIdsOfGroup(anotherGroup.getName()); // test alternative interface

        // verify assignments
        Assert.assertEquals(2, usersOfDefaultGroup.size());
        Assert.assertEquals(1, usersOfAnotherGroup.size());

        Assert.assertTrue(usersOfDefaultGroup.stream().anyMatch(userId -> userId.equals(theUser.getId())));
        Assert.assertTrue(usersOfDefaultGroup.stream().anyMatch(userId -> userId.equals(anotherUser.getId())));
        Assert.assertTrue(usersOfAnotherGroup.stream().anyMatch(userId -> userId.equals(theUser.getId())));
    }

    /**
     * test that a user's group membership search throws exceptions if required parameters are missing
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testGroupsOfUserFailure() {

        groupService.getGroupsOfUser(null);
        Assert.fail("Expected exception about invalid argument not received!");
    }

}
