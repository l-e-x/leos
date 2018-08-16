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
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSuggestionAcceptSuccessResponse;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
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
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class AcceptSuggestionTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", ACCESS_TOKEN2 = "anothertoken";
    private static final String REFRESH_TOKEN = "refr", REFRESH_TOKEN2 = "refr2";

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        User theUser = new User("demo");
        userRepos.save(theUser);
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now().plusMinutes(5)));

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
    private AnnotationService annotService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private AnnotationRepository annotRepos;

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
     * successfully accept an existing suggestion, expected HTTP 200 and ID of the accepted suggestion
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testAcceptSuggestionOk() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        String id = jsAnnot.getId();

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + id + "/accept")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        JsonSuggestionAcceptSuccessResponse jsResponse = SerialisationHelper.deserializeJsonSuggestionAcceptSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(id, jsResponse.getId());
        Assert.assertTrue(jsResponse.getAccepted());

        // the annotation was deleted
        Assert.assertEquals(0, annotRepos.count());
    }

    /**
     * try accepting a non-existing suggestion, expected HTTP 404
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testAcceptSuggestionNotFound() throws Exception {

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/theid/accept")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // failure reason must have been set
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try accepting an existing annotation, which however is no suggestion
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testAcceptSuggestionNotOkIsNoSuggestion() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("highlight")); // -> no suggestion
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        String id = jsAnnot.getId();

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + id + "/accept")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // failure reason must have been set
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try accepting an existing suggestion, but permissions are insufficient
     * expected HTTP 404
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testAcceptSuggestionNoPermission() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        String id = jsAnnot.getId();

        // create another user, which however does not belong to the suggestion's group
        User secondUser = new User("secondUser");
        userRepos.save(secondUser);
        tokenRepos.save(new Token(secondUser, ACCESS_TOKEN2, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN2, LocalDateTime.now().plusMinutes(5)));

        Group secondGroup = new Group("secondGroupId", "second group", "description", true);
        groupRepos.save(secondGroup);
        userGroupRepos.save(new UserGroup(secondUser.getId(), secondGroup.getId()));
        authService.setAuthenticatedUser(secondUser);

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + id + "/accept")
                .header("authorization", "Bearer " + ACCESS_TOKEN2);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // failure reason must have been set
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }
}
