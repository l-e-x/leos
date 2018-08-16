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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.UserServiceWithTestFunctions;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.UserAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;
import eu.europa.ec.leos.annotate.services.impl.UUIDGeneratorServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.UserServiceImpl;
import org.apache.http.auth.InvalidCredentialsException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")

// it should also be possible to run these tests with:
// @ActiveProfiles("testUserService")
// the associate test configuration scripts are still available; this should demonstrate that several different test scripts can be used, if needed
public class UserServiceTest {

    /**
     * NOTE: This test class demonstrates how to use a specific profile that does not use the default scripts for creation of the database 
     */

    private static Logger LOG = LoggerFactory.getLogger(UserServiceTest.class);

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test that a new user is created based on its login and assigned to the default group
     */
    @Test
    public void testCreateNewUser() {

        final String userLogin = "myuserlogin";

        // initially no users are registered; default group is available (by DB initialization script)
        Assert.assertEquals(0, userRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());
        Assert.assertEquals(1, groupRepos.count());

        User foundUser = userService.findByLogin(userLogin);
        Assert.assertNull(foundUser);

        User newUser = null;

        try {
            newUser = userService.createUser(new User(userLogin));
        } catch (UserAlreadyExistingException | DefaultGroupNotFoundException e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        Assert.assertNotNull(newUser);
        Assert.assertEquals(1, userRepos.count());
        Assert.assertEquals(1, groupRepos.count()); // still 1, i.e. no additional groups have been created
        Assert.assertEquals(1, userGroupRepos.count());

        // retrieve group ID
        Group theGroup = groupRepos.findByName("__world__");

        // verify user has been added to default group
        UserGroup userInGroup = userGroupRepos.findByUserIdAndGroupId(newUser.getId(), theGroup.getId());
        Assert.assertNotNull(userInGroup);

        // verify that user can be found using login
        foundUser = userService.findByLogin(userLogin);
        Assert.assertNotNull(foundUser);
    }

    /**
     * test that no user is registered twice (based on its login)
     */
    @Test
    public void testDontCreateDuplicateUser() {

        final String userLogin = "theuserlogin";

        // initially no users are registered; default group is available (by test initialization)
        Assert.assertEquals(0, userRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());
        Assert.assertEquals(1, groupRepos.count());

        User newUser = null;

        try {
            newUser = userService.createUser(userLogin);
        } catch (UserAlreadyExistingException | DefaultGroupNotFoundException e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(newUser);

        try {
            userService.createUser(userLogin);
            Assert.fail("Expected exception not thrown when trying to recreate existing user");
        } catch (UserAlreadyExistingException | DefaultGroupNotFoundException ex) {
            LOG.info("Expected exception for duplicate user received.");
        }

        // user was not created, but exception was thrown instead due to duplicate user
    }

    /**
     * tests that no new user can be registered if default group is not configured in database 
     */
    @Test
    public void testDontCreateUserWithoutDefaultGroup() {

        final String userLogin = "theuserlogin";

        // remove the default group initialized by test setup
        groupRepos.deleteAll();

        try {
            userService.createUser(new User(userLogin));
            Assert.fail("Expected exception about missing default user group not received!");
        } catch (UserAlreadyExistingException e) {
            Assert.fail("Unexpected exception received: " + e);
        } catch (DefaultGroupNotFoundException e) {
            LOG.info("Expected exception about missing default group received");
        }

        Assert.assertNull(userRepos.findByLogin(userLogin));
    }

    /**
     * test that user creation throws an exception if required user name is missing
     */
    @Test
    public void testCreateUser_InvalidParameters() {
        
        try {
            userService.createUserIfNotExists("");
            Assert.fail("User creation should throw an error if user name is missing; did not!");
        } catch (IllegalArgumentException ile) {
            // OK
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }
    
    /**
     * test that a user is created if not existing, 
     * but don't throw exception or do anything if already existing
     */
    @Test
    public void testCreateUserIfNotExists() {

        final String userLogin = "theuserlogin";

        User theUser = null;

        Assert.assertEquals(0, userRepos.count());

        // let the user be created
        try {
            theUser = userService.createUserIfNotExists(userLogin);
            Assert.assertNotNull(theUser);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertEquals(1, userRepos.count()); // user exists now

        // call the creation a second time - should also return existing user without exception
        try {
            theUser = userService.createUserIfNotExists(userLogin);
            Assert.assertNotNull(theUser);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertEquals(1, userRepos.count()); // user still exists, but no further users
    }

    /**
     * test adding users to groups with invalid parameters 
     */
    @Test
    public void testAssignUserToGroup_InvalidParameters() {

        try {
            userService.addUserToEntityGroup(null, null);
            Assert.fail("method should fail for invalid arguments - it did not");
        } catch (IllegalArgumentException iae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        User dummyUser = new User("login");
        try {
            userService.addUserToEntityGroup(dummyUser, null);
            Assert.fail("method should fail for invalid arguments - it did not");
        } catch (IllegalArgumentException iae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

        Assert.assertFalse(userService.addUserToEntityGroup(dummyUser, new UserDetails("login", Long.valueOf(1), "first", "last", "", "", null)));
    }

    /**
     * test creation of new group for a user and assignment of user to the group
     */
    @Test
    public void testAssignUserToGroup_createNewGroupAndAssign() {

        final String login = "login", entity = "AGRI";

        // arrange
        UserDetails details = new UserDetails(login, Long.valueOf(4), "John", "Doe", entity, "a@b.eu", null);

        User theUser = new User(login);
        userRepos.save(theUser);

        // only default group defined before
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(theUser, details));

        // verify
        // new group
        Assert.assertEquals(2, groupRepos.count());
        Group createdGroup = groupRepos.findByName(entity);
        Assert.assertNotNull(createdGroup);

        // user is member of group
        Assert.assertEquals(1, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), createdGroup.getId()));
    }

    /**
     * test assignment of a user to an existing group
     */
    @Test
    public void testAssignUserToGroup_groupExistsAlready() {

        final String login = "login", entity = "AGRI";

        // arrange
        UserDetails details = new UserDetails(login, Long.valueOf(4), "John", "Doe", entity, "a@b.eu", null);

        User theUser = new User(login);
        userRepos.save(theUser);

        Group newGroup = new Group(entity, true);
        groupRepos.save(newGroup);

        // groups defined before, but no memberships
        Assert.assertEquals(2, groupRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(theUser, details));

        // verify
        Assert.assertEquals(2, groupRepos.count()); // no additional groups, existing was used

        // user is member of group
        Assert.assertEquals(1, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), newGroup.getId()));
    }

    /**
     * test assignment of a user to a group does not do anything as the user details don't contain an entity
     */
    @Test
    public void testAssignUserToGroup_noUserEntityDefined() {

        final String login = "login";

        // arrange
        UserDetails details = new UserDetails(login, Long.valueOf(4), "John", "Doe", "", "a@b.eu", null); // entity empty

        User theUser = new User(login);
        userRepos.save(theUser);

        // only default group defined before
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());

        // act
        Assert.assertFalse(userService.addUserToEntityGroup(theUser, details));

        // verify
        Assert.assertEquals(1, groupRepos.count()); // no additional group was created

        // user did not get any group membership
        Assert.assertEquals(0, userGroupRepos.count());
    }

    /**
     * test randomized creation of user details and thus group memberships
     */
    @Test
    public void testAssignUserToGroup_randomized() {

        final int numberOfUsers = 1000, numberOfEntities = 200;

        List<User> users = new ArrayList<User>();
        List<UserDetails> details = new ArrayList<UserDetails>();
        List<String> entities = new ArrayList<String>();
        List<Integer> entityStats = new ArrayList<Integer>();

        // prepare user accounts
        for (int i = 0; i < numberOfUsers; i++) {
            User user = new User("login" + String.valueOf(i));
            userRepos.save(user);
            users.add(user);
        }

        // generate entities - use our UUID generator for simplicity
        UUIDGeneratorService uuidService = new UUIDGeneratorServiceImpl();
        for (int i = 0; i < numberOfEntities; i++) {
            entities.add(uuidService.generateUrlSafeUUID().replaceAll("-", ""));

            // create entry in entity statistics
            entityStats.add(Integer.valueOf(0));
        }

        java.util.Random r = new java.util.Random();

        // generate user details
        for (int i = 0; i < numberOfUsers; i++) {

            UserDetails detail = new UserDetails(users.get(i).getLogin(), users.get(i).getId(), "firstname", "lastname", "", "a@b.eu", null);
            int generatedValue = r.nextInt(100) + 1;
            if (generatedValue < 80) { // about 80% of users should have an entity assigned in our test
                int entityToUse = r.nextInt(numberOfEntities);
                detail.setEntity(entities.get(entityToUse));

                // increase statistics counter
                entityStats.set(entityToUse, entityStats.get(entityToUse) + 1);
            }

            details.add(detail);
        }

        // now let all users to assigned to their groups
        for (int i = 0; i < numberOfUsers; i++) {

            // expected result depends on whether an entity is set
            boolean addResult = userService.addUserToEntityGroup(users.get(i), details.get(i));
            boolean expectedAddResult = !StringUtils.isEmpty(details.get(i).getEntity());
            Assert.assertEquals(expectedAddResult, addResult);
        }

        // verify that all groups have been created: default group + new entity groups
        int entityGroupsUsed = 1; // note: the number of actually used entities could be less than the ones defined - depends on randomness...
        for (int i = 0; i < numberOfEntities; i++) {
            if (entityStats.get(i) > 0) {
                entityGroupsUsed++;
            }
        }

        // check that number of groups is correct
        Assert.assertEquals(entityGroupsUsed, groupRepos.count());

        // check that total number of group memberships is correct
        for (int i = 0; i < numberOfEntities; i++) {

            if (entityStats.get(i) == 0) {

                // the entity was not used - thus there shouldn't be a corresponding group in the database
                Group groupNotToFind = groupRepos.findByName(entities.get(i));
                Assert.assertNull(groupNotToFind);
            } else {
                // the entity was assigned in the randomized procedure above
                // -> find the group in the database...
                Group groupToFind = groupRepos.findByName(entities.get(i));

                // ... and count the number of users assigned to it - must correspond to the statistics
                Assert.assertEquals(entityStats.get(i).intValue(), userGroupRepos.findByGroupId(groupToFind.getId()).size());
            }
        }
    }

    /**
     * tests extraction of user account from hypothesis-wrapped user account
     */
    @Test
    public void testHypothesisAccountToUsername() {

        final String hypoUsername = "acct:user@domain.eu";
        final String expectedUsername = "user@domain.eu";

        Assert.assertEquals(expectedUsername, userService.getUserIdFromHypothesisUserAccount(hypoUsername));
    }

    /**
     * tests extraction of user account from non-hypothesis-wrapped user account
     */
    @Test
    public void testHypothesisAccountToUsername_InvalidHypothesisAccount() {

        // test with string not matching default hypothes.is format
        final String hypoUsername = "theacct:user@domain.eu";
        Assert.assertNull(userService.getUserIdFromHypothesisUserAccount(hypoUsername));

        // test with empty string
        Assert.assertNull(userService.getUserIdFromHypothesisUserAccount(""));
    }

    /**
     * test wrapping of user name to hypothesis user account
     */
    @Test
    public void testUsernameToHypoUserAccount() {

        final String userId = "theuserId12";
        final String expectedHypoUsername = "acct:" + userId;

        Assert.assertEquals(expectedHypoUsername, userService.getHypothesisUserAccountFromUserId(userId));

        // test with empty input
        Assert.assertEquals("", userService.getHypothesisUserAccountFromUserId(""));
    }

    /**
     * test wrapping of user name to hypothesis user account by using user detail information coming from the external UD repo
     * note: user details do not contain authority information
     */
    @Test
    public void testGetHypothesisUserAccountFromUserDetailsWithoutAuthority() {

        final String login = "thelogin";
        final String DefaultAuthority = "defaultAuth";

        User user = new User(login);
        UserServiceWithTestFunctions myUserService = new UserServiceImpl(null);
        myUserService.setDefaultAuthority(DefaultAuthority);

        UserDetails details = new UserDetails(login, (long) 4712, "first", "last", "COMM", login + "@domain.eu", null);
        myUserService.cacheUserDetails(login, details); // test function of extended interface

        Assert.assertEquals("acct:" + login + "@" + DefaultAuthority, myUserService.getHypothesisUserAccountFromUser(user));
    }

    /**
     * test wrapping of user name to hypothesis user account by using user detail information coming from the external UD repo
     * note: user details DO contain authority information
     */
    @Test
    public void testGetHypothesisUserAccountFromUserDetailsWithAuthority() {

        final String login = "thelogin";
        final String MyAuthority = "thisismyauthori.ty";

        User user = new User(login);
        UserServiceWithTestFunctions myUserService = new UserServiceImpl(null);

        UserDetails details = new UserDetails(login, (long) 4712, "first", "last", "COMM", login + "@domain.eu", null);
        details.setAuthority(MyAuthority);
        myUserService.cacheUserDetails(login, details); // test function of extended interface

        Assert.assertEquals("acct:" + login + "@" + MyAuthority, myUserService.getHypothesisUserAccountFromUser(user));
    }

    /**
     * test that determining hypothesis user account returns empty string when no user is supplied 
     */
    @Test
    public void testGetHypothesisUserAccountFromUser_UserNull() {

        Assert.assertEquals("", userService.getHypothesisUserAccountFromUser(null));
    }

    /**
     * test that determining hypothesis user account returns empty string when user is incomplete 
     */
    @Test
    public void testGetHypothesisUserAccountFromUser_UserDetailsIncomplete() {

        final String login = "thelogin";

        User user = new User(login);
        UserServiceWithTestFunctions myUserService = new UserServiceImpl(null);

        UserDetails details = new UserDetails("", (long) 4712, "first", "last", "COMM", login + "@domain.eu", null); // login left empty!
        details.setAuthority(""); // authority left empty!
        myUserService.cacheUserDetails(login, details); // test function of extended interface for filling cache

        Assert.assertEquals("", myUserService.getHypothesisUserAccountFromUser(user));
    }

    /**
     * test that an exception is thrown when preferences are to be updated for an unknown user
     */
    @Test
    public void testUpdatePreferenceForUnknownUser() {

        try {
            userService.updateSidebarTutorialVisible("unknown", true);
            Assert.fail("Expected exception not received!");
        } catch (UserNotFoundException e) {
            // OK, expected
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e);
        }
    }

