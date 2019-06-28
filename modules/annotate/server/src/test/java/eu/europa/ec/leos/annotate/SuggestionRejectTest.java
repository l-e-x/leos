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
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotRejectSentSuggestionException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotRejectSuggestionException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
import eu.europa.ec.leos.annotate.services.exceptions.NoSuggestionException;
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

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class SuggestionRejectTest {

    /**
     * This class contains tests for rejecting suggestions
     * this also covers special cases like deleting suggestions with sub replies
     */

    private User theUser;
    private static final String DEMO_LOGIN = "demo";
    private static final String USER_ACCOUNT = "acct:" + DEMO_LOGIN + "@" + Authorities.ISC;

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
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private TagRepository tagRepos;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        theUser = userRepos.save(new User(DEMO_LOGIN));
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
     * a suggestion without replies is rejected
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testRejectSimpleSuggestion() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;

        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // reject it again - works as user was assigned to annotation group in test setup
        annotService.rejectSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the document remains in DB and the tags have also been kept
        // annotation is flagged as rejected
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.REJECTED, theUser.getId());

        // check that the annotation status would be reported correctly in JSON format
        final Annotation annot = annotRepos.findById(jsAnnot.getId());
        final JsonAnnotation jsConverted = conversionService.convertToJsonAnnotation(annot, userInfo);
        Assert.assertNotNull(jsConverted);
        Assert.assertEquals(AnnotationStatus.REJECTED, jsConverted.getStatus().getStatus());
        Assert.assertNotNull(jsConverted.getStatus().getUpdated());
        // corresponds to user of UserInformation instance
        Assert.assertEquals(userInfo.getAsHypothesisAccount(), jsConverted.getStatus().getUpdated_by());
    }

    /**
     * a suggestion with response status SENT should be rejected in ISC
     * -> refused since SENT items may not be changed
     */
    @Test
    public void testCannotRejectSuggestionInIsc() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;
        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // change metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        try {
            // reject it - should throw exception due to response status SENT
            annotService.rejectSuggestionById(jsAnnot.getId(), userInfo);
            Assert.fail("Expected exception for not being able to reject SENT suggestion not received");
        } catch (CannotRejectSentSuggestionException ex) {
            // OK
        }

        // verify that the document remains in DB and the tags have also been kept
        // annotation is not flagged as rejected, but still as NORMAL
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.NORMAL, null);
    }

    /**
     * a suggestion with response status SENT should be rejected in LEOS
     * -> feasible
     */
    @Test
    public void testRejectSuggestionInLeos() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;
        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // change metadata response status to SENT
        final Metadata savedMetadata = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMetadata.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMetadata);

        // now make the user become a LEOS user
        userInfo.setAuthority(Authorities.EdiT);
        
        // reject it - should be possible
        annotService.rejectSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the document remains in DB and the tags have also been kept
        // annotation is flagged as rejected
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.REJECTED, userInfo.getUser().getId());
    }
    
    /**
     * a suggestion with a reply is rejected
     * -> this also cleans up the document referred to
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testRejectSimpleSuggestionWithReply() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);

        // retrieve our test suggestion and a reply to it
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, userInfo);

        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(hypothesisUserAccount, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, userInfo);

        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());

        // reject the suggestion - works as user was assigned to annotation group in test setup
        annotService.rejectSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the document remains in DB and the tags have also been kept
        // annotations are all flagged as being REJECTED
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.REJECTED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsReply.getId(), AnnotationStatus.REJECTED, theUser.getId());
    }

    /**
     * a suggestion with undefined ID is tried to be rejected
     * -> error expected 
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testUndefinedSuggestionId() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        annotService.rejectSuggestionById(null, userInfo);
        Assert.fail("Expected exception for undefined suggestion ID not received");
    }

    /**
     * a suggestion cannot be found by its ID
     * -> error expected
     */
    @Test(expected = CannotRejectSuggestionException.class)
    public void testSuggestionNotFound() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        annotService.rejectSuggestionById("suggestionId", userInfo);
        Assert.fail("Expected exception for unavailable suggestion not received");
    }

    /**
     * an annotation not representing a suggestion is tried to be rejected
     * -> error expected
     */
    @Test(expected = NoSuggestionException.class)
    public void testRejectNonSuggestion() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;
        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_COMMENT)); // -> no suggestion
        annotService.createAnnotation(jsAnnot, userInfo);

        annotService.rejectSuggestionById(jsAnnot.getId(), userInfo);
        Assert.fail("Expected exception for invalid annotation type (no suggestion) not received");
    }

    /**
     * a suggestion should be rejected, but requesting user not specified
     * -> error expected
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRejectSuggestionWithoutUserLogin() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;

        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, new UserInformation(theUser, Authorities.ISC));

        annotService.rejectSuggestionById(jsAnnot.getId(), null);
        Assert.fail("Expected exception for invalid user not received");
    }

    /**
     * a suggestion should be rejected, but requesting user is unknown
     * -> error expected
     */
    @Test(expected = MissingPermissionException.class)
    public void testRejectSuggestionWithUnknownUser() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;
        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, userInfo);

        userInfo.setUser(null);
        
        annotService.rejectSuggestionById(jsAnnot.getId(), userInfo);
        Assert.fail("Expected exception for invalid user not received");
    }

    /**
     * a suggestion should be rejected, but requesting user is not member of the group to which the suggestion belongs
     * -> error expected
     */
    @Test(expected = MissingPermissionException.class)
    public void testRejectSuggestionWithUserNotBeingGroupMember() throws Exception {

        final String hypothesisUserAccount = USER_ACCOUNT;
        final String login = DEMO_LOGIN;
        final String authority = Authorities.ISC;
        final UserInformation userInfo = new UserInformation(theUser, authority);
        
        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(hypothesisUserAccount);
        annotService.createAnnotation(jsAnnot, userInfo);

        // remove user from group
        final User user = userRepos.findByLogin(login);
        userGroupRepos.delete(userGroupRepos.findByUserId(user.getId()));

        annotService.rejectSuggestionById(jsAnnot.getId(), userInfo); // should fail now as user is not member of the requested group any more
        Assert.fail("Expected exception for invalid user not received");
    }

}
