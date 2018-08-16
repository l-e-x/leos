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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.repository.DocumentRepository;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.MetadataRepository;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class MetadataServiceTest {

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private MetadataService metadataService;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that saving empty metadata is prohibited
    @Test
    public void testSaveEmptyMetadata() {

        try {
            metadataService.saveMetadata(null);
            Assert.fail("Saving empty metadata should throw exception, did not!");
        } catch (CannotCreateMetadataException ccme) {
            // OK
        }
    }

    // test that saving metadata filled with minimal values is working
    @Test
    public void testSaveSuccessful() throws URISyntaxException, CannotCreateMetadataException {

        Document doc = new Document(new URI("www.a.eu"), "title");
        documentRepos.save(doc);

        Metadata meta = new Metadata(doc, defaultGroup, "LEOS");
        metadataService.saveMetadata(meta);

        Assert.assertEquals(1, metadataRepos.count());
    }

    // test that trying to save metadata with missing dependent data throws expected exception
    @Test
    public void testSaveUnsuccessful_missingDependency() {

        try {
            metadataService.saveMetadata(new Metadata(null, defaultGroup, "LEOS"));
            Assert.fail("Saving incomplete metadata should throw exception, did not!");
        } catch (CannotCreateMetadataException ccme) {
            // OK
        }
    }

    // test that the given "metadata block" is chunked and reassembled as expected
    @Test
    public void testDistributionOfKeyValuePairs() throws URISyntaxException, CannotCreateMetadataException {

        final String newSystemId = "theSystemId";
        final String propWithQuotes = "\"anotherQuoteAtBeginning";
        final String expectedConcatenatedKVPairs = "someProp:08/15\npropWithQuote:\"anotherQuoteAtBeginning\n";

        Document doc = new Document(new URI("www.a.eu"), "title");
        documentRepos.save(doc);

        // insert some metadata into the HashMap, parts of them will be redistributed
        Metadata meta = new Metadata(doc, defaultGroup, "LEOS");
        HashMap<String, String> kvPairs = new HashMap<String, String>();
        kvPairs.put("systemId", newSystemId);
        kvPairs.put("responseStatus", "IN_PREPARATION");
        kvPairs.put("someProp", "08/15");
        kvPairs.put("propWithQuote", propWithQuotes);
        meta.setKeyValuePropertyFromHashMap(kvPairs);

        // verify that the kvPairs were chunked as expected
        Assert.assertEquals(newSystemId, meta.getSystemId());
        Assert.assertEquals(Metadata.ResponseStatus.IN_PREPARATION, meta.getResponseStatus());
        Assert.assertTrue(meta.getKeyValuePairs().contains("someProp:08/15"));
        Assert.assertTrue(meta.getKeyValuePairs().contains("propWithQuote:" + propWithQuotes));
        Assert.assertEquals(expectedConcatenatedKVPairs, meta.getKeyValuePairs());

        // verify that asking for the HashMap assembles it again correctly,
        // including the redistributed properties not saved in the "keyValuePairs" property
        HashMap<String, String> retrieved = meta.getKeyValuePropertyAsHashMap();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(4, retrieved.keySet().size());
        Assert.assertEquals(newSystemId, retrieved.get("systemId"));
        Assert.assertEquals("IN_PREPARATION", retrieved.get("responseStatus"));
        Assert.assertEquals("08/15", retrieved.get("someProp"));
        Assert.assertEquals(propWithQuotes, retrieved.get("propWithQuote"));
    }

    // test behavior when invalid (or only partially valid/available) metadata is processed
    @Test
    public void testDistributionOfInvalidKeyValuePairs() throws URISyntaxException {

        // insert some metadata into the HashMap, parts of them will be redistributed
        Metadata meta = new Metadata(null, defaultGroup, "");
        HashMap<String, String> kvPairs = new HashMap<String, String>();
        // no systemId!
        kvPairs.put("responseStatus", "UNKNOWN_VALUE"); // will raise internal enum conversion error
        meta.setKeyValuePropertyFromHashMap(kvPairs);

        Assert.assertTrue(StringUtils.isEmpty(meta.getSystemId()));
        Assert.assertNull(meta.getResponseStatus());

        // test opposite direction
        meta.setKeyValuePairs("key value no colon\n");
        HashMap<String, String> converted = meta.getKeyValuePropertyAsHashMap();
        Assert.assertNotNull(converted);
        Assert.assertEquals(1, converted.keySet().size());
        Assert.assertNotNull(converted.get("systemId"));
    }
}
