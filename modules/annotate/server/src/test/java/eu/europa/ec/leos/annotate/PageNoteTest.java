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
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationTargets;
import eu.europa.ec.leos.annotate.repository.AnnotationTestRepository;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationConversionService;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class PageNoteTest {

    /**
     * This test class contains tests for creating page notes (=annotations without targets)
     */

    private User user;
    private static final String LOGIN = "demo";

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

    @Autowired
    private AnnotationConversionService conversionService;

    @Autowired
    private AnnotationTestRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        user = userRepos.save(new User(LOGIN));
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * simple creation of a page note
     * notes: implicitly also tests retrieval of an existing page note
     */
    @Test
    public void testCreatePageNote() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net";
        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);

        // create a page note
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTarget(null);
        annotService.createAnnotation(annot, userInfo);

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals("null", readAnnot.getTargetSelectors()); // verify that target was null (serialized as string)

        // target list is just one item with source URI
        final JsonAnnotation jsReply = conversionService.convertToJsonAnnotation(readAnnot, userInfo);
        Assert.assertNotNull(jsReply);
        Assert.assertNotNull(jsReply.getTarget());
        Assert.assertEquals(1, jsReply.getTarget().size());

        // target only contains source URI
        final JsonAnnotationTargets target = jsReply.getTarget().get(0);
        Assert.assertNull(target.getSelector());
        Assert.assertNotNull(target.getSource());
    }

    /**
     * update of an existing page note, e.g. by adding text
     */
    @Test
    public void testUpdatePageNote()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net";
        final String updatedText = "text added now";

        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);

        // create a highlight
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTarget(null);
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // update: add text
        annot.setText(updatedText);
        annotService.updateAnnotation(annotId, annot, userInfo);

        // read annotation from database and verify that target remains "null"
        final Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals("null", readAnnot.getTargetSelectors());
    }

    /**
     * updating an existing page note
     * note: permission checks have already been tested for annotations
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testDeletePageNote()
            throws CannotCreateAnnotationException, CannotDeleteAnnotationException,
            MissingPermissionException, CannotDeleteSentAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net";

        // create a highlight
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTarget(null);

        final UserInformation userInfo = new UserInformation(user, Authorities.EdiT);
        annotService.createAnnotation(annot, userInfo);

        // delete the annotation
        annotService.deleteAnnotationById(annot.getId(), userInfo);

        // verify that it was deleted
        final Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNull(readAnnot);
        TestHelper.assertHasStatus(annotRepos, annot.getId(), AnnotationStatus.DELETED, user.getId());
    }

    /**
     * search for existing page notes
     */
    @Test
    public void testSearchPageNote() throws CannotCreateAnnotationException {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = "acct:user@" + authority;

        final UserInformation userInfo = new UserInformation(
                new Token(user, authority, "acc", LocalDateTime.now().plusMinutes(5), "ref", LocalDateTime.now().plusMinutes(5)));

        // create a page note
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setTarget(null);
        annotService.createAnnotation(annot, userInfo);

        // delete the annotation
        final AnnotationSearchOptions options = new AnnotationSearchOptions(annot.getUri().toString(), "__world__", false, 200, 0, "asc", "created");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);

        // verify: there is only the page note
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(annot.getId(), result.getItems().get(0).getId());
        Assert.assertEquals("null", result.getItems().get(0).getTargetSelectors());
    }

}
