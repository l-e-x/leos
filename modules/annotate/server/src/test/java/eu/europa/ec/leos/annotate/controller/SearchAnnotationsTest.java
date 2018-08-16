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
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
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
public class SearchAnnotationsTest {

    private final static String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refr";
    private final static String USER1 = "acct:user@domain.eu", USER2 = "acct:user2@domain.net";
    private final static String LOGIN1 = "demo", LOGIN2 = "john";

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        
        // create two users of same group
        User user1 = new User(LOGIN1);
        userRepos.save(user1);
        tokenRepos.save(new Token(user1, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now()));
        
        User user2 = userRepos.save(new User(LOGIN2));
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
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * replies are not separated from annotations
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testSearchAnnotationsNoSeparateReplies() throws Exception {

        // preparation: save an annotation and a reply for it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(USER1);
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, LOGIN1);
        String idFirst = jsAnnotFirst.getId();

        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(USER2, jsAnnotFirst.getUri(), Arrays.asList(idFirst));
        jsAnnotReply = annotService.createAnnotation(jsAnnotReply, LOGIN2);
        String idReply = jsAnnotReply.getId();

        // save a second annotation
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(USER2);
        jsAnnotSecond = annotService.createAnnotation(jsAnnotSecond, LOGIN2);
        String idSecond = jsAnnotSecond.getId();

        // send search request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=false&sort=created&order=asc&uri=" + jsAnnotFirst.getUri().toString() + "&group=__world__")
                .header("authorization", "Bearer " + ACCESS_TOKEN);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        JsonSearchResult jsResponse = SerialisationHelper.deserializeJsonSearchResult(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(3, jsResponse.getTotal());
        Assert.assertEquals(3, jsResponse.getRows().size());

        // verify that all IDs are found
        for (int i = 0; i < 3; i++) {
            Assert.assertTrue(Arrays.asList(idFirst, idReply, idSecond).contains(jsResponse.getRows().get(i).getId()));
        }
    }

    /**
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * replies are to be separated from annotations
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testSearchAnnotationsWithSeparateReplies() throws Exception {

        // preparation: save an annotation and a reply for it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(USER1);
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, LOGIN1);
        String idFirst = jsAnnotFirst.getId();

        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(USER2, jsAnnotFirst.getUri(), Arrays.asList(idFirst));
        jsAnnotReply = annotService.createAnnotation(jsAnnotReply, LOGIN2);
        String idReply = jsAnnotReply.getId();

        // save a second annotation
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(USER2);
        jsAnnotSecond = annotService.createAnnotation(jsAnnotSecond, LOGIN2);
        String idSecond = jsAnnotSecond.getId();

        // send search request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=true&sort=created&order=asc&uri=" + jsAnnotFirst.getUri().toString() + "&group=__world__")
                .header("authorization", "Bearer " + ACCESS_TOKEN);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(2, jsResponse.getTotal()); // only replies are counted
        Assert.assertEquals(2, jsResponse.getRows().size());
        Assert.assertEquals(1, jsResponse.getReplies().size()); // one reply

        // verify that all annotation IDs are found
        for (int i = 0; i < 2; i++) {
            Assert.assertTrue(Arrays.asList(idFirst, idSecond).contains(jsResponse.getRows().get(i).getId()));
        }

        // verify reply ID
        Assert.assertEquals(idReply, jsResponse.getReplies().get(0).getId());
    }
}
