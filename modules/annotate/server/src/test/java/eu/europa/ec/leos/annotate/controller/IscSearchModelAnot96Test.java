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
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchCount;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
import org.assertj.core.api.StringAssert;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class IscSearchModelAnot96Test {

    /**
     * class testing the particular changes in the ISC search model introduced with ANOT-96
     * and the permission assignment defined with ANOT-102
     * tests are executed for the "search" and "count" APIs
     */

    private static final String uriString = "uri://LEOS/dummy_bill_for_test";
    private static final String dummySelector = "[{\"selector\":null,\"source\":\"" + uriString + "\"}]";
    private Group groupDigit, groupAgri, groupSg;
    private User digitWorldUser, agriWorldUser, sgWorldUser;

    private static final String ANNOT_A = "idA", ANNOT_B = "idB", ANNOT_C = "idC", ANNOT_D = "idD";
    private static final String ANNOT_E = "idE", ANNOT_F = "idF", ANNOT_G = "idG", ANNOT_H = "idH";
    private static final String ANNOT_I = "idI";

    private final static String DIGIT = "DIGIT";
    private final static String AGRI = "AGRI";
    @SuppressWarnings("PMD.ShortVariable")
    private final static String SG = "SG";

    private enum ExpectedPermissions {
        USER, NONE
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

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

        createTestData();

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
        userDetailsCache.clear();
    }

    // -------------------------------------
    // Test data
    // -------------------------------------

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private void createTestData() {

        final Group groupWorld = TestDbHelper.insertDefaultGroup(groupRepos);
        groupDigit = new Group(DIGIT, DIGIT, DIGIT, true);
        groupAgri = new Group(AGRI, AGRI, AGRI, false);
        groupSg = new Group(SG, SG, SG, true);
        groupRepos.save(Arrays.asList(groupDigit, groupAgri, groupSg));

        final String digitWorldUserLogin = "digitAndWorld";
        final String agriWorldUserLogin = "agriAndWorld";
        final String sgWorldUserLogin = "sgAndWorld";

        // create users and assign them to their groups
        digitWorldUser = new User(digitWorldUserLogin);
        agriWorldUser = new User(agriWorldUserLogin);
        sgWorldUser = new User(sgWorldUserLogin);
        userRepos.save(Arrays.asList(digitWorldUser, agriWorldUser, sgWorldUser));

        final List<UserEntity> entitiesDigit = Arrays.asList(new UserEntity("2", DIGIT, DIGIT));
        final List<UserEntity> entitiesAgri = Arrays.asList(new UserEntity("3", AGRI, AGRI));
        final List<UserEntity> entitiesSg = Arrays.asList(new UserEntity("4", "SG", "SG"));

        // cache info for users in order to speed up test execution
        userDetailsCache.cache(digitWorldUser.getLogin(), new UserDetails(digitWorldUser.getLogin(), Long.valueOf(2), DIGIT, "user2", entitiesDigit, "", null));
        userDetailsCache.cache(agriWorldUser.getLogin(), new UserDetails(agriWorldUser.getLogin(), Long.valueOf(3), AGRI, "user3", entitiesAgri, "", null));
        userDetailsCache.cache(sgWorldUser.getLogin(), new UserDetails(sgWorldUser.getLogin(), Long.valueOf(4), "sg", "user4", entitiesSg, "", null));

        userGroupRepos.save(new UserGroup(digitWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(digitWorldUser.getId(), groupDigit.getId()));
        userGroupRepos.save(new UserGroup(agriWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(agriWorldUser.getId(), groupAgri.getId()));
        userGroupRepos.save(new UserGroup(sgWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(sgWorldUser.getId(), groupSg.getId()));

        // create a document
        final Document document = new Document(URI.create(uriString), "title");
        documentRepos.save(document);

        // we want to create the following annotations/metadata:
        // A: AGRI, SENT, responseVersion 1
        // B: AGRI, SENT, responseVersion 2
        // C: DIGIT, SENT, responseVersion 1
        // D: DIGIT, SENT, responseVersion 1, sentDeleted:true, DELETED
        // E: DIGIT, SENT, responseVersion 2, linked to G
        // F: DIGIT, SENT, responseVersion 2, sentDeleted:true
        // G: DIGIT, IN_PREPARATION, responseVersion 3, linked to E
        // H: DIGIT, IN_PREPARATION, responseVersion 3
        // I: AGRI, IN_PREPARATION, responseVersion 3

        // for A:
        final SimpleMetadata metaHelpSentV1 = new SimpleMetadata(Metadata.PROP_RESPONSE_VERSION, "1");
        metaHelpSentV1.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());

        final Metadata metaSentAgriV1 = new Metadata(document, groupAgri, Authorities.ISC, metaHelpSentV1);
        addResponseId(metaSentAgriV1, AGRI);
        metadataRepos.save(metaSentAgriV1);
        createAnnotation(ANNOT_A, agriWorldUser, metaSentAgriV1);

        // for B:
        final SimpleMetadata metaHelpSentV2 = new SimpleMetadata(Metadata.PROP_RESPONSE_VERSION, "2");
        metaHelpSentV2.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());

        final Metadata metaSentAgriV2 = new Metadata(document, groupAgri, Authorities.ISC, metaHelpSentV2);
        addResponseId(metaSentAgriV2, AGRI);
        metadataRepos.save(metaSentAgriV2);
        createAnnotation(ANNOT_B, agriWorldUser, metaSentAgriV2);

        // for C:
        final Metadata metaSentDigitV1 = new Metadata(document, groupDigit, Authorities.ISC, metaHelpSentV1);
        addResponseId(metaSentDigitV1, DIGIT);
        metadataRepos.save(metaSentDigitV1);
        createAnnotation(ANNOT_C, digitWorldUser, metaSentDigitV1);

        // for D:
        @SuppressWarnings("PMD.LongVariable")
        final Metadata metaSentDigitV1SentDeletedDeleted = new Metadata(document, groupDigit, Authorities.ISC, metaHelpSentV1);
        addResponseId(metaSentDigitV1SentDeletedDeleted, DIGIT);
        metadataRepos.save(metaSentDigitV1SentDeletedDeleted);
        final Annotation annotD = createAnnotation(ANNOT_D, digitWorldUser, metaSentDigitV1SentDeletedDeleted);
        annotD.setSentDeleted(true);
        annotD.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(annotD);

        // for E:
        final Metadata metaSentDigitV2Linked = new Metadata(document, groupDigit, Authorities.ISC, metaHelpSentV2);
        addResponseId(metaSentDigitV2Linked, DIGIT);
        metadataRepos.save(metaSentDigitV2Linked);
        final Annotation annotE = createAnnotation(ANNOT_E, digitWorldUser, metaSentDigitV2Linked);
        // will be linked to G below

        // for F:
        @SuppressWarnings("PMD.LongVariable")
        final Metadata metaSentDigitV2SentDeleted = new Metadata(document, groupDigit, Authorities.ISC, metaHelpSentV2);
        addResponseId(metaSentDigitV2SentDeleted, DIGIT);
        metadataRepos.save(metaSentDigitV2SentDeleted);
        final Annotation annotF = createAnnotation(ANNOT_F, digitWorldUser, metaSentDigitV2SentDeleted);
        annotF.setSentDeleted(true);
        annotRepos.save(annotF);

        // for G:
        final SimpleMetadata metaHelpInPrepV3 = new SimpleMetadata(Metadata.PROP_RESPONSE_VERSION, "3");
        metaHelpInPrepV3.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        final Metadata metaInPrepDigitV3Linked = new Metadata(document, groupDigit, Authorities.ISC, metaHelpInPrepV3);
        addResponseId(metaInPrepDigitV3Linked, DIGIT);
        metadataRepos.save(metaInPrepDigitV3Linked);
        final Annotation annotG = createAnnotation(ANNOT_G, digitWorldUser, metaInPrepDigitV3Linked);
        annotG.setLinkedAnnotationId(annotE.getId());
        annotE.setLinkedAnnotationId(annotG.getId());
        annotRepos.save(annotG);
        annotRepos.save(annotE);

        // for H:
        final Metadata metaInPrepDigitV3 = new Metadata(document, groupDigit, Authorities.ISC, metaHelpInPrepV3);
        metadataRepos.save(metaInPrepDigitV3);
        createAnnotation(ANNOT_H, digitWorldUser, metaInPrepDigitV3);

        // for I:
        final Metadata metaInPrepAgriV3 = new Metadata(document, groupAgri, Authorities.ISC, metaHelpInPrepV3);
        metadataRepos.save(metaInPrepAgriV3);
        final Annotation annotI = createAnnotation(ANNOT_I, agriWorldUser, metaInPrepAgriV3);
        annotRepos.save(annotI);
    }

    private Annotation createAnnotation(final String annotId, final User user, final Metadata meta) {

        final Annotation annot = new Annotation();
        annot.setId(annotId);
        annot.setUser(user);
        annot.setMetadata(meta);

        // mandatory fields
        annot.setId(annotId);
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setTargetSelectors(dummySelector);

        annot.setShared(true);

        return annotRepos.save(annot);
    }

    private void addResponseId(final Metadata meta, final String respId) {

        final SimpleMetadata kvPairs = meta.getKeyValuePropertyAsSimpleMetadata();
        kvPairs.put("responseId", respId);
        meta.setKeyValuePropertyFromSimpleMetadata(kvPairs);
    }

    // -------------------------------------
    // Tests for search model ISC.1 - search
    // -------------------------------------

    /**
     * search for AGRI user in AGRI group
     */
    @Test
    public void search_Agri_Agri() {

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriWorldUser, groupAgri, AGRI,
                Arrays.asList(ANNOT_A, ANNOT_B, // SENT by AGRI
                        ANNOT_C, // SENT by DIGIT
                        ANNOT_E, // SENT by DIGIT, edited afterwards
                        ANNOT_F, // SENT by DIGIT, deleted afterwards
                        ANNOT_I));// IN_PREPARATION by AGRI

        // permissions are expected for annotations A, B (due to matching connectedEntity)
        verifyPermissionEditDelete(foundAnnots, ANNOT_A, ExpectedPermissions.USER);
        verifyPermissionEditDelete(foundAnnots, ANNOT_B, ExpectedPermissions.USER);

        // no permissions are expected for annotations C, E, F (since connectedEntity is set at all)
        verifyPermissionEditDelete(foundAnnots, ANNOT_C, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_E, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_F, ExpectedPermissions.NONE);

        // note: we don't check the IN_PREPARATION annotation permissions here
    }

    /**
     * search for AGRI user in DIGIT group
     * currently not supported - in ISC, user only searches for his own group
     * -> no matches
     */
    @Test
    public void search_Agri_Digit() {

        executeSearchRequest(agriWorldUser, groupDigit, "",
                new ArrayList<String>());
    }

    /**
     *  search for DIGIT user in DIGIT group
     *  note: the connectedEntity is not set to SG (-> no permissions for editing&deleting)
     *  -> sees all SENT non-deleted items (A, B, C)
     *  -> does not see the ones he edited (E) or deleted (F)
     *  -> sees the ones being IN_PREPARATION for DIGIT (G, H)
     *  -> does not see the AGRI items being IN_PREPARATION (I)
     */
    @Test
    public void search_Digit_Digit() {

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(digitWorldUser, groupDigit, "",
                Arrays.asList(ANNOT_A, ANNOT_B, // SENT by AGRI
                        ANNOT_C, // SENT by DIGIT
                        ANNOT_G, ANNOT_H)); // IN_PREPARATION by DIGIT

        // no permissions are expected for annotations A, B, C (since connectedEntity is not set at all)
        verifyPermissionEditDelete(foundAnnots, ANNOT_A, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_B, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_C, ExpectedPermissions.NONE);
    }

    /**
     *  search for DIGIT user in DIGIT group
     *  note: the connectedEntity is set to DIGIT (-> permissions for editing&deleting)
     *  -> sees all SENT non-deleted items (A, B, C)
     *  -> does not see the SENT items he edited (E) or deleted (F)
     *  -> sees the ones being IN_PREPARATION for DIGIT (G, H)
     *  -> does not see the AGRI items being IN_PREPARATION (I)
     */
    @Test
    public void search_Digit_Digit_ConnectedEntitySet() {

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(digitWorldUser, groupDigit, DIGIT,
                Arrays.asList(ANNOT_A, ANNOT_B, // SENT by AGRI
                        ANNOT_C, // SENT by DIGIT
                        ANNOT_G, ANNOT_H)); // IN_PREPARATION by DIGIT

        // no permissions are expected for annotations A, B (different group)
        verifyPermissionEditDelete(foundAnnots, ANNOT_A, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_B, ExpectedPermissions.NONE);

        // permissions are expected for annotations C (since connectedEntity is set at all)
        verifyPermissionEditDelete(foundAnnots, ANNOT_C, ExpectedPermissions.USER);
    }

    /**
     *  search for SG user in SG group; the connectedEntity is also set to SG 
     * -> no permissions for editing&deleting available since no annotation comes from SG
     *  -> sees all SENT non-deleted items (A, B, C)
     *  -> still sees the ones edited and deleted by DIGIT, but not yet SENT again (E, F) 
     */
    @Test
    public void search_Sg_Sg() {

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgWorldUser, groupSg, SG,
                Arrays.asList(ANNOT_A, ANNOT_B, // SENT by AGRI
                        ANNOT_C, // SENT by DIGIT
                        ANNOT_E, // SENT by DIGIT, edited afterwards
                        ANNOT_F)); // SENT by DIGIT, deleted afterwards

        // no permissions are expected for annotations A, B, C, E, F (since connectedEntity is not set at all)
        verifyPermissionEditDelete(foundAnnots, ANNOT_A, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_B, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_C, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_E, ExpectedPermissions.NONE);
        verifyPermissionEditDelete(foundAnnots, ANNOT_F, ExpectedPermissions.NONE);
    }

    // -------------------------------------
    // Tests for search model ISC.1 - counting
    // -------------------------------------

    /**
     * count annotations for AGRI user in AGRI group
     */
    @Test
    public void count_Agri_Agri() {

        // ANNOT_A, ANNOT_B: SENT by AGRI
        // ANNOT_C: SENT by DIGIT
        // ANNOT_E: SENT by DIGIT, edited afterwards
        // ANNOT_F: SENT by DIGIT, deleted afterwards
        // ANNOT_I: IN_PREPARATION by AGRI
        // -> 6 items
        executeCountRequest(agriWorldUser, groupAgri, 6);
    }

    /**
     * count for AGRI user in DIGIT group
     * currently not supported - in ISC, user only searches for his own group
     * -> no matches
     */
    @Test
    public void count_Agri_Digit() {

        // -1 is returned as this is not supported
        executeCountRequest(agriWorldUser, groupDigit, -1);
    }

    /**
     *  count for DIGIT user in DIGIT group
     *  -> sees all SENT non-deleted items (A, B, C)
     *  -> does not see the ones he edited (E) or deleted (F)
     *  -> sees the ones being IN_PREPARATION for DIGIT (G, H)
     *  -> does not see the AGRI items being IN_PREPARATION (I)
     */
    @Test
    public void count_Digit_Digit() {

        // ANNOT_A, ANNOT_B: SENT by AGRI
        // ANNOT_C: SENT by DIGIT
        // ANNOT_G, ANNOT_H: IN_PREPARATION by DIGIT
        // -> 5 items
        executeCountRequest(digitWorldUser, groupDigit, 5);
    }
    
    /**
     *  count for SG user in SG group; the connectedEntity is also set to SG 
     * -> no permissions for editing&deleting available since no annotation comes from SG
     *  -> sees all SENT non-deleted items (A, B, C)
     *  -> still sees the ones edited and deleted by DIGIT, but not yet SENT again (E, F) 
     */
    @Test
    public void count_Sg_Sg() {

        // we create a highlight that should have been SENT by DIGIT (same metadata as ANNOT_C)
        // this will most probably not occur in real-life, but here it verifies that highlights are ignored;
        // in the same way, we create a private annotation having same metadata as ANNOT_C
        final Annotation refAnnot = annotRepos.findById(ANNOT_C);
        
        final Annotation newHighlight = createAnnotation("highl", digitWorldUser, refAnnot.getMetadata());
        newHighlight.setTags(Arrays.asList(new Tag(Annotation.ANNOTATION_HIGHLIGHT, newHighlight)));
        annotRepos.save(newHighlight);
        
        final Annotation privAnnot = createAnnotation("private", digitWorldUser, refAnnot.getMetadata());
        privAnnot.setShared(false);
        annotRepos.save(privAnnot);
        
        // ANNOT_A, ANNOT_B: SENT by AGRI
        // ANNOT_C: SENT by DIGIT
        // ANNOT_E: SENT by DIGIT, edited afterwards
        // ANNOT_F: SENT by DIGIT, deleted afterwards
        // -> 5 items (highlight and private annotation are ignored)
        executeCountRequest(sgWorldUser, groupSg, 5);
    }
    
    // -------------------------------------
    // internal function that does the actual job and verifies the result
    // -------------------------------------
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private List<JsonAnnotation> executeSearchRequest(final User user, final Group group, final String connectedEntity,
            final List<String> expectedAnnotIds) {

        final Token userToken = new Token(user, Authorities.ISC, "@cc", LocalDateTime.now().plusMinutes(5), "refr", LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(userToken);

        try {
            final StringBuffer assembledUrl = new StringBuffer("/api/search?_separate_replies=false&sort=created&order=asc&uri=").append(uriString)
                    .append("&group=").append(group.getName());
            if (!StringUtils.isEmpty(connectedEntity)) {
                assembledUrl.append("&connectedEntity=").append(connectedEntity);
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
            Assert.assertEquals(expectedAnnotIds.size(), jsResponse.getRows().size());

            final List<String> receivedIds = jsResponse.getRows().stream().map(jsAnn -> jsAnn.getId()).collect(Collectors.toList());

            // verify that all IDs are found
            for (int i = 0; i < jsResponse.getRows().size(); i++) {
                Assert.assertTrue("Received annotation " + receivedIds.get(i) + " unexpected!", expectedAnnotIds.contains(receivedIds.get(i)));
                Assert.assertTrue("Expected annotation " + expectedAnnotIds.get(i) + " not received!", receivedIds.contains(expectedAnnotIds.get(i)));
            }

            return jsResponse.getRows();
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }

        return null;
    }

    /**
     * successfully count annotations, expected HTTP 200 and found number (of matching annotations)
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void executeCountRequest(final User user, final Group group, final int expectedAnnotations) {

        try {
            final Token userToken = new Token(user, Authorities.ISC, "ac", LocalDateTime.now().plusMinutes(5), "re", LocalDateTime.now().plusMinutes(5));
            tokenRepos.save(userToken);
    
            final StringBuffer url = new StringBuffer().append("/api/count?uri=").append(uriString)
                    .append("&group=").append(group.getName());
    
            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url.toString())
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + userToken.getAccessToken());
    
            final ResultActions result = this.mockMvc.perform(builder);
    
            // expected: Http 200
            result.andExpect(MockMvcResultMatchers.status().isOk());
    
            final MvcResult resultContent = result.andReturn();
            final String responseString = resultContent.getResponse().getContentAsString();
    
            // ID must have been set
            final JsonSearchCount jsCountResponse = SerialisationHelper.deserializeJsonSearchCount(responseString);
            Assert.assertNotNull(jsCountResponse);
            Assert.assertEquals("Received number of annotations differs from expected value", expectedAnnotations, jsCountResponse.getCount());
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

    // verifies the editing ("update") and deletion permissions that are set to the annotation
    private void verifyPermissionEditDelete(final List<JsonAnnotation> jsAnnots, final String annotId, final ExpectedPermissions expectedPermEditDelete) {

        Assert.assertNotNull(jsAnnots);

        final JsonAnnotation jsAnnot = jsAnnots.stream().filter(jsAnn -> jsAnn.getId().equals(annotId)).findFirst().get();
        Assert.assertNotNull(jsAnnot.getPermissions());
        Assert.assertFalse(CollectionUtils.isEmpty(jsAnnot.getPermissions().getUpdate()));
        Assert.assertFalse(CollectionUtils.isEmpty(jsAnnot.getPermissions().getDelete()));

        final String updatePerm = jsAnnot.getPermissions().getUpdate().get(0);
        verifyPermission(updatePerm, expectedPermEditDelete);

        final String deletePerm = jsAnnot.getPermissions().getDelete().get(0);
        verifyPermission(deletePerm, expectedPermEditDelete);
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
