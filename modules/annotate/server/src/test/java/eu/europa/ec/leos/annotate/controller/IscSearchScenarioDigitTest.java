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
import eu.europa.ec.leos.annotate.model.*;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocumentLink;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonDeleteSuccessResponse;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.status.StatusUpdateSuccessResponse;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
import org.assertj.core.api.StringAssert;
import org.javatuples.Pair;
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
public class IscSearchScenarioDigitTest {

    /**
     * class for testing the individual steps of the evaluation sheet of DIGIT
     * including visibility of items from different DGs and permissions
     */

    private static final String uriString = "uri://LEOS/dummy_bill_for_test";
    private static final String IscRef = "ISCReference";
    private static final String IscRefVal = "ISC/2019/4";
    private static final String UnexpExc = "Unexpected exception: ";
    private Group groupWorld, groupDigit;
    private User sgUser, sjUser;
    private Token agriToken, sgToken, sjToken, editToken;

    private enum ExpectedPermissions {
        ALL, USER, NONE
    }

    private final static String AGRI = "AGRI";
    @SuppressWarnings("PMD.ShortVariable")
    private final static String SG = "SG";
    @SuppressWarnings("PMD.ShortVariable")
    private final static String SJ = "SJ";

    // annotation IDs
    private static final String ANNOT_SG_1 = "Annot_SG_1";
    private static final String ANNOT_SG_2 = "Annot_SG_2";
    private static final String ANNOT_SJ = "Annot_SJ";
    private static final String ANNOT_SG_1_CHANGED = "Annot_SG_1_changed";
    private String idAnnot_SG_1, idAnnot_SG_2, idAnnot_SJ, idAnnot_SG_1_changed;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

    @Autowired
    private AnnotationConversionService annotConvService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

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

        groupWorld = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group groupAgri = new Group(AGRI, AGRI, AGRI, false);
        final Group groupSg = new Group(SG, SG, SG, true);
        final Group groupSj = new Group(SJ, SJ, SJ, true);
        groupDigit = new Group("DIGIT", true);
        groupRepos.save(Arrays.asList(groupAgri, groupSg, groupSj, groupDigit));

        final String agriWorldUserLogin = "agriAndWorld";
        final String sgWorldUserLogin = "sgAndWorld";
        final String sjWorldUserLogin = "sjAndWorld";

        // create users and assign them to their groups
        final User agriUser = new User(agriWorldUserLogin);
        sgUser = new User(sgWorldUserLogin);
        sjUser = new User(sjWorldUserLogin);
        userRepos.save(Arrays.asList(agriUser, sgUser, sjUser));

        final List<UserEntity> entitiesAgri = Arrays.asList(new UserEntity("2", AGRI, AGRI));
        final List<UserEntity> entitiesSg = Arrays.asList(new UserEntity("3", "SG", "SG"));
        final List<UserEntity> entitiesSj = Arrays.asList(new UserEntity("4", "SJ", "SJ"));

        // cache info for users in order to speed up test execution
        userDetailsCache.cache(agriUser.getLogin(), new UserDetails(agriUser.getLogin(), Long.valueOf(2), AGRI, "user2", entitiesAgri, "", null));
        userDetailsCache.cache(sgUser.getLogin(), new UserDetails(sgUser.getLogin(), Long.valueOf(3), SG, "user3", entitiesSg, "", null));
        userDetailsCache.cache(sjUser.getLogin(), new UserDetails(sjUser.getLogin(), Long.valueOf(4), SJ, "user4", entitiesSj, "", null));

