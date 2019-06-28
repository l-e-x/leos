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
package eu.europa.ec.leos.annotate.controller;

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.helper.TestHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocument;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationDocumentLink;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonSearchResult;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class MassiveRequestsTest {

    /**
     * Test case for generating high volume of requests via the REST controllers
     * will create, retrieve, search and delete annotations
     * Could e.g. be used for measuring performance (given an appropriate setup)
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationRepository annotRepos;

    @Autowired
    private AuthenticatedUserStore authUser;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private DocumentRepository docRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private UserDetailsCache userDetailsCache;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // the following number influence the volume of the test - increase or decrease as needed, but remember: the test may take some time...
    private final static int MAX_USERS = 5, MAX_GROUPS = 3;
    private final static int MAX_DOCS = 3;
    private final static int MAX_ANNOTS_PER_USER = 50;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);
        TestDbHelper.insertDefaultGroup(groupRepos);

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
        authUser.clear();
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    @Test
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    public void createManyRandomAnnotations() throws Exception {

        final Random rand = new Random();

        final int numberOfUsers = rand.nextInt(MAX_USERS) + 1;
        final int numberOfGroups = rand.nextInt(MAX_GROUPS) + 1;
        final int numberOfDocs = rand.nextInt(MAX_DOCS) + 1;

        List<User> theUsers = new ArrayList<User>();
        for (int i = 0; i < numberOfUsers; i++) {

            final User user = new User();
            user.setLogin("user" + i);
            theUsers.add(user);
        }

        List<Group> theGroups = new ArrayList<Group>();
        for (int i = 0; i < numberOfGroups; i++) {

            final Group group = new Group("group" + i, "disp" + i, "desc", i % 2 == 0);
            theGroups.add(group);
        }

        List<Document> theDocuments = new ArrayList<Document>();
        for (int i = 0; i < numberOfDocs; i++) {

            final Document doc = new Document(new URI("http://leos/doc" + i + ".eu"), "title");
            theDocuments.add(doc);
        }

        // save all users, groups, documents
        theDocuments = (List<Document>) docRepos.save(theDocuments);
        theUsers = (List<User>) userRepos.save(theUsers);
        theGroups = (List<Group>) groupRepos.save(theGroups);

        // assign all users to all groups
        for (final User u : theUsers) {
            for (final Group g : theGroups) {
                userGroupRepos.save(new UserGroup(u.getId(), g.getId()));
            }
        }

        // create tokens for the users, wrapped in UserInformation objects
        final List<UserInformation> userInfos = createUserInfos(theUsers);        

        // idea: create random number of annotations for (users x groups x documents)
        int totalAnnots = 0;
        final List<String> createdAnnotIds = createAnnotations(theDocuments, theUsers, theGroups, userInfos, rand);
        totalAnnots = createdAnnotIds.size();

        Assert.assertEquals(totalAnnots, annotRepos.count());

        // search each annotation individually - for each user!
        // will work as each user is member of each group
        for (final String annotId : createdAnnotIds) {
            for (int userIndex = 0; userIndex < theUsers.size(); userIndex++) {

                final UserInformation userInfo = userInfos.get(userIndex);
                authUser.setUserInfo(userInfo);
                executeFindAnnotationRequest(userInfo.getCurrentToken().getAccessToken(), annotId);
            }
        }

        // search for annotation using search function - for each user and group
        searchAnnotations(theUsers, userInfos, theGroups, theDocuments);

        // delete each annotation individually
        for (final String annotId : createdAnnotIds) {
            executeDeleteAnnotationRequest(annotId, userInfos.get(0).getCurrentToken().getAccessToken()); // any user can delete an annotation

            // verify that number of annotations decreased
            totalAnnots--;
            // Assert.assertEquals(totalAnnots, annotRepos.count());
            Assert.assertEquals(totalAnnots, annotRepos.count(new NormalAnnotationSpecifiation()));
            Assert.assertNotNull(annotRepos.findByIdAndStatus(annotId, AnnotationStatus.DELETED));
        }
    }

    private List<UserInformation> createUserInfos(final List<User> theUsers) {
        
        final List<UserInformation> userInfos = new ArrayList<UserInformation>();
        for (final User u : theUsers) {
            final UserInformation userInfo = new UserInformation(new Token(u, Authorities.EdiT, "acc" + u.getId(), LocalDateTime.now().plusHours(1),
                    "refr" + u.getId(), LocalDateTime.now().plusHours(1)));
            userInfos.add(userInfo);

            // save the token in DB
            tokenRepos.save(userInfo.getCurrentToken());

            // finally cache user infos
            userDetailsCache.cache(u.getLogin(),
                    new UserDetails(u.getLogin(), u.getId(), "first_" + u.getLogin(), "last_" + u.getLogin(), "someentity", u.getLogin() + "@eu", null));
        }
        return userInfos;
    }
    
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private List<String> createAnnotations(final List<Document> theDocuments, final List<User> theUsers, final List<Group> theGroups,
            final List<UserInformation> userInfos, final Random rand) throws Exception {
        
        final List<String> createdAnnotIds = new ArrayList<String>();
        for (final Document doc : theDocuments) {
            for (int userIndex = 0; userIndex < theUsers.size(); userIndex++) {
                final User user = theUsers.get(userIndex);
                for (final Group group : theGroups) {

                    int numAnnotatsToCreate = rand.nextInt(MAX_ANNOTS_PER_USER);
                    while (numAnnotatsToCreate == 0) {
                        numAnnotatsToCreate = rand.nextInt(MAX_ANNOTS_PER_USER);
                    }

                    for (int annIndex = 0; annIndex < numAnnotatsToCreate; annIndex++) {
                        authUser.setUserInfo(userInfos.get(userIndex));

                        // set document
                        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(user.getLogin());
                        final JsonAnnotationDocument jsDoc = new JsonAnnotationDocument();
                        jsDoc.setTitle(doc.getTitle());

                        final JsonAnnotationDocumentLink link = new JsonAnnotationDocumentLink();
                        link.setHref(new URI(doc.getUri()));
                        jsDoc.setLink(Arrays.asList(link));
                        jsAnnot.setDocument(jsDoc);
                        jsAnnot.setUri(new URI(doc.getUri()));

                        jsAnnot.setGroup(group.getName());
                        jsAnnot.setUser("acct:" + user.getLogin());

                        // create annotation via HTTP call
                        createdAnnotIds.add(executeCreateAnnotationRequest(userInfos.get(userIndex).getCurrentToken().getAccessToken(), jsAnnot));
                    }
                }
            }
        }
        
        return createdAnnotIds;
    }
    
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void searchAnnotations(final List<User> theUsers, final List<UserInformation> userInfos, 
            final List<Group> theGroups, final List<Document> theDocuments) throws Exception {
        
        for (int userIndex = 0; userIndex < theUsers.size(); userIndex++) {

            final UserInformation userInfo = userInfos.get(userIndex);
            authUser.setUserInfo(userInfo);
            final long userId = userInfo.getUser().getId();

            for (final Group group : theGroups) {
                final Long groupId = group.getId();
                for (final Document doc : theDocuments) {
                    final Long docId = doc.getId();

                    final List<String> expectedAnnotationIds = ((List<Annotation>) annotRepos.findAll()).stream()
                            .filter(annot -> annot.getDocument().getId().equals(docId) &&
                                    annot.getUserId() == userId &&
                                    annot.getGroup().getId().equals(groupId))
                            .map(Annotation::getId)
                            .sorted()
                            .collect(Collectors.toList());
                    executeSearchAnnotationRequest(userInfo, expectedAnnotationIds, group, doc);
                }
            }
        }
    }
    
    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private String executeCreateAnnotationRequest(final String token, final JsonAnnotation jsAnnot) throws Exception {

        final String serializedAnnotation = SerialisationHelper.serialize(jsAnnot);

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .post("/api/annotations")
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializedAnnotation);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // extract and return ID
        final JsonAnnotation jsResponse = SerialisationHelper.deserializeJsonAnnotation(responseString);
        Assert.assertNotNull(jsResponse);
        return jsResponse.getId();
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void executeFindAnnotationRequest(final String token, final String annotId) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/annotations/" + annotId)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + token)
                .contentType(MediaType.APPLICATION_JSON);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // make sure there was a response having the ID
        final JsonAnnotation jsResponse = SerialisationHelper.deserializeJsonAnnotation(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(annotId, jsResponse.getId());
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void executeSearchAnnotationRequest(final UserInformation userInfo,
            final List<String> sortedAnnotIds,
            final Group group,
            final Document doc) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/search/?group=" + group.getName() + "&uri=" + doc.getUri() + "&user=" + userInfo.getLogin() + "&limit=" +
                        MAX_ANNOTS_PER_USER)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + userInfo.getCurrentToken().getAccessToken())
                .contentType(MediaType.APPLICATION_JSON);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // make sure there was a response having the ID
        final JsonSearchResult jsResponse = SerialisationHelper.deserializeJsonSearchResult(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(sortedAnnotIds.size(), jsResponse.getRows().size());

        final List<String> receivedIds = jsResponse.getRows().stream()
                .map(JsonAnnotation::getId)
                .sorted()
                .collect(Collectors.toList());

        // compare list - works since they are sorted already
        Assert.assertTrue(sortedAnnotIds.equals(receivedIds));
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidDuplicateLiterals"})
    private void executeDeleteAnnotationRequest(final String annotId, final String accessToken) throws Exception {

        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .delete("/api/annotations/" + annotId)
                .header(TestHelper.AUTH_HEADER, TestHelper.AUTH_BEARER + accessToken);

        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 200
        result.andExpect(MockMvcResultMatchers.status().isOk());
    }

    // search specification for retrieving annotations with NORMAL status
    private static class NormalAnnotationSpecifiation implements Specification<Annotation> {

        @Override
        public Predicate toPredicate(final Root<Annotation> root, final CriteriaQuery<?> query, final CriteriaBuilder critBuilder) {

            final List<Predicate> predicates = new ArrayList<>();

            predicates.add(critBuilder.equal(root.get("status"), AnnotationStatus.NORMAL));

            return critBuilder.and(predicates.toArray(new Predicate[1])); // 1 = predicates.size()
        }
    }

}
