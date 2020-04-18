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
package eu.europa.ec.leos.annotate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.UserServiceWithTestFunctions;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;
import eu.europa.ec.leos.annotate.services.impl.UserServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class UserDetailsFromExternalRepoTest {

    /**
     * Test interaction with UD-REPO 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserServiceWithTestFunctions userService; // use implementation of UserService in order to be able to set the RestTemplate

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private GroupRepository groupRepos;

    // URL to the external user repository
    @Value("${user.repository.url}")
    private String repositoryUrl;

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
     * Test that the UD-repo is called, but content is cached subsequently 
     * (using a Mock for the REST template used for launching REST calls)
     */
    @Test
    public void testRestUdRepoCalls() {

        final String login = "login";
        final String orga = "DIGIT";

        // mock the RestTemplate and inject it into the UserService
        final RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        final UserService userService = new UserServiceImpl(restOperations);

        final Map<String, String> params = new ConcurrentHashMap<String, String>();
        params.put("userId", login);

        // prepare Mockito to return the desired user details
        final List<UserEntity> entitiesDigit = Arrays.asList(new UserEntity("2", orga, orga));
        final UserDetails details = new UserDetails(login, (long) 47, "Santa", "Clause", entitiesDigit, "santa@clause.europa.eu", null);
        final UserEntity[] entities = new UserEntity[3];
        entities[0] = new UserEntity("1", orga + ".B.2", orga);
        entities[1] = new UserEntity("2", orga + ".B", orga);
        entities[2] = new UserEntity("2", orga, orga);
        Mockito.when(restOperations.getForObject(null, UserDetails.class, params)).thenReturn(details);
        Mockito.when(restOperations.getForObject(null, UserEntity[].class, params)).thenReturn(entities);

        // verify that the RestTemplate was called - should return the objects specified for Mockito
        UserDetails result = userService.getUserDetailsFromUserRepo(login);
        Assert.assertEquals(details, result);
        Assert.assertEquals(entities.length, result.getAllEntities().size());

        // call again - there should not be a second call for the ud-repo via the REST template (i.e. cached value is used)
        userService.getUserDetailsFromUserRepo(login);
        Mockito.verify(restOperations, Mockito.times(1)).getForObject(null, UserDetails.class, params);
        Mockito.verify(restOperations, Mockito.times(1)).getForObject(null, UserEntity[].class, params);

        // query another user - there should be one call for ud-repo again
        final Map<String, String> secondCallParams = new ConcurrentHashMap<String, String>();
        secondCallParams.put("userId", "anotheruser");

        result = userService.getUserDetailsFromUserRepo("anotheruser");
        Assert.assertNull(result);
        Mockito.verify(restOperations, Mockito.times(1)).getForObject(null, UserDetails.class, secondCallParams);
    }

    /**
     * Test retrieving user data from UD repo and return a display name for the user
     * when calling the user profile
     * (test uses a mock for the REST template used for launching REST calls)
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testUserDisplayNameWithUdRepo() throws UserNotFoundException {

        // mock the RestTemplate and inject it into the UserService
        final RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        userService.setRestTemplate(restOperations);

        final String userLogin = "login";
        final String userEntityName = "DIGIT";
        final List<UserEntity> entities = Arrays.asList(new UserEntity("2", userEntityName, userEntityName));

        final User theUser = userRepos.save(new User(userLogin));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        final Map<String, String> params = new ConcurrentHashMap<String, String>();
        params.put("userId", userLogin);

        // prepare Mockito to return the desired user details
        final UserDetails details = new UserDetails(userLogin, (long) 47, "Santa", "Clause", entities, "santa@clause.europa.eu", null);
        Mockito.when(restOperations.getForObject(repositoryUrl, UserDetails.class, params)).thenReturn(details);

        // retrieve user profile and expect that display name is provided therein
        final JsonUserProfile profile = userService.getUserProfile(new UserInformation(theUser, "authority"));
        Assert.assertNotNull(profile);
        Assert.assertNotNull(profile.getUser_info());
        Assert.assertEquals("Clause Santa", profile.getUser_info().getDisplay_name());
        Assert.assertEquals(userEntityName, profile.getUser_info().getEntity_name());
    }

}
