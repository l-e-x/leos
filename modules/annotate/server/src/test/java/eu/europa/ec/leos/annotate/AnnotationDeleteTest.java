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
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteSentAnnotationException;
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

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class AnnotationDeleteTest {

    /**
     * This class contains tests for deleting annotations
     * this also covers special cases like deleting (intermediate) replies and cleaning no longer required documents from the database
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private AnnotationConversionService conversionService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserDetailsCache userDetailsCache;

    // -------------------------------------
    // Help variables
    // -------------------------------------
    private final static String LOGIN = "demo";
    private final static String HYPO_PREFIX = "acct:user@";
    private User theUser;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        userDetailsCache.clear();
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * an annotation without replies is deleted
     * -> this also cleans up the document referred to and the tags since both become orphaned by the removal of the annotation
     */
    @Test
    public void testDeleteSimpleAnnotation() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa";
        final String authority = "europa";

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        userDetailsCache.cache(LOGIN, new UserDetails(LOGIN, theUser.getId(), "use", "r", Arrays.asList(new UserEntity("1", "EMPL", "EMPL")),
                "use@r.com", null));
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // remove it again
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);

        // verify that the tags, document and annotation remain in DB, but annotation is flagged as being deleted
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.DELETED, theUser.getId());

        // check that the annotation status would be reported correctly in JSON format
        final Annotation annot = annotRepos.findById(jsAnnot.getId());
        final JsonAnnotation jsConverted = conversionService.convertToJsonAnnotation(annot, userInfo);
        Assert.assertNotNull(jsConverted);
        Assert.assertEquals(AnnotationStatus.DELETED, jsConverted.getStatus().getStatus());
        Assert.assertNotNull(jsConverted.getStatus().getUpdated());
        Assert.assertEquals("EMPL", jsConverted.getStatus().getUpdated_by()); // corresponds to entity of UserInformation instance
        Assert.assertEquals(userInfo.getAsHypothesisAccount(), jsConverted.getStatus().getUser_info()); // corresponds to user of UserInformation instance
        Assert.assertFalse(jsConverted.getStatus().isSentDeleted()); // this flag does not apply to pure deletion
    }

    /**
     * an annotation is deleted, but deletion request is lacking the annotation ID
     * -> should throw exception
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testDeleteAnnotationWithoutId() throws Exception {

        // remove an annotation without specifying its ID
        annotService.deleteAnnotationById("", new UserInformation(LOGIN, Authorities.EdiT));
    }

    /**
     * an annotation without replies is tried to be deleted by another user 
     * -> accepted (previously refused, accepted since ANOT-56)
     */
    @Test
    public void testDeleteSimpleAnnotationOfOtherUser() throws Exception {

        final String authority = "someauth";
        final String hypothesisUserAccount = HYPO_PREFIX + authority;
        final String otherLogin = "demo2";
        final User otherUser = new User(otherLogin);
        userRepos.save(otherUser);

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        final UserInformation otherUserInfo = new UserInformation(otherLogin, authority);

        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // other user tries to remove it -> ok
        annotService.deleteAnnotationById(jsAnnot.getId(), otherUserInfo);

        // verify that the tags and the document have not been removed, but the annotation is flagged as being deleted
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.DELETED, otherUser.getId());
    }

    /**
     * an annotation having response status SENT is tried to be deleted (by same ISC user) 
     * -> accepted, annotation flagged as "sent deleted"
     */
    @Test
    public void testSentDeleteSentAnnotationFromIsc() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // set metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // try to remove it -> passes, sets the new "sentDeleted" flag
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);

        // verify: annotation has the "sentDeleted" flag
        final Annotation readAnnot = annotRepos.findById(jsAnnot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertTrue(readAnnot.isSentDeleted());
        Assert.assertEquals(AnnotationStatus.NORMAL, readAnnot.getStatus()); // not changed to DELETED status!
    }

    /**
     * an annotation having response status SENT is deleted twice (by same ISC user) 
     * -> accepted, annotation flagged as "sent deleted" (and not just toggled each time)
     */
    @Test
    public void testSentDeleteSentAnnotationFromIscTwiceDoesNotToggleStatus() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        annotService.createAnnotation(jsAnnot, userInfo);

        // set metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // try to remove it -> passes, sets the new "sentDeleted" flag
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);

        // verify: annotation has the "sentDeleted" flag
        Annotation readAnnot = annotRepos.findById(jsAnnot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertTrue(readAnnot.isSentDeleted());

        // try to remove it once more -> passes, "sentDeleted" flag remains set (is not toggled)
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);
        
        // verify the flag is still set
        readAnnot = annotRepos.findById(jsAnnot.getId());
        Assert.assertTrue(readAnnot.isSentDeleted());
    }

    /**
     * an annotation having response status SENT is tried to be deleted (by ISC user of the same group) 
     * -> accepted, annotation flagged as "sent deleted"
     */
    @Test
    public void testSentDeleteSentAnnotationFromIscSameGroup() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;
        final String username = "jane";
        final String groupname = "AGRI";

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setGroup(groupname);

        final Group groupAgri = new Group(groupname, true);
        groupRepos.save(groupAgri);

        final UserInformation userInfo = new UserInformation(username, authority);
        final User userJane = new User(username);
        userRepos.save(userJane);
        userInfo.setUser(userJane);

        userGroupRepos.save(new UserGroup(userJane.getId(), groupAgri.getId()));

        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // set metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // try to remove it -> passes, sets the new "sentDeleted" flag
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);

        // verify: annotation has the "sentDeleted" flag
        final Annotation readAnnot = annotRepos.findById(jsAnnot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertTrue(readAnnot.isSentDeleted());
        Assert.assertEquals(AnnotationStatus.NORMAL, readAnnot.getStatus()); // not changed to DELETED status!
    }

    /**
     * an annotation having response status SENT is tried to be deleted (by ISC user of the different group) 
     * -> accepted, annotation flagged as "sent deleted"
     */
    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void testSentDeleteSentAnnotationFromIscDifferentGroup() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;
        final String username = "jodi";
        final String groupname = "BGRI";

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setGroup(groupname);

        final Group groupBgri = new Group(groupname, true);
        groupRepos.save(groupBgri);

        final UserInformation userInfo = new UserInformation(username, authority);
        final User userJane = new User(username);
        userRepos.save(userJane);
        userInfo.setUser(userJane);

        final UserGroup bgriMembership = userGroupRepos.save(new UserGroup(userJane.getId(), groupBgri.getId()));

        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());

        // set metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // remove the user from the group
        userGroupRepos.delete(bgriMembership);

        // try to remove it -> should throw exception
        try {
            annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);
            Assert.fail("Expected CannotDeleteSentAnnotationException not received!");
        } catch (CannotDeleteSentAnnotationException cdsae) {
            // OK
        }

        // verify: annotation does not have the "sentDeleted" flag
        final Annotation readAnnot = annotRepos.findById(jsAnnot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertFalse(readAnnot.isSentDeleted());
        Assert.assertEquals(AnnotationStatus.NORMAL, readAnnot.getStatus()); // not changed to DELETED status!
    }

    /**
     * an annotation having response status SENT is tried to be deleted (by another LEOS user) 
     * -> feasible
     */
    @Test
    public void testDeleteSentAnnotationFromEdit() throws Exception {

        final String hypothesisUserAccount = HYPO_PREFIX + Authorities.ISC;

        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, Authorities.ISC);
        userInfo.setUser(theUser);

        // annotation is created as an ISC user
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // set metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // try to remove it as an EdiT user -> should be possible
        final UserInformation userInfoEdit = new UserInformation(LOGIN, Authorities.EdiT);
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfoEdit);

        final Annotation readAnnot = annotRepos.findById(jsAnnot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertFalse(readAnnot.isSentDeleted());
        Assert.assertEquals(AnnotationStatus.DELETED, readAnnot.getStatus()); // was really DELETED (status change)
    }

    /**
     * a reply of another user is tried to be deleted
     * -> accepted, but parent annotation shall remain
     */
    @Test
    public void testDeleteSimpleReplyOfOtherUser() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;
        final String otherLogin = "demo2";
        final User otherUser = new User(otherLogin);
        userRepos.save(otherUser);

        // retrieve out test annotation: has two tags
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        final UserInformation otherUserInfo = new UserInformation(otherLogin, authority);

        annotService.createAnnotation(jsAnnot, userInfo);
        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(LOGIN, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, userInfo);

        Assert.assertEquals(2, annotRepos.count());

        // other user tries to remove the reply -> ok
        annotService.deleteAnnotationById(jsReply.getId(), otherUserInfo);

        // verify that the parent annotation and the document remain
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertNotNull(annotRepos.findById(jsAnnot.getId()));

        // "deleted" annotation must be flagged, its parent must not be flagged
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.NORMAL, null);
        TestHelper.assertHasStatus(annotRepos, jsReply.getId(), AnnotationStatus.DELETED, otherUser.getId());
    }

    /**
     * a reply with response status SENT is tried to be deleted as an ISC user
     * -> should be accepted and reply be flagged as "sent deleted"
     */
    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void testSentDeleteSentReplyFromIsc() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        annotService.createAnnotation(jsAnnot, userInfo);
        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(LOGIN, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, userInfo);

        Assert.assertEquals(2, annotRepos.count());

        // now change response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsReply.getId()).getMetadata();
        savedMetadata.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // try to remove the reply -> will be flagged
        annotService.deleteAnnotationById(jsReply.getId(), userInfo);

        // verify that the parent annotation and the reply remain
        Assert.assertEquals(2, annotRepos.count());

        // reply must still be flagged as "NORMAL", it must not be flagged as DELETED - but as "sent deleted"
        TestHelper.assertHasStatus(annotRepos, jsReply.getId(), AnnotationStatus.NORMAL, null);
        Assert.assertTrue(annotRepos.findById(jsReply.getId()).isSentDeleted());
    }

    /**
     * a reply with response status SENT is tried to be deleted as an EdiT user
     * -> should be possible
     */
    @Test
    public void testDeleteSentReplyFromLeos() throws Exception {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        annotService.createAnnotation(jsAnnot, userInfo);
        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(LOGIN, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, userInfo);

        Assert.assertEquals(2, annotRepos.count());

        // now change response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsReply.getId()).getMetadata();
        savedMetadata.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // try to remove the reply as a LEOS/EdiT user -> allowed
        userInfo.setAuthority(Authorities.EdiT);
        annotService.deleteAnnotationById(jsReply.getId(), userInfo);

        // verify that the parent annotation and the reply remain
        Assert.assertEquals(2, annotRepos.count());

        // reply must be flagged as "DELETED" now
        TestHelper.assertHasStatus(annotRepos, jsReply.getId(), AnnotationStatus.DELETED, userInfo.getUser().getId());
    }

    /**
     *  there are two annotations to the same document; both shared some tags
     *  one of the annotations is deleted
     *  -> the other one remains, the document and the shared tags also 
     */
    @Test
    public void testDeleteOneAnnotationOfTwoForSameDocument() throws Exception {

        final String authority = "EU";
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out first test annotation: has two tags; save it
        final JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        annotService.createAnnotation(jsAnnotFirst, userInfo);

        // retrieve a second test annotation with three tags; save it
        final JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnotSecond.getTags().add("a_third_tag");
        annotService.createAnnotation(jsAnnotSecond, userInfo);

        final String annotId1 = jsAnnotFirst.getId();
        final String annotId2 = jsAnnotSecond.getId();

        // verify after initial storage: document is shared, two of the three tags also
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(5, tagRepos.count()); // five in total...
        Assert.assertEquals(2, tagRepos.countByAnnotationId(annotId1)); // ... two of them for first annotation, ...
        Assert.assertEquals(3, tagRepos.countByAnnotationId(annotId2)); // ... three of them for the second annotation

        // now we delete the first annotation
        annotService.deleteAnnotationById(annotId1, userInfo);

        // verify
        Assert.assertEquals(2, annotRepos.count()); // both remain in database
        Assert.assertEquals(1, documentRepos.count()); // document must remain as it is still referred to
        Assert.assertEquals(5, tagRepos.count()); // tags remain (must not have been removed since annotation logically remains in DB)
        Assert.assertEquals(3, tagRepos.countByAnnotationId(annotId2)); // and still three remaining tags refer to the second annotation
        Assert.assertEquals(2, tagRepos.countByAnnotationId(annotId1)); // and still two remaining tags refer to the first ("deleted") annotation

        // check that the "deleted" annotation was flagged accordingly
        TestHelper.assertHasStatus(annotRepos, annotId1, AnnotationStatus.DELETED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, annotId2, AnnotationStatus.NORMAL, null);
    }

    /**
     *  an annotation with two immediate replies and a reply to a reply is created
     *  the annotation is deleted
     *  -> all child replies are also removed (i.e. all flagged as being DELETED)
     */
    @Test
    public void testEntireAnnotationThreadRemoved() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotation: has two tags; save it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, userInfo);
        Assert.assertEquals(2, annotRepos.count());

        // add a reply to the reply
        final JsonAnnotation jsAnnotFirstReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotFirstReply.getId()));
        annotService.createAnnotation(jsAnnotFirstReplyReply, userInfo);
        Assert.assertEquals(3, annotRepos.count());

        // create a second reply to the root annotation
        final JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsAnnotSecondReply, userInfo);
        Assert.assertEquals(4, annotRepos.count());

        // still only one document is present
        Assert.assertEquals(1, documentRepos.count());

        // now remove the root
        annotService.deleteAnnotationById(jsAnnot.getId(), userInfo);

        // verify:
        // - all annotations and have been "removed" (=flagged as being DELETED)
        // - the referenced (now orphaned) document still remains
        Assert.assertEquals(4, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        assertFlag(Arrays.asList(jsAnnot, jsAnnotFirstReply, jsAnnotFirstReplyReply, jsAnnotSecondReply), AnnotationStatus.DELETED, theUser.getId());
    }

    /**
     *  an annotation with two immediate replies and a reply to a reply is created
     *  the immediate reply not having sub replies is deleted
     *  -> only the immediate reply is removed, the annotation as well as the other reply with its sub reply are retained
     */
    @Test
    public void testDeleteReplyWithoutSubreply() throws Exception {

        final String auth = Authorities.ISC;
        final String hypothesisUserAccount = HYPO_PREFIX + auth;

        // retrieve out test annotation: has two tags; save it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, auth);
        userInfo.setUser(theUser);

        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, userInfo);
        Assert.assertEquals(2, annotRepos.count());

        // add a reply to the reply
        final JsonAnnotation jsAnnotFirstReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotFirstReply.getId()));
        annotService.createAnnotation(jsAnnotFirstReplyReply, userInfo);
        Assert.assertEquals(3, annotRepos.count());

        // create a second reply to the root annotation
        final JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsAnnotSecondReply, userInfo);
        Assert.assertEquals(4, annotRepos.count());

        // still only one document is present
        Assert.assertEquals(1, documentRepos.count());

        // now remove the reply not having sub replies
        annotService.deleteAnnotationById(jsAnnotSecondReply.getId(), userInfo);

        // verify: all four annotations are remaining, but one is flagged as being deleted; the referenced document also remains
        Assert.assertEquals(4, annotRepos.count());
        Assert.assertEquals(3, annotRepos.countByRootAnnotationIdNotNull());
        Assert.assertEquals(1, documentRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnotSecondReply.getId(), AnnotationStatus.DELETED, theUser.getId());

        // other annotations are not flagged
        assertFlag(Arrays.asList(jsAnnot, jsAnnotFirstReply, jsAnnotFirstReplyReply), AnnotationStatus.NORMAL, null);
    }

    /**
     *  an annotation with two immediate replies and a reply to a reply is created
     *  the reply having another sub reply is deleted
     *  -> only the intermediate reply is removed, the annotation as well as the other reply and the sub reply (of the deleted one) are retained
     *     (i.e. no deletion of the whole reply sub tree is executed)
     */
    @Test
    public void testDeleteReplyHavingSubreply() throws Exception {

        final String auth = "europa";
        final String hypothesisUserAccount = HYPO_PREFIX + auth;

        final UserInformation userInfo = new UserInformation(LOGIN, auth);
        userInfo.setUser(theUser);

        // retrieve out test annotation: has two tags; save it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, userInfo);
        Assert.assertEquals(2, annotRepos.count());

        // add a reply to the reply
        final JsonAnnotation jsAnnotFirstReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotFirstReply.getId()));
        annotService.createAnnotation(jsAnnotFirstReplyReply, userInfo);
        Assert.assertEquals(3, annotRepos.count());

        // create a second reply to the root annotation
        final JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsAnnotSecondReply, userInfo);
        Assert.assertEquals(4, annotRepos.count());

        // still only one document is present
        Assert.assertEquals(1, documentRepos.count());

        // now remove the reply having a sub reply
        annotService.deleteAnnotationById(jsAnnotFirstReply.getId(), userInfo);

        // verify: all four annotations are remaining; the referenced document also remains
        Assert.assertEquals(4, annotRepos.count());
        Assert.assertEquals(3, annotRepos.countByRootAnnotationIdNotNull());
        Assert.assertEquals(1, documentRepos.count());

        // verify: one annotation is flagged for being deleted, others are not
        TestHelper.assertHasStatus(annotRepos, jsAnnotFirstReply.getId(), AnnotationStatus.DELETED, theUser.getId());

        // verify in particular that the annotation, the sub reply of first reply, and the second reply are the items remaining
        assertFlag(Arrays.asList(jsAnnot, jsAnnotFirstReplyReply, jsAnnotSecondReply), AnnotationStatus.NORMAL, null);
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnot.getId())); // annotation
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnotFirstReplyReply.getId())); // sub reply of first reply
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnotSecondReply.getId())); // second reply
    }

    private void assertFlag(final List<JsonAnnotation> annots, final AnnotationStatus status, final Long userId) {
        annots.forEach(annot -> TestHelper.assertHasStatus(annotRepos, annot.getId(), status, userId));
    }

}
