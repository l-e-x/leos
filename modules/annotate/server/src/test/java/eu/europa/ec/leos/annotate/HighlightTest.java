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
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserGroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotDeleteAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationException;
import eu.europa.ec.leos.annotate.services.exceptions.MissingPermissionException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class HighlightTest {

    /**
     * NOTE: This test class contains tests for creating highlights (=annotations without text)
     */

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        User theUser = userRepos.save(new User("demo"));
        userGroupRepos.save(new UserGroup(theUser.getId(), defaultGroup.getId()));
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
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * simple creation of a highlight (=annotation without text)
     * note: implicitly also tests retrieval of an existing highlight
     */
    @Test
    public void testCreateHighlight() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";

        // create a highlight
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, login);

        // read annotation from database and verify
        Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertNull(readAnnot.getText()); // verify that text remains empty

        JsonAnnotation jsReply = annotService.convertToJsonAnnotation(readAnnot);
        Assert.assertNotNull(jsReply);
        Assert.assertNull(jsReply.getText()); // verify that text remains empty
    }

    /**
     * update of an existing highlight, e.g. adding text (by which it becomes an annotation)
     */
    @Test
    public void testUpdateHighlight() throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";
        final String updatedText = "text added now";

        // create a highlight
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, login);

        String id = annot.getId();

        // update: add text
        annot.setText(updatedText);
        annotService.updateAnnotation(id, annot, login);

        // read annotation from database and verify
        Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNotNull(readAnnot);
        Assert.assertEquals(updatedText, readAnnot.getText()); // verify that text was written
    }

    /**
     * updating an existing highlight
     * note: permission checks have already been tested for annotations
     */
    @Test
    public void testDeleteHighlight() throws CannotCreateAnnotationException, CannotDeleteAnnotationException, MissingPermissionException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";

        // create a highlight
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, login);

        // delete the annotation
        annotService.deleteAnnotationById(annot.getId(), login);

        // read annotation from database and verify
        Annotation readAnnot = annotService.findAnnotationById(annot.getId());
        Assert.assertNull(readAnnot);
    }

    /**
     * search for existing highlights
     */
    @Test
    public void testSearchHighlights() throws CannotCreateAnnotationException {

        final String hypothesisUserAccount = "acct:user@email.net", login = "demo";

        // create a highlight
        JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        annot.setText("");
        annotService.createAnnotation(annot, login);

        // delete the annotation
        AnnotationSearchOptions options = new AnnotationSearchOptions(annot.getUri().toString(), "__world__", false, 200, 0, "asc", "created");
        List<Annotation> result = annotService.searchAnnotations(options, login);

        // verify: there is only the highlight
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(annot.getId(), result.get(0).getId());
        Assert.assertNull(result.get(0).getText());
    }
}
