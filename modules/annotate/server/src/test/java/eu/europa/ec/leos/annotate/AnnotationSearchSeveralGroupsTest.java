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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class AnnotationSearchSeveralGroupsTest {

    /**
     * Tests on search functionality for annotations, with users and annotations belonging to different groups
     * 
     * memberships:
     *         | user 1  |  user 2  |  user 3
     * --------|---------|----------|------------        
     * group A |   x     |          |    x
     * --------|---------|----------|------------   
     * group B |         |    x     |    x
     * 
     * annotations:
     *         |  user 1  |  user 2  |  user 3
     * --------|----------|----------|-------------
     * group A |   pub    |          |  pub reply
     * --------|----------|----------|------------   
     * group B |          |   pub    |  priv reply
     * 
     * expected visibilities:
     *         |      user 1        |    user 2      |    user 3      
     * --------|--------------------|----------------|----------------
     * group A |  his pub annot,    |                |  user 1's pub annot,
     *         | user 3's pub reply |                |    his pub reply
     * --------|--------------------|----------------|----------------
     * group B |                    |  his pub annot | user 2's pub annot,
     *         |                    |                |    his priv reply
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
    private static final String FIRST_USER_LOGIN = "firstUserLogin";
    private static final String SECOND_USER_LOGIN = "secondUserLogin";
    private static final String THIRD_USER_LOGIN = "thirdUserLogin";
    
    private static final String AUTHORITY = Authorities.EdiT;

    private static final String ID_PUBANNOT_FIRSTUSER = "id1";
    private static final String ID_PUBREPLY_THIRDUSER = "id1_r";
    private static final String ID_PUBANNOT_SECONDUSER = "id2";
    private static final String ID_PRIVREPLY_THIRDUSER = "id2_r";

    private static final String THE_URI = "http://myurl.net";
    private static final String SORT = "desc";
    private static final String SORTCOL_UPDATED = "updated";

    private Group groupA, groupB;
    private UserInformation firstUserInfo, secondUserInfo, thirdUserInfo;

    @Before
    public void cleanDatabaseBeforeTests() throws URISyntaxException {

        // create default group and two more groups
        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        groupA = new Group("A", true);
        groupB = new Group("B", true);
        groupRepos.save(Arrays.asList(groupA, groupB));

        // insert two users, assign them both to the default group, and one to each group
        final User firstUser = new User(FIRST_USER_LOGIN);
        final User secondUser = new User(SECOND_USER_LOGIN);
        final User thirdUser = new User(THIRD_USER_LOGIN);
        userRepos.save(Arrays.asList(firstUser, secondUser, thirdUser));

        userGroupRepos.save(new UserGroup(firstUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(firstUser.getId(), groupA.getId())); // first user to group A
        userGroupRepos.save(new UserGroup(secondUser.getId(), groupB.getId())); // second user to group B
        userGroupRepos.save(new UserGroup(thirdUser.getId(), groupA.getId())); // third user to both groups A and B
        userGroupRepos.save(new UserGroup(thirdUser.getId(), groupB.getId())); // third user to both groups A and B

        firstUserInfo = new UserInformation(new Token(firstUser, AUTHORITY, "@cc1", LocalDateTime.now().plusMinutes(5), "refr1", LocalDateTime.now().plusMinutes(5)));
        secondUserInfo = new UserInformation(new Token(secondUser, AUTHORITY, "@cc2", LocalDateTime.now().plusMinutes(5), "refr2", LocalDateTime.now().plusMinutes(5)));
        thirdUserInfo = new UserInformation(new Token(thirdUser, AUTHORITY, "@cc3", LocalDateTime.now().plusMinutes(5), "refr3", LocalDateTime.now().plusMinutes(5)));
        
        // insert document
        final Document theDoc = new Document(new URI(THE_URI), "first document's title");
        documentRepos.save(theDoc);

        // save metadata for the document
        final Metadata worldMeta = new Metadata(theDoc, defaultGroup, AUTHORITY);
        final Metadata groupAMeta = new Metadata(theDoc, groupA, AUTHORITY);
        final Metadata groupBMeta = new Metadata(theDoc, groupB, AUTHORITY);
        metadataRepos.save(Arrays.asList(worldMeta, groupAMeta, groupBMeta));

        // dummy selector
        final String dummySelector = "[{\"selector\":null,\"source\":\"" + theDoc.getUri() + "\"}]";

        // insert annotations - one public annotation with a public reply for each user in his group

        // public annotation of first user in group A
        // visible for first user, third user
        final Annotation annotFirstUser = new Annotation();
        annotFirstUser.setId(ID_PUBANNOT_FIRSTUSER);
        annotFirstUser.setCreated(LocalDateTime.of(2012, 12, 21, 12, 0));
        annotFirstUser.setUpdated(LocalDateTime.of(2012, 12, 22, 12, 0));
        annotFirstUser.setMetadata(groupAMeta);
        annotFirstUser.setReferences("");
        annotFirstUser.setShared(true);
        annotFirstUser.setTargetSelectors(dummySelector);
        annotFirstUser.setText("first annotation's content");
        annotFirstUser.setUser(firstUser);
        annotRepos.save(annotFirstUser);

        // public reply of third user to first user's public annotation
        // visible for first user, third user
        final Annotation replyToFirstAnnot = new Annotation();
        replyToFirstAnnot.setId(ID_PUBREPLY_THIRDUSER);
        replyToFirstAnnot.setCreated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToFirstAnnot.setUpdated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToFirstAnnot.setMetadata(groupAMeta);
        replyToFirstAnnot.setReferences(ID_PUBANNOT_FIRSTUSER);
        replyToFirstAnnot.setShared(true);
        replyToFirstAnnot.setTargetSelectors(dummySelector);
        replyToFirstAnnot.setText("second annotation to first doc");
        replyToFirstAnnot.setUser(thirdUser);
        annotRepos.save(replyToFirstAnnot);

        // public annotation of second user in group B
        // visible for second user, third user
        final Annotation annotSecondUser = new Annotation();
        annotSecondUser.setId(ID_PUBANNOT_SECONDUSER);
        annotSecondUser.setCreated(LocalDateTime.of(2012, 12, 21, 12, 0));
        annotSecondUser.setUpdated(LocalDateTime.of(2012, 12, 22, 12, 0));
        annotSecondUser.setMetadata(groupBMeta);
        annotSecondUser.setReferences("");
        annotSecondUser.setShared(true);
        annotSecondUser.setTargetSelectors(dummySelector);
        annotSecondUser.setText("first annotation's content");
        annotSecondUser.setUser(secondUser);
        annotRepos.save(annotSecondUser);

        // private reply of third user to second user's public annotation
        // visible for third user only
        final Annotation replyToSecondAnnot = new Annotation();
        replyToSecondAnnot.setId(ID_PRIVREPLY_THIRDUSER);
        replyToSecondAnnot.setCreated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToSecondAnnot.setUpdated(LocalDateTime.of(2012, 12, 23, 12, 0));
        replyToSecondAnnot.setMetadata(groupAMeta);
        replyToSecondAnnot.setReferences(ID_PUBANNOT_SECONDUSER);
        replyToSecondAnnot.setShared(false);
        replyToSecondAnnot.setTargetSelectors(dummySelector);
        replyToSecondAnnot.setText("second annotation to first doc");
        replyToSecondAnnot.setUser(thirdUser);
        annotRepos.save(replyToSecondAnnot);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // search from first user's perspective in group A - should see his annotation and public reply of third user
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchFirstUserGroupA() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                THE_URI, groupA.getName(), // URI, group
                true,                      // provide separate replies
                200, 0,                    // limit, offset
                SORT, SORTCOL_UPDATED);    // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        try {
            annots = annotService.searchAnnotations(options, firstUserInfo);
            replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received during search for first user in group A: " + e);
        }
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // only his public annotation
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_FIRSTUSER);
        Assert.assertEquals(1, replies.size());
        assertAnnotationContained(replies, ID_PUBREPLY_THIRDUSER);
    }

    // search from first user's perspective in group B - should see nothing (since he is not member of the group)
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchFirstUserGroupB() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                THE_URI, groupB.getName(), // URI, group
                true,                      // provide separate replies
                200, 0,                    // limit, offset
                SORT, SORTCOL_UPDATED);    // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        try {
            annots = annotService.searchAnnotations(options, firstUserInfo);
            replies = annotService.searchRepliesForAnnotations(annots, options, firstUserInfo);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received during search for first user in group B: " + e);
        }
        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size());
        Assert.assertEquals(0, replies.size());
    }

    // search from second user's perspective in group A - should see nothing (since he is not member of the group)
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchSecondUserGroupA() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                THE_URI, groupA.getName(), // URI, group
                true,                      // provide separate replies
                200, 0,                    // limit, offset
                SORT, SORTCOL_UPDATED);    // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        try {
            annots = annotService.searchAnnotations(options, secondUserInfo);
            replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received during search for second user in group A: " + e);
        }

        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size());
        Assert.assertEquals(0, replies.size());
    }

    // search from second user's perspective in group B - should see his annotation only (but not the private reply of third user)
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchSecondUserGroupB() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                THE_URI, groupB.getName(), // URI, group
                true,                      // provide separate replies
                200, 0,                    // limit, offset
                SORT, SORTCOL_UPDATED);    // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        try {
            annots = annotService.searchAnnotations(options, secondUserInfo);
            replies = annotService.searchRepliesForAnnotations(annots, options, secondUserInfo);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // only his public annotation
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_SECONDUSER);
        Assert.assertEquals(0, replies.size());
    }

    // search from third user's perspective in group A - should see first user's annotation and his public reply of third user
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchThirdUserGroupA() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                THE_URI, groupA.getName(), // URI, group
                true,                      // provide separate replies
                200, 0,                    // limit, offset
                SORT, SORTCOL_UPDATED);    // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        try {
            annots = annotService.searchAnnotations(options, thirdUserInfo);
            replies = annotService.searchRepliesForAnnotations(annots, options, thirdUserInfo);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // first user's public annotation
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_FIRSTUSER);
        Assert.assertEquals(1, replies.size()); // his public reply
        assertAnnotationContained(replies, ID_PUBREPLY_THIRDUSER);
    }

    // search from third user's perspective in group B - should see second user's annotation and his private reply of third user
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSearchThirdUserGroupB() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                THE_URI, groupB.getName(), // URI, group
                true,                      // provide separate replies
                200, 0,                    // limit, offset
                SORT, SORTCOL_UPDATED);    // order, sort column

        AnnotationSearchResult annots = null;
        List<Annotation> replies = null;

        try {
            annots = annotService.searchAnnotations(options, thirdUserInfo);
            replies = annotService.searchRepliesForAnnotations(annots, options, thirdUserInfo);
        } catch (Exception e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(annots);
        Assert.assertEquals(1, annots.size()); // first user's public annotation
        assertAnnotationContained(annots.getItems(), ID_PUBANNOT_SECONDUSER);
        Assert.assertEquals(1, replies.size()); // his private reply
        assertAnnotationContained(replies, ID_PRIVREPLY_THIRDUSER);
    }

    // helper function
    private void assertAnnotationContained(final List<Annotation> items, final String annotId) {
        Assert.assertTrue(items.stream().anyMatch(ann -> ann.getId().equals(annotId)));
    }
}
