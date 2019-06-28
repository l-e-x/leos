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
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class SearchModelTest {

    private static final String uriString = "uri://LEOS/dummy_bill_for_test";
    private static final String dummySelector = "[{\"selector\":null,\"source\":\"" + uriString + "\"}]";
    private Group groupWorld, groupDigit, groupAgri, groupSg, groupEnv;
    private User digitWorldUser, agriWorldUser, sgWorldUser, envWorldSgUser, agriDigitUser;

    private static final String ANNOT_1 = "id1", ANNOT_2 = "id2", ANNOT_3 = "id3", ANNOT_4 = "id4";
    private static final String ANNOT_5 = "id5", ANNOT_6 = "id6", ANNOT_7 = "id7", ANNOT_8 = "id8";
    private static final String ANNOT_9 = "id9", ANNOT_10 = "id10", ANNOT_11 = "id11", ANNOT_12 = "id12";
    private static final String ANNOT_13 = "id13", ANNOT_14 = "id14", ANNOT_15 = "id15", ANNOT_16 = "id16";
    private static final String ANNOT_17 = "id17", ANNOT_18 = "id18", ANNOT_19 = "id19", ANNOT_20 = "id20";

    private final static String DIGIT = "DIGIT";
    private final static String AGRI = "AGRI";
    private final static String ENV = "ENV";
    private final static String MetaIscIdVers = "metadatasets=[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\"}]";
    private final static String MetaIscRespVers1 = "metadatasets=[%7B\"responseVersion\":\"1\"%7D]"; // encode curly brackets as otherwise, MockMvc considers the
                                                                                               // single entry as a variable
    private final static String MetaIscRespVers2 = "metadatasets=[%7B\"responseVersion\":\"2\"%7D]";
    private final static String MetaIscIdVersStatprep = "metadatasets=[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\", responseStatus: \"IN_PREPARATION\"}]";
    private final static String MetaIscIdVersStatsent = "metadatasets=[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\", responseStatus: \"SENT\"}]";

    private final Consumer<SimpleMetadata> setResponseSent = hashMap -> hashMap.put("responseStatus", "SENT");
    private final Consumer<SimpleMetadata> setResponseInPreparation = hashMap -> hashMap.put("responseStatus", "IN_PREPARATION");

    private final Consumer<SimpleMetadata> setIscData = hashMap -> {
        hashMap.put("responseId", "id-123");
        hashMap.put("ISCReference", "ISC/2018/00918");
    };

    private final Consumer<SimpleMetadata> setResponseVersion1 = (SimpleMetadata hashMap) -> hashMap.put("responseVersion", "1");
    private final Consumer<SimpleMetadata> setResponseVersion2 = (SimpleMetadata hashMap) -> hashMap.put("responseVersion", "2");

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

    private void createTestData() throws URISyntaxException {

        groupWorld = TestDbHelper.insertDefaultGroup(groupRepos);
        groupDigit = new Group(DIGIT, DIGIT, DIGIT, true);
        groupAgri = new Group(AGRI, AGRI, AGRI, false);
        groupSg = new Group("SG", "SG", "SG", true);
        groupEnv = new Group(ENV, ENV, ENV, true);
        groupRepos.save(Arrays.asList(groupDigit, groupAgri, groupSg, groupEnv));

        final String worldUserLogin = "userWorldOnly";
        final String digitWorldUserLogin = "digitAndWorld";
        final String agriWorldUserLogin = "agriAndWorld";
        final String agriDigitUserLogin = "digitAndAgri";
        final String sgWorldUserLogin = "sgAndWorld";
        final String envWorldUserLogin = "envAndWorld";

        // create users and assign them to their groups
        final User worldUser = new User(worldUserLogin);
        digitWorldUser = new User(digitWorldUserLogin);
        agriWorldUser = new User(agriWorldUserLogin);
        agriDigitUser = new User(agriDigitUserLogin);
        sgWorldUser = new User(sgWorldUserLogin);
        envWorldSgUser = new User(envWorldUserLogin);
        userRepos.save(Arrays.asList(worldUser, digitWorldUser, agriWorldUser, agriDigitUser, sgWorldUser, envWorldSgUser));

        // cache info for users in order to speed up test execution
        userDetailsCache.cache(worldUser.getLogin(), new UserDetails(worldUser.getLogin(), Long.valueOf(1), "world", "user1", "world", "", null));
        userDetailsCache.cache(digitWorldUser.getLogin(), new UserDetails(digitWorldUser.getLogin(), Long.valueOf(2), DIGIT, "user2", DIGIT, "", null));
        userDetailsCache.cache(agriWorldUser.getLogin(), new UserDetails(agriWorldUser.getLogin(), Long.valueOf(3), AGRI, "user3", AGRI, "", null));
        userDetailsCache.cache(sgWorldUser.getLogin(), new UserDetails(sgWorldUser.getLogin(), Long.valueOf(4), "sg", "user4", "SG", "", null));
        userDetailsCache.cache(envWorldSgUser.getLogin(), new UserDetails(envWorldSgUser.getLogin(), Long.valueOf(5), ENV, "user5", ENV, "", null));
        userDetailsCache.cache(agriDigitUser.getLogin(), new UserDetails(agriDigitUser.getLogin(), Long.valueOf(6), AGRI, "user6", AGRI, "", null));

        userGroupRepos.save(new UserGroup(worldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(digitWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(digitWorldUser.getId(), groupDigit.getId()));
        userGroupRepos.save(new UserGroup(agriWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(agriWorldUser.getId(), groupAgri.getId()));
        userGroupRepos.save(new UserGroup(agriDigitUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(agriDigitUser.getId(), groupAgri.getId()));
        userGroupRepos.save(new UserGroup(agriDigitUser.getId(), groupDigit.getId()));
        userGroupRepos.save(new UserGroup(sgWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(sgWorldUser.getId(), groupSg.getId()));

        // one user is member of three groups
        userGroupRepos.save(new UserGroup(envWorldSgUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(envWorldSgUser.getId(), groupEnv.getId()));
        userGroupRepos.save(new UserGroup(envWorldSgUser.getId(), groupSg.getId()));

        // create a document
        Document document = null;
        document = new Document(new URI(uriString), "title");
        documentRepos.save(document);

        final Metadata metaLeosWorld = new Metadata(document, groupWorld, Authorities.EdiT, null);
        final Metadata metaLeosDigit = new Metadata(document, groupDigit, Authorities.EdiT, getHashMapSentVers1());
        final Metadata metaLeosAgri = new Metadata(document, groupAgri, Authorities.EdiT, getHashMapInPrepVers2());
        final Metadata metaLeosEnv = new Metadata(document, groupEnv, Authorities.EdiT, null);
        final Metadata metaLeosSg = new Metadata(document, groupSg, Authorities.EdiT, null);
        metadataRepos.save(Arrays.asList(metaLeosWorld, metaLeosDigit, metaLeosAgri, metaLeosEnv, metaLeosSg));

        createAnnotation(ANNOT_1, worldUser, metaLeosWorld);
        createAnnotation(ANNOT_2, digitWorldUser, metaLeosDigit);
        createAnnotation(ANNOT_3, digitWorldUser, metaLeosWorld);
        createAnnotation(ANNOT_4, agriWorldUser, metaLeosAgri);
        createAnnotation(ANNOT_5, agriWorldUser, metaLeosWorld);
        createAnnotation(ANNOT_6, envWorldSgUser, metaLeosEnv);
        createAnnotation(ANNOT_7, envWorldSgUser, metaLeosWorld);
        createAnnotation(ANNOT_8, envWorldSgUser, metaLeosSg);
        createAnnotation(ANNOT_9, sgWorldUser, metaLeosSg);
        createAnnotation(ANNOT_10, sgWorldUser, metaLeosWorld);

        final Metadata metaIscDigit = new Metadata(document, groupDigit, Authorities.ISC, getHashMapSentVers1());
        final Metadata metaIscSg = new Metadata(document, groupSg, Authorities.ISC, getHashMapSentVers2());
        final Metadata metaIscAgri = new Metadata(document, groupAgri, Authorities.ISC, getHashMapInPrepVers1());
        final Metadata metaIscEnv = new Metadata(document, groupEnv, Authorities.ISC, getHashMapSentVers1());
        final Metadata metaIscWorld = new Metadata(document, groupWorld, Authorities.ISC, getHashMapInPrepVers2());
        metadataRepos.save(Arrays.asList(metaIscDigit, metaIscSg, metaIscAgri, metaIscEnv, metaIscWorld));

        createAnnotation(ANNOT_11, worldUser, metaIscWorld);
        createAnnotation(ANNOT_12, digitWorldUser, metaIscDigit);
        createAnnotation(ANNOT_13, digitWorldUser, metaIscWorld);
        createAnnotation(ANNOT_14, agriWorldUser, metaIscAgri);
        createAnnotation(ANNOT_15, agriWorldUser, metaIscWorld);
        createAnnotation(ANNOT_16, sgWorldUser, metaIscSg);
        createAnnotation(ANNOT_17, sgWorldUser, metaIscWorld);

        // envWorldSgUser has annotations in all his groups
        createAnnotation(ANNOT_18, envWorldSgUser, metaIscEnv);
        createAnnotation(ANNOT_19, envWorldSgUser, metaIscWorld);
        createAnnotation(ANNOT_20, envWorldSgUser, metaIscSg);
    }

    private void createAnnotation(final String annotId, final User user, final Metadata meta) {

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

        annotRepos.save(annot);
    }

    private SimpleMetadata getHashMapSentVers1() {
        return generateSimpleMetadata(setResponseSent, setResponseVersion1, setIscData);
    }

    private SimpleMetadata getHashMapSentVers2() {
        return generateSimpleMetadata(setResponseSent, setResponseVersion2, null);
    }

    private SimpleMetadata getHashMapInPrepVers1() {
        return generateSimpleMetadata(setResponseInPreparation, setResponseVersion1, setIscData);
    }

    private SimpleMetadata getHashMapInPrepVers2() {
        return generateSimpleMetadata(setResponseInPreparation, setResponseVersion2, null);
    }

    private SimpleMetadata generateSimpleMetadata(
            final Consumer<SimpleMetadata> status,
            final Consumer<SimpleMetadata> version,
            final Consumer<SimpleMetadata> iscData) {

        final SimpleMetadata hashMap = new SimpleMetadata();
        status.accept(hashMap);
        version.accept(hashMap);
        if (iscData != null) {
            iscData.accept(hashMap);
        }
        return hashMap;
    }

    // -------------------------------------
    // Tests for search model LEOS.1
    // -------------------------------------

    // search for DIGIT user in world group
    @Test
    public void searchModelLeos1_Digit() {

        executeRequest(digitWorldUser, Authorities.EdiT, groupWorld,
                "",
                Arrays.asList(ANNOT_1, ANNOT_3, ANNOT_5, ANNOT_7, ANNOT_10, // LEOS/world group (user is member)
                        ANNOT_2, // LEOS/DIGIT group (user is member)
                        ANNOT_12, // ISC/DIGIT group (SENT)
                        ANNOT_16, // ISC/SG group (SENT)
                        ANNOT_18, // ISC/ENV group (SENT)
                        ANNOT_20)); // ISC/SG group (SENT)
    }

    // search for AGRI user in world group
    @Test
    public void searchModelLeos1_Agri() {

        executeRequest(agriWorldUser, Authorities.EdiT, groupWorld,
                "",
                Arrays.asList(ANNOT_1, ANNOT_3, ANNOT_5, ANNOT_7, ANNOT_10, // LEOS/world group (user is member)
                        ANNOT_4, // LEOS/AGRI group (user is member)
                        ANNOT_12, // ISC/DIGIT group (SENT)
                        ANNOT_16, // ISC/SG group (SENT)
                        ANNOT_18, // ISC/ENV group (SENT)
                        ANNOT_20)); // ISC/SG group (SENT)
    }

    // search for SG/ENV user in world group
    @Test
    public void searchModelLeos1_EnvSg() {

        executeRequest(envWorldSgUser, Authorities.EdiT, groupWorld,
                "",
                Arrays.asList(ANNOT_1, ANNOT_3, ANNOT_5, ANNOT_7, ANNOT_10, // LEOS/world group (user is member)
                        ANNOT_6, // LEOS/ENV group (user is member)
                        ANNOT_8, // LEOS/SG group (user is member)
                        ANNOT_9, // LEOS/SG group (published by other user)
                        ANNOT_12, // ISC/DIGIT group (SENT)
                        ANNOT_16, // ISC/SG group (SENT)
                        ANNOT_18, // ISC/ENV group (SENT)
                        ANNOT_20)); // ISC/SG group (SENT)
    }

    // search for SG user in world group
    @Test
    public void searchModelLeos1_Sg() {

        executeRequest(sgWorldUser, Authorities.EdiT, groupWorld,
                "",
                Arrays.asList(ANNOT_1, ANNOT_3, ANNOT_5, ANNOT_7, ANNOT_10, // LEOS/world group (user is member)
                        ANNOT_8, ANNOT_9, // LEOS/SG group (user is member)
                        ANNOT_12, // ISC/DIGIT group (SENT)
                        ANNOT_16, // ISC/SG group (SENT)
                        ANNOT_18, // ISC/ENV group (SENT)
                        ANNOT_20)); // ISC/SG group (SENT)
    }

    // -------------------------------------
    // Tests for search model LEOS.2
    // -------------------------------------

    // search for DIGIT user in DIGIT group
    @Test
    public void searchModelLeos2_Digit() {

        executeRequest(digitWorldUser, Authorities.EdiT, groupDigit,
                "",
                Arrays.asList(ANNOT_2, ANNOT_12));
    }

    // search for DIGIT user in AGRI group
    @Test
    public void searchModelLeos2_Digit_GroupAgri() {

        executeRequest(digitWorldUser, Authorities.EdiT, groupAgri,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in SG group
    @Test
    public void searchModelLeos2_Digit_GroupSg() {

        executeRequest(digitWorldUser, Authorities.EdiT, groupSg,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in ENV group
    @Test
    public void searchModelLeos2_Digit_GroupEnv() {

        executeRequest(digitWorldUser, Authorities.EdiT, groupEnv,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in AGRI group
    @Test
    public void searchModelLeos2_Agri() {

        executeRequest(agriWorldUser, Authorities.EdiT, groupAgri,
                "",
                Arrays.asList(ANNOT_4)); // LEOS/AGRI group (user is member)
        // no matches in ISC as AGRI ISC annotations were not SENT yet
    }

    // search for AGRI user in DIGIT group
    @Test
    public void searchModelLeos2_Agri_GroupDigit() {

        executeRequest(agriWorldUser, Authorities.EdiT, groupDigit,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in ENV group
    @Test
    public void searchModelLeos2_Agri_GroupEnv() {

        executeRequest(agriWorldUser, Authorities.EdiT, groupEnv,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in SG group
    @Test
    public void searchModelLeos2_Agri_GroupSg() {

        executeRequest(agriWorldUser, Authorities.EdiT, groupSg,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in SG group
    @Test
    public void searchModelLeos2_Sg() {

        executeRequest(sgWorldUser, Authorities.EdiT, groupSg,
                "",
                Arrays.asList(ANNOT_8,// LEOS/SG group (published by other user)
                        ANNOT_9, // LEOS/SG group (user is member)
                        ANNOT_16, // ISC/SG group (user is member), SENT
                        ANNOT_20)); // ISC/SG group (publichsed by other user), SENT
    }

    // search for SG user in AGRI group
    @Test
    public void searchModelLeos2_Sg_GroupAgri() {

        executeRequest(sgWorldUser, Authorities.EdiT, groupAgri,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in DIGIT group
    @Test
    public void searchModelLeos2_Sg_GroupDigit() {

        executeRequest(sgWorldUser, Authorities.EdiT, groupDigit,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in ENV group
    @Test
    public void searchModelLeos2_Sg_GroupEnv() {

        executeRequest(sgWorldUser, Authorities.EdiT, groupEnv,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG/ENV user in SG group
    @Test
    public void searchModelLeos2_EnvSg_GroupSg() {

        executeRequest(envWorldSgUser, Authorities.EdiT, groupSg,
                "",
                Arrays.asList(ANNOT_8,// LEOS/SG group (published by other user)
                        ANNOT_9, // LEOS/SG group (user is member)
                        ANNOT_16, // ISC/SG group (user is member), SENT
                        ANNOT_20)); // ISC/SG group (publichsed by other user), SENT
    }

    // search for SG/ENV user in ENV group
    @Test
    public void searchModelLeos2_EnvSg_GroupEnv() {

        executeRequest(envWorldSgUser, Authorities.EdiT, groupEnv,
                "",
                Arrays.asList(ANNOT_6,// LEOS/ENV group (user is member)
                        ANNOT_18)); // ISC/ENV group (user is member), SENT
    }

    // search for SG/ENV user in AGRI group
    @Test
    public void searchModelLeos2_EnvSg_GroupAgri() {

        executeRequest(envWorldSgUser, Authorities.EdiT, groupAgri,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG/ENV user in DIGIT group
    @Test
    public void searchModelLeos2_EnvSg_GroupDigit() {

        executeRequest(envWorldSgUser, Authorities.EdiT, groupDigit,
                "",
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // -------------------------------------
    // Tests for search model ISC.1
    // -------------------------------------

    // search for DIGIT user in DIGIT group
    @Test
    public void searchModelIsc1_Digit() {

        executeRequest(digitWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVers,
                Arrays.asList(ANNOT_12)); // ISC/DIGIT group (user is member)
    }

    // search for DIGIT user in AGRI group
    @Test
    public void searchModelIsc1_Digit_GroupAgri() {

        executeRequest(digitWorldUser, Authorities.ISC, groupAgri,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in ENV group
    @Test
    public void searchModelIsc1_Digit_GroupEnv() {

        executeRequest(digitWorldUser, Authorities.ISC, groupEnv,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in SG group
    @Test
    public void searchModelIsc1_Digit_GroupSg() {

        executeRequest(digitWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in AGRI group
    @Test
    public void searchModelIsc1_Agri() {

        executeRequest(agriWorldUser, Authorities.ISC, groupAgri,
                MetaIscIdVers,
                Arrays.asList(ANNOT_14)); // ISC/AGRI group (user is member)
    }

    // search for AGRI user in DIGIT group
    @Test
    public void searchModelIsc1_Agri_GroupDigit() {

        executeRequest(agriWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in ENV group
    @Test
    public void searchModelIsc1_Agri_GroupEnv() {

        executeRequest(agriWorldUser, Authorities.ISC, groupEnv,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in SG group
    @Test
    public void searchModelIsc1_Agri_GroupSg() {

        executeRequest(agriWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI-DIGIT user in AGRI group
    @Test
    public void searchModelIsc1_AgriDigit_GroupAgri() {

        executeRequest(agriDigitUser, Authorities.ISC, groupAgri,
                MetaIscIdVers,
                Arrays.asList(ANNOT_14)); // ISC/AGRI group (user is member)
    }

    // search for AGRI-DIGIT user in DIGIT group
    @Test
    public void searchModelIsc1_AgriDigit_GroupDigit() {

        executeRequest(agriDigitUser, Authorities.ISC, groupDigit,
                MetaIscIdVers,
                Arrays.asList(ANNOT_12)); // ISC/DIGIT group (user is member)
    }

    // search for AGRI-DIGIT user in ENV group
    @Test
    public void searchModelIsc1_AgriDigit_GroupEnv() {

        executeRequest(agriDigitUser, Authorities.ISC, groupEnv,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI-DIGIT user in SG group
    @Test
    public void searchModelIsc1_AgriDigit_GroupSg() {

        executeRequest(agriDigitUser, Authorities.ISC, groupSg,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in SG group
    @Test
    public void searchModelIsc1_Sg() {

        executeRequest(sgWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVers,
                new ArrayList<String>()); // "responseId" and "ISCReference" not set
    }

    // search for SG user in SG group - but for response version only
    @Test
    public void searchModelIsc1_Sg_RespVers2() {

        executeRequest(sgWorldUser, Authorities.ISC, groupSg,
                MetaIscRespVers2,
                Arrays.asList(ANNOT_16, ANNOT_20)); // annotations by sgWorldUser and envWorldSgUser
    }

    // search for SG user in AGRI group
    @Test
    public void searchModelIsc1_Sg_GroupAgri() {

        executeRequest(sgWorldUser, Authorities.ISC, groupAgri,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in DIGIT group
    @Test
    public void searchModelIsc1_Sg_GroupDigit() {

        executeRequest(sgWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in ENV group
    @Test
    public void searchModelIsc1_Sg_GroupEnv() {

        executeRequest(sgWorldUser, Authorities.ISC, groupEnv,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for ENV-SG user in SG group
    @Test
    public void searchModelIsc1_EnvSg_GroupSg() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupSg,
                MetaIscIdVers,
                new ArrayList<String>()); // "responseId" and "ISCReference" not set for ISC/SG group annotations
    }

    // search for ENV-SG user in ENV group
    @Test
    public void searchModelIsc1_EnvSg_GroupEnv() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupEnv,
                MetaIscIdVers,
                Arrays.asList(ANNOT_18)); // ISC/ENV group (user is member)
    }

    // search for ENV-SG user in ENV group - for response version only
    @Test
    public void searchModelIsc1_EnvSg_GroupEnv_RespVersion1() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupEnv,
                MetaIscRespVers1,
                Arrays.asList(ANNOT_18)); // ISC/ENV group (user is member)
    }

    // search for ENV-SG user in AGRI group
    @Test
    public void searchModelIsc1_EnvSg_GroupAgri() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupAgri,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for ENV-SG user in DIGIT group
    @Test
    public void searchModelIsc1_EnvSg_GroupDigit() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupDigit,
                MetaIscIdVers,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // -------------------------------------
    // Tests for search model ISC.2
    // -------------------------------------

    // search for AGRI user in AGRI group
    @Test
    public void searchModelIsc2_Agri() {

        executeRequest(agriWorldUser, Authorities.ISC, groupAgri,
                MetaIscIdVersStatprep,
                Arrays.asList(ANNOT_14)); // ISC/AGRI group (user is member)
    }

    // search for AGRI user in DIGIT group
    @Test
    public void searchModelIsc2_Agri_GroupDigit() {

        executeRequest(agriWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in SG group
    @Test
    public void searchModelIsc2_Agri_GroupSg() {

        executeRequest(agriWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for AGRI user in ENV group
    @Test
    public void searchModelIsc2_Agri_GroupEnv() {

        executeRequest(agriWorldUser, Authorities.ISC, groupEnv,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in DIGIT group
    @Test
    public void searchModelIsc2_Digit() {

        executeRequest(digitWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // metadata "responseStatus" does not match (is: SENT)
        tokenRepos.deleteAll();

        // search again with matching metadata (responseStatus)
        executeRequest(digitWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVersStatsent,
                Arrays.asList(ANNOT_12)); // ISC/DIGIT group (user is member)
        tokenRepos.deleteAll();

        // search again without response status
        executeRequest(digitWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVers,
                Arrays.asList(ANNOT_12)); // ISC/DIGIT group (user is member)
    }

    // search for DIGIT user in AGRI group
    @Test
    public void searchModelIsc2_Digit_GroupAgri() {

        executeRequest(digitWorldUser, Authorities.ISC, groupAgri,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in ENV group
    @Test
    public void searchModelIsc2_Digit_GroupEnv() {

        executeRequest(digitWorldUser, Authorities.ISC, groupEnv,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for DIGIT user in SG group
    @Test
    public void searchModelIsc2_Digit_GroupSg() {

        executeRequest(digitWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in SG group
    @Test
    public void searchModelIsc2_Sg() {

        executeRequest(sgWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // metadata "responseStatus" does not match (is: SENT)
        tokenRepos.deleteAll();

        // search again with matching metadate "responseStatus", but others still missing
        executeRequest(sgWorldUser, Authorities.ISC, groupSg,
                MetaIscIdVersStatsent,
                new ArrayList<String>()); // no fully matching metadata
    }

    // search for SG user in AGRI group
    @Test
    public void searchModelIsc2_Sg_GroupAgri() {

        executeRequest(sgWorldUser, Authorities.ISC, groupAgri,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in DIGIT group
    @Test
    public void searchModelIsc2_Sg_GroupDigitgri() {

        executeRequest(sgWorldUser, Authorities.ISC, groupDigit,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for SG user in ENV group
    @Test
    public void searchModelIsc2_Sg_GroupEnv() {

        executeRequest(sgWorldUser, Authorities.ISC, groupEnv,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for ENV user in ENV group
    @Test
    public void searchModelIsc2_Env_GroupEnv() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupEnv,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // metadata "responseStatus" does not match (is: SENT)
        tokenRepos.deleteAll();

        executeRequest(envWorldSgUser, Authorities.ISC, groupEnv,
                MetaIscIdVersStatsent,
                Arrays.asList(ANNOT_18)); // ISC/ENV group (user is member)
    }

    // search for ENV user in ENV group - for response version 1 (ok)
    @Test
    public void searchModelIsc2_Env_GroupEnv_RespVers1() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupEnv,
                MetaIscRespVers1,
                Arrays.asList(ANNOT_18)); // ISC/ENV group (user is member, response version matches)
    }

    // search for ENV user in ENV group - for response version 2 (not ok)
    @Test
    public void searchModelIsc3_Env_GroupEnv_RespVers2() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupEnv,
                MetaIscRespVers2,
                new ArrayList<String>()); // ISC/ENV group (user is member, but response version doesn't match)
    }

    // search for ENV user in SG group
    @Test
    public void searchModelIsc2_Env_GroupSg() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupSg,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // metadata "responseStatus" does not match in SG group (is: SENT), others are not defined (e.g. ISCReference)
    }

    // search for ENV user in AGRI group
    @Test
    public void searchModelIsc2_Env_GroupAgri() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupAgri,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // search for ENV user in DIGIT group
    @Test
    public void searchModelIsc2_Env_GroupDigit() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupDigit,
                MetaIscIdVersStatprep,
                new ArrayList<String>()); // no annotation as user is not member of group
    }

    // -------------------------------------
    // Tests for search model ISC.3
    // -------------------------------------

    // search for DIGIT user in world group
    @Test
    public void searchModelIsc3_Digit() {

        executeRequest(digitWorldUser, Authorities.ISC, groupWorld,
                MetaIscIdVersStatsent,
                Arrays.asList(ANNOT_12)); // ISC/DIGIT group (user is member); ISC/world has different responseStatus that does not match
    }

    // search for AGRI user in world group
    @Test
    public void searchModelIsc3_Agri() {

        executeRequest(agriWorldUser, Authorities.ISC, groupWorld,
                MetaIscIdVersStatsent,
                new ArrayList<String>()); // ISC/AGRI and ISC/world both have different responseStatus that does not match
        tokenRepos.deleteAll();

        executeRequest(agriWorldUser, Authorities.ISC, groupWorld,
                MetaIscIdVersStatprep,
                Arrays.asList(ANNOT_14)); // ISC/AGRI matches with responseStatus
    }

    // search for AGRI-DIGIT user in AGRI group
    @Test
    public void searchModelIsc3_AgriDigit() {

        executeRequest(agriDigitUser, Authorities.ISC, groupWorld,
                MetaIscRespVers1,
                Arrays.asList(ANNOT_12, ANNOT_14)); // annotations from AGRI and DIGIT group (each 1)
    }

    // search for ENV user in world group
    @Test
    public void searchModelIsc3_Env() {

        executeRequest(envWorldSgUser, Authorities.ISC, groupWorld,
                MetaIscIdVersStatsent,
                Arrays.asList(ANNOT_18)); // ISC/ENV group (user is member); ISC/world and ISC/SG both have different metadata that does not match
    }

    // search for SG user in world group
    @Test
    public void searchModelIsc3_Sg() {

        executeRequest(sgWorldUser, Authorities.ISC, groupWorld,
                MetaIscIdVersStatsent,
                new ArrayList<String>()); // ISC/SG has different metadata that does not match
    }

    // search for SG user in world group - but only for responseVersion
    @Test
    public void searchModelIsc3_Sg_RespVersion2() {

        executeRequest(sgWorldUser, Authorities.ISC, groupWorld,
                MetaIscRespVers2,
                Arrays.asList(ANNOT_16, ANNOT_20, // SG group (user is member, response version 2)
                        ANNOT_11, ANNOT_13, ANNOT_15, ANNOT_17, ANNOT_19)); // world group, response version 2
    }

    // -------------------------------------
    // internal function that does the actual job
    // -------------------------------------
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private void executeRequest(final User user, final String authority, final Group group, final String metadataRequest, final List<String> expectedAnnotIds) {

        final Token userToken = new Token(user, authority, "@cc", LocalDateTime.now().plusMinutes(5), "refr", LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(userToken);

        try {
            final String assembledUrl = "/api/search?_separate_replies=false&sort=created&order=asc&uri=" + uriString + "&group=" + group.getName() +
                    "&" + metadataRequest;

            // send search request
            final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                    .get(assembledUrl)
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
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e.getMessage());
        }
    }

}
