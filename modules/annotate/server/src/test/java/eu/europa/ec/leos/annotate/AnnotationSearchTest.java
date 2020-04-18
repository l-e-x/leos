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

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class AnnotationSearchTest {

    /**
     * Tests on search functionality for annotations 
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
    private AnnotationConversionService conversionService;
    
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
    private static final String FIRST_URL = "http://myurl.net";
    private static final String SECOND_URL = "http://seconddoc.net/id=5";

    private static final String FIRST_USER_LOGIN = "firstUserLogin";
    private static final String SECOND_USER_LOGIN = "secondUserLogin";

    private static final String ID_PUBANNOT_USER1_DOC1 = "id1_1";
    private static final String ID_PUBREPLY_USER1_DOC1 = "id1_r1";
    private static final String ID_PUBANNOT2_USER1_DOC1 = "id1.2_1";
    private static final String ID_PRIVREPLY_USER2_DOC1 = "id1_r2";
    private static final String ID_PRIVANNOT_USER2_DOC1 = "id2_1";
    private static final String ID_PUBANNOT_USER2_DOC2 = "id2_2";

    private static final String AUTHORITY = Authorities.EdiT;

    private static final String WORLDGROUP = "__world__";
    private static final String ASC = "asc";
    private static final String DESC = "desc";
    private static final String CREATED = "created";
    private static final String UPDATED = "updated";
    private static final String UNEXPECTED_EXC = "Unexpected exception received: ";

    private UserInformation firstUserInfo, secondUserInfo;

    @Before
    public void cleanDatabaseBeforeTests() throws URISyntaxException {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // insert two users, assign them to the default group
        final User firstUser = new User(FIRST_USER_LOGIN);
        final User secondUser = new User(SECOND_USER_LOGIN);
        userRepos.save(Arrays.asList(firstUser, secondUser));
        userGroupRepos.save(new UserGroup(firstUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));

        firstUserInfo = new UserInformation(
                new Token(firstUser, AUTHORITY, "acc1", LocalDateTime.now().plusMinutes(5), "ref1", LocalDateTime.now().plusMinutes(5)));
        secondUserInfo = new UserInformation(
                new Token(secondUser, AUTHORITY, "acc1", LocalDateTime.now().plusMinutes(5), "ref1", LocalDateTime.now().plusMinutes(5)));

        // insert two documents
        final Document firstDoc = new Document(new URI(FIRST_URL), "first document's title");
        final Document secondDoc = new Document(new URI(SECOND_URL), null);
        documentRepos.save(Arrays.asList(firstDoc, secondDoc));

        // save metadata for the two documents
        final Metadata firstMeta = new Metadata(firstDoc, defaultGroup, AUTHORITY);
        final Metadata secondMeta = new Metadata(secondDoc, defaultGroup, AUTHORITY);
        metadataRepos.save(Arrays.asList(firstMeta, secondMeta));

        createTestData(firstDoc, secondDoc, firstMeta, secondMeta, firstUser, secondUser);
    }

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private void createTestData(final Document firstDoc, final Document secondDoc, 
            final Metadata firstMeta, final Metadata secondMeta,
            final User firstUser, final User secondUser) {
        
        // insert annotations
        // first line: annotations/replies
        // second line: private annotations/private replies

        // ------ | document 1 | document 2
        // user 1 | --- 2/1*-- | --- 0/0 --
        // ------ | --- 0/0 -- | --- 0/0 --
        // user 2 | --- 0/0 -- | --- 1/0 --
        // ------ | --- 1/1*-- | --- 0/0 --
        // * = replies to the first user's annotation

        // two dummy selectors
        final String dummySelectorFirstDoc = "[{\"selector\":null,\"source\":\"" + firstDoc.getUri() + "\"}]";
        final String dummySelectorSecondDoc = "[{\"selector\":null,\"source\":\"" + secondDoc.getUri() + "\"}]";

        // public annotation of first user to first document
        // visible for first user, second user
        final Annotation firstAnnotToFirstDoc = new Annotation();
        firstAnnotToFirstDoc.setId(ID_PUBANNOT_USER1_DOC1);
        firstAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 21, 12, 0));
        firstAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 22, 12, 0));
        firstAnnotToFirstDoc.setMetadata(firstMeta);
        firstAnnotToFirstDoc.setReferences("");
        firstAnnotToFirstDoc.setShared(true);
        firstAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        firstAnnotToFirstDoc.setText("first annotation's content");
        firstAnnotToFirstDoc.setUser(firstUser);
        annotRepos.save(firstAnnotToFirstDoc);

        // public reply of first user to his public annotation
        // visible for first user, second user
        final Annotation replyToAnnotToFirstDoc = new Annotation();
        replyToAnnotToFirstDoc.setId(ID_PUBREPLY_USER1_DOC1);
        replyToAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToAnnotToFirstDoc.setMetadata(firstMeta);
        replyToAnnotToFirstDoc.setReferences(ID_PUBANNOT_USER1_DOC1);
        replyToAnnotToFirstDoc.setShared(true);
        replyToAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        replyToAnnotToFirstDoc.setText("second annotation to first doc");
        replyToAnnotToFirstDoc.setUser(firstUser);
        annotRepos.save(replyToAnnotToFirstDoc);

        // another public annotation of first user to first document
        // visible for first user, second user
        final Annotation secondAnnotToFirstDoc = new Annotation();
        secondAnnotToFirstDoc.setId(ID_PUBANNOT2_USER1_DOC1);
        secondAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 25, 12, 0));
        secondAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 27, 12, 0));
        secondAnnotToFirstDoc.setMetadata(firstMeta);
        secondAnnotToFirstDoc.setReferences("");
        secondAnnotToFirstDoc.setShared(true);
        secondAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        secondAnnotToFirstDoc.setText("second annotation's content");
        secondAnnotToFirstDoc.setUser(firstUser);
        annotRepos.save(secondAnnotToFirstDoc);

        // private reply of second user to first users' public annotation in first document
        // visible for second user
        @SuppressWarnings("PMD.LongVariable")
        final Annotation replySecondUserToFirstUserAnnotToFirstDoc = new Annotation();
        replySecondUserToFirstUserAnnotToFirstDoc.setId(ID_PRIVREPLY_USER2_DOC1);
        replySecondUserToFirstUserAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replySecondUserToFirstUserAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replySecondUserToFirstUserAnnotToFirstDoc.setMetadata(firstMeta);
        replySecondUserToFirstUserAnnotToFirstDoc.setReferences(ID_PUBANNOT_USER1_DOC1); // public annotation of first user in first document
        replySecondUserToFirstUserAnnotToFirstDoc.setShared(false);
        replySecondUserToFirstUserAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        replySecondUserToFirstUserAnnotToFirstDoc.setText("private reply of second user to first doc's annotation of first user");
        replySecondUserToFirstUserAnnotToFirstDoc.setUser(secondUser);
        annotRepos.save(replySecondUserToFirstUserAnnotToFirstDoc);

        // insert a private annotation of second user to first document
        // visible for second user
        @SuppressWarnings("PMD.LongVariable")
        final Annotation firstAnnotSecondUserToFirstDoc = new Annotation();
        firstAnnotSecondUserToFirstDoc.setId(ID_PRIVANNOT_USER2_DOC1);
        firstAnnotSecondUserToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 22, 12, 0));
        firstAnnotSecondUserToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 24, 12, 0));
        firstAnnotSecondUserToFirstDoc.setMetadata(firstMeta);
        firstAnnotSecondUserToFirstDoc.setReferences("");
        firstAnnotSecondUserToFirstDoc.setShared(false);
        firstAnnotSecondUserToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        firstAnnotSecondUserToFirstDoc.setText("first annotation of second user to first document");
        firstAnnotSecondUserToFirstDoc.setUser(secondUser);
        annotRepos.save(firstAnnotSecondUserToFirstDoc);

        // insert a public annotation of second user to second document
        // visible for first user, second user
        @SuppressWarnings("PMD.LongVariable")
        final Annotation firstAnnotSecondUserToSecondDoc = new Annotation();
        firstAnnotSecondUserToSecondDoc.setId(ID_PUBANNOT_USER2_DOC2);
        firstAnnotSecondUserToSecondDoc.setCreated(LocalDateTime.of(2012, 12, 22, 12, 0));
        firstAnnotSecondUserToSecondDoc.setUpdated(LocalDateTime.of(2012, 12, 24, 12, 0));
        firstAnnotSecondUserToSecondDoc.setMetadata(secondMeta);
        firstAnnotSecondUserToSecondDoc.setReferences("");
        firstAnnotSecondUserToSecondDoc.setShared(true);
        firstAnnotSecondUserToSecondDoc.setTargetSelectors(dummySelectorSecondDoc);
        firstAnnotSecondUserToSecondDoc.setText("first annotation of second user to second document");
        firstAnnotSecondUserToSecondDoc.setUser(secondUser);
        annotRepos.save(firstAnnotSecondUserToSecondDoc);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test: search does not work without any search parameters specified
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSearchDoesNotWorkWithoutOptions() throws Exception {

        annotService.searchAnnotations(null, firstUserInfo); // no Options supplied
    }

    /**
     * test: search does not work without specifying user login
     */
    @Test(expected = IllegalArgumentException.class)
    public void testSearchDoesNotWorkWithoutLogin() throws Exception {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                ASC, CREATED);         // order, sort column

        annotService.searchAnnotations(options, null); // no user login supplied
    }

    /**
     * simple search: group and URI set
     */
    @Test
    public void testSearchGroupAndUri() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                ASC, CREATED);         // order, sort column

        // run search for first user
        AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);

        Assert.assertNotNull(annots);

        // three annotations in total on first document (his two own public annotation and his own public reply to it)
        Assert.assertEquals(2, annots.size());
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_USER1_DOC1);

        List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);
        assertAnnotationContained(replies, ID_PUBREPLY_USER1_DOC1);

        // run search for second user
        annots = annotService.searchAnnotations(options, secondUserInfo);

        Assert.assertNotNull(annots);

        // three annotations in total on first document:
        // - first user's public annotation having one public reply from both users each
        // - another public annotation from first user
        // - and one private annotation of second user
        Assert.assertEquals(3, annots.size());
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_USER1_DOC1);
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT2_USER1_DOC1);
        assertAnnotationContained(annots.getItems(), ID_PRIVANNOT_USER2_DOC1);

        replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);
        assertAnnotationContained(replies, ID_PUBREPLY_USER1_DOC1);
        assertAnnotationContained(replies, ID_PRIVREPLY_USER2_DOC1);

        // verify ordering of items
        assertDatesAscending(annots.getItems().get(0).getCreated(), annots.getItems().get(1).getCreated());
        assertDatesAscending(annots.getItems().get(1).getCreated(), annots.getItems().get(2).getCreated());
        assertDatesAscending(replies.get(0).getCreated(), replies.get(1).getCreated());
    }

    /**
     * simple search (eight following tests): group, URI and user set
     * verify all eight combinations of requesting user/searched user/document
     * 
     * CAUTION: this test might twist your brain! Think carefully.
     * Some hints:
     * when the annotations of ANOTHER user are searched for, only his public annotations are given - and the replies thereof
     * so e.g. my private replies to his public annotations are returned, but not my public replies to his PRIVATE annotations!
     */

    // 1) search from first user's perspective for his own annotations in first document
    @Test
    public void testSearchGroupAndUriAndUser_firstUser_ownAnnots_firstDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                DESC, UPDATED);        // order, sort column
        options.setUser(FIRST_USER_LOGIN);

        // search from first user's perspective for his own annotations in first document
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(2, annots.size()); // only two of the items in total on first document are from first user (2 public annotations)
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_USER1_DOC1);
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT2_USER1_DOC1);
        Assert.assertEquals(1, replies.size());
        assertAnnotationContained(replies, ID_PUBREPLY_USER1_DOC1);

        // verify ordering of items
        assertDatesAscending(annots.getItems().get(1).getUpdated(), annots.getItems().get(0).getUpdated());
    }

    // 2) search from first user's perspective for second user's entries in first document
    @Test
    public void testSearchGroupAndUriAndUser_firstUser_secondUserAnnots_firstDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                DESC, UPDATED);        // order, sort column

        // search from first user's perspective for second user's entries in first document
        options.setUser(SECOND_USER_LOGIN);

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size()); // second user only has private items in first document -> not visible for first user
        Assert.assertNotNull(replies);
        Assert.assertEquals(0, replies.size());
    }

    // 3) search from second user's perspective for first user's entries in first document
    @Test
    public void testSearchGroupAndUriAndUser_secondUser_firstUserAnnots_firstDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                DESC, UPDATED);        // order, sort column
        options.setUser(FIRST_USER_LOGIN);

        // search from second user's perspective for first user's entries in first document
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, secondUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(2, annots.size()); // first user's two public annotations and one public reply in first doc -> both visible for second user
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_USER1_DOC1);
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT2_USER1_DOC1);
        Assert.assertNotNull(replies);
        Assert.assertEquals(2, replies.size());
        assertAnnotationContained(replies, ID_PUBREPLY_USER1_DOC1);
        assertAnnotationContained(replies, ID_PRIVREPLY_USER2_DOC1);

        // verify ordering of items
        assertDatesAscending(annots.getItems().get(1).getUpdated(), annots.getItems().get(0).getUpdated());
    }

    // 4) search from second user's perspective for his own annotations in first document
    @Test
    public void testSearchGroupAndUriAndUser_secondUser_secondUserAnnots_firstDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                DESC, UPDATED);        // order, sort column

        // search from second user's perspective for his own annotations in first document
        options.setUser(SECOND_USER_LOGIN);

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, secondUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // private annotation of second user
        assertAnnotationContained(annots.getItems(), ID_PRIVANNOT_USER2_DOC1);
        Assert.assertNotNull(replies);
        Assert.assertEquals(0, replies.size()); // note: private reply to first user's annotation is not returned, as the base annotation is from other user
    }

    // 5) search from first user's perspective for his entries in second document
    @Test
    public void testSearchGroupAndUriAndUser_firstUser_firstUserAnnots_secondDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                SECOND_URL, WORLDGROUP, // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                DESC, UPDATED);         // order, sort column

        // search from first user's perspective for his entries in second document
        options.setUser(FIRST_USER_LOGIN);

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size()); // no annotations!
        Assert.assertEquals(0, replies.size()); // and thus no replies
    }

    // 6) search from first user's perspective for second user's entries in second document
    @Test
    public void testSearchGroupAndUriAndUser_firstUser_secondUserAnnots_secondDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                SECOND_URL, WORLDGROUP, // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                DESC, UPDATED);         // order, sort column

        // search from first user's perspective for second user's entries in second document
        options.setUser(SECOND_USER_LOGIN);

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // one public annotation
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_USER2_DOC2);
        Assert.assertEquals(0, replies.size()); // but no reply
    }

    // 7) search from second user's perspective for first user's entries in second document
    @Test
    public void testSearchGroupAndUriAndUser_secondUser_firstUserAnnots_secondDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                SECOND_URL, WORLDGROUP, // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                DESC, UPDATED);         // order, sort column

        // search from second user's perspective for first user's entries in second document
        options.setUser(FIRST_USER_LOGIN);

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, secondUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size()); // nothing
        Assert.assertEquals(0, replies.size()); // thus nothing either
    }

    // 8) search from second user's perspective for his own entries in second document
    @Test
    public void testSearchGroupAndUriAndUser_secondUser_secondUserAnnots_secondDocument() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                SECOND_URL, WORLDGROUP, // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                DESC, UPDATED);         // order, sort column

        // search from second user's perspective for his own entries in second document
        options.setUser(SECOND_USER_LOGIN);

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, secondUserInfo);
        final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);

        // verify
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // one public annotation
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_USER2_DOC2);
        Assert.assertEquals(0, replies.size()); // but no replies
    }

    /**
     * simple search: group and URI set, but group is unknown
     */
    @Test
    public void testSearchUnknownGroupAndUri() {

        // search for different (unknown) group
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "unknownGroup", // URI, group (unknown)
                false,                     // provide separate replies
                200, 0,                    // limit, offset
                ASC, CREATED);             // order, sort column

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size());
    }

    /**
     * simple search: group and URI set, but user is unknown
     */
    @Test
    public void testSearchUnknownUser() {

        // search for unknown user
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group (unknown)
                false,                 // provide separate replies
                200, 0,                // limit, offset
                ASC, CREATED);         // order, sort column
        options.setUser("unknown@user.com"); // unknown user

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size());
    }

    /**
     * simple search: URI unknown
     */
    @Test
    public void testSearchUnknownUri() {

        // search for unknown URI
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                "https://unknown.uri/here", // URI (unknown)
                WORLDGROUP, false,          // group, provide separate replies
                200, 0,                     // limit, offset
                ASC, CREATED);              // order, sort column

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size());
    }

    /**
     * simple search: group and URI set; result limited to 1 item
     */
    @Test
    public void testSearchGroupAndUri_requestOnlyOneResult() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                1, 0,                  // limit, offset
                ASC, CREATED);         // order, sort column

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // there are two items in the DB, but only one was requested!
    }

    /**
     * simple search: group and URI set; result offset exceeds amount of available results
     */
    @Test
    public void testSearchGroupAndUri_OffsetTooHigh() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 10,               // limit, offset (higher than number of results)
                ASC, CREATED);         // order, sort column

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size()); // there are two items in the DB, but the requested offset is too large
    }

    /**
     * simple search: group and URI set; limit is set to negative value -> provide all items
     */
    @Test
    public void testSearchGroupAndUri_NegativeLimit_ProvideAll() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                -1, 100,               // limit (negative->provide all items), offset (should be ignored)
                ASC, CREATED);         // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(2, annots.size()); // there are two annotations in the DB that user 1 can see for document 1 - is returned, despite offset "100"

        replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);
        Assert.assertNotNull(replies);
        Assert.assertEquals(1, replies.size()); // one reply

        annots = annotService.searchAnnotations(options, secondUserInfo);
        Assert.assertNotNull(annots);
        replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);
        Assert.assertNotNull(replies);

        // there are three annotations in the DB that user 2 can see for document 1 (his own, and two of user 1), and two replies
        Assert.assertEquals(3, annots.size());
        Assert.assertEquals(2, replies.size());

        /**
         *  repeat search for second document
         */
        options.setUri(SECOND_URL);

        annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // there is one item in the DB that user 1 can see for document 2 (from user 2)

        annots = annotService.searchAnnotations(options, secondUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // there is one item in the DB that user 2 can see for document 2 (his own)
    }

    /**
     * simple search: group and URI set; limit is set to zero value -> use default limit, keep positive offset
     */
    @Test
    public void testSearchGroupAndUri_ZeroLimit_DifferentOffsets() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                0, 1,                  // limit (zero->use default), offset positive (thus keep)
                ASC, CREATED);         // order, sort column

        AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);

        // there are two items in the DB that user 1 can see for document 1 - but only one is returned (due to offset "1")
        Assert.assertEquals(1, annots.size());

        // verification that there really is the one item that was skipped before (by using offset 0)
        options.setItemLimitAndOffset(1, 0);
        annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertEquals(1, annots.size());
    }

    /**
     * simple search: group and URI set; limit is set to zero value -> use default limit, change negative offset
     */
    @Test
    public void testSearchGroupAndUri_ZeroLimit_NegativeOffset() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                0, -5,                 // limit (zero->use default), offset negative -> default offset to be used
                ASC, CREATED);         // order, sort column

        final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
        Assert.assertNotNull(annots);
        Assert.assertEquals(2, annots.size()); // there are two annotations in the DB that user 1 can see for document 1 - is returned due to default offset
    }

    /**
     * test search item conversion - no separate replies
     */
    @Test
    public void testSearchNoSeparateReplies() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                false,                 // provide separate replies
                200, 0,                // limit, offset
                ASC, CREATED);         // order, sort column

        JsonSearchResult result = null;

        try {
            final AnnotationSearchResult annots = annotService.searchAnnotations(options, secondUserInfo);
            final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);
            result = conversionService.convertToJsonSearchResult(annots, replies, options, secondUserInfo);
        } catch (Exception e) {
            Assert.fail(UNEXPECTED_EXC + e);
        }

        Assert.assertNotNull(result);
        Assert.assertFalse(result instanceof JsonSearchResultWithSeparateReplies);
        Assert.assertEquals(3, result.getTotal()); // three annotations (the two replies are not counted)
        Assert.assertEquals(5, result.getRows().size());

        // second user can see all annotations of first user and his own (which are private to him)
        assertJsonAnnotationContained(result.getRows(), ID_PUBANNOT_USER1_DOC1);
        assertJsonAnnotationContained(result.getRows(), ID_PUBREPLY_USER1_DOC1);
        assertJsonAnnotationContained(result.getRows(), ID_PUBANNOT2_USER1_DOC1);
        assertJsonAnnotationContained(result.getRows(), ID_PRIVANNOT_USER2_DOC1);
        assertJsonAnnotationContained(result.getRows(), ID_PRIVREPLY_USER2_DOC1);
    }

    /**
     * test search item conversion - separate replies
     */
    @Test
    public void testSearchSeparateReplies() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                true,                  // provide separate replies
                200, 0,                // limit, offset
                ASC, CREATED);         // order, sort column

        JsonSearchResult result = null;

        try {
            final AnnotationSearchResult annots = annotService.searchAnnotations(options, firstUserInfo);
            final List<Annotation> replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);
            result = conversionService.convertToJsonSearchResult(annots, replies, options, firstUserInfo);
        } catch (Exception e) {
            Assert.fail(UNEXPECTED_EXC + e);
        }

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof JsonSearchResultWithSeparateReplies);

        final JsonSearchResultWithSeparateReplies jsrwsr = (JsonSearchResultWithSeparateReplies) result;
        Assert.assertEquals(2, jsrwsr.getRows().size()); // two annotations...
        Assert.assertEquals(1, jsrwsr.getReplies().size()); // ... and one reply
        Assert.assertEquals(2, jsrwsr.getTotal()); // total shows number of annotations without taking replies into account

        // check that it's the first user's public annotation and reply that were found
        assertJsonAnnotationContained(jsrwsr.getRows(), ID_PUBANNOT_USER1_DOC1);
        assertJsonAnnotationContained(jsrwsr.getReplies(), ID_PUBREPLY_USER1_DOC1);

        // check that the 'rows' contains annotations
        jsrwsr.getRows().stream().allMatch(jsonAnnot -> !jsonAnnot.isReply());

        // check that the 'replies' contains replies
        jsrwsr.getReplies().stream().allMatch(jsonAnnot -> jsonAnnot.isReply());
    }

    /**
     * test that input is returned when no annotations are specified for being the replies' parents
     */
    @Test
    public void testCannotSearchRepliesWithoutAnnotations() {

        // annotations null -> null
        Assert.assertNull(annotService.searchRepliesForAnnotations(null, null, null));

        // annotations list empty -> empty list
        Assert.assertEquals(0, annotService.searchRepliesForAnnotations(new AnnotationSearchResult(), null, null).size());
    }

    /**
     * test that exception is thrown when no annotations search options are specified
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCannotSearchRepliesWithoutSearchOptions() {

        // dummy annotations list, but no options -> exception
        final AnnotationSearchResult dummy = new AnnotationSearchResult();
        dummy.getItems().add(new Annotation());
        annotService.searchRepliesForAnnotations(dummy, null, null); // should throw IllegalArgumentException
    }

    /**
     * test that exception is thrown when no user information is specified
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCannotSearchRepliesWithoutUserInfo() {

        // dummy annotations list, options, but no user information -> exception
        final AnnotationSearchResult dummy = new AnnotationSearchResult();
        dummy.getItems().add(new Annotation());
        final AnnotationSearchOptions options = new AnnotationSearchOptions("uri", "group", true, 100, 0, ASC, CREATED);
        annotService.searchRepliesForAnnotations(dummy, options, null); // should throw IllegalArgumentException
    }

    /**
     * test search result conversion without any valid parameter -> errors expected
     */
    @Test
    public void testConvertSearchResultsWithoutParameters() {

        Assert.assertNull(conversionService.convertToJsonSearchResult(null, null, null, null));
    }

    /**
     * test search result conversion without options -> considered as separate replies set to false
     */
    @Test
    public void testConvertSearchResultsWithoutOptions() {

        final AnnotationSearchResult annots = new AnnotationSearchResult();
        final List<Annotation> replies = new ArrayList<Annotation>();

        final JsonSearchResult result = conversionService.convertToJsonSearchResult(annots, replies, null, null);
        Assert.assertNull(result);
    }

    /**
     * test search result conversion without replies
     */
    @Test
    public void testConvertSearchResultsWithoutReplies() {

        final AnnotationSearchResult annots = new AnnotationSearchResult();

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, WORLDGROUP, // URI, group
                true,                  // provide separate replies
                200, 0,                // limit, offset
                ASC, CREATED);         // order, sort column

        JsonSearchResult result = conversionService.convertToJsonSearchResult(annots, null, options, firstUserInfo);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof JsonSearchResultWithSeparateReplies);

        // change options to not provide separate replies
        options.setSeparateReplies(false);
        result = conversionService.convertToJsonSearchResult(annots, null, options, firstUserInfo);
        Assert.assertNotNull(result);
        Assert.assertFalse(result instanceof JsonSearchResultWithSeparateReplies);
    }

    // helper function for uncomfortable Java Date comparison
    private void assertDatesAscending(final LocalDateTime older, final LocalDateTime newer) {
        Assert.assertTrue(older.isBefore(newer) || older.equals(newer));
    }

    private void assertAnnotationContained(final List<Annotation> items, final String annotId) {
        Assert.assertTrue(items.stream().anyMatch(ann -> ann.getId().equals(annotId)));
    }

    private void assertJsonAnnotationContained(final List<JsonAnnotation> items, final String annotId) {
        Assert.assertTrue(items.stream().anyMatch(ann -> ann.getId().equals(annotId)));
    }
}
