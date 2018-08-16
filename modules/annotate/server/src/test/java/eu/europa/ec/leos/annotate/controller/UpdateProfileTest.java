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
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserPreferences;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.model.web.user.JsonUserShowSideBarPreference;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class UpdateProfileTest {

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        authService.setAuthenticatedUser(null);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successfully update the user profile preferences for an existing user, expected HTTP 200 and the updated profile data
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testUpdateProfileOk() throws Exception {

        final String login = "demo", accessToken = "demoAccessToken";

        // preparation: save a user and assign it to a group
        User user = new User(login, false); // pretend sidebar tutorial was not yet dismissed
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(user, accessToken, LocalDateTime.now().plusMinutes(4), "re", LocalDateTime.now()));

        JsonUserPreferences prefs = new JsonUserPreferences();
        prefs.setPreferences(new JsonUserShowSideBarPreference(false));
        String serializedPreferenceUpdate = SerialisationHelper.serialize(prefs);

        // send profile retrieval request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedPreferenceUpdate)
                .header("authorization", "Bearer " + accessToken);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned
        JsonUserProfile jsResponse = SerialisationHelper.deserializeJsonUserProfile(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(jsResponse.getPreferences().isShow_sidebar_tutorial());

        // verify user setting was updated in database
        Assert.assertTrue(userRepos.findByLogin(login).isSidebarTutorialDismissed());
    }

    /**
     * failure retrieving the user profile as user is not authenticated yet
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testUpdateProfileFailure() throws Exception {

        final String login = "demo", accessToken = "demoAccessToken";

        // preparation: save a user and assign it to a group
        User user = new User(login, false); // pretend sidebar tutorial was not yet dismissed
        // note: the access token is NOT registered for the user
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        authService.setAuthenticatedUser(null); // no user is authenticated!

        JsonUserPreferences prefs = new JsonUserPreferences();
        prefs.setPreferences(new JsonUserShowSideBarPreference(false));
        String serializedPreferenceUpdate = SerialisationHelper.serialize(prefs);

        // send profile retrieval request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedPreferenceUpdate)
                .header("authorization", "Bearer " + accessToken);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 401
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getError_description().isEmpty());
    }
}
