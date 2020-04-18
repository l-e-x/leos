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
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.repository.impl.MetadataVersionUpToSearchSpec;
import eu.europa.ec.leos.annotate.services.impl.MetadataListHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * tests for the search Specification class {@link MetadataVersionUpToSearchSpec}
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class MetadataVersionUpToTest {

    private Group defaultGroup;
    private Document document;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private DocumentRepository documentRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws URISyntaxException {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // insert a document
        final String URI = "https://leos/4";
        document = new Document(new URI(URI), "document's title");
        documentRepos.save(document);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    private Metadata createMetadata(final String version) {

        Metadata meta = new Metadata(document, defaultGroup, Authorities.EdiT);
        meta.setVersion(version);

        meta = metadataRepos.save(meta);
        return meta;
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void testAllIds() {

        final Metadata meta1 = createMetadata("2.0");
        final Metadata meta2 = createMetadata("1.0");

        List<Metadata> result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.0",
                MetadataListHelper.getMetadataSetIds(Arrays.asList(meta1, meta2))));

        Assert.assertEquals(1, result.size());
        assertResultContains(result, meta1.getId());

        // search with less given IDs (in particular without the matching ID)
        result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.0", Arrays.asList(meta1.getId())));
        Assert.assertEquals(0, result.size());
    }

    // test different combinations of wanted version and metadata ID restriction
    @Test
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void testManyVersion() {

        final Metadata meta01 = createMetadata("0.1");
        final Metadata meta02 = createMetadata("0.2");
        final Metadata meta03 = createMetadata("0.3");
        final Metadata meta10 = createMetadata("1.0");
        final Metadata meta11 = createMetadata("1.1");
        final Metadata meta111 = createMetadata("1.1.1");
        final Metadata meta12 = createMetadata("1.2");
        final Metadata meta121 = createMetadata("1.2.1");
        final Metadata meta13 = createMetadata("1.3");
        final Metadata meta20 = createMetadata("2.0");
        final Metadata meta20_2 = createMetadata("2.0");
        final Metadata meta30 = createMetadata("3.0");

        // search for all metadata with version 1.0
        List<Metadata> result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.0", new ArrayList<Long>()));

        Assert.assertEquals(4, result.size());
        assertResultContains(result, Arrays.asList(meta01, meta02, meta03, meta10));

        // search for some metadata with version 1.0
        result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.0",
                Arrays.asList(meta02.getId(), meta10.getId(), meta20.getId()))); // only 02 and 10 will match

        Assert.assertEquals(2, result.size());
        assertResultContains(result, Arrays.asList(meta02, meta10));

        // search all metadata with version up to 2.5
        result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("2.5", new ArrayList<Long>()));

        Assert.assertEquals(11, result.size());
        assertResultContains(result, Arrays.asList(meta01, meta02, meta03, meta10, meta11, meta111, meta12, meta121,
                meta13, meta20, meta20_2)); // note: both "2.0" items should be found!

        // search for all metadata with version up to 1.2 -> should not find 1.2.1!
        result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.2", new ArrayList<Long>()));

        Assert.assertEquals(7, result.size());
        assertResultContains(result, Arrays.asList(meta01, meta02, meta03, meta10, meta11, meta111, meta12));

        // search for all metadata with version up to 1.2, but restrict metadata IDs
        result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.2",
                Arrays.asList(meta03.getId(), meta30.getId(), meta111.getId()))); // only 0.3 and 1.1.1 will match

        Assert.assertEquals(2, result.size());
        assertResultContains(result, Arrays.asList(meta03, meta111));
    }

    // don't give any metadata IDs to the search -> should take all in DB into account
    @Test
    public void searchWithoutIds() {

        createMetadata("2.0");
        final Metadata meta2 = createMetadata("1.0");
        final Metadata meta3 = createMetadata("1.2");

        // search without IDs -> no restriction on IDs, all are taken into account
        final List<Metadata> result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("1.5",
                new ArrayList<Long>()));
        Assert.assertEquals(2, result.size());
        assertResultContains(result, Arrays.asList(meta2, meta3));
    }

    // don't give any version to the search -> should take all versions into account
    @Test
    public void searchWithoutVersion() {

        final Metadata meta1 = createMetadata("2.0");
        createMetadata("1.0");
        final Metadata meta3 = createMetadata("1.2");

        // search without version -> should deliver exactly those metadata asked for
        final List<Metadata> result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("",
                Arrays.asList(meta1.getId(), meta3.getId())));
        Assert.assertEquals(2, result.size());
        assertResultContains(result, Arrays.asList(meta1, meta3));
    }

    // don't give any version or metadata IDs to the search -> should take all items of the DB
    @Test
    public void searchWithoutAnything() {

        final Metadata meta1 = createMetadata("2.0");
        final Metadata meta2 = createMetadata("1.0");
        final Metadata meta3 = createMetadata("1.2");

        // search without version -> should deliver exactly those metadata asked for
        final List<Metadata> result = metadataRepos.findAll(new MetadataVersionUpToSearchSpec("",
                new ArrayList<Long>()));
        Assert.assertEquals(3, result.size());
        assertResultContains(result, Arrays.asList(meta1, meta2, meta3));
    }

    private void assertResultContains(final List<Metadata> result, final long expectedId) {

        final Optional<Metadata> item = result.stream().filter(meta -> meta.getId() == expectedId).findFirst();
        Assert.assertNotNull(item);
    }

    private void assertResultContains(final List<Metadata> result, final List<Metadata> matches) {

        for (final Metadata meta : matches) {
            assertResultContains(result, meta.getId());
        }
    }
}
