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
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonDeleteSuccessResponse;
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

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class DeleteAnnotationTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "r8", API_PREFIX = "/api/annotations/";
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
    private MetadataRepository metadataRepos;

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
        tokenRepos.save(new Token(user, Authorities.ISC, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now().plusMinutes(5)));

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
     * successfully delete an existing annotation, expected HTTP 200 and ID of the deleted annotation
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testDeleteAnnotationOk() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu";

        // preparation: save an annotation that can be deleted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, new UserInformation(user, "AGRI"));
        final String annotId = jsAnnot.getId();
        Assert.assertEquals(AnnotationStatus.NORMAL, annotRepos.findById(annotId).getStatus()); // check: annotation is existing

        // send deletion request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(API_PREFIX + annotId)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonDeleteSuccessResponse jsResponse = SerialisationHelper.deserializeJsonDeleteSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(annotId, jsResponse.getId());

        // the annotation was "deleted"
        Assert.assertEquals(1, annotRepos.count());
        TestHelper.assertHasStatus(annotRepos, annotId, AnnotationStatus.DELETED, user.getId());
    }

    /**
     * successfully delete an existing annotation of another user, expected HTTP 200
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testDeleteAnnotationOfAnotherUser() throws Exception {

        final String authority = "ABC";
        final String login = "demo2";
        final String hypothesisUserAccount = "acct:user@domain.eu";

        // create another user
        final User otherUser = new User(login);
        userRepos.save(otherUser);

        // preparation: save an annotation (of another user than the logged-in user)
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, new UserInformation(user, authority));
        final String annotId = jsAnnot.getId();
        Assert.assertEquals(AnnotationStatus.NORMAL, annotRepos.findById(annotId).getStatus()); // check: annotation is existing
        
        // send deletion request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(API_PREFIX + annotId)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN); // access token is saved for user "demo", which is not the annotation owner

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonDeleteSuccessResponse jsResponse = SerialisationHelper.deserializeJsonDeleteSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(annotId, jsResponse.getId());

        // the annotation was deleted
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(AnnotationStatus.DELETED, annotRepos.findById(annotId).getStatus()); // check: annotation is flagged as being deleted
    }

    /**
     * failure deleting a non-existing annotation, expected HTTP 404 and failure response
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testDeleteAnnotationFailureNotFound() throws Exception {

        // send deletion request for non-existing ID
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(API_PREFIX + "theid")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // failure response is received
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());
    }
    
    /**
     * try to delete an existing annotation with response status SENT as an ISC user
     * expected HTTP 200
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testDeleteSentAnnotationForIscUserOk() throws Exception {

        final String hypothesisUserAccount = "acct:user@domain.eu";

        // preparation: save an annotation that can be deleted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, new UserInformation(user, "AGRI"));
        final String annotId = jsAnnot.getId();
        Assert.assertEquals(AnnotationStatus.NORMAL, annotRepos.findById(annotId).getStatus()); // check: annotation is existing

        // modify the response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(annotId).getMetadata();
        savedMetadata.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);
        
        // send deletion request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete(API_PREFIX + annotId)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonDeleteSuccessResponse jsResponse = SerialisationHelper.deserializeJsonDeleteSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(annotId, jsResponse.getId());

        // the annotation was not deleted, but flagged as "sent deleted"
        Assert.assertEquals(1, annotRepos.count());
        TestHelper.assertHasStatus(annotRepos, annotId, AnnotationStatus.NORMAL, null);
        Assert.assertTrue(annotRepos.findById(annotId).isSentDeleted());
    }
}
