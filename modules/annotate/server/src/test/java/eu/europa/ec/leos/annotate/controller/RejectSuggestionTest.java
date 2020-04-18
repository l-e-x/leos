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
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSuggestionRejectSuccessResponse;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class RejectSuggestionTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", ACCESS_TOKEN2 = "anothertoken";
    private static final String REFRESH_TOKEN = "refr", REFRESH_TOKEN2 = "refr2";
    private User user;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

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
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        user = new User("demo");
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(user, "auth", ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now()));

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
     * successfully reject an existing suggestion, expected HTTP 200 and ID of the rejected suggestion
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testRejectSuggestionOk() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu";

        // preparation: save a suggestion annotation that can be rejected later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_SUGGESTION));
        jsAnnot = annotService.createAnnotation(jsAnnot, new UserInformation(user, Authorities.ISC));
        final String annotId = jsAnnot.getId();

        // send rejection request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/reject")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonSuggestionRejectSuccessResponse jsResponse = SerialisationHelper.deserializeJsonSuggestionRejectSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(annotId, jsResponse.getId());
        Assert.assertTrue(jsResponse.getRejected());

        // the annotation was rejected
        Assert.assertEquals(1, annotRepos.count());
        TestHelper.assertHasStatus(annotRepos, annotId, AnnotationStatus.REJECTED, user.getId());
    }

    /**
     * try rejecting a non-existing suggestion, expected HTTP 404
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testRejectSuggestionNotFound() throws Exception {

        // send acceptance request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/theid/reject")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try rejecting an existing annotation, which however is no suggestion
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testRejectSuggestionNotOkIsNoSuggestion() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu";

        // preparation: save a suggestion annotation that can be rejected later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_HIGHLIGHT)); // -> no suggestion
        jsAnnot = annotService.createAnnotation(jsAnnot, new UserInformation(user, Authorities.EdiT));
        final String annotId = jsAnnot.getId();

        // send rejection request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/reject")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

    /**
     * try rejecting an existing suggestion, but permissions are insufficient
     * expected HTTP 404
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testRejectSuggestionNoPermission() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu";

        // preparation: save a suggestion annotation that can be rejected later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_SUGGESTION));
        jsAnnot = annotService.createAnnotation(jsAnnot, new UserInformation(user, Authorities.ISC));
        final String annotId = jsAnnot.getId();

        // create another user, which however does not belong to the suggestion's group
        final User secondUser = new User("secondUser");
        userRepos.save(secondUser);
        final Token secondUserToken = new Token(secondUser, "auth", ACCESS_TOKEN2, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN2, LocalDateTime.now());
        tokenRepos.save(secondUserToken);

        final Group secondGroup = new Group("secondGroupId", "second group", "description", true);
        groupRepos.save(secondGroup);
        userGroupRepos.save(new UserGroup(secondUser.getId(), secondGroup.getId()));

        // send rejection request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId + "/reject")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN2);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }

}
