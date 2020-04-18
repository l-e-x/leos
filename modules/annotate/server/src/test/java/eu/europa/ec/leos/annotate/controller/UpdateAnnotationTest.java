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
import org.springframework.http.MediaType;
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
public class UpdateAnnotationTest {

    private static final String API_URL = "/api/annotations/";
    
    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private TagRepository tagRepos;

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

    private final static String ACCESS_TOKEN = "demoaccesstoken";
    private final static String REFRESH_TOKEN = "refr";
    private User user;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        user = new User("demo");
        userRepos.save(user);
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
     * successfully update an annotation, expected HTTP 200 and returned annotation
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testUpdateAnnotationOk() throws Exception {

        final String UPDATED_TEXT = "new text";
        final String UPDATED_TAG = "updatedtag";
        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = "acct:user@" + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // preparation: save an annotation that can be updated later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        final String annotId = jsAnnot.getId();

        // modify the annotation
        jsAnnot.setText("new text");
        jsAnnot.setTags(Arrays.asList(UPDATED_TAG));

        // send
        final String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch(API_URL + annotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonAnnotation jsResponse = SerialisationHelper.deserializeJsonAnnotation(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(annotId, jsResponse.getId());
        Assert.assertEquals(UPDATED_TEXT, jsResponse.getText());

        // verify "status" properties
        Assert.assertNotNull(jsResponse.getStatus());
        Assert.assertEquals(AnnotationStatus.NORMAL, jsResponse.getStatus().getStatus());
        Assert.assertNull(jsResponse.getStatus().getUpdated());
        Assert.assertNull(jsResponse.getStatus().getUpdated_by());
        
        // the annotation was updated - no new annotation saved
        Assert.assertEquals(1, annotRepos.count());

        // check the content was updated in the database
        final Annotation ann = annotRepos.findById(annotId);
        Assert.assertNotNull(ann);
        Assert.assertEquals(UPDATED_TEXT, ann.getText());
        Assert.assertEquals(1, ann.getTags().size());
        Assert.assertEquals(UPDATED_TAG, ann.getTags().get(0).getName());

        // and there is only one tag left in the database
        Assert.assertEquals(1, tagRepos.count());
    }

    /**
     * update an annotation that has response status SENT (by ISC user not being part of the group to which the annotation belongs)
     * expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testUpdateSentAnnotationFailure() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = "acct:user@" + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // preparation: save an annotation that can be updated later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final String initialText = jsAnnot.getText();
        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        final String annotId = jsAnnot.getId();

        // update the metadata in the database
        final Metadata savedMetadata = annotService.findAnnotationById(annotId).getMetadata();
        savedMetadata.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);
        
        // modify the annotation
        jsAnnot.setText(initialText + " new text");

        // send
        final String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch(API_URL + annotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // failure response is received
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());

        // check the content was NOT updated in the database
        final Annotation ann = annotRepos.findById(annotId);
        Assert.assertNotNull(ann);
        Assert.assertEquals(initialText, ann.getText());
    }
    
    /**
     * update an annotation that has response status SENT (by ISC user being part of the group to which the annotation belongs)
     * expected HTTP 200 and a new annotation be returned
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testUpdateSentAnnotationOk() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = "acct:user@" + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // preparation: save an annotation that can be updated later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final String initialText = jsAnnot.getText();
        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        final String annotId = jsAnnot.getId();

        // update the metadata in the database
        final Metadata savedMetadata = annotService.findAnnotationById(annotId).getMetadata();
        savedMetadata.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);
        
        // add user to the group
        final Group group = groupRepos.findByName(jsAnnot.getGroup());
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));
        
        // modify the annotation
        jsAnnot.setText(initialText + " new text");

        // send
        final String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch(API_URL + annotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // response is a new annotation with different ID, but linked to the original
        final JsonAnnotation jsResponse = SerialisationHelper.deserializeJsonAnnotation(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertNotEquals(jsResponse.getId(), jsAnnot.getId());
        Assert.assertEquals(jsResponse.getLinkedAnnotationId(), jsAnnot.getId());
        Assert.assertTrue(jsResponse.getDocument().getMetadata().containsValue(Metadata.ResponseStatus.IN_PREPARATION.toString()));

        // check that original annotation now is linked to the new one in the database
        final Annotation ann = annotRepos.findById(annotId);
        Assert.assertNotNull(ann);
        Assert.assertEquals(jsResponse.getId(), ann.getLinkedAnnotationId());
    }
    
    /**
     * failure upon updating a non-existing annotation, expected HTTP 404 and failure response
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testUpdateAnnotationFailureNotFound() throws Exception {

        // create an annotation, but don't save it in the DB
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:user@domain.eu");
        final String annotId = "theid";
        jsAnnot.setId(annotId);

        // send update request
        final String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch(API_URL + annotId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation)
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

        // the annotation was not saved
        Assert.assertEquals(0, annotRepos.count());
    }
}
