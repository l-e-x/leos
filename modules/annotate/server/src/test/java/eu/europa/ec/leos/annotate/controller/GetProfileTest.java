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
import eu.europa.ec.leos.annotate.model.web.user.JsonUserProfile;
import eu.europa.ec.leos.annotate.repository.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
public class GetProfileTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "re123";

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
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successfully retrieve the user profile for an existing user, expected HTTP 200 and the profile data
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testGetProfileOk() throws Exception {

        final String login = "demo";
        final String authority = "myauthority";

        // preparation: save a user and assign it to a group
        final User theUser = new User(login, true); // pretend sidebar tutorial was dismissed
        userRepos.save(theUser);
        tokenRepos.save(new Token(theUser, authority, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now()));

        final UserGroup membership = new UserGroup();
        membership.setUserId(theUser.getId());
        membership.setGroupId(defaultGroup.getId());
        userGroupRepos.save(membership);

        // send profile retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/profile?authority=" + authority)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected profile was returned
        final JsonUserProfile jsResponse = SerialisationHelper.deserializeJsonUserProfile(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(authority, jsResponse.getAuthority());
        Assert.assertEquals(1, jsResponse.getGroups().size());
        Assert.assertEquals(defaultGroup.getName(), jsResponse.getGroups().get(0).getId());
        Assert.assertFalse(jsResponse.getPreferences().isShow_sidebar_tutorial());
    }

    /**
     * failure retrieving the user profile for a non-existing user (i.e. not for the token used), expected HTTP 401 and failure response
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testGetProfileFailure() throws Exception {

        final String authority = "myauthority";

        // send profile retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/profile?authority=" + authority)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 401
        result.andExpect(MockMvcResultMatchers.status().isUnauthorized());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // failure response is received
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getAuthenticationErrorResult(), jsResponse);
        Assert.assertTrue(!jsResponse.getError().isEmpty());
        Assert.assertTrue(!jsResponse.getError_description().isEmpty());
    }
}
