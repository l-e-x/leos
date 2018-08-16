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

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.DocumentService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteDocumentException;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationDeleteWithMockDocumentServiceTest {

    /**
     * test that expected exceptions are thrown when internal errors occur
     */

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

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationServiceImpl annotService; // mocked DocumentService and AnnotationRepository are injected

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Mock
    private AnnotationRepository annotRepos;

    @Autowired
    private AnnotationRepository realAnnotRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Mock
    private DocumentService documentService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test deletion of an annotation when annotation repository has internal failures - annotation deletion should throw expected exception
    @Test
    public void testDeleteAnnotation_DocumentServiceFailure()
            throws CannotDeleteDocumentException, CannotDeleteAnnotationException {

        final String login = "demo";

        // add user to default group
        User theUser = userRepos.save(new User(login));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        // save simple annotation
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

        a = realAnnotRepos.save(a);

        Mockito.when(annotRepos.findById(Mockito.anyString())).thenReturn(a);
        Mockito.doThrow(new CannotDeleteDocumentException(new RuntimeException())) // first our custom exception
                .doThrow(new RuntimeException()) // then generic exception
                .when(documentService).deleteDocument(Mockito.any(Document.class));

        // try to delete the annotation - should be successful, the internal exception of documentService is not propagated!
        annotService.deleteAnnotationById(a.getId(), login);

        // modify annotation and save again
        a = realAnnotRepos.save(a);

        // try deleting once more - this time, the internal DocumentService throws another exception, which again is not propagated
        annotService.deleteAnnotationById(a.getId(), login);
    }

}
