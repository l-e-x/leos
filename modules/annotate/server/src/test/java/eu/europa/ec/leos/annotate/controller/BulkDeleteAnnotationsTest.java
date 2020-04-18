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
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonBulkDeleteSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonIdList;
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
public class BulkDeleteAnnotationsTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "r8";
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
        tokenRepos.save(new Token(user, "auth", ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now().plusMinutes(5)));

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
     * successfully delete existing annotations, expected HTTP 200 and success response
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testBulkDeleteAnnotationsOk() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = "acct:user@domain.eu";
        final UserInformation userInfo = new UserInformation(user, authority);
        
        // preparation: save two annotations that can be deleted later on
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        JsonAnnotation jsAnnot2 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot2 = annotService.createAnnotation(jsAnnot2, userInfo);

        final JsonIdList idList = new JsonIdList();
        idList.setIds(Arrays.asList(jsAnnot.getId(), jsAnnot2.getId()));
        final String serializedList = SerialisationHelper.serialize(idList);
        
        // send deletion request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedList);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonBulkDeleteSuccessResponse jsResponse = SerialisationHelper.deserializeJsonBulkDeleteSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);

        // the annotation was "deleted"
        Assert.assertEquals(2, annotRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.DELETED, user.getId());
        TestHelper.assertHasStatus(annotRepos, jsAnnot2.getId(), AnnotationStatus.DELETED, user.getId());
    }
}
