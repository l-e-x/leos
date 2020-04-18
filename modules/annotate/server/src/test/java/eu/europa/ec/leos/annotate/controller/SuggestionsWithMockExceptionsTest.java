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
import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.controllers.SuggestionApiController;
import eu.europa.ec.leos.annotate.helper.*;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationService;
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
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class SuggestionsWithMockExceptionsTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // the AnnotationService is mocked...
    @Mock
    private AnnotationService annotService;

    // ... but we also need a working real service - create an implementation instance directly
    @Autowired
    private AnnotationServiceImpl realAnnotService;

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

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refreshtoken";
    private static final String LOGIN = "demo";
    private User user;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        MockitoAnnotations.initMocks(this);

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        user = new User(LOGIN);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(user, "auth", ACCESS_TOKEN, LocalDateTime.now(), REFRESH_TOKEN, LocalDateTime.now()));

        final StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(suggController);
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
     * try accepting an existing suggestion, but service throws exception
     * expected HTTP 500
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings({"PMD.AvoidThrowingNullPointerException"}) // required for test purpose
    @Test
    public void testAcceptSuggestionWithException() throws Exception {

        final String annotId = "id";
        final String hypothesisUserAccount = "acct:user@domain.eu";
        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);
        userInfo.setUser(user);

        // preparation: save a suggestion annotation that can be accepted later on
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_SUGGESTION));

        // make the mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.doThrow(new NullPointerException()).when(annotService).acceptSuggestionById(annotId, userInfo);

        realAnnotService.createAnnotation(jsAnnot, userInfo);

        // send acceptance request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/accept")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isInternalServerError());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try rejecting an existing suggestion, but service throws exception
     * expected HTTP 500
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings({"PMD.AvoidThrowingNullPointerException"}) // required for test purpose
    @Test
    public void testRejectSuggestionWithException() throws Exception {

        final String annotId = "id";
        final String hypothesisUserAccount = "acct:user@domain.eu";
        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);
        userInfo.setUser(user);

        // preparation: save a suggestion annotation that can be rejected later on
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_SUGGESTION));

        // make the mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.doThrow(new NullPointerException()).when(annotService).rejectSuggestionById(annotId, userInfo);

        realAnnotService.createAnnotation(jsAnnot, userInfo);

        // send acceptance request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/reject")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isInternalServerError());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }
}
