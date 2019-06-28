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
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateSentAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class AnnotationUpdateTest {

    /**
     * This test class contains tests for updating annotations 
     */

    private User user;
    private static final String ACCOUNT_PREFIX = "acct:user@";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private DocumentRepository documentRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);
        user = new User("demo");
        userRepos.save(user);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * simple update of annotation (text, updated, shared) - ID should remain same; tags not considered here
     * user requesting update is the creator of the annotation -> valid
     */
    @Test
    public void testSimpleAnnotationUpdate()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String authority = "anyauthority";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, userInfo);

        final LocalDateTime savedUpdated = annot.getUpdated();
        final LocalDateTime savedCreated = annot.getCreated();
        final String annotId = annot.getId();

        // update the annotation properties and launch update via service
        annot.setText("new text");
        annot.getPermissions().setRead(Arrays.asList(hypothesisUserAccount)); // change from public to private

        // update via service
        final JsonAnnotation updatedAnnot = annotService.updateAnnotation(annot.getId(), annot, userInfo);
        Assert.assertEquals(annotId, updatedAnnot.getId()); // id remains same

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals("new text", readAnnot.getText());
        Assert.assertFalse(readAnnot.isShared());
        Assert.assertEquals(savedCreated, readAnnot.getCreated());
        Assert.assertTrue(readAnnot.getUpdated().compareTo(savedUpdated) >= 0); // equal or after, but not before!
    }

    /**
     * simple update of annotation that is marked as DELETED
     * -> should not be possible
     */
    @Test(expected = CannotUpdateAnnotationException.class)
    public void testSimpleAnnotationUpdate_DeletedAnnotation()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String authority = "myauthority";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // mark the annotation as being deleted
        final Annotation readAnnot = annotRepos.findById(annotId);
        readAnnot.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(readAnnot);

        // update the annotation properties and launch update via service
        annot.setText("new text");
        annotService.updateAnnotation(annot.getId(), annot, userInfo); // should throw exception as annotation is "deleted" already
    }

    /**
     * simple update of annotation, but from a different user than the creator
     * -> not valid
     */
    @Test
    public void testSimpleAnnotationUpdateForbidden()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String otherLogin = "demo2";

        // create second user
        final User otherUser = new User(otherLogin);
        userRepos.save(otherUser);

        final UserInformation firstUserInfo = new UserInformation(user, authority);
        final UserInformation otherUserInfo = new UserInformation(otherUser, authority);

        // create an annotation from first user
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, firstUserInfo);

        final LocalDateTime savedUpdated = annot.getUpdated();
        final LocalDateTime savedCreated = annot.getCreated();
        final String savedText = annot.getText();
        final String annotId = annot.getId();

        // update the annotation properties and launch update via service
        annot.setText("new text");

        // update via service - as second user
        try {
            annotService.updateAnnotation(annot.getId(), annot, otherUserInfo);
            Assert.fail("Expected exception about missing permissions not received");
        } catch (MissingPermissionException mpe) {
            // OK
        }

        // read annotation from database and verify that it was not touched
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(savedText, readAnnot.getText());
        Assert.assertTrue(readAnnot.isShared());
        Assert.assertEquals(savedCreated, readAnnot.getCreated());
        Assert.assertEquals(savedUpdated, readAnnot.getUpdated());
    }

    /**
     * trying to update a non-existing annotation -> exception
     */
    @Test(expected = CannotUpdateAnnotationException.class)
    public void testUpdateNonExistingAnnotation() throws Exception {

        final String authority = "someauth";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setId("myid");

        // should fail as the annotation with this ID is unknown and thus cannot be updated
        annotService.updateAnnotation(annot.getId(), annot, new UserInformation("login", authority));
        Assert.fail("Expected exception not thrown!");
    }

    /**
     * trying to update an annotation without any given user information should fail
     */
    @Test(expected = CannotUpdateAnnotationException.class)
    public void testUpdateWithoutUserInfo() throws Exception {

        // should fail as UserInformation is undefined
        annotService.updateAnnotation("someId", new JsonAnnotation(), null);
    }

    /**
     * trying to update an existing annotation without specifying annotation ID-> exception
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testUpdateAnnotationWithoutAnnotationId() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String login = "demo";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot = annotService.createAnnotation(annot, new UserInformation(user, authority));

        // should fail as the annotation ID is missing
        annotService.updateAnnotation("", annot, new UserInformation(login, authority));
        Assert.fail("Expected exception not thrown!");
    }

    /**
     * update an annotation not having tags: add tags
     * -> verify that the tags are properly saved
     */
    @Test
    public void testUpdateAnnotationWithNewTags()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String authority = "AuTh";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String firstTagName = "mynewtag";
        final String secondTagName = "mysecondtag";

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTags(null);
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // update annotation
        annot.setTags(Arrays.asList(firstTagName, secondTagName));
        annotService.updateAnnotation(annotId, annot, userInfo);

        // verify that the tags were saved and are assigned to the annotation
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot.getTags());
        Assert.assertEquals(2, readAnnot.getTags().size());

        // check via tag repository
        final List<Tag> readTags = (List<Tag>) tagRepos.findAll();
        Assert.assertEquals(2, readTags.size());

        // check that the tags have correct associations
        checkTagInListAndAssociatedToAnnotation(readTags, firstTagName, annotId);
        checkTagInListAndAssociatedToAnnotation(readTags, secondTagName, annotId);
    }

    /**
     * update an annotation having tags: remove tags
     * -> verify that the tags are properly removed from repository
     * (this test case was created as by pure hibernate could not be used for removal)
     */
    @Test
    public void testUpdateAnnotationByRemovingTags()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String firstTagName = "mynewtag";
        final String secondTagName = "mysecondtag";

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTags(Arrays.asList(firstTagName, secondTagName));
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // update annotation
        annot.setTags(new ArrayList<String>());
        annotService.updateAnnotation(annotId, annot, userInfo);

        // verify that the tags were removed
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot.getTags());
        Assert.assertEquals(0, readAnnot.getTags().size());

        // check via tag repository
        final List<Tag> readTags = (List<Tag>) tagRepos.findAll();
        Assert.assertEquals(0, readTags.size());
    }

    /**
     * update an annotation having tags: add and remove tags
     * -> verify that the tags are properly saved/removed
     * (this test case was created as by pure hibernate could not be used for removal)
     */
    @Test
    public void testUpdateAnnotationWithDifferentTags()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String authority = "domain";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String tagNameRemains = "mynewtag";
        final String tagNameRemoved = "mysecondtag";
        final String tagNameAdded = "mythirdtag";

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTags(Arrays.asList(tagNameRemains, tagNameRemoved));
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // update annotation
        annot.setTags(Arrays.asList(tagNameRemains, tagNameAdded)); // tag tagNameRemoved is replaced by tagNameAdded
        annotService.updateAnnotation(annotId, annot, userInfo);

        // verify that the tags were saved and are assigned to the annotation
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot.getTags());
        Assert.assertEquals(2, readAnnot.getTags().size());
        checkTagInListAndAssociatedToAnnotation(readAnnot.getTags(), tagNameRemains, annotId);
        checkTagInListAndAssociatedToAnnotation(readAnnot.getTags(), tagNameAdded, annotId);

        // check via tag repository
        final List<Tag> readTags = (List<Tag>) tagRepos.findAll();
        Assert.assertEquals(2, readTags.size());

        // check that the tags have correct associations
        checkTagInListAndAssociatedToAnnotation(readTags, tagNameRemains, annotId);
        checkTagInListAndAssociatedToAnnotation(readTags, tagNameAdded, annotId);
    }

    /**
     * update an annotation by changing its group
     * the metadata for the new group/document combination is existing already
     */
    @Test
    public void testUpdateAnnotationGroupWithNewMetadataExistingAlready() throws Exception {

        final String authority = "theauthority";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // save the new target group
        final Group newGroup = new Group("newgroup", true);
        groupRepos.save(newGroup);

        final Document doc = ((List<Document>) documentRepos.findAll()).get(0);
        final Metadata newGroupMeta = new Metadata(doc, newGroup, authority);
        metadataRepos.save(newGroupMeta);

        // update the annotation's group
        annot.setGroup(newGroup.getName());

        // update via service
        final JsonAnnotation updatedAnnot = annotService.updateAnnotation(annot.getId(), annot, userInfo);
        Assert.assertEquals(annotId, updatedAnnot.getId()); // id remains same

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(newGroup, readAnnot.getGroup());

        // note: comparing Metadata objects directly fails because documentId and groupId are not set yet
        Assert.assertEquals(newGroupMeta.getId(), readAnnot.getMetadata().getId());
        Assert.assertEquals(newGroup, readAnnot.getMetadata().getGroup());

        // check that old metadata entry is removed since it is no longer referenced
        Assert.assertEquals(1, metadataRepos.count());
    }

    /**
     * update an annotation by changing its group
     * the metadata for the new group/document combination is not yet existing
     */
    @Test
    public void testUpdateAnnotationGroupWithNewMetadataNotExisting() throws Exception {

        final String authority = "theauthority";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, userInfo);
        Assert.assertEquals(1, metadataRepos.count()); // only one metadata existing so far

        final String annotId = annot.getId();

        // save the new target group
        final Group newGroup = new Group("newgroup", true);
        groupRepos.save(newGroup);

        // update the annotation's group - note that we did not yet save new Metadata for this group/document combination
        annot.setGroup(newGroup.getName());

        // update via service
        final JsonAnnotation updatedAnnot = annotService.updateAnnotation(annot.getId(), annot, userInfo);
        Assert.assertEquals(annotId, updatedAnnot.getId()); // id remains same

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annotId);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(newGroup, readAnnot.getGroup());

        Assert.assertEquals(newGroup, readAnnot.getMetadata().getGroup());
        Assert.assertEquals(1, metadataRepos.count()); // new metadata set was added, but old one was removed

        // read new metadata set from DB and check that it's existing
        final List<Metadata> readMetas = metadataRepos.findByDocumentAndGroupAndSystemId(
                documentRepos.findByUri(annot.getUri().toString()), newGroup, authority);
        Assert.assertNotNull(readMetas);
        Assert.assertEquals(1, readMetas.size());
    }

    /**
     * test that two metadata sets exist which are in use; then one annotation is reassigned from one of them to the other;
     * but still, the metadata set is still in use and may not be deleted
     */
    @Test
    public void testUpdateAnnotationGroupWithoutDeletingMetadata() throws Exception {

        // initial situation:
        // - two annotations ANN1 and ANN2 assigned to metadata set M1
        // - two annotations ANN3 and ANN4 assigned to metadata set M2
        // then ANN3 is assigned to M1 -> ANN4 is still assigned to M2, thus M2 should not be deleted from the database
        final String authority = "theauthority";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String grp1 = "group1";
        final String grp2 = "group2";

        // prepare

        // create two groups to which the annotations will be assigned (and thus two metadata sets be created)
        groupRepos.save(new Group(grp1, true));
        groupRepos.save(new Group(grp2, true));

        final UserInformation userInfo = new UserInformation(user, authority);

        // create two annotations assigned to M1
        final JsonAnnotation annot1 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot1.setGroup(grp1);
        annotService.createAnnotation(annot1, userInfo);
        Assert.assertEquals(1, metadataRepos.count());
        final Metadata meta1 = ((List<Metadata>) metadataRepos.findAll()).stream().filter(meta -> meta.getGroup().getName().equals(grp1)).findFirst().get();
        Assert.assertNotNull(meta1);

        final JsonAnnotation annot2 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot2.setGroup(grp1);
        annotService.createAnnotation(annot2, userInfo);
        Assert.assertEquals(1, metadataRepos.count()); // assigned to same metadata

        // create two annotations assigned to M3
        final JsonAnnotation annot3 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot3.setGroup(grp2);
        annotService.createAnnotation(annot3, userInfo);
        Assert.assertEquals(2, metadataRepos.count()); // new metadata must have been created
        final Metadata meta2 = ((List<Metadata>) metadataRepos.findAll()).stream().filter(meta -> meta.getGroup().getName().equals(grp2)).findFirst().get();
        Assert.assertNotNull(meta2);

        final JsonAnnotation annot4 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot4.setGroup(grp2);
        annotService.createAnnotation(annot4, userInfo);
        Assert.assertEquals(2, metadataRepos.count()); // assigned to same metadata as previous annotation

        // act: assign ANN3 to M1 by assigning other group (was group2 before)
        annot3.setGroup(grp1);

        annotService.updateAnnotation(annot3.getId(), annot3, userInfo);

        // verify
        // - ANN1, ANN2 and ANN3 are assigned to M1
        // - ANN4 is assigned to M2
        Assert.assertEquals(2, metadataRepos.count()); // both metadata still exist
        Assert.assertEquals(meta1.getId(), annotService.findAnnotationById(annot1.getId()).getMetadata().getId());
        Assert.assertEquals(meta1.getId(), annotService.findAnnotationById(annot2.getId()).getMetadata().getId());
        Assert.assertEquals(meta1.getId(), annotService.findAnnotationById(annot3.getId()).getMetadata().getId());// this is the updated changed assignment
        Assert.assertEquals(meta2.getId(), annotService.findAnnotationById(annot4.getId()).getMetadata().getId());
    }

    /**
     * test that updating an annotation is refused when it is in "SENT" status
     */
    @Test(expected = CannotUpdateSentAnnotationException.class)
    public void testUpdateAnnotationRefusedInSentStatus() throws Exception {

        final String authority = "theauthority";
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // create a simple annotation with associate metadata having response status SENT
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.getDocument().getMetadata().put("responseStatus", "IN_PREPARATION");
        final JsonAnnotation savedAnnot = annotService.createAnnotation(annot, userInfo);

        // change metadata response status to "SENT"
        final Metadata savedMetadata = annotService.findAnnotationById(savedAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // update an annotation property and launch update via service
        annot.setText("updated text");

        // update via service
        annotService.updateAnnotation(annot.getId(), annot, userInfo);
    }

    // check that a tag with given name is contained in a list of tags and that it is associated to an annotation having a given ID
    private void checkTagInListAndAssociatedToAnnotation(final List<Tag> tagList, final String tagName, final String annotationId) {

        final Optional<Tag> result = tagList.stream().filter(tag -> tag.getName().equals(tagName)).findAny();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(annotationId, result.get().getAnnotation().getId());
    }
}
