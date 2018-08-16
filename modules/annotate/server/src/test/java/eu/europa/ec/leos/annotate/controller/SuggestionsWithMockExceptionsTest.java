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
import eu.europa.ec.leos.annotate.controllers.SuggestionApiController;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
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
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class SuggestionsWithMockExceptionsTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refreshtoken";

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        MockitoAnnotations.initMocks(this);

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        User theUser = new User("demo");
        userRepos.save(theUser);
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now(), REFRESH_TOKEN, LocalDateTime.now()));

        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(suggController);
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

    // the AnnotationService is mocked...
    @Mock
    private AnnotationService annotService;

    // ... but we also need a working real service - create an implementation instance directly
    @Autowired
    private AnnotationServiceImpl realUserService;

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

    @InjectMocks
    private SuggestionApiController suggController;

    private MockMvc mockMvc;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * try accepting an existing suggestion, but service throws exception
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testAcceptSuggestionWithException() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo", annotId = "id";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));

        // make the mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.when(annotService.createAnnotation(jsAnnot, login)).thenReturn(realUserService.createAnnotation(jsAnnot, login));
        Mockito.doThrow(new NullPointerException()).when(annotService).acceptSuggestionById(annotId, login);

        annotService.createAnnotation(jsAnnot, login);

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/accept")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try rejecting an existing suggestion, but service throws exception
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testRejectSuggestionWithException() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo", annotId = "id";

        // preparation: save a suggestion annotation that can be rejected later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));

        // make the mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.when(annotService.createAnnotation(jsAnnot, login)).thenReturn(realUserService.createAnnotation(jsAnnot, login));
        Mockito.doThrow(new NullPointerException()).when(annotService).rejectSuggestionById(annotId, login);

        annotService.createAnnotation(jsAnnot, login);

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/reject")
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }
}
