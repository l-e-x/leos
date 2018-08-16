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
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
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

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationRetrievalTest {

    private Group defaultGroup;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------

    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * let an annotation be created by the service, and verify if can be found again by the same user
     */
    @Test
    public void testFindAnnotation() throws MissingPermissionException {

        // done here instead of in tests before-execution function to avoid SpotBugs complaint and to avoid suppressing SpotBugs warning
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";
        userRepos.save(new User(login));

        // let the annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        try {
            jsAnnot = annotService.createAnnotation(jsAnnot, login);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(jsAnnot);

        // verification: search the annotation again based on its ID with the same user
        Annotation ann = annotService.findAnnotationById(jsAnnot.getId(), login);
        Assert.assertNotNull(ann);
    }

    /**
     * search for an annotation by using empty ID, and for non-existing annotation
     * @throws MissingPermissionException 
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Done on purpose as test checks that exception is thrown")
    public void testFindNonExistingAnnotation() throws CannotCreateAnnotationException, MissingPermissionException {

        // done here instead of in tests before-execution function to avoid SpotBugs complaint and to avoid suppressing SpotBugs warning
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";
        userRepos.save(new User(login));

        // let the annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, login);

        // verification: search the annotation again based on its ID with the same user
        try {
            annotService.findAnnotationById(null, login);
            Assert.fail("Expected exception for missing annotation ID not received");
        } catch (Exception e) {
            // OK
        }

        // search for non-existing ID
        Assert.assertNull(annotService.findAnnotationById("myid", login));
    }

    /**
     * let a private annotation be created by the service, and different user wants to see it
     * -> not shown
     */
    @Test
    public void testFindPrivateAnnotationOfOtherUser() throws MissingPermissionException, CannotCreateAnnotationException {

        // done here instead of in tests before-execution function to avoid SpotBugs complaint and to avoid suppressing SpotBugs warning
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create two users and assign them to the default group
        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo", otherLogin = "demo2";

        User theUser = userRepos.save(new User(login));
        User secondUser = userRepos.save(new User(otherLogin));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));

        // let the annotation be created - private for the first user
        JsonAnnotation jsAnnot = TestData.getTestPrivateAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        Assert.assertNotNull(jsAnnot);

        // verification: search the annotation again based on its ID and from another user
        // -> as it is private, the other user may not see it
        try {
            annotService.findAnnotationById(jsAnnot.getId(), otherLogin);
            Assert.fail("Expected exception concerning permission violation not received!");
        } catch (MissingPermissionException mpe) {
            // OK
        }
    }

    /**
     * let a public annotation be created, try retrieving it by a different user of same group
     * --> is valid
     */
    @Test
    public void testFindPublicAnnotationOfOtherUser() throws MissingPermissionException, CannotCreateAnnotationException {

        // done here instead of in tests before-execution function to avoid SpotBugs complaint and to avoid suppressing SpotBugs warning
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create two users and assign them to the default group
        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo", otherLogin = "demo2";

        User theUser = userRepos.save(new User(login));
        User secondUser = userRepos.save(new User(otherLogin));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));

        // let the public annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        Assert.assertNotNull(jsAnnot);

        // verification: search the annotation again based on its ID and from another user
        // -> as it is public, the other user may see it
        Annotation annot = annotService.findAnnotationById(jsAnnot.getId(), otherLogin);
        Assert.assertNotNull(annot);
    }

    /**
     * let a public annotation be created; different user not belonging to the group tries seeing it
     * -> not shown
     */
    @Test
    public void testFindPublicAnnotationOfOtherUserNotInGroup() throws MissingPermissionException, CannotCreateAnnotationException {

        // done here instead of in tests before-execution function to avoid SpotBugs complaint and to avoid suppressing SpotBugs warning
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create two users and assign only one of them to the default group
        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo", otherLogin = "demo2";
        User theUser = userRepos.save(new User(login));
        userRepos.save(new User(otherLogin));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        // let the public annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot = annotService.createAnnotation(jsAnnot, login);
        Assert.assertNotNull(jsAnnot);

        // verification: search the annotation again based on its ID and from another user
        // -> as it is public, the other user may theoretically see it, but he is not a group member
        try {
            annotService.findAnnotationById(jsAnnot.getId(), otherLogin);
            Assert.fail("Expected exception concerning permission violation not received!");
        } catch (MissingPermissionException mpe) {
            // OK
        }
    }
}
