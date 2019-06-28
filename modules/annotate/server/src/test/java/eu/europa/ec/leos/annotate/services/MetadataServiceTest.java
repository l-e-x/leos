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
package eu.europa.ec.leos.annotate.services;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.repository.DocumentRepository;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.MetadataRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.exceptions.CannotCreateMetadataException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotUpdateAnnotationStatusException;
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
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class MetadataServiceTest {

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

    @Autowired
    private UserRepository userRepos;

    private Group defaultGroup;

    private static final String RESP_STAT = "responseStatus";
    private static final String SYS_ID = "systemId";

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that saving empty metadata is prohibited
    @Test(expected = CannotCreateMetadataException.class)
    public void testSaveEmptyMetadata() throws Exception {

        metadataService.saveMetadata(null);
    }

    // test that saving metadata filled with minimal values is working
    @Test
    public void testSaveSuccessful() throws URISyntaxException, CannotCreateMetadataException {

        final Document doc = new Document(new URI("www.a.eu"), "sometitle");
        documentRepos.save(doc);

        final Metadata meta = new Metadata(doc, defaultGroup, Authorities.EdiT);
        metadataService.saveMetadata(meta);

        Assert.assertEquals(1, metadataRepos.count());
    }

    // test that trying to save metadata with missing dependent data throws expected exception
    @Test(expected = CannotCreateMetadataException.class)
    public void testSaveUnsuccessful_missingDependency() throws Exception {

        metadataService.saveMetadata(new Metadata(null, defaultGroup, Authorities.EdiT));
    }

    // test that the given "metadata block" is chunked and reassembled as expected
    @Test
    public void testDistributionOfKeyValuePairs() throws URISyntaxException, CannotCreateMetadataException {

        final String newSystemId = "theSystemId";
        final String propWithQuotes = "\"anotherQuoteAtBeginning";
        final String expectedConcatKVPairs = "someProp:08/15\npropWithQuote:\"anotherQuoteAtBeginning\n";

        final Document doc = new Document(new URI("www.abc.eu"), "mytitle");
        documentRepos.save(doc);

        // insert some metadata into the HashMap, parts of them will be redistributed
        final Metadata meta = new Metadata(doc, defaultGroup, Authorities.EdiT);
        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(SYS_ID, newSystemId);
        kvPairs.put(RESP_STAT, "IN_PREPARATION");
        kvPairs.put("someProp", "08/15");
        kvPairs.put("propWithQuote", propWithQuotes);
        meta.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        // verify that the kvPairs were chunked as expected
        Assert.assertEquals(newSystemId, meta.getSystemId());
        Assert.assertEquals(Metadata.ResponseStatus.IN_PREPARATION, meta.getResponseStatus());
        Assert.assertTrue(meta.getKeyValuePairs().contains("someProp:08/15"));
        Assert.assertTrue(meta.getKeyValuePairs().contains("propWithQuote:" + propWithQuotes));
        Assert.assertEquals(expectedConcatKVPairs, meta.getKeyValuePairs());

        // verify that asking for the HashMap assembles it again correctly,
        // including the redistributed properties not saved in the "keyValuePairs" property
        final SimpleMetadata retrieved = meta.getKeyValuePropertyAsSimpleMetadata();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(2, retrieved.keySet().size());
        Assert.assertEquals("08/15", retrieved.get("someProp"));
        Assert.assertEquals(propWithQuotes, retrieved.get("propWithQuote"));
        // note: systemId and responseStatus are not contained!

        // verify that when asking FOR ALL items, systemId and responseStatus are contained
        final SimpleMetadata retrievedAll = meta.getAllMetadataAsSimpleMetadata();
        Assert.assertNotNull(retrievedAll);
        Assert.assertEquals(4, retrievedAll.keySet().size());
        Assert.assertEquals("08/15", retrievedAll.get("someProp"));
        Assert.assertEquals(propWithQuotes, retrievedAll.get("propWithQuote"));
        Assert.assertEquals(newSystemId, retrievedAll.get(SYS_ID));
        Assert.assertEquals("IN_PREPARATION", retrievedAll.get(RESP_STAT));
    }

    // test behavior when invalid (or only partially valid/available) metadata is processed
    @Test
    public void testDistributionOfInvalidKeyValuePairs() throws URISyntaxException {

        // insert some metadata into the HashMap, parts of them will be redistributed
        final Metadata meta = new Metadata(null, defaultGroup, "");
        final SimpleMetadata kvPairs = new SimpleMetadata();
        // no systemId!
        kvPairs.put(RESP_STAT, "UNKNOWN_VALUE"); // will raise internal enum conversion error
        meta.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        Assert.assertTrue(StringUtils.isEmpty(meta.getSystemId()));
        Assert.assertNull(meta.getResponseStatus());

        // test opposite direction
        meta.setKeyValuePairs("key value no colon\n");
        final SimpleMetadata converted = meta.getKeyValuePropertyAsSimpleMetadata();
        Assert.assertNotNull(converted);
        Assert.assertEquals(0, converted.keySet().size());

        // test opposite direction WITH ALL metadata
        final SimpleMetadata convertedFull = meta.getAllMetadataAsSimpleMetadata();
        Assert.assertNotNull(convertedFull);
        Assert.assertEquals(1, convertedFull.keySet().size());
        Assert.assertNotNull(convertedFull.get(SYS_ID));
    }

    // simple test for successful comparison of HashMap items with database metadata object
    @Test
    public void testAllMetadataContainedInDbObject_Successful() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(RESP_STAT, "SENT");
        kvPairs.put("whatever", "hello");
        kvPairs.put("someint", "5");
        kvPairs.put(SYS_ID, "someauthority");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("someauthority");
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();

        Assert.assertTrue(metadataService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // simple test for comparing incomplete objects
    @Test
    public void testAllMetadataContainedInDbObject_EmptyObjects() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        final Metadata dbMetadata = new Metadata();

        // no requirements given -> ok
        Assert.assertTrue(metadataService.areAllMetadataContainedInDbMetadata(null, dbMetadata));
        Assert.assertTrue(metadataService.areAllMetadataContainedInDbMetadata(null, null));

        // empty requirements -> ok
        Assert.assertTrue(metadataService.areAllMetadataContainedInDbMetadata(kvPairs, null));

        // both empty -> equal
        Assert.assertTrue(metadataService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));

        // requirements present, but DB empty -> fail
        kvPairs.put("something", "content");
        Assert.assertFalse(metadataService.areAllMetadataContainedInDbMetadata(kvPairs, null));
    }

    // simple test for comparison of HashMap items with database metadata object: HashMap contains more objects than DB item
    @Test
    public void testAllMetadataContainedInDbObject_MapRequiresMore() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(RESP_STAT, "SENT");
        kvPairs.put("whatever", "hello");
        kvPairs.put("someint", "5");
        kvPairs.put(SYS_ID, "thesystem");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();
        kvPairsCopy.put("onemore", "item"); // add one more item in map, but is not contained in DB object

        Assert.assertFalse(metadataService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // simple test for comparison of HashMap items with database metadata object: database object contains more objects
    @Test
    public void testAllMetadataContainedInDbObject_DbHasMore() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(RESP_STAT, "SENT");
        kvPairs.put("whatever", "hello");
        kvPairs.put("someint", "5");
        kvPairs.put("onemore", "item"); // add one more item in map, is contained in DB object

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();
        kvPairsCopy.remove("onemore"); // remove the item from the map of required items -> should still match

        Assert.assertTrue(metadataService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // test that Hashmap contains a system ID, but database object does not having one
    @Test
    public void testAllMetadataContainedInDbObject_NoSystemidInDbObject() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(SYS_ID, "mysys");

        final Metadata dbMetadata = new Metadata();

        Assert.assertFalse(metadataService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));
    }

    // test that Hashmap contains a system ID, but database object contains a different one
    @Test
    public void testAllMetadataContainedInDbObject_SystemidInDbObjectDifferent() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(SYS_ID, "mysys");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("yoursys");

        Assert.assertFalse(metadataService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));
    }

    // test that retrieving IDs of list of Metadata objects works as expected for empty inputs
    @Test
    public void testGetMetadataSetIds_EmptyInput() {

        Assert.assertNull(metadataService.getMetadataSetIds(null));

        // empty list should also return null
        final List<Metadata> testList = new ArrayList<Metadata>();
        Assert.assertNull(metadataService.getMetadataSetIds(testList));

        // null entries should be discarded -> empty list -> return null
        testList.add(null);
        Assert.assertNull(metadataService.getMetadataSetIds(testList));
    }

    // test that retrieving IDs of list of Metadata objects works as expected: ignoring null, filtering duplicates
    @Test
    public void testGetMetadataSetIds_Filtered() {

        final List<Metadata> testList = new ArrayList<Metadata>();

        final Metadata firstItem = new Metadata();
        firstItem.setId(4);
        testList.add(firstItem);

        testList.add(null);

        final Metadata secondItem = new Metadata();
        secondItem.setId(8);
        testList.add(secondItem);

        final Metadata thirdItem = new Metadata();
        thirdItem.setId(4);
        testList.add(thirdItem);

        testList.add(null);

        // null entries should be discarded, duplicates be filtered -> 2 items remain
        final List<Long> resultList = metadataService.getMetadataSetIds(testList);
        Assert.assertEquals(2, resultList.size());
        Assert.assertTrue(resultList.contains(Long.valueOf(4)));
        Assert.assertTrue(resultList.contains(Long.valueOf(8)));
    }

    // test that retrieving IDs of lists of Metadata objects works as expected for empty inputs
    @Test
    public void testGetMetadataSetIds_Lists_EmptyInput() {

        Assert.assertNull(metadataService.getMetadataSetIds(null, null));

        // empty list should also return null
        final List<Metadata> testList = new ArrayList<Metadata>();
        Assert.assertNull(metadataService.getMetadataSetIds(testList, null));
        Assert.assertNull(metadataService.getMetadataSetIds(null, testList));

        // null entries should be discarded -> empty list -> return null
        testList.add(null);
        Assert.assertNull(metadataService.getMetadataSetIds(testList, null));
        Assert.assertNull(metadataService.getMetadataSetIds(null, testList));
        Assert.assertNull(metadataService.getMetadataSetIds(testList, testList));
    }

    // test that retrieving IDs of lists of Metadata objects works as expected: ignoring null, filtering duplicates - applies to both lists
    @Test
    public void testGetMetadataSetIds_Lists_Filtered() {

        final List<Metadata> testList1 = new ArrayList<Metadata>();

        final Metadata firstItem = new Metadata();
        firstItem.setId(4);
        testList1.add(firstItem);

        testList1.add(null);

        final Metadata secondItem = new Metadata();
        secondItem.setId(8);
        testList1.add(secondItem);

        final Metadata thirdItem = new Metadata();
        thirdItem.setId(4);
        testList1.add(thirdItem);

        testList1.add(null);

        final List<Metadata> testList2 = new ArrayList<Metadata>();
        testList2.add(null);

        final Metadata item1 = new Metadata();
        item1.setId(4);
        testList2.add(item1);

        final Metadata item2 = new Metadata();
        item2.setId(1);
        testList2.add(item2);

        // null entries should be discarded, duplicates be filtered -> 3 items remain (4, 8 from first list; 1 from second list)
        final List<Long> resultList = metadataService.getMetadataSetIds(testList1, testList2);
        Assert.assertEquals(3, resultList.size());
        Assert.assertTrue(resultList.contains(Long.valueOf(1)));
        Assert.assertTrue(resultList.contains(Long.valueOf(4)));
        Assert.assertTrue(resultList.contains(Long.valueOf(8)));
    }

    // test that retrieving IDs of list of Metadata objects always returns a list, even if empty
    @Test
    public void testGetNonNullMetadataSetIds() {

        Assert.assertNotNull(metadataService.getNonNullMetadataSetIds(null));

        // empty list should also return null
        final List<Metadata> testList = new ArrayList<Metadata>();
        Assert.assertNotNull(metadataService.getNonNullMetadataSetIds(testList));

        // null entries should be discarded -> empty list -> still not null
        testList.add(null);
        Assert.assertNotNull(metadataService.getNonNullMetadataSetIds(testList));

        // valid content -> still not null as result
        final Metadata meta = new Metadata();
        meta.setId(2);
        testList.add(meta);

        final List<Long> resultList = metadataService.getNonNullMetadataSetIds(testList);
        Assert.assertNotNull(resultList);
        Assert.assertEquals(1, resultList.size());
    }

    // test the getIdsOfMatchingMetadatas method when no candidates are given (i.e. {@literal null})
    @Test
    public void testGetIdsOfMatchingMetadatas_candidatesNull() {

        final SimpleMetadata request = new SimpleMetadata();
        final List<SimpleMetadata> requestList = Arrays.asList(request);

        Assert.assertNull(metadataService.getIdsOfMatchingMetadatas(null, requestList));
    }

    // test the getIdsOfMatchingMetadatas method when no candidates are given (i.e. empty list)
    @Test
    public void testGetIdsOfMatchingMetadatas_noCandidates() {

        final SimpleMetadata request = new SimpleMetadata();
        final List<SimpleMetadata> requestList = Arrays.asList(request);

        final List<Metadata> metaList = new ArrayList<Metadata>();
        Assert.assertNull(metadataService.getIdsOfMatchingMetadatas(metaList, requestList));
    }

    // test the getIdsOfMatchingMetadatas method when no requested metadata are given (i.e. {@literal null})
    // should return the candidates
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test
    public void testGetIdsOfMatchingMetadatas_requestedNull() {

        final List<Metadata> metaList = new ArrayList<Metadata>();
        final Metadata randomMeta = new Metadata();
        final Random rand = new Random();
        randomMeta.setId(rand.nextLong());
        metaList.add(randomMeta);

        final SimpleMetadata metaDummy = null;
        final List<Long> result = metadataService.getIdsOfMatchingMetadatas(metaList, metaDummy);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(randomMeta.getId(), result.get(0).longValue());
    }

    // test the getIdsOfMatchingMetadatas method when no requested metadata are given (i.e. empty list)
    // should return the candidates
    @Test
    public void testGetIdsOfMatchingMetadatas_requestedEmpty() {

        final List<SimpleMetadata> requestList = new ArrayList<SimpleMetadata>();

        final List<Metadata> metaList = new ArrayList<Metadata>();
        final Metadata randomMeta = new Metadata();
        final Random rand = new Random();
        randomMeta.setId(rand.nextLong());
        metaList.add(randomMeta);

        final List<Long> result = metadataService.getIdsOfMatchingMetadatas(metaList, requestList);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(randomMeta.getId(), result.get(0).longValue());
    }

    // test the getIdsOfMatchingMetadatas method when no requested metadata are given (i.e. list with empty map)
    // should return the candidates
    @Test
    public void testGetIdsOfMatchingMetadatas_requestedEmptyMap() {

        final SimpleMetadata emptyMap = new SimpleMetadata();
        final List<SimpleMetadata> requestList = new ArrayList<SimpleMetadata>();
        requestList.add(emptyMap);

        final List<Metadata> metaList = new ArrayList<Metadata>();
        final Metadata randomMeta = new Metadata();
        final Random rand = new Random();
        randomMeta.setId(rand.nextLong());
        metaList.add(randomMeta);

        final List<Long> result = metadataService.getIdsOfMatchingMetadatas(metaList, requestList);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(randomMeta.getId(), result.get(0).longValue());
    }

    // test the getIdsOfMatchingMetadatas method
    // should return two of the three candidates
    @Test
    public void testGetIdsOfMatchingMetadatas() {

        final String COMMON_KEY= "common";
        final String COMMON_VAL = "commonval";
        final String SINGLE_KEY = "single";
        final String SINGLE_VAL = "singleval";
        
        // create list of requested metadata
        final SimpleMetadata firstMap = new SimpleMetadata(); // will match
        firstMap.put(COMMON_KEY, COMMON_VAL);
        firstMap.put("some", "thing");
        
        final SimpleMetadata secondMap = new SimpleMetadata(); // will not match
        secondMap.put("un", "known");
        
        final SimpleMetadata thirdMap = new SimpleMetadata(); // will match
        thirdMap.put(SINGLE_KEY, SINGLE_VAL);
        
        final List<SimpleMetadata> requestList = new ArrayList<SimpleMetadata>();
        requestList.add(firstMap);
        requestList.add(secondMap);
        requestList.add(thirdMap);

        final Random rand = new Random();
        
        // create three candidate metadata
        // first will match the "common key"
        final List<Metadata> metaList = new ArrayList<Metadata>();
        final Metadata firstMeta = new Metadata();
        firstMeta.setId(rand.nextLong());
        final SimpleMetadata itemsFirstMeta = new SimpleMetadata(firstMap);
        itemsFirstMeta.put("and", "this");
        firstMeta.setKeyValuePropertyFromSimpleMetadata(itemsFirstMeta);
        metaList.add(firstMeta);
        
        // second won't match
        final Metadata secondMeta = new Metadata();
        secondMeta.setId(rand.nextLong());
        final SimpleMetadata itemsSecondMeta = new SimpleMetadata();
        itemsSecondMeta.put("thisis", "nowhere else");
        secondMeta.setKeyValuePropertyFromSimpleMetadata(itemsSecondMeta);
        metaList.add(secondMeta);

        // third will match the "single key"
        final Metadata thirdMeta = new Metadata();
        thirdMeta.setId(rand.nextLong());
        final SimpleMetadata itemsThirdMeta = new SimpleMetadata(thirdMap);
        thirdMeta.setKeyValuePropertyFromSimpleMetadata(itemsThirdMeta);
        metaList.add(thirdMeta);
        
        // act
        final List<Long> result = metadataService.getIdsOfMatchingMetadatas(metaList, requestList);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(firstMeta.getId()));
        Assert.assertTrue(result.contains(thirdMeta.getId()));
    }

    // test that the various find methods return correct result for a Metadata object not being SENT
    @Test
    public void testFindMetadataMethods_MetadataNotSent() throws URISyntaxException {

        final String SystemId = Authorities.EdiT;

        final Document doc = new Document(new URI("leos://5"), "title5");
        documentRepos.save(doc);

        final Group group = new Group("thegroup2", false);
        groupRepos.save(group);

        final Metadata metaNotSent = new Metadata(doc, group, SystemId);
        metaNotSent.setResponseStatus(ResponseStatus.IN_PREPARATION);
        metadataRepos.save(metaNotSent);

        // functions returning matched object
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentGroupSystemid(doc, group, SystemId).size());
        Assert.assertEquals(0, metadataService.findMetadataOfDocumentGroupSystemidSent(doc, group, SystemId).size());

        // functions returning lists of matched objects - never return null, but at least empty list
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentSystemidGroupIds(doc, SystemId, Arrays.asList(group.getId())).size());
        Assert.assertEquals(0, metadataService.findMetadataOfDocumentSystemidGroupIds(doc, SystemId, Arrays.asList(group.getId() + 4)).size()); // ID unknown
        Assert.assertEquals(0, metadataService.findMetadataOfDocumentSystemidSent(doc, SystemId).size());
    }

    // test that the various find methods return correct result for a Metadata object being SENT
    @Test
    public void testFindMetadataMethods_MetadataSent() throws URISyntaxException {

        final String SystemId = Authorities.EdiT;

        final Document doc = new Document(new URI("leos://6"), "title6");
        documentRepos.save(doc);

        final Group group = new Group("othergroup", false);
        groupRepos.save(group);

        final Metadata metaNotSent = new Metadata(doc, group, SystemId);
        metaNotSent.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(metaNotSent);

        // functions returning matched object
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentGroupSystemid(doc, group, SystemId).size()); // SENT status does not matter
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentGroupSystemidSent(doc, group, SystemId).size());

        // functions returning lists of matched objects - never return null, but at least empty list
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentSystemidGroupIds(doc, SystemId, Arrays.asList(group.getId())).size());
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentSystemidGroupIds(doc, SystemId,
                Arrays.asList(group.getId(), group.getId() + 4)).size()); // some unknown
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentSystemidSent(doc, SystemId).size());
    }

    // test that calling one of the find methods with an empty list is prohibited
    @Test(expected = IllegalArgumentException.class)
    public void testFindMetadataOfDocumentSystemidGroupIds_NoEmptyListAccepted() throws Exception {

        final Document document = new Document(new URI("leos://1"), "title1");
        documentRepos.save(document);

        final Group group = new Group("mygroup", false);
        groupRepos.save(group);

        metadataService.findMetadataOfDocumentSystemidGroupIds(document, "system", null);
    }

    // tests retrieval of Metadata without other reference metadata
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testFindExactMetadataWithoutReferenceMetadata() throws Exception {

        final String systemId = "syst";
        final Document document = new Document(new URI("leos://2"), "title2");
        documentRepos.save(document);

        final Group group = new Group("theirgroup", false);
        groupRepos.save(group);

        final Metadata metaToSave = new Metadata(document, group, systemId);
        metaToSave.setKeyValuePairs("someprop:5");
        metadataService.saveMetadata(metaToSave);

        final Metadata nullMeta = null;
        Metadata readMeta = metadataService.findExactMetadata(document, group, systemId, nullMeta);
        Assert.assertNull(readMeta); // currently, all saved metadata have at least some properties -> no exact match

        // now add a metadata set without further properties
        final Metadata metaToSave2 = new Metadata(document, group, systemId);
        metadataService.saveMetadata(metaToSave2);

        readMeta = metadataService.findExactMetadata(document, group, systemId, nullMeta);
        Assert.assertNotNull(readMeta); // now there is a metadata without further properties -> exact match
    }

    // tests retrieval of Metadata with reference metadata containing more properties
    @Test
    public void testFindExactMetadataWithReferenceContainingMoreProps() throws Exception {

        final String systemId = "sys";
        final Document document = new Document(new URI("leos://3"), "title3");
        documentRepos.save(document);

        final Group group = new Group("thisgroup", false);
        groupRepos.save(group);

        final Metadata metaToSave = new Metadata(document, group, systemId);
        metaToSave.setKeyValuePairs("someprop:5");
        metadataService.saveMetadata(metaToSave);

        final Metadata refMeta = new Metadata(document, group, systemId);
        refMeta.setKeyValuePairs("someprop:5\ntheId:\"xyz\"");

        // test and verify: reference has more properties than DB content metadata -> no exact match!
        final Metadata readMeta = metadataService.findExactMetadata(document, group, systemId, refMeta);
        Assert.assertNull(readMeta);
    }

    // tests retrieval of Metadata with reference metadata containing less properties -> no match
    @Test
    public void testFindExactMetadataWithReferenceContainingLessProps() throws Exception {

        final String systemId = "sys";
        final Document document = new Document(new URI("leos://8"), "title8");
        documentRepos.save(document);

        final Group group = new Group("ourgroup", false);
        groupRepos.save(group);

        final Metadata metaToSave = new Metadata(document, group, systemId);
        metaToSave.setKeyValuePairs("someprop:5\ntheId:\"xyz\"\nother:8");
        metadataService.saveMetadata(metaToSave);

        final Metadata refMeta = new Metadata(document, group, systemId);
        refMeta.setKeyValuePairs("someprop:5");

        // test and verify: reference has less properties than DB content metadata -> no exact match!
        final Metadata readMeta = metadataService.findExactMetadata(document, group, systemId, refMeta);
        Assert.assertNull(readMeta);
    }

    // tests retrieval of Metadata with reference metadata containing the same properties -> match!
    @Test
    public void testFindExactMetadataWithReferenceContainingSameProps() throws Exception {

        final String systemId = "sys";
        final Document document = new Document(new URI("leos://5"), "title5");
        documentRepos.save(document);

        final Group group = new Group("thegroup", false);
        groupRepos.save(group);

        final Metadata metaToSave = new Metadata(document, group, systemId);
        metaToSave.setKeyValuePairs("someprop:5\ntheId:\"xyz\"\nlast:\"first\"");
        metadataService.saveMetadata(metaToSave);

        final Metadata refMeta = new Metadata(document, group, systemId);
        refMeta.setKeyValuePairs("last:\"first\"\nsomeprop:5\ntheId:\"xyz\""); // same properties, but ordered differently

        // test and verify: reference has same properties than DB content metadata -> exact match!
        final Metadata readMeta = metadataService.findExactMetadata(document, group, systemId, refMeta);
        Assert.assertNotNull(readMeta);
    }

    // test retrieval of Metadata returns {@literal null} if mandatory fields are missing
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testFindExactMetadataWithInvalidParameters() {

        final Metadata nullMeta = null;

        Assert.assertNull(metadataService.findExactMetadata(null, null, null, nullMeta));

        Assert.assertNull(metadataService.findExactMetadata(new Document(), null, null, nullMeta));

        Assert.assertNull(metadataService.findExactMetadata(new Document(), new Group(), null, nullMeta));

        Assert.assertNull(metadataService.findExactMetadata(new Document(), new Group(), "", nullMeta));
    }

    // -------------------------------------
    // Tests for updating metadata status
    // -------------------------------------

    // status update is not executed without request parameters
    @Test(expected = IllegalArgumentException.class)
    public void updateMetadataWithoutRequestParameters() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("", "");
        metadataService.updateMetadata(null, userInfo);
    }

    // status update is not executed without required user information
    @Test(expected = CannotUpdateAnnotationStatusException.class)
    public void updateMetadataWithoutUser() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        metadataService.updateMetadata(null, null);
    }

    // status update not executed without group set in request parameters
    @Test(expected = IllegalArgumentException.class)
    public void updateMetadataWithoutRequestGroup() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("", "");
        final StatusUpdateRequest sur = new StatusUpdateRequest();
        metadataService.updateMetadata(sur, userInfo);
    }

    // status update not executed without uri set in request parameters
    @Test(expected = IllegalArgumentException.class)
    public void updateMetadataWithoutRequestUri() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("", "");
        final StatusUpdateRequest sur = new StatusUpdateRequest();
        sur.setGroup("somegroups");
        metadataService.updateMetadata(sur, userInfo);
    }

    // status update not executed when user is not an ISC user
    @Test(expected = MissingPermissionException.class)
    public void updateMetadata_MissingPermission() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("", Authorities.EdiT); // LEOS/EdiT user may not launch the update
        final StatusUpdateRequest sur = new StatusUpdateRequest("somegroup", "http://dummy2", ResponseStatus.IN_PREPARATION);

        // update request should be refused
        metadataService.updateMetadata(sur, userInfo);
    }

    // status update not executed when referenced document is not contained in database
    @Test(expected = CannotUpdateAnnotationStatusException.class)
    public void updateMetadata_DocumentUnknown() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("", Authorities.ISC);
        final StatusUpdateRequest sur = new StatusUpdateRequest("somegroup", "http://dummy2", ResponseStatus.IN_PREPARATION);

        // document is not saved in documentRepos
        metadataService.updateMetadata(sur, userInfo);
    }

    // status update not executed when referenced group is not contained in database
    @Test(expected = CannotUpdateAnnotationStatusException.class)
    public void updateMetadata_GroupUnknown() throws URISyntaxException, CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("me", Authorities.ISC);
        final String uri = "http://dummy";
        final StatusUpdateRequest sur = new StatusUpdateRequest("somegroup", uri, ResponseStatus.IN_PREPARATION);

        documentRepos.save(new Document(new URI(uri), "title"));

        // group is not saved in groupRepos
        metadataService.updateMetadata(sur, userInfo);
    }

    // status update not executed when no matching metadata is not contained in database
    @Test(expected = CannotUpdateAnnotationStatusException.class)
    public void updateMetadata_MetadataUnknown() throws URISyntaxException, CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("login", Authorities.ISC);
        final String uri = "http://dummy";
        final String groupName = "thegroup";
        final StatusUpdateRequest sur = new StatusUpdateRequest(groupName, uri, ResponseStatus.IN_PREPARATION);

        documentRepos.save(new Document(new URI(uri), "title"));
        groupRepos.save(new Group(groupName, false));

        // metadata is not saved in metadataRepos
        metadataService.updateMetadata(sur, userInfo);
    }

    // status update fails as no matching metadata found
    @Test(expected = CannotUpdateAnnotationStatusException.class)
    public void updateMetadata_NotFound() throws MissingPermissionException, URISyntaxException, CannotUpdateAnnotationStatusException {

        final List<Object> prep = prepareMetadataUpdateSuccessfulTest();
        final StatusUpdateRequest sur = (StatusUpdateRequest) prep.get(0);
        final UserInformation userInfo = (UserInformation) prep.get(1);

        metadataService.updateMetadata(sur, userInfo);
    }

    // status update executed correctly for a single metadata set
    @Test
    public void updateMetadataSuccessful_SingleMetadata() throws MissingPermissionException, URISyntaxException, CannotUpdateAnnotationStatusException {

        final List<Object> prep = prepareMetadataUpdateSuccessfulTest();
        final StatusUpdateRequest sur = (StatusUpdateRequest) prep.get(0);
        final UserInformation userInfo = (UserInformation) prep.get(1);
        final Metadata meta = (Metadata) prep.get(2);
        final User user = (User) prep.get(3);

        // also set the responseStatus of the database metadata -> then there is a full match
        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);
        metadataRepos.save(meta);

        final List<Long> updateResult = metadataService.updateMetadata(sur, userInfo);
        Assert.assertEquals(1, updateResult.size());
        Assert.assertEquals(meta.getId(), updateResult.get(0).longValue());

        final Metadata readMetadata = metadataRepos.findOne(meta.getId());
        Assert.assertTrue(TestHelper.withinLastSeconds(readMetadata.getResponseStatusUpdated(), 5));
        Assert.assertEquals(user.getId(), readMetadata.getResponseStatusUpdatedBy());

        // add another item to the database, which has even more properties -> should also be found using same update request
    }

    // status update executed correctly for several metadata sets
    @Test
    public void updateMetadataSuccessful_SeveralMetadata() throws MissingPermissionException, URISyntaxException, CannotUpdateAnnotationStatusException {

        final List<Object> prep = prepareMetadataUpdateSuccessfulTest();
        final StatusUpdateRequest sur = (StatusUpdateRequest) prep.get(0);
        final UserInformation userInfo = (UserInformation) prep.get(1);
        final Metadata meta = (Metadata) prep.get(2);

        // also set the responseStatus of the database metadata -> then there is a full match
        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);
        metadataRepos.save(meta);

        // add another item to the database, which has even more properties
        // -> should also be found using same update request
        final Metadata secondMeta = new Metadata(meta);
        final SimpleMetadata kvpSecond = secondMeta.getKeyValuePropertyAsSimpleMetadata();
        kvpSecond.put("anotherprop", "anotherval");
        secondMeta.setKeyValuePropertyFromSimpleMetadata(kvpSecond);
        metadataRepos.save(secondMeta);

        // add a third item which has different response status -> should not match!
        final Metadata thirdMeta = new Metadata(meta);
        thirdMeta.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(thirdMeta);

        // launch update request - only meta and secondMeta should match
        final List<Long> updateResult = metadataService.updateMetadata(sur, userInfo);
        Assert.assertEquals(2, updateResult.size());
        Assert.assertTrue(updateResult.stream().anyMatch(metaId -> metaId.equals(meta.getId())));
        Assert.assertTrue(updateResult.stream().anyMatch(metaId -> metaId.equals(secondMeta.getId())));
    }

    private List<Object> prepareMetadataUpdateSuccessfulTest() throws URISyntaxException {

        final String authority = Authorities.ISC;
        final String login = "login";
        final UserInformation userInfo = new UserInformation(login, authority);
        final String uri = "http://dummy";
        final String groupName = "thegroup";
        final String ISC_REF = "ISCReference";
        final String ISC_REF_VAL = "ISC/2018/421";

        final SimpleMetadata metadataToMatch = new SimpleMetadata();
        metadataToMatch.put(ISC_REF, ISC_REF_VAL);
        metadataToMatch.put(RESP_STAT, "IN_PREPARATION");

        final StatusUpdateRequest sur = new StatusUpdateRequest(groupName, uri, ResponseStatus.SENT);
        sur.setMetadataToMatch(metadataToMatch);

        // save data that does not yet fully match
        final Document doc = new Document(new URI(uri), "title");
        documentRepos.save(doc);
        final Group group = new Group(groupName, false);
        groupRepos.save(group);

        final Metadata meta = new Metadata(doc, group, authority);
        meta.setKeyValuePairs(ISC_REF + ":" + ISC_REF_VAL);
        metadataRepos.save(meta);

        final User user = new User(login);
        userRepos.save(user);
        userInfo.setUser(user);

        return Arrays.asList(sur, userInfo, meta, user); // ugly; nice Tuple classes could be great...
    }
}
