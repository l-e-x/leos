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
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchCount;
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
public class SearchAndCountAnnotationsWithMetadataSetsAndStatusesTest {

    /**
     * test combinations of searching for different metadata sets and different statuses at the same time  
     */

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

    private static final String ACCESS_TOKEN = "demoaccesstoken", REFRESH_TOKEN = "refr";
    private static final String AUTHORITY = Authorities.ISC; // authority used for most tests; tests with EdiT authority change it in the DB
    private static final String LOGIN1 = "demo";
    private static final String LOGINACC = "accep";
    private static final String LOGINDEL = "del";
    private static final String ENTITYACC = "SG";
    private static final String ENTITYDEL = "AGRI";
    private static final String USER_ACCOUNT = "acct:" + LOGIN1 + "@" + AUTHORITY;
    private static final String USER_ACCOUNT_ACC = "acct:" + LOGINACC + "@" + AUTHORITY;
    private static final String USER_ACCOUNT_DEL = "acct:" + LOGINDEL + "@" + AUTHORITY;
    private static final String ISCREF = "ISCReference";
    private static final String ISCREF1 = "ISC/2018/1";
    private static final String ISCREF2 = "ISC/2018/2";
    private static final String RESPVERS = "responseVersion";
    private static final String GROUPNAME = "mygroup";
    private UserInformation userInfo;
    private Group theGroup;
    private Document document;
    private User userAcc, userDel;

    private final Consumer<SimpleMetadata> setIscRef1 = hashMap -> hashMap.put(ISCREF, ISCREF1);
    private final Consumer<SimpleMetadata> setIscRef2 = hashMap -> hashMap.put(ISCREF, ISCREF2);
    private final Consumer<SimpleMetadata> setRespVers1 = hashMap -> hashMap.put(RESPVERS, "1");
    private final Consumer<SimpleMetadata> setRespVers2 = hashMap -> hashMap.put(RESPVERS, "2");

    private Annotation ann1, ann2, ann3, ann4, ann5, ann6;
    private Annotation ann1R1N, ann1R2D, ann2R1N, ann2R2N, ann3R1N, ann4R1D, ann4R2D;
    private Annotation ann5R1A, ann5R2D;

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

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();

        // insert a document
        final String URI = "https://leos/4";
        document = new Document(new URI(URI), "document's title");
        documentRepos.save(document);

        // cache information about the users in order to enable annotation status update to be verified
        final String entity = "DIGIT";
        final List<UserEntity> entities = Arrays.asList(new UserEntity("1", entity, entity), new UserEntity("2", "DIGIT.B", entity),
                new UserEntity("3", "DIGIT.B.2", entity));
        final UserDetails userDetails = new UserDetails(LOGIN1, user1.getId(), "de", "mo", entities, "de.mo@ec.eu", null);
        userDetailsCache.cache(LOGIN1, userDetails);

        // create users that accepted and deleted annotations
        userAcc = new User(LOGINACC);
        userDel = new User(LOGINDEL);
        userRepos.save(userAcc);
        userRepos.save(userDel);