    /**
     * test that an exception is thrown when preferences are to be updated for an invalid user
     */
    @Test
    public void testUpdatePreferenceForInvalidUser() {

        try {
            userService.updateSidebarTutorialVisible("", true);
            Assert.fail("Expected exception not received!");
        } catch (IllegalArgumentException e) {
            // OK, expected
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e);
        }
    }
    
    /**
     * test that user preferences can be properly saved
     */
    @Test
    public void testUpdatePreferenceForUser()
            throws InvalidCredentialsException, UserAlreadyExistingException, DefaultGroupNotFoundException, UserNotFoundException {

        // register a user
        final String userLogin = "mylogin";
        userService.createUser(userLogin);

        // and update its preference and verify
        userService.updateSidebarTutorialVisible(userLogin, false);

        User readUser = userRepos.findByLogin(userLogin);
        Assert.assertTrue(readUser.isSidebarTutorialDismissed());

        // update the other way and verify again
        userService.updateSidebarTutorialVisible(userLogin, true);

        readUser = userRepos.findByLogin(userLogin);
        Assert.assertFalse(readUser.isSidebarTutorialDismissed());
    }

    /**
     * test retrieval of user profile for known user
     */
    @Test
    public void testGetUserProfileOfKnownUser() throws UserNotFoundException {

        final String login = "demo";

        User theUser = userRepos.save(new User(login));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        JsonUserProfile profile = userService.getUserProfile(login, "authority");

        // verify
        Assert.assertNotNull(profile);
        Assert.assertTrue(profile.getUserid() != null && !profile.getUserid().isEmpty());
        Assert.assertEquals(1, profile.getGroups().size());
        Assert.assertEquals(defaultGroup.getName(), profile.getGroups().get(0).getId());

        Assert.assertNotNull(profile.getFeatures());
        Assert.assertNotNull(profile.getFlash());
        Assert.assertNotNull(profile.getUser_info()); // user_info object is available, although its content might be null
    }

    /**
     * test retrieval of user profile throws exception when user is not known
     */
    @Test
    public void testGetUserProfileOfUnknownUser() {

        try {
            userService.getUserProfile("unknownlogin", "authority");
            Assert.fail("Expected exception about unknown user not received!");
        } catch (UserNotFoundException unfe) {
            // OK
        } catch (Exception e) {
            Assert.fail("Unexpected exception received!");
        }
    }
}
