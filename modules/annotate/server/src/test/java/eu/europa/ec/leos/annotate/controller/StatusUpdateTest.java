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
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.status.StatusUpdateSuccessResponse;
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
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class StatusUpdateTest {

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "helloRefresh";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationRepository annotRepos;
    
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private DocumentRepository documentRepos;
    
    @Autowired
    private UserRepository userRepos;

    @Autowired
    private TokenRepository tokenRepos;
    
    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;
    private User user;

    private static final String ISC_REF = "ISCReference";
    private static final String RESP_STATUS = "responseStatus";
    private static final String RESP_VERS = "responseVersion";
    private static final String RESP_ID = "responseId";
    
    private static final String RESP_STATUS_PREP = "IN_PREPARATION";


    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        user = new User("demo");
        userRepos.save(user);

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
     * successfully update annotation status, expected HTTP 200 and success message
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testResponseStatusUpdateOk() throws Exception {

        final String RESP_VERS_VAL = "1";
        final String ISC_REF_VAL = "ISC/2016/642";
        final String RESP_ID_VAL = "id3";
        final Token token = new Token(user, Authorities.ISC, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(token);
        final UserInformation userInfo = new UserInformation(token);

        // save an annotation having the required metadata
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:" + user.getLogin() + "@" + Authorities.ISC);
        jsAnnot.getDocument().getMetadata().put(ISC_REF, ISC_REF_VAL);
        jsAnnot.getDocument().getMetadata().put(RESP_VERS, RESP_VERS_VAL);
        jsAnnot.getDocument().getMetadata().put(RESP_ID, RESP_ID_VAL);
        jsAnnot.getDocument().getMetadata().put(RESP_STATUS, RESP_STATUS_PREP);

        final String annotId = annotService.createAnnotation(jsAnnot, userInfo).getId();

        // verify that annotation status was set according to metadata
        Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(Metadata.ResponseStatus.IN_PREPARATION, readAnnot.getMetadata().getResponseStatus());

        // create a second annotation in SENT status that is linked to the first annotation
        final JsonAnnotation jsAnnotSent = TestData.getTestAnnotationObject("acct:somebody@" + Authorities.ISC);
        final String annotIdSent = annotService.createAnnotation(jsAnnotSent, userInfo).getId();
        final Metadata readMetaSent = annotService.findAnnotationById(annotIdSent).getMetadata();
        readMetaSent.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(readMetaSent);
        
        readAnnot.setLinkedAnnotationId(annotIdSent);
        final Annotation readAnnotSent = annotService.findAnnotationById(annotIdSent);
        readAnnotSent.setLinkedAnnotationId(annotId);
        annotRepos.save(readAnnot);
        annotRepos.save(readAnnotSent);
        
        final SimpleMetadata metaMap = new SimpleMetadata();
        metaMap.put(ISC_REF, ISC_REF_VAL);
        metaMap.put(RESP_VERS, RESP_VERS_VAL);
        metaMap.put(RESP_ID, RESP_ID_VAL);
        metaMap.put(RESP_STATUS, RESP_STATUS_PREP);
        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaMap);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .post("/api/changeStatus?group=" + jsAnnot.getGroup() + "&uri=" + jsAnnot.getDocument().getLink().get(0).getHref().toString() +
                        "&responseStatus=SENT")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedMetadataToMatch);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // should be success message
        final StatusUpdateSuccessResponse jsResponse = SerialisationHelper.deserializeJsonStatusUpdateSuccessResponse(responseString);
        Assert.assertNotNull(jsResponse);

        // verify
        // - the annotation's metadata status was changed from IN_PREPARATION to SENT
        // - the metadata status change was tracked
        // - it is no longer linked to another annotation
        readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(Metadata.ResponseStatus.SENT, readAnnot.getMetadata().getResponseStatus());
        Assert.assertTrue(TestHelper.withinLastSeconds(readAnnot.getMetadata().getResponseStatusUpdated(), 5));
        Assert.assertEquals(user.getId(), readAnnot.getMetadata().getResponseStatusUpdatedBy());
        Assert.assertNull(readAnnot.getLinkedAnnotationId());
    }

    /**
     * updating annotation status fails since requested group does not exist, expected HTTP 404 and error message
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testResponseStatusUpdateFailure() throws Exception {

        final String RESP_VERS_VAL = "1";
        final String ISC_REF_VAL = "ISC/2016/644";
        final String RESP_ID_VAL = "id2";
        final Token token = new Token(user, Authorities.ISC, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(token);

        final SimpleMetadata metaMap = new SimpleMetadata();
        metaMap.put(ISC_REF, ISC_REF_VAL);
        metaMap.put(RESP_VERS, RESP_VERS_VAL);
        metaMap.put(RESP_ID, RESP_ID_VAL);
        metaMap.put(RESP_STATUS, RESP_STATUS_PREP);
        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaMap);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .post("/api/changeStatus?group=somegroup&uri=http://dummy&responseStatus=SENT")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedMetadataToMatch);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // should be success message
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!StringUtils.isEmpty(jsResponse.getReason()));
    }

    /**
     * launch a request with metadata not matching anything; in addition, there are not sentDeleted items requiring being
     * really deleted -> should throw exception internally
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testResponseStatusUpdateFailure_nothingToUpdate() throws Exception {

        // create the document
        final String DOCURI = "file://LEOS/1";
        final Document doc = new Document(URI.create(DOCURI), "title");
        documentRepos.save(doc);
        
        final String RESP_VERS_VAL = "1";
        final String ISC_REF_VAL = "ISC/2016/644";
        final String RESP_ID_VAL = "id2";
        final Token token = new Token(user, Authorities.ISC, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(token);

        final SimpleMetadata metaMap = new SimpleMetadata();
        metaMap.put(ISC_REF, ISC_REF_VAL);
        metaMap.put(RESP_VERS, RESP_VERS_VAL);
        metaMap.put(RESP_ID, RESP_ID_VAL);
        metaMap.put(RESP_STATUS, RESP_STATUS_PREP);
        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaMap);

        // we launch the request with some metadata, but there are neither metadata matches nor any sentDeleted items exist
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .post("/api/changeStatus?group=__world__&uri=" + DOCURI + "&responseStatus=SENT")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedMetadataToMatch);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // should be success message
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!StringUtils.isEmpty(jsResponse.getReason()));
    }
    
    /**
     * updating annotation status fails since group was not set in request, expected HTTP 400 and error message
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testResponseStatusUpdateFailure_MissingParameters() throws Exception {

        final String RESP_VERS_VAL = "1";
        final String ISC_REF_VAL = "ISC/2016/648";
        final String RESP_ID_VAL = "id2";
        final Token token = new Token(user, Authorities.ISC, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(token);
        
        final SimpleMetadata metaMap = new SimpleMetadata();
        metaMap.put(ISC_REF, ISC_REF_VAL);
        metaMap.put(RESP_VERS, RESP_VERS_VAL);
        metaMap.put(RESP_ID, RESP_ID_VAL);
        metaMap.put(RESP_STATUS, RESP_STATUS_PREP);
        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaMap);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .post("/api/changeStatus?uri=http://dummy&responseStatus=SENT")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedMetadataToMatch);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // should be success message
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!StringUtils.isEmpty(jsResponse.getReason()));
    }
    
    /**
     * updating annotation status fails since user does not have required permission, expected HTTP 404 and error message
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testResponseStatusUpdateFailure_MissingPermission() throws Exception {

        final String ISC_REF_VAL = "ISC/2016/642";
        final Token token = new Token(user, Authorities.EdiT, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN,
                LocalDateTime.now().plusMinutes(5)); // user is no ISC user -> is lacking permissions
        tokenRepos.save(token);
        
        final SimpleMetadata metaMap = new SimpleMetadata();
        metaMap.put(ISC_REF, ISC_REF_VAL);
        metaMap.put(RESP_STATUS, RESP_STATUS_PREP);
        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaMap);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .post("/api/changeStatus?group=thegroup&uri=http://thedocument/uri&responseStatus=SENT")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedMetadataToMatch);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 404
        result.andExpect(MockMvcResultMatchers.status().isNotFound());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // should be success message
        final JsonFailureResponse jsResponse = SerialisationHelper.deserializeJsonFailureResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertTrue(!StringUtils.isEmpty(jsResponse.getReason()));
    }
    
}
