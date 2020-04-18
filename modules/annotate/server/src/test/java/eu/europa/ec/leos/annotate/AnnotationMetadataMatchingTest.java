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
package eu.europa.ec.leos.annotate;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import org.hamcrest.Matchers;
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
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"}) // test classes for complex relations
public class AnnotationMetadataMatchingTest {

    /**
     * Tests on search functionality for annotations, especially tolerant metadata matching
     * the tests use combinations of one authority creating annotations (EdiT/ISC) and 
     * an authority asking for annotations (EdiT/ISC)
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private AnnotationService annotService;

    // -------------------------------------
    // Cleanup of database content and prepare test data
    // -------------------------------------
    private static final String URL = "http://myurl.net";

    private static final String LOGIN = "login";

    private static final String ISCREF = "ISCReference";
    private static final String ISCREF1 = "ISC/2018/1";
    private static final String ISCREF2 = "ISC/2018/2";
    private static final String RESPVERS = "responseVersion";
    private static final String RESPSTATUS = "responseStatus";
    private static final String SENT = "SENT";

    private UserInformation userInfo;
    private Group theGroup, secondGroup;

    private AnnotationSearchOptions options;
    private Annotation ann1, ann2, ann3, ann4, ann5, ann6, ann7, annGroup2;

    private final Consumer<SimpleMetadata> setIscRef1 = hashMap -> hashMap.put(ISCREF, ISCREF1);
    private final Consumer<SimpleMetadata> setIscRef2 = hashMap -> hashMap.put(ISCREF, ISCREF2);
    private final Consumer<SimpleMetadata> setRespVers1 = hashMap -> hashMap.put(RESPVERS, "1");
    private final Consumer<SimpleMetadata> setRespVers2 = hashMap -> hashMap.put(RESPVERS, "2");
    private final Consumer<SimpleMetadata> setRespStatusSent = hashMap -> hashMap.put(RESPSTATUS, SENT);

    @Before
    public void cleanDatabaseBeforeTests() throws URISyntaxException, CannotCreateAnnotationException {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    private void createTestData(final String authority)
            throws URISyntaxException, CannotCreateAnnotationException {

        createTestData(authority, authority);
    }

    private void createTestData(final String authorityForAnnot, final String authorityForUser)
            throws URISyntaxException, CannotCreateAnnotationException {

        TestDbHelper.cleanupRepositories(this);
        theGroup = new Group("mygroup", true);// note: we do not use the __world__ group, as it would use a special search model
        groupRepos.save(theGroup);

        // insert two users, assign them to the default group
        final User user = new User(LOGIN);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), theGroup.getId()));

        userInfo = new UserInformation(
                new Token(user, authorityForAnnot, "acc1", LocalDateTime.now().plusMinutes(5), "ref1", LocalDateTime.now().plusMinutes(5)));

        // insert a document
        final Document document = new Document(new URI(URL), "document's title");
        documentRepos.save(document);

        // save metadata sets for the document containing different metadata
        final Metadata firstMeta = createMetadata(document, theGroup, authorityForAnnot, null);
        final Metadata secondMeta = createMetadata(document, theGroup, authorityForAnnot, Arrays.asList(setIscRef1, setRespVers1));
        final Metadata thirdMeta = createMetadata(document, theGroup, authorityForAnnot, Arrays.asList(setIscRef1, setRespVers2));
        final Metadata fourthMeta = createMetadata(document, theGroup, authorityForAnnot, Arrays.asList(setIscRef2, setRespVers1));
        final Metadata fifthMeta = createMetadata(document, theGroup, authorityForAnnot, Arrays.asList(setIscRef2, setRespVers2));
        final Metadata sixthMeta = createMetadata(document, theGroup, authorityForAnnot, Arrays.asList(setIscRef1));
        final Metadata seventhMeta = createMetadata(document, theGroup, authorityForAnnot, Arrays.asList(setIscRef1, setRespStatusSent));

        // save one annotation per metadata set
        ann1 = createAnnotation(firstMeta, "id1");
        ann2 = createAnnotation(secondMeta, "id2");
        ann3 = createAnnotation(thirdMeta, "id3");
        ann4 = createAnnotation(fourthMeta, "id4");
        ann5 = createAnnotation(fifthMeta, "id5");
        ann6 = createAnnotation(sixthMeta, "id6");
        ann7 = createAnnotation(seventhMeta, "id7");

        options = new AnnotationSearchOptions(
                URL, theGroup.getName(), // URI, group
                false,                   // provide separate replies
                200, 0,                  // limit, offset
                "asc", "created");       // order, sort column

        // finally add some noise: annotations in another group of the same document
        // with matching metadata - should not be found when asking for the default group
        secondGroup = new Group("grouptitle", true);
        groupRepos.save(secondGroup);
        userGroupRepos.save(new UserGroup(user.getId(), secondGroup.getId()));

        final Metadata secondGroupMeta = createMetadata(document, secondGroup, authorityForAnnot, Arrays.asList(setIscRef1));
        annGroup2 = createAnnotation(secondGroupMeta, "idGroup2");

        // set authority of the user launching the query
        userInfo.setAuthority(authorityForUser);
        userInfo.getCurrentToken().setAuthority(authorityForUser);
    }

    private Annotation createAnnotation(final Metadata meta, final String annotId) throws CannotCreateAnnotationException {

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:user@ISC");
        jsAnnot.setGroup(theGroup.getName());
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

    private Annotation createReply(final List<String> parentAnnotIds, final String uri, final String group)
            throws CannotCreateAnnotationException, URISyntaxException {

        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation("acct:user@ISC", new URI(uri), parentAnnotIds);
        jsReply.setGroup(group);
        annotService.createAnnotation(jsReply, userInfo);

        return annotRepos.findById(jsReply.getId());
    }

    private Metadata createMetadata(final Document document, final Group group, final String authorityToUse,
            final List<Consumer<SimpleMetadata>> metaFillers) {

        final Metadata meta = new Metadata(document, group, authorityToUse);
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

    // set the response status to SENT of a given list of annotations
    private void setMetadataSent(final List<Annotation> annotsToSent) {

        for (final Annotation annotToSent : annotsToSent) {
            final Metadata meta = annotToSent.getMetadata();
            meta.setResponseStatus(Metadata.ResponseStatus.SENT);
            metadataRepos.save(meta);
        }
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test with ISC: search for group only without specifying further metadata
     * -> should return all annotations of the group
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        // no metadata sent -> all annotations should match
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC: search for group only without specifying further metadata
     * -> should return all annotations of the group
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann3, ann5)); // some are SENT only -> could be found

        // no metadata sent -> all annotations should match
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann3, ann5, ann7));
    }

    /**
     * test that asking for DELETED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_DeletedStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7, annGroup2)); // all are SENT, could be found

        // no metadata sent -> all annotations should match; status DELETED -> only one matches (2)
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(Arrays.asList(AnnotationStatus.DELETED), Arrays.asList(ann2));
    }

    /**
     * test that asking for ACCEPTED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_AcceptedStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        // no metadata sent -> all annotations should match; status ACCEPTED -> only two match (4, 5)
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(Arrays.asList(AnnotationStatus.ACCEPTED), Arrays.asList(ann4, ann5));
    }

    /**
     * test that asking for REJECTED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_RejectedStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));

        // no metadata sent -> all annotations should match; status REJECTED -> only one matches (3)
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(Arrays.asList(AnnotationStatus.REJECTED), Arrays.asList(ann3));
    }

    /**
     * test that asking for NORMAL and DELETED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_NormalDeletedStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        // no metadata sent -> all annotations should match; status NORMAL, DELETED -> several matches (1, 2, 6, 7)
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.DELETED), Arrays.asList(ann1, ann2, ann6, ann7));
    }

    /**
     * test that asking for NORMAL and ACCEPTED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_NormalAcceptedStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all SENT -> could be found

        // no metadata sent -> all annotations should match; status NORMAL, ACCEPTED -> several matches (1, 4, 5, 6, 7)
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED), Arrays.asList(ann1, ann4, ann5, ann6, ann7));
    }

    /**
     * test that asking for NORMAL and ACCEPTED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_NormalAcceptedStatus_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann4, ann7)); // some SENT -> could be found

        // no metadata sent -> all annotations should match; status NORMAL, ACCEPTED -> several matches (1, 4, 5, 6, 7)
        // but only 1, 4 and 7 are SENT -> result: 1, 4, 7
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.ACCEPTED), Arrays.asList(ann1, ann4, ann7));
    }

    /**
     * test that asking for DELETED, ACCEPTED and REJECTED annotations only returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_DeletedAcceptedRejectedStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7, annGroup2)); // all are SENT, could be found

        // no metadata sent -> all annotations should match; status DELETED, ACCEPTED, REJECTED -> several matches (2, 3, 4, 5)
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.DELETED, AnnotationStatus.ACCEPTED, AnnotationStatus.REJECTED),
                Arrays.asList(ann2, ann3, ann4, ann5));
    }

    /**
     * test that asking for ALL annotations returns them; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_AllStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        // no metadata sent -> all annotations should match; ALL status -> all match
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.ALL),
                Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test that asking for ALL annotations returns them; no further metadata specified
     * however only those being SENT are returned
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_AllStatus_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann2, ann4, ann5)); // only some are SENT, could be found

        // no metadata sent -> all annotations should match; ALL status -> all match; 2+4+5+7 SENT -> only those match
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.ALL),
                Arrays.asList(ann2, ann4, ann5, ann7));
    }

    /**
     * test that asking for DELETED and ALL annotations returns all annotations; no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_DeletedAllStatus() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        // no metadata sent -> all annotations should match; status DELETED + ALL = ALL -> all match
        runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(
                Arrays.asList(AnnotationStatus.DELETED, AnnotationStatus.ALL),
                Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    private void runTestIscUserIsc_SearchForGroupOnly_StatusCombinations(final List<AnnotationStatus> statuses,
            final List<Annotation> expectedAnnots) throws URISyntaxException, CannotCreateAnnotationException {

        ann2.setStatus(AnnotationStatus.DELETED);
        ann3.setStatus(AnnotationStatus.REJECTED);
        ann4.setStatus(AnnotationStatus.ACCEPTED);
        ann5.setStatus(AnnotationStatus.ACCEPTED);
        annotRepos.save(Arrays.asList(ann2, ann3, ann4, ann5));

        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(null, statuses)));
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, expectedAnnots);
    }

    /**
     * test that asking for NORMAL or DELETED annotations returns specific annotations, considering replies also;
     * no further metadata specified
     */
    @Test
    public void testIscUserIsc_SearchForGroupOnly_ReplyDeletedStatus() throws Exception {

        createTestData(Authorities.ISC);
        // remove some annotations to keep it small
        annotRepos.delete(Arrays.asList(ann5, ann6, ann7));

        // create a reply to an annotation
        final Annotation ann1Reply = createReply(Arrays.asList(ann1.getId()), ann1.getDocument().getUri(), ann1.getGroup().getName());

        // create an reply, then delete the parent and the reply
        final Annotation ann2Reply = createReply(Arrays.asList(ann2.getId()), ann2.getDocument().getUri(), ann2.getGroup().getName());
        ann2.setStatus(AnnotationStatus.DELETED);
        ann2Reply.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(Arrays.asList(ann2, ann2Reply));

        // create a reply to another annotation, and then delete only the reply (but not the parent) -> should be ignored
        final Annotation ann3Reply = createReply(Arrays.asList(ann3.getId()), ann3.getDocument().getUri(), ann3.getGroup().getName());
        ann3Reply.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(ann3Reply);

        // create a reply and delete its parent - but not the reply
        // (should not happen in real life, but let's verify the behaviour is consistent)
        final Annotation ann4Reply = createReply(Arrays.asList(ann4.getId()), ann4.getDocument().getUri(), ann4.getGroup().getName());
        ann4.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(ann4);

        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found
        
        // act
        // no metadata sent -> all annotations should match; status NORMAL -> two matches, and one of its replies
        options.setStatuses(Arrays.asList(AnnotationStatus.NORMAL));
        AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann1, ann3));
        List<Annotation> replies = annotService.searchRepliesForAnnotations(result, options, userInfo);
        Assert.assertEquals(1, replies.size());
        Assert.assertEquals(ann1Reply.getId(), replies.get(0).getId());

        // no metadata sent -> all annotations should match; status DELETED -> two matches, and one of its replies
        options.setStatuses(Arrays.asList(AnnotationStatus.DELETED));
        result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann2, ann4));
        replies = annotService.searchRepliesForAnnotations(result, options, userInfo);
        Assert.assertEquals(1, replies.size());
        Assert.assertEquals(ann2Reply.getId(), replies.get(0).getId());

        // no metadata sent -> all annotations should match; status NORMAL or DELETED -> four matches, and all of its replies
        options.setStatuses(Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.DELETED));
        result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4));
        replies = annotService.searchRepliesForAnnotations(result, options, userInfo);
        Assert.assertEquals(4, replies.size());
        Assert.assertThat(Arrays.asList(ann1Reply.getId(), ann2Reply.getId(), ann3Reply.getId(), ann4Reply.getId()),
                Matchers.containsInAnyOrder(replies.stream().map(Annotation::getId).toArray()));
    }

    /**
     * test with EdiT: search for group only without specifying further metadata
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForGroupOnly() throws Exception {

        createTestData(Authorities.EdiT);

        // no metadata sent -> all annotations should match
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with EdiT user and ISC annotations: search for group only without specifying further metadata
     * -> should return one annotations of the group (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForGroupOnly() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        // no metadata sent -> all annotations should match
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search for group only without specifying further metadata
     * -> should not return any annotations (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForGroupOnly() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        // no metadata sent -> all annotations should match
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkResult(result, new ArrayList<Annotation>());
    }

    /**
     * test with ISC: search with specifying ISC reference 1 only
     * -> should return four annotations (2, 3, 6, 7)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1Only() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1Only();
        checkResult(result, Arrays.asList(ann2, ann3, ann6, ann7));
    }

    /**
     * test with ISC: search with specifying ISC reference 1 only
     * -> should return four annotations (2, 7)
     * similar as testIscUserIsc_SearchForIscRef1Only, but fewer annotations are SENT
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1Only_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann2, ann7)); // not all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1Only();
        checkResult(result, Arrays.asList(ann2, ann7));
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1Only() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Only();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 only
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1Only() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Only();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 1 only
     * -> should not return any annotations (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1Only() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1Only();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1Only() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF1);
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 2 only
     * -> should return two annotations of the group (4, 5)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef2Only() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef2Only();
        checkResult(result, Arrays.asList(ann4, ann5));
    }

    /**
     * test with EdiT: search with specifying ISC reference 2 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef2Only() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2Only();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 2 only
     * -> should return one annotation of the group (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef2Only() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2Only();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 2 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef2Only() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef2Only();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef2Only() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF2);
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying response version 1 only
     * -> should return two annotations of the group (2, 4)
     */
    @Test
    public void testIscUserIsc_SearchForRespVers1Only() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all SENT -> could be found

        final AnnotationSearchResult result = runSearchForRespVers1Only();
        checkResult(result, Arrays.asList(ann2, ann4));
    }

    /**
     * test with EdiT: search with specifying response version 1 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForRespVers1Only() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForRespVers1Only();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying response version 1 only
     * -> should return one annotation of the group (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForRespVers1Only() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForRespVers1Only();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying response version 1 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForRespVers1Only() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForRespVers1Only();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForRespVers1Only() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(RESPVERS, "1");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying response version 2 only
     * -> should return two annotations (3, 5)
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2Only() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        final AnnotationSearchResult result = runSearchForRespVers2Only();
        checkResult(result, Arrays.asList(ann3, ann5));
    }

    /**
     * test with EdiT: search with specifying response version 2 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForRespVers2Only() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForRespVers2Only();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying response version 2 only
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForRespVers2Only() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForRespVers2Only();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC users: search with specifying response version 2 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForRespVers2Only() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForRespVers2Only();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForRespVers2Only() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(RESPVERS, "2");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 1 and response version 1 only
     * -> should return one annotation (2)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1RespVers1() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1RespVers1();
        checkResult(result, Arrays.asList(ann2));
    }

    /**
     * test with ISC: search with specifying ISC reference 1 and response version 1 only
     * however, the only matching annotation (2) is not SENT
     * -> should not return any annotation
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1RespVers1_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann3)); // only some are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1RespVers1();
        checkResult(result, new ArrayList<Annotation>());
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 and response version 1 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1RespVers1() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1RespVers1();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 and response version 1 only
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1RespVers1() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1RespVers1();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 1 and response version 1 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1RespVers1() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1RespVers1();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1RespVers1() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF1);
        metaToMatch.put(RESPVERS, "1");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 1 and response version 2 only
     * -> should return one annotation (3)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1RespVers2() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann3, ann5)); // only some are SENT, but at least the one we aim for

        final AnnotationSearchResult result = runSearchForIscRef1RespVers2();
        checkResult(result, Arrays.asList(ann3));
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 and response version 2 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1RespVers2() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1RespVers2();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 and response version 2 only
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1RespVers2() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1RespVers2();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 1 and response version 2 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1RespVers2() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1RespVers2();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1RespVers2() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF1);
        metaToMatch.put(RESPVERS, "2");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 2 and response version 1 only
     * -> should return one annotation (4)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef2RespVers1() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef2RespVers1();
        checkResult(result, Arrays.asList(ann4));
    }

    /**
     * test with EdiT: search with specifying ISC reference 2 and response version 1 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef2RespVers1() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2RespVers1();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 2 and response version 1 only
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef2RespVers1() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2RespVers1();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 2 and response version 1 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef2RespVers1() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef2RespVers1();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef2RespVers1() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF2);
        metaToMatch.put(RESPVERS, "1");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 2 and response version 2 only
     * -> should return one annotation (5)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef2RespVers2() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef2RespVers2();
        checkResult(result, Arrays.asList(ann5));
    }

    /**
     * test with EdiT: search with specifying ISC reference 2 and response version 2 only
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef2RespVers2() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2RespVers2();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 2 and response version 2 only
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef2RespVers2() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2RespVers2();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 2 and response version 2 only
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef2RespVers2() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef2RespVers2();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef2RespVers2() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF2);
        metaToMatch.put(RESPVERS, "2");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC references 1 and 2 in two metadata sets
     * -> should return six annotations (2, 3, 4, 5, 6, 7)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1Ref2() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1Ref2();
        checkResult(result, Arrays.asList(ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC: search with specifying ISC references 1 and 2 in two metadata sets
     * -> should return four annotations (2, 4, 6, 7)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1Ref2_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann2, ann4, ann6)); // only some are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1Ref2();
        checkResult(result, Arrays.asList(ann2, ann4, ann6, ann7));
    }

    /**
     * test with EdiT: search with specifying ISC references 1 and 2 in two metadata sets
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1Ref2() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Ref2();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC references 1 and 2 in two metadata sets
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1Ref2() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Ref2();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC references 1 and 2 in two metadata sets
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1Ref2() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1Ref2();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1Ref2() throws JsonProcessingException {

        final SimpleMetadata firstMetaToMatch = new SimpleMetadata(ISCREF, ISCREF1);
        final SimpleMetadata secondMetaToMatch = new SimpleMetadata(ISCREF, ISCREF2);

        options.setMetadataMapsWithStatusesList(Arrays.asList(
                new SimpleMetadataWithStatuses(firstMetaToMatch, null),
                new SimpleMetadataWithStatuses(secondMetaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference in versions 1 and 2 in two metadata sets
     * -> should return two annotations (2, 3)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1Version1And2() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef1Version1And2();
        checkResult(result, Arrays.asList(ann2, ann3));
    }

    /**
     * test with EdiT: search with specifying ISC reference in versions 1 and 2 in two metadata sets
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1Version1And2() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Version1And2();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference in versions 1 and 2 in two metadata sets
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1Version1And2() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Version1And2();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference in versions 1 and 2 in two metadata sets
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1Version1And2() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1Version1And2();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1Version1And2() throws JsonProcessingException {

        final SimpleMetadata firstMetaToMatch = new SimpleMetadata(ISCREF, ISCREF1);
        firstMetaToMatch.put(RESPVERS, "1");
        final SimpleMetadata secondMetaToMatch = new SimpleMetadata(ISCREF, ISCREF1);
        secondMetaToMatch.put(RESPVERS, "2");

        options.setMetadataMapsWithStatusesList(Arrays.asList(
                new SimpleMetadataWithStatuses(firstMetaToMatch, null),
                new SimpleMetadataWithStatuses(secondMetaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 1 in version 1 
     * and ISC reference 2 in version 2 in two metadata sets
     * -> should return two annotations (2, 5)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1Version1Ref2Version2() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        final AnnotationSearchResult result = runSearchForIscRef1Version1Ref2Version2();
        checkResult(result, Arrays.asList(ann2, ann5));
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 in version 1 
     * and ISC reference 2 in version 2 in two metadata sets
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1Version1Ref2Version2() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Version1Ref2Version2();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 in version 1 
     * and ISC reference 2 in version 2 in two metadata sets
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1Version1Ref2Version2() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1Version1Ref2Version2();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 1 in version 1 
     * and ISC reference 2 in version 2 in two metadata sets
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1Version1Ref2Version2() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1Version1Ref2Version2();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1Version1Ref2Version2() throws JsonProcessingException {

        final SimpleMetadata firstMetaToMatch = new SimpleMetadata(ISCREF, ISCREF1);
        firstMetaToMatch.put(RESPVERS, "1");
        final SimpleMetadata secondMetaToMatch = new SimpleMetadata(ISCREF, ISCREF2);
        secondMetaToMatch.put(RESPVERS, "2");

        options.setMetadataMapsWithStatusesList(Arrays.asList(
                new SimpleMetadataWithStatuses(firstMetaToMatch, null),
                new SimpleMetadataWithStatuses(secondMetaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying response version 2 and response status SENT in two metadata sets
     * -> should return three annotations (3, 5, 7)
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2AndSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann7)); // some are SENT

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        
     // 3+5: version 2; 7: response status SENT anyway; 1,2,3,7: also SENT -> 5 is dropped (since not SENT)
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann7)); 
    }

    /**
     * test with ISC: search with specifying response version 2 and response status SENT in two metadata sets
     * -> should return two annotations (5, 7)
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2AndSent_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann5, ann6, ann7)); // only some are SENT -> item 3 won't match

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        checkResult(result, Arrays.asList(ann5, ann6, ann7)); // 5: version 2; 6: SENT; 7: response status SENT anyway
    }

    /**
     * test with ISC: search with specifying response version 2 and response status SENT in two metadata sets
     * one annotation is DELETED (5)
     * -> should return all annotations except 5
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2AndSent_OneDeleted() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT, could be found

        ann5.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(ann5);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann6, ann7)); // 3: version 2; all: response status SENT; 5 was DELETED -> ignored
    }

    /**
     * test with ISC: search with specifying response version 2 and response status SENT in two metadata sets
     * one annotation is DELETED (5)
     * -> should return two annotations (3, 7)
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2AndSent_OneDeleted_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann3, ann4, ann5, ann7)); // some are SENT, could be found

        ann5.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(ann5);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        checkResult(result, Arrays.asList(ann3, ann4, ann7)); // 3: version 2; 4,7: response status SENT
    }
    
    /**
     * test with ISC: search with specifying response version 2 and response status SENT in two metadata sets
     * one annotation is DELETED (5); we ask for DELETED ones
     * -> should return one annotation (5)
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2AndSent_OneDeleted_SearchDeleted() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all SENT -> could be found

        ann5.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(ann5);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.of(Arrays.asList(AnnotationStatus.DELETED)));
        checkResult(result, Arrays.asList(ann5)); // 5 was DELETED and has version 2
    }

    /**
     * test with ISC: search with specifying response version 2 and response status SENT in two metadata sets
     *  we ask for DELETED ones, but none is DELETED
     * -> should not return any annotation
     */
    @Test
    public void testIscUserIsc_SearchForRespVers2AndSent_SearchDeleted() throws Exception {

        createTestData(Authorities.ISC);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.of(Arrays.asList(AnnotationStatus.DELETED)));
        checkResult(result, new ArrayList<Annotation>()); // none found since none is deleted
    }

    /**
     * test with EdiT: search with specifying response version 2 and response status SENT in two metadata sets
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForRespVers2AndSent() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying response version 2 and response status SENT in two metadata sets
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForRespVers2AndSent() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying response version 2 and response status SENT in two metadata sets
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForRespVers2AndSent() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForRespVers2AndSent(Optional.empty());
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForRespVers2AndSent(final Optional<List<AnnotationStatus>> statusesToSet) throws JsonProcessingException {

        final SimpleMetadata firstMetaToMatch = new SimpleMetadata(RESPVERS, "2");
        final SimpleMetadata secondMetaToMatch = new SimpleMetadata(RESPSTATUS, "SENT");

        List<AnnotationStatus> statuses = null;
        if (statusesToSet.isPresent()) statuses = statusesToSet.get();

        options.setMetadataMapsWithStatusesList(Arrays.asList(
                new SimpleMetadataWithStatuses(firstMetaToMatch, statuses),
                new SimpleMetadataWithStatuses(secondMetaToMatch, statuses)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 1 version 2 and response version 3 (unknown) 
     * and unknown ISC reference in three metadata sets
     * -> should return one annotation (4)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef2Vers1AndVers3AndUnknownRef() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4)); // some are SENT, but all candidates

        final AnnotationSearchResult result = runSearchForIscRef2Vers1AndVers3AndUnknownRef();
        checkResult(result, Arrays.asList(ann4)); // matches ISC reference 1 version 2
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 version 2 and response version 3 (unknown) 
     * and unknown ISC reference in three metadata sets
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef2Vers1AndVers3AndUnknownRef() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2Vers1AndVers3AndUnknownRef();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 version 2 and response version 3 (unknown) 
     * and unknown ISC reference in three metadata sets
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef2Vers1AndVers3AndUnknownRef() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2Vers1AndVers3AndUnknownRef();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 1 version 2 and response version 3 (unknown) 
     * and unknown ISC reference in three metadata sets
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef2Vers1AndVers3AndUnknownRef() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef2Vers1AndVers3AndUnknownRef();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef2Vers1AndVers3AndUnknownRef() throws JsonProcessingException {

        final SimpleMetadata firstMetaToMatch = new SimpleMetadata(ISCREF, ISCREF2);
        firstMetaToMatch.put(RESPVERS, "1");
        final SimpleMetadata secondMetaToMatch = new SimpleMetadata(RESPVERS, "3");
        final SimpleMetadata thirdMetaToMatch = new SimpleMetadata(ISCREF, "ISC/4711");

        options.setMetadataMapsWithStatusesList(Arrays.asList(
                new SimpleMetadataWithStatuses(firstMetaToMatch, null),
                new SimpleMetadataWithStatuses(secondMetaToMatch, null),
                new SimpleMetadataWithStatuses(thirdMetaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 3 (unknown)
     * -> should not return any annotation
     */
    @Test
    public void testIscUserIsc_SearchForUnknownIscRef() throws Exception {

        createTestData(Authorities.ISC);

        final AnnotationSearchResult result = runSearchForUnknownIscRef();
        checkResult(result, new ArrayList<Annotation>());
    }

    /**
     * test with EdiT: search with specifying ISC reference 3 (unknown)
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForUnknownIscRef() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForUnknownIscRef();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 3 (unknown)
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForUnknownIscRef() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForUnknownIscRef();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 3 (unknown)
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForUnknownIscRef() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForUnknownIscRef();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForUnknownIscRef() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, "someref");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying response status SENT
     * -> should return one annotation (7)
     */
    @Test
    public void testIscUserIsc_SearchForResponseStatusSent() throws Exception {

        createTestData(Authorities.ISC);

        final AnnotationSearchResult result = runSearchForResponseStatusSent();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT: search with specifying response status SENT
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForResponseStatusSent() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForResponseStatusSent();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying response status SENT
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForResponseStatusSent() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForResponseStatusSent();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying response status SENT
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForResponseStatusSent() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForResponseStatusSent();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForResponseStatusSent() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(RESPSTATUS, SENT);
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 1 and response status SENT
     * -> should return one annotation (7)
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1ResponseStatusSent() throws Exception {

        createTestData(Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1ResponseStatusSent();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 and response status SENT
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1ResponseStatusSent() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1ResponseStatusSent();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 and response status SENT
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1ResponseStatusSent() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1ResponseStatusSent();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 1 and response status SENT
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1ResponseStatusSent() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1ResponseStatusSent();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1ResponseStatusSent() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF1);
        metaToMatch.put(RESPSTATUS, SENT);
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 2 and response status SENT
     * -> should return annotations 4 and 5
     */
    @Test
    public void testIscUserIsc_SearchForIscRef2ResponseStatusSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7)); // all are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef2ResponseStatusSent();
        checkResult(result, Arrays.asList(ann4, ann5), Arrays.asList(ISCREF2));
    }

    /**
     * test with ISC: search with specifying ISC reference 2 and response status SENT
     * -> should return annotation 4 only as annotation 5 is not SENT yet
     */
    @Test
    public void testIscUserIsc_SearchForIscRef2ResponseStatusSent_NotAllSent() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann6, ann7)); // all except 5 are SENT -> could be found

        final AnnotationSearchResult result = runSearchForIscRef2ResponseStatusSent();
        checkResult(result, Arrays.asList(ann4), Arrays.asList(ISCREF2));
    }

    /**
     * test with EdiT: search with specifying ISC reference 2 and response status SENT
     * -> should return all annotations of the group (since metadata is ignored)
     */
    @Test
    public void testEditUserEdit_SearchForIscRef2ResponseStatusSent() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2ResponseStatusSent();
        checkResult(result, Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 2 and response status SENT
     * -> should return one annotation (7, i.e. the SENT one)
     */
    @Test
    public void testIscUserEdit_SearchForIscRef2ResponseStatusSent() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef2ResponseStatusSent();
        checkResult(result, Arrays.asList(ann7));
    }

    /**
     * test with EdiT annotations and ISC user: search with specifying ISC reference 2 and response status SENT
     * -> should not return any annotation (since ISC users do not see EdiT annotations)
     */
    @Test
    public void testEditUserIsc_SearchForIscRef2ResponseStatusSent() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef2ResponseStatusSent();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef2ResponseStatusSent() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF2);
        metaToMatch.put(RESPSTATUS, SENT);
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));

        return annotService.searchAnnotations(options, userInfo);
    }

    /**
     * test with ISC: search with specifying ISC reference 1 and the second group
     * -> should only return all annotations of all groups having ISC reference 1, despite the group being specified
     * ( group is no longer taken into account with ANOT-96) 
     */
    @Test
    public void testIscUserIsc_SearchForIscRef1InSecondGroup() throws Exception {

        createTestData(Authorities.ISC);
        setMetadataSent(Arrays.asList(ann1, ann2, ann3, ann4, ann5, ann6, ann7, annGroup2)); // all are SENT, could be found

        final AnnotationSearchResult result = runSearchForIscRef1InSecondGroup();
        checkResult(result, Arrays.asList(ann2, ann3, ann6, ann7, annGroup2));
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 and the second group
     * -> should return all annotations of the second group (since metadata is ignored) 
     */
    @Test
    public void testEditUserEdit_SearchForIscRef1InSecondGroup() throws Exception {

        createTestData(Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1InSecondGroup();
        checkResult(result, Arrays.asList(annGroup2));
    }

    /**
     * test with ISC annotations and EdiT user: search with specifying ISC reference 1 and the second group
     * -> should not return any annotation of the second group (as there is no SENT one) 
     */
    @Test
    public void testIscUserEdit_SearchForIscRef1InSecondGroup() throws Exception {

        createTestData(Authorities.ISC, Authorities.EdiT);

        final AnnotationSearchResult result = runSearchForIscRef1InSecondGroup();
        checkResult(result, new ArrayList<Annotation>());
    }

    /**
     * test with EdiT: search with specifying ISC reference 1 and the second group
     * -> should not return any annotation (since ISC users do not see EdiT annotations) 
     */
    @Test
    public void testEditUserIsc_SearchForIscRef1InSecondGroup() throws Exception {

        createTestData(Authorities.EdiT, Authorities.ISC);

        final AnnotationSearchResult result = runSearchForIscRef1InSecondGroup();
        checkResult(result, new ArrayList<Annotation>());
    }

    private AnnotationSearchResult runSearchForIscRef1InSecondGroup() throws JsonProcessingException {

        final SimpleMetadata metaToMatch = new SimpleMetadata();
        metaToMatch.put(ISCREF, ISCREF1);
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(metaToMatch, null)));
        options.setGroup(secondGroup.getName());

        return annotService.searchAnnotations(options, userInfo);
    }

    private void checkResult(final AnnotationSearchResult result, final List<Annotation> expectedAnnots) {
        checkResult(result, expectedAnnots, null);
    }

    // check the number of results and check that the expected annotations are received
    private void checkResult(final AnnotationSearchResult result, final List<Annotation> expectedAnnots,
            final List<String> metadataValuesToMatch) {

        Assert.assertEquals("Different number of annotations received!",
                expectedAnnots.size(), result.getTotalItems());

        for (final Annotation ann : expectedAnnots) {
            // we compare the received and expected ID
            Assert.assertTrue("Annotation with id '" + ann.getId() + " not received!",
                    result.getItems().stream().anyMatch(annot -> annot.getId().equals(ann.getId())));

            if (!CollectionUtils.isEmpty(metadataValuesToMatch)) {
                metadataValuesToMatch.forEach(metaToMatch -> Assert.assertTrue(ann.getMetadata().getKeyValuePairs().contains(metaToMatch)));
            }
        }
    }

}
