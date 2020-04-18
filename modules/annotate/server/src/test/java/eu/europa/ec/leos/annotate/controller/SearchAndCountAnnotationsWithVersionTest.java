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
import eu.europa.ec.leos.annotate.model.*;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class SearchAndCountAnnotationsWithVersionTest {

    /**
     * test combinations of searching for different metadata sets and different statuses at the same time  
     */

    private static final String VERSION = "version";

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
    private DocumentRepository documentRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private UserDetailsCache userDetailsCache;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    private final static String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refr";
    private static final String AUTHORITY = Authorities.ISC; // authority used for most tests; tests with EdiT authority change it in the DB
    private final static String LOGIN1 = "demo";
    private static final String ISCREF = "ISCReference";
    private static final String ISCREF1 = "ISC/2018/1";
    private static final String ISCREF2 = "ISC/2018/2";
    private static final String RESPVERS = "responseVersion";
    private static final String GROUPNAME = "mygroup";
    private UserInformation userInfo;
    private Group theGroup;
    private Document document;

    private final Consumer<SimpleMetadata> setIscRef1 = hashMap -> hashMap.put(ISCREF, ISCREF1);
    private final Consumer<SimpleMetadata> setIscRef2 = hashMap -> hashMap.put(ISCREF, ISCREF2);
    private final Consumer<SimpleMetadata> setRespVers1 = hashMap -> hashMap.put(RESPVERS, "1");
    private final Consumer<SimpleMetadata> setRespVers2 = hashMap -> hashMap.put(RESPVERS, "2");

    private Annotation ann1, ann2, ann3, ann4, ann5, ann6;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws URISyntaxException {

        TestDbHelper.cleanupRepositories(this);
        final Group defGroup = TestDbHelper.insertDefaultGroup(groupRepos); // used for EdiT queries
        theGroup = new Group(GROUPNAME, true);// note: we do not use the __world__ group for our ISC queries, as it would use a special search model
        groupRepos.save(theGroup);

        // create two users of same group
        final User user1 = new User(LOGIN1);
        userRepos.save(user1);
        final Token token = new Token(user1, AUTHORITY, ACCESS_TOKEN, LocalDateTime.now().plusMinutes(5), REFRESH_TOKEN, LocalDateTime.now());
        tokenRepos.save(token);
        userInfo = new UserInformation(token);

        userGroupRepos.save(new UserGroup(user1.getId(), theGroup.getId()));
        userGroupRepos.save(new UserGroup(user1.getId(), defGroup.getId()));

        // mock user data to speed up test execution
        userDetailsCache.cache(LOGIN1,
                new UserDetails(LOGIN1, Long.valueOf(1234), "Firstname", "Lastname", Arrays.asList(new UserEntity("entityId", "entityname", "org")),
                        "first@last.eu", null));
        
        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();

        // insert a document
        final String URI = "https://leos/4";
        document = new Document(new URI(URI), "document's title");
        documentRepos.save(document);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
        userDetailsCache.clear();
    }

    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    private void createTestData() throws CannotCreateAnnotationException, URISyntaxException {

        // save metadata sets for the document containing different metadata
        final Metadata firstMeta = createMetadata(document, theGroup, "0.1", null);
        final Metadata secondMeta = createMetadata(document, theGroup, "1.0", Arrays.asList(setIscRef1, setRespVers1));
        final Metadata thirdMeta = createMetadata(document, theGroup, "1.1", Arrays.asList(setIscRef1, setRespVers2));
        final Metadata fourthMeta = createMetadata(document, theGroup, "2.0", Arrays.asList(setIscRef2, setRespVers1));
        final Metadata fifthMeta = createMetadata(document, theGroup, "2.0", Arrays.asList(setIscRef2, setRespVers2));
        final Metadata sixthMeta = createMetadata(document, theGroup, "2.0.1", Arrays.asList(setIscRef1));

        // save one annotation per metadata set, and assign different statuses (later as creating replies would be blocked otherwise)
        ann1 = createAnnotation(firstMeta, "id1");
        ann2 = createAnnotation(secondMeta, "id2");
        ann3 = createAnnotation(thirdMeta, "id3");
        ann4 = createAnnotation(fourthMeta, "id4");
        ann5 = createAnnotation(fifthMeta, "id5");
        ann6 = createAnnotation(sixthMeta, "id6");

        // now assign different statuses
        ann3.setStatus(AnnotationStatus.DELETED);
        ann4.setStatus(AnnotationStatus.DELETED);
        ann5.setStatus(AnnotationStatus.ACCEPTED);
        ann6.setStatus(AnnotationStatus.ACCEPTED);
        annotRepos.save(Arrays.asList(ann3, ann4, ann5, ann6));
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private Annotation createAnnotation(final Metadata meta, final String annotId) throws CannotCreateAnnotationException {

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:user@ISC");
        jsAnnot.setGroup(theGroup.getName());
        jsAnnot.setTags(Arrays.asList("comment"));
        annotService.createAnnotation(jsAnnot, userInfo);
        final Annotation annot = annotRepos.findById(jsAnnot.getId());
        annot.setMetadata(meta);
        annot.setId(annotId);
        annotRepos.save(annot);

        // changing the ID creates a new entry in the database
        // therefore we delete the initial entry
        annotRepos.delete(jsAnnot.getId());

        return annot;
    }

    private Metadata createMetadata(final Document document, final Group group, final String version,
            final List<Consumer<SimpleMetadata>> metaFillers) {

        final Metadata meta = new Metadata(document, group, AUTHORITY);
        meta.setVersion(version);
        final SimpleMetadata metaMap = new SimpleMetadata();
        if (metaFillers != null) {
            for (final Consumer<SimpleMetadata> filler : metaFillers) {
                filler.accept(metaMap);
            }
        }
        meta.setKeyValuePropertyFromSimpleMetadata(metaMap);
        meta.setResponseStatus(Metadata.ResponseStatus.SENT); // ANOT-96: set all metadata to begin SENT in order to be retrievable
        metadataRepos.save(meta);

        return meta;
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // ask for metadata having version 2.0
    // and any status
    @Test
    public void testSearch_Version2() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaVers2 = new SimpleMetadata();
        metaVers2.put(VERSION, "2.0");
        requestedMetadata.add(metaVers2);

        // items 4 and 5 have version 2.0
        final List<Annotation> expAnnotIds = Arrays.asList(ann4, ann5);

        runSearchInternal(requestedMetadata, AnnotationStatus.getAllValues(), expAnnotIds);
    }

    // ask for metadata having version 2.0
    // and ACCEPTED status
    @Test
    public void testSearch_Version2_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        // version 2.0, ACCEPTED -> matches annotations 5 (item 4 also has version 2.0, but different status)
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();
        final SimpleMetadata metaVers2 = new SimpleMetadata();
        metaVers2.put(VERSION, "2.0");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaVers2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // item 5 has version 2.0
        final List<Annotation> expAnnotIds = Arrays.asList(ann5);

        runSearchInternal(requestedMetadata, expAnnotIds);
    }

    // ask for metadata having version 2.0
    // and NORMAL status -> no match
    @Test
    public void testSearch_Version2_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        // version 2.0, NORMAL -> no matches (items 4 and 5 have version 2.0, but different status)
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();
        final SimpleMetadata metaVers2 = new SimpleMetadata();
        metaVers2.put(VERSION, "2.0");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaVers2, Arrays.asList(AnnotationStatus.NORMAL)));

        // no match
        final List<Annotation> expAnnotIds = new ArrayList<Annotation>();

        runSearchInternal(requestedMetadata, expAnnotIds);
    }

    // ask for metadata having version 2.0.1
    // and any status
    @Test
    public void testSearch_Version2_0_1() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaVers2 = new SimpleMetadata();
        metaVers2.put(VERSION, "2.0.1");
        requestedMetadata.add(metaVers2);

        // item 6 has version 2.0.1 - should not give ann4/5 which have "2.0"
        final List<Annotation> expAnnotIds = Arrays.asList(ann6);

        runSearchInternal(requestedMetadata, AnnotationStatus.getAllValues(), expAnnotIds);
    }

    // ask for metadata having versions 1.0 or 2.0
    // and NORMAL / ACCEPTED status, respectively
    @Test
    public void testSearch_Version1_or_2() throws Exception {

        createTestData();

        // launch one metadata set
        // version 1.0 -> matches annotations 2
        // version 2.0 -> matches annotations 4 and 5 (4: DELETED, 5: ACCEPTED)
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();
        final SimpleMetadata metaVers1 = new SimpleMetadata();
        metaVers1.put(VERSION, "1.0");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaVers1, Arrays.asList(AnnotationStatus.NORMAL)));

        final SimpleMetadata metaVers2 = new SimpleMetadata();
        metaVers2.put(VERSION, "2.0");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaVers2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // item 2 matches 1.0/NORMAL, item 5 matches 2.0/ACCEPTED
        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann5);

        runSearchInternal(requestedMetadata, expAnnotIds);
    }

    // ask for metadata having version up to 1.1
    // and any status
    @Test
    public void testSearch_Version_upto1_1() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaVers = new SimpleMetadata();
        metaVers.put(VERSION, "<=1.1");
        requestedMetadata.add(metaVers);

        // item 1 has version 0.1
        // item 2 has version 1.0
        // item 3 has version 1.1
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann3);

        runSearchInternal(requestedMetadata, AnnotationStatus.getAllValues(), expAnnotIds);
    }

    // ask for metadata having version up to 2.0
    // and any status
    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void testSearch_Version_upto2() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaVers = new SimpleMetadata();
        metaVers.put(VERSION, "<=2.0");
        requestedMetadata.add(metaVers);

        // item 1 has version 0.1
        // item 2 has version 1.0
        // item 3 has version 1.1
        // items 4 and 5 have version 2.0
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann3, ann4, ann5);

        runSearchInternal(requestedMetadata, AnnotationStatus.getAllValues(), expAnnotIds);
    }

    // ask for metadata having version up to 2.0
    // and status NORMAL or ACCEPTED
    @Test
    public void testSearch_Version_upto2_NormalAccepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaVers = new SimpleMetadata();
        metaVers.put(VERSION, "<=2.0");
        requestedMetadata.add(metaVers);

        // item 1 has version 0.1 + NORMAL
        // item 2 has version 1.0 + NORMAL
        // item 5 has version 2.0 + ACCEPTED
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann5);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED), expAnnotIds);
    }

    // ask for metadata having version up to 2.0 and status NORMAL
    // or
    // version 2.0.1 and stastus ACCEPTED
    @Test
    public void testSearch_Version_upto2Normal_or_201Accepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();
        final SimpleMetadata metaVers = new SimpleMetadata();
        metaVers.put(VERSION, "<=2.0");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaVers, Arrays.asList(AnnotationStatus.NORMAL)));

        final SimpleMetadata meta201 = new SimpleMetadata();
        meta201.put(VERSION, "2.0.1");
        requestedMetadata.add(new SimpleMetadataWithStatuses(meta201, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // item 1 has version 0.1 + NORMAL
        // item 2 has version 1.0 + NORMAL
        // item 6 has version 2.0.1 + ACCEPTED
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann6);

        runSearchInternal(requestedMetadata, expAnnotIds);
    }

    // ask for different metadata, in three sets
    @Test
    public void testSearch_Version_Complex() throws Exception {

        createTestData();

        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();
        final SimpleMetadata meta11 = new SimpleMetadata();
        meta11.put(VERSION, "<=1.1");
        meta11.put(RESPVERS, "1");
        requestedMetadata.add(new SimpleMetadataWithStatuses(meta11, Arrays.asList(AnnotationStatus.NORMAL)));

        final SimpleMetadata meta201 = new SimpleMetadata();
        meta201.put(VERSION, "<=2.0");
        meta201.put(ISCREF, ISCREF2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(meta201, Arrays.asList(AnnotationStatus.ACCEPTED)));

        final SimpleMetadata metaEmpty = new SimpleMetadata();
        metaEmpty.put("whatever", "isnotthere");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaEmpty, AnnotationStatus.getAllValues()));

        // item 2 has version <= 1.1 + NORMAL and response version 1
        // item 5 has version <= 2.0 + ACCEPTED and response version 2
        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann5);

        runSearchInternal(requestedMetadata, expAnnotIds);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void runSearchInternal(final List<SimpleMetadataWithStatuses> requestedMetadata,
            final List<Annotation> expectedAnnotations) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeSimpleMetadataWithStatusesList(requestedMetadata);
        runSearchInternal(serializedMetaRequest, expectedAnnotations, GROUPNAME);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void runSearchInternal(final List<SimpleMetadata> requestedMetadata, final List<AnnotationStatus> statuses,
            final List<Annotation> expectedAnnotations) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeMetadataAndStatus(requestedMetadata, statuses);
        runSearchInternal(serializedMetaRequest, expectedAnnotations, GROUPNAME);
    }

    /**
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * use several given metadata sets and status sets
     * compare received annotations and replies based on their IDs
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void runSearchInternal(final String serializedMetaRequest,
            final List<Annotation> expectedAnnotations,
            final String groupName) throws Exception {

        /**
         * run the /search query
         */
        final StringBuffer url = new StringBuffer().append("/api/search?_separate_replies=true&sort=created&order=asc&uri=").append(ann1.getDocument().getUri())
                .append("&group=").append(groupName)
                .append("&metadatasets=").append(serializedMetaRequest);

        // send search request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get(url.toString())
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that the expected annotation was returned (compare IDs)
        final JsonSearchResultWithSeparateReplies jsResponse = SerialisationHelper.deserializeJsonSearchResultWithSeparateReplies(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(expectedAnnotations.size(), jsResponse.getTotal()); // non-replies
        Assert.assertEquals(expectedAnnotations.size(), jsResponse.getRows().size());

        // verify that all annotation IDs are found
        final List<String> expectedAnnotationIds = expectedAnnotations.stream().map(Annotation::getId).collect(Collectors.toList());
        Assert.assertThat(expectedAnnotationIds,
                containsInAnyOrder(jsResponse.getRows().stream().map(JsonAnnotation::getId).toArray()));
    }

    

}
