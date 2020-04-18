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
import eu.europa.ec.leos.annotate.model.PublishContributionsResult;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.PublishContributionsRequest;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.StatusUpdateService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotPublishContributionsException;
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
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class PublishContributionsTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

    @Autowired
    private StatusUpdateService statusService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private TokenRepository tokenRepos;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    @Test
    public void testPublish1()
            throws CannotCreateAnnotationException, CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final String LOGIN = "mylogin";
        final String ISCRef = "ISC/2019/4";
        final String GROUPNAME = "SG";
        final User user = new User(LOGIN);
        userRepos.save(user);

        JsonAnnotation annot = TestData.getTestPrivateAnnotationObject("acct:user@" + Authorities.ISC);
        annot.setGroup(GROUPNAME);

        final Group group = new Group(GROUPNAME, true);
        groupRepos.save(group);
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));

        // create first annotation that should be published
        final SimpleMetadata meta = annot.getDocument().getMetadata();
        meta.put("ISCReference", ISCRef);
        meta.put("responseVersion", "1");
        meta.put("responseId", "SG");
        meta.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        annot.getDocument().setMetadata(meta);

        final Token token = new Token(user, Authorities.ISC, "a", LocalDateTime.now().plusMinutes(5), "r", LocalDateTime.now().plusMinutes(5));
        tokenRepos.save(token);
        final UserInformation userInfo = new UserInformation(token);

        annot = annotService.createAnnotation(annot, userInfo);

        // create second annotation that should be published
        JsonAnnotation annot2 = TestData.getTestPrivateAnnotationObject("acct:user@" + Authorities.ISC);
        annot2.setGroup(GROUPNAME);
        annot2.getDocument().setMetadata(meta);
        annot2 = annotService.createAnnotation(annot2, userInfo);

        // create another annotation having the same metadata, but not being private
        JsonAnnotation annot3 = TestData.getTestAnnotationObject("acct:user@" + Authorities.ISC);
        annot3.setGroup(GROUPNAME);
        annot3.getDocument().setMetadata(meta);
        annot3 = annotService.createAnnotation(annot3, userInfo);

        final PublishContributionsRequest publishRequest = new PublishContributionsRequest(
                annot.getDocument().getLink().get(0).getHref().toString(), annot.getGroup(), user.getLogin(), ISCRef);
        final PublishContributionsResult publishResult = statusService.publishContributions(publishRequest, userInfo);

        Assert.assertNotNull(publishResult);
        Assert.assertEquals(2, publishResult.getUpdatedAnnotIds().size());
        assertIdContained(publishResult.getUpdatedAnnotIds(), annot.getId());
        assertIdContained(publishResult.getUpdatedAnnotIds(), annot2.getId());

        // read the annotations from the database again
        final Annotation annotDb1 = annotRepos.findById(annot.getId());
        final Annotation annotDb2 = annotRepos.findById(annot2.getId());
        final Annotation annotDb3 = annotRepos.findById(annot3.getId());

        // there should be a new metadata entry in the db...
        Assert.assertEquals(2, metadataRepos.count());

        // ... which is assigned to the two annotations being published and should have the new originMode
        final Metadata meta1 = metadataRepos.findOne(annotDb1.getMetadataId());
        Assert.assertEquals("private", meta1.getKeyValuePropertyAsSimpleMetadata().get("originMode"));

        Assert.assertEquals(meta1.getId(), annotDb2.getMetadataId());

        // and there is another entry (the original one), which does not have the originMode entry
        final Metadata meta3 = metadataRepos.findOne(annotDb3.getMetadataId());
        Assert.assertNull(meta3.getKeyValuePropertyAsSimpleMetadata().get("originMode"));
    }

    // verify that not giving a publishing request throws an exception
    @Test(expected = CannotPublishContributionsException.class)
    public void testPublish_noPublishRequest() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        statusService.publishContributions(null, null);
    }

    // verify that not giving a valid user throws an exception
    @Test(expected = IllegalArgumentException.class)
    public void testPublish_noUserinfo() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final PublishContributionsRequest request = new PublishContributionsRequest();
        statusService.publishContributions(request, null);
    }

    // verify that giving an EdiT user throws an exception
    @Test(expected = MissingPermissionException.class)
    public void testPublish_EditUser() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final PublishContributionsRequest request = new PublishContributionsRequest();
        final UserInformation userInfo = new UserInformation("me", Authorities.EdiT);

        statusService.publishContributions(request, userInfo);
    }

    // verify that an exception is thrown when no matching metadata is found
    @Test(expected = CannotPublishContributionsException.class)
    public void testPublish_noMatchingMetadata() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final PublishContributionsRequest request = new PublishContributionsRequest();
        final UserInformation userInfo = new UserInformation("me2", Authorities.ISC);

        statusService.publishContributions(request, userInfo);
    }

    // verify that an exception is thrown when no matching metadata having the desired ISC reference is found
    @Test(expected = CannotPublishContributionsException.class)
    public void testPublish_noMatchingIscReference() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final Document doc = new Document(URI.create("http://some.url"), "some title");
        documentRepos.save(doc);

        final Group group = new Group("secretgroup", true);
        groupRepos.save(group);

        final Metadata meta = new Metadata(doc, group, Authorities.ISC);
        meta.setKeyValuePropertyFromSimpleMetadata(new SimpleMetadata("ISCRef", "4"));
        metadataRepos.save(meta);

        // define the publishing request - but the saved metadata does not have the same ISC reference
        final UserInformation userInfo = new UserInformation("me2", Authorities.ISC);
        final PublishContributionsRequest request = new PublishContributionsRequest(doc.getUri(), group.getName(), userInfo.getLogin(), "ISC/1");

        statusService.publishContributions(request, userInfo);
    }

    // verify that an exception is thrown when matching metadata is found, but no annotations assigned to it
    @Test(expected = CannotPublishContributionsException.class)
    public void testPublish_noAnnotations() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final String IscRef = "ISC8";

        final Document doc = new Document(URI.create("http://some2.url"), "some2 title");
        documentRepos.save(doc);

        final Group group = new Group("secretgroup2", true);
        groupRepos.save(group);

        final User user = new User("its_me");
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));

        final Metadata meta = new Metadata(doc, group, Authorities.ISC);
        meta.setKeyValuePropertyFromSimpleMetadata(new SimpleMetadata(Metadata.PROP_ISC_REF, IscRef));
        metadataRepos.save(meta);

        // define the publishing request - but the saved metadata does not have the same ISC reference
        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);
        final PublishContributionsRequest request = new PublishContributionsRequest(doc.getUri(), group.getName(), userInfo.getLogin(), IscRef);

        statusService.publishContributions(request, userInfo);
    }

    // verify that no exception is thrown when only public annotations are found, but result is empty
    @Test
    public void testPublish_noPrivateAnnotations() throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final String IscRef = "ISC8";

        final Document doc = new Document(URI.create("http://some3.url"), "some3 title");
        documentRepos.save(doc);

        final Group group = new Group("secretgroup3", true);
        groupRepos.save(group);

        final User user = new User("its_me2");
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));

        final Token token = new Token(user, Authorities.ISC, "ac", LocalDateTime.now(), "re", LocalDateTime.now());
        tokenRepos.save(token);

        final Metadata meta = new Metadata(doc, group, Authorities.ISC);
        meta.setKeyValuePropertyFromSimpleMetadata(new SimpleMetadata(Metadata.PROP_ISC_REF, IscRef));
        meta.setResponseStatus(Metadata.ResponseStatus.IN_PREPARATION);
        metadataRepos.save(meta);

        final Annotation annot = new Annotation();
        annot.setId("an1");
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setText("text");
        annot.setUser(user);
        annot.setMetadata(meta);
        annot.setTargetSelectors("a");
        annot.setShared(true);
        annotRepos.save(annot);

        // define the publishing request - but the saved metadata does not have the same ISC reference
        final UserInformation userInfo = new UserInformation(token);
        final PublishContributionsRequest request = new PublishContributionsRequest(doc.getUri(), group.getName(), userInfo.getLogin(), IscRef);

        final PublishContributionsResult result = statusService.publishContributions(request, userInfo);
        Assert.assertNotNull(result);
        Assert.assertTrue(CollectionUtils.isEmpty(result.getUpdatedAnnotIds()));
    }

    // verify the result when a private annotation is found and published; verify that no additional metadata is created when all annotations are assigned
    // to a single metadata (which is updated in this case)
    @Test
    public void testPublish_allAnnotationsToSameMetadata()
            throws CannotPublishContributionsException, MissingPermissionException, CannotCreateMetadataException {

        final String IscRef = "ISC4";

        final Document doc = new Document(URI.create("http://some4.url"), "some4 title");
        documentRepos.save(doc);

        final Group group = new Group("secretgroup4", true);
        groupRepos.save(group);

        final User user = new User("its_me3");
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));

        final Token token = new Token(user, Authorities.ISC, "ac", LocalDateTime.now(), "re", LocalDateTime.now());
        tokenRepos.save(token);

        final Metadata meta = new Metadata(doc, group, Authorities.ISC);
        meta.setKeyValuePropertyFromSimpleMetadata(new SimpleMetadata(Metadata.PROP_ISC_REF, IscRef));
        meta.setResponseStatus(Metadata.ResponseStatus.IN_PREPARATION);
        metadataRepos.save(meta);

        final Annotation annot = new Annotation();
        annot.setId("an1");
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setText("text");
        annot.setUser(user);
        annot.setMetadata(meta);
        annot.setTargetSelectors("a");
        annot.setShared(false); // -> will be published
        annotRepos.save(annot);

        // define the publishing request - but the saved metadata does not have the same ISC reference
        final UserInformation userInfo = new UserInformation(token);
        final PublishContributionsRequest request = new PublishContributionsRequest(doc.getUri(), group.getName(), userInfo.getLogin(), IscRef);

        final PublishContributionsResult result = statusService.publishContributions(request, userInfo);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.getUpdatedAnnotIds().size());

        // no additional entry was created; and this entry has the "originMode" set
        Assert.assertEquals(1, metadataRepos.count());
        Assert.assertEquals("private", ((List<Metadata>) metadataRepos.findAll()).get(0).getAllMetadataAsSimpleMetadata().get("originMode"));
    }

    @Test
    public void testMakeShared_noItems() throws CannotCreateAnnotationException {
        
        final Document doc = new Document(URI.create("http://some5.url"), "some5 title");
        documentRepos.save(doc);

        final Group group = new Group("secretgroup5", true);
        groupRepos.save(group);

        final User user = new User("its_me4");
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));
        
        final Metadata meta = new Metadata(doc, group, Authorities.ISC);
        metadataRepos.save(meta);

        final Annotation annot = new Annotation();
        annot.setId("annot_xy");
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setText("text");
        annot.setUser(user);
        annot.setMetadata(meta);
        annot.setTargetSelectors("ab");
        annot.setShared(false);
        annotRepos.save(annot);
        
        // try making them public
        annotService.makeShared(null);
        
        // check that nothing was changed in the repository
        final List<Annotation> allAnnots = (List<Annotation>) annotRepos.findAll();
        Assert.assertFalse(allAnnots.get(0).isShared());
    }
    
    @Test
    public void testMakeShared_OneItem() throws CannotCreateAnnotationException {
        
        final Document doc = new Document(URI.create("http://some6.url"), "some6 title");
        documentRepos.save(doc);

        final Group group = new Group("secretgroup6", true);
        groupRepos.save(group);

        final User user = new User("its_me5");
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));
        
        final Metadata meta = new Metadata(doc, group, Authorities.ISC);
        metadataRepos.save(meta);

        final Annotation annot = new Annotation();
        annot.setId("annot_xy");
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setText("text2");
        annot.setUser(user);
        annot.setMetadata(meta);
        annot.setTargetSelectors("abc");
        annot.setShared(false);
        annotRepos.save(annot);
        
        // try making them public
        annotService.makeShared(Arrays.asList(annot));
        
        // check that the single item was changed in the repository
        final List<Annotation> allAnnots = (List<Annotation>) annotRepos.findAll();
        Assert.assertEquals(1, allAnnots.size());
        Assert.assertTrue(allAnnots.get(0).isShared());
    }
    
    private void assertIdContained(final List<String> annots, final String idToCheck) {

        Assert.assertTrue(annots.contains(idToCheck));
    }
}
