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
import eu.europa.ec.leos.annotate.model.web.user.JsonGroupWithDetails;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
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
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class GetGroupsTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "r";

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

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successfully retrieve the user's groups, expected HTTP 200 and the group data
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testGetGroupsOk() throws Exception {

        final String login = "demo", authority = "myauthority";

        // preparation: save a user and assign it to the default group
        User theUser = new User(login);
        userRepos.save(theUser);
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now().plusMinutes(5)));
        
        UserGroup membershipDefault = new UserGroup(theUser.getId(), defaultGroup.getId());
        userGroupRepos.save(membershipDefault);

        // create a second private group and assign user to it
        Group privateGroup = new Group("id", "private group", "description", false);
        groupRepos.save(privateGroup);
        userGroupRepos.save(new UserGroup(theUser.getId(), privateGroup.getId()));

        // send group retrieval request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/groups?authority=" + authority + "&expand=ignore&document_uri=ignore")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected groups were returned
        List<JsonGroupWithDetails> jsResponse = SerialisationHelper.deserializeJsonGroupWithDetails(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(2, jsResponse.size());

        // verify
        JsonGroupWithDetails extractedDefaultGroup = jsResponse.stream().filter(group -> group.getId().equals(TestDbHelper.DEFAULT_GROUP_INTERNALNAME))
                .findAny().get();
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());

        JsonGroupWithDetails extractedAnotherGroup = jsResponse.stream().filter(group -> group.getId().equals(privateGroup.getName())).findAny().get();
        Assert.assertNotNull(extractedAnotherGroup);
        Assert.assertEquals("private", extractedAnotherGroup.getType());
        Assert.assertFalse(extractedAnotherGroup.isPublic());
        Assert.assertFalse(extractedAnotherGroup.isScoped());
    }

    /**
     * retrieve empty list for a user not belonging to any groups, expected HTTP 200 and empty list/array
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testGetGroupsEmpty() throws Exception {

        final String login = "login", authority = "myauthority";

        // preparation: create a user, but don't assign it to any group -> we should not receive any groups
        User theUser = new User(login);
        userRepos.save(theUser);
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now()));

        // send group retrieval request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/groups?authority=" + authority + "&expand=ignore&document_uri=ignore")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        Assert.assertEquals("[]", responseString); // empty array received
    }
    
    /**
     * retrieve default group when not being authenticated, expected HTTP 200
     */
    @Test
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "Initialisation is done in function that is run before each test")
    public void testGetGroupsNotAuthenticated() throws Exception {

        final String authority = "myauthority";

        // send group retrieval request - without authorization header
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/groups?authority=" + authority + "&expand=ignore&document_uri=ignore");

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected groups were returned: default group (only)
        List<JsonGroupWithDetails> jsResponse = SerialisationHelper.deserializeJsonGroupWithDetails(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(1, jsResponse.size());

        // verify
        JsonGroupWithDetails extractedDefaultGroup = jsResponse.stream().filter(group -> group.getId().equals(TestDbHelper.DEFAULT_GROUP_INTERNALNAME))
                .findAny().get();
        Assert.assertNotNull(extractedDefaultGroup);
        Assert.assertEquals("open", extractedDefaultGroup.getType());
        Assert.assertTrue(extractedDefaultGroup.isPublic());
        Assert.assertFalse(extractedDefaultGroup.isScoped());
    }
    
    // note: the groups API should hardly run into exceptions - therefore, this scenario is tested using mock services, see respective test cases
}
