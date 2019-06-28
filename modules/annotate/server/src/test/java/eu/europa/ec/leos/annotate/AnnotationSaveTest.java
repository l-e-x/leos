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
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.TagsService;
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
import org.springframework.util.StringUtils;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class AnnotationSaveTest {

    private Group defaultGroup;
    private static final String LOGIN = "demo";
    private static final String PREFIX = "acct:";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private TagRepository tagRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private TagsService tagService;

    // -------------------------------------
    // Cleanup of database content before running new test
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
     * tests saving an incoming annotation to the database using hibernate
     * -> checks that related objects are persisted into their proper repository by re-reading object from DB 
     *    (coarsely only; we do not want to test hibernate framework as such, just principle setup of our model) 
     * 
     * @throws URISyntaxException
     */
    @Test
    public void testSaveAnnotationWithRepository() {

        final Document doc = new Document();
        doc.setTitle("title");
        doc.setUri("http://www.a.com");
        documentRepos.save(doc);

        final User user = new User();
        user.setLogin("login");
        userRepos.save(user);

        final Group group = new Group();
        group.setName("groupname");
        group.setDisplayName("display");
        group.setDescription("description");
        groupRepos.save(group);

        final Tag tag = new Tag();
        tag.setName("thetag");

        final Metadata meta = new Metadata(doc, group, "sys");
        metadataRepos.save(meta);

        final Annotation annot = new Annotation();
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setId("theid");
        annot.setUser(user);
        annot.setTargetSelectors("a");
        annot.setMetadata(meta);

        tag.setAnnotation(annot);
        annot.getTags().add(tag);

        annotRepos.save(annot);

        Assert.assertEquals(1, annotRepos.count());

        final Annotation foundAnnotation = annotRepos.findById("theid");
        Assert.assertNotNull(foundAnnotation);
        Assert.assertNotNull(foundAnnotation.getDocument());
        Assert.assertNotNull(foundAnnotation.getUser());
        Assert.assertNotNull(foundAnnotation.getGroup());
        Assert.assertNotNull(foundAnnotation.getTags());
        Assert.assertEquals(1, foundAnnotation.getTags().size());

        // now we delete the tag again (only the tag) using our custom delete function
        // (hibernate does not do it due to the way our model is configured; the Annotation is the master for the Annotation-Tag relation)
        tagService.removeTags(foundAnnotation.getTags());

        Assert.assertEquals(0, tagRepos.count());
        Assert.assertEquals(1, annotRepos.count()); // the annotation was not deleted, only its tag
    }

    /**
     * tests saving an incoming annotation to the database using the annotation service
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testSaveAnnotationWithService() throws Exception {

        final String username = "acct:myusername@domain";
        final String authority = "domain";

        // add user to default group
        final User theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        final UserInformation userInfo = new UserInformation(theUser, authority);

        // save the annotation using the annotation service
        JsonAnnotation returnAnnot = null;
        try {
            returnAnnot = annotService.createAnnotation(TestData.getTestAnnotationObject(username), userInfo);
        } catch (CannotCreateAnnotationException e) {
            Assert.fail("Unexpected exception received: " + e);
        }

        // verify properties that should have been filled
        Assert.assertNotNull(returnAnnot);
        Assert.assertFalse(returnAnnot.getId().isEmpty());
        Assert.assertNotNull(returnAnnot.getUpdated());

        Assert.assertEquals(1, annotRepos.count());

        // verify that dependent objects are saved
        Annotation readAnnotation = null;
        readAnnotation = annotService.findAnnotationById(returnAnnot.getId(), LOGIN);

        Assert.assertNotNull(readAnnotation);
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, userRepos.count());
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(2, tagRepos.count());
        Assert.assertEquals(1, metadataRepos.count());
    }

    // test creation of an annotation when no metadata is given - default entry should be created using configured default systemId
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testCreateAnnotationWithoutMetadata() throws CannotCreateAnnotationException {

        final String authority = Authorities.ISC;
        final String username = PREFIX + LOGIN + "@" + authority;

        // add user to default group
        final User theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        final UserInformation userInfo = new UserInformation(theUser, authority);

        // let the annotation be created, but without providing any metadata
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        jsAnnot.getDocument().setMetadata(null);

        annotService.createAnnotation(jsAnnot, userInfo);

        // verify the annotation and a metadata record were saved
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, metadataRepos.count());

        // verify that some system Id was supplied, namely the user's authority
        final List<Metadata> allMetas = (List<Metadata>) metadataRepos.findAll();
        final Metadata singleMeta = allMetas.get(0);
        Assert.assertTrue(!StringUtils.isEmpty(singleMeta.getSystemId()));
        Assert.assertEquals(authority, singleMeta.getSystemId());
    }

    // test creation of an annotation when given metadata has response status set to SENT - should be refused
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testCannotCreateAnnotationWithSentResponseStatus() throws CannotCreateAnnotationException {

        final String authority = Authorities.ISC;
        final String username = PREFIX + LOGIN + "@" + authority;

        // add user to default group
        final User theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        final UserInformation userInfo = new UserInformation(theUser, authority);

        // let the annotation be created, but without providing any metadata
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        final SimpleMetadata metadata = new SimpleMetadata();
        metadata.put("responseStatus", "SENT");
        jsAnnot.getDocument().setMetadata(metadata);

        try {
            annotService.createAnnotation(jsAnnot, userInfo);
            Assert.fail("Did not receive expected exception");
        } catch (CannotCreateAnnotationException ccae) {
            // OK
        }

        // verify that no annotation was created
        Assert.assertEquals(0, annotRepos.count());
        Assert.assertEquals(0, metadataRepos.count());
    }

    // test creation of an annotation when metadata is given, but systemId is missing - systemId from user token should be used
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testCreateAnnotationWithoutSystemIdMetadata() throws CannotCreateAnnotationException {

        final String authority = Authorities.ISC;
        final String username = PREFIX + LOGIN + "@" + authority;

        // add user to default group
        final User theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        final UserInformation userInfo = new UserInformation(theUser, authority);

        // let the annotation be created, but without providing any metadata
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        jsAnnot.getDocument().getMetadata().remove("systemId");

        annotService.createAnnotation(jsAnnot, userInfo);

        // verify the annotation and a metadata record were saved
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, metadataRepos.count());

        // verify that some system Id was supplied, namely the user's authority
        final List<Metadata> allMetas = (List<Metadata>) metadataRepos.findAll();
        final Metadata singleMeta = allMetas.get(0);
        Assert.assertTrue(!StringUtils.isEmpty(singleMeta.getSystemId()));
        Assert.assertEquals(authority, singleMeta.getSystemId());
    }

    /**
     * tests saving an annotation fails when no user is specified
     */
    @Test
    public void testCannotCreateAnnotationWithoutUser() throws URISyntaxException {

        final String username = "acct:myusername@domain.com";

        // save the annotation, but let user's login be missing
        try {
            annotService.createAnnotation(TestData.getTestAnnotationObject(username), null);
            Assert.fail("Expected exception about missing user not received");
        } catch (CannotCreateAnnotationException e) {
            // OK
        }

        Assert.assertEquals(0, annotRepos.count());
    }

    /**
     * test that different metadata sets are created and assigned to annotations if different requests were sent
     */
    @Test
    public void testCreatingAnnotationsWithDifferentMetadata() throws Exception {

        final String login = "somebody";
        final String authority = Authorities.EdiT;
        final String username = PREFIX + login + "@" + authority;

        // add user to default group
        final User theUser = userRepos.save(new User(login));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
        final UserInformation userInfo = new UserInformation(theUser, authority);

        // let the annotation be created
        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        jsAnnot.getDocument().getMetadata().put("someprop", "someval");

        annotService.createAnnotation(jsAnnot, userInfo);

        // verify the annotation and a metadata record were saved
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, metadataRepos.count());

        // create a second annotation with different metadata, but for same document/group/system
        // -> new metadata set should be created
        final JsonAnnotation jsAnnotSecond = TestData.getTestAnnotationObject(username);
        jsAnnotSecond.getDocument().getMetadata().put("someprop", "someval");
        jsAnnotSecond.getDocument().getMetadata().put("anotherprop", "anotherval");// additional property, which was not set for first annotation

        annotService.createAnnotation(jsAnnotSecond, userInfo);

        // verify that two annotations and metadata are present, and but annotations are assigned to different metadata sets
        Assert.assertEquals(2, annotRepos.count());
        Assert.assertEquals(2, metadataRepos.count());

        final long firstMetaId = annotRepos.findById(jsAnnot.getId()).getMetadata().getId();
        final long secondMetaId = annotRepos.findById(jsAnnotSecond.getId()).getMetadata().getId();
        Assert.assertNotEquals(firstMetaId, secondMetaId);
    }
}
