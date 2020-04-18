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
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationStatus;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.*;
import eu.europa.ec.leos.annotate.services.exceptions.CannotAcceptSuggestionException;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
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
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * try to accept a suggestion, but repository throws exception -> appropriate exception should be given
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class SuggestionAcceptWithMockReposTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationServiceImpl annotService; // mocked AnnotationRepository is injected

    @Mock
    private AnnotationTestRepository annotRepos;

    @SuppressWarnings({"PMD.UnusedPrivateField"})
    @Mock
    private DocumentRepository documentRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Mock
    private AnnotationConversionService convService;

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

    @Mock
    private TagsService tagsService;

    @Mock
    private AnnotationPermissionService annotPermService;

    @Autowired
    private UserDetailsCache userDetailsCache;

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

        userDetailsCache.clear();
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    /**
     * a suggestion should be accepted, but repository throws exception
     * -> should throw expected exception
     */
    @Test(expected = CannotAcceptSuggestionException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testAcceptSimpleSuggestionWithException() throws Exception {

        final String authority = Authorities.ISC;

        // retrieve our test annotation and make it become a suggestion
        final JsonAnnotation jsAnnot = TestData.getTestSuggestionObject(HYPO_USER_ACCOUNT);
        final UserInformation userInfo = new UserInformation(theUser, authority);
        final UserDetails userDetails = new UserDetails(theUser.getLogin(), theUser.getId(), "a", "b",
                Arrays.asList(new UserEntity("4", "COMP", "COMP")), "", null);
        userDetailsCache.cache(theUser.getLogin(), userDetails);

        Mockito.when(userService.findByLogin(LOGIN)).thenReturn(theUser);
        Mockito.when(groupService.findGroupByName("__world__")).thenReturn(defaultGroup);
        Mockito.when(documentService.findDocumentByUri(new URI("https://a.com"))).thenReturn(new Document());
        Mockito.when(uuidService.generateUrlSafeUUID()).thenReturn("1234");

        final Annotation ann = new Annotation();
        ann.setId("1");
        ann.setUpdated(LocalDateTime.now());
        ann.setStatus(AnnotationStatus.NORMAL);
        ann.setTags(Arrays.asList(new Tag("suggestion", ann)));

        final JsonAnnotationStatus jsonStatus = new JsonAnnotationStatus();
        jsonStatus.setStatus(AnnotationStatus.NORMAL);

        // first, repository should pretend to save, but second call should fail with exception
        Mockito.when(annotRepos.save(Mockito.any(Annotation.class))).thenReturn(ann).thenThrow(new RuntimeException());
        Mockito.when(convService.getJsonAnnotationStatus(ann)).thenReturn(jsonStatus);

        annotService.createAnnotation(jsAnnot, userInfo);

        Mockito.when(annotService.findAnnotationById("1")).thenReturn(ann);
        Mockito.when(tagsService.hasSuggestionTag(Mockito.anyListOf(Tag.class))).thenReturn(true);
        Mockito.when(annotPermService.hasUserPermissionToAcceptSuggestion(Mockito.any(Annotation.class), Mockito.any(User.class))).thenReturn(true);

        // accept it - should fail as repository throws an exception upon saving the annotation
        annotService.acceptSuggestionById(jsAnnot.getId(), userInfo);
    }
}
