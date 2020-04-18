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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
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
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
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
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class UpdateProfileTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AuthenticatedUserStore authUser;

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

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
        authUser.clear();
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successfully update the user profile preferences for an existing user, expected HTTP 200 and the updated profile data
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testUpdateProfileOk() throws Exception {

        final String login = "demo";
        final String accessToken = "demoAccessToken";

        // preparation: save a user and assign it to a group
        final User user = new User(login, false); // pretend sidebar tutorial was not yet dismissed
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(user, "auth", accessToken, LocalDateTime.now().plusMinutes(4), "re", LocalDateTime.now()));

        final JsonUserPreferences prefs = new JsonUserPreferences();
        prefs.setPreferences(new JsonUserShowSideBarPreference(false));
        final String serializedPrefUpdate = SerialisationHelper.serialize(prefs);

        // send profile retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedPrefUpdate)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + accessToken);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned
        final JsonUserProfile jsResponse = SerialisationHelper.deserializeJsonUserProfile(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(jsResponse.getPreferences().isShow_sidebar_tutorial());

        // verify user setting was updated in database
        Assert.assertTrue(userRepos.findByLogin(login).isSidebarTutorialDismissed());
    }

    /**
     * failure retrieving the user profile as user is not authenticated yet
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testUpdateProfileFailure() throws Exception {

        final String login = "demo";
        final String accessToken = "demoAccessToken";

        // preparation: save a user and assign it to a group
        final User user = new User(login, false); // pretend sidebar tutorial was not yet dismissed
        // note: the access token is NOT registered for the user
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        authUser.clear(); // no user is authenticated!

        final JsonUserPreferences prefs = new JsonUserPreferences();
        prefs.setPreferences(new JsonUserShowSideBarPreference(false));
        final String serializedPrefUpdate = SerialisationHelper.serialize(prefs);

        // send profile retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedPrefUpdate)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + accessToken);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 401
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getError_description().isEmpty());
    }
}
