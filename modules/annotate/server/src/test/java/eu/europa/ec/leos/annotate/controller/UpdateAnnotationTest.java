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
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
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
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class UpdateAnnotationTest {

    private final static String ACCESS_TOKEN = "demoaccesstoken";
    private final static String REFRESH_TOKEN = "refr";

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        User user = new User("demo");
        userRepos.save(user);
        tokenRepos.save(new Token(user, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now()));

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
    private AnnotationRepository annotRepos;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private GroupRepository groupRepos;

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
     * successfully update an annotation, expected HTTP 200 and returned annotation
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testUpdateAnnotationOk() throws Exception {

        final String UPDATED_TEXT = "new text", UPDATED_TAG = "updatedtag";
        final String hypothesisUserAccount = "acct:user@domain.eu", login = "demo";

        // preparation: save an annotation that can be updated later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        String id = jsAnnot.getId();

        // modify the annotation
        jsAnnot.setText("new text");
        jsAnnot.setTags(Arrays.asList(UPDATED_TAG));

        // send
        String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/annotations/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation)
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        JsonAnnotation jsResponse = SerialisationHelper.deserializeJsonAnnotation(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(id, jsResponse.getId());
        Assert.assertEquals(UPDATED_TEXT, jsResponse.getText());

        // the annotation was updated - no new annotation saved
        Assert.assertEquals(1, annotRepos.count());

        // check the content was updated in the database
        Annotation ann = annotRepos.findById(id);
        Assert.assertNotNull(ann);
        Assert.assertEquals(UPDATED_TEXT, ann.getText());
        Assert.assertEquals(1, ann.getTags().size());
        Assert.assertEquals(UPDATED_TAG, ann.getTags().get(0).getName());

        // and there is only one tag left in the database
        Assert.assertEquals(1, tagRepos.count());
    }

    /**
     * failure upon updating a non-existing annotation, expected HTTP 404 and failure response
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testUpdateAnnotationFailureNotFound() throws Exception {

        // create an annotation, but don't save it in the DB
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:user@domain.eu");
        String id = "theid";
        jsAnnot.setId(id);

        // send update request
        String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.patch("/api/annotations/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation)
                .header("authorization", "Bearer " + ACCESS_TOKEN);

        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // failure response is received
        JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!jsResponse.getReason().isEmpty());

        // the annotation was not saved
        Assert.assertEquals(0, annotRepos.count());
    }
}
