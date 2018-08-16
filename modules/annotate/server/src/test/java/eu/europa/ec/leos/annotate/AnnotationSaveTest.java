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

import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.TagsService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationSaveTest {

    @SuppressWarnings("unused")
    private static Logger LOG = LoggerFactory.getLogger(AnnotationSaveTest.class);
    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
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
    private DocumentRepository documentRepos;

    @Autowired
    private AnnotationRepository annotRepos;

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

        Document d = new Document();
        d.setTitle("title");
        d.setUri("http://www.a.com");
        documentRepos.save(d);

        User u = new User();
        u.setLogin("login");
        userRepos.save(u);

        Group g = new Group();
        g.setName("groupname");
        g.setDisplayName("display");
        g.setDescription("description");
        groupRepos.save(g);

        Tag t = new Tag();
        t.setName("thetag");

        Metadata m = new Metadata(d, g, "sys");
        metadataRepos.save(m);

        Annotation a = new Annotation();
        a.setCreated(LocalDateTime.now());
        a.setUpdated(LocalDateTime.now());
        a.setId("theid");
        a.setUser(u);
        a.setGroup(g);
        a.setTargetSelectors("a");
        a.setDocument(d);
        a.setMetadata(m);

        t.setAnnotation(a);
        a.getTags().add(t);

        annotRepos.save(a);

        Assert.assertEquals(1, annotRepos.count());

        Annotation foundAnnotation = annotRepos.findById("theid");
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
    @Test
    public void testSaveAnnotationWithService() throws URISyntaxException {

        final String username = "acct:myusername@domain.com", login = "demo";

        // add user to default group
        User theUser = userRepos.save(new User("demo"));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        // save the annotation using the annotation service
        JsonAnnotation returnAnnot = null;
        try {
            returnAnnot = annotService.createAnnotation(TestData.getTestAnnotationObject(username), login);
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
        try {
            readAnnotation = annotService.findAnnotationById(returnAnnot.getId(), login);
        } catch (MissingPermissionException e) {
            Assert.fail("Unexpected exception received: " + e);
        }
        Assert.assertNotNull(readAnnotation);
        Assert.assertEquals(1, documentRepos.count());
        Assert.assertEquals(1, userRepos.count());
        Assert.assertEquals(1, groupRepos.count());
        Assert.assertEquals(2, tagRepos.count());
        Assert.assertEquals(1, metadataRepos.count());
    }

    // test creation of an annotation when no metadata is given - default entry should be created using configured default systemId
    @Test
    public void testCreateAnnotationWithoutMetadata() throws CannotCreateAnnotationException {

        final String username = "acct:myusername@europa.eu", login = "demo";

        // add user to default group
        User theUser = userRepos.save(new User("demo"));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        // let the annotation be created, but without providing any metadata
        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        jsAnnot.getDocument().setMetadata(null);

        annotService.createAnnotation(jsAnnot, login);
        
        // verify the annotation and a metadata record were saved
        Assert.assertEquals(1, annotRepos.count());
        Assert.assertEquals(1, metadataRepos.count());

        // verify that some system Id was supplied
        List<Metadata> allMetas = (List<Metadata>) metadataRepos.findAll();
        Metadata singleMeta = allMetas.get(0);
        Assert.assertTrue(!StringUtils.isEmpty(singleMeta.getSystemId()));
    }

    /**
     * tests saving an annotation fails when no user is specified
     */
    @Test
    public void testCannotCreateAnnotationWithoutUser() throws URISyntaxException {

        final String username = "acct:myusername@domain.com";

        // save the annotation, but let user's login be missing
        try {
            annotService.createAnnotation(TestData.getTestAnnotationObject(username), "");
            Assert.fail("Expected exception about missing user not received");
        } catch (CannotCreateAnnotationException e) {
            // OK
        }

        Assert.assertEquals(0, annotRepos.count());
    }
}
