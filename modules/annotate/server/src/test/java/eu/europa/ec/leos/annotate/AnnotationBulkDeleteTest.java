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
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class AnnotationBulkDeleteTest {

    /**
     * This class contains tests for bulk deletion of annotations
     * this also covers special cases like deleting (intermediate) replies and cleaning no longer required documents from the database
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
    private DocumentRepository documentRepos;
    
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

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
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    
    /**
     * test bulk deletion: delete two out of three annotations
     * -> should work, one annotation remains
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testBulkDeleteAnnotations() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotations
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final JsonAnnotation jsAnnot2 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final JsonAnnotation jsAnnot3 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        annotService.createAnnotation(jsAnnot, userInfo);
        annotService.createAnnotation(jsAnnot2, userInfo);
        annotService.createAnnotation(jsAnnot3, userInfo);

        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());

        // remove the first two again
        final List<String> deletedAnnotations = annotService.deleteAnnotationsById(Arrays.asList(jsAnnot.getId(), jsAnnot2.getId()), userInfo);
        Assert.assertNotNull(deletedAnnotations);
        Assert.assertEquals(2, deletedAnnotations.size());
        Assert.assertTrue(deletedAnnotations.stream().anyMatch(annotId -> annotId.equals(jsAnnot.getId())));
        Assert.assertTrue(deletedAnnotations.stream().anyMatch(annotId -> annotId.equals(jsAnnot2.getId())));
        
        // verify that the document and annotations remain in DB, but annotations are flagged as being deleted
        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.DELETED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsAnnot2.getId(), AnnotationStatus.DELETED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsAnnot3.getId(), AnnotationStatus.NORMAL, null);
    }
    
    /**
     * test bulk deletion: delete a root annotation before trying to delete a reply of it
     * -> should work without error
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testBulkDeleteAnnotations_DeleteRootBeforeReply() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);

        // retrieve our test annotations: root, reply, reply to the reply
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, userInfo);

        final JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), jsAnnot);
        annotService.createAnnotation(jsAnnotReply, userInfo);
        
        final JsonAnnotation jsAnnotReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), 
                Arrays.asList(jsAnnot.getId(), jsAnnotReply.getId()));
        annotService.createAnnotation(jsAnnotReplyReply, userInfo);

        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());

        // remove the root and the reply: by removing the root, all children should already be removed
        final List<String> deletedAnnotations = annotService.deleteAnnotationsById(Arrays.asList(jsAnnot.getId(), jsAnnotReply.getId()), userInfo);
        Assert.assertNotNull(deletedAnnotations);
        Assert.assertEquals(1, deletedAnnotations.size());
        Assert.assertTrue(deletedAnnotations.stream().anyMatch(annotId -> annotId.equals(jsAnnot.getId())));
        
        // verify that the annotations remain in DB, but have ALL been flagged as being deleted (as all replies are deleted when root is deleted)
        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.DELETED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsAnnotReply.getId(), AnnotationStatus.DELETED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsAnnotReplyReply.getId(), AnnotationStatus.DELETED, theUser.getId());
    }
    
    /**
     * test bulk deletion: try to delete two annotations, which do not exist
     * -> should not delete anything, all annotations remain
     */
    @Test
    public void testBulkDeleteAnnotations_NoneFound() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotations
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final JsonAnnotation jsAnnot2 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        annotService.createAnnotation(jsAnnot, userInfo);
        annotService.createAnnotation(jsAnnot2, userInfo);

        Assert.assertEquals(2, annotRepos.count());

        // try removing some arbitrary other annotations
        final List<String> deletedAnnotations = annotService.deleteAnnotationsById(Arrays.asList(jsAnnot.getId() + "x", jsAnnot2.getId() + "y"), userInfo);
        Assert.assertNotNull(deletedAnnotations);
        Assert.assertEquals(0, deletedAnnotations.size());
        
        // verify that the annotations remain in DB, and annotations are still flagged NORMAL
        Assert.assertEquals(2, annotRepos.count());
        
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.NORMAL, null);
        TestHelper.assertHasStatus(annotRepos, jsAnnot2.getId(), AnnotationStatus.NORMAL, null);
    }
    
    /**
     * test bulk deletion: try to delete no annotations
     * -> should not delete anything, all annotations remain
     */
    @Test
    public void testBulkDeleteAnnotations_NoIdsGiven() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotations
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final JsonAnnotation jsAnnot2 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        annotService.createAnnotation(jsAnnot, userInfo);
        annotService.createAnnotation(jsAnnot2, userInfo);

        Assert.assertEquals(2, annotRepos.count());

        // try removing "no" annotations
        final List<String> deletedAnnotations = annotService.deleteAnnotationsById(new ArrayList<String>(), userInfo);
        Assert.assertNotNull(deletedAnnotations);
        Assert.assertEquals(0, deletedAnnotations.size());
        
        // verify that the annotations remain in DB, and annotations are still flagged NORMAL
        Assert.assertEquals(2, annotRepos.count());
        
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.NORMAL, null);
        TestHelper.assertHasStatus(annotRepos, jsAnnot2.getId(), AnnotationStatus.NORMAL, null);
    }
    
    /**
     * test bulk deletion: try to delete no annotations (give null)
     * -> should not delete anything, all annotations remain
     */
    @Test
    public void testBulkDeleteAnnotations_IdsNull() throws Exception {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = HYPO_PREFIX + authority;

        // retrieve out test annotations
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        userInfo.setUser(theUser);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());

        // try removing some arbitrary other annotations
        final List<String> deletedAnnotations = annotService.deleteAnnotationsById(null, userInfo);
        Assert.assertNotNull(deletedAnnotations);
        Assert.assertEquals(0, deletedAnnotations.size());
        
        // verify that the annotations remain in DB, and annotations are still flagged NORMAL
        Assert.assertEquals(1, annotRepos.count());
        
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.NORMAL, null);
    }
    
    /**
     * test bulk deletion: try to delete no annotations without giving user information
     * -> should throw exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBulkDeleteAnnotations_NoUserinfo() throws Exception {

        // try removing some arbitrary other annotations
        annotService.deleteAnnotationsById(Arrays.asList("someid"), null);
    }
    
}
