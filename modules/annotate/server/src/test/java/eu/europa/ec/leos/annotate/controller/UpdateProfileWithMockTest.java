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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.controllers.UserApiController;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserPreferences;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserShowSideBarPreference;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.exceptions.UserNotFoundException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class UpdateProfileWithMockTest {
    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "veryRefreshing";

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        MockitoAnnotations.initMocks(this);

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(contr);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private TokenRepository tokenRepos;

    // note: we operate on the UserApiController directly as it allows mocking easier than the WebApplicationContext
    @InjectMocks
    private UserApiController contr; // UserApiController will receive a mocked UserService

    private MockMvc mockMvc;

    // we are mocking the user and authentication services to achieve desired behavior
    @Mock
    private UserService userService;

    @Mock
    private AuthenticationService authService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * failure updating the user profile for a user, expected HTTP 404 and failure response
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testUpdateProfileFailureUserNotFound() throws Exception {

        final String login = "demo";

        // preparation: save a user and assign it to a group
        User theUser = new User(login);
        userRepos.save(theUser);
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now(), REFRESH_TOKEN, LocalDateTime.now()));

        UserGroup membership = new UserGroup();
        membership.setUserId(theUser.getId());
        membership.setGroupId(defaultGroup.getId());
        userGroupRepos.save(membership);

        // mock configuration:
        // - pretend the user was authenticated, but...
        // - ... when looking him up during profile retrieval, there is a failure anyway
        Mockito.when(authService.getAuthenticatedUser()).thenReturn(theUser);
        Mockito.when(userService.updateSidebarTutorialVisible(Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new UserNotFoundException(login));

        JsonUserPreferences prefs = new JsonUserPreferences();
        prefs.setPreferences(new JsonUserShowSideBarPreference(false));
        String serializedPreferenceUpdate = SerialisationHelper.serialize(prefs);

        // send profile retrieval request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedPreferenceUpdate)
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // failure response is received
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * (internal) failure updating the user profile for a user, expected HTTP 404 and failure response
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testUpdateProfileFailure_UserServiceFailure() throws Exception {

        final String login = "demo";

        // preparation: save a user and assign it to a group
        User theUser = new User(login);
        userRepos.save(theUser);
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(2), REFRESH_TOKEN, LocalDateTime.now()));

        UserGroup membership = new UserGroup();
        membership.setUserId(theUser.getId());
        membership.setGroupId(defaultGroup.getId());
        userGroupRepos.save(membership);

        // mock configuration:
        // - pretend the user was authenticated, but...
        // - ... when looking him up during profile retrieval, there is a failure anyway
        Mockito.when(authService.getAuthenticatedUser()).thenReturn(theUser);
        Mockito.when(userService.updateSidebarTutorialVisible(Mockito.anyString(), Mockito.anyBoolean())).thenThrow(new RuntimeException("some exception"));

        JsonUserPreferences prefs = new JsonUserPreferences();
        prefs.setPreferences(new JsonUserShowSideBarPreference(false));
        String serializedPreferenceUpdate = SerialisationHelper.serialize(prefs);

        // send profile retrieval request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedPreferenceUpdate)
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // failure response is received
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }
}
