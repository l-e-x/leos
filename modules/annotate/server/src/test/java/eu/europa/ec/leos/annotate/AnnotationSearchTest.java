/*
 * Copyright 2018 European Commission
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
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
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
    private AnnotationRepository annotRepos;

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

    private static final String ID_PUBANNOT_FIRSTUSER_FIRSTDOC = "id1_1";
    private static final String ID_PUBREPLY_FIRSTUSER_FIRSTDOC = "id1_r1";
    private static final String ID_PUBANNOT2_FIRSTUSER_FIRSTDOC = "id1.2_1";
    private static final String ID_PRIVREPLY_SECONDUSER_FIRSTDOC = "id1_r2";
    private static final String ID_PRIVANNOT_SECONDUSER_FIRSTDOC = "id2_1";
    private static final String ID_PUBANNOT_SECONDUSER_SECONDDOC = "id2_2";

    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // insert two users, assign them to the default group
        User firstUser = new User(FIRST_USER_LOGIN);
        User secondUser = new User(SECOND_USER_LOGIN);
        userRepos.save(Arrays.asList(firstUser, secondUser));
        userGroupRepos.save(new UserGroup(firstUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));

        // insert two documents
        Document firstDoc = new Document(new URI(FIRST_URL), "first document's title");
        Document secondDoc = new Document(new URI(SECOND_URL), null);
        documentRepos.save(Arrays.asList(firstDoc, secondDoc));

        // save metadata for the two documents
        Metadata firstMeta = new Metadata(firstDoc, defaultGroup, "LEOS");
        Metadata secondMeta = new Metadata(secondDoc, defaultGroup, "LEOS");
        metadataRepos.save(Arrays.asList(firstMeta, secondMeta));

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
        String dummySelectorFirstDoc = "[{\"selector\":null,\"source\":\"" + firstDoc.getUri() + "\"}]";
        String dummySelectorSecondDoc = "[{\"selector\":null,\"source\":\"" + secondDoc.getUri() + "\"}]";

        // public annotation of first user to first document
        // visible for first user, second user
        Annotation firstAnnotToFirstDoc = new Annotation();
        firstAnnotToFirstDoc.setId(ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        firstAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 21, 12, 0));
        firstAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 22, 12, 0));
        firstAnnotToFirstDoc.setDocument(firstDoc);
        firstAnnotToFirstDoc.setGroup(defaultGroup);
        firstAnnotToFirstDoc.setMetadata(firstMeta);
        firstAnnotToFirstDoc.setReferences("");
        firstAnnotToFirstDoc.setShared(true);
        firstAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        firstAnnotToFirstDoc.setText("first annotation's content");
        firstAnnotToFirstDoc.setUser(firstUser);
        annotRepos.save(firstAnnotToFirstDoc);

        // public reply of first user to his public annotation
        // visible for first user, second user
        Annotation replyToAnnotToFirstDoc = new Annotation();
        replyToAnnotToFirstDoc.setId(ID_PUBREPLY_FIRSTUSER_FIRSTDOC);
        replyToAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToAnnotToFirstDoc.setDocument(firstDoc);
        replyToAnnotToFirstDoc.setGroup(defaultGroup);
        replyToAnnotToFirstDoc.setMetadata(firstMeta);
        replyToAnnotToFirstDoc.setReferences(ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        replyToAnnotToFirstDoc.setShared(true);
        replyToAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        replyToAnnotToFirstDoc.setText("second annotation to first doc");
        replyToAnnotToFirstDoc.setUser(firstUser);
        annotRepos.save(replyToAnnotToFirstDoc);

        // another public annotation of first user to first document
        // visible for first user, second user
        Annotation secondAnnotToFirstDoc = new Annotation();
        secondAnnotToFirstDoc.setId(ID_PUBANNOT2_FIRSTUSER_FIRSTDOC);
        secondAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 25, 12, 0));
        secondAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 27, 12, 0));
        secondAnnotToFirstDoc.setDocument(firstDoc);
        secondAnnotToFirstDoc.setGroup(defaultGroup);
        secondAnnotToFirstDoc.setMetadata(firstMeta);
        secondAnnotToFirstDoc.setReferences("");
        secondAnnotToFirstDoc.setShared(true);
        secondAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        secondAnnotToFirstDoc.setText("second annotation's content");
        secondAnnotToFirstDoc.setUser(firstUser);
        annotRepos.save(secondAnnotToFirstDoc);

        // private reply of second user to first users' public annotation in first document
        // visible for second user
        Annotation replySecondUserToFirstUserAnnotToFirstDoc = new Annotation();
        replySecondUserToFirstUserAnnotToFirstDoc.setId(ID_PRIVREPLY_SECONDUSER_FIRSTDOC);
        replySecondUserToFirstUserAnnotToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replySecondUserToFirstUserAnnotToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replySecondUserToFirstUserAnnotToFirstDoc.setDocument(firstDoc);
        replySecondUserToFirstUserAnnotToFirstDoc.setGroup(defaultGroup);
        replySecondUserToFirstUserAnnotToFirstDoc.setMetadata(firstMeta);
        replySecondUserToFirstUserAnnotToFirstDoc.setReferences(ID_PUBANNOT_FIRSTUSER_FIRSTDOC); // public annotation of first user in first document
        replySecondUserToFirstUserAnnotToFirstDoc.setShared(false);
        replySecondUserToFirstUserAnnotToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        replySecondUserToFirstUserAnnotToFirstDoc.setText("private reply of second user to first doc's annotation of first user");
        replySecondUserToFirstUserAnnotToFirstDoc.setUser(secondUser);
        annotRepos.save(replySecondUserToFirstUserAnnotToFirstDoc);

        // insert a private annotation of second user to first document
        // visible for second user
        Annotation firstAnnotSecondUserToFirstDoc = new Annotation();
        firstAnnotSecondUserToFirstDoc.setId(ID_PRIVANNOT_SECONDUSER_FIRSTDOC);
        firstAnnotSecondUserToFirstDoc.setCreated(LocalDateTime.of(2012, 12, 22, 12, 0));
        firstAnnotSecondUserToFirstDoc.setUpdated(LocalDateTime.of(2012, 12, 24, 12, 0));
        firstAnnotSecondUserToFirstDoc.setDocument(firstDoc);
        firstAnnotSecondUserToFirstDoc.setGroup(defaultGroup);
        firstAnnotSecondUserToFirstDoc.setMetadata(firstMeta);
        firstAnnotSecondUserToFirstDoc.setReferences("");
        firstAnnotSecondUserToFirstDoc.setShared(false);
        firstAnnotSecondUserToFirstDoc.setTargetSelectors(dummySelectorFirstDoc);
        firstAnnotSecondUserToFirstDoc.setText("first annotation of second user to first document");
        firstAnnotSecondUserToFirstDoc.setUser(secondUser);
        annotRepos.save(firstAnnotSecondUserToFirstDoc);

        // insert a public annotation of second user to second document
        // visible for first user, second user
        Annotation firstAnnotSecondUserToSecondDoc = new Annotation();
        firstAnnotSecondUserToSecondDoc.setId(ID_PUBANNOT_SECONDUSER_SECONDDOC);
        firstAnnotSecondUserToSecondDoc.setCreated(LocalDateTime.of(2012, 12, 22, 12, 0));
        firstAnnotSecondUserToSecondDoc.setUpdated(LocalDateTime.of(2012, 12, 24, 12, 0));
        firstAnnotSecondUserToSecondDoc.setDocument(secondDoc);
        firstAnnotSecondUserToSecondDoc.setGroup(defaultGroup);
        firstAnnotSecondUserToSecondDoc.setMetadata(secondMeta);
        firstAnnotSecondUserToSecondDoc.setReferences("");
        firstAnnotSecondUserToSecondDoc.setShared(true);
        firstAnnotSecondUserToSecondDoc.setTargetSelectors(dummySelectorSecondDoc);
        firstAnnotSecondUserToSecondDoc.setText("first annotation of second user to second document");
        firstAnnotSecondUserToSecondDoc.setUser(secondUser);
        annotRepos.save(firstAnnotSecondUserToSecondDoc);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test: search does not work without any search parameters specified
     */
    @Test
    public void testSearchDoesNotWorkWithoutOptions() {

        try {
            annotService.searchAnnotations(null, FIRST_USER_LOGIN); // no Options supplied
            Assert.fail("Expected exception of search due to missing options not received");
        } catch (IllegalArgumentException iae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }

    /**
     * test: search does not work without specifying user login
     */
    @Test
    public void testSearchDoesNotWorkWithoutLogin() {

        try {
            AnnotationSearchOptions options = new AnnotationSearchOptions(
                    FIRST_URL, "__world__",  // URI, group
                    false,                   // provide separate replies
                    200, 0,                  // limit, offset
                    "asc", "created");       // order, sort column

            annotService.searchAnnotations(options, ""); // no user login supplied
            Assert.fail("Expected exception of search due to missing user login not received");
        } catch (IllegalArgumentException iae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }

    /**
     * simple search: group and URI set
     */
    @Test
    public void testSearchGroupAndUri() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                "asc", "created");      // order, sort column

        List<Annotation> items = null;

        // run search for first user
        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        Assert.assertNotNull(items);

        // three annotation in total on first document (his two own public annotation and his own public reply to it)
        Assert.assertEquals(2, items.size());
        assertAnnotationContained(items, ID_PUBANNOT_FIRSTUSER_FIRSTDOC);

        List<Annotation> replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
        assertAnnotationContained(replies, ID_PUBREPLY_FIRSTUSER_FIRSTDOC);

        // run search for second user
        try {
            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        Assert.assertNotNull(items);

        // three annotations in total on first document:
        // - first user's public annotation having one public reply from both users each
        // - another public annotation from first user
        // - and one private annotation of second user
        Assert.assertEquals(3, items.size());
        assertAnnotationContained(items, ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        assertAnnotationContained(items, ID_PUBANNOT2_FIRSTUSER_FIRSTDOC);
        assertAnnotationContained(items, ID_PRIVANNOT_SECONDUSER_FIRSTDOC);

        replies = annotService.searchRepliesForAnnotations(items, options, SECOND_USER_LOGIN);
        assertAnnotationContained(replies, ID_PUBREPLY_FIRSTUSER_FIRSTDOC);
        assertAnnotationContained(replies, ID_PRIVREPLY_SECONDUSER_FIRSTDOC);

        // verify ordering of items
        assertDatesAscending(items.get(0).getCreated(), items.get(1).getCreated());
        assertDatesAscending(items.get(1).getCreated(), items.get(2).getCreated());
        assertDatesAscending(replies.get(0).getCreated(), replies.get(1).getCreated());
    }

    /**
     * simple search: group, URI and user set
     * verify all eight combinations of requesting user/searched user/document
     * 
     * CAUTION: this test might twist your brain! Think carefully.
     * Some hints:
     * when the annotations of ANOTHER user are searched for, only his public annotations are given - and the replies thereof
     * so e.g. my private replies to his public annotations are returned, but not my public replies to his PRIVATE annotations!
     */
    @Test
    public void testSearchGroupAndUriAndUser() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                "desc", "updated");     // order, sort column
        options.setUser(FIRST_USER_LOGIN);

        List<Annotation> items = null, replies = null;

        // 1) search from first user's perspective for his own annotations in first document
        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(2, items.size()); // only two of the items in total on first document are from first user (2 public annotations)
        assertAnnotationContained(items, ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        assertAnnotationContained(items, ID_PUBANNOT2_FIRSTUSER_FIRSTDOC);
        Assert.assertEquals(1, replies.size());
        assertAnnotationContained(replies, ID_PUBREPLY_FIRSTUSER_FIRSTDOC);

        // verify ordering of items
        assertDatesAscending(items.get(1).getUpdated(), items.get(0).getUpdated());

        // 2) search from first user's perspective for second user's entries in first document
        items = null;
        options.setUser(SECOND_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size()); // second user only has private items in first document -> not visible for first user
        Assert.assertNotNull(replies);
        Assert.assertEquals(0, replies.size());

        // 3) search from second user's perspective for first user's entries in first document
        items = null;
        options.setUser(FIRST_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(2, items.size()); // first user's two public annotations and one public reply in first doc -> both visible for second user
        assertAnnotationContained(items, ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        assertAnnotationContained(items, ID_PUBANNOT2_FIRSTUSER_FIRSTDOC);
        Assert.assertNotNull(replies);
        Assert.assertEquals(1, replies.size());
        assertAnnotationContained(replies, ID_PUBREPLY_FIRSTUSER_FIRSTDOC);

        // verify ordering of items
        assertDatesAscending(items.get(1).getUpdated(), items.get(0).getUpdated());

        // 4) search from second user's perspective for his own annotations in first document
        items = null;
        options.setUser(SECOND_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, SECOND_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size()); // private annotation of second user
        assertAnnotationContained(items, ID_PRIVANNOT_SECONDUSER_FIRSTDOC);
        Assert.assertNotNull(replies);
        Assert.assertEquals(0, replies.size()); // note: private reply to first user's annotation is not returned, as the base annotation is from other user

        // 5) search from first user's perspective for his entries in second document
        items = null;
        options = new AnnotationSearchOptions(
                SECOND_URL, "__world__", // URI, group
                false,                   // provide separate replies
                200, 0,                  // limit, offset
                "desc", "updated");      // order, sort column
        options.setUser(FIRST_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size()); // no annotations!
        Assert.assertEquals(0, replies.size()); // and thus no replies

        // 6) search from first user's perspective for second user's entries in second document
        items = null;
        options.setUser(SECOND_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size()); // one public annotation
        assertAnnotationContained(items, ID_PUBANNOT_SECONDUSER_SECONDDOC);
        Assert.assertEquals(0, replies.size()); // but no reply

        // 7) search from second user's perspective for first user's entries in second document
        items = null;
        options = new AnnotationSearchOptions(SECOND_URL, "__world__", false, 200, 0, "desc", "updated");
        options.setUser(FIRST_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, SECOND_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size()); // nothing
        Assert.assertEquals(0, replies.size()); // thus nothing either

        // 8) search from second user's perspective for his own entries in second document
        items = null;
        options = new AnnotationSearchOptions(
                SECOND_URL, "__world__", // URI, group
                false,                   // provide separate replies
                200, 0,                  // limit, offset
                "desc", "updated");      // order, sort column
        options.setUser(SECOND_USER_LOGIN);

        try {
            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            replies = annotService.searchRepliesForAnnotations(items, options, SECOND_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(1, items.size()); // one public annotation
        assertAnnotationContained(items, ID_PUBANNOT_SECONDUSER_SECONDDOC);
        Assert.assertEquals(0, replies.size()); // but no replies
    }

    /**
     * simple search: group and URI set, but group is unknown
     */
    @Test
    public void testSearchUnknownGroupAndUri() {

        // search for different (unknown) group
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "unknownGroup", // URI, group (unknown)
                false,                     // provide separate replies
                200, 0,                    // limit, offset
                "asc", "created");         // order, sort column

        List<Annotation> items = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size());
    }

    /**
     * simple search: group and URI set, but user is unknown
     */
    @Test
    public void testSearchUnknownUser() {

        // search for unknown user
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group (unknown)
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                "asc", "created");      // order, sort column
        options.setUser("unknown@user.com"); // unknown user

        List<Annotation> items = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size());
    }

    /**
     * simple search: URI unknown
     */
    @Test
    public void testSearchUnknownUri() {

        // search for unknown URI
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "https://unknown.uri/here", // URI (unknown)
                "__world__", false,         // group, provide separate replies
                200, 0,                     // limit, offset
                "asc", "created");          // order, sort column

        List<Annotation> items = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size());
    }

    /**
     * simple search: group and URI set; result limited to 1 item
     */
    @Test
    public void testSearchGroupAndUri_requestOnlyOneResult() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                1, 0,                   // limit, offset
                "asc", "created");      // order, sort column

        List<Annotation> items = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size()); // there are two items in the DB, but only one was requested!
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }

    /**
     * simple search: group and URI set; result offset exceeds amount of available results
     */
    @Test
    public void testSearchGroupAndUri_OffsetTooHigh() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                200, 10,                // limit, offset (higher than number of results)
                "asc", "created");      // order, sort column

        List<Annotation> items = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(0, items.size()); // there are two items in the DB, but the requested offset is too large
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }

    /**
     * simple search: group and URI set; limit is set to negative value -> provide all items
     */
    @Test
    public void testSearchGroupAndUri_NegativeLimit_ProvideAll() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                -1, 100,                // limit (negative->provide all items), offset (should be ignored)
                "asc", "created");      // order, sort column

        List<Annotation> items = null, replies = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(2, items.size()); // there are two annotations in the DB that user 1 can see for document 1 - is returned, despite offset "100"

            replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
            Assert.assertNotNull(replies);
            Assert.assertEquals(1, replies.size()); // one reply

            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            Assert.assertNotNull(items);
            replies = annotService.searchRepliesForAnnotations(items, options, SECOND_USER_LOGIN);
            Assert.assertNotNull(replies);

            // there are three annotations in the DB that user 2 can see for document 1 (his own, and two of user 1), and two replies
            Assert.assertEquals(3, items.size());
            Assert.assertEquals(2, replies.size());
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        // repeat search for second document
        options.setUri(SECOND_URL);

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size()); // there is one item in the DB that user 1 can see for document 2 (from user 2)

            items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size()); // there is one item in the DB that user 2 can see for document 2 (his own)
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }

    /**
     * simple search: group and URI set; limit is set to zero value -> use default limit; keep positive offset, change negative offset
     */
    @Test
    public void testSearchGroupAndUri_ZeroLimit_DifferentOffsets() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                0, 1,                   // limit (zero->use default), offset positive (thus keep)
                "asc", "created");      // order, sort column

        List<Annotation> items = null;

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(1, items.size()); // there are two items in the DB that user 1 can see for document 1 - but only one is returned (due to offset
                                                  // "1")

            // verification that there really is the one item that was skipped before (by using offset 0)
            options.setItemLimitAndOffset(1, 0);
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertEquals(1, items.size());
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        // repeat search, but set offset negative -> default offset to be used
        options.setItemLimitAndOffset(0, -5);

        try {
            items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            Assert.assertNotNull(items);
            Assert.assertEquals(2, items.size()); // there are two annotations in the DB that user 1 can see for document 1 - is returned due to default offset
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
    }

    /**
     * test search item conversion - no separate replies
     */
    @Test
    public void testSearchNoSeparateReplies() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                false,                  // provide separate replies
                200, 0,                 // limit, offset
                "asc", "created");      // order, sort column

        JsonSearchResult result = null;

        try {
            List<Annotation> items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
            List<Annotation> replies = annotService.searchRepliesForAnnotations(items, options, SECOND_USER_LOGIN);
            result = annotService.convertToJsonSearchResult(items, replies, options);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        Assert.assertNotNull(result);
        Assert.assertFalse(result instanceof JsonSearchResultWithSeparateReplies);
        Assert.assertEquals(5, result.getTotal()); // three annotations and two replies
        Assert.assertEquals(5, result.getRows().size());

        // second user can see all annotations of first user and his own (which are private to him)
        assertJsonAnnotationContained(result.getRows(), ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        assertJsonAnnotationContained(result.getRows(), ID_PUBREPLY_FIRSTUSER_FIRSTDOC);
        assertJsonAnnotationContained(result.getRows(), ID_PUBANNOT2_FIRSTUSER_FIRSTDOC);
        assertJsonAnnotationContained(result.getRows(), ID_PRIVANNOT_SECONDUSER_FIRSTDOC);
        assertJsonAnnotationContained(result.getRows(), ID_PRIVREPLY_SECONDUSER_FIRSTDOC);
    }

    /**
     * test search item conversion - separate replies
     */
    @Test
    public void testSearchSeparateReplies() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                true,                   // provide separate replies
                200, 0,                 // limit, offset
                "asc", "created");      // order, sort column

        JsonSearchResult result = null;

        try {
            List<Annotation> items = annotService.searchAnnotations(options, FIRST_USER_LOGIN);
            List<Annotation> replies = annotService.searchRepliesForAnnotations(items, options, FIRST_USER_LOGIN);
            result = annotService.convertToJsonSearchResult(items, replies, options);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof JsonSearchResultWithSeparateReplies);

        JsonSearchResultWithSeparateReplies jsrwsr = (JsonSearchResultWithSeparateReplies) result;
        Assert.assertEquals(2, jsrwsr.getRows().size()); // two annotations...
        Assert.assertEquals(1, jsrwsr.getReplies().size()); // ... and one reply
        Assert.assertEquals(2, jsrwsr.getTotal()); // total shows number of annotations without taking replies into account

        // check that it's the first user's public annotation and reply that were found
        assertJsonAnnotationContained(jsrwsr.getRows(), ID_PUBANNOT_FIRSTUSER_FIRSTDOC);
        assertJsonAnnotationContained(jsrwsr.getReplies(), ID_PUBREPLY_FIRSTUSER_FIRSTDOC);

        // check that the 'rows' contains annotations
        jsrwsr.getRows().stream().allMatch(jsonAnnot -> !jsonAnnot.isReply());

        // check that the 'replies' contains replies
        jsrwsr.getReplies().stream().allMatch(jsonAnnot -> jsonAnnot.isReply());
    }

    /**
     * test search result conversion without any valid parameter -> errors expected
     */
    @Test
    public void testConvertSearchResultsWithoutParameters() {

        Assert.assertNull(annotService.convertToJsonSearchResult(null, null, null));
    }

    /**
     * test search result conversion without options -> considered as separate replies set to false
     */
    @Test
    public void testConvertSearchResultsWithoutOptions() {

        List<Annotation> annots = new ArrayList<Annotation>(), replies = new ArrayList<Annotation>();

        JsonSearchResult result = annotService.convertToJsonSearchResult(annots, replies, null);
        Assert.assertNull(result);
    }

    /**
     * test search result conversion without replies
     */
    @Test
    public void testConvertSearchResultsWithoutReplies() {

        List<Annotation> annots = new ArrayList<Annotation>();

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                FIRST_URL, "__world__", // URI, group
                true,                   // provide separate replies
                200, 0,                 // limit, offset
                "asc", "created");      // order, sort column

        JsonSearchResult result = annotService.convertToJsonSearchResult(annots, null, options);
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof JsonSearchResultWithSeparateReplies);

        // change options to not provide separate replies
        options.setSeparateReplies(false);
        result = annotService.convertToJsonSearchResult(annots, null, options);
        Assert.assertNotNull(result);
        Assert.assertFalse(result instanceof JsonSearchResultWithSeparateReplies);
    }

    // helper function for uncomfortable Java Date comparison
    private void assertDatesAscending(LocalDateTime older, LocalDateTime newer) {
        Assert.assertTrue(older.isBefore(newer) || older.equals(newer));
    }

    private void assertAnnotationContained(List<Annotation> items, String annotId) {
        Assert.assertTrue(items.stream().filter(ann -> ann.getId().equals(annotId)).findFirst().isPresent());
    }

    private void assertJsonAnnotationContained(List<JsonAnnotation> items, String annotId) {
        Assert.assertTrue(items.stream().filter(ann -> ann.getId().equals(annotId)).findFirst().isPresent());
    }
}
