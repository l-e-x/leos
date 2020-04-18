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
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteAnnotationException;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class AnnotationDeleteWithMockTest {

    /**
     * test that expected exceptions are thrown when internal errors occur
     */

    private Group defaultGroup;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationServiceImpl annotService; // mocked AnnotationRepository is injected

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Mock
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    @Qualifier("annotationRepos")
    private AnnotationRepository realAnnotRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test deletion of an annotation when annotation repository has internal failures - annotation deletion should throw expected exception
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test(expected = CannotDeleteAnnotationException.class)
    public void testDeleteAnnotation_ReposFailure() throws Exception {

        final String login = "demo";

        // add user to default group
        final User theUser = userRepos.save(new User(login));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        final UserInformation userInfo = new UserInformation(login, Authorities.EdiT);
        
        // save simple annotation
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

        Annotation annot = new Annotation();
        annot.setCreated(LocalDateTime.now());
        annot.setUpdated(LocalDateTime.now());
        annot.setId("theid");
        annot.setUser(user);
        annot.setTargetSelectors("a");
        annot.setMetadata(meta);

        tag.setAnnotation(annot);
        annot.getTags().add(tag);

        annot = realAnnotRepos.save(annot);

        Mockito.when(annotRepos.findById(annot.getId())).thenReturn(annot);
        Mockito.doThrow(new RuntimeException()).when(annotRepos).delete(Mockito.any(Annotation.class));

        // try to delete the annotation - should not be successful
        annotService.deleteAnnotationById(annot.getId(), userInfo);
    }

}
