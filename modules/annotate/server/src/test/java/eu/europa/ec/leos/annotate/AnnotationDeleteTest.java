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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationDeleteTest {

    /**
     * This class contains tests for deleting annotations
     * this also covers special cases like deleting (intermediate) replies and cleaning no longer required documents from the database
     */

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        User theUser = userRepos.save(new User("demo"));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
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
    private AnnotationRepository annotRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * an annotation without replies is deleted
     * -> this also cleans up the document referred to and the tags since both become orphaned by the removal of the annotation
     */
    @Test
    public void testDeleteSimpleAnnotation() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve out test annotation: has two tags
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, login);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // remove it again
        annotService.deleteAnnotationById(jsAnnot.getId(), login);

        // verify that the document and the tags have also been removed as they are not referred to
        // by any other annotation
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, documentRepos.count());
        Assert.assertEquals(0, tagRepos.count());
    }

    /**
     * an annotation is deleted, but deletion request is lacking the annotation ID
     * -> should throw exception
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testDeleteAnnotationWithoutId() throws Exception {

        // remove an annotation without specifying its ID
        try {
            annotService.deleteAnnotationById("", "demo");
            Assert.fail("Expected exception about missing annotation ID not received");
        } catch (Exception e) {
            // OK
        }
    }

    /**
     * an annotation without replies is tried to be deleted by another user 
     * -> accepted (previously refused, accepted since ANOT-56)
     */
    @Test
    public void testDeleteSimpleAnnotationOfOtherUser() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo", otherLogin = "demo2";

        userRepos.save(new User(otherLogin));

        // retrieve out test annotation: has two tags
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, login);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count());

        // other user tries to remove it -> ok
        annotService.deleteAnnotationById(jsAnnot.getId(), otherLogin);

        // verify that the document and the tags have been removed
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, documentRepos.count());
        Assert.assertEquals(0, tagRepos.count());
    }

    /**
     * a reply of another user is tried to be deleted
     * -> accepted, but parent annotation shall remain
     */
    @Test
    public void testDeleteSimpleReplyOfOtherUser() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo", otherLogin = "demo2";

        userRepos.save(new User(otherLogin));

        // retrieve out test annotation: has two tags
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, login);
        JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(login, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, login);

        Assert.assertEquals(2, annotRepos.count());

        // other user tries to remove the reply -> ok
        annotService.deleteAnnotationById(jsReply.getId(), otherLogin);

        // verify that the parent annotation and the document remain
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertNotNull(annotRepos.findById(jsAnnot.getId()));
    }
    
    /**
     *  there are two annotations to the same document; both shared some tags
     *  one of the annotations is deleted
     *  -> the other one remains, the document and the shared tags also 
     */
    @Test
    public void testDeleteOneAnnotationOfTwoForSameDocument() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", LOGIN = "demo";

        // retrieve out first test annotation: has two tags; save it
        JsonAnnotation jsAnnotFirst = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnotFirst, LOGIN);

        // retrieve a second test annotation with three tags; save it
        JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnotSecond.getTags().add("a_third_tag");
        annotService.createAnnotation(jsAnnotSecond, LOGIN);

        String annotId1 = jsAnnotFirst.getId(), annotId2 = jsAnnotSecond.getId();

        // verify after initial storage: document is shared, two of the three tags also
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(5, tagRepos.count()); // five in total...
        Assert.assertEquals(2, tagRepos.countByAnnotationId(annotId1)); // ... two of them for first annotation, ...
        Assert.assertEquals(3, tagRepos.countByAnnotationId(annotId2)); // ... three of them for the second annotation

        // now we delete the first annotation
        annotService.deleteAnnotationById(annotId1, LOGIN);

        // verify
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count()); // document must remain as it is still referred to
        Assert.assertEquals(3, tagRepos.count()); // two tags must have been removed
        Assert.assertEquals(3, tagRepos.countByAnnotationId(annotId2)); // and all three remaining tags refer to the second annotation
    }

    /**
     *  an annotation with two immediate replies and a reply to a reply is created
     *  the annotation is deleted
     *  -> all child replies are also removed
     */
    @Test
    public void testEntireAnnotationThreadRemoved() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve out test annotation: has two tags; save it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, login);
        Assert.assertEquals(2, annotRepos.count());

        // add a reply to the reply
        JsonAnnotation jsAnnotFirstReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotFirstReply.getId()));
        annotService.createAnnotation(jsAnnotFirstReplyReply, login);
        Assert.assertEquals(3, annotRepos.count());

        // create a second reply to the root annotation
        JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsAnnotSecondReply, login);
        Assert.assertEquals(4, annotRepos.count());

        // still only one document is present
        Assert.assertEquals(1, documentRepos.count());

        // now remove the root
        annotService.deleteAnnotationById(jsAnnot.getId(), login);

        // verify: all annotations and the referenced (now orphaned) document have been removed
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     *  an annotation with two immediate replies and a reply to a reply is created
     *  the immediate reply not having sub replies is deleted
     *  -> only the immediate reply is removed, the annotation as well as the other reply with its sub reply are retained
     */
    @Test
    public void testDeleteReplyWithoutSubreply() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve out test annotation: has two tags; save it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, login);
        Assert.assertEquals(2, annotRepos.count());

        // add a reply to the reply
        JsonAnnotation jsAnnotFirstReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotFirstReply.getId()));
        annotService.createAnnotation(jsAnnotFirstReplyReply, login);
        Assert.assertEquals(3, annotRepos.count());

        // create a second reply to the root annotation
        JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsAnnotSecondReply, login);
        Assert.assertEquals(4, annotRepos.count());

        // still only one document is present
        Assert.assertEquals(1, documentRepos.count());

        // now remove the reply not having sub replies
        annotService.deleteAnnotationById(jsAnnotSecondReply.getId(), login);

        // verify: three annotations are remaining; the referenced document also remains
        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(2, annotRepos.countByRootAnnotationIdNotNull());
        Assert.assertEquals(1, documentRepos.count());
    }

    /**
     *  an annotation with two immediate replies and a reply to a reply is created
     *  the reply having another sub reply is deleted
     *  -> only the intermediate reply is removed, the annotation as well as the other reply and the sub reply (of the deleted one) are retained
     *     (i.e. no deletion of the whole reply sub tree is executed)
     */
    @Test
    public void testDeleteReplyHavingSubreply() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve out test annotation: has two tags; save it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, login);
        Assert.assertEquals(2, annotRepos.count());

        // add a reply to the reply
        JsonAnnotation jsAnnotFirstReplyReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotFirstReply.getId()));
        annotService.createAnnotation(jsAnnotFirstReplyReply, login);
        Assert.assertEquals(3, annotRepos.count());

        // create a second reply to the root annotation
        JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsAnnotSecondReply, login);
        Assert.assertEquals(4, annotRepos.count());

        // still only one document is present
        Assert.assertEquals(1, documentRepos.count());

        // now remove the reply having a sub reply
        annotService.deleteAnnotationById(jsAnnotFirstReply.getId(), login);

        // verify: three annotations are remaining; the referenced document also remains
        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(2, annotRepos.countByRootAnnotationIdNotNull());
        Assert.assertEquals(1, documentRepos.count());

        // verify in particular that the annotation, the sub reply of first reply, and the second reply are the items remaining
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnot.getId())); // annotation
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnotFirstReplyReply.getId())); // sub reply of first reply
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnotSecondReply.getId())); // second reply
    }

}