        final List<UserEntity> entitiesAcc = Arrays.asList(new UserEntity("10", ENTITYACC, ENTITYACC), new UserEntity("11", "SG.R", ENTITYACC));
        final List<UserEntity> entitiesDel = Arrays.asList(new UserEntity("20", ENTITYDEL, ENTITYDEL), new UserEntity("21", "AGRI.I", ENTITYDEL),
                new UserEntity("22", "AGRI.I.3", ENTITYDEL));
        userDetailsCache.cache(LOGINACC, new UserDetails(LOGINACC, userAcc.getId(), "acceptor", "the", entitiesAcc, "ac@ec.eu", null));
        userDetailsCache.cache(LOGINDEL, new UserDetails(LOGINDEL, userDel.getId(), "deletor", "the", entitiesDel, "del@ec.eu", null));
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    private void createTestData() throws CannotCreateAnnotationException, URISyntaxException {

        // save metadata sets for the document containing different metadata
        final Metadata firstMeta = createMetadata(document, theGroup, null);
        final Metadata secondMeta = createMetadata(document, theGroup, Arrays.asList(setIscRef1, setRespVers1));
        final Metadata thirdMeta = createMetadata(document, theGroup, Arrays.asList(setIscRef1, setRespVers2));
        final Metadata fourthMeta = createMetadata(document, theGroup, Arrays.asList(setIscRef2, setRespVers1));
        final Metadata fifthMeta = createMetadata(document, theGroup, Arrays.asList(setIscRef2, setRespVers2));
        final Metadata sixthMeta = createMetadata(document, theGroup, Arrays.asList(setIscRef1));

        // save one annotation per metadata set, and assign different statuses (later as creating replies would be blocked otherwise)
        ann1 = createAnnotation(firstMeta, "id1");
        ann2 = createAnnotation(secondMeta, "id2");
        ann3 = createAnnotation(thirdMeta, "id3");
        ann4 = createAnnotation(fourthMeta, "id4");
        ann5 = createAnnotation(fifthMeta, "id5");
        ann6 = createAnnotation(sixthMeta, "id6");

        // create replies
        ann1R1N = createReply(Arrays.asList(ann1.getId()), document, theGroup, AnnotationStatus.NORMAL, null, null);
        ann1R2D = createReply(Arrays.asList(ann1.getId()), document, theGroup, AnnotationStatus.DELETED, userDel, LocalDateTime.of(2019, 8, 24, 13, 24));
        ann2R1N = createReply(Arrays.asList(ann2.getId()), document, theGroup, AnnotationStatus.NORMAL, null, null);
        ann2R2N = createReply(Arrays.asList(ann2.getId()), document, theGroup, AnnotationStatus.NORMAL, null, null);
        ann3R1N = createReply(Arrays.asList(ann3.getId()), document, theGroup, AnnotationStatus.NORMAL, null, null); // cannot occur in real life
        ann4R1D = createReply(Arrays.asList(ann4.getId()), document, theGroup, AnnotationStatus.DELETED, userDel, LocalDateTime.of(2019, 8, 24, 13, 24));
        ann4R2D = createReply(Arrays.asList(ann4.getId()), document, theGroup, AnnotationStatus.DELETED, userDel, LocalDateTime.of(2019, 8, 24, 13, 24));
        ann5R1A = createReply(Arrays.asList(ann5.getId()), document, theGroup, AnnotationStatus.ACCEPTED, userAcc, LocalDateTime.of(2018, 6, 25, 12, 25));
        ann5R2D = createReply(Arrays.asList(ann5.getId()), document, theGroup, AnnotationStatus.DELETED, userDel, LocalDateTime.of(2019, 8, 24, 13, 24));

        // now assign different statuses
        setAnnotationStatus(ann3, AnnotationStatus.DELETED, userDel, LocalDateTime.of(2019, 8, 24, 13, 25));
        setAnnotationStatus(ann4, AnnotationStatus.DELETED, userDel, LocalDateTime.of(2019, 9, 1, 14, 24));
        setAnnotationStatus(ann5, AnnotationStatus.ACCEPTED, userAcc, LocalDateTime.of(2018, 6, 25, 12, 25));
        setAnnotationStatus(ann6, AnnotationStatus.ACCEPTED, userAcc, LocalDateTime.of(2019, 8, 24, 9, 48));
        annotRepos.save(Arrays.asList(ann3, ann4, ann5, ann6));
        
        // make all Metadata items be SENT, so they can be found - ANOT-96 is not the primary test focus here, therefore we keep the response status simple
        metadataRepos.findAll().forEach(meta -> {meta.setResponseStatus(Metadata.ResponseStatus.SENT); metadataRepos.save(meta);});
    }
    
