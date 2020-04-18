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

import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.UUIDGeneratorService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
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
import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class CreateAnnotationRepliesTest {

    private static final String UNEXPECTED_EXCEPTION = "Unexpected exception received: ";
    private User user;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private UserRepository userRepos;
    
    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private AnnotationService annotService;

    @Autowired
    private UUIDGeneratorService uuidGeneratorService;

    // -------------------------------------
    // Cleanup of database content before running new test
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
     * a new top-level annotation is created, and a reply for it
     * -> verifies that the reply was created and is associated to the parent annotation
     */
    @Test
    public void testInsertAnnotationAndReply() {

        final String authority = "LEOS";
        final String username = "acct:myusername@" + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        // let the root annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        try {
            jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertNotNull(jsAnnot);
        Assert.assertTrue(!jsAnnot.getId().isEmpty());

        // create a reply to the root annotation (-> reference contains the root annotation's ID)
        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(username, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        try {
            jsAnnotReply = annotService.createAnnotation(jsAnnotReply, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertTrue(!jsAnnotReply.getId().isEmpty());

        // verification: search the reply again based on its ID...
        final Annotation ann = annotService.findAnnotationById(jsAnnotReply.getId());
        Assert.assertNotNull(ann);
        Assert.assertEquals(jsAnnot.getId(), ann.getReferences());
        Assert.assertEquals(1, ann.getReferencesList().size());
        Assert.assertEquals(jsAnnot.getId(), ann.getReferencesList().get(0));
        Assert.assertEquals(jsAnnot.getId(), ann.getRootAnnotationId()); // computed value set correctly for annotation root
        Assert.assertTrue(ann.isReply());

        // ...and there must be two annotations in total and three tags (2 from annotation, 1 from reply)
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(3, tagRepos.count());
    }

    /**
     * a new top-level annotation is created, and a reply for it
     * -> verifies that the reply was created and is associated to the parent annotation
     */
    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void testCannotReplyToSentAnnotation() {

        final String annotUsername = "acct:myusername@" + Authorities.ISC;
        final String replyUsername = "acct:otheruser@" + Authorities.EdiT;
        final String ISC_REF = "ISC/4/8";

        final User replyUser = new User("demoReply");
        userRepos.save(replyUser);
        
        final UserInformation userInfoAnnot = new UserInformation(user, Authorities.ISC);
        final UserInformation userInfoReply = new UserInformation(replyUser, Authorities.EdiT);

        // let the root annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(annotUsername);
        jsAnnot.getDocument().getMetadata().put("responseStatus", "IN_PREPARATION");
        jsAnnot.getDocument().getMetadata().put("ISCReference", ISC_REF);
        
        try {
            jsAnnot = annotService.createAnnotation(jsAnnot, userInfoAnnot);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        
        // modify the metadata: set its response status to "SENT"
        final Metadata readMeta = annotRepos.findById(jsAnnot.getId()).getMetadata();
        readMeta.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(readMeta);

        // create a reply to the root annotation (-> reference contains the root annotation's ID)
        final JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(replyUsername, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        
        // note: the below code might be reactivated in the future, when it is allowed to reply on a SENT annotation
        // at the moment, it is NOT allowed to reply to a SENT annotation; an exception is thrown in this case
        try {
            //jsAnnotReply = 
            annotService.createAnnotation(jsAnnotReply, userInfoReply);
            Assert.fail("Expected exception when replying to SENT annotation not received");
        } catch (CannotCreateAnnotationException e) {
            // OK
        }
        
        /* try {
            jsAnnotReply = annotService.createAnnotation(jsAnnotReply, userInfoReply);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertTrue(!jsAnnotReply.getId().isEmpty());

        // verification: search the annotation and the reply again based on their IDs...
        final Annotation ann = annotService.findAnnotationById(jsAnnot.getId());
        final Annotation annReply = annotService.findAnnotationById(jsAnnotReply.getId());
        Assert.assertNotNull(ann);
        
        Assert.assertNotEquals(ann.getMetadata().getId(), annReply.getMetadata().getId());
        
        // check that the annotation is anonymised from the replier's point of view, but his reply is not
        final JsonAnnotation annAsJson = annotService.convertToJsonAnnotation(ann, userInfoReply);
        final JsonAnnotation annReplyAsJson = annotService.convertToJsonAnnotation(annReply, userInfoReply);
        
        Assert.assertEquals(ISC_REF, annAsJson.getUser_info().getDisplay_name());
        Assert.assertEquals("DIGIT", annAsJson.getUser());
        Assert.assertNotEquals(ISC_REF, annReplyAsJson.getUser_info().getDisplay_name());
        Assert.assertNotEquals("DIGIT", annReplyAsJson.getUser());*/
    }
    
    /*
     *  a new top-level annotation should be created - but its metadata has response status
     *  set to "SENT" already -> creation is refused
     */    
    @Test(expected = CannotCreateAnnotationException.class)
    public void testCannotCreateIscAnnotationAlreadySent() throws CannotCreateAnnotationException {

        final String annotUsername = "acct:myusername@" + Authorities.ISC;
        final String ISC_REF = "ISC/4/8";

        final User replyUser = new User("demoReply");
        userRepos.save(replyUser);
        
        final UserInformation userInfoAnnot = new UserInformation(user, Authorities.ISC);

        // let the root annotation be created
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(annotUsername);
        jsAnnot.getDocument().getMetadata().put("responseStatus", "SENT");
        jsAnnot.getDocument().getMetadata().put("ISCReference", ISC_REF);
        
        annotService.createAnnotation(jsAnnot, userInfoAnnot);
    }
    
    /**
     * a new top-level annotation is created and two replies for it
     * -> verifies that the replies were created and are associated to the parent annotation
     */
    @Test
    public void testInsertAnnotationWithTwoReplies() {

        final String username = "acct:myusername@europa.eu";

        final UserInformation userInfo = new UserInformation(user, Authorities.EdiT);

        // let the root annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        try {
            jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertNotNull(jsAnnot);
        Assert.assertTrue(!jsAnnot.getId().isEmpty());

        // create a reply to the root annotation
        JsonAnnotation jsAnnotFirstReply = TestData.getTestReplyToAnnotation(username, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        try {
            jsAnnotFirstReply = annotService.createAnnotation(jsAnnotFirstReply, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertTrue(!jsAnnotFirstReply.getId().isEmpty());

        // create a second reply to the root annotation
        JsonAnnotation jsAnnotSecondReply = TestData.getTestReplyToAnnotation(username, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        try {
            jsAnnotSecondReply = annotService.createAnnotation(jsAnnotSecondReply, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertTrue(!jsAnnotSecondReply.getId().isEmpty());

        // verification: search the replies again based on their IDs...
        final Annotation annFirstReply = annotService.findAnnotationById(jsAnnotFirstReply.getId());
        Assert.assertNotNull(annFirstReply);
        Assert.assertEquals(jsAnnot.getId(), annFirstReply.getRootAnnotationId());
        Assert.assertTrue(annFirstReply.isReply());

        final Annotation annSecondReply = annotService.findAnnotationById(jsAnnotSecondReply.getId());
        Assert.assertNotNull(annSecondReply);
        Assert.assertEquals(jsAnnot.getId(), annSecondReply.getRootAnnotationId());
        Assert.assertTrue(annSecondReply.isReply());

        // ...and there must be three annotations in total, two of which are replies
        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(2, annotRepos.countByRootAnnotationIdNotNull());
    }

    /**
     * a new top-level annotation is created, a reply for it, and a reply to the reply
     * -> verifies that the replies were created and are associated to the parent annotation
     */
    @Test
    public void testInsertAnnotationWithReplyAndReplyReply() {

        final String username = "acct:myusername@europa.eu";

        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);

        // let the root annotation be created
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        try {
            jsAnnot = annotService.createAnnotation(jsAnnot, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertNotNull(jsAnnot);
        Assert.assertTrue(!jsAnnot.getId().isEmpty());

        // create a reply to the root annotation
        JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(username, jsAnnot.getUri(), Arrays.asList(jsAnnot.getId()));
        try {
            jsAnnotReply = annotService.createAnnotation(jsAnnotReply, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertTrue(!jsAnnotReply.getId().isEmpty());

        // create a reply to the reply
        JsonAnnotation jsAnnotReplyReply = TestData.getTestReplyToAnnotation(username, jsAnnot.getUri(),
                Arrays.asList(jsAnnot.getId(), jsAnnotReply.getId()));
        try {
            jsAnnotReplyReply = annotService.createAnnotation(jsAnnotReplyReply, userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }
        Assert.assertTrue(!jsAnnotReplyReply.getId().isEmpty());

        // verification: search the reply-reply again based on its ID...
        final Annotation ann = annotService.findAnnotationById(jsAnnotReplyReply.getId());
        Assert.assertNotNull(ann);
        Assert.assertEquals(jsAnnot.getId() + "," + jsAnnotReply.getId(), ann.getReferences()); // ancestor chain set correctly
        Assert.assertEquals(jsAnnot.getId(), ann.getRootAnnotationId()); // computed value set correctly for annotation root
        Assert.assertTrue(ann.isReply()); // reply-reply also is a reply ;-)

        // verify ordering of parent items
        Assert.assertEquals(2, ann.getReferencesList().size());
        Assert.assertEquals(jsAnnot.getId(), ann.getReferencesList().get(0)); // root
        Assert.assertEquals(jsAnnotReply.getId(), ann.getReferencesList().get(1)); // immediate parent (first degree reply)

        // ...and there must be three annotations in total, two of which are replies
        Assert.assertEquals(3, annotRepos.count());
        Assert.assertEquals(2, annotRepos.countByRootAnnotationIdNotNull());
    }

    /**
     * a reply for an annotation is created, but the referenced top-level annotation is missing
     * -> verifies that the reply was not created
     */
    @Test
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void testInsertReplyWithoutExistingParent() throws Exception {

        final String username = "acct:myusername@europa.eu";
        final URI uri = new URI("http://leos/document?id=3");

        // create a reply to the root annotation
        // -> reference contains the root annotation's ID, which does not exist
        // as the 'root' property is a foreign key on the annotation ID, this is a foreign key violation
        final JsonAnnotation jsAnnotReply = TestData.getTestReplyToAnnotation(username, uri, Arrays.asList(uuidGeneratorService.generateUrlSafeUUID()));

        try {
            annotService.createAnnotation(jsAnnotReply, new UserInformation(user, "authority"));
            Assert.fail("Inserting a reply to a non-existing annotation should fail, but it did not!");
        } catch (CannotCreateAnnotationException e) {
            // OK
        } catch (Exception e) {
            Assert.fail(UNEXPECTED_EXCEPTION + e);
        }

        // ...and there must not be any annotation nor tag
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, tagRepos.count());
    }

}
