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
import eu.europa.ec.leos.annotate.controllers.AnnotationApiController;
import eu.europa.ec.leos.annotate.helper.*;
import eu.europa.ec.leos.annotate.model.UserInformation;
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
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
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

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class DeleteAnnotationWithMockTest {

    /**
     * Test search for annotations - internal search method throws an error
     */
    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refreshtoken";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // the AnnotationService is mocked...
    @Mock
    private AnnotationService annotService;

    // ... but we also need a working real service - create an implementation instance directly
    @Autowired
    private AnnotationServiceImpl realAnnotService;

    @Mock
    private AuthenticatedUserStore authUser;

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

    private Token userToken;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        MockitoAnnotations.initMocks(this);
        TestDbHelper.cleanupRepositories(this);

        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        final User theUser = new User("demo");
        userRepos.save(theUser);
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userToken = new Token(theUser, "auth", ACCESS_TOKEN, LocalDateTime.now(), REFRESH_TOKEN, LocalDateTime.now());
        tokenRepos.save(userToken);

        final StandaloneMockMvcBuilder builder = MockMvcBuilders.standaloneSetup(annotController);
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
     * try deleting an annotation, but authentication service internally throws exception
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testDeleteAnnotations_WithExceptionInternally() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu";

        // preparation: save a suggestion annotation that can be accepted later on
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);

        final UserInformation userInfo = new UserInformation(userToken);

        // make the AnnotationService mock simply throw any exception - but work correctly during creation by using a real AnnotationService
        Mockito.when(authUser.getUserInfo()).thenReturn(userInfo);
        Mockito.doThrow(new RuntimeException()).when(annotService).deleteAnnotationById(Mockito.anyString(), Mockito.any(UserInformation.class));

        realAnnotService.createAnnotation(jsAnnot, userInfo);

        // send acceptance request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .delete("/api/annotations/" + jsAnnot.getId())
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isInternalServerError());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // error must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

}