    private void setAnnotationStatus(final Annotation ann, final AnnotationStatus newStatus, final User user, final LocalDateTime dateTime) {

        ann.setStatus(newStatus);
        ann.setStatusUpdated(dateTime);
        ann.setStatusUpdatedBy(user.getId());
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

    private Annotation createReply(final List<String> parentAnnotIds, final Document document,
            final Group group, final AnnotationStatus status,
            final User statusModifyingUser, final LocalDateTime statusModificationDate)
            throws CannotCreateAnnotationException, URISyntaxException {

        JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(USER_ACCOUNT,
                new URI(document.getUri()), parentAnnotIds);
        jsReply.setGroup(group.getName());
        jsReply = annotService.createAnnotation(jsReply, userInfo);

        // apply status
        final Annotation annot = annotRepos.findById(jsReply.getId());
        annot.setStatus(status);
        annot.setStatusUpdatedBy(statusModifyingUser == null ? null : statusModifyingUser.getId());
        annot.setStatusUpdated(statusModificationDate);
        annotRepos.save(annot);

        return annot;
    }

    private Metadata createMetadata(final Document document, final Group group,
            final List<Consumer<SimpleMetadata>> metaFillers) {

        final Metadata meta = new Metadata(document, group, AUTHORITY);
        final SimpleMetadata metaMap = new SimpleMetadata();
        if (metaFillers != null) {
            for (final Consumer<SimpleMetadata> filler : metaFillers) {
                filler.accept(metaMap);
            }
        }
        meta.setKeyValuePropertyFromSimpleMetadata(metaMap);
        metadataRepos.save(meta);

        return meta;
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // ask for metadata ISCReference 1
    // and status NORMAL
    @Test
    public void testSearch_IscRef1_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 2 has NORMAL
        final List<Annotation> expAnnotIds = Arrays.asList(ann2);
        // two replies: to annotations 2 (both)
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISCReference 1
    // and status NORMAL
    @Test
    public void testCount_IscRef1_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 2 has NORMAL
        // -> only ann2 is counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL), 1);
    }

    // ask for metadata ISCReference 1
    // and status DELETED
    @Test
    public void testSearch_IscRef1_Deleted() throws Exception {

        createTestData();

        // launch two different metadata sets, only one should match
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 3 has DELETED status
        final List<Annotation> expAnnotIds = Arrays.asList(ann3);
        // no replies: only reply to annotation 3 has status NORMAL (not realistic though)
        final List<Annotation> expReplyIds = new ArrayList<Annotation>();

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.DELETED), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISCReference 1
    // and status DELETED
    @Test
    public void testCount_IscRef1_Deleted() throws Exception {

        createTestData();

        // launch two different metadata sets, only one should match
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 3 has DELETED status
        // -> only ann3 is counted

        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.DELETED), 1);
    }

    // ask for metadata ISCReference 1
    // and status NORMAL or DELETED
    @Test
    public void testSearch_IscRef1_NormalDeleted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 2 and 3 have NORMAL or DELETED status
        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann3);
        // three replies: to annotations 2 (x2) and 3
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N, ann3R1N);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.DELETED), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISCReference 1
    // and status NORMAL or DELETED
    @Test
    public void testCount_IscRef1_NormalDeleted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 2 and 3 have NORMAL or DELETED status
        // -> 2 and 3 are counted

        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.DELETED), 2);
    }

    // ask for metadata ISCReference 1
    // and status ACCEPTED
    @Test
    public void testSearch_IscRef1_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 5, 6 have ACCEPTED status
        final List<Annotation> expAnnotIds = Arrays.asList(ann6);
        // no reply: annotations 6 has no replies
        final List<Annotation> expReplyIds = new ArrayList<Annotation>();

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISCReference 1
    // and status ACCEPTED
    @Test
    public void testCount_IscRef1_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6; 5, 6 have ACCEPTED status
        // -> only ann6 is counted

        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), 1);
    }

    // ask for metadata ISCReference 1
    // and all statuses
    @Test
    public void testSearch_IscRef1_AllStatuses() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6
        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann3, ann6);
        // three replies: to annotations 2 (x2), 3
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N, ann3R1N);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ALL), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISCReference 1
    // and all statuses
    @Test
    public void testCount_IscRef1_AllStatuses() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(metaIscRef1);

        // ISCRef1 is contained in 2, 3, 6
        // -> all three are counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ALL), 3);
    }

    // ask for metadata response version 2
    // and status NORMAL
    @Test
    public void testSearch_RespVers2_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(metaRespVers2);

        // response version 2 is contained in 3, 5; none has NORMAL (3: DELETED, 5: ACCEPTED)
        final List<Annotation> expAnnotIds = new ArrayList<Annotation>();
        // no replies
        final List<Annotation> expReplyIds = new ArrayList<Annotation>();

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL), expAnnotIds, expReplyIds);
    }

    // ask for metadata response version 2
    // and status NORMAL
    @Test
    public void testCount_RespVers2_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(metaRespVers2);

        // response version 2 is contained in 3, 5; none has NORMAL (3: DELETED, 5: ACCEPTED)
        // -> no intersection
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL), 0);
    }

    // ask for metadata response version 2
    // and status ACCEPTED
    @Test
    public void testSearch_RespVers2_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(metaRespVers2);

        // response version 2 is contained in 3, 5; 5, 6 have status ACCEPTED
        final List<Annotation> expAnnotIds = Arrays.asList(ann5);
        // one reply (ann5; second reply of ann5 is DELETED)
        final List<Annotation> expReplyIds = Arrays.asList(ann5R1A);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), expAnnotIds, expReplyIds);
    }

    // ask for metadata response version 2
    // and status ACCEPTED
    @Test
    public void testCount_RespVers2_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(metaRespVers2);

        // response version 2 is contained in 3, 5; 5, 6 have status ACCEPTED
        // -> only ann5 is counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), 1);
    }

    // ask for metadata response version 2
    // and status ACCEPTED OR DELETED
    @Test
    public void testSearch_RespVers2_AcceptedDeleted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(metaRespVers2);

        // response version 2 is contained in 3, 5; 3 has status DELETED, 5 has status ACCEPTED
        final List<Annotation> expAnnotIds = Arrays.asList(ann3, ann5);
        // two replies (ann5: 2; ann3's replies have status NORMAL)
        final List<Annotation> expReplyIds = Arrays.asList(ann5R1A, ann5R2D);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.DELETED), expAnnotIds, expReplyIds);
    }

    // ask for metadata response version 2
    // and status ACCEPTED OR DELETED
    @Test
    public void testCount_RespVers2_AcceptedDeleted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(metaRespVers2);

        // response version 2 is contained in 3, 5; 3 has status DELETED, 5 has status ACCEPTED
        // -> ann2 and ann3 are counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.DELETED), 2);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and status NORMAL
    @Test
    public void testSearch_IscRef2OrRespVers1_Normal() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4, 6; ISC reference 2 is contained in 4, 5
        // only 2 has NORMAL status
        final List<Annotation> expAnnotIds = Arrays.asList(ann2);
        // no replies
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and status NORMAL
    @Test
    public void testCount_IscRef2OrRespVers1_Normal() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4, 6; ISC reference 2 is contained in 4, 5
        // only 2 has NORMAL status and is thus counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.NORMAL), 1);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and status ACCEPTED
    @Test
    public void testSearch_IscRef2OrRespVers1_Accepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4; ISC reference 2 is contained in 4, 5 --> 2, 4, 5
        // 5 and 6 have ACCEPTED status
        // -> 5 remains
        final List<Annotation> expAnnotIds = Arrays.asList(ann5);
        // one reply (only one reply of ann5 has ACCEPTED status)
        final List<Annotation> expReplyIds = Arrays.asList(ann5R1A);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and status ACCEPTED
    @Test
    public void testCount_IscRef2OrRespVers1_Accepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4; ISC reference 2 is contained in 4, 5 --> 2, 4, 5
        // 5 and 6 have ACCEPTED status
        // -> 5 remains and is counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), 1);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and status DELETED
    @Test
    public void testSearch_IscRef2OrRespVers1_Deleted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4; ISC reference 2 is contained in 4, 5 --> 2, 4, 5
        // 3 and 4 have DELETED status
        // -> 4 remains
        final List<Annotation> expAnnotIds = Arrays.asList(ann4);
        // two replies (both replies of ann4 have DELETED status)
        final List<Annotation> expReplyIds = Arrays.asList(ann4R1D, ann4R2D);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.DELETED), expAnnotIds, expReplyIds);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and status DELETED
    @Test
    public void testCount_IscRef2OrRespVers1_Deleted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4; ISC reference 2 is contained in 4, 5 --> 2, 4, 5
        // 3 and 4 have DELETED status
        // -> 4 remains and is counted only
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.DELETED), 1);
    }

    // ask for metadata ISC reference 2 or response version 1
    // and all statuses
    @Test
    public void testSearch_IscRef2OrRespVers1_AllStatuses() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4; ISC reference 2 is contained in 4, 5 --> 2, 4, 5
        // 2 has NORMAL status, 4 has DELETED status, 5 has ACCEPTED status
        // -> all remain
        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann4, ann5);
        // six replies (each two per annotation)
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N, ann4R1D, ann4R2D, ann5R1A, ann5R2D);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ALL, AnnotationStatus.DELETED), expAnnotIds, expReplyIds); // ALL takes priority
    }

    // ask for metadata ISC reference 2 or response version 1
    // and all statuses
    @Test
    public void testCount_IscRef2OrRespVers1_AllStatuses() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers1.accept(metaRespVers1);
        requestedMetadata.add(metaRespVers1);
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(metaIscRef2);

        // response version 1 is contained in 2, 4; ISC reference 2 is contained in 4, 5 --> 2, 4, 5
        // 2 has NORMAL status, 4 has DELETED status, 5 has ACCEPTED status
        // -> only ann4 is counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ALL, AnnotationStatus.DELETED), 3); // ALL takes priority
    }

    // ask for metadata ISC reference 2, status DELETED
    // or ISC reference 1, status NORMAL or ACCEPTED
    @Test
    public void testSearch_IscRef2Deleted_Or_IscRef1NormalAccepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // ISC reference 2, deleted -> matches annotation 4 (and its two replies)
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2, Arrays.asList(AnnotationStatus.DELETED)));

        // ISC reference 1, normal&accepted -> matches annotations 2 (and its two replies) and 6 (no replies)
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1, Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED)));

        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann4, ann6);
        // four replies
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N, ann4R1D, ann4R2D);

        runSearchInternal(requestedMetadata, expAnnotIds, expReplyIds);
    }

    // ask for metadata ISC reference 2, status DELETED
    // or ISC reference 1, status NORMAL or ACCEPTED
    @Test
    public void testCount_IscRef2Deleted_Or_IscRef1NormalAccepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // ISC reference 2, deleted -> matches annotation 4
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2, Arrays.asList(AnnotationStatus.DELETED)));

        // ISC reference 1, normal&accepted -> matches annotations 2 and 6
        final SimpleMetadata metaIscRef1 = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1, Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED)));

        // 3 = ann2, ann4, ann6
        runCountInternal(requestedMetadata, 3);
    }

    // ask for no metadata, status NORMAL
    // or response version 2, status ACCEPTED
    @Test
    public void testSearch_NormalWithoutMetadata_Or_RespVers2Accepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // no metadata, NORMAL
        // -> matches annotation 1 (and its first reply)
        // -> matches annotation 2 (and its two replies)
        requestedMetadata.add(new SimpleMetadataWithStatuses(new SimpleMetadata(), Arrays.asList(AnnotationStatus.NORMAL)));

        // response version 2, ACCEPTED -> matches annotations 5 (and its first reply)
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaRespVers2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann5);
        // four replies
        final List<Annotation> expReplyIds = Arrays.asList(ann1R1N, ann2R1N, ann2R2N, ann5R1A);

        runSearchInternal(requestedMetadata, expAnnotIds, expReplyIds);
    }

    // ask for no metadata, status NORMAL
    // or response version 2, status ACCEPTED
    @Test
    public void testCount_NormalWithoutMetadata_Or_RespVers2Accepted() throws Exception {

        createTestData();

        // launch two metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // no metadata, NORMAL
        // -> matches annotation 1
        // -> matches annotation 2
        requestedMetadata.add(new SimpleMetadataWithStatuses(new SimpleMetadata(), Arrays.asList(AnnotationStatus.NORMAL)));

        // response version 2, ACCEPTED -> matches annotations 5
        final SimpleMetadata metaRespVers2 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaRespVers2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // 3 = ann1, ann2, ann5
        runCountInternal(requestedMetadata, 3);
    }

    // ask for ISC reference 2, response version 1, status DELETED
    // or ISC reference 2, response version 2, status ACCEPTED
    // -> should not show the deleted reply of annotation 5 (since annotation 5 is accepted)
    // or REJECTED items without metadata
    @Test
    public void testSearch_IscRef2RespVers1Deleted_Or_IscRef2RespVers2Accepted_Or_Rejected() throws Exception {

        createTestData();

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // no metadata, REJECTED -> no matches
        requestedMetadata.add(new SimpleMetadataWithStatuses(new SimpleMetadata(), Arrays.asList(AnnotationStatus.REJECTED)));

        // ISC reference 2, response version 1, DELETED -> matches annotations 4 (and its two replies)
        final SimpleMetadata metaIscRef2RespVers1 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2RespVers1);
        setRespVers1.accept(metaIscRef2RespVers1);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2RespVers1, Arrays.asList(AnnotationStatus.DELETED)));

        // ISC reference 2, response version 2, status ACCEPTED -> matches annotation 5 and its first reply
        // note: here, the second (deleted) reply may not show up since the "DELETED" matcher searches for other metadata
        final SimpleMetadata metaIscRef2RespVers2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2RespVers2);
        setRespVers2.accept(metaIscRef2RespVers2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2RespVers2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        final List<Annotation> expAnnotIds = Arrays.asList(ann4, ann5);
        // three replies
        final List<Annotation> expReplyIds = Arrays.asList(ann4R1D, ann4R2D, ann5R1A);

        runSearchInternal(requestedMetadata, expAnnotIds, expReplyIds);
    }

    // ask for ISC reference 2, response version 1, status DELETED
    // or ISC reference 2, response version 2, status ACCEPTED
    // -> should not show the deleted reply of annotation 5 (since annotation 5 is accepted)
    // or REJECTED items without metadata
    @Test
    public void testCount_IscRef2RespVers1Deleted_Or_IscRef2RespVers2Accepted_Or_Rejected() throws Exception {

        createTestData();

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // no metadata, REJECTED -> no matches
        requestedMetadata.add(new SimpleMetadataWithStatuses(new SimpleMetadata(), Arrays.asList(AnnotationStatus.REJECTED)));

        // ISC reference 2, response version 1, DELETED -> matches annotations 4
        final SimpleMetadata metaIscRef2RespVers1 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2RespVers1);
        setRespVers1.accept(metaIscRef2RespVers1);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2RespVers1, Arrays.asList(AnnotationStatus.DELETED)));

        // ISC reference 2, response version 2, status ACCEPTED -> matches annotation 5
        final SimpleMetadata metaIscRef2RespVers2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2RespVers2);
        setRespVers2.accept(metaIscRef2RespVers2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2RespVers2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // 2 = ann4, ann5
        runCountInternal(requestedMetadata, 2);
    }

    // ask for ISC reference 1, status ACCEPTED
    // or ISC reference 2, status ACCEPTED
    // or ISC reference 1, status DELETED
    @Test
    public void testSearch_IscRef1Accepted_Or_IscRef2Accepted_Or_IscRef1Deleted() throws Exception {

        createTestData();

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // ISC reference 1, ACCEPTED -> matches annotations 6 (has no replies)
        final SimpleMetadata metaIscRef1A = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1A);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1A, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // ISC reference 2, ACCEPTED -> matches annotations 5 (and one ACCEPTED reply)
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // ISC reference 1, DELETED -> matches annotations 3 (but no replies)
        final SimpleMetadata metaIscRef1D = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1D);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1D, Arrays.asList(AnnotationStatus.DELETED)));

        final List<Annotation> expAnnotIds = Arrays.asList(ann3, ann5, ann6);
        // one reply
        final List<Annotation> expReplyIds = Arrays.asList(ann5R1A);

        runSearchInternal(requestedMetadata, expAnnotIds, expReplyIds);
    }

    // ask for ISC reference 1, status ACCEPTED
    // or ISC reference 2, status ACCEPTED
    // or ISC reference 1, status DELETED
    @Test
    public void testCount_IscRef1Accepted_Or_IscRef2Accepted_Or_IscRef1Deleted() throws Exception {

        createTestData();

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // ISC reference 1, ACCEPTED -> matches annotations 6
        final SimpleMetadata metaIscRef1A = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1A);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1A, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // ISC reference 2, ACCEPTED -> matches annotations 5
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2, Arrays.asList(AnnotationStatus.ACCEPTED)));

        // ISC reference 1, DELETED -> matches annotations 3
        final SimpleMetadata metaIscRef1D = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1D);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1D, Arrays.asList(AnnotationStatus.DELETED)));

        // 3 = ann3, ann5, ann6
        runCountInternal(requestedMetadata, 3);
    }

    // ask for ISC reference 1, ALL statuses
    // or response version 1, status NORMAL (implicitly)
    // or ISC reference 1, status DELETED
    // -> checks that no annotation or reply is given multiple times if it matches in different metadata/status sets
    @Test
    public void testSearch_IscRef1All_Or_RespVers1Normal_Or_IscRef1Deleted() throws Exception {

        createTestData();

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // ISC reference 1, ALL statuses -> matches annotations 2 (and its two replies), 3 (and one reply), 6 (has no replies)
        final SimpleMetadata metaIscRef1All = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1All);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1All, Arrays.asList(AnnotationStatus.ALL)));

        // response version 1, NORMAL -> matches annotations 2 (and its two replies)
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers1);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaRespVers1, null)); // implicit: NORMAL status

        // ISC reference 1, DELETED -> matches annotations 3 (but no replies)
        final SimpleMetadata metaIscRef1D = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1D);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1D, Arrays.asList(AnnotationStatus.DELETED)));

        final List<Annotation> expAnnotIds = Arrays.asList(ann2, ann3, ann6);
        // three replies
        final List<Annotation> expReplyIds = Arrays.asList(ann2R1N, ann2R2N, ann3R1N);

        runSearchInternal(requestedMetadata, expAnnotIds, expReplyIds);
    }

    // ask for ISC reference 1, ALL statuses
    // or response version 1, status NORMAL (implicitly)
    // or ISC reference 1, status DELETED
    // -> checks that no annotation or reply is given multiple times if it matches in different metadata/status sets
    @Test
    public void testCount_IscRef1All_Or_RespVers1Normal_Or_IscRef1Deleted() throws Exception {

        createTestData();

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // ISC reference 1, ALL statuses -> matches annotations 2, 3, 6
        final SimpleMetadata metaIscRef1All = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1All);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1All, Arrays.asList(AnnotationStatus.ALL)));

        // response version 1, NORMAL -> matches annotations 2
        final SimpleMetadata metaRespVers1 = new SimpleMetadata();
        setRespVers2.accept(metaRespVers1);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaRespVers1, null)); // implicit: NORMAL status

        // ISC reference 1, DELETED -> matches annotations 3
        final SimpleMetadata metaIscRef1D = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1D);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1D, Arrays.asList(AnnotationStatus.DELETED)));

        // 3 = ann2, ann3, ann6
        runCountInternal(requestedMetadata, 3);
    }

    // EdiT search (!)
    // ask for ISC reference 1 + ISC response 1, status NORMAL+ACCEPTED
    // or ISC reference 2, status DELETED+ACCEPTED
    // note: there is no corresponding Count() test as this is only allowed for ISC users
    @Test
    public void testSearch_Edit_IscRef1Resp1NormalAccepted_Or_IscRef2DeletedAccepted() throws Exception {

        createTestData();

        // adapt the token to be an EdiT token -> becomes EdiT search request
        final Token theToken = tokenRepos.findByAccessToken(ACCESS_TOKEN);
        theToken.setAuthority(Authorities.EdiT);
        tokenRepos.save(theToken);

        // adapt metadata: first should be recognized as an edit metadata
        // others receive response status SENT
        final List<Metadata> allMeta = metadataRepos.findByDocumentAndGroupAndSystemId(document, theGroup, Authorities.ISC);
        for (final Metadata meta : allMeta) {
            if (meta.getKeyValuePropertyAsSimpleMetadata().size() == 0) {
                meta.setSystemId(Authorities.EdiT);
                meta.setKeyValuePropertyFromSimpleMetadata(new SimpleMetadata("key", "value")); // create some metadata
            } else {
                meta.setResponseStatus(ResponseStatus.SENT);
            }
        }
        metadataRepos.save(allMeta);

        // add another EdiT metadata item without any entries
        final Metadata newLeosMeta = new Metadata(document, theGroup, Authorities.EdiT);
        metadataRepos.save(newLeosMeta);

        // create a new EdiT annotation associated to the new metadata item - should not be matched due to missing metadata entries
        createAnnotation(newLeosMeta, "id8");

        // launch three metadata sets
        final List<SimpleMetadataWithStatuses> requestedMetadata = new ArrayList<SimpleMetadataWithStatuses>();

        // match the key/value for EdiT -> matches annotation 1, all states -> matches annotation 1 (and its two replies)
        final SimpleMetadata metaLeos = new SimpleMetadata();
        metaLeos.put("key", "value");
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaLeos, AnnotationStatus.getAllValues()));

        // ISC reference 1 + response version 1, NORMAL+ACCEPTED -> matches annotations 2 (and its two replies)
        final SimpleMetadata metaIscRef1A = new SimpleMetadata();
        setIscRef1.accept(metaIscRef1A);
        setRespVers1.accept(metaIscRef1A);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef1A, Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED)));

        // ISC reference 2, DELETED+ACCEPTED -> matches annotations 4+5 (and one ACCEPTED and three DELETED replies)
        final SimpleMetadata metaIscRef2 = new SimpleMetadata();
        setIscRef2.accept(metaIscRef2);
        requestedMetadata.add(new SimpleMetadataWithStatuses(metaIscRef2, Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.DELETED)));

        // ann1 is reported to the EdiT, others coming from ISC
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann4, ann5);
        // eight replies
        final List<Annotation> expReplyIds = Arrays.asList(ann1R1N, ann1R2D, ann2R1N, ann2R2N, ann4R1D, ann4R2D, ann5R1A, ann5R2D);

        runSearchInternalWithoutReplyCheck(requestedMetadata, expAnnotIds, expReplyIds, "__world__");
    }

    // don't ask for any metadata
    // and status NORMAL
    @Test
    public void testSearch_NoMetadata_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); 1 and 2 have NORMAL
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2);
        // three replies: one to ann1, two to ann2
        final List<Annotation> expReplyIds = Arrays.asList(ann1R1N, ann2R1N, ann2R2N);

        // ask for NORMAL implicitly by skipping status parameter
        runSearchInternal(requestedMetadata, new ArrayList<AnnotationStatus>(), expAnnotIds, expReplyIds);
    }

    // don't ask for any metadata
    // and status NORMAL
    @Test
    public void testCount_NoMetadata_Normal() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); 1 and 2 have NORMAL
        // -> ann1 and ann2 are counted
        runCountInternal(requestedMetadata, new ArrayList<AnnotationStatus>(), 2);
    }

    // don't ask for any metadata
    // and status DELETED
    @Test
    public void testSearch_NoMetadata_Deleted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); 3 and 4 have DELETED status
        final List<Annotation> expAnnotIds = Arrays.asList(ann3, ann4);
        // two replies: both to ann4 (are both DELETED)
        final List<Annotation> expReplyIds = Arrays.asList(ann4R1D, ann4R2D);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.DELETED), expAnnotIds, expReplyIds);
    }

    // don't ask for any metadata
    // and status DELETED
    @Test
    public void testCount_NoMetadata_Deleted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); 3 and 4 have DELETED status
        // -> only ann3 and ann4 are counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.DELETED), 2);
    }

    // don't ask for any metadata
    // and status ACCEPTED
    @Test
    public void testSearch_NoMetadata_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); 5 and 6 have ACCEPTED status
        final List<Annotation> expAnnotIds = Arrays.asList(ann5, ann6);
        // one reply: one answer to ann5 is ACCEPTED
        final List<Annotation> expReplyIds = Arrays.asList(ann5R1A);

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), expAnnotIds, expReplyIds);
    }

    // don't ask for any metadata
    // and status ACCEPTED
    @Test
    public void testCount_NoMetadata_Accepted() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); 5 and 6 have ACCEPTED status
        // -> only ann5 and ann6 are counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.ACCEPTED), 2);
    }

    // don't ask for any metadata
    // and status REJECTED
    @Test
    public void testSearch_NoMetadata_Rejected() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); none have REJECTED status
        final List<Annotation> expAnnotIds = new ArrayList<Annotation>();
        // thus no reply either
        final List<Annotation> expReplyIds = new ArrayList<Annotation>();

        runSearchInternal(requestedMetadata, Arrays.asList(AnnotationStatus.REJECTED), expAnnotIds, expReplyIds);
    }

    // don't ask for any metadata
    // and status REJECTED
    @Test
    public void testCount_NoMetadata_Rejected() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); none have REJECTED status
        // -> none are counted
        runCountInternal(requestedMetadata, Arrays.asList(AnnotationStatus.REJECTED), 0);
    }

    // don't ask for any metadata
    // and all statuses
    @Test
    public void testSearch_NoMetadata_AllStatuses() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); all have a matching status
        final List<Annotation> expAnnotIds = Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6);
        // all replies
        final List<Annotation> expReplyIds = Arrays.asList(ann1R1N, ann1R2D, ann2R1N, ann2R2N, ann3R1N, ann4R1D, ann4R2D, ann5R1A, ann5R2D);

        // ask for all statuses explicitly
        runSearchInternal(requestedMetadata,
                Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.NORMAL, AnnotationStatus.DELETED), expAnnotIds,
                expReplyIds);
    }

    // don't ask for any metadata
    // and all statuses
    @Test
    public void testCount_NoMetadata_AllStatuses() throws Exception {

        createTestData();

        // launch one metadata set
        final List<SimpleMetadata> requestedMetadata = new ArrayList<SimpleMetadata>();

        // matches for metadata: all (1, 2, 3, 4, 5, 6); all have a matching status
        // -> all 6 are counted

        // ask for all statuses explicitly
        runCountInternal(requestedMetadata,
                Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED, AnnotationStatus.NORMAL, AnnotationStatus.DELETED),
                6);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void runSearchInternal(final List<SimpleMetadataWithStatuses> requestedMetadata,
            final List<Annotation> expectedAnnotations, final List<Annotation> expectedReplies) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeSimpleMetadataWithStatusesList(requestedMetadata);
        runSearchInternal(serializedMetaRequest, expectedAnnotations, expectedReplies, GROUPNAME, true);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void runSearchInternal(final List<SimpleMetadata> requestedMetadata, final List<AnnotationStatus> statuses,
            final List<Annotation> expectedAnnotations, final List<Annotation> expectedReplies) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeMetadataAndStatus(requestedMetadata, statuses);
        runSearchInternal(serializedMetaRequest, expectedAnnotations, expectedReplies, GROUPNAME, true);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void runSearchInternalWithoutReplyCheck(final List<SimpleMetadataWithStatuses> requestedMetadata,
            final List<Annotation> expectedAnnotations, final List<Annotation> expectedReplies,
            final String groupName) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeSimpleMetadataWithStatusesList(requestedMetadata);
        runSearchInternal(serializedMetaRequest, expectedAnnotations, expectedReplies, groupName, false);
    }

    /**
     * successfully search for annotations, expected HTTP 200 and the annotation data
     * use several given metadata sets and status sets
     * compare received annotations and replies based on their IDs
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void runSearchInternal(final String serializedMetaRequest,
            final List<Annotation> expectedAnnotations, final List<Annotation> expectedReplies,
            final String groupName, final boolean checkReplyDetails) throws Exception {

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

        // verify reply IDs
        Assert.assertEquals(expectedReplies.size(), jsResponse.getReplies().size());

        final List<String> expectedReplyIds = expectedReplies.stream().map(Annotation::getId).collect(Collectors.toList());
        Assert.assertThat(expectedReplyIds,
                containsInAnyOrder(jsResponse.getReplies().stream().map(JsonAnnotation::getId).toArray()));

        checkStatus(jsResponse, expectedAnnotations, expectedReplies, checkReplyDetails);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void runCountInternal(final List<SimpleMetadata> requestedMetadata, final List<AnnotationStatus> statuses,
            final int expectedAnnotations) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeMetadataAndStatus(requestedMetadata, statuses);
        runCountInternal(serializedMetaRequest, expectedAnnotations, GROUPNAME);
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void runCountInternal(final List<SimpleMetadataWithStatuses> requestedMetadata,
            final int expectedAnnotations) throws Exception {

        final String serializedMetaRequest = SerialisationHelper.serializeSimpleMetadataWithStatusesList(requestedMetadata);
        runCountInternal(serializedMetaRequest, expectedAnnotations, GROUPNAME);
    }

    /**
     * successfully count annotations, expected HTTP 200 and the annotation data
     * use several given metadata sets and status sets
     * compare received annotations and replies based on their IDs
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private void runCountInternal(final String serializedMetaRequest,
            final int expectedAnnotations, final String groupName) throws Exception {

        final StringBuffer url = new StringBuffer().append("/api/count?uri=").append(ann1.getDocument().getUri())
                .append("&group=").append(groupName)
                .append("&metadatasets=").append(serializedMetaRequest);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(url.toString())
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + ACCESS_TOKEN);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // ID must have been set
        final JsonSearchCount jsCountResponse = SerialisationHelper.deserializeJsonSearchCount(responseString);
        Assert.assertNotNull(jsCountResponse);
        Assert.assertEquals(expectedAnnotations, jsCountResponse.getCount());
    }

    // check that the JSON format of the found annotations have the correct status value assigned
    private void checkStatus(final JsonSearchResultWithSeparateReplies result,
            final List<Annotation> anns, final List<Annotation> replies, final boolean checkReplyDetails) {

        for (final Annotation annCheck : anns) {
            final JsonAnnotation foundAnnot = result.getRows().stream().filter(jsAnn -> jsAnn.getId().equals(annCheck.getId()))
                    .findFirst().get();
            checkStatus(annCheck, foundAnnot, true);
        }

        for (final Annotation annCheck : replies) {
            final JsonAnnotation foundAnnot = result.getReplies().stream().filter(jsAnn -> jsAnn.getId().equals(annCheck.getId()))
                    .findFirst().get();
            checkStatus(annCheck, foundAnnot, checkReplyDetails);
        }
    }

    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    private void checkStatus(final Annotation annRef, final JsonAnnotation foundAnnot, final boolean checkReplyDetails) {

        Assert.assertNotNull(foundAnnot);
        Assert.assertNotNull(foundAnnot.getStatus());
        Assert.assertEquals(annRef.getStatus(), foundAnnot.getStatus().getStatus());

        if (checkReplyDetails) {
            if (annRef.getStatusUpdatedBy() == null) {
                Assert.assertNull(foundAnnot.getStatus().getUpdated());
                Assert.assertNull(foundAnnot.getStatus().getUpdated_by());
                Assert.assertNull(foundAnnot.getStatus().getUser_info());
            } else {
                Assert.assertEquals(annRef.getStatusUpdated(), foundAnnot.getStatus().getUpdated());

                // check that user and its entity were correctly resolved
                if (annRef.getStatusUpdatedBy().longValue() == userAcc.getId().longValue()) {
                    Assert.assertEquals(USER_ACCOUNT_ACC, foundAnnot.getStatus().getUser_info());
                    Assert.assertEquals(ENTITYACC, foundAnnot.getStatus().getUpdated_by());
                } else if (annRef.getStatusUpdatedBy().longValue() == userDel.getId().longValue()) {
                    Assert.assertEquals(USER_ACCOUNT_DEL, foundAnnot.getStatus().getUser_info());
                    Assert.assertEquals(ENTITYDEL, foundAnnot.getStatus().getUpdated_by());
                }
            }
        }
    }
}