        userGroupRepos.save(new UserGroup(agriUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(agriUser.getId(), groupAgri.getId()));
        userGroupRepos.save(new UserGroup(agriUser.getId(), groupDigit.getId()));
        userGroupRepos.save(new UserGroup(sgUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(sgUser.getId(), groupSg.getId()));
        userGroupRepos.save(new UserGroup(sgUser.getId(), groupDigit.getId()));
        userGroupRepos.save(new UserGroup(sjUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(sjUser.getId(), groupSj.getId()));
        userGroupRepos.save(new UserGroup(sjUser.getId(), groupDigit.getId()));

        agriToken = new Token(agriUser, Authorities.ISC, "agriA", LocalDateTime.now().plusMinutes(5), "agriR", LocalDateTime.now().plusMinutes(5));
        sgToken = new Token(sgUser, Authorities.ISC, "sgA", LocalDateTime.now().plusMinutes(5), "sgR", LocalDateTime.now().plusMinutes(5));
        sjToken = new Token(sjUser, Authorities.ISC, "sjA", LocalDateTime.now().plusMinutes(5), "sjR", LocalDateTime.now().plusMinutes(5));
        editToken = new Token(agriUser, Authorities.EdiT, "editA", LocalDateTime.now().plusMinutes(5), "editR", LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(Arrays.asList(agriToken, sgToken, sjToken, editToken));
    }

    private JsonAnnotation createAnnotation(final String annotId, final User user, final Group group,
            final String responseId, final int responseVersion, final Token token) {

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(user.getLogin());
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

    // -------------------------------------
    // Test steps
    // -------------------------------------

    /**
     * step 1: SG adds Annot_SG_1 and Annot_SG_2
     */
    private void executeStep1() {

        final JsonAnnotation jsAnnotSg1 = createAnnotation(ANNOT_SG_1, sgUser, groupDigit, SG, 1, sgToken); // posted to DIGIT!
        idAnnot_SG_1 = jsAnnotSg1.getId();

        idAnnot_SG_2 = createAnnotation(ANNOT_SG_2, sgUser, groupDigit, SG, 1, sgToken).getId(); // posted to DIGIT!
    }

    /**
     * step 2: SG sends the answer
     */
    private void executeStep2() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = getMetadataForQuery(1, SG);
        metaToMatch.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaToMatch);
        executeStatusUpdateRequest(groupDigit, uriString, serializedMetadataToMatch, sgToken); // update DIGIT annots
    }

    /**
     * step 3: SJ adds Annot_SJ
     */
    private void executeStep3() {

        final JsonAnnotation jsAnnotSj = createAnnotation(ANNOT_SJ, sjUser, groupDigit, SJ, 1, sjToken); // posted to DIGIT (?)
        idAnnot_SJ = jsAnnotSj.getId();
    }

    /**
     * step 4: SG edits Annot_SG_1 (-> creates Annot_SG_1_changed)
     */
    private void executeStep4() throws CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        final UserInformation sgUserInfo = new UserInformation(sgToken);

        final Annotation annotSg1 = annotService.findAnnotationById(idAnnot_SG_1, sgUser.getLogin());
        final JsonAnnotation jsAnnotSg1 = annotConvService.convertToJsonAnnotation(annotSg1, sgUserInfo);
        jsAnnotSg1.setText(ANNOT_SG_1_CHANGED);

        final SimpleMetadata meta = jsAnnotSg1.getDocument().getMetadata();
        meta.put(Metadata.PROP_RESPONSE_VERSION, Integer.toString(2));
        jsAnnotSg1.getDocument().setMetadata(meta);
        jsAnnotSg1.setUpdated(LocalDateTime.now());

        idAnnot_SG_1_changed = annotService.updateAnnotation(idAnnot_SG_1, jsAnnotSg1, sgUserInfo).getId();
    }

    /**
     * step 5: SG sends the answer
     */
    private void executeStep5() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = getMetadataForQuery(2, SG);
        metaToMatch.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaToMatch);
        executeStatusUpdateRequest(groupDigit, uriString, serializedMetadataToMatch, sgToken); // send with DIGIT group!
    }

    /**
     * step 6: SG deletes Annot_SG_2
     */
    private void executeStep6() throws CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        final UserInformation sgUserInfo = new UserInformation(sgToken);
        annotService.deleteAnnotationById(idAnnot_SG_2, sgUserInfo);
    }

    /**
     * step 7: SG sends the answer
     */
    private void executeStep7() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = getMetadataForQuery(3, SG);
        metaToMatch.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaToMatch);
        executeStatusUpdateRequest(groupDigit, uriString, serializedMetadataToMatch, sgToken); // send as DIGIT!
    }

    private void sendSgVersion4() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = getMetadataForQuery(4, SG);
        metaToMatch.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaToMatch);
        executeStatusUpdateRequest(groupDigit, uriString, serializedMetadataToMatch, sgToken);// send as DIGIT!
    }

    /**
     * step 8: SJ sends the answer
     */
    private void executeStep8() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = getMetadataForQuery(1, SJ);
        metaToMatch.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final String serializedMetadataToMatch = SerialisationHelper.serialize(metaToMatch);
        executeStatusUpdateRequest(groupDigit, uriString, serializedMetadataToMatch, sjToken); // send as DIGIT!
    }

    /**
     * step 9: AGRI deletes Annot_SJ in EdiT
     */
    private void executeStep9() throws JsonProcessingException {

        executeDeleteRequest(idAnnot_SJ, editToken);
    }

    // -------------------------------------
    // Tests step 1
    // -------------------------------------

    // AGRI cannot yet see anything in ISC as nothing was SENT yet - so this test case is not relevant
    /*@Test
    public void testStep1Agri() throws JsonProcessingException {
    
        executeStep1();
        
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, "");
        verifyFoundAnnots(foundAnnots, new ArrayList<String>());
    }*/

    @Test
    public void testStep1Sg_Context1() throws JsonProcessingException {

        executeStep1();

        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    // SJ cannot yet see anything in ISC as nothing was SENT yet - so this test case is not relevant
    /*@Test
    public void testStep1Sj() {
    
        executeStep1();
    
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, "");
        verifyFoundAnnots(foundAnnots, new ArrayList<String>());
    }*/

    @Test
    public void testStep1Edit() throws JsonProcessingException {

        executeStep1();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, new ArrayList<String>());
    }

    // -------------------------------------
    // Tests step 2
    // -------------------------------------

    @Test
    public void testStep2Agri_context2() throws JsonProcessingException {

        executeStep1();
        executeStep2();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep2Sg_context2() throws JsonProcessingException {

        executeStep1();
        executeStep2();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep2Sj_context2() throws JsonProcessingException {

        executeStep1();
        executeStep2();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep2Edit() throws JsonProcessingException {

        executeStep1();
        executeStep2();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 3
    // -------------------------------------

    @Test
    public void testStep3Agri_context2() throws JsonProcessingException {

        executeStep1();
        executeStep2();
        executeStep3();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep3Sg_context2() throws JsonProcessingException {

        executeStep1();
        executeStep2();
        executeStep3();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        // user must have permissions on his own annotations
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep3Sj_context2() throws JsonProcessingException {

        executeStep1();
        executeStep2();
        executeStep3();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep3Sj_context3() throws JsonProcessingException {

        executeStep1();
        executeStep2();
        executeStep3();

        // consult SG's V1 answer
        // and SJ's V1 items being IN_PREPARATION
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep3Edit() throws JsonProcessingException {

        executeStep1();
        executeStep2();
        executeStep3();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 4
    // -------------------------------------

    @Test
    public void testStep4Agri_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep4Sg_context1()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();

        // consult SG's V2 items being IN_PREPARATION
        // and SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(2, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_2, idAnnot_SG_1_changed));

        // user must have permissions on his own annotations
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep4Sg_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        // user must have permissions on his own annotations
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep4Sj_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();

        // consult SG's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep4Sj_context3()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();

        // consult SG's V1 answer
        // and SJ's V1 items being IN_PREPARATION
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep4Edit()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 5
    // -------------------------------------

    @Test
    public void testStep5Agri_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();

        // consult SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed));

        // note: Annot_SG_1 is visible due to asking for V1 items
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep5Sg_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();

        // consult SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own annotation, deleted
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep5Sj_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();

        // consult SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot, deleted
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep5Sj_context3()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();

        // consult SG's V2 answer (including V1)
        // and SJ's V1 items being IN_PREPARATION
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed, idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot, deleted
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep5Edit()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_2, idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 6
    // -------------------------------------

    @Test
    public void testStep6Agri_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        // consult SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed));

        // note: Annot_SG_2 is still visible since SG did not yet send the answer
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep6Sg_context1()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        // consult SG's V3 items being IN_PREPARATION
        // and SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(2, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own annotation, deleted
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep6Sg_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        // consult SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed));

        // note: Annot_SG_2 is returned since we ask for a responseVersion (<=2) in which it was not deleted yet
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own anntation, deleted
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep6Sj_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        // consult SG's V2 answer (including V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep6Sj_context3()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        // consult SG's V2 answer (including V1)
        // and SJ's V1 items being IN_PREPARATION
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_2, idAnnot_SG_1_changed, idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep6Edit()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_2, idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 7
    // -------------------------------------

    @Test
    public void testStep7Agri_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        // note: Annot_SG_2 is still returned although having been deleted since we ask for all statuses
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep7Sg_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        // note: Annot_SG_2 is still returned although having been deleted since we ask for all statuses
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own annot, deleted
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep7Sj_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        // note: Annot_SG_2 is still returned although having been deleted since we ask for all statuses
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep7Sj_context3()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();

        // consult SG's V3 answer (including V2 and V1)
        // and SJ's V1 items being IN_PREPARATION
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG),
                new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2, idAnnot_SJ));

        // note: Annot_SG_2 is still returned although having been deleted since we ask for all statuses
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    // step 6 is modified such that another annotation is also deleted
    // -> verify that both deleted annotations are correctly assigned to the same responseVersion
    @Test
    public void testStep7Sg_context2_modifiedStep6()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();

        // delete another annotation
        final UserInformation sgUserInfo = new UserInformation(sgToken);
        annotService.deleteAnnotationById(idAnnot_SG_1_changed, sgUserInfo);

        executeStep7();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        // Annot_SG_1_changed and Annot_SG_2 are visible since we ask for all versions and statuses
        verifySentDeletedAndDeleted(idAnnot_SG_1_changed, 3);
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);
    }

    // test with deleting another annotation after step 7 and SENDing it
    // -> verify that deleted annotation is assigned to the correct responseVersion
    @Test
    public void testStep7Sg_context2_deleteAgainAfterStep7()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6(); // only deletes one annotation
        executeStep7(); // sends it

        // delete another annotation (Annot_SG_1_changed)
        final UserInformation sgUserInfo = new UserInformation(sgToken);
        annotService.deleteAnnotationById(idAnnot_SG_1_changed, sgUserInfo);

        // and send
        sendSgVersion4();

        // consult SG's V4 answer (including V3, V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(4, SG),
                new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2)); 
        // Annot_SG_1_changed and Annot_SG_2 are visible since we ask for all version and statuses

        // verify that each annotation is properly assigned to the correct responseVersion
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);
        verifySentDeletedAndDeleted(idAnnot_SG_1_changed, 4);
    }

    @Test
    public void testStep7Edit()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 8
    // -------------------------------------

    @Test
    public void testStep8Agri_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        // note: produces same result also when not giving any metadata
        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        // note: Annot_SG_2 is still returned although having been deleted since we ask for all statuses
        verifySentDeletedAndDeleted(idAnnot_SG_2, 3);

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep8Agri_context3()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        // consult SJ's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(agriToken, AGRI, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep8Sg_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        // Annot_SG_2 is shown since historical versions are requested
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own annot, deleted (since updated)
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own annot, deleted
    }

    @Test
    public void testStep8Sg_context4()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        // consult SJ's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sgToken, SG, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep8Sj_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        // consult SG's V3 answer (including V2 and V1)
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(3, SG),
                new Pair<Integer, String>(2, SG),
                new Pair<Integer, String>(1, SG)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1, idAnnot_SG_1_changed, idAnnot_SG_2));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_2, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // foreign annot
    }

    @Test
    public void testStep8Sj_context4()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        // consult SJ's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.USER, ExpectedPermissions.USER); // own annotation
    }

    @Test
    public void testStep8Edit()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1_changed, idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // Tests step 9
    // -------------------------------------

    @Test
    public void testStep9Agri_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        testStep8Agri_context2(); // identical
    }

    @Test
    public void testStep9Agri_context3()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        testStep8Agri_context3(); // identical; Annot_SJ was deleted, but has same permissions
    }

    @Test
    public void testStep9Sg_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        testStep8Sg_context2(); // identical
    }

    @Test
    public void testStep9Sg_context4()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        testStep8Sg_context4(); // identical; Annot_SJ was deleted, but has same permissions
    }

    @Test
    public void testStep9Sj_context2()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        testStep8Sj_context2(); // identical
    }

    @Test
    public void testStep9Sj_context4()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();
        executeStep9();

        // consult SJ's V1 answer
        final String serialisedMetadata = getSearchMetadata(Arrays.asList(new Pair<Integer, String>(1, SJ)));

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(sjToken, SJ, serialisedMetadata);
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SJ));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SJ, ExpectedPermissions.NONE, ExpectedPermissions.NONE); // own annotation, deleted
    }

    @Test
    public void testStep9Edit()
            throws JsonProcessingException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException,
            CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        executeStep1();
        executeStep2();
        executeStep3();
        executeStep4();
        executeStep5();
        executeStep6();
        executeStep7();
        executeStep8();
        executeStep9();

        final List<JsonAnnotation> foundAnnots = executeSearchRequest(editToken, groupWorld.getName(), getSearchMetadataEdit());
        verifyFoundAnnots(foundAnnots, Arrays.asList(idAnnot_SG_1_changed));

        verifyPermissionEditDelete(foundAnnots, idAnnot_SG_1_changed, ExpectedPermissions.NONE, ExpectedPermissions.ALL);
    }

    // -------------------------------------
    // internal helper functions
    // -------------------------------------

    // assembles the String of serialised metadata for ISC search
    private String getSearchMetadata(final List<Pair<Integer, String>> metaPairs) throws JsonProcessingException {

        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        for (final Pair<Integer, String> metaPair : metaPairs) {

            final SimpleMetadata metaHelp = getMetadataForQuery(metaPair.getValue0(), metaPair.getValue1());
            requestedMetadata.add(new SimpleMetadataWithStatuses(metaHelp, Arrays.asList(AnnotationStatus.ALL)));
        }

        return SerialisationHelper.serializeSimpleMetadataWithStatusesList(requestedMetadata);
    }

    // assembles the String of serialised metadata for EdiT search
    private String getSearchMetadataEdit() throws JsonProcessingException {

        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        final SimpleMetadata metaHelp = new SimpleMetadata(IscRef, IscRefVal);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaHelp, Arrays.asList(AnnotationStatus.NORMAL)));

        return SerialisationHelper.serializeSimpleMetadataWithStatusesList(requestedMetadata);
    }

    // run a search
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private List<JsonAnnotation> executeSearchRequest(final Token userToken, final String connectedEntity, final String metadatasets) {

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

    private JsonDeleteSuccessResponse executeDeleteRequest(final String annotId, final Token token) {

        try {
            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.delete("/api/annotations/" + annotId)
                    .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + token.getAccessToken());

            final ResultActions result = this.mockMvc.perform(builder);

            // expected: Http 200
            result.andExpect(MockMvcResultMatchers.status().isOk());

            final MvcResult resultContent = result.andReturn();
            final String responseString = resultContent.getResponse().getContentAsString();

            // ID must have been set
            final JsonDeleteSuccessResponse jsResponse = SerialisationHelper.deserializeJsonDeleteSuccessResponse(responseString);
            Assert.assertNotNull(jsResponse);

            return jsResponse;
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

    private void verifySentDeletedAndDeleted(final String annotId, final long sentDeletedVersion) {

        final Annotation annot = annotRepos.findById(annotId);
        Assert.assertNotNull(annot);
        Assert.assertTrue(annot.isSentDeleted());
        Assert.assertEquals(AnnotationStatus.DELETED, annot.getStatus());
        Assert.assertEquals(sentDeletedVersion, annot.getRespVersionSentDeleted());
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
