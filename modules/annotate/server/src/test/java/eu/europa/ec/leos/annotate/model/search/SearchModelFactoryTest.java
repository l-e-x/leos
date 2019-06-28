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
package eu.europa.ec.leos.annotate.model.search;

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.repository.*;
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

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class SearchModelFactoryTest {

    /**
     * tests for the factory methods of the {@link SearchModelFactory} class
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private SearchModelFactory searchModelFactory;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private MetadataRepository metadataRepos;

    @Autowired
    private DocumentRepository documentRepos;

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that no search model can be deducted without required options
    @Test(expected = IllegalArgumentException.class)
    public void testNoModelWithoutResolvedSearchOptions() {

        searchModelFactory.getSearchModel(null);
    }

    // test that no search model can be deducted when given options are missing the user's token
    @Test
    public void testNoModelWithoutUserToken() {

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }

    // test that no search model can be deducted when authority is unknown
    @Test
    public void testNoModelWithoutKnownAuthority() {

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        final Token token = new Token();
        token.setAuthority("someauthority");

        rso.setExecutingUserToken(token);
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }

    // test that an EdiT.1 search model is received when expected
    @Test
    public void testEdit1ModelReceived() throws Exception {

        final String SystemId = Authorities.EdiT;

        final User executingUser = new User("itsme");
        userRepos.save(executingUser);

        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        userGroupRepos.save(new UserGroup(executingUser.getId(), defaultGroup.getId()));

        final Document document = new Document(new URI("leos://5"), "title");
        documentRepos.save(document);

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, SystemId, null, null, null, null));
        rso.setGroup(defaultGroup); // public default world group -> EdiT.1 model
        rso.setDocument(document);

        // store metadata for document/group combination -> EdiT.1 has all conditions fulfilled
        final Metadata meta = new Metadata(document, defaultGroup, SystemId);
        metadataRepos.save(meta);
        rso.setMetadataWithStatusesList("");

        // check that correct model is returned
        final SearchModel resultModel = searchModelFactory.getSearchModel(rso);
        Assert.assertNotNull(resultModel);
        Assert.assertTrue(resultModel instanceof SearchModelLeosAllGroups);
    }

    // test that no EdiT.1 search model is received when required metadata is missing
    @Test
    public void testNoEdit1ModelWithoutMetadata() {

        final User executingUser = new User("itsme2");
        userRepos.save(executingUser);

        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        userGroupRepos.save(new UserGroup(executingUser.getId(), defaultGroup.getId()));

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, Authorities.EdiT, null, null, null, null));
        rso.setGroup(defaultGroup); // public default world group -> EdiT.1 model

        // no metadata supplied in database for document/group combination -> no search model
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }

    // test that an EdiT.2 search model is received when expected
    @Test
    public void testEdit2ModelReceived() throws Exception {

        final String SystemId = Authorities.EdiT;

        final User executingUser = new User("cestmoi");
        userRepos.save(executingUser);

        final Group someGroup = new Group("somegroup", false);
        groupRepos.save(someGroup);
        userGroupRepos.save(new UserGroup(executingUser.getId(), someGroup.getId()));

        final Document document = new Document(new URI("leos://6"), "thetitle");
        documentRepos.save(document);

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, SystemId, null, null, null, null));
        rso.setGroup(someGroup); // specific group -> EdiT.2 model
        rso.setDocument(document);
        rso.setMetadataWithStatusesList("");
        
        // store metadata for document/group combination -> EdiT.2 has all conditions fulfilled
        final Metadata meta = new Metadata(document, someGroup, SystemId);
        metadataRepos.save(meta);

        // check that correct model is returned
        final SearchModel resultModel = searchModelFactory.getSearchModel(rso);
        Assert.assertNotNull(resultModel);
        Assert.assertTrue(resultModel instanceof SearchModelLeosSingleGroup);
    }

    // test that no EdiT.2 search model is received when required metadata is missing
    @Test
    public void testNoEdit2ModelWithoutMetadata() {

        final User executingUser = new User("ich");
        userRepos.save(executingUser);

        final Group someGroup = new Group("thegroup", false);
        groupRepos.save(someGroup);
        userGroupRepos.save(new UserGroup(executingUser.getId(), someGroup.getId()));

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, Authorities.EdiT, null, null, null, null));
        rso.setGroup(someGroup); // specific group -> EdiT.2 model

        // no metadata supplied in database for document/group combination -> no search model
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }

    // test that an ISC.1 search model is received when expected
    // uses a single metadata set
    @Test
    public void testIsc1ModelReceived() throws Exception {

        final String metadata = "[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\"}]";

        runTestIsc1ModelReceived(metadata);
    }

    // test that an ISC.1 search model is received when expected
    // uses several requested metadata sets
    @Test
    public void testIsc1ModelReceived_severalMetadataSets() throws Exception {

        final String metadata = "[" + "{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\"}," +
                "{ISCReference: \"ISC/2018/00918\",responseId: \"id-4\",responseVersion: \"2\"}" + "]";

        runTestIsc1ModelReceived(metadata);
    }

    private void runTestIsc1ModelReceived(final String metadata) throws URISyntaxException {

        final String SystemId = Authorities.ISC;

        final User executingUser = new User("me");
        userRepos.save(executingUser);

        final Group someGroup = new Group("mygroup", false);
        groupRepos.save(someGroup);
        userGroupRepos.save(new UserGroup(executingUser.getId(), someGroup.getId()));

        final Document document = new Document(new URI("leos://7"), "mytitle");
        documentRepos.save(document);

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, SystemId, null, null, null, null));
        rso.setGroup(someGroup); // specific group and some few metadata -> ISC.1 model
        rso.setDocument(document);
        rso.setMetadataWithStatusesList(metadata);
        Assert.assertFalse(rso.getMetadataWithStatusesList().isEmpty()); // added since the map became a list of maps

        // store metadata for document/group combination -> ISC.1 has all conditions fulfilled
        final Metadata meta = new Metadata(document, someGroup, SystemId);
        meta.setKeyValuePairs("ISCReference:ISC/2018/00918\nresponseId:id-123\nresponseVersion:1");
        metadataRepos.save(meta);

        // check that correct model is returned
        final SearchModel resultModel = searchModelFactory.getSearchModel(rso);
        Assert.assertNotNull(resultModel);
        Assert.assertTrue(resultModel instanceof SearchModelIscSingleGroup);
    }

    // test that no ISC.1 search model is received when required metadata is missing
    @Test
    public void testNoIsc1ModelWithoutMetadata() {

        final User executingUser = new User("moi");
        userRepos.save(executingUser);

        final Group someGroup = new Group("ourgroup", false);
        groupRepos.save(someGroup);
        userGroupRepos.save(new UserGroup(executingUser.getId(), someGroup.getId()));

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, Authorities.ISC, null, null, null, null));
        rso.setGroup(someGroup); // specific group and some few metadata -> ISC.1 model
        rso.setMetadataWithStatusesList("[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\"}]");
        Assert.assertFalse(rso.getMetadataWithStatusesList().isEmpty()); // added since the map became a list of maps

        // no metadata supplied in database for document/group combination -> no search model
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }

    // test that an ISC.2 search model is received when expected
    // uses a single metadata set
    @Test
    public void testIsc2ModelReceived() throws Exception {

        final String metadata = "[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\", responseStatus: \"IN_PREPARATION\"}]";

        runTestIsc2ModelReceived(metadata);
    }

    // test that an ISC.2 search model is received when expected
    // uses several metadata set
    @Test
    public void testIsc2ModelReceived_severalMetadataSets() throws Exception {

        final String metadata = "[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\", responseStatus: \"IN_PREPARATION\"}," +
                "{ISCReference: \"ISC/2018/00918\",responseId: \"id-5\",responseVersion: \"2\", responseStatus: \"IN_PREPARATION\"}," +
                "{ISCReference: \"ISC/2018/00918\",responseId: \"id-5\",responseVersion: \"3\", responseStatus: \"SENT\"}" + "]";

        runTestIsc2ModelReceived(metadata);
    }

    private void runTestIsc2ModelReceived(final String metadata) throws URISyntaxException {

        final String SystemId = Authorities.ISC;

        final User executingUser = new User("myself");
        userRepos.save(executingUser);

        final Group someGroup = new Group("yourgroup", false);
        groupRepos.save(someGroup);
        userGroupRepos.save(new UserGroup(executingUser.getId(), someGroup.getId()));

        final Document document = new Document(new URI("leos://8"), "doctitle");
        documentRepos.save(document);

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, SystemId, null, null, null, null));
        rso.setGroup(someGroup); // specific group and some few metadata -> ISC.1 model
        rso.setDocument(document);
        rso.setMetadataWithStatusesList(metadata);
        Assert.assertFalse(rso.getMetadataWithStatusesList().isEmpty()); // added since the map became a list of maps

        // store metadata for document/group combination -> ISC.2 has all conditions fulfilled
        final Metadata meta = new Metadata(document, someGroup, SystemId);
        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);
        meta.setKeyValuePairs("ISCReference:ISC/2018/00918\nresponseId:id-123\nresponseVersion:1");
        metadataRepos.save(meta);

        // check that correct model is returned
        final SearchModel resultModel = searchModelFactory.getSearchModel(rso);
        Assert.assertNotNull(resultModel);
        Assert.assertTrue(resultModel instanceof SearchModelIscSingleGroup);
    }

    // test that no ISC.2 search model is received when required metadata is missing
    @Test
    public void testNoIsc2ModelWithoutMetadata() {

        final User executingUser = new User("i");
        userRepos.save(executingUser);

        final Group someGroup = new Group("theirgroup", false);
        groupRepos.save(someGroup);
        userGroupRepos.save(new UserGroup(executingUser.getId(), someGroup.getId()));

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, Authorities.ISC, null, null, null, null));
        rso.setGroup(someGroup); // specific group and some metadata (including responseStatus)-> ISC.2 model
        rso.setMetadataWithStatusesList("[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\",responseStatus: \"IN_PREPARATION\"}]");
        Assert.assertFalse(rso.getMetadataWithStatusesList().isEmpty()); // added since the map became a list of maps

        // no metadata supplied in database for document/group combination -> no search model
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }

    // test that an ISC.3 search model is received when expected
    // uses a single metadata set
    @Test
    public void tesIsc3Received() throws URISyntaxException {

        final String metadata = "[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\",responseStatus: \"SENT\"}]";
        runTesIsc3Received(metadata);
    }

    // test that an ISC.3 search model is received when expected
    // uses several single metadata sets
    @Test
    public void tesIsc3Received_severalMetadataSets() throws URISyntaxException {

        final String metadata = "[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\",responseStatus: \"SENT\"}," +
                "{ISCReference: \"ISC/2018/00444\",responseId: \"id-123\",responseVersion: \"1\",responseStatus: \"SENT\"}" + "]";
        runTesIsc3Received(metadata);
    }

    private void runTesIsc3Received(final String metadata) throws URISyntaxException {

        final String SystemId = Authorities.ISC;

        final User executingUser = new User("you");
        userRepos.save(executingUser);

        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        userGroupRepos.save(new UserGroup(executingUser.getId(), defaultGroup.getId()));

        final Document document = new Document(new URI("leos://9"), "thetitle");
        documentRepos.save(document);

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, SystemId, null, null, null, null));
        rso.setGroup(defaultGroup); // default public world group and some metadata (including responseStatus)-> ISC.3 model
        rso.setDocument(document);
        rso.setMetadataWithStatusesList(metadata);
        Assert.assertFalse(rso.getMetadataWithStatusesList().isEmpty()); // added since the map became a list of maps

        // store metadata for document/group combination -> ISC.3 has all conditions fulfilled
        final Metadata meta = new Metadata(document, defaultGroup, SystemId);
        meta.setResponseStatus(ResponseStatus.SENT);
        meta.setKeyValuePairs("ISCReference:ISC/2018/00918\nresponseId:id-123\nresponseVersion:1");
        metadataRepos.save(meta);

        // check that correct model is returned
        final SearchModel resultModel = searchModelFactory.getSearchModel(rso);
        Assert.assertNotNull(resultModel);
        Assert.assertTrue(resultModel instanceof SearchModelIscAllGroups);
    }

    // test that no ISC.3 search model is received when required metadata is missing
    @Test
    public void testNoIsc3ModelWithoutMetadata() {

        final User executingUser = new User("we");
        userRepos.save(executingUser);

        final Group defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
        userGroupRepos.save(new UserGroup(executingUser.getId(), defaultGroup.getId()));

        final ResolvedSearchOptions rso = new ResolvedSearchOptions();
        rso.setExecutingUser(executingUser);
        rso.setExecutingUserToken(new Token(executingUser, Authorities.ISC, null, null, null, null));
        rso.setGroup(defaultGroup); // default public world group and some metadata (including responseStatus)-> ISC.3 model
        rso.setMetadataWithStatusesList("[{ISCReference: \"ISC/2018/00918\",responseId: \"id-123\",responseVersion: \"1\",responseStatus: \"SENT\"}]");
        Assert.assertFalse(rso.getMetadataWithStatusesList().isEmpty()); // added since the map became a list of maps

        // no metadata supplied in database for document/group combination -> no search model
        Assert.assertNull(searchModelFactory.getSearchModel(rso));
    }
}