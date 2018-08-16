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
import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResultWithSeparateReplies;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationSearchWithSeveralPagesTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private AnnotationRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private AnnotationService annotService;

    // -------------------------------------
    // Cleanup of database content and prepare test data
    // -------------------------------------
    private static final String DOC_URL = "uri://LEOS/doc1";

    private static final String FIRST_USER_LOGIN = "userLogin1", SECOND_USER_LOGIN = "userLogin2";

    private static final String ID_1_PUB = "id1";
    private static final String ID_2_PUB = "id2";
    private static final String ID_3_PRIV = "id3";
    private static final String ID_4_PRIV = "id4";
    private static final String ID_5_PRIV = "id5";
    private static final String ID_6_PRIV = "id6";
    private static final String ID_7_PUB = "id7";
    private static final String ID_8_PRIV = "id8";
    private static final String ID_9_PRIV = "id9";
    private static final String ID_10_PRIV = "id10";
    private static final String ID_11_PUB = "id11";
    private static final String ID_12_PUB = "id12";

    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // insert user, assign to the default group
        User user = new User(FIRST_USER_LOGIN), secondUser = new User(SECOND_USER_LOGIN);
        userRepos.save(Arrays.asList(user, secondUser));
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        userGroupRepos.save(new UserGroup(secondUser.getId(), defaultGroup.getId()));

        // insert a document
        Document doc = new Document(new URI(DOC_URL), "document's title");
        documentRepos.save(doc);

        // insert metadata
        Metadata meta = new Metadata(doc, defaultGroup, "sysId");
        metadataRepos.save(meta);
        
        // idea: we use offset = 1 and limit = 3 for querying under account of SECOND user -> private annotations of FIRST user are filtered out
        // we retrieve only the public annotations of FIRST user;
        // -> first query only gives us 1 annotation (from items 2-4)
        // -> second query only gives us 1 annotation (from items 5-7)
        // -> third query does not give us annotations (from items 8-10)
        // -> fourth query gives us 1 annotation (item 11), thereby reaching the limit of 3

        // insert annotations; following structure (only annotations of FIRST user, no replies)
        // 1: public
        // ----------
        // 2: public
        // 3: private
        // 4: private
        // ----------
        // 5: private
        // 6: private
        // 7: public
        // ----------
        // 8: private
        // 9: private
        // 10: private
        // ----------
        // 11: public
        // 12: public

        // insert the first user's annotations
        addAnnotation(ID_1_PUB, LocalDateTime.of(2017, 12, 22, 11, 40, 57), true, doc, defaultGroup, user, meta); // first annotation: public
        addAnnotation(ID_2_PUB, LocalDateTime.of(2017, 12, 23, 11, 40, 57), true, doc, defaultGroup, user, meta); // second annotation: public
        addAnnotation(ID_3_PRIV, LocalDateTime.of(2017, 12, 24, 11, 40, 57), false, doc, defaultGroup, user, meta); // third annotation: private
        addAnnotation(ID_4_PRIV, LocalDateTime.of(2017, 12, 25, 11, 40, 57), false, doc, defaultGroup, user, meta); // fourth annotation: private
        addAnnotation(ID_5_PRIV, LocalDateTime.of(2017, 12, 26, 11, 40, 57), false, doc, defaultGroup, user, meta); // fifth annotation: private
        addAnnotation(ID_6_PRIV, LocalDateTime.of(2017, 12, 27, 11, 40, 57), false, doc, defaultGroup, user, meta); // sixth annotation: private
        addAnnotation(ID_7_PUB, LocalDateTime.of(2017, 12, 28, 11, 40, 57), true, doc, defaultGroup, user, meta); // seventh annotation: public
        addAnnotation(ID_8_PRIV, LocalDateTime.of(2017, 12, 29, 11, 40, 57), false, doc, defaultGroup, user, meta); // eighth annotation: private
        addAnnotation(ID_9_PRIV, LocalDateTime.of(2017, 12, 30, 11, 40, 57), false, doc, defaultGroup, user, meta); // ninth annotation: private
        addAnnotation(ID_10_PRIV, LocalDateTime.of(2017, 12, 31, 11, 40, 57), false, doc, defaultGroup, user, meta); // tenth annotation: private
        addAnnotation(ID_11_PUB, LocalDateTime.of(2087, 01, 01, 11, 40, 57), true, doc, defaultGroup, user, meta); // eleventh annotation: public
        addAnnotation(ID_12_PUB, LocalDateTime.of(2087, 01, 02, 11, 40, 57), true, doc, defaultGroup, user, meta); // twelfth annotation: public
    }

    private void addAnnotation(String id, LocalDateTime createdDate, boolean shared, Document theDoc, Group theGroup, User theUser, Metadata theMeta)
            throws URISyntaxException, ParseException {

        // dummy selector
        final String dummySelector = "[{\"selector\":null,\"source\":\"" + new URI(DOC_URL) + "\"}]";

        Annotation ann = new Annotation();
        ann.setId(id);
        ann.setCreated(createdDate);
        ann.setUpdated(LocalDateTime.of(2017, 12, 22, 11, 42, 59));
        ann.setDocument(theDoc);
        ann.setGroup(theGroup);
        ann.setMetadata(theMeta);
        ann.setReferences("");
        ann.setShared(shared);
        ann.setTargetSelectors(dummySelector);
        ann.setText(id);
        ann.setUser(theUser);
        annotRepos.save(ann);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    /**
     * test: several DB queries are required to retrieve the desired amount of items 
     */
    @Test
    public void testSeveralQueriesRequired() {

        // retrieve second 10 annotations
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                DOC_URL, "__world__", // URI, group
                true,                 // provide separate replies
                3, 1,                 // limit, offset - tuned such that our scenario produces a dedicated result
                "asc", "created");    // order, sort column

        // use second user -> private annotation of first user are filtered out
        List<Annotation> items = annotService.searchAnnotations(options, SECOND_USER_LOGIN);
        Assert.assertEquals(3, items.size());

        JsonSearchResult initialResult = annotService.convertToJsonSearchResult(items, null, options);
        Assert.assertTrue(initialResult instanceof JsonSearchResultWithSeparateReplies);
        
        JsonSearchResultWithSeparateReplies result = (JsonSearchResultWithSeparateReplies) initialResult; 
        Assert.assertNotNull(result);
        Assert.assertEquals(3, result.getRows().size());   // 3 items expected
        Assert.assertEquals(0, result.getReplies().size()); // and no replies

        // check that the correct items (in correct order) were returned and check their respective number of replies
        Assert.assertEquals(ID_2_PUB, result.getRows().get(0).getId());
        Assert.assertEquals(ID_7_PUB, result.getRows().get(1).getId());
        Assert.assertEquals(ID_11_PUB, result.getRows().get(2).getId());
    }
}
