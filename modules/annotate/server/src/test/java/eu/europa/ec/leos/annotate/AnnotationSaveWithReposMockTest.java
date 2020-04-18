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
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.*;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class AnnotationSaveWithReposMockTest {
    
    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationServiceImpl annotService; // mocked AnnotationRepository is injected

    @Mock
    private AnnotationTestRepository annotRepos;

    @SuppressWarnings({"PMD.UnusedPrivateField", "PMD.AvoidDuplicateLiterals"})
    @Mock
    private DocumentRepository documentRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @SuppressWarnings({"PMD.UnusedPrivateField"})
    @Mock
    private MetadataService metadataService;

    @SuppressWarnings({"PMD.UnusedPrivateField"})
    @Mock
    private MetadataMatchingService metadataMatchingService;
    
    @Mock
    private UserService userService;

    @Mock
    private GroupService groupService;

    @Mock
    private DocumentService documentService;

    @Mock
    private UUIDGeneratorService uuidService;

    @SuppressWarnings({"PMD.UnusedPrivateField"})
    @Mock
    private TagsService tagsService;

    private Group defaultGroup;
    private User theUser;
    private static final String LOGIN = "demo";
    private static final String HYPO_USER_ACCOUNT = "acct:" + LOGIN + "@" + Authorities.ISC;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        MockitoAnnotations.initMocks(this);

        theUser = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    /**
     * a suggestion is created, but repository throws exception
     * -> should produce an expected exception
     */
    @Test(expected = CannotCreateAnnotationException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testAcceptSimpleSuggestion() throws Exception {

        final String authority = Authorities.ISC;

        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        final UserInformation userInfo = new UserInformation(theUser, authority);

        Mockito.when(userService.findByLogin(LOGIN)).thenReturn(theUser);
        Mockito.when(groupService.findGroupByName("__world__")).thenReturn(defaultGroup);
        Mockito.when(documentService.findDocumentByUri(new URI("https://a.com"))).thenReturn(new Document());
        Mockito.when(uuidService.generateUrlSafeUUID()).thenReturn("1234");

        Mockito.when(annotRepos.save(Mockito.any(Annotation.class))).thenThrow(new RuntimeException());

        // will fail due to repository's exception
        annotService.createAnnotation(jsAnnot, userInfo);
    }

}
