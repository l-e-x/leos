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
import eu.europa.ec.leos.annotate.controllers.AnnotationApiController;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
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
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class SearchAnnotationsWithMockTest {

    /**
     * Test search for annotations - internal search method throws an error
     */
    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refreshtoken";

    private User theUser;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        MockitoAnnotations.initMocks(this);
        TestDbHelper.cleanupRepositories(this);

        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        theUser = new User("demo");
        userRepos.save(theUser);
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(theUser, ACCESS_TOKEN, LocalDateTime.now(), REFRESH_TOKEN, LocalDateTime.now()));

        StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(annotController);
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

    @Mock
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
    private AnnotationApiController annotController;

    private MockMvc mockMvc;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * try searching for annotations, but service internally throws exception
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testSearchAnnotations_WithExceptionInternally() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));

        // make the AnnotationService mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.when(authService.getAuthenticatedUser()).thenReturn(theUser);
        Mockito.when(annotService.createAnnotation(jsAnnot, login)).thenReturn(realUserService.createAnnotation(jsAnnot, login));
        Mockito.when(annotService.searchAnnotations(Mockito.any(AnnotationSearchOptions.class), Mockito.anyString())).thenThrow(new RuntimeException());

        annotService.createAnnotation(jsAnnot, login);

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=false&sort=created&order=asc&uri=" + jsAnnot.getUri().toString() + "&url=" + jsAnnot.getUri().toString() +
                        "&group=__world__")
                .header("authorization", "Bearer " + ACCESS_TOKEN);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // error must have been set
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try searching for annotations, but service internally returns no result
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testSearchAnnotations_WithEmptyResultInternally() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));

        // make the AnnotationService mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.when(authService.getAuthenticatedUser()).thenReturn(theUser);
        Mockito.when(annotService.createAnnotation(jsAnnot, login)).thenReturn(realUserService.createAnnotation(jsAnnot, login));
        Mockito.when(annotService.searchAnnotations(Mockito.any(AnnotationSearchOptions.class), Mockito.anyString())).thenReturn(new ArrayList<Annotation>());

        annotService.createAnnotation(jsAnnot, login);

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=false&sort=created&order=asc&uri=" + jsAnnot.getUri().toString() + "&url=&group=__world__")
                .header("authorization", "Bearer " + ACCESS_TOKEN);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        // don't inspect result closer
    }

    /**
     * try searching for annotations, but service internally returns no result (2 - different way)
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testSearchAnnotations_WithEmptyResultInternally2() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save a suggestion annotation that can be accepted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));

        // make the AnnotationService mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.when(authService.getAuthenticatedUser()).thenReturn(theUser);
        Mockito.when(annotService.createAnnotation(jsAnnot, login)).thenReturn(realUserService.createAnnotation(jsAnnot, login));
        Mockito.when(annotService.searchAnnotations(Mockito.any(AnnotationSearchOptions.class), Mockito.anyString())).thenReturn(null);

        annotService.createAnnotation(jsAnnot, login);

        // send acceptance request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=false&sort=created&order=asc&uri=" + jsAnnot.getUri().toString() + "&url=&group=__world__")
                .header("authorization", "Bearer " + ACCESS_TOKEN);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        // don't inspect result closer
    }
}
