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
package eu.europa.ec.leos.annotate.repository;

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class AnnotationRepositoryTest {

    // note: we do not need to test JPA, we trust it
    // but these tests check that our understanding of JPA's cool dynamic query creation is correct

    private User user;
    private Metadata meta1, meta2;
    private Annotation annot1, annot3, annot4, annot6;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        user = new User("demo");
        userRepos.save(user);

        // prepare test data and required objects annotations are depending on
        final Document doc = new Document(URI.create("http://a/5"), "title");
        documentRepos.save(doc);
        final Group group = new Group("team", true);
        groupRepos.save(group);

        meta1 = new Metadata(doc, group, Authorities.EdiT);
        metadataRepos.save(meta1);
        meta2 = new Metadata(doc, group, Authorities.ISC);
        metadataRepos.save(meta2);

        // create:
        // ann 1: Metadata 1, Status Normal, SentDeleted=True
        // ann 2: Metadata 1, Status Deleted, SentDeleted=False
        // ann 3: Metadata 1, Status Accepted, SentDeleted=True
        // ann 4: Metadata 2, Status Normal, SentDeleted=True
        // ann 5: Metadata 2, Status Normal, SentDeleted=False
        // ann 6: Metadata 2, Status Deleted, SentDeleted=True

        annot1 = annotRepos.save(createAnnotation(meta1, AnnotationStatus.NORMAL, true, "a1"));
        annotRepos.save(createAnnotation(meta1, AnnotationStatus.DELETED, false, "a2")); // annot2
        annot3 = annotRepos.save(createAnnotation(meta1, AnnotationStatus.ACCEPTED, true, "a3"));
        annot4 = annotRepos.save(createAnnotation(meta2, AnnotationStatus.NORMAL, true, "a4"));
        annotRepos.save(createAnnotation(meta2, AnnotationStatus.NORMAL, false, "a5")); // annot5
        annot6 = annotRepos.save(createAnnotation(meta2, AnnotationStatus.DELETED, true, "a6"));
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    @Test
    public void testMetadataStatusSentDeletedSearch1() {

        // act
        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta1.getId()), Arrays.asList(AnnotationStatus.NORMAL));

        // verify
        Assert.assertEquals(1, foundAnnots.size());
        Assert.assertEquals(annot1.getId(), foundAnnots.get(0).getId());
    }

    @Test
    public void testMetadataStatusSentDeletedSearch2() {

        // deleted with metadata 1 -> no match (since only SentDeleted=False exists)
        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta1.getId()), Arrays.asList(AnnotationStatus.DELETED));

        Assert.assertEquals(0, foundAnnots.size());
    }

    @Test
    public void testMetadataStatusSentDeletedSearch3() {

        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta1.getId()), Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.DELETED));

        Assert.assertEquals(1, foundAnnots.size());
        Assert.assertEquals(annot1.getId(), foundAnnots.get(0).getId());
    }

    @Test
    public void testMetadataStatusSentDeletedSearch4() {

        // all status with all metadata -> four matches
        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta1.getId(), meta2.getId()), AnnotationStatus.getAllValues());

        Assert.assertEquals(4, foundAnnots.size()); // 1, 3, 4, 6
        checkIds(foundAnnots, Arrays.asList(annot1, annot3, annot4, annot6));
    }

    @Test
    public void testMetadataStatusSentDeletedSearch5() {

        // one status with all metadata -> two matches
        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta1.getId(), meta2.getId()), Arrays.asList(AnnotationStatus.NORMAL));

        Assert.assertEquals(2, foundAnnots.size()); // 1, 4
        checkIds(foundAnnots, Arrays.asList(annot1, annot4));

    }

    @Test
    public void testMetadataStatusSentDeletedSearch6() {

        // one metadata with all status -> two matches
        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta2.getId()), AnnotationStatus.getAllValues());

        Assert.assertEquals(2, foundAnnots.size()); // 4, 6
        checkIds(foundAnnots, Arrays.asList(annot4, annot6));
    }

    @Test
    public void testMetadataStatusSentDeletedSearch7() {
        final List<Annotation> foundAnnots = annotRepos.findByMetadataIdIsInAndStatusIsInAndSentDeletedIsTrue(
                Arrays.asList(meta1.getId(), meta2.getId()), new ArrayList<AnnotationStatus>());

        Assert.assertEquals(0, foundAnnots.size());
    }

    private void checkIds(final List<Annotation> foundAnnots, final List<Annotation> expectedAnnots) {

        Assert.assertEquals(expectedAnnots.size(), foundAnnots.size());
        foundAnnots.stream().allMatch(annot -> expectedAnnots.contains(annot));
    }

    private Annotation createAnnotation(final Metadata meta, final AnnotationStatus status,
            final boolean sentDel, final String annotId) {

        final Annotation ann = new Annotation();
        ann.setMetadata(meta);
        ann.setStatus(status);
        ann.setSentDeleted(sentDel);
        ann.setId(annotId);
        ann.setCreated(LocalDateTime.now());
        ann.setUpdated(LocalDateTime.now());
        ann.setUser(user);
        ann.setTargetSelectors("a");
        return ann;
    }
}
