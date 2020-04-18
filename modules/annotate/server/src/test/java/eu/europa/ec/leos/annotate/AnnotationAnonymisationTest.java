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

import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.impl.AnnotationConversionServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class AnnotationAnonymisationTest {

    /**
     * This class contains a few tests for anonymisation of annotations when converted to JSON 
     * depending on annotation's authority, response status, and search model used
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationConversionServiceImpl conversionService;
    
    @SuppressWarnings("PMD.UnusedPrivateField")
    @Mock
    private AnnotationPermissionServiceImpl annotPermService;

    @Mock
    private UserService userService;

    private final static String login = "somebody";
    private final static String prefix = "acct:" + login + "@";
    private final static String firstName = "Some";
    private final static String lastName = "Body";
    private final static String entityName = "DG AGRI";
    private final static String groupName = "mygroup";
    private final static String groupDisplayName = "group display name";
    private final static String iscReference = "ISC/2018/00918";
    private final static LocalDateTime AnnotCreated = LocalDateTime.of(2019, 01, 24, 15, 48);
    private final static LocalDateTime RespUpdated = LocalDateTime.of(2019, 03, 25, 11, 36);

    private Group group;
    private Document doc;
    private User user;
    private final UserDetails details = new UserDetails(login, (long) 1, firstName, lastName,
            Arrays.asList(new UserEntity("2", entityName, entityName)), "", Arrays.asList("author"));

    // -------------------------------------
    // Cleanup of database content before running new test
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        MockitoAnnotations.initMocks(this);
        Mockito.when(userService.getUserDetailsFromUserRepo(Mockito.anyString())).thenReturn(details);

        group = new Group(groupName, groupDisplayName, "desc", false);
        doc = new Document();
        doc.setUri("http://dummy");

        user = new User(login);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that user names are anonmyised when annotations are from ISC system and have response status "SENT"
     * parameters: 
     * - response status "SENT", not "SENT"
     * - annotation authority LEOS, ISC
     * - search model LEOS, ISC
     * 
     * note that some of the eight possible combinations from above parameters may not occur in real life (indicated with brackets), 
     * but nevertheless should behave according to the rules
     * numbers indicate the test order below
     * 
     *                          | authority ISC,    | authority ISC,   | authority LEOS,   | authority LEOS,
     *                          | search model LEOS | search model ISC | search model LEOS | search model ISC
     * -------------------------|-------------------|------------------|-------------------|-------------------
     * response status not SENT |     1, normal     |    2, normal     |     3, normal     |    (4, normal)
     * response status SENT     |     5, anonym     |    6, normal     |     7, normal     |    (8, normal)
     */

    // test 1: convert with both LEOS search models, but response status is not "SENT" -> no anonymisation expected
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testNoAnonymisationIscAnnotationNotSentLeosModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.ISC, ResponseStatus.IN_PREPARATION);
        final String expectedUsername = prefix + Authorities.ISC;
        final UserInformation userInfo = createUserInfo(Authorities.ISC);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);
    }

    // test 2: convert with ISC search models, response status still NOT "SENT" -> no anonymisation
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testNoAnonymisationIscAnnotationNotSentIscModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.ISC, ResponseStatus.IN_PREPARATION);
        final String expectedUsername = prefix + Authorities.ISC;
        final UserInformation userInfo = createUserInfo(Authorities.ISC);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);
    }

    // test 3: convert with LEOS search models and annotation as LEOS authority; response status NOT "SENT" -> no anonymisation
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testNoAnonymisationLeosAnnotationNotSentLeosModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.EdiT, ResponseStatus.IN_PREPARATION);
        final String expectedUsername = prefix + Authorities.EdiT;
        final UserInformation userInfo = createUserInfo(Authorities.EdiT);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);
    }

    // test 4: convert with ISC search models, response status still NOT "SENT" -> no anonymisation
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testNoAnonymisationLeosAnnotationNotSentIscModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.EdiT, ResponseStatus.IN_PREPARATION);
        final String expectedUsername = prefix + Authorities.EdiT;
        final UserInformation userInfo = createUserInfo(Authorities.EdiT);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyNotAnonymized(converted, expectedUsername);
    }

    // test 5: convert again with LEOS search model, but now response status is "SENT" -> SHOULD BE ANONYMISED
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testAnonymisationIscAnnotationSentLeosModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.ISC, ResponseStatus.SENT);
        final String expectedEntity = entityName;
        final UserInformation userInfo = createUserInfo(Authorities.EdiT);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(prefix + Authorities.EdiT);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedEntity);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedEntity);
    }

    // test 5b: convert again with LEOS search model, but now response status is "SENT" -> SHOULD BE ANONYMISED
    // but this time, we do not have the user's entity available -> "unknown" expected
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testAnonymisationIscAnnotationSentLeosModels_noUserDetails() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.ISC, ResponseStatus.SENT);
        final String expectedEntity = "unknown";
        final UserInformation userInfo = createUserInfo(Authorities.EdiT);

        // make sure no user details are found
        Mockito.when(userService.getUserDetailsFromUserRepo(Mockito.anyString())).thenReturn(null);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedEntity);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedEntity);
    }

    // test 5c: convert again with LEOS search model, but now response status is "SENT" -> SHOULD BE ANONYMISED
    // but this time, we do not have the search model available, requesting user is from LEOS/EdiT authority
    // -> doesn't matter, should still be anonymised
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testAnonymisationIscAnnotationSentLeosModels_noSearchModel() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.ISC, ResponseStatus.SENT);
        final String expectedUsername = entityName;
        final UserInformation userInfo = createUserInfo(Authorities.EdiT);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(prefix + Authorities.EdiT);

        // act + verify
        final JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername); // "SENT" -> anonymized
    }

    // test 6: convert again with ISC search model, response status is "SENT" -> anonymisation (due to "SENT")!
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testNoAnonymisationIscAnnotationSentIscModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.ISC, ResponseStatus.SENT);
        final String expectedUsername = entityName;
        final UserInformation userInfo = createUserInfo(Authorities.ISC);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername);
    }

    // test 7: convert LEOS annotation again with LEOS search model, response status is "SENT" -> anonymisation (since "SENT")
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testAnonymisationLeosAnnotationSentLeosModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.EdiT, ResponseStatus.SENT);
        final String expectedUsername = entityName;
        final String expectedDisplayName = "unknown ISC reference"; // (usually, EdiT annotations don't have the response status)

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        final UserInformation userInfo = createUserInfo(Authorities.EdiT);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername, expectedDisplayName);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername, expectedDisplayName);
    }

    // test 8: convert again with ISC search model, response status is "SENT" -> anonymisation (due to "SENT")
    // note: search model does not influence the result any more, user's authority and the annotation are the main criteria
    @Test
    public void testNoAnonymisationLeosAnnotationSentIscModels() {

        // prepare
        final Annotation annot = createAnnotation(Authorities.EdiT, ResponseStatus.SENT);
        final String expectedUsername = entityName;
        final String expectedEntity = "unknown ISC reference"; // (usually, EdiT annotations don't have the response status)
        final UserInformation userInfo = createUserInfo(Authorities.ISC);

        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(expectedUsername);

        // act + verify
        JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername, expectedEntity);

        // act + verify
        converted = conversionService.convertToJsonAnnotation(annot, userInfo);
        verifyAnonymized(converted, expectedUsername, expectedEntity);
    }

    // -------------------------------------
    // helper functions for data creation and verification
    // -------------------------------------

    private Annotation createAnnotation(final String authority, final ResponseStatus respStatus) {

        final Annotation annot = new Annotation();
        annot.setTargetSelectors("[{\"selector\": [{\"type\": \"a\"}]}]");
        annot.setCreated(AnnotCreated);

        final Metadata meta = new Metadata(doc, group, authority);
        meta.setResponseStatus(respStatus);
        if (respStatus == ResponseStatus.SENT) {
            meta.setResponseStatusUpdated(RespUpdated);
            meta.setResponseStatusUpdatedBy((long) 3);
        }
        if (Authorities.isIsc(authority)) {
            meta.setKeyValuePairs("ISCReference:" + iscReference);
        }
        annot.setMetadata(meta);

        annot.setUser(user);

        return annot;
    }

    private UserInformation createUserInfo(final String authority) {

        final UserInformation userInfo = new UserInformation(login, authority);
        userInfo.setUserDetails(details);
        return userInfo;
    }

    private void verifyNotAnonymized(final JsonAnnotation converted, final String expectedUsername) {

        Assert.assertNotNull(converted);
        Assert.assertEquals(expectedUsername, converted.getUser());
        Assert.assertEquals(lastName + " " + firstName, converted.getUser_info().getDisplay_name());
        Assert.assertEquals(entityName, converted.getUser_info().getEntity_name());

        Assert.assertEquals(AnnotCreated, converted.getCreated());
    }

    private void verifyAnonymized(final JsonAnnotation converted, final String expectedEntity) {

        verifyAnonymized(converted, expectedEntity, iscReference);
    }

    private void verifyAnonymized(final JsonAnnotation converted, final String expectedEntity, final String expectedIscRef) {

        Assert.assertNotNull(converted);
        Assert.assertEquals(expectedEntity, converted.getUser());

        Assert.assertEquals(expectedIscRef, converted.getUser_info().getDisplay_name());
        Assert.assertEquals(expectedEntity, converted.getUser_info().getEntity_name());

        Assert.assertEquals(RespUpdated, converted.getCreated()); // date from metadata was taken over
    }
}
