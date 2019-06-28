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
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class SearchAnnotationsTest {

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

    private final static String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refr";
    private static final String AUTHORITY = Authorities.EdiT;
    private final static String LOGIN1 = "demo", LOGIN2 = "john";
    private final static String USER1 = "acct:" + LOGIN1 + "@" + AUTHORITY, USER2 = "acct:" + LOGIN2 + "@" + AUTHORITY;
    private User user2;
    private UserInformation userInfo1;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create two users of same group
        final User user1 = new User(LOGIN1);
        userRepos.save(user1);
        final Token token = new Token(user1, AUTHORITY, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now());
        tokenRepos.save(token);
        userInfo1 = new UserInformation(token);

        user2 = userRepos.save(new User(LOGIN2));
        userGroupRepos.save(new UserGroup(user1.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(user2.getId(), defaultGroup.getId()));

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
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * replies are not separated from annotations
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchAnnotationsNoSeparateReplies() throws Exception {

        final UserInformation userInfo2 = new UserInformation(user2, AUTHORITY);

        // preparation: save an annotation and a reply for it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(USER1);
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, userInfo1);
        final String idFirst = jsAnnotFirst.getId();

        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(USER2, jsAnnotFirst.getUri(), Arrays.asList(idFirst));
        jsAnnotReply = annotService.createAnnotation(jsAnnotReply, userInfo2);
        final String idReply = jsAnnotReply.getId();

        // save a second annotation
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(USER2);
        jsAnnotSecond = annotService.createAnnotation(jsAnnotSecond, userInfo2);
        final String idSecond = jsAnnotSecond.getId();

        // send search request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=false&sort=created&order=asc&uri=" + jsAnnotFirst.getUri().toString() + "&group=__world__")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        final JsonSearchResult jsResponse = SerialisationHelper.deserializeJsonSearchResult(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(2, jsResponse.getTotal()); // only number of items is counted, without the number of replies
        Assert.assertEquals(3, jsResponse.getRows().size());

        // verify that all IDs are found
        Assert.assertThat(Arrays.asList(idFirst, idReply, idSecond),
                containsInAnyOrder(jsResponse.getRows().stream().map(JsonAnnotation::getId).toArray()));
    }

    /**
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * replies are to be separated from annotations
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchAnnotationsWithSeparateReplies() throws Exception {

        final UserInformation userInfo2 = new UserInformation(user2, AUTHORITY);

        // preparation: save an annotation and a reply for it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(USER1);
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, userInfo1);
        final String idFirst = jsAnnotFirst.getId();

        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(USER2, jsAnnotFirst.getUri(), Arrays.asList(idFirst));
        jsAnnotReply = annotService.createAnnotation(jsAnnotReply, userInfo2);
        final String idReply = jsAnnotReply.getId();

        // save a second annotation
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(USER2);
        jsAnnotSecond = annotService.createAnnotation(jsAnnotSecond, userInfo2);
        final String idSecond = jsAnnotSecond.getId();

        // send search request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=true&sort=created&order=asc&uri=" + jsAnnotFirst.getUri().toString() + "&group=__world__")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        final JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(2, jsResponse.getTotal()); // only replies are counted
        Assert.assertEquals(2, jsResponse.getRows().size());
        Assert.assertEquals(1, jsResponse.getReplies().size()); // one reply

        // verify that all annotation IDs are found
        Assert.assertThat(Arrays.asList(idFirst, idSecond),
                containsInAnyOrder(jsResponse.getRows().stream().map(JsonAnnotation::getId).toArray()));

        // verify reply ID
        Assert.assertEquals(idReply, jsResponse.getReplies().get(0).getId());
    }

    /**
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * use several matching metadata sets
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchAnnotationsWithSeveralMetadataSets() throws Exception {

        final String VERSION = "version";
        final String REFERENCE = "ref";
        final String USER_ACCOUNT = "acct:demo@" + Authorities.ISC;

        // note: requesting user is an ISC user (since for EdiT users, everything is retrieved)!
        userInfo1.setAuthority(Authorities.ISC);
        userInfo1.getCurrentToken().setAuthority(Authorities.ISC);

        // manipulate the authority also in the saved token
        final Token savedToken = tokenRepos.findByAccessToken(ACCESS_TOKEN);
        savedToken.setAuthority(Authorities.ISC);
        tokenRepos.save(savedToken);

        // preparation: save an annotation and a reply for it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(USER_ACCOUNT);
        jsAnnotFirst.getDocument().getMetadata().put(VERSION, "4");
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, userInfo1);
        final String idFirst = jsAnnotFirst.getId();

        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(USER_ACCOUNT, jsAnnotFirst.getUri(), Arrays.asList(idFirst));
        jsAnnotReply = annotService.createAnnotation(jsAnnotReply, userInfo1);
        final String idReply = jsAnnotReply.getId();

        // save a second annotation having different metadata
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(USER_ACCOUNT);
        jsAnnotSecond.getDocument().getMetadata().put(VERSION, "2");
        jsAnnotSecond.getDocument().getMetadata().put(REFERENCE, "theref");
        jsAnnotSecond = annotService.createAnnotation(jsAnnotSecond, userInfo1);
        final String idSecond = jsAnnotSecond.getId();

        // save a third annotation having again different metadata
        final JsonAnnotation jsAnnotThird = TestData.getTestAnnotationObject(USER_ACCOUNT);
        jsAnnotThird.getDocument().getMetadata().put(VERSION, "8");
        jsAnnotThird.getDocument().getMetadata().put(REFERENCE, "anotherref");
        annotService.createAnnotation(jsAnnotThird, userInfo1);

        // launch four different metadata sets, only one should match
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        requestedMetadata.add(new SimpleMetadata(VERSION, "4"));
        requestedMetadata.add(new SimpleMetadata(REFERENCE, "theref"));
        requestedMetadata.add(new SimpleMetadata(VERSION, "888")); // won't match
        requestedMetadata.add(new SimpleMetadata("unknownprop", "unknownval")); // won't match

        final String serializedMetaRequest = SerialisationHelper.serialize(requestedMetadata).replace("{", "%7B").replace("}", "%7D");

        // send search request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=true&sort=created&order=asc&uri=" + jsAnnotFirst.getUri().toString() + "&group=__world__" +
                        "&metadatasets=" + serializedMetaRequest)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        final JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(2, jsResponse.getTotal()); // only non-replies are counted
        Assert.assertEquals(2, jsResponse.getRows().size());
        Assert.assertEquals(1, jsResponse.getReplies().size()); // one reply

        // verify that all annotation IDs are found
        Assert.assertThat(Arrays.asList(idFirst, idSecond),
                containsInAnyOrder(jsResponse.getRows().stream().map(JsonAnnotation::getId).toArray()));

        // verify reply ID
        Assert.assertEquals(idReply, jsResponse.getReplies().get(0).getId());
    }

    /**
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * use several different annotation statuses
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchAnnotationsWithSeveralStatuses() throws Exception {

        final String USER_ACCOUNT = "acct:demo@" + Authorities.ISC;

        // preparation: save an annotation and a reply for it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(USER_ACCOUNT);
        jsAnnotFirst.getDocument().getMetadata().remove("systemId");
        jsAnnotFirst = annotService.createAnnotation(jsAnnotFirst, userInfo1);
        final String idFirst = jsAnnotFirst.getId();

        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(USER_ACCOUNT, jsAnnotFirst.getUri(), Arrays.asList(idFirst));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, userInfo1);
        final String idFirstReply = jsAnnotFirstReply.getId();

        // save a second annotation with a reply, but delete the reply
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(USER_ACCOUNT);
        jsAnnotSecond.getDocument().getMetadata().remove("systemId");
        jsAnnotSecond = annotService.createAnnotation(jsAnnotSecond, userInfo1);
        final String idSecond = jsAnnotSecond.getId();

        JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(USER_ACCOUNT, jsAnnotSecond.getUri(), Arrays.asList(idSecond));
        jsAnnotSecondReply = annotService.createAnnotation(jsAnnotSecondReply, userInfo1);
        final String idSecondReply = jsAnnotSecondReply.getId();
        annotService.deleteAnnotationById(idSecondReply, userInfo1);

        // send search request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search?_separate_replies=true&sort=created&order=asc&uri=" + jsAnnotFirst.getUri().toString() + "&group=__world__" 
        + "&metadatasets=[%7Bstatus:[\"NORMAL\", \"DELETED\"]%7D]")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        final JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(2, jsResponse.getTotal()); // only non-replies are counted
        Assert.assertEquals(2, jsResponse.getRows().size());
        Assert.assertEquals(2, jsResponse.getReplies().size()); // both replies are returned

        // verify that all annotation IDs are found
        Assert.assertThat(Arrays.asList(idFirst, idSecond),
                containsInAnyOrder(jsResponse.getRows().stream().map(JsonAnnotation::getId).toArray()));

        // verify reply IDs
        Assert.assertThat(Arrays.asList(idFirstReply, idSecondReply),
                containsInAnyOrder(jsResponse.getReplies().stream().map(JsonAnnotation::getId).toArray()));
    }

    // call the search without any parameters - will provoke exception internally
    // expected: Http 400 and error description
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testSearchAnnotationsWithoutParameters() throws Exception {

        // send search request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search") // no parameters given -> will throw exception internally
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertThat(jsResponse.getReason(), not(isEmptyString()));
    }
}
