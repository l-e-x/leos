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
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.DocumentRepository;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.DocumentService;
import eu.europa.ec.leos.annotate.services.GroupService;
import eu.europa.ec.leos.annotate.services.MetadataService;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateDocumentException;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
import org.junit.After;
import org.junit.Assert;
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

import java.net.URI;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationSaveWithDocumentMockTest {

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
    private AnnotationServiceImpl annotService; // mocked MetadataService is injected

    @Mock
    private DocumentRepository documentRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Mock
    private MetadataService metadataService;

    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private DocumentService documentService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test creation of an annotation when document service has internal failures - annotation creation should throw expected exception
    @Test
    public void testCreateAnnotation_DocumentService_Failure() throws CannotCreateAnnotationException, CannotCreateDocumentException {

        final String username = "acct:myusername@europa.eu", login = "demo";

        // add user to default group
        User theUser = userRepos.save(new User("demo"));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));

        JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(username);
        jsAnnot.setDocument(null); // no document information available

        // configure mocks; especially DocumentService should throw a custom exception
        Mockito.when(userService.findByLogin(Mockito.anyString())).thenReturn(theUser);
        Mockito.when(groupService.findGroupByName(Mockito.anyString())).thenReturn(defaultGroup);
        Mockito.when(documentService.findDocumentByUri(Mockito.any(URI.class))).thenReturn(null);
        Mockito.when(documentService.createNewDocument(Mockito.any(URI.class))).thenThrow(new CannotCreateDocumentException(new RuntimeException()));

        // let the annotation be created - should not be successful
        try {
            annotService.createAnnotation(jsAnnot, login);
            Assert.fail("Expected exception due to document service failure not received");
        } catch (CannotCreateAnnotationException ccae) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception");
        }

    }
}
