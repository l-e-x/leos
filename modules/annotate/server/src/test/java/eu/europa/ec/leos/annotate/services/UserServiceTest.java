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
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.UserAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;
import eu.europa.ec.leos.annotate.services.impl.GroupServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.UUIDGeneratorServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.UserServiceImpl;
import org.apache.http.auth.InvalidCredentialsException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
// it should also be possible to run these tests with:
// @ActiveProfiles("testUserService")
// the associate test configuration scripts are still available; this should demonstrate that several different test scripts can be used, if needed
public class UserServiceTest {

    /**
     * NOTE: This test class demonstrates how to use a specific profile that does not use the default scripts for creation of the database 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private UserService userService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    private static final Logger Log = LoggerFactory.getLogger(UserServiceTest.class);

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

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
            Assert.fail("Unexpected exception when creating new user: " + e);
        }

        Assert.assertNotNull(newUser);
        Assert.assertEquals(1, userRepos.count());
        Assert.assertEquals(1, groupRepos.count()); // still 1, i.e. no additional groups have been created
        Assert.assertEquals(1, userGroupRepos.count());

        // retrieve group ID
        final Group theGroup = groupRepos.findByName("__world__");

        // verify user has been added to default group
        final UserGroup userInGroup = userGroupRepos.findByUserIdAndGroupId(newUser.getId(), theGroup.getId());
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
            Assert.fail("Unexpected exception received during user creation: " + e);
        }
        Assert.assertNotNull(newUser);

        Assert.assertNotNull(userService.getUserById(newUser.getId())); // can be found by ID
        Assert.assertNull(userService.getUserById(newUser.getId() + 1)); // different ID is not found

        try {
            userService.createUser(userLogin);
            Assert.fail("Expected exception not thrown when trying to recreate existing user");
        } catch (UserAlreadyExistingException | DefaultGroupNotFoundException ex) {
            Log.info("Expected exception for duplicate user received.");
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
            Log.info("Expected exception about missing default group received");
        }

        Assert.assertNull(userRepos.findByLogin(userLogin));
    }

    /**
     * test that user creation throws an exception if required user name is missing
     * @throws DefaultGroupNotFoundException 
     * @throws UserAlreadyExistingException 
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateUser_InvalidParameters() throws Exception {

        userService.createUserIfNotExists("");
        Assert.fail("User creation should throw an error if user name is missing; did not!");
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
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test(expected = IllegalArgumentException.class)
    public void testAssignUserToGroup_InvalidParameter1() {

        userService.addUserToEntityGroup(null);
    }

    /**
     * test adding users to groups with invalid parameters 
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test(expected = IllegalArgumentException.class)
    public void testAssignUserToGroup_InvalidParameter2() {

        final UserDetails userDetails = null;

        userService.addUserToEntityGroup(new UserInformation(null, userDetails)); // should throw IllegalArgumentException
    }

    /**
     * test adding users to groups with invalid parameters 
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test(expected = IllegalArgumentException.class)
    public void testAssignUserToGroup_InvalidParameter3() {

        final UserDetails userDetails = null;

        final User dummyUser = new User("somelogin");
        userService.addUserToEntityGroup(new UserInformation(dummyUser, userDetails)); // should throw IllegalArgumentException
    }

    /**
     * test adding users to groups with invalid parameters 
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test
    public void testAssignUserToGroup_InvalidParameter4() {

        final User dummyUser = new User("login2");

        Assert.assertFalse(userService.addUserToEntityGroup(
                new UserInformation(dummyUser, new UserDetails("login2", Long.valueOf(1), "first", "last", null, "", null))));
    }

    /**
     * test creation of new group for a user and assignment of user to the group
     */
    @Test
    public void testAssignUserToGroup_createNewGroupAndAssign() {

        final String login = "alogin";
        final List<UserEntity> entities = new ArrayList<UserEntity>();
        entities.add(new UserEntity("4", "COMM.D.1", "COMM"));

        // arrange
        final UserDetails details = new UserDetails(login, Long.valueOf(4), "Sledge", "Hammer", entities, "a@c.eu", null);

        final User theUser = new User(login);
        userRepos.save(theUser);

        // only default group defined before
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify
        // new group
        Assert.assertEquals(2, groupRepos.count());
        final Group createdGroup = groupRepos.findByName(entities.get(0).getName());
        Assert.assertNotNull(createdGroup);

        // user is member of group
        Assert.assertEquals(1, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), createdGroup.getId()));
    }

    /**
     * since ANOT-85, the UD-repo may report several entities, and the user is assigned to all those entities
     * but: exactly to those, meaning that old group memberships are removed (ANOT-86) 
     * @throws DefaultGroupNotFoundException 
     * @throws UserAlreadyExistingException 
     */
    @Test
    public void testAssignAndDeleteUserFromGroups() throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        final String login = "itsme";
        final String entity = "AGRI";
        final String entity2name = entity + ".2";
        final String entity4name = entity + ".4";

        final List<UserEntity> entities = new ArrayList<UserEntity>();
        entities.add(new UserEntity("1", entity + ".1", entity));
        entities.add(new UserEntity("2", entity2name, entity));

        // arrange
        final UserDetails details = new UserDetails(login, Long.valueOf(4), "Johnny", "Cash", entities, "a@c.eu", null);
        final User theUser = userService.createUser(login);

        // only default group defined before, user is member
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(1, userGroupRepos.count());

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify
        // two new groups
        Assert.assertEquals(3, groupRepos.count());
        Group createdGroup = groupRepos.findByName(entities.get(0).getName());
        Assert.assertNotNull(createdGroup);
        createdGroup = groupRepos.findByName(entities.get(1).getName());
        Assert.assertNotNull(createdGroup);

        // now we want to call the method a second time, this time with entities 2+3+4
        // -> membership of entity group 1 should be removed
        // -> group and membership in group for group 3 should be created
        // -> membership in group 4 should be created (group exists already)

        // create group for entity 4 already
        groupRepos.save(new Group(entity4name, false));

        final List<UserEntity> newEntities = new ArrayList<UserEntity>();
        newEntities.add(new UserEntity("2", entity2name, entity));
        newEntities.add(new UserEntity("2", entity + ".3", entity));
        newEntities.add(new UserEntity("4", entity4name, entity));

        // act
        details.setEntities(newEntities);
        Assert.assertTrue(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify
        // two more new groups - five in total; the two new groups are tested
        Assert.assertEquals(5, groupRepos.count());
        Assert.assertNotNull(groupRepos.findByName(newEntities.get(1).getName()));
        Assert.assertNotNull(groupRepos.findByName(newEntities.get(2).getName()));

        // user is member of four groups now: 2+3+4 and default group
        Assert.assertEquals(4, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(newEntities.get(0).getName()).getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(newEntities.get(1).getName()).getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(newEntities.get(2).getName()).getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), defaultGroup.getId()));
    }

    /**
     * since ANOT-85, the UD-repo may report several entities, and the user is assigned to all those entities
     * here we check that not only the "entities" property is considered, but also the "allEntities" property
     */
    @Test
    public void testAssignAndDeleteUserFromGroups_AllEntities() throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        final String login = "itsme";
        final String entity = "AGRI";

        final List<UserEntity> entities = new ArrayList<UserEntity>();
        entities.add(new UserEntity("1", "AGRI.something", entity));

        final List<UserEntity> newEntities = new ArrayList<UserEntity>();
        newEntities.add(new UserEntity("1", entity, entity));
        newEntities.add(new UserEntity("2", entity + ".I", entity));
        newEntities.add(new UserEntity("4", entity + ".I.1", entity));

        // arrange
        final UserDetails details = new UserDetails(login, Long.valueOf(4), "John", "Doe", entities, "a@c.eu", null);
        details.setAllEntities(newEntities); // these are the entities that are primarily tested here
        final User theUser = userService.createUser(login);

        // only default group defined before, user is member
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(1, userGroupRepos.count());

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify
        // four new groups
        Assert.assertEquals(5, groupRepos.count()); // default group and the four new ones
        Assert.assertNotNull(groupRepos.findByName(entity + ".something"));
        Assert.assertNotNull(groupRepos.findByName(entity));
        Assert.assertNotNull(groupRepos.findByName(entity + ".I"));
        Assert.assertNotNull(groupRepos.findByName(entity + ".I.1"));

        // user is member of four groups now: four AGRI groups and default group
        Assert.assertEquals(5, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(entity + ".something").getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(entity).getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(entity + ".I").getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), groupRepos.findByName(entity + ".I.1").getId()));
    }

    /**
     * test assignment of a user to an existing group
     */
    @Test
    public void testAssignUserToGroup_groupExistsAlready() {

        final String login = "login";
        final String entityName = "AGRI.D.8";
        final List<UserEntity> entities = new ArrayList<UserEntity>();
        entities.add(new UserEntity("4", entityName, "AGRI"));

        // arrange
        final UserDetails details = new UserDetails(login, Long.valueOf(4), "John", "Doe", entities, "a@b.eu", null);

        final User theUser = new User(login);
        userRepos.save(theUser);

        final Group newGroup = new Group(entityName, true);
        groupRepos.save(newGroup);

        // groups defined before, but no memberships
        Assert.assertEquals(2, groupRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify
        Assert.assertEquals(2, groupRepos.count()); // no additional groups, existing was used

        // user is member of group
        Assert.assertEquals(1, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), newGroup.getId()));
    }

    /**
     * test assignment of a user to a new group (user's first login), and the group contains a blank
     */
    @Test
    public void testAssignUserToEntityGroup_GroupNameWithBlanks() {

        final String login = "demouser";
        final String COMP = "DG COMP";
        final String COMP2 = "DG COMP I3";
        final List<UserEntity> entities = new ArrayList<UserEntity>();
        entities.add(new UserEntity("2", COMP, COMP));
        entities.add(new UserEntity("3", COMP2, COMP));

        // arrange
        final UserDetails details = new UserDetails(login, Long.valueOf(4), "Demo", "User", entities, "demouser@LEOS", null);

        final User theUser = new User(login);
        userRepos.save(theUser);

        // act
        Assert.assertTrue(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify: two groups must have been created, without spaces in their names, but still with spaces in their display names
        // third group is the default group
        Assert.assertEquals(3, groupRepos.count());

        // group is found again, although its internal name is different!
        final Group grpComp = groupService.findGroupByName(COMP);
        Assert.assertNotNull(grpComp);
        Assert.assertEquals("DGCOMP", grpComp.getName());
        Assert.assertEquals(COMP, grpComp.getDisplayName()); // display name remains original

        // make sure it is also found using its internal name
        Assert.assertEquals(grpComp, groupService.findGroupByName("DGCOMP"));

        final Group grpComp2 = groupService.findGroupByName(COMP2);
        Assert.assertNotNull(grpComp2);
        Assert.assertEquals("DGCOMPI3", grpComp2.getName());
        Assert.assertEquals(COMP2, grpComp2.getDisplayName());

        // there must be two group memberships - this implicitly checks that groups have been identified superfluous and deleted, which was the case previously
        Assert.assertEquals(2, userGroupRepos.count());
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), grpComp.getId()));
        Assert.assertNotNull(userGroupRepos.findByUserIdAndGroupId(theUser.getId(), grpComp2.getId()));
    }

    /**
     * test assignment of a user to a group does not do anything as the user details don't contain an entity
     */
    @Test
    public void testAssignUserToGroup_noUserEntityDefined() {

        final String login = "login";

        // arrange
        final UserDetails details = new UserDetails(login, Long.valueOf(4), "John", "Doe", null, "a@b.eu", null); // entity empty

        final User theUser = new User(login);
        userRepos.save(theUser);

        // only default group defined before
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(0, userGroupRepos.count());

        // act
        Assert.assertFalse(userService.addUserToEntityGroup(new UserInformation(theUser, details)));

        // verify
        Assert.assertEquals(1, groupRepos.count()); // no additional group was created

        // user did not get any group membership
        Assert.assertEquals(0, userGroupRepos.count());
    }

    /**
     * test randomized creation of user details and thus group memberships
     */
    @Test
    @SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.CyclomaticComplexity",
            "PMD.ModifiedCyclomaticComplexity", "PMD.NPathComplexity"})
    public void testAssignUserToGroup_randomized() {

        final int numberOfUsers = 1000;
        final int numberOfEntities = 200;

        final List<User> users = new ArrayList<User>();
        final List<UserDetails> details = new ArrayList<UserDetails>();
        final List<String> entities = new ArrayList<String>();
        final List<Integer> entityStats = new ArrayList<Integer>();

        // prepare user accounts
        for (int i = 0; i < numberOfUsers; i++) {
            final User user = new User("login" + i);
            userRepos.save(user);
            users.add(user);
        }

        // generate entities - use our UUID generator for simplicity
        final UUIDGeneratorService uuidService = new UUIDGeneratorServiceImpl();
        for (int i = 0; i < numberOfEntities; i++) {
            entities.add(uuidService.generateUrlSafeUUID().replaceAll("-", ""));

            // create entry in entity statistics
            entityStats.add(Integer.valueOf(0));
        }

        final java.util.Random rand = new java.util.Random();

        // generate user details
        for (int i = 0; i < numberOfUsers; i++) {

            final UserDetails detail = new UserDetails(users.get(i).getLogin(), users.get(i).getId(), "firstname", "lastname", null, "a@b.eu", null);
            final int generatedValue = rand.nextInt(100) + 1;
            if (generatedValue < 80) { // about 80% of users should have an entity assigned in our test
                final int entityToUse = rand.nextInt(numberOfEntities);
                final UserEntity newEntity = new UserEntity(String.valueOf(i), entities.get(entityToUse), "somename");
                detail.setEntities(Arrays.asList(newEntity));

                // increase statistics counter
                entityStats.set(entityToUse, entityStats.get(entityToUse) + 1);
            }

            details.add(detail);
        }

        // now let all users to assigned to their groups
        for (int i = 0; i < numberOfUsers; i++) {

            // expected result depends on whether an entity is set
            final boolean addResult = userService.addUserToEntityGroup(new UserInformation(users.get(i), details.get(i)));
            final boolean expectedAddResult = !CollectionUtils.isEmpty(details.get(i).getEntities()) &&
                    !StringUtils.isEmpty(details.get(i).getEntities().get(0).getName());
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
                final Group groupNotToFind = groupRepos.findByName(entities.get(i));
                Assert.assertNull(groupNotToFind);
            } else {
                // the entity was assigned in the randomized procedure above
                // -> find the group in the database...
                final Group groupToFind = groupRepos.findByName(entities.get(i));

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

        Assert.assertEquals(expectedHypoUsername, userService.getHypothesisUserAccountFromUserName(userId));
    }

    /**
     * test wrapping of user name to hypothesis user account - with empty user
     */
    @Test
    public void testEmptyUsernameToHypoUserAccount() {

        // test with empty input
        Assert.assertEquals("", userService.getHypothesisUserAccountFromUserName(""));
    }

    /**
     * test wrapping of user name to hypothesis user account by using user detail information coming from the external UD-repo
     * note: user details DO contain authority information
     */
    @Test
    public void testGetHypothesisUserAccountFromUserDetailsWithAuthority() {

        final String login = "thelogin";
        final String MyAuthority = "thisismyauthori.ty";

        final User user = new User(login);
        final UserServiceWithTestFunctions myUserService = new UserServiceImpl(null);

        final UserEntity entity = new UserEntity("8", "COMM", "COMM");
        final UserDetails details = new UserDetails(login, (long) 4712, "first", "last", Arrays.asList(entity), login + "@domain.eu", null);
        myUserService.cacheUserDetails(login, details); // test function of extended interface

        Assert.assertEquals("acct:" + login + "@" + MyAuthority, myUserService.getHypothesisUserAccountFromUser(user, MyAuthority));
    }

    /**
     * test that determining hypothesis user account returns empty string when no user is supplied 
     */
    @Test
    public void testGetHypothesisUserAccountFromUser_UserNull() {

        Assert.assertEquals("", userService.getHypothesisUserAccountFromUser(null, null));
    }

    /**
     * test that determining hypothesis user account returns empty string when user is incomplete 
     */
    @Test
    public void testGetHypothesisUserAccountFromUser_UserDetailsIncomplete() {

        final String login = "thelogin";

        final User user = new User(login);
        final UserServiceWithTestFunctions myUserService = new UserServiceImpl(null);

        Assert.assertEquals("", myUserService.getHypothesisUserAccountFromUser(user, ""));
    }

    /**
     * test retrieval of hypothesis account name returns empty string when user cannot be found
     */
    @Test
    public void testGetHypothesisUserAccountFromUserId_UserUnknown() {

        // provide unknown database ID
        Assert.assertEquals("", userService.getHypothesisUserAccountFromUserId(-3, ""));
    }

    /**
     * test retrieval of hypothesis account name based on user's database ID
     */
    @Test
    public void testGetHypothesisUserAccountFromUserId() {

        final String login = "itsme";
        final String authority = "someauthority";
        final String expectedHypoUsername = "acct:" + login + "@" + authority;

        final User user = new User(login, true);
        userRepos.save(user);

        Assert.assertEquals(expectedHypoUsername, userService.getHypothesisUserAccountFromUserId(user.getId(), authority));
    }

    /**
     * test that an exception is thrown when preferences are to be updated for an unknown user
     */
    @Test(expected = UserNotFoundException.class)
    public void testUpdatePreferenceForUnknownUser() throws UserNotFoundException {

        userService.updateSidebarTutorialVisible("unknown", true);// should throw UserNotFoundException
    }

    /**
     * test that an exception is thrown when preferences are to be updated for an invalid user
     */
    @Test(expected = IllegalArgumentException.class)
    public void testUpdatePreferenceForInvalidUser() throws UserNotFoundException {

        userService.updateSidebarTutorialVisible("", true); // should throw IllegalArgumentException
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
     * test retrieval of user profile for known user of LEOS/EdiT
     */
    @Test
    public void testGetUserProfileOfKnownLeosUser() throws UserNotFoundException {

        final String login = "demo";
        final String authority = Authorities.EdiT;

        final User theUser = userRepos.save(new User(login));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        // assign user to a second group
        final Group otherGroup = new Group("otherGroup", true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(theUser.getId(), otherGroup.getId()));

        final UserInformation userInfo = new UserInformation(theUser, authority);
        final JsonUserProfile profile = userService.getUserProfile(userInfo);

        // verify
        Assert.assertNotNull(profile);
        Assert.assertTrue(profile.getUserid() != null && !profile.getUserid().isEmpty());
        Assert.assertEquals(2, profile.getGroups().size());
        Assert.assertEquals(defaultGroup.getName(), profile.getGroups().get(0).getId());
        Assert.assertEquals(otherGroup.getName(), profile.getGroups().get(1).getId());

        Assert.assertNotNull(profile.getFeatures());
        Assert.assertNotNull(profile.getFlash());
        Assert.assertNotNull(profile.getUser_info()); // user_info object is available, although its content might be null
        Assert.assertEquals(authority, profile.getAuthority());
    }

    /**
     * test retrieval of user profile for known user of ISC
     * -> should not report the default group
     */
    @Test
    public void testGetUserProfileOfKnownIscUser() throws UserNotFoundException {

        final String login = "demo";
        final String authority = Authorities.ISC;

        final User theUser = userRepos.save(new User(login));
        // user is member of the default group, but it shouldn't be reported!
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        // assign user to a second group
        final Group otherGroup = new Group("otherGroup", true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(theUser.getId(), otherGroup.getId()));

        final UserInformation userInfo = new UserInformation(theUser, authority);
        final JsonUserProfile profile = userService.getUserProfile(userInfo);

        // verify
        Assert.assertNotNull(profile);
        Assert.assertTrue(profile.getUserid() != null && !profile.getUserid().isEmpty());
        Assert.assertEquals(1, profile.getGroups().size());
        Assert.assertEquals(otherGroup.getName(), profile.getGroups().get(0).getId());

        Assert.assertNotNull(profile.getFeatures());
        Assert.assertNotNull(profile.getFlash());
        Assert.assertNotNull(profile.getUser_info()); // user_info object is available, although its content might be null
        Assert.assertEquals(authority, profile.getAuthority());
    }

    /**
     * test retrieval of user profile without specifying user -> exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetUserProfileWithoutUser() throws Exception {

        userService.getUserProfile(null); // should throw IllegalArgumentException
    }

    /**
     * test retrieval of user profile throws exception when user is not known
     */
    @Test(expected = UserNotFoundException.class)
    public void testGetUserProfileOfUnknownUser() throws UserNotFoundException {

        userService.getUserProfile(new UserInformation("unknownlogin", "someauthority")); // should throw UserNotFoundException
    }

    /**
     * test that no exception is thrown when UD-repo cannot be contacted (and display_name remains empty)
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetUserProfile_RestException() throws UserNotFoundException {

        // mock the RestTemplate and inject it into the UserService
        final RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        Mockito.when(restOperations.getForObject(Mockito.anyString(), Mockito.any(), Mockito.anyMap())).thenThrow(new RestClientException("error"));

        // mock the GroupService also to cover other branches
        final UserServiceWithTestFunctions userServiceExtended = new UserServiceImpl(restOperations);
        final GroupService grpServ = Mockito.mock(GroupServiceImpl.class);
        Mockito.when(grpServ.getGroupsOfUser(Mockito.any(User.class))).thenReturn(null);

        userServiceExtended.setGroupService(grpServ);

        final UserInformation userInfo = new UserInformation(new User("someuser"), "auth");

        final JsonUserProfile prof = userServiceExtended.getUserProfile(userInfo);
        Assert.assertNotNull(prof);// result received, no exception
        Assert.assertNull(prof.getUser_info().getDisplay_name()); // but no display name
    }

}
