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

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
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
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings({"PMD.TooManyMethods", "PMD.GodClass"})
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

    private static final String SYS_ID = "systemId";
    private static final String VERSION = "version";

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
        kvPairs.put(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString());
        kvPairs.put(VERSION, "1.1");
        kvPairs.put("someProp", "08/15");
        kvPairs.put("propWithQuote", propWithQuotes);
        meta.setKeyValuePropertyFromSimpleMetadata(kvPairs);

        // verify that the kvPairs were chunked as expected
        Assert.assertEquals(newSystemId, meta.getSystemId());
        Assert.assertEquals(Metadata.ResponseStatus.IN_PREPARATION, meta.getResponseStatus());
        Assert.assertTrue(meta.getKeyValuePairs().contains("someProp:08/15"));
        Assert.assertTrue(meta.getKeyValuePairs().contains("propWithQuote:" + propWithQuotes));
        Assert.assertEquals(meta.getVersion(), "1.1");
        Assert.assertEquals(expectedConcatKVPairs, meta.getKeyValuePairs());

        // verify that asking for the HashMap assembles it again correctly,
        // including the redistributed properties not saved in the "keyValuePairs" property
        final SimpleMetadata retrieved = meta.getKeyValuePropertyAsSimpleMetadata();
        Assert.assertNotNull(retrieved);
        Assert.assertEquals(2, retrieved.keySet().size());
        Assert.assertEquals("08/15", retrieved.get("someProp"));
        Assert.assertEquals(propWithQuotes, retrieved.get("propWithQuote"));
        // note: systemId, version and responseStatus are not contained!

        // verify that when asking FOR ALL items, systemId and responseStatus are contained
        final SimpleMetadata retrievedAll = meta.getAllMetadataAsSimpleMetadata();
        Assert.assertNotNull(retrievedAll);
        Assert.assertEquals(5, retrievedAll.keySet().size());
        Assert.assertEquals("08/15", retrievedAll.get("someProp"));
        Assert.assertEquals(propWithQuotes, retrievedAll.get("propWithQuote"));
        Assert.assertEquals(newSystemId, retrievedAll.get(SYS_ID));
        Assert.assertEquals("IN_PREPARATION", retrievedAll.get(Metadata.PROP_RESPONSE_STATUS));
        Assert.assertEquals("1.1", retrievedAll.get(VERSION));
    }

    // test behavior when invalid (or only partially valid/available) metadata is processed
    @Test
    public void testDistributionOfInvalidKeyValuePairs() throws URISyntaxException {

        // insert some metadata into the HashMap, parts of them will be redistributed
        final Metadata meta = new Metadata(null, defaultGroup, "");
        final SimpleMetadata kvPairs = new SimpleMetadata();
        // no systemId!
        kvPairs.put(Metadata.PROP_RESPONSE_STATUS, "UNKNOWN_VALUE"); // will raise internal enum conversion error
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
        Assert.assertEquals(1, metadataService.findMetadataOfDocumentGroupSystemidInPreparation(doc, group, SystemId).size());
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
        Assert.assertEquals(0, metadataService.findMetadataOfDocumentGroupSystemidInPreparation(doc, group, SystemId).size());
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

    // test that searching for metadata without an existing document returns an empty list
    @Test
    public void testFindMetadata_NoDocument() {

        final List<Metadata> items = metadataService.findMetadataOfDocumentGroupSystemid("http://LEOS/2", null, "");
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size());
    }

    // test that searching for metadata without an existing group returns an empty list
    @Test
    public void testFindMetadata_NoGroup() {

        final Document document = new Document(URI.create("leos://1"), "title1");
        documentRepos.save(document);

        final List<Metadata> items = metadataService.findMetadataOfDocumentGroupSystemid(document.getUri(), "dontknow", "");
        Assert.assertNotNull(items);
        Assert.assertEquals(0, items.size());
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

        // document and group exist in DB
        final String DOCURI = "http://dummy2";
        final String GROUPNAME = "usergroup";
        documentRepos.save(new Document(URI.create(DOCURI), "hello"));
        groupRepos.save(new Group(GROUPNAME, true));

        final UserInformation userInfo = new UserInformation("", Authorities.EdiT); // LEOS/EdiT user may not launch the update
        final StatusUpdateRequest sur = new StatusUpdateRequest(GROUPNAME, DOCURI, ResponseStatus.IN_PREPARATION);

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
    @Test
    public void updateMetadata_MetadataUnknown() throws URISyntaxException, CannotUpdateAnnotationStatusException, MissingPermissionException {

        final UserInformation userInfo = new UserInformation("login", Authorities.ISC);
        final String uri = "http://dummy";
        final String groupName = "thegroup";
        final StatusUpdateRequest sur = new StatusUpdateRequest(groupName, uri, ResponseStatus.IN_PREPARATION);

        documentRepos.save(new Document(new URI(uri), "title"));
        groupRepos.save(new Group(groupName, false));

        // metadata is not saved in metadataRepos
        final List<Metadata> updatedMetadata = metadataService.updateMetadata(sur, userInfo);
        Assert.assertNotNull(updatedMetadata);
        Assert.assertEquals(0, updatedMetadata.size());
    }

    // status update finds no matching metadata
    @Test
    public void updateMetadata_NotFound() throws MissingPermissionException, URISyntaxException, CannotUpdateAnnotationStatusException {

        final List<Object> prep = prepareMetadataUpdateSuccessfulTest();
        final StatusUpdateRequest sur = (StatusUpdateRequest) prep.get(0);
        final UserInformation userInfo = (UserInformation) prep.get(1);

        final List<Metadata> updatedMetadata = metadataService.updateMetadata(sur, userInfo);
        Assert.assertNotNull(updatedMetadata);
        Assert.assertEquals(0, updatedMetadata.size());
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

        final List<Metadata> updateResult = metadataService.updateMetadata(sur, userInfo);
        Assert.assertEquals(1, updateResult.size());
        Assert.assertEquals(meta.getId(), updateResult.get(0).getId());

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
        final Metadata firstMeta = (Metadata) prep.get(2);

        // also set the responseStatus of the database metadata -> then there is a full match
        firstMeta.setResponseStatus(ResponseStatus.IN_PREPARATION);
        metadataRepos.save(firstMeta);

        // add another item to the database, which has even more properties
        // -> should also be found using same update request
        final Metadata secondMeta = new Metadata(firstMeta);
        final SimpleMetadata kvpSecond = secondMeta.getKeyValuePropertyAsSimpleMetadata();
        kvpSecond.put("anotherprop", "anotherval");
        secondMeta.setKeyValuePropertyFromSimpleMetadata(kvpSecond);
        metadataRepos.save(secondMeta);

        // add a third item which has different response status -> should not match!
        final Metadata thirdMeta = new Metadata(firstMeta);
        thirdMeta.setResponseStatus(ResponseStatus.SENT);
        metadataRepos.save(thirdMeta);

        // launch update request - only meta and secondMeta should match
        final List<Metadata> updateResult = metadataService.updateMetadata(sur, userInfo);
        Assert.assertEquals(2, updateResult.size());
        Assert.assertTrue(updateResult.stream().anyMatch(meta -> meta.getId() == firstMeta.getId()));
        Assert.assertTrue(updateResult.stream().anyMatch(meta -> meta.getId() == secondMeta.getId()));
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
        metadataToMatch.put(Metadata.PROP_RESPONSE_STATUS, "IN_PREPARATION");

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
