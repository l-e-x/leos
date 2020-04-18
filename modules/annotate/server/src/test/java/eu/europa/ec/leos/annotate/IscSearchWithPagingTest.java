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

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
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
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class IscSearchWithPagingTest {

    /**
     * test for using the "ISC single group" search model together with paging options
     * (this test is required as this search model uses a post-filtering that destroys 
     * the "original paging" of the database)
     */

    private static final String uriString = "uri://LEOS/dummy_bill_for_test";
    private static final String dummySelector = "[{\"selector\":null,\"source\":\"" + uriString + "\"}]";
    private static final String DIGIT = "DIGIT";
    private static final String ASC = "asc";
    private static final String CREATED = "created";
    private static final String ANNOT_A = "idA", ANNOT_B = "idB", ANNOT_C = "idC", ANNOT_D = "idD";
    private static final String ANNOT_E = "idE", ANNOT_F = "idF", ANNOT_G = "idG", ANNOT_H = "idH";

    private User digitWorldUser;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AnnotationService annotService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserDetailsCache userDetailsCache;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws URISyntaxException {

        TestDbHelper.cleanupRepositories(this);

        createTestData();
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Test data
    // -------------------------------------

    @SuppressWarnings("PMD.ExcessiveMethodLength")
    private void createTestData() {

        final Group groupWorld = TestDbHelper.insertDefaultGroup(groupRepos);
        final Group groupDigit = new Group(DIGIT, DIGIT, DIGIT, true);
        groupRepos.save(groupDigit);

        final String digitWorldUserLogin = "digitAndWorld";

        // create user and assign to the group
        digitWorldUser = new User(digitWorldUserLogin);
        userRepos.save(digitWorldUser);

        final List<UserEntity> entitiesDigit = Arrays.asList(new UserEntity("2", DIGIT, DIGIT));

        // cache info for users in order to speed up test execution
        userDetailsCache.cache(digitWorldUser.getLogin(), new UserDetails(digitWorldUser.getLogin(), Long.valueOf(2), DIGIT, "user2", entitiesDigit, "", null));

        userGroupRepos.save(new UserGroup(digitWorldUser.getId(), groupWorld.getId()));
        userGroupRepos.save(new UserGroup(digitWorldUser.getId(), groupDigit.getId()));

        // create a document
        final Document document = new Document(URI.create(uriString), "title");
        documentRepos.save(document);

        // we want to create the following annotations/metadata, all created by DIGIT:
        // always: SENT -> responseVersion 1; IN_PREPARATION -> responseVersion 2
        // A: SENT
        // B: IN_PREPARATION, linked to C (link inside 3-page)
        // C: SENT, linked to B
        // D: SENT
        // E: SENT, linked to H (link to next 3-page)
        // F: IN_PREPARATION, linked to G (link to next 3-page)
        // G: SENT, linked to F (link to previous 3-page)
        // H: IN_PREPARATION, linked to E (link to previous 3-page)

        final SimpleMetadata metaHelpSentV1 = new SimpleMetadata(Metadata.PROP_RESPONSE_VERSION, "1");
        metaHelpSentV1.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());

        final SimpleMetadata metaHelpInPrepV2 = new SimpleMetadata(Metadata.PROP_RESPONSE_VERSION, "2");
        metaHelpInPrepV2.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());

        final Metadata metaSent = new Metadata(document, groupDigit, Authorities.ISC, metaHelpSentV1);
        addResponseId(metaSent, DIGIT);
        metadataRepos.save(metaSent);

        final Metadata metaInPrep = new Metadata(document, groupDigit, Authorities.ISC, metaHelpInPrepV2);
        addResponseId(metaInPrep, DIGIT);
        metadataRepos.save(metaInPrep);

        // create annotations, linking needs to be done later
        createAnnotation(ANNOT_A, digitWorldUser, metaSent, 8);
        final Annotation annotB = createAnnotation(ANNOT_B, digitWorldUser, metaInPrep, 7);
        final Annotation annotC = createAnnotation(ANNOT_C, digitWorldUser, metaSent, 6);
        createAnnotation(ANNOT_D, digitWorldUser, metaSent, 5);
        final Annotation annotE = createAnnotation(ANNOT_E, digitWorldUser, metaSent, 4);
        final Annotation annotF = createAnnotation(ANNOT_F, digitWorldUser, metaInPrep, 3);
        final Annotation annotG = createAnnotation(ANNOT_G, digitWorldUser, metaSent, 2);
        final Annotation annotH = createAnnotation(ANNOT_H, digitWorldUser, metaInPrep, 1);

        annotB.setLinkedAnnotationId(annotC.getId());
        annotC.setLinkedAnnotationId(annotB.getId());
        annotE.setLinkedAnnotationId(annotH.getId());
        annotF.setLinkedAnnotationId(annotG.getId());
        annotG.setLinkedAnnotationId(annotF.getId());
        annotH.setLinkedAnnotationId(annotE.getId());

        annotRepos.save(Arrays.asList(annotB, annotC, annotE, annotF, annotG, annotH));
    }

    // creation of annotation
    // the {@param minBeforeNow} serves for having a well-defined order of the annotations in the result
    private Annotation createAnnotation(final String annotId, final User user, final Metadata meta, final int minBeforeNow) {

        final Annotation annot = new Annotation();
        annot.setId(annotId);
        annot.setUser(user);
        annot.setMetadata(meta);

        // mandatory fields
        annot.setId(annotId);
        annot.setCreated(LocalDateTime.now().minusMinutes(minBeforeNow));
        annot.setUpdated(LocalDateTime.now());
        annot.setTargetSelectors(dummySelector);

        annot.setShared(true);

        return annotRepos.save(annot);
    }

    private void addResponseId(final Metadata meta, final String respId) {

        final SimpleMetadata kvPairs = meta.getKeyValuePropertyAsSimpleMetadata();
        kvPairs.put(Metadata.PROP_RESPONSE_ID, respId);
        meta.setKeyValuePropertyFromSimpleMetadata(kvPairs);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    @Test
    public void testPage1() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                uriString, DIGIT, // URI, group
                false,            // provide separate replies
                3, 0,             // limit, offset
                ASC, CREATED);    // order, sort column

        final UserInformation userInfo = getUserInfoWithToken();

        // run search
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, userInfo);

        Assert.assertNotNull(annots);

        // three annotations on first page: A, B, D; C was filtered out
        Assert.assertEquals(3, annots.size());
        assertAnnotationContained(annots.getItems(), ANNOT_A);
        assertAnnotationContained(annots.getItems(), ANNOT_B);
        assertAnnotationContained(annots.getItems(), ANNOT_D);
    }

    @Test
    public void testPage2() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                uriString, DIGIT, // URI, group
                false,            // provide separate replies
                3, 3,             // limit, offset
                ASC, CREATED);    // order, sort column

        final UserInformation userInfo = getUserInfoWithToken();

        // run search
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, userInfo);

        Assert.assertNotNull(annots);

        // three annotations on first page: F, H; D is contained in first 3-page; E+G are filtered out
        Assert.assertEquals(2, annots.size());
        assertAnnotationContained(annots.getItems(), ANNOT_F);
        assertAnnotationContained(annots.getItems(), ANNOT_H);
    }

    // retrieve third page of 3-chunks -> should be empty
    @Test
    public void testPage3() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                uriString, DIGIT, // URI, group
                false,            // provide separate replies
                3, 6,             // limit, offset
                ASC, CREATED);    // order, sort column

        final UserInformation userInfo = getUserInfoWithToken();

        // run search
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, userInfo);

        Assert.assertNotNull(annots);

        Assert.assertEquals(0, annots.size());
    }

    // in this test, the offset used does not correspond to a real page chunk
    @Test
    public void testPage_length4offset1() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                uriString, DIGIT, // URI, group
                false,            // provide separate replies
                4, 1,             // limit, offset
                ASC, CREATED);    // order, sort column

        final UserInformation userInfo = getUserInfoWithToken();

        // run search
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, userInfo);

        Assert.assertNotNull(annots);

        // four annotations on first page: B, D, F, H
        // A is before offset; C, E, G were filtered out
        Assert.assertEquals(4, annots.size());
        assertAnnotationContained(annots.getItems(), ANNOT_B);
        assertAnnotationContained(annots.getItems(), ANNOT_D);
        assertAnnotationContained(annots.getItems(), ANNOT_F);
        assertAnnotationContained(annots.getItems(), ANNOT_H);
    }

    // in this test, the initial search (before post-filtering and paging) does not give any matches already
    @Test
    public void testNoMatchesWithInitialSearch() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                "uri://unknown", DIGIT, // URI, group
                false,            // provide separate replies
                8, 0,             // limit, offset
                ASC, CREATED);    // order, sort column

        final UserInformation userInfo = getUserInfoWithToken();

        // run search
        final AnnotationSearchResult annots = annotService.searchAnnotations(options, userInfo);

        Assert.assertNotNull(annots);
        Assert.assertEquals(0, annots.size());
    }

    private UserInformation getUserInfoWithToken() {

        return new UserInformation(new Token(digitWorldUser, Authorities.ISC, "acc1",
                LocalDateTime.now().plusMinutes(5), "ref1", LocalDateTime.now().plusMinutes(5)));
    }

    private void assertAnnotationContained(final List<Annotation> items, final String annotId) {
        Assert.assertTrue(items.stream().anyMatch(ann -> ann.getId().equals(annotId)));
    }
}
