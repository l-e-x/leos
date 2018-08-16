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
import eu.europa.ec.leos.annotate.services.exceptions.*;
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
public class SuggestionRejectTest {

    /**
     * This class contains tests for rejecting suggestions
     * this also covers special cases like deleting suggestions with sub replies
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
     * a suggestion without replies is rejected
     * -> this also cleans up the document referred to
     */
    @Test
    public void testRejectSimpleSuggestion() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve our test annotation and make it become a suggestion
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));
        annotService.createAnnotation(jsAnnot, login);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // reject it again - works as user was assigned to annotation group in test setup
        annotService.rejectSuggestionById(jsAnnot.getId(), login);

        // verify that the document and the tags have also been removed as they are not referred to
        // by any other annotation
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, documentRepos.count());
        Assert.assertEquals(0, tagRepos.count());
    }

    /**
     * a suggestion with a reply is rejected
     * -> this also cleans up the document referred to
     */
    @Test
    public void testRejectSimpleSuggestionWithReply() throws Exception {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve our test suggestion and a reply to it
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion"));
        annotService.createAnnotation(jsAnnot, login);

        JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, login);

        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());

        // reject the suggestion - works as user was assigned to annotation group in test setup
        annotService.rejectSuggestionById(jsAnnot.getId(), login);

        // verify that the document and the tags have also been removed as they are not referred to
        // by any other annotation
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, documentRepos.count());
    }

    /**
     * a suggestion with undefined ID is tried to be rejected
     * -> error expected 
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testUndefinedSuggestionId() {

        try {
            annotService.rejectSuggestionById(null, "login");
            Assert.fail("Expected exception for undefined suggestion ID not received");
        } catch (Exception e) {
            // OK
        }
    }

    /**
     * a suggestion cannot be found by its ID
     * -> error expected
     */
    @Test
    public void testSuggestionNotFound() {

        try {
            annotService.rejectSuggestionById("suggestionId", "login");
            Assert.fail("Expected exception for unavailable suggestion not received");
        } catch (CannotRejectSuggestionException casex) {
            // ok
        } catch (Exception e) {
            Assert.fail("Received different exception than expected for unavailable suggestion");
        }
    }

    /**
     * an annotation not representing a suggestion is tried to be rejected
     * -> error expected
     */
    @Test
    public void testRejectNonSuggestion() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve our test annotation
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("comment")); // -> no suggestion
        annotService.createAnnotation(jsAnnot, login);

        try {
            annotService.rejectSuggestionById(jsAnnot.getId(), login);
            Assert.fail("Expected exception for invalid annotation type (no suggestion) not received");
        } catch (NoSuggestionException casex) {
            // ok
        } catch (Exception e) {
            Assert.fail("Received different exception than expected for trying to accept non-suggestion annotation");
        }
    }

    /**
     * a suggestion should be rejected, but requesting user not specified
     * -> error expected
     */
    @Test
    public void testRejectSuggestionWithoutUserLogin() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve our test annotation
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion")); // -> becomes suggestion
        annotService.createAnnotation(jsAnnot, login);

        try {
            annotService.rejectSuggestionById(jsAnnot.getId(), null);
            Assert.fail("Expected exception for invalid user not received");
        } catch (MissingPermissionException casex) {
            // ok
        } catch (Exception e) {
            Assert.fail("Received different exception than expected for trying to reject suggestion");
        }
    }

    /**
     * a suggestion should be rejected, but requesting user is unknown
     * -> error expected
     */
    @Test
    public void testRejectSuggestionWithUnknownUser() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve our test annotation
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion")); // -> becomes suggestion
        annotService.createAnnotation(jsAnnot, login);

        try {
            annotService.rejectSuggestionById(jsAnnot.getId(), "unknownUser");
            Assert.fail("Expected exception for invalid user not received");
        } catch (MissingPermissionException casex) {
            // ok
        } catch (Exception e) {
            Assert.fail("Received different exception than expected for trying to accept suggestion");
        }
    }

    /**
     * a suggestion should be rejected, but requesting user is not member of the group to which the suggestion belongs
     * -> error expected
     */
    @Test
    public void testRejectSuggestionWithUserNotBeingGroupMember() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@europa.eu", login = "demo";

        // retrieve our test annotation
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList("suggestion")); // -> becomes suggestion
        annotService.createAnnotation(jsAnnot, login);

        // remove user from group
        User user = userRepos.findByLogin(login);
        userGroupRepos.delete(userGroupRepos.findByUserId(user.getId()));

        try {
            annotService.rejectSuggestionById(jsAnnot.getId(), login); // should fail now as user is not member of the requested group any more
            Assert.fail("Expected exception for invalid user not received");
        } catch (MissingPermissionException casex) {
            // ok
        } catch (Exception e) {
            Assert.fail("Received different exception than expected for trying to reject suggestion");
        }
    }

}
