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
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.repository.*;
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

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
public class MetadataMatchingServiceTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private MetadataService metadataService;

    @Autowired
    private MetadataMatchingService metadataMatchingService;

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private DocumentRepository documentRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    private Group defaultGroup;

    private static final String SYS_ID = "systemId";
    private static final String VERSION = "version";
    private static final String DOCURI = "http://leos/1";

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

    // simple test for successful comparison of HashMap items with database metadata object
    @Test
    public void testAllMetadataContainedInDbObject_Successful() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());
        kvPairs.put("whatevery", "helloToo");
        kvPairs.put("someinteger", "5");
        kvPairs.put(SYS_ID, "someauth");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("someauth");
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();

        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // simple test for comparison of HashMap items with database metadata object
    // database does not contain the wanted version property
    @Test
    public void testAllMetadataContainedInDbObjectWithWrongVersion() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());
        kvPairs.put("whatever", "hello");
        kvPairs.put("someint", "5");
        kvPairs.put(SYS_ID, "someauthor");
        kvPairs.put(VERSION, "2.0");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("someauthor");
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();

        // first test: should be a match
        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));

        // now change the version in the database -> no full match any more
        kvPairs.put(VERSION, "1.0");
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);
        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));

        // change the query data to be <=2.0 for the version -> should match
        kvPairsCopy.put(VERSION, "<=2.0");
        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // a certain version number is requested, but DB item does not contain it
    @Test
    public void testAllMetadataContainedInDbObject_VersionRequiredButNotInDb() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(VERSION, "2.0");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("someauthority");

        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));
    }

    // a certain version number is requested, but DB item contains a higher version number
    @Test
    public void testAllMetadataContainedInDbObject_VersionUpToNotInDb() {

        final SimpleMetadata kvPairsRequested = new SimpleMetadata();
        kvPairsRequested.put(VERSION, "<=2.0");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("someauthority");
        dbMetadata.setVersion("3.0");

        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsRequested, dbMetadata));
    }

    // simple test for comparing incomplete objects
    @Test
    public void testAllMetadataContainedInDbObject_EmptyObjects() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        final Metadata dbMetadata = new Metadata();

        // no requirements given -> ok
        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(null, dbMetadata));
        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(null, null));

        // empty requirements -> ok
        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairs, null));

        // both empty -> equal
        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));

        // requirements present, but DB empty -> fail
        kvPairs.put("something", "content");
        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairs, null));
    }

    // simple test for comparison of HashMap items with database metadata object: HashMap contains more objects than DB item
    @Test
    public void testAllMetadataContainedInDbObject_MapRequiresMore() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());
        kvPairs.put("whatever", "hello");
        kvPairs.put("someint", "5");
        kvPairs.put(SYS_ID, "thesystem");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();
        kvPairsCopy.put("onemore", "item"); // add one more item in map, but is not contained in DB object

        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // simple test for comparison of HashMap items with database metadata object: database object contains more objects
    @Test
    public void testAllMetadataContainedInDbObject_DbHasMore() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.SENT.toString());
        kvPairs.put("whatever", "hello");
        kvPairs.put("someint", "5");
        kvPairs.put("onemore", "item"); // add one more item in map, is contained in DB object

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        final SimpleMetadata kvPairsCopy = (SimpleMetadata) kvPairs.clone();
        kvPairsCopy.remove("onemore"); // remove the item from the map of required items -> should still match

        Assert.assertTrue(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairsCopy, dbMetadata));
    }

    // test that Hashmap contains a system ID, but database object does not having one
    @Test
    public void testAllMetadataContainedInDbObject_NoSystemidInDbObject() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(SYS_ID, "mysys");

        final Metadata dbMetadata = new Metadata();

        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));
    }

    // test that Hashmap contains a system ID, but database object contains a different one
    @Test
    public void testAllMetadataContainedInDbObject_SystemidInDbObjectDifferent() {

        final SimpleMetadata kvPairs = new SimpleMetadata();
        kvPairs.put(SYS_ID, "mysys");

        final Metadata dbMetadata = new Metadata();
        dbMetadata.setSystemId("yoursys");

        Assert.assertFalse(metadataMatchingService.areAllMetadataContainedInDbMetadata(kvPairs, dbMetadata));
    }

    // test the getIdsOfMatchingMetadatas method when no candidates are given (i.e. {@literal null})
    @Test
    public void testGetIdsOfMatchingMetadatas_candidatesNull() {

        final SimpleMetadata request = new SimpleMetadata();
        final List<SimpleMetadata> requestList = Arrays.asList(request);

        Assert.assertNull(metadataMatchingService.getIdsOfMatchingMetadatas(null, requestList));
    }

    // test the getIdsOfMatchingMetadatas method when no candidates are given (i.e. {@literal null})
    @Test
    public void testGetIdsOfMatchingMetadatas_candidatesNull2() {

        final SimpleMetadata request = new SimpleMetadata();

        Assert.assertNull(metadataMatchingService.getIdsOfMatchingMetadatas(null, request));
    }

    // test the getIdsOfMatchingMetadatas method when no candidates are given (i.e. empty list)
    @Test
    public void testGetIdsOfMatchingMetadatas_noCandidates() {

        final SimpleMetadata request = new SimpleMetadata();
        final List<SimpleMetadata> requestList = Arrays.asList(request);

        final List<Metadata> metaList = new ArrayList<Metadata>();
        Assert.assertNull(metadataMatchingService.getIdsOfMatchingMetadatas(metaList, requestList));
    }

    // test the getIdsOfMatchingMetadatas method when no candidates are given (i.e. empty list), but request are given (including version)
    @Test
    public void testGetIdsOfMatchingMetadatas_noCandidatesButRequests() {

        final SimpleMetadata request = new SimpleMetadata();
        request.put("item1", "value1");
        request.put("version", "1.0");
        final List<SimpleMetadata> requestList = Arrays.asList(request);

        final List<Metadata> metaList = new ArrayList<Metadata>();
        Assert.assertNull(metadataMatchingService.getIdsOfMatchingMetadatas(metaList, requestList));
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
        final List<Long> result = metadataMatchingService.getIdsOfMatchingMetadatas(metaList, metaDummy);
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

        final List<Long> result = metadataMatchingService.getIdsOfMatchingMetadatas(metaList, requestList);
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

        final List<Long> result = metadataMatchingService.getIdsOfMatchingMetadatas(metaList, requestList);
        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(randomMeta.getId(), result.get(0).longValue());
    }

    // test the getIdsOfMatchingMetadatas method
    // should return two of the three candidates
    @Test
    public void testGetIdsOfMatchingMetadatas() {

        final String COMMON_KEY = "common";
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
        final List<Long> result = metadataMatchingService.getIdsOfMatchingMetadatas(metaList, requestList);
        Assert.assertNotNull(result);
        Assert.assertEquals(2, result.size());
        Assert.assertTrue(result.contains(firstMeta.getId()));
        Assert.assertTrue(result.contains(thirdMeta.getId()));
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
        Metadata readMeta = metadataMatchingService.findExactMetadata(document, group, systemId, nullMeta);
        Assert.assertNull(readMeta); // currently, all saved metadata have at least some properties -> no exact match

        // now add a metadata set without further properties
        final Metadata metaToSave2 = new Metadata(document, group, systemId);
        metadataService.saveMetadata(metaToSave2);

        readMeta = metadataMatchingService.findExactMetadata(document, group, systemId, nullMeta);
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
        final Metadata readMeta = metadataMatchingService.findExactMetadata(document, group, systemId, refMeta);
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
        final Metadata readMeta = metadataMatchingService.findExactMetadata(document, group, systemId, refMeta);
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
        final Metadata readMeta = metadataMatchingService.findExactMetadata(document, group, systemId, refMeta);
        Assert.assertNotNull(readMeta);
    }

    // test retrieval of Metadata returns {@literal null} if mandatory fields are missing
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testFindExactMetadataWithInvalidParameters() {

        final Metadata nullMeta = null;

        Assert.assertNull(metadataMatchingService.findExactMetadata(null, null, null, nullMeta));

        Assert.assertNull(metadataMatchingService.findExactMetadata(new Document(), null, null, nullMeta));

        Assert.assertNull(metadataMatchingService.findExactMetadata(new Document(), new Group(), null, nullMeta));

        Assert.assertNull(metadataMatchingService.findExactMetadata(new Document(), new Group(), "", nullMeta));
    }

    // -------------------------------------
    // Tests for matching metadata while ignoring responseVersion
    // -------------------------------------

    // no document specified - should throw
    @Test(expected = IllegalArgumentException.class)
    public void findExactMetadataWithoutResponseVersion_noDocument() {

        metadataMatchingService.findExactMetadataWithoutResponseVersion(null, null, null, null);
    }

    // no group specified - should throw
    @Test(expected = IllegalArgumentException.class)
    public void findExactMetadataWithoutResponseVersion_noGroup() {

        metadataMatchingService.findExactMetadataWithoutResponseVersion(new Document(), null, null, null);
    }

    // no authority specified - should throw
    @Test(expected = IllegalArgumentException.class)
    public void findExactMetadataWithoutResponseVersion_noAuthority() {

        metadataMatchingService.findExactMetadataWithoutResponseVersion(new Document(), new Group(), null, null);
    }

    // no other metadata specified - should not return a result (= {@literal null})
    @Test
    public void findExactMetadataWithoutResponseVersion_noOtherMetadata() {

        final Document doc = documentRepos.save(new Document(URI.create("file://LEOS/2"), "doctitle"));
        Assert.assertNull(metadataMatchingService.findExactMetadataWithoutResponseVersion(doc, defaultGroup, Authorities.ISC, null));
    }

    // other metadata specified is empty - should not return a result (= {@literal null})
    @Test
    public void findExactMetadataWithoutResponseVersion_emptyOtherMetadata() {

        final Document doc = documentRepos.save(new Document(URI.create("file://LEOS/2"), "doctitle"));
        Assert.assertNull(metadataMatchingService.findExactMetadataWithoutResponseVersion(doc, defaultGroup, Authorities.ISC, new Metadata()));
    }

    // matches are found
    @Test
    public void findExactMetadataWithoutResponseVersion_matches() {

        final Document doc = documentRepos.save(new Document(URI.create("file://LEOS/3"), "thetitle"));

        // save two metadata that will match, and a third that will not match
        final Metadata savedMeta1 = new Metadata(doc, defaultGroup, Authorities.ISC);
        savedMeta1.setKeyValuePairs("responseId:SG\nresponseVersion:4");
        metadataRepos.save(savedMeta1);

        final Metadata savedMeta2 = new Metadata(doc, defaultGroup, Authorities.ISC);
        savedMeta2.setKeyValuePairs("responseId:SG\nresponseVersion:3");
        metadataRepos.save(savedMeta2);

        final Metadata savedMeta3 = new Metadata(doc, defaultGroup, Authorities.ISC);
        savedMeta3.setKeyValuePairs("responseId:AGRI\nresponseVersion:3"); // no match due to different responseId
        metadataRepos.save(savedMeta3);

        // ask with similar metadata only differing in responseVersion
        final Metadata otherMetadata = new Metadata(doc, defaultGroup, Authorities.ISC);
        otherMetadata.setKeyValuePairs("responseId:SG\nresponseVersion:2"); // different reponseVersion -> should not disturb (will be ignored)

        // act
        final List<Long> foundMetas = metadataMatchingService.findExactMetadataWithoutResponseVersion(doc, defaultGroup, Authorities.ISC, otherMetadata);

        // verify
        Assert.assertNotNull(foundMetas);
        Assert.assertEquals(2, foundMetas.size());
        Assert.assertEquals(savedMeta1.getId(), foundMetas.get(0).longValue()); // in particular, the IDs are not negative
        Assert.assertEquals(savedMeta2.getId(), foundMetas.get(1).longValue());
    }

    // no match is found
    @Test
    public void findExactMetadataWithoutResponseVersion_noMatch() {

        final Document doc = documentRepos.save(new Document(URI.create("file://LEOS/3"), "thetitle"));

        final Metadata savedMeta = new Metadata(doc, defaultGroup, Authorities.ISC);
        savedMeta.setKeyValuePairs("responseId:SG\nresponseVersion:4\nISCReference:ISC/2019/8");
        metadataRepos.save(savedMeta);

        // ask with similar metadata only differing in responseVersion
        final Metadata otherMetadata = new Metadata(doc, defaultGroup, Authorities.ISC);

        // different reponseVersion -> should not disturb (will be ignored), but ISCReference is missing -> no match
        otherMetadata.setKeyValuePairs("responseId:SG\nresponseVersion:2");

        // act
        final List<Long> foundMetas = metadataMatchingService.findExactMetadataWithoutResponseVersion(doc, defaultGroup, Authorities.ISC, otherMetadata);

        // verify
        Assert.assertNotNull(foundMetas);
        Assert.assertEquals(0, foundMetas.size());
    }

    // -------------------------------------
    // Tests for computing the highest available responseVersion
    // -------------------------------------

    // an EdiT annotation doesn't have a responseVersion
    @Test
    public void testHighestResponseVersion_EditAnnotation() {

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        meta.setSystemId(Authorities.EdiT);
        annot.setMetadata(meta);

        Assert.assertEquals((long) -1, metadataMatchingService.getHighestResponseVersion(annot));
    }

    // retrieval of highest responseVersion does not find matching metadata
    @Test
    public void testHighestResponseVersion_noMatchingAnnotation() {

        final Document doc = new Document(URI.create(DOCURI), "");
        documentRepos.save(doc);

        final Group group = new Group("thegroup", true);
        groupRepos.save(group);

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        meta.setSystemId(Authorities.ISC);
        meta.setDocument(doc);
        meta.setGroup(group);
        annot.setMetadata(meta);

        Assert.assertEquals((long) -1, metadataMatchingService.getHighestResponseVersion(annot));
    }

    // retrieval of highest responseVersion finds matching metadata being IN_PREPARATION
    @Test
    public void testHighestResponseVersion_inPreparation() {

        final Document doc = new Document(URI.create(DOCURI), "");
        documentRepos.save(doc);

        final Group group = new Group("thegroup2", true);
        groupRepos.save(group);

        // save a metadata set being SENT, responseVersion 1
        // and one metadata set being IN_PREPARATION, responseVersion 2
        // -> the set being IN_PREPARATION takes precedence
        final Metadata metaSent = new Metadata();
        metaSent.setSystemId(Authorities.ISC);
        metaSent.setDocument(doc);
        metaSent.setGroup(group);
        metaSent.setKeyValuePairs("responseVersion:1\nresponseId:SG");
        metaSent.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(metaSent);

        final Metadata metaInPrep = new Metadata();
        metaInPrep.setSystemId(Authorities.ISC);
        metaInPrep.setDocument(doc);
        metaInPrep.setGroup(group);
        metaInPrep.setKeyValuePairs("responseVersion:2\nresponseId:SG");
        metaInPrep.setResponseStatus(Metadata.ResponseStatus.IN_PREPARATION);
        metadataRepos.save(metaInPrep);

        final Annotation annot = new Annotation();
        annot.setMetadata(metaSent); // any metadata is assigned to the annotation

        // responseVersion 2 of the IN_PREPARATION item is used
        Assert.assertEquals((long) 2, metadataMatchingService.getHighestResponseVersion(annot));
    }

    // retrieval of highest responseVersion finds matching metadata but without a response status
    @Test
    public void testHighestResponseVersion_matchWithoutResponseStatus() {

        final Document doc = new Document(URI.create(DOCURI), "");
        documentRepos.save(doc);

        final Group group = new Group("thegroup3", true);
        groupRepos.save(group);

        // save a metadata set, responseVersion 1 but without response status
        final Metadata meta = new Metadata();
        meta.setSystemId(Authorities.ISC);
        meta.setDocument(doc);
        meta.setGroup(group);
        meta.setKeyValuePairs("responseVersion:1\nresponseId:SG");
        metadataRepos.save(meta);

        final Annotation annot = new Annotation();
        annot.setMetadata(meta); // any metadata is assigned to the annotation

        // matching metadata found, but without response status -> new response must be version 1
        Assert.assertEquals((long) 1, metadataMatchingService.getHighestResponseVersion(annot));
    }

    // retrieval of highest responseVersion finds matching metadata being SENT,
    // and no annotations having a responseVersionSentDeleted set
    @Test
    public void testHighestResponseVersion_sent() {

        final Document doc = new Document(URI.create(DOCURI), "");
        documentRepos.save(doc);

        final Group group = new Group("thegroup4", true);
        groupRepos.save(group);

        // save a metadata set being SENT, responseVersion 4
        // -> new response version must be 5
        final Metadata metaSent = new Metadata();
        metaSent.setSystemId(Authorities.ISC);
        metaSent.setDocument(doc);
        metaSent.setGroup(group);
        metaSent.setKeyValuePairs("responseVersion:4\nresponseId:SG");
        metaSent.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(metaSent);

        final Annotation annot = new Annotation();
        annot.setMetadata(metaSent); // any metadata is assigned to the annotation

        Assert.assertEquals((long) 5, metadataMatchingService.getHighestResponseVersion(annot));
    }

    // retrieval of highest responseVersion finds matching metadata being SENT, but also
    // annotations that are sentDeleted and DELETED with higher responseVersion
    // (thus have responseVersionSentDeleted set)
    // -> annotations' responseVersionSentDeleted takes precedence
    @Test
    public void testHighestResponseVersion_sentAndDeleted() {

        final Document doc = new Document(URI.create(DOCURI), "");
        documentRepos.save(doc);

        final Group group = new Group("thegroup6", true);
        groupRepos.save(group);

        // save a metadata set being SENT, responseVersion 1
        final Metadata metaSent = new Metadata();
        metaSent.setSystemId(Authorities.ISC);
        metaSent.setDocument(doc);
        metaSent.setGroup(group);
        metaSent.setKeyValuePairs("responseVersion:1\nresponseId:SG");
        metaSent.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(metaSent);

        final User user = new User("jane");
        userRepos.save(user);

        // save an annotation assigned to this metadata, but being sentDeleted and DELETED
        final Annotation annotSent = new Annotation();
        annotSent.setMetadata(metaSent);
        annotSent.setUser(user);
        annotSent.setSentDeleted(true);
        annotSent.setStatus(AnnotationStatus.DELETED);
        annotSent.setRespVersionSentDeleted((long) 4);
        annotSent.setId("someid");
        annotSent.setCreated(LocalDateTime.now());
        annotSent.setUpdated(LocalDateTime.now());
        annotSent.setText("my annot");
        annotSent.setTargetSelectors("a");
        annotRepos.save(annotSent);

        // ask for the status of a new annotation
        final Annotation annot = new Annotation();
        annot.setMetadata(metaSent);

        // the annotation's responseVersionSentDeleted item wins and is increased
        Assert.assertEquals((long) 5, metadataMatchingService.getHighestResponseVersion(annot));
    }
    
    // test various filters for different ISC references on a given list of metadata items
    @Test
    public void testFilterIscReference() {
        
        final List<Metadata> metaList = new ArrayList<Metadata>();
        
        final Metadata meta1 = new Metadata();
        final SimpleMetadata simpleMeta1 = new SimpleMetadata(Metadata.PROP_ISC_REF, "ISC/1");
        meta1.setKeyValuePropertyFromSimpleMetadata(simpleMeta1);
        metaList.add(meta1);
        
        final Metadata meta1_2 = new Metadata(meta1);
        metaList.add(meta1_2);
        
        final Metadata meta2 = new Metadata();
        final SimpleMetadata simpleMeta2 = new SimpleMetadata(Metadata.PROP_ISC_REF, "ISC/2");
        meta2.setKeyValuePropertyFromSimpleMetadata(simpleMeta2);
        metaList.add(meta2);
        
        final Metadata meta3 = new Metadata();
        metaList.add(meta3);
        
        List<Metadata> filtered = metadataMatchingService.filterByIscReference(metaList, "ISC/1");
        Assert.assertEquals(2, filtered.size());
        
        filtered = metadataMatchingService.filterByIscReference(metaList, "ISC/2");
        Assert.assertEquals(1, filtered.size());
        
        filtered = metadataMatchingService.filterByIscReference(metaList, "ISC/3");
        Assert.assertEquals(0, filtered.size());
    }
    
    // filter an empty list of Metadata
    @Test
    public void testFilterIscReference_emptyList() {
        
        final List<Metadata> metaList = new ArrayList<Metadata>();
        
        final List<Metadata> filtered = metadataMatchingService.filterByIscReference(metaList, "ISC/4");
        Assert.assertEquals(0, filtered.size());
    }
}
