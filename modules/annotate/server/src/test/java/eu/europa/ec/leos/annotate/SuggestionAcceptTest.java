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
import eu.europa.ec.leos.annotate.services.exceptions.CannotAcceptSentSuggestionException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotAcceptSuggestionException;
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
public class SuggestionAcceptTest {

    /**
     * This class contains tests for accepting suggestions
     * this also covers special cases like deleting suggestions with sub replies
     */

    private static final String LOGIN = "demo";
    private static final String HYPO_USER_ACCOUNT = "acct:" + LOGIN + "@" + Authorities.ISC;
    private User theUser;

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
     * a suggestion without replies is accepted
     * -> should be working
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testAcceptSimpleSuggestion() throws Exception {

        final String authority = Authorities.ISC;

        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        final UserInformation userInfo = new UserInformation(theUser, authority);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // accept it again - works as user was assigned to annotation group in test setup
        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the document remains in DB and the tags have been kept
        // annotation is flagged as ACCEPTED
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.ACCEPTED, theUser.getId());

        // check that the annotation status would be reported correctly in JSON format
        final Annotation annot = annotRepos.findById(jsAnnot.getId());
        final JsonAnnotation jsConverted = conversionService.convertToJsonAnnotation(annot, userInfo);
        Assert.assertNotNull(jsConverted);
        Assert.assertEquals(AnnotationStatus.ACCEPTED, jsConverted.getStatus().getStatus());
        Assert.assertNotNull(jsConverted.getStatus().getUpdated());
        // corresponds to user of UserInformation instance
        Assert.assertEquals(userInfo.getAsHypothesisAccount(), jsConverted.getStatus().getUpdated_by());
    }

    /**
     * a suggestion with response status SENT is accepted from ISC
     * -> should be prohibited
     */
    @Test
    public void testCannotAcceptSentSuggestionInIsc() throws Exception {

        final String authority = Authorities.ISC;
        final UserInformation userInfo = new UserInformation(theUser, authority);
        
        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // change the metadata response status of the suggestion
        final Metadata savedMeta = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMeta.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMeta);

        // accept it - should throw exception since the SENT response status does not allow accepting
        try {
            annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);
            Assert.fail("Expected exception for disallowed accepting of SENT suggestion not received");
        } catch (CannotAcceptSentSuggestionException ex) {
            // OK
        }

        // verify that the document remains in DB and the tags have been kept
        // annotation is still flagged as NORMAL, not ACCEPTED!
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.NORMAL, null);
    }

    /**
     * a suggestion with response status SENT is accepted from LEOS
     * -> should be possible
     */
    @Test
    public void testAcceptSentSuggestionInLeos() throws Exception {

        final String authority = Authorities.ISC;
        final UserInformation userInfo = new UserInformation(theUser, authority);
        
        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, userInfo);

        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());

        // change the metadata response status of the suggestion
        final Metadata savedMeta = annotService.findAnnotationById(jsAnnot.getId()).getMetadata();
        savedMeta.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(savedMeta);

        // change the user's authority such that he is a LEOS user now
        userInfo.setAuthority(Authorities.EdiT);
        
        // accept it - should be ok
        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the document remains in DB and the tags have been kept
        // annotation is flagged as ACCEPTED
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, tagRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.ACCEPTED, theUser.getId());
    }
    
    /**
     * a suggestion with a reply is accepted
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testAcceptSimpleSuggestionWithReply() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);

        // retrieve our test suggestion and a reply to it
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, userInfo);

        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(HYPO_USER_ACCOUNT, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, userInfo);

        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());

        // accept the suggestion - works as user was assigned to annotation group in test setup
        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the document remains in DB, and the suggestion and its reply are flagged as being accepted
        // verify that the tags have been kept as they are still referred to by the annotation
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(2, tagRepos.count()); // (suggestion, myreplytag)
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.ACCEPTED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsReply.getId(), AnnotationStatus.ACCEPTED, theUser.getId());
    }

    /**
     * a suggestion with a reply is accepted; the reply was already DELETED before, 
     * and should thus keep its DELETED state
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testAcceptSimpleSuggestionWithDeletedReply() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);

        // retrieve our test suggestion and a reply to it
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, userInfo);

        final JsonAnnotation jsReply = TestData.getTestReplyToAnnotation(HYPO_USER_ACCOUNT, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        annotService.createAnnotation(jsReply, userInfo);

        // make the reply be deleted
        final Annotation savedReply = annotRepos.findById(jsReply.getId());
        savedReply.setStatus(AnnotationStatus.DELETED);
        annotRepos.save(savedReply);

        // accept the suggestion - works as user was assigned to annotation group in test setup
        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);

        // verify that the annotation was ACCEPTED, but its reply remains in DELETED state (was not ACCEPTED)
        Assert.assertEquals(2, annotRepos.count());
        TestHelper.assertHasStatus(annotRepos, jsAnnot.getId(), AnnotationStatus.ACCEPTED, theUser.getId());
        TestHelper.assertHasStatus(annotRepos, jsReply.getId(), AnnotationStatus.DELETED, null); // null since we manually manipulated status; AnnotationService
                                                                                                 // was not involved
    }

    /**
     * a suggestion with undefined ID is tried to be accepted
     * -> error expected 
     */
    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testUndefinedSuggestionId() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        annotService.acceptSuggestionById(null, userInfo);
        Assert.fail("Expected exception for undefined suggestion ID not received");
    }

    /**
     * a suggestion cannot be found by its ID
     * -> error expected
     */
    @Test(expected = CannotAcceptSuggestionException.class)
    public void testSuggestionNotFound() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        annotService.acceptSuggestionById("suggestionId", userInfo);
        Assert.fail("Expected exception for unavailable suggestion not received");
    }

    /**
     * an annotation not representing a suggestion is tried to be accepted
     * -> error expected
     */
    @Test(expected = NoSuggestionException.class)
    public void testAcceptNonSuggestion() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(HYPO_USER_ACCOUNT);
        jsAnnot.setTags(Arrays.asList(Annotation.ANNOTATION_COMMENT)); // -> no suggestion
        annotService.createAnnotation(jsAnnot, userInfo);

        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);
        Assert.fail("Expected exception for invalid annotation type (no suggestion) not received");
    }

    /**
     * a suggestion should be accepted, but requesting user not specified
     * -> error expected
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAcceptSuggestionWithoutUserLogin() throws Exception {

        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, new UserInformation(theUser, Authorities.ISC));

        annotService.acceptSuggestionById(jsAnnot.getId(), null);
        Assert.fail("Expected exception for invalid user not received");
    }

    /**
     * a suggestion should be accepted, but requesting user is unknown
     * -> error expected
     */
    @Test(expected = MissingPermissionException.class)
    public void testAcceptSuggestionWithUnknownUser() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, userInfo);

        userInfo.setUser(null);
        
        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);
        Assert.fail("Expected exception for invalid user not received");
    }

    /**
     * a suggestion should be accepted, but requesting user is not member of the group to which the suggestion belongs
     * -> error expected
     */
    @Test(expected = MissingPermissionException.class)
    public void testAcceptSuggestionWithUserNotBeingGroupMember() throws Exception {

        final UserInformation userInfo = new UserInformation(theUser, Authorities.ISC);
        
        // retrieve our test annotation
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        annotService.createAnnotation(jsAnnot, userInfo);

        // remove user from group
        final User user = userRepos.findByLogin(LOGIN);
        userGroupRepos.delete(userGroupRepos.findByUserId(user.getId()));

        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo); // should fail now as user is not member of the requested group any more
        Assert.fail("Expected exception for invalid user not received");
    }
}
