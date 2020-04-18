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

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.*;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.JsonFailureResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocumentLink;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.status.PublishContributionsSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.status.StatusUpdateSuccessResponse;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
import org.assertj.core.api.StringAssert;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class ContributorTest {

    /**
     * Tests around annotations made by a contributor 
     * note: annotations are posted to DIGIT group although users also belong to SG
     *       and although the annotations denote an SG response 
     */

    private static final String uriString = "uri://LEOS/dummy_bill_for_test";
    private static final String IscRef = "ISCReference";
    private static final String IscRefVal = "ISC/2019/4";
    private static final String UnexpExc = "Unexpected exception: ";
    private Group groupDigit;
    private User sgUser, contribUser;
    private Token sgToken, contribToken;

    @SuppressWarnings("PMD.ShortVariable")
    private final static String SG = "SG";

    // annotation IDs
    private static final String ANNOT_SG = "Annot_SG";
    private static final String ANNOT_CONTRIB = "Annot_Contrib";
    private String idAnnot_SG, idAnnot_Contrib;

    private enum ExpectedPermissions {
        ALL, USER, NONE
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private UserDetailsCache userDetailsCache;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws URISyntaxException {

        TestDbHelper.cleanupRepositories(this);
        userDetailsCache.clear();

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();

        prepareDb();
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
        userDetailsCache.clear();
    }

    // -------------------------------------
    // Test helper functions
    // -------------------------------------

    private void prepareDb() {

        final Group groupWorld = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group groupSg = new Group(SG, SG, SG, true);
        groupDigit = new Group("DIGIT", true); // just needs to be present
        groupRepos.save(Arrays.asList(groupSg, groupDigit));

        final String sgWorldUserLogin = "sgAndWorld";
        final String contribUserLogin = "contrib";

        // create users and assign them to their groups
        sgUser = new User(sgWorldUserLogin);
        contribUser = new User(contribUserLogin);
        userRepos.save(Arrays.asList(sgUser, contribUser));

        final List<UserEntity> entitiesSg = Arrays.asList(new UserEntity("3", "SG", "SG"));

        // cache info for users in order to speed up test execution
        userDetailsCache.cache(sgUser.getLogin(), new UserDetails(sgUser.getLogin(), Long.valueOf(3), SG, "user3", entitiesSg, "", null));
        userDetailsCache.cache(contribUser.getLogin(), new UserDetails(contribUser.getLogin(), Long.valueOf(3), SG, "user4", entitiesSg, "", null));

        userGroupRepos.save(new UserGroup(sgUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(sgUser.getId(), groupSg.getId()));
        userGroupRepos.save(new UserGroup(sgUser.getId(), groupDigit.getId()));
        userGroupRepos.save(new UserGroup(contribUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(contribUser.getId(), groupSg.getId()));
        userGroupRepos.save(new UserGroup(contribUser.getId(), groupDigit.getId()));

        sgToken = new Token(sgUser, Authorities.ISC, "sgA", LocalDateTime.now().plusMinutes(5), "sgR", LocalDateTime.now().plusMinutes(5));
        contribToken = new Token(contribUser, Authorities.ISC, "conA", LocalDateTime.now().plusMinutes(5), "conR", LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(Arrays.asList(sgToken, contribToken));
    }

    private JsonAnnotation createAnnotation(final String annotId, final boolean isPublic, final User user,
            final Group group, final String responseId, final int responseVersion,
            final Token token) {

        JsonAnnotation jsAnnot;

        if (isPublic) {
            jsAnnot = TestData.getTestAnnotationObject(user.getLogin());
        } else {
            jsAnnot = TestData.getTestPrivateAnnotationObject(user.getLogin());
        }
        jsAnnot.setText(annotId);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_COMMENT));
        jsAnnot.setGroup(group.getName());

        final SimpleMetadata meta = getMetadataForQuery(responseVersion, responseId);
        meta.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        jsAnnot.getDocument().setMetadata(meta);
        jsAnnot.getDocument().setLink(Arrays.asList(new JsonAnnotationDocumentLink(URI.create(uriString))));
        jsAnnot.getDocument().setTitle("title");

        jsAnnot.setUri(URI.create(uriString));

        // send annotation creation request
        return executeAnnotationCreationRequest(jsAnnot, token.getAccessToken());
    }

    private SimpleMetadata getMetadataForQuery(final int responseVersion, final String responseId) {

        // ISC reference 1, responseId, responseVersion
        final SimpleMetadata meta = new SimpleMetadata();
        meta.put(Metadata.PROP_RESPONSE_VERSION, Integer.toString(responseVersion));
        meta.put(Metadata.PROP_RESPONSE_ID, responseId);
        meta.put(IscRef, IscRefVal);

        return meta;
    }

    /**
     *  create a private ISC annotation - only the creator shall see it
     */
    @Test
    public void testPrivateAnnotVisibleForCreator() {

        final JsonAnnotation jsAnnotSg = createAnnotation(ANNOT_SG, true, sgUser, groupDigit, SG, 1, sgToken);
        idAnnot_SG = jsAnnotSg.getId();

        final JsonAnnotation jsAnnotContrib = createAnnotation(ANNOT_CONTRIB, false, contribUser, groupDigit, SG, 1, contribToken);
        idAnnot_Contrib = jsAnnotContrib.getId();

        // standard SG user sees his annotation only and has permissions on it
        final List<JsonAnnotation> annotsSgUser = executeSearchRequest(sgToken, SG, "", false);
        verifyFoundAnnots(annotsSgUser, Arrays.asList(idAnnot_SG));
        verifyPermissionEditDelete(annotsSgUser, idAnnot_SG, ExpectedPermissions.USER, ExpectedPermissions.USER);

        // the contributor sees his own private annotation and the public only of the standard SG user
        // he has permissions on his own annotation, but none on the one of the other SG user
        final List<JsonAnnotation> annotsContrib = executeSearchRequest(contribToken, SG, "", true);
        verifyFoundAnnots(annotsContrib, Arrays.asList(idAnnot_SG, idAnnot_Contrib));
        verifyPermissionEditDelete(annotsContrib, idAnnot_SG, ExpectedPermissions.NONE, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(annotsContrib, idAnnot_Contrib, ExpectedPermissions.USER, ExpectedPermissions.USER);
    }

    /**
     *  send the annotations and verify visibility remains the same 
     *  (i.e. standard user cannot see contributor's private annotation)
     */
    @Test
    public void testSendAnnotationsAndCheckVisibility() throws JsonProcessingException {

        testPrivateAnnotVisibleForCreator();

        final SimpleMetadata metaToMatch = getMetadataForQuery(1, SG);
        metaToMatch.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaToMatch);
        executeStatusUpdateRequest(groupDigit, uriString, serializedMetadataToMatch, sgToken);

        // standard SG user sees his annotation only - should have normal permissions
        final List<JsonAnnotation> annotsSgUser = executeSearchRequest(sgToken, SG, "", false);
        verifyFoundAnnots(annotsSgUser, Arrays.asList(idAnnot_SG));
        verifyPermissionEditDelete(annotsSgUser, idAnnot_SG, ExpectedPermissions.USER, ExpectedPermissions.USER);

        // the contributor sees his own private annotation and the public only of the standard SG user
        // he has permissions on his own annotation, but none on the one of the other SG user
        final List<JsonAnnotation> annotsContrib = executeSearchRequest(contribToken, SG, "", true);
        Assert.assertEquals(2, annotsContrib.size());
        verifyFoundAnnots(annotsContrib, Arrays.asList(idAnnot_SG, idAnnot_Contrib));
        verifyPermissionEditDelete(annotsContrib, idAnnot_SG, ExpectedPermissions.NONE, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(annotsContrib, idAnnot_Contrib, ExpectedPermissions.USER, ExpectedPermissions.USER);
    }

    /**
     * publish the contributor's annotation and verify visibility
     */
    @Test
    public void testPublishContributions() {

        testPrivateAnnotVisibleForCreator();
        executePublishingRequest(groupDigit, uriString, contribUser.getLogin(), IscRefVal, contribToken, HttpStatus.OK);

        // standard SG user sees his annotation only - should have normal permissions
        final List<JsonAnnotation> annotsSgUser = executeSearchRequest(sgToken, SG, "", false);
        verifyFoundAnnots(annotsSgUser, Arrays.asList(idAnnot_SG, idAnnot_Contrib));
        verifyPermissionEditDelete(annotsSgUser, idAnnot_SG, ExpectedPermissions.USER, ExpectedPermissions.USER);
        verifyPermissionEditDelete(annotsSgUser, idAnnot_Contrib, ExpectedPermissions.USER, ExpectedPermissions.USER);

        // the contributor sees his own private annotation and the public only of the standard SG user
        // he has permissions on his own annotation, but none on the one of the other SG user
        final List<JsonAnnotation> annotsContrib = executeSearchRequest(contribToken, SG, "", true);
        Assert.assertEquals(2, annotsContrib.size());
        verifyFoundAnnots(annotsContrib, Arrays.asList(idAnnot_SG, idAnnot_Contrib));
        verifyPermissionEditDelete(annotsContrib, idAnnot_SG, ExpectedPermissions.NONE, ExpectedPermissions.NONE);
     // once they are not his own any more, he has no rights any more
        verifyPermissionEditDelete(annotsContrib, idAnnot_Contrib, ExpectedPermissions.NONE, ExpectedPermissions.NONE); 
    }

    @Test
    public void testPublishContributions_noPermission() {
        
        testPrivateAnnotVisibleForCreator();
        
        contribToken.setAuthority(Authorities.EdiT); // EdiT users may not publish
        tokenRepos.save(contribToken);
        executePublishingRequest(groupDigit, uriString, contribUser.getLogin(), IscRefVal, contribToken, HttpStatus.NOT_FOUND);
    }
    
    // create a new annotation
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private JsonAnnotation executeAnnotationCreationRequest(final JsonAnnotation jsAnnot,
            final String accessToken) {

        try {
            final String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post("/api/annotations")
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(serializedAnnotation);

            final ResultActions result = this.mockMvc.perform(builder);

            // expected: Http 200
            result.andExpect(MockMvcResultMatchers.status().isOk());

            final MvcResult resultContent = result.andReturn();
            final String responseString = resultContent.getResponse().getContentAsString();

            // ID must have been set
            final JsonAnnotation jsResponse = SerialisationHelper.deserializeJsonAnnotation(responseString);
            Assert.assertNotNull(jsResponse);
            Assert.assertTrue(!jsResponse.getId().isEmpty());

            return jsResponse;
        } catch (Exception e) {
            Assert.fail(UnexpExc + e.getMessage());
        }

        return null;
    }

    // launch status update
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private StatusUpdateSuccessResponse executeStatusUpdateRequest(final Group group, final String uri, final String metadatas,
            final Token token) {

        try {
            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                    .post("/api/changeStatus?group=" + group.getName() + "&uri=" + uri +
                            "&responseStatus=SENT")
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + token.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(metadatas);

            final ResultActions result = this.mockMvc.perform(builder);

            // expected: Http 200
            result.andExpect(MockMvcResultMatchers.status().isOk());

            final MvcResult resultContent = result.andReturn();
            final String responseString = resultContent.getResponse().getContentAsString();

            // should be success message
            return SerialisationHelper.deserializeJsonStatusUpdateSuccessResponse(responseString);
        } catch (Exception e) {
            Assert.fail(UnexpExc + e.getMessage());
        }

        return null;
    }

    // launch request to publish contributor's annotations
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private void executePublishingRequest(final Group group, final String uri,
            final String userLogin, final String iscReference, final Token token, final HttpStatus expectedStatus) {

        try {
            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                    .post("/api/publishContrib?group=" + group.getName() + "&docUri=" + uri +
                            "&userId=" + userLogin + "&iscReference=" + iscReference)
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + token.getAccessToken());

            final ResultActions result = this.mockMvc.perform(builder);

            if(expectedStatus == HttpStatus.OK) {
                // expected: Http 200
                result.andExpect(MockMvcResultMatchers.status().isOk());
    
                final MvcResult resultContent = result.andReturn();
                final String responseString = resultContent.getResponse().getContentAsString();
    
                // should be success message
                final PublishContributionsSuccessResponse resp = 
                        SerialisationHelper.deserializeJsonPublishContributionsSuccessResponse(responseString);
                Assert.assertNotNull(resp);
            
            } else if(expectedStatus == HttpStatus.NOT_FOUND) {
                result.andExpect(MockMvcResultMatchers.status().isNotFound());
                
                final MvcResult resultContent = result.andReturn();
                final String responseString = resultContent.getResponse().getContentAsString();
                
                final JsonFailureResponse failure = SerialisationHelper.deserializeJsonFailureResponse(responseString);
                Assert.assertNotNull(failure);
            }
        } catch (Exception e) {
            Assert.fail(UnexpExc + e.getMessage());
        }
    }

    // run a search
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private List<JsonAnnotation> executeSearchRequest(final Token userToken, final String connectedEntity,
            final String metadatasets, final boolean isPrivateMode) {

        try {
            final StringBuffer assembledUrl = new StringBuffer("/api/search?_separate_replies=false&sort=created&order=asc&uri=").append(uriString);

            if (Authorities.isIsc(userToken.getAuthority())) {
                assembledUrl.append("&group=DIGIT&connectedEntity=") // always ask with DIGIT since this is what is done in our ISC scenario that we want to
                                                                     // reproduce
                        .append(connectedEntity);
            } else if (Authorities.isLeos(userToken.getAuthority())) {
                assembledUrl.append("&group=").append(connectedEntity);
            }

            if (!StringUtils.isEmpty(metadatasets)) {
                assembledUrl.append("&metadatasets=").append(metadatasets);
            }

            if (isPrivateMode) {
                assembledUrl.append("&mode=private");
            }

            // send search request
            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                    .get(assembledUrl.toString())
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + userToken.getAccessToken());
            final ResultActions result = this.mockMvc.perform(builder);

            // expected: Http 200
            result.andExpect(MockMvcResultMatchers.status().isOk());

            final MvcResult resultContent = result.andReturn();
            final String responseString = resultContent.getResponse().getContentAsString();

            // check that the expected annotation was returned (compare IDs)
            final JsonSearchResult jsResponse = SerialisationHelper.deserializeJsonSearchResult(responseString);
            Assert.assertNotNull(jsResponse);

            return jsResponse.getRows();
        } catch (Exception e) {
            Assert.fail(UnexpExc + e.getMessage());
        }

        return null;
    }

    private void verifyFoundAnnots(final List<JsonAnnotation> foundAnnots, final List<String> expectedAnnotIds) {

        Assert.assertEquals("Number of received annotation is not the expected one", expectedAnnotIds.size(), foundAnnots.size());

        final List<String> receivedIds = foundAnnots.stream().map(jsAnn -> jsAnn.getId()).collect(Collectors.toList());

        // verify that all IDs are found
        for (int i = 0; i < foundAnnots.size(); i++) {
            Assert.assertTrue("Received annotation " + receivedIds.get(i) + " unexpected!", expectedAnnotIds.contains(receivedIds.get(i)));
            Assert.assertTrue("Expected annotation " + expectedAnnotIds.get(i) + " not received!", receivedIds.contains(expectedAnnotIds.get(i)));
        }
    }

    // verifies the editing ("update") and deletion permissions that are set to the annotation
    private void verifyPermissionEditDelete(final List<JsonAnnotation> jsAnnots, final String annotId,
            final ExpectedPermissions expectedPermEdit, final ExpectedPermissions expectedPermDelete) {

        Assert.assertNotNull(jsAnnots);

        final JsonAnnotation jsAnnot = jsAnnots.stream().filter(jsAnn -> jsAnn.getId().equals(annotId)).findFirst().get();
        Assert.assertNotNull(jsAnnot.getPermissions());
        Assert.assertFalse(CollectionUtils.isEmpty(jsAnnot.getPermissions().getUpdate()));
        Assert.assertFalse(CollectionUtils.isEmpty(jsAnnot.getPermissions().getDelete()));

        final String updatePerm = jsAnnot.getPermissions().getUpdate().get(0);
        verifyPermission(updatePerm, expectedPermEdit);

        final String deletePerm = jsAnnot.getPermissions().getDelete().get(0);
        verifyPermission(deletePerm, expectedPermDelete);
    }

    private void verifyPermission(final String actualPerm, final ExpectedPermissions expectedPerm) {

        final StringAssert strAss = new StringAssert(actualPerm);

        if (expectedPerm == ExpectedPermissions.USER) {
            strAss.startsWith("acct:");
        } else if (expectedPerm == ExpectedPermissions.NONE) {
            strAss.isEmpty();
        }
    }
}
