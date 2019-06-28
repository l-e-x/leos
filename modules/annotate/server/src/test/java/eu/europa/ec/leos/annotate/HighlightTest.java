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
import eu.europa.ec.leos.annotate.repository.*;
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
public class HighlightTest {

    private User user;

    /**
     * This test class contains tests for creating highlights (=annotations without text)
     */

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

        user = userRepos.save(new User("demo"));
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
     * simple creation of a highlight (=annotation without text)
     * note: implicitly also tests retrieval of an existing highlight
     */
    @Test
    public void testCreateHighlight() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net";
        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);

        // create a highlight
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, userInfo);

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertNull(readAnnot.getText()); // verify that text remains empty

        final JsonAnnotation jsReply = conversionService.convertToJsonAnnotation(readAnnot, userInfo);
        Assert.assertNotNull(jsReply);
        Assert.assertNull(jsReply.getText()); // verify that text remains empty
    }

    /**
     * update of an existing highlight, e.g. adding text (by which it becomes an annotation)
     */
    @Test
    public void testUpdateHighlight()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net";
        final String updatedText = "text added now";

        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);

        // create a highlight
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, userInfo);

        final String annotId = annot.getId();

        // update: add text
        annot.setText(updatedText);
        annotService.updateAnnotation(annotId, annot, userInfo);

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(updatedText, readAnnot.getText()); // verify that text was written
    }

    /**
     * updating an existing highlight
     * note: permission checks have already been tested for annotations
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testDeleteHighlight()
            throws CannotCreateAnnotationException, CannotDeleteAnnotationException,
            MissingPermissionException, CannotDeleteSentAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net";
        final String authority = Authorities.EdiT;

        // create a highlight
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        final UserInformation userInfo = new UserInformation(user, authority);
        annotService.createAnnotation(annot, userInfo);

        // delete the annotation
        annotService.deleteAnnotationById(annot.getId(), userInfo);

        // read annotation from database and verify
        final Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNull(readAnnot);
        TestHelper.assertHasStatus(annotRepos, annot.getId(), AnnotationStatus.DELETED, user.getId());
    }

    /**
     * search for existing highlights
     */
    @Test
    public void testSearchHighlights() throws CannotCreateAnnotationException {

        final String authority = Authorities.EdiT;
        final String hypothesisUserAccount = "acct:user@" + authority;

        final UserInformation userInfo = new UserInformation(
                new Token(user, authority, "acc", LocalDateTime.now().plusMinutes(5), "ref", LocalDateTime.now().plusMinutes(5)));

        // create a highlight
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, userInfo);

        // delete the annotation
        final AnnotationSearchOptions options = new AnnotationSearchOptions(annot.getUri().toString(), "__world__", false, 200, 0, "asc", "created");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);

        // verify: there is only the highlight
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(annot.getId(), result.getItems().get(0).getId());
        Assert.assertNull(result.getItems().get(0).getText());
    }
}
