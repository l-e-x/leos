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
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.AnnotationTestRepository;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class SoftDeleteFindTest {

    private Group defaultGroup;
    
    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

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
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * let an annotation be created by the service, manipulate its status to being DELETED/ACCEPTED/REJECTED
     * -> verify that it cannot be found again by the same user
     */
    @Test
    public void testFindSoftdeletedAnnotation() throws MissingPermissionException {

        final String authority = "auth";
        final String login = "demo";
        final String hypothesisUserAccount = "acct:" + login + "@" + authority;

        final User user = new User(login);
        userRepos.save(user);
        
        final UserInformation userInfo = new UserInformation(user, authority);

        // let the annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        try {
            jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(jsAnnot);

        // verify that the annotation can be found again
        Assert.assertNotNull(annotRepos.findById(jsAnnot.getId()));

        // make it appear to be deleted already
        setAnnotationStatus(jsAnnot, AnnotationStatus.DELETED);

        // verification: search the annotation again based on its ID - not found since it is "DELETED", but technically existing
        Assert.assertNull(annotService.findAnnotationById(jsAnnot.getId(), login));
        Assert.assertNull(annotService.findAnnotationById(jsAnnot.getId()));
        Assert.assertNotNull(annotRepos.findByIdAndStatus(jsAnnot.getId(), AnnotationStatus.DELETED));

        // change the annotation to have been "ACCEPTED"
        setAnnotationStatus(jsAnnot, AnnotationStatus.ACCEPTED);

        // verification: search the annotation again based on its ID - not found since it is "ACCEPTED", but technically existing
        Assert.assertNull(annotService.findAnnotationById(jsAnnot.getId(), login));
        Assert.assertNull(annotService.findAnnotationById(jsAnnot.getId()));
        Assert.assertNotNull(annotRepos.findByIdAndStatus(jsAnnot.getId(), AnnotationStatus.ACCEPTED));

        // change the annotation to have been "REJECTED"
        setAnnotationStatus(jsAnnot, AnnotationStatus.REJECTED);

        // verification: search the annotation again based on its ID - not found since it is "REJECTED", but technically existing
        Assert.assertNull(annotService.findAnnotationById(jsAnnot.getId(), login));
        Assert.assertNull(annotService.findAnnotationById(jsAnnot.getId()));
        Assert.assertNotNull(annotRepos.findByIdAndStatus(jsAnnot.getId(), AnnotationStatus.REJECTED));

        // make it appear again
        setAnnotationStatus(jsAnnot, AnnotationStatus.NORMAL);

        // verification: search the annotation again based on its ID - not found since it is "ACCEPTED", but technically existing
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnot.getId(), login));
        Assert.assertNotNull(annotService.findAnnotationById(jsAnnot.getId()));
        Assert.assertNotNull(annotRepos.findByIdAndStatus(jsAnnot.getId(), AnnotationStatus.NORMAL));
    }

    /**
     * extensive test for (non-)retrieval of annotations and/or their replies depending on their respective status 
     * @throws CannotCreateAnnotationException 
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testFindAnnotationsDependingOnStatus() throws CannotCreateAnnotationException {

        // we create four annotations - one of each having one of the statuses NORMAL, DELETED, ACCEPTED, REJECTED
        // each of the annotations has four replies, again with each one with one of the statuses
        final String authority = Authorities.EdiT;
        final String login = "demo";
        final String hypothesisUserAccount = "acct:" + login + "@" + authority;

        final User user = new User(login);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));

        final Token token = new Token(user, authority, "a", LocalDateTime.now().plusMinutes(5), "r", LocalDateTime.now().plusMinutes(5));
        final UserInformation userInfo = new UserInformation(token);

        // let the annotations be created
        JsonAnnotation jsAnnotNormal = TestData.getTestAnnotationObject(hypothesisUserAccount);
        final URI uri = jsAnnotNormal.getUri();
        jsAnnotNormal = annotService.createAnnotation(jsAnnotNormal, userInfo);

        JsonAnnotation jsAnnotDeleted = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnotDeleted = annotService.createAnnotation(jsAnnotDeleted, userInfo);

        JsonAnnotation jsAnnotAccepted = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnotAccepted = annotService.createAnnotation(jsAnnotAccepted, userInfo);

        JsonAnnotation jsAnnotRejected = TestData.getTestAnnotationObject(hypothesisUserAccount);
        jsAnnotRejected = annotService.createAnnotation(jsAnnotRejected, userInfo);

        // create replies for "NORMAL" annotation
        final JsonAnnotation jsAnnotNormalReplyNormal = createReply(hypothesisUserAccount, userInfo, uri, jsAnnotNormal, AnnotationStatus.NORMAL);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotNormal, AnnotationStatus.DELETED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotNormal, AnnotationStatus.ACCEPTED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotNormal, AnnotationStatus.REJECTED);

        // create replies for the "DELETED" annotation
        final JsonAnnotation jsAnnotDeletedReplyNormal = createReply(hypothesisUserAccount, userInfo, uri, jsAnnotDeleted, AnnotationStatus.NORMAL);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotDeleted, AnnotationStatus.DELETED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotDeleted, AnnotationStatus.ACCEPTED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotDeleted, AnnotationStatus.REJECTED);

        // create replies for the "ACCEPTED" annotation
        final JsonAnnotation jsAnnotAcceptReplyNormal = createReply(hypothesisUserAccount, userInfo, uri, jsAnnotAccepted, AnnotationStatus.NORMAL);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotAccepted, AnnotationStatus.DELETED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotAccepted, AnnotationStatus.ACCEPTED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotAccepted, AnnotationStatus.REJECTED);

        // create replies for the "REJECTED" annotation
        final JsonAnnotation jsAnnotRejectReplyNormal = createReply(hypothesisUserAccount, userInfo, uri, jsAnnotRejected, AnnotationStatus.NORMAL);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotRejected, AnnotationStatus.DELETED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotRejected, AnnotationStatus.ACCEPTED);
        createReply(hypothesisUserAccount, userInfo, uri, jsAnnotRejected, AnnotationStatus.REJECTED);
        
        // we now have to adapt the status of the parent annotations
        // (the status could not be set before as then creating the replies would throw errors)
        setAnnotationStatus(jsAnnotDeleted, AnnotationStatus.DELETED);
        setAnnotationStatus(jsAnnotAccepted, AnnotationStatus.ACCEPTED);
        setAnnotationStatus(jsAnnotRejected, AnnotationStatus.REJECTED);

        Assert.assertEquals(20, annotRepos.count());
        
        // now launch the search - should only show the "NORMAL" parent annotation
        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), jsAnnotNormal.getGroup(),
                true, 100, 0,
                "asc", "created");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        Assert.assertEquals(1, result.getItems().size());
        Assert.assertEquals(jsAnnotNormal.getId(), result.getItems().get(0).getId());
        
        // now let's search for the associate replies - should only provide the "NORMAL" reply
        List<Annotation> replies = annotService.searchRepliesForAnnotations(result, options, userInfo);
        Assert.assertEquals(1, replies.size());
        Assert.assertEquals(jsAnnotNormalReplyNormal.getId(), replies.get(0).getId());
        
        // launch a search for the replies of ALL parent annotations - should only provide the "NORMAL" replies of the parent annotations
        final List<Annotation> allParentAnnots = new ArrayList<Annotation>();
        allParentAnnots.add(annotRepos.findById(jsAnnotNormal.getId()));
        allParentAnnots.add(annotRepos.findById(jsAnnotDeleted.getId()));
        allParentAnnots.add(annotRepos.findById(jsAnnotAccepted.getId()));
        allParentAnnots.add(annotRepos.findById(jsAnnotRejected.getId()));
        result.setItems(allParentAnnots); // pretend these items would have been returned by search
        
        replies = annotService.searchRepliesForAnnotations(result, options, userInfo);
        Assert.assertEquals(4, replies.size());
        Assert.assertTrue(replies.stream().anyMatch(ann -> jsAnnotNormalReplyNormal.getId().equals(ann.getId())));
        Assert.assertTrue(replies.stream().anyMatch(ann -> jsAnnotDeletedReplyNormal.getId().equals(ann.getId())));
        Assert.assertTrue(replies.stream().anyMatch(ann -> jsAnnotAcceptReplyNormal.getId().equals(ann.getId())));
        Assert.assertTrue(replies.stream().anyMatch(ann -> jsAnnotRejectReplyNormal.getId().equals(ann.getId())));
    }

    // -------------------------------------
    // Helper functions
    // -------------------------------------
    private void setAnnotationStatus(final JsonAnnotation jsAnnot, final AnnotationStatus newStatus) {

        final Annotation readAnnotation = annotRepos.findById(jsAnnot.getId());
        readAnnotation.setStatus(newStatus);
        annotRepos.save(readAnnotation);
    }

    private JsonAnnotation createReply(final String user, final UserInformation userInfo,
            final URI uri, final JsonAnnotation parent, final AnnotationStatus status) throws CannotCreateAnnotationException {

        JsonAnnotation jsAnnot = TestData.getTestReplyToAnnotation(user, uri, parent);
        jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        setAnnotationStatus(jsAnnot, status);
        return jsAnnot;
    }
}
