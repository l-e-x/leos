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
import eu.europa.ec.leos.annotate.model.ResponseStatusUpdateResult;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.AnnotationRepository;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.MetadataRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.StatusUpdateService;
import eu.europa.ec.leos.annotate.services.exceptions.*;
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
import org.springframework.test.context.web.WebAppConfiguration;

import java.time.LocalDateTime;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class AnnotationResponseStatusUpdateTest {

    /**
     * tests on updating the "response status"
     */
    private User user;
    private Group groupDigit;
    private static final String ACCOUNT_PREFIX = "acct:user@";
    private static final String ENTITY = "entity";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private StatusUpdateService statusService;
    
    @Autowired
    private GroupService groupService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationRepository annotRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        user = new User("demo");
        userRepos.save(user);

        groupDigit = new Group("DIGIT", true);
        groupRepos.save(groupDigit);
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * successful update of some annotations' response status; others are ignored since they don't match
     * also verifies that linked annotations are deleted and link is removed in one direction
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testUpdateResponseStatusOk()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException, CannotUpdateAnnotationStatusException {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String agriEntity = "AGRI";

        final UserInformation userInfo = new UserInformation(user, authority);

        // A (in prep) <-> B (sent)
        // create annotation A: in preparation
        final JsonAnnotation annotA = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotA.getDocument().getMetadata().put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        annotA.getDocument().getMetadata().put(ENTITY, agriEntity);
        annotService.createAnnotation(annotA, userInfo);

        // create annotation B: SENT already (must be adjusted in the DB)
        JsonAnnotation annotB = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotB.getDocument().getMetadata().put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.UNKNOWN.toString());
        annotB.getDocument().getMetadata().put(ENTITY, agriEntity);
        annotB = annotService.createAnnotation(annotB, userInfo);

        Annotation annHelp = annotService.findAnnotationById(annotB.getId());
        Metadata metaHelp = metadataRepos.findOne(annHelp.getMetadataId());
        metaHelp.setResponseStatus(Metadata.ResponseStatus.SENT);
        final LocalDateTime oldUpdateTime = LocalDateTime.now().plusMinutes(-1);
        metaHelp.setResponseStatusUpdated(oldUpdateTime);
        metaHelp.setResponseStatusUpdatedBy(user.getId() + 1); // set a different ID than the one of the user
        metadataRepos.save(metaHelp);

        // link both items
        annHelp.setLinkedAnnotationId(annotA.getId());
        annotRepos.save(annHelp);

        annHelp = annotService.findAnnotationById(annotA.getId());
        annHelp.setLinkedAnnotationId(annotB.getId());
        annotRepos.save(annHelp);

        // create annotation C (in_prep), but different "entity" metadata than other annotations -> should not be affected by response status update
        JsonAnnotation annotC = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotC.getDocument().getMetadata().put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        annotC.getDocument().getMetadata().put(ENTITY, "DIGIT"); // different entity
        annotC = annotService.createAnnotation(annotC, userInfo);

        // create annotation D: also associated to same metadata as annotation A, but not linked
        JsonAnnotation annotD = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotD = annotService.createAnnotation(annotD, userInfo);
        annHelp = annotService.findAnnotationById(annotD.getId());
        metaHelp = annotService.findAnnotationById(annotA.getId()).getMetadata();
        annHelp.setMetadata(metaHelp);
        annotRepos.save(annHelp);

        // now send the response status update request
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annotA.getGroup(), annotA.getUri().toString(),
                Metadata.ResponseStatus.SENT);
        updateRequest.setMetadataToMatch(new SimpleMetadata(ENTITY, agriEntity));
        final ResponseStatusUpdateResult rsue = statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        Assert.assertNotNull(rsue);
        final List<String> updatedAnnotIds = rsue.getUpdatedAnnotIds();
        final List<String> deletedAnnotIds = rsue.getDeletedAnnotIds();

        // check: A has changed status and is no longer linked to B
        Assert.assertEquals(2, updatedAnnotIds.size());
        Assert.assertEquals(annotA.getId(), updatedAnnotIds.get(0));
        annHelp = annotRepos.findByIdAndStatus(annotA.getId(), AnnotationStatus.NORMAL);
        Assert.assertEquals(Metadata.ResponseStatus.SENT, annHelp.getMetadata().getResponseStatus());
        Assert.assertNull(annHelp.getLinkedAnnotationId());

        // check: B still has link to A, but is DELETED; response status has not been touched
        Assert.assertEquals(1, deletedAnnotIds.size());
        annHelp = annotRepos.findByIdAndStatus(annotB.getId(), AnnotationStatus.DELETED);
        Assert.assertEquals(annotA.getId(), annHelp.getLinkedAnnotationId());
        Assert.assertEquals(AnnotationStatus.DELETED, annHelp.getStatus());
        Assert.assertEquals(annHelp.getMetadata().getResponseStatusUpdated(), oldUpdateTime); // was not updated again!
        Assert.assertNotEquals(user.getId(), annHelp.getMetadata().getResponseStatusUpdatedBy());

        // check: C was not changed, still has response status IN_PREPARATION
        annHelp = annotRepos.findByIdAndStatus(annotC.getId(), AnnotationStatus.NORMAL);
        Assert.assertEquals(Metadata.ResponseStatus.IN_PREPARATION, annHelp.getMetadata().getResponseStatus());

        // check: D has changed status, is not linked
        Assert.assertEquals(annotD.getId(), updatedAnnotIds.get(1));
        annHelp = annotRepos.findByIdAndStatus(annotD.getId(), AnnotationStatus.NORMAL);
        Assert.assertEquals(Metadata.ResponseStatus.SENT, annHelp.getMetadata().getResponseStatus());
        Assert.assertNull(annHelp.getLinkedAnnotationId());
    }

    /**
     * update of response status - nothing to update (i.e. metadata defined, but no annotations assigned to it)
     */
    @Test
    public void testUpdateResponseStatus_nothingToUpdate()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException, CannotUpdateAnnotationStatusException {

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // create annotation A: in preparation
        final JsonAnnotation annotA = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotA.getDocument().getMetadata().put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        annotA.getDocument().getMetadata().put(ENTITY, "AGRI");
        annotService.createAnnotation(annotA, userInfo);

        // but assign the annotation to different metadata that's newly created
        final Metadata metaDb = metadataRepos.findAll().iterator().next();
        final Metadata replacementMetadata = new Metadata(metaDb.getDocument(), metaDb.getGroup(), metaDb.getSystemId());
        metadataRepos.save(replacementMetadata);
        Annotation annHelp = annotService.findAnnotationById(annotA.getId());
        annHelp.setMetadata(replacementMetadata);
        annotRepos.save(annHelp);

        // now send the response status update request - there should be matching metadata, but no annotation is assigned to this metadata
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annotA.getGroup(), annotA.getUri().toString(),
                Metadata.ResponseStatus.SENT);
        updateRequest.setMetadataToMatch(new SimpleMetadata(ENTITY, "AGRI"));
        final ResponseStatusUpdateResult rsur = statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        // check: there were no updates, A has its previous status
        Assert.assertNotNull(rsur);
        Assert.assertEquals(0, rsur.getUpdatedAnnotIds().size());
        Assert.assertEquals(0, rsur.getDeletedAnnotIds().size());
        annHelp = annotRepos.findByIdAndStatus(annotA.getId(), AnnotationStatus.NORMAL);
        Assert.assertNull(annHelp.getMetadata().getResponseStatus()); // replacementMetadata doesn't have a response status set
    }

    /**
     * testing the entire workflow:
     * - annotations with version 1.0 are created, response status IN_PREPARATION
     * - they are SENT -> version 1.0, response status SENT
     * - they are deleted -> version 1.0, response status SENT, sentDeleted=true
     * - new annotations are created with version 2.0, response status IN_PREPARATION
     * - they are SENT -> now upon sending, we have to identify older annotations being sentDeleted=true and delete them
     *  
     * @throws CannotCreateAnnotationException
     * @throws CannotUpdateAnnotationStatusException
     * @throws MissingPermissionException
     * @throws CannotDeleteAnnotationException
     * @throws CannotDeleteSentAnnotationException
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testUpdateResponseStatusWithDeletedSentAnnotations() throws CannotCreateAnnotationException, CannotUpdateAnnotationStatusException,
            MissingPermissionException, CannotDeleteAnnotationException, CannotDeleteSentAnnotationException {

        // create an annotation with response status IN_PREPARATION, then SENT it -> must have sentDeleted flag set
        // delete it, and then SENT again -> must be deleted then

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;
        final String version = "version";
        final String version1 = "1.0";
        final String version2 = "2.0";

        // add user to DIGIT group
        groupService.assignUserToGroup(user, groupDigit);
        final UserInformation userInfo = new UserInformation(user, authority);

        // create annotation: in preparation
        JsonAnnotation annotV1 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotV1.getDocument().getMetadata().put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        annotV1.getDocument().getMetadata().put(version, version1);
        annotV1.setGroup(groupDigit.getName());
        annotV1 = annotService.createAnnotation(annotV1, userInfo);

        // now send the response status update request - annotation should get different response status now (SENT)
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annotV1.getGroup(), annotV1.getUri().toString(),
                Metadata.ResponseStatus.SENT);
        updateRequest.setMetadataToMatch(new SimpleMetadata(version, version1));
        final ResponseStatusUpdateResult rsur = statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        // check: there should have been one update
        Assert.assertNotNull(rsur);
        Assert.assertEquals(1, rsur.getUpdatedAnnotIds().size());
        Assert.assertEquals(0, rsur.getDeletedAnnotIds().size());

        // delete the annotation
        annotService.deleteAnnotationById(annotV1.getId(), userInfo);

        // check: should have sentDeleted=true
        Annotation readAnnot = annotService.findAnnotationById(annotV1.getId());
        Assert.assertTrue(readAnnot.isSentDeleted());
        Assert.assertTrue(readAnnot.isResponseStatusSent());
        Assert.assertEquals(AnnotationStatus.NORMAL, readAnnot.getStatus());

        // create another annotation in IN_PREPARATION status for version 2.0 -> will be SENT afterwards
        JsonAnnotation annotV2 = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annotV2.getDocument().getMetadata().put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        annotV2.getDocument().getMetadata().put(version, version2);
        annotV2.setGroup(groupDigit.getName());
        annotV2 = annotService.createAnnotation(annotV2, userInfo);

        // now send again a response status update request - should really delete the annotation now
        updateRequest.setMetadataToMatch(new SimpleMetadata(version, version2)); // send a newer version now!

        final ResponseStatusUpdateResult rsur2 = statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        // check: there should have been one deletion (annotV1), and one update (annotV2)
        Assert.assertNotNull(rsur2);
        Assert.assertEquals(1, rsur2.getUpdatedAnnotIds().size());
        Assert.assertEquals(1, rsur2.getDeletedAnnotIds().size());
        Assert.assertEquals(rsur2.getUpdatedAnnotIds().get(0), annotV2.getId());
        Assert.assertEquals(rsur2.getDeletedAnnotIds().get(0), annotV1.getId());

        // check: annotV1 is DELETED now, but still has the SENT response status and the sentDeleted flags
        readAnnot = annotRepos.findByIdAndStatus(annotV1.getId(), AnnotationStatus.DELETED);
        Assert.assertTrue(readAnnot.isSentDeleted());
        Assert.assertTrue(readAnnot.isResponseStatusSent());
        Assert.assertEquals(AnnotationStatus.DELETED, readAnnot.getStatus());
    }
}
