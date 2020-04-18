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

import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchOptions;
import eu.europa.ec.leos.annotate.model.search.AnnotationSearchResult;
import eu.europa.ec.leos.annotate.model.web.StatusUpdateRequest;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import eu.europa.ec.leos.annotate.services.StatusUpdateService;
import eu.europa.ec.leos.annotate.services.exceptions.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.StringUtils;
import org.junit.Assert;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class LinkedAnnotationsTest {

    /**
     * tests around new linked annotations and their visibility
     * 
     * start: an annotation having response status SENT is updated -> new additional annotation is created
     * -> we launch tests for different users/authorities/groups for searching; only the SENT one may be found
     * 
     * next: the annotation is SENT using status API function -> now has SENT status, previous SENT annotation is deleted
     * -> we launch tests checking that only the "newly SENT" item is found, the previous one was DELETED meanwhile 
     */

    private static final String LOGIN = "demo";
    private static final String OTHERLOGIN = "omed";
    private static final String ACCOUNT_PREFIX = "acct:user@";
    private static final String GROUP = "someGroup";
    private static final String OTHERGROUP = "puorGemos";
    private static final String SORTORDER = "ASC";
    private URI uri;
    private Group defaultGroup;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    @Qualifier("annotationTestRepos")
    private AnnotationTestRepository annotRepos;

    @Autowired
    private AnnotationService annotService;

    @Autowired
    private StatusUpdateService statusService;
    
    @Autowired
    private UserRepository userRepos;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private MetadataRepository metadataRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException, MissingPermissionException, CannotUpdateSentAnnotationException {

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        createTestData();
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    private void createTestData()
            throws CannotCreateAnnotationException, CannotUpdateAnnotationException,
            MissingPermissionException, CannotUpdateSentAnnotationException {

        final User user = new User(LOGIN);
        userRepos.save(user);

        final String authority = Authorities.ISC;
        final String hypothesisUserAccount = ACCOUNT_PREFIX + authority;

        final UserInformation userInfo = new UserInformation(user, authority);

        final Group group = groupRepos.save(new Group(GROUP, true));
        userGroupRepos.save(new UserGroup(user.getId(), group.getId()));

        // create an annotation
        final JsonAnnotation annot = TestData.getTestAnnotationObject(hypothesisUserAccount);
        uri = annot.getDocument().getLink().get(0).getHref();
        annot.setGroup(GROUP);
        annotService.createAnnotation(annot, userInfo);

        // modify saved metadata to make the annotation be SENT already
        final Metadata meta = metadataRepos.findAll().iterator().next();
        meta.setResponseStatus(Metadata.ResponseStatus.SENT);
        metadataRepos.save(meta);

        // update the metadata in the JSON annotation also
        annot.getDocument().getMetadata().put("responseStatus", Metadata.ResponseStatus.SENT.toString());

        // send an update of the annotation to create a link to a new IN_PREPARATION annotation
        annot.setText("new content");
        annotService.updateAnnotation(annot.getId(), annot, userInfo);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /*
     * ISC user searches for annotations - should see his own: the "linked" one having
     * response status IN_PREPARATION
     * and the other part of the link, i.e. the SENT item; (note: this was filtered out by ANOT-96, but is taken in again with ANOT-101)
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForOwner() {

        // create user information
        final User user = userRepos.findByLogin(LOGIN);
        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);
        final Token userToken = new Token(user, Authorities.ISC, "@cce$s", LocalDateTime.now().plusMinutes(5), "refr",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, true, findSentAnnotationFromRepos().getId());
    }

    /*
     * ISC user searches for annotations after an updated ISC annotation was SENT 
     * - should see the newly created one (during update): he is owner so he should always see it
     * - should not see the original one any more since it is DELETED meanwhile
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForOwner_afterSent() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        // create user information
        final User user = userRepos.findByLogin(LOGIN);
        final UserInformation userInfo = new UserInformation(user, Authorities.ISC);

        // find the annotation being IN_PREPARATION and update its response status
        final Annotation annInPrep = findInPrepAnnotationFromRepos();
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annInPrep.getGroup().getName(), uri.toString(),
                Metadata.ResponseStatus.SENT);
        statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        final Token userToken = new Token(user, Authorities.ISC, "@cce$s", LocalDateTime.now().plusMinutes(5), "refr",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, false, annInPrep.getId());
    }

    /*
     * ISC user searches for annotations - should see IN_PREPARATION one as he is member of same group
     * and the SENT item of the same group (new ISC search rule, cf. ANOT-96 and ANOT-101; in the latter, it's included again!)
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForSameGroup() {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.ISC);
        final Token userToken = new Token(otherUser, Authorities.ISC, "@cces$", LocalDateTime.now().plusMinutes(5), "new",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to the same group as the annotations
        userGroupRepos.save(new UserGroup(otherUser.getId(), groupRepos.findByName(GROUP).getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, true, findSentAnnotationFromRepos().getId()); // SENT item is not filtered out any more since ANOT-101!
    }

    /*
     * ISC user searches for annotations after an updated ISC annotation was SENT 
     * - should see the newly created one (during update): he is member of the same group
     * - should not see the original one any more since it is DELETED meanwhile
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForSameGroup_afterSent() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.ISC);
        final Token userToken = new Token(otherUser, Authorities.ISC, "@cces$", LocalDateTime.now().plusMinutes(5), "new",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to the same group as the annotations
        userGroupRepos.save(new UserGroup(otherUser.getId(), groupRepos.findByName(GROUP).getId()));

        // find the annotation being IN_PREPARATION and update its response status
        final Annotation annInPrep = findInPrepAnnotationFromRepos();
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annInPrep.getGroup().getName(), uri.toString(),
                Metadata.ResponseStatus.SENT);
        statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");
        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, false, annInPrep.getId());
    }

    /*
     * ISC user searches for annotations - should not see any annotations as he is not member of their group
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForOtherGroup() {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.ISC);
        final Token userToken = new Token(otherUser, Authorities.ISC, "@c§ess", LocalDateTime.now().plusMinutes(5), "r€f",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to a different group than the annotations
        final Group otherGroup = new Group(OTHERGROUP, true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(otherUser.getId(), otherGroup.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(
                new SimpleMetadata("responseStatus", "IN_PREPARATION"), Arrays.asList(AnnotationStatus.NORMAL))));

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, false, false, "");
    }

    /*
     * ISC user searches for annotations - should not see any annotations as he is not member of their group
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForOtherGroup_afterSent() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.ISC);
        final Token userToken = new Token(otherUser, Authorities.ISC, "@c§ess", LocalDateTime.now().plusMinutes(5), "r€f",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to a different group than the annotations
        final Group otherGroup = new Group(OTHERGROUP, true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(otherUser.getId(), otherGroup.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");
        options.setMetadataMapsWithStatusesList(Arrays.asList(new SimpleMetadataWithStatuses(
                new SimpleMetadata(Metadata.PROP_RESPONSE_STATUS, Metadata.ResponseStatus.IN_PREPARATION.toString()),
                Arrays.asList(AnnotationStatus.NORMAL))));

        // find the annotation being IN_PREPARATION and update its response status
        final Annotation annInPrep = findInPrepAnnotationFromRepos();
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annInPrep.getGroup().getName(), uri.toString(),
                Metadata.ResponseStatus.SENT);
        statusService.updateAnnotationResponseStatus(updateRequest, userInfo);

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, false, false, "");
    }

    /*
     * LEOS/EdiT user searches for annotations - should see only those being sent, not the ones being IN_PREPARATION
     * use __world__ group for searching
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForLeosDefaultGroup() {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.EdiT);
        final Token userToken = new Token(otherUser, Authorities.EdiT, "@cCess", LocalDateTime.now().plusMinutes(5), "Ref",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to a different group than the annotations
        final Group otherGroup = new Group(OTHERGROUP, true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(otherUser.getId(), otherGroup.getId()));
        userGroupRepos.save(new UserGroup(otherUser.getId(), defaultGroup.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), defaultGroup.getName(), true,
                -1, 0, SORTORDER, "");

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, false, "");
    }

    /*
     * LEOS/EdiT user searches for annotations after the response status was set to SENT
     * -> should see only those being SENT and not DELETED (= the one originally being IN_PREPARATION before response status update)
     * use __world__ group for searching
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForLeosDefaultGroup_afterSent() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        // find the annotation being IN_PREPARATION and update its response status (done by the owner, of course)
        final Annotation annInPrep = findInPrepAnnotationFromRepos();
        final UserInformation userInfoOwner = new UserInformation(userRepos.findByLogin(LOGIN), Authorities.ISC);
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annInPrep.getGroup().getName(), uri.toString(),
                Metadata.ResponseStatus.SENT);
        statusService.updateAnnotationResponseStatus(updateRequest, userInfoOwner);

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.EdiT);
        final Token userToken = new Token(otherUser, Authorities.EdiT, "@cCess", LocalDateTime.now().plusMinutes(5), "rEf",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to a different group than the annotations
        final Group otherGroup = new Group(OTHERGROUP, true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(otherUser.getId(), otherGroup.getId()));
        userGroupRepos.save(new UserGroup(otherUser.getId(), defaultGroup.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), defaultGroup.getName(), true,
                -1, 0, SORTORDER, "");

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, false, annInPrep.getId());
    }

    /*
     * LEOS/EdiT user searches for annotations - should see only those being sent, not the ones being IN_PREPARATION
     * use annotations' group for searching
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForLeosSameGroup() {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.EdiT);
        final Token userToken = new Token(otherUser, Authorities.EdiT, "@cc", LocalDateTime.now().plusMinutes(5), "r",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to the group to which the annotations belong
        final Group group = groupRepos.findByName(GROUP);
        userGroupRepos.save(new UserGroup(otherUser.getId(), group.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, false, "");
    }

    /*
     * LEOS/EdiT user searches for annotations after the response status was changed to SENT
     * -> should see only those being SENT (which were IN_PREPARATION before)
     * use annotations' group for searching
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForLeosSameGroup_afterSent() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        // find the annotation being IN_PREPARATION and update its response status (done by the owner, of course)
        final Annotation annInPrep = findInPrepAnnotationFromRepos();
        final UserInformation userInfoOwner = new UserInformation(userRepos.findByLogin(LOGIN), Authorities.ISC);
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annInPrep.getGroup().getName(), uri.toString(),
                Metadata.ResponseStatus.SENT);
        statusService.updateAnnotationResponseStatus(updateRequest, userInfoOwner);

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.EdiT);
        final Token userToken = new Token(otherUser, Authorities.EdiT, "@ccess", LocalDateTime.now().plusMinutes(5), "ref",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to the group to which the annotations belong
        final Group group = groupRepos.findByName(GROUP);
        userGroupRepos.save(new UserGroup(otherUser.getId(), group.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), GROUP, true,
                -1, 0, SORTORDER, "");

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, true, false, annInPrep.getId());
    }

    /*
     * LEOS/EdiT user searches for annotations - should not see any items due to different group
     * (use different group for searching than annotations' group) 
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForLeosOtherGroup() {

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.EdiT);
        final Token userToken = new Token(otherUser, Authorities.EdiT, "@ccess", LocalDateTime.now().plusMinutes(5), "ref",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to a different group than the annotations
        final Group otherGroup = new Group(OTHERGROUP, true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(otherUser.getId(), otherGroup.getId()));
        userGroupRepos.save(new UserGroup(otherUser.getId(), defaultGroup.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), OTHERGROUP, true,
                -1, 0, SORTORDER, "");

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, false, false, "");
    }

    /*
     * LEOS/EdiT user searches for annotations after response status was changed to SENT
     * -> should not see any items due to different group
     * (use different group for searching than annotations' group)
     */
    @Test
    public void testVisibilityOfLinkedAnnotationForLeosOtherGroup_afterSent() throws CannotUpdateAnnotationStatusException, MissingPermissionException {

        // find the annotation being IN_PREPARATION and update its response status (done by the owner, of course)
        final Annotation annInPrep = findInPrepAnnotationFromRepos();
        final UserInformation userInfoOwner = new UserInformation(userRepos.findByLogin(LOGIN), Authorities.ISC);
        final StatusUpdateRequest updateRequest = new StatusUpdateRequest(annInPrep.getGroup().getName(), uri.toString(),
                Metadata.ResponseStatus.SENT);
        statusService.updateAnnotationResponseStatus(updateRequest, userInfoOwner);

        // create user information
        final User otherUser = userRepos.save(new User(OTHERLOGIN));
        final UserInformation userInfo = new UserInformation(otherUser, Authorities.EdiT);
        final Token userToken = new Token(otherUser, Authorities.EdiT, "@ccess", LocalDateTime.now().plusMinutes(5), "ref",
                LocalDateTime.now().plusMinutes(5));
        tokenRepository.save(userToken);
        userInfo.setCurrentToken(userToken);

        // assign the user to a different group than the annotations
        final Group otherGroup = new Group(OTHERGROUP, true);
        groupRepos.save(otherGroup);
        userGroupRepos.save(new UserGroup(otherUser.getId(), otherGroup.getId()));
        userGroupRepos.save(new UserGroup(otherUser.getId(), defaultGroup.getId()));

        final AnnotationSearchOptions options = new AnnotationSearchOptions(uri.toString(), OTHERGROUP, true,
                -1, 0, SORTORDER, "");

        final AnnotationSearchResult result = annotService.searchAnnotations(options, userInfo);
        checkContainedItems(result, false, false, null);
    }

    /**
     * helper function: check that a given {@link AnnotationSearchResult} contains a SENT and/or IN_PREPARATION item 
     */
    private void checkContainedItems(final AnnotationSearchResult result, final boolean containsSent,
            final boolean containsInPrep, final String sentId) {

        Assert.assertNotNull(result);

        int itemsExpected = 0;
        if (containsSent) {
            itemsExpected += 1;
        }
        if (containsInPrep) {
            itemsExpected += 1;
        }
        Assert.assertEquals(itemsExpected, result.getItems().size());

        if (containsSent) {
            final Optional<Annotation> foundAnnot = result.getItems().stream().filter(item -> item.isResponseStatusSent()).findFirst();
            Assert.assertTrue(foundAnnot.isPresent());
            if (!StringUtils.isEmpty(sentId)) {
                Assert.assertEquals(sentId, foundAnnot.get().getId());
            }
        }
        if (containsInPrep) {
            Assert.assertTrue(result.getItems().stream().filter(item -> item.getMetadata().getResponseStatus() == Metadata.ResponseStatus.IN_PREPARATION)
                    .findFirst().isPresent());
        }
    }

    /**
     * helper function: find the one annotation in the repository that has response status IN_PREPARATION
     * @return found {@link Annotation}, or {@literal null}
     */
    private Annotation findInPrepAnnotationFromRepos() {

        return findAnnotationFromRepos(Metadata.ResponseStatus.IN_PREPARATION);
    }
    
    /**
     * helper function: find the one annotation in the repository that has response status SENT
     * @return found {@link Annotation}, or {@literal null}
     */
    private Annotation findSentAnnotationFromRepos() {

        return findAnnotationFromRepos(Metadata.ResponseStatus.SENT);
    }
    
    private Annotation findAnnotationFromRepos(final Metadata.ResponseStatus respStatus) {

        final Iterator<Annotation> ann = annotRepos.findAll().iterator();
        while (ann.hasNext()) {
            final Annotation annCheck = ann.next();
            if (annCheck.getMetadata().getResponseStatus().equals(respStatus)) {
                return annCheck;
            }
        }

        return null;
    }
}
