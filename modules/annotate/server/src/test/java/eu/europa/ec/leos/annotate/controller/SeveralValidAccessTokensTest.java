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
import eu.europa.ec.leos.annotate.helper.*;
import eu.europa.ec.leos.annotate.model.UserInformation;
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
public class SeveralValidAccessTokensTest {

    private final static String ACCESS_TOKEN1 = "@cce$$1", REFRESH_TOKEN1 = "refr1";
    private final static String ACCESS_TOKEN2 = "@cce$$2", REFRESH_TOKEN2 = "refr2";
    private final static String ACCESS_TOKEN_SECOND_USER = "@cce$$3", REFRESH_TOKEN_SECOND_USER = "refr3";
    private final static String LOGIN1 = "demo", LOGIN2 = "john";
    private final static String AUTHORITY1_USER1 = Authorities.EdiT, AUTHORITY2_USER1 = Authorities.EdiT, AUTHORITY_USER2 = Authorities.EdiT;
    private final static String USER1 = "acct:" + LOGIN1 + "@" + AUTHORITY1_USER1, USER2 = "acct:" + LOGIN2 + "@" + AUTHORITY_USER2;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

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

    private UserInformation userInfo1_token1, userInfo1_token2, userInfo2;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create two users of same group - first user has two valid tokens, from different authorities
        final User user1 = new User(LOGIN1);
        userRepos.save(user1);
        final Token user1token1 = new Token(user1, AUTHORITY1_USER1, ACCESS_TOKEN1, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN1, LocalDateTime.now());
        final Token user1token2 = new Token(user1, AUTHORITY2_USER1, ACCESS_TOKEN2, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN2, LocalDateTime.now());
        tokenRepos.save(user1token1);
        tokenRepos.save(user1token2);

        final User user2 = new User(LOGIN2);
        userRepos.save(user2);
        final Token user2token = new Token(user2, AUTHORITY_USER2, ACCESS_TOKEN_SECOND_USER, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN_SECOND_USER,
                LocalDateTime.now());
        tokenRepos.save(user2token);

        userGroupRepos.save(new UserGroup(user1.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(user2.getId(), defaultGroup.getId()));

        userInfo1_token1 = new UserInformation(user1token1);
        userInfo1_token2 = new UserInformation(user1token2);
        userInfo2 = new UserInformation(user2token);

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
     * successfully search for annotations using different access tokens for one of the users, expected HTTP 200 and the annotation data
     * annotations are private, so they should always be shown to the first requesting user, but not for the other
     * (this is our criteria instead of more detailed verification) 
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchAnnotationsWithSeparateReplies() throws Exception {

        // preparation: save two private annotations of first user
        JsonAnnotation jsAnnotFirst = TestData.getTestPrivateAnnotationObject(USER1);
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, userInfo1_token1);

        final JsonAnnotation jsAnnotSecond = TestData.getTestPrivateAnnotationObject(USER1);
        annotService.createAnnotation(jsAnnotSecond, userInfo1_token2);

        // save an annotation of the second user
        final JsonAnnotation jsAnnotSecondUser = TestData.getTestAnnotationObject(USER2);
        annotService.createAnnotation(jsAnnotSecondUser, userInfo2);

        final String uri = jsAnnotFirst.getUri().toString();

        // now send search request of first user - should see his two private annotations and the annotation of the second user
        executeRequest(userInfo1_token1.getCurrentToken().getAccessToken(), uri, 3);

        // second step
        // request the same using second user - should see only his annotation
        executeRequest(userInfo2.getCurrentToken().getAccessToken(), uri, 1);

        // third step
        // request again the same using first user once more, but with his second token (and thus second authority) - should see all annotations
        executeRequest(userInfo1_token2.getCurrentToken().getAccessToken(), uri, 3);

        // fourth step
        // request again the same using first user once more, but with his FIRST token again - should still be valid, and see all annotations
        executeRequest(userInfo1_token1.getCurrentToken().getAccessToken(), uri, 3);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private void executeRequest(final String token, final String uri, final int expectedNumberOfAnnotations) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=true&sort=created&order=asc&uri=" + uri + "&group=__world__")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + token);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        final JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(expectedNumberOfAnnotations, jsResponse.getRows().size());
    }

}
