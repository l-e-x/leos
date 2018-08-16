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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Tag;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.TagRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationUpdateTest {

    /**
     * NOTE: This test class contains tests for updating annotations 
     */

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);
        userRepos.save(new User("demo"));
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private TagRepository tagRepos;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * simple update of annotation (text, updated, shared) - ID should remain same; tags not considered here
     * user requesting update is the creator of the annotation -> valid
     */
    @Test
    public void testSimpleAnnotationUpdate() throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, login);

        LocalDateTime savedUpdated = annot.getUpdated();
        LocalDateTime savedCreated = annot.getCreated();
        String id = annot.getId();

        // update the annotation properties and launch update via service
        annot.setText("new text");
        annot.getPermissions().setRead(Arrays.asList(hypothesisUserAccount)); // change from public to private

        // update via service
        JsonAnnotation updatedAnnot = annotService.updateAnnotation(annot.getId(), annot, login);
        Assert.assertEquals(id, updatedAnnot.getId()); // id remains same

        // read annotation from database and verify
        Annotation readAnnot = annotService.findAnnotationById(id);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals("new text", readAnnot.getText());
        Assert.assertFalse(readAnnot.isShared());
        Assert.assertEquals(savedCreated, readAnnot.getCreated());
        Assert.assertTrue(readAnnot.getUpdated().compareTo(savedUpdated) >= 0); // equal or after, but now before!
    }

    /**
     * simple update of annotation, but from a different user than the creator
     * -> not valid
     */
    @Test
    public void testSimpleAnnotationUpdateForbidden() throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo", otherLogin = "demo2";

        // create second user
        userRepos.save(new User(otherLogin));

        // create an annotation from first user
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(annot, login);

        LocalDateTime savedUpdated = annot.getUpdated();
        LocalDateTime savedCreated = annot.getCreated();
        String savedText = annot.getText();
        String id = annot.getId();

        // update the annotation properties and launch update via service
        annot.setText("new text");

        // update via service - as second user
        try {
            annotService.updateAnnotation(annot.getId(), annot, otherLogin);
            Assert.fail("Expected exception about missing permissions not received");
        } catch (MissingPermissionException mpe) {
            // OK
        }

        // read annotation from database and verify that it was not touched
        Annotation readAnnot = annotService.findAnnotationById(id);
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(savedText, readAnnot.getText());
        Assert.assertTrue(readAnnot.isShared());
        Assert.assertEquals(savedCreated, readAnnot.getCreated());
        Assert.assertEquals(savedUpdated, readAnnot.getUpdated());
    }

    /**
     * trying to update a non-existing annotation -> exception
     */
    @Test
    public void testUpdateNonExistingAnnotation() {

        final String hypothesisUserAccount = "acct:user@email.net";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setId("myid");

        try {
            // should fail as the annotation with this ID is unknown and thus cannot be updated
            annotService.updateAnnotation(annot.getId(), annot, hypothesisUserAccount);
            Assert.fail("Expected exception not thrown!");
        } catch (CannotUpdateAnnotationException cuae) {
            // OK
        } catch (Exception e) {
            // NOK
            Assert.fail("Received unexpected exception: " + e);
        }
    }

    /**
     * trying to update an existing annotation without specifying annotation ID-> exception
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testUpdateAnnotationWithoutAnnotationId() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot = annotService.createAnnotation(annot, login);

        try {
            // should fail as the annotation ID is missing
            annotService.updateAnnotation("", annot, login);
            Assert.fail("Expected exception not thrown!");
        } catch (Exception e) {
            // OK
        }
    }

    /**
     * update an annotation not having tags: add tags
     * -> verify that the tags are properly saved
     */
    @Test
    public void testUpdateAnnotationWithNewTags() throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";
        final String firstTagName = "mynewtag", secondTagName = "mysecondtag";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTags(null);
        annotService.createAnnotation(annot, login);

        String id = annot.getId();

        // update annotation
        annot.setTags(Arrays.asList(firstTagName, secondTagName));
        annotService.updateAnnotation(id, annot, login);

        // verify that the tags were saved and are assigned to the annotation
        Annotation readAnnot = annotService.findAnnotationById(id);
        Assert.assertNotNull(readAnnot.getTags());
        Assert.assertEquals(2, readAnnot.getTags().size());

        // check via tag repository
        List<Tag> readTags = (List<Tag>) tagRepos.findAll();
        Assert.assertEquals(2, readTags.size());

        // check that the tags have correct associations
        checkTagInListAndAssociatEdToAnnotation(readTags, firstTagName, id);
        checkTagInListAndAssociatEdToAnnotation(readTags, secondTagName, id);
    }

    /**
     * update an annotation having tags: remove tags
     * -> verify that the tags are properly removed from repository
     * (this test case was created as by pure hibernate could not be used for removal)
     */
    @Test
    public void testUpdateAnnotationByRemovingTags() throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";
        final String firstTagName = "mynewtag", secondTagName = "mysecondtag";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTags(Arrays.asList(firstTagName, secondTagName));
        annotService.createAnnotation(annot, login);

        String id = annot.getId();

        // update annotation
        annot.setTags(new ArrayList<String>());
        annotService.updateAnnotation(id, annot, login);

        // verify that the tags were removed
        Annotation readAnnot = annotService.findAnnotationById(id);
        Assert.assertNotNull(readAnnot.getTags());
        Assert.assertEquals(0, readAnnot.getTags().size());

        // check via tag repository
        List<Tag> readTags = (List<Tag>) tagRepos.findAll();
        Assert.assertEquals(0, readTags.size());
    }

    /**
     * update an annotation having tags: add and remove tags
     * -> verify that the tags are properly saved/removed
     * (this test case was created as by pure hibernate could not be used for removal)
     */
    @Test
    public void testUpdateAnnotationWithDifferentTags() throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";
        final String tagNameRemains = "mynewtag", tagNameRemoved = "mysecondtag", tagNameAdded = "mythirdtag";

        // create an annotation
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTags(Arrays.asList(tagNameRemains, tagNameRemoved));
        annotService.createAnnotation(annot, login);

        String id = annot.getId();

        // update annotation
        annot.setTags(Arrays.asList(tagNameRemains, tagNameAdded)); // tag tagNameRemoved is replaced by tagNameAdded
        annotService.updateAnnotation(id, annot, login);

        // verify that the tags were saved and are assigned to the annotation
        Annotation readAnnot = annotService.findAnnotationById(id);
        Assert.assertNotNull(readAnnot.getTags());
        Assert.assertEquals(2, readAnnot.getTags().size());
        checkTagInListAndAssociatEdToAnnotation(readAnnot.getTags(), tagNameRemains, id);
        checkTagInListAndAssociatEdToAnnotation(readAnnot.getTags(), tagNameAdded, id);

        // check via tag repository
        List<Tag> readTags = (List<Tag>) tagRepos.findAll();
        Assert.assertEquals(2, readTags.size());

        // check that the tags have correct associations
        checkTagInListAndAssociatEdToAnnotation(readTags, tagNameRemains, id);
        checkTagInListAndAssociatEdToAnnotation(readTags, tagNameAdded, id);
    }

    // check that a tag with given name is contained in a list of tags and that it is associated to an annotation having a given ID
    private void checkTagInListAndAssociatEdToAnnotation(List<Tag> tagList, String tagName, String annotationId) {

        Optional<Tag> result = tagList.stream().filter(tag -> tag.getName().equals(tagName)).findAny();
        Assert.assertTrue(result.isPresent());
        Assert.assertEquals(annotationId, result.get().getAnnotation().getId());
    }
}
