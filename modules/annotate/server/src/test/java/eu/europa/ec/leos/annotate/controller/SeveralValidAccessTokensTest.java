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
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
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

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class SeveralValidAccessTokensTest {

    private final static String ACCESS_TOKEN1 = "@cce$$1", REFRESH_TOKEN1 = "refr1";
    private final static String ACCESS_TOKEN2 = "@cce$$2", REFRESH_TOKEN2 = "refr2";
    private final static String ACCESS_TOKEN_SECOND_USER = "@cce$$3", REFRESH_TOKEN_SECOND_USER = "refr3";
    private final static String LOGIN1 = "demo", LOGIN2 = "john";
    private final static String USER1 = "acct:" + LOGIN1 + "@domain.eu", USER2 = "acct:" + LOGIN2 + "@domain.net";

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create two users of same group - first user has two valid token
        User user1 = new User(LOGIN1);
        userRepos.save(user1);
        tokenRepos.save(new Token(user1, ACCESS_TOKEN1, LocalDateTime.now().plusMinutes(1), REFRESH_TOKEN1, LocalDateTime.now()));
        tokenRepos.save(new Token(user1, ACCESS_TOKEN2, LocalDateTime.now().plusMinutes(1), REFRESH_TOKEN2, LocalDateTime.now()));

        User user2 = userRepos.save(new User(LOGIN2));
        userRepos.save(user2);
        tokenRepos.save(new Token(user2, ACCESS_TOKEN_SECOND_USER, LocalDateTime.now().plusMinutes(1), REFRESH_TOKEN_SECOND_USER, LocalDateTime.now()));

        userGroupRepos.save(new UserGroup(user1.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(user2.getId(), defaultGroup.getId()));

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
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successfully search for annotations using different access tokens for one of the users, expected HTTP 200 and the annotation data
     * annotations are private, so they should always be shown to the first requesting user, but not for the other
     * (this is our criteria instead of more detailed verification) 
     */
    @Test
    public void testSearchAnnotationsWithSeparateReplies() throws Exception {

        // preparation: save two private annotations of first suer
        JsonAnnotation jsAnnotFirst = TestData.getTestPrivateAnnotationObject(USER1);
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, LOGIN1);

        JsonAnnotation jsAnnotSecond = TestData.getTestPrivateAnnotationObject(USER1);
        annotService.createAnnotation(jsAnnotSecond, LOGIN1);

        // save an annotation of the second user
        JsonAnnotation jsAnnotSecondUser = TestData.getTestAnnotationObject(USER2);
        annotService.createAnnotation(jsAnnotSecondUser, LOGIN2);

        // now send search request of first user - should see his two private annotations and the annotation of the second user
        executeRequest(ACCESS_TOKEN1, jsAnnotFirst.getUri().toString(), 3);

        // second step
        // request the same using second user - should see only his annotation
        executeRequest(ACCESS_TOKEN_SECOND_USER, jsAnnotFirst.getUri().toString(), 1);

        // third step
        // request again the same using first user once more, but with his second token - should see all annotations
        executeRequest(ACCESS_TOKEN2, jsAnnotFirst.getUri().toString(), 3);

        // fourth step
        // request again the same using first user once more, but with his FIRST token again - should still be valid, and see all annotations
        executeRequest(ACCESS_TOKEN2, jsAnnotFirst.getUri().toString(), 3);
    }

    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    private void executeRequest(String token, String uri, int expectedNumberOfAnnotations) throws Exception {

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=true&sort=created&order=asc&uri=" + uri + "&group=__world__")
                .header("authorization", "Bearer " + token);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(expectedNumberOfAnnotations, jsResponse.getRows().size());
    }

}
