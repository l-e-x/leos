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
package eu.europa.ec.leos.annotate;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
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

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class UserDetailsFromExternalRepoTest {

    /**
     * Test interaction with UD-REPO 
     */

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

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that the UD-repo is called, but content is cached subsequently 
     * (using a Mock for the REST template used for launching REST calls)
     */
    @Test
    public void testRestUdRepoCalls() {

        // mock the RestTemplate and inject it into the UserService
        RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        UserService userService = new UserServiceImpl(restOperations);

        Map<String, String> params = new HashMap<String, String>();
        params.put("userId", "login");

        // prepare Mockito to return the desired user details
        UserDetails details = new UserDetails("login", (long) 47, "Santa", "Clause", "DIGIT", "santa@clause.europa.eu", null);
        Mockito.when(restOperations.getForObject(null, UserDetails.class, params)).thenReturn(details);

        // verify that the RestTemplate was called - should return the object specified for Mockito
        UserDetails result = userService.getUserDetailsFromUserRepo("login");
        Assert.assertEquals(details, result);

        // call again - there should not be a second call for the ud-repo via the REST template (i.e. cached value is used)
        userService.getUserDetailsFromUserRepo("login");
        Mockito.verify(restOperations, Mockito.times(1)).getForObject(null, UserDetails.class, params);

        // query another user - there should be one call for ud-repo again
        Map<String, String> secondCallParams = new HashMap<String, String>();
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
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testUserDisplayNameWithUdRepo() throws UserNotFoundException {

        // mock the RestTemplate and inject it into the UserService
        RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        userService.setRestTemplate(restOperations);

        String userLogin = "login";

        User theUser = userRepos.save(new User(userLogin));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        Map<String, String> params = new HashMap<String, String>();
        params.put("userId", userLogin);

        // prepare Mockito to return the desired user details
        UserDetails details = new UserDetails(userLogin, (long) 47, "Santa", "Clause", "DIGIT", "santa@clause.europa.eu", null);
        Mockito.when(restOperations.getForObject(repositoryUrl, UserDetails.class, params)).thenReturn(details);

        // retrieve user profile and expect that display name is provided therein
        JsonUserProfile profile = userService.getUserProfile(userLogin, "authority");
        Assert.assertNotNull(profile);
        Assert.assertNotNull(profile.getUser_info());
        Assert.assertEquals("Clause Santa", profile.getUser_info().getDisplay_name());
    }

}
