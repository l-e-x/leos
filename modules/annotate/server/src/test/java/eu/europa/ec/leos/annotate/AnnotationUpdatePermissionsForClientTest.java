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
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationPermissions;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.impl.AnnotationConversionServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionServiceImpl;
import org.assertj.core.api.StringAssert;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class AnnotationUpdatePermissionsForClientTest {

    /**
     * tests for checking that expected permissions are set on annotation at JSON level:
     * if annotation has response status "SENT", it should not have update/delete/admin permissions
     */

    private static final String userLeos = "acct:login@" + Authorities.EdiT;
    private static final String userIsc = "acct:login@" + Authorities.ISC;
    private static final String LOGIN = "login";
    private static final String ENTITY = "theEntity";

    // -------------------------------------
    // Required services
    // -------------------------------------
    @InjectMocks
    private AnnotationConversionServiceImpl conversionService;

    @Autowired
    @InjectMocks
    private AnnotationPermissionServiceImpl annotPermService;
    
    @Mock
    private UserService userService;

    private enum PermType {
        NONE, USER, GROUP, EVERYBODY
    }

    // test helper class for storing the permissions that are expected
    private static final class ExpectedPermissions {

        public final PermType adminPerm, editPerm, deletePerm, readPerm;

        public ExpectedPermissions(final PermType admin, final PermType edit,
                final PermType delete, final PermType read) {

            this.adminPerm = admin;
            this.editPerm = edit;
            this.deletePerm = delete;
            this.readPerm = read;
        }
    }

    // -------------------------------------
    // Mock initialisation before running new test
    // -------------------------------------
    @Before
    public void setupServiceMocks() {

        MockitoAnnotations.initMocks(this);
        annotPermService.setDefaultGroupName("__world__");
        conversionService.setPermissionService(annotPermService);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // a LEOS annotation was not SENT yet, user is author -> normal permissions (admin/update/delete: user, read: group)
    @Test
    public void testUpdatePermissionsNotSentLeos_Author() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.USER, PermType.USER, PermType.USER, PermType.GROUP);
        runTest(Authorities.EdiT, userLeos, ResponseStatus.IN_PREPARATION, AnnotationStatus.NORMAL, expPerm, false);
    }

    // a LEOS annotation was not SENT yet, user is support member -> normal permissions (admin/update/delete: user, read: group)
    @Test
    public void testUpdatePermissionsNotSentLeos_Support() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.USER, PermType.USER, PermType.USER, PermType.GROUP);
        runTest(Authorities.EdiT, userLeos, ResponseStatus.IN_PREPARATION, AnnotationStatus.NORMAL, expPerm, false);
    }

    // a LEOS annotation was not SENT yet, user has no particular role -> normal permissions (admin/update/delete: user, read: group)
    @Test
    public void testUpdatePermissionsNotSentLeos_NoRole() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.USER, PermType.USER, PermType.USER, PermType.GROUP);
        runTest(Authorities.EdiT, userLeos, ResponseStatus.IN_PREPARATION, AnnotationStatus.NORMAL, expPerm, false);
    }

    // an ISC annotation was not SENT yet -> normal permissions (admin/update/delete: user, read: group)
    // user does not belong to the same entity/group as the annotation
    @Test
    public void testUpdatePermissionsNotSentIsc() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.USER, PermType.USER, PermType.USER, PermType.GROUP);
        runTest(Authorities.ISC, userIsc, ResponseStatus.IN_PREPARATION, AnnotationStatus.NORMAL, expPerm, false);
    }

    // an ISC annotation was not SENT yet -> normal permissions (admin/update/delete: user, read: group)
    // user belongs to the same entity/group as the annotation -> no influence as annotation is not SENT
    @Test
    public void testUpdatePermissionsNotSentIsc_UserBelongsToSameEntity() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.USER, PermType.USER, PermType.USER, PermType.GROUP);
        runTest(Authorities.ISC, userIsc, ResponseStatus.IN_PREPARATION, AnnotationStatus.NORMAL, expPerm, true);
    }

    // a LEOS annotation was SENT -> should have restricted permissions
    // -> admin: no, edit: no, delete: everybody (=__world__ group), read: group)
    @Test
    public void testUpdatePermissionsSentLeos() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.NONE, PermType.NONE, PermType.EVERYBODY, PermType.GROUP);
        runTest(Authorities.EdiT, userLeos, ResponseStatus.SENT, AnnotationStatus.NORMAL, expPerm, false);
    }

    // an ISC annotation was SENT -> should have very restricted permissions
    // user does not belong to the annotation's entity/group
    // admin: no, delete/edit: no, read: group
    @Test
    public void testUpdatePermissionsSentIsc() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.NONE, PermType.NONE, PermType.NONE, PermType.GROUP);
        runTest(Authorities.ISC, userIsc, ResponseStatus.SENT, AnnotationStatus.NORMAL, expPerm, false);
    }

    // an ISC annotation was SENT and then DELETED -> should have no permissions
    // user belongs to the annotation's entity/group
    // admin: no, delete/edit: no, read: group
    @Test
    public void testUpdatePermissionsSentDeletedIsc() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.NONE, PermType.NONE, PermType.NONE, PermType.GROUP);
        runTest(Authorities.ISC, userIsc, ResponseStatus.SENT, AnnotationStatus.DELETED, expPerm, true);
    }
    
    // an ISC annotation was SENT -> should have restricted permissions
    // user belongs to the annotation's entity/group
    // admin: no, delete/edit: user, read: group
    @Test
    public void testUpdatePermissionsSentIsc_UserBelongsToSameEntity() {

        final ExpectedPermissions expPerm = new ExpectedPermissions(PermType.NONE, PermType.USER, PermType.USER, PermType.GROUP);
        runTest(Authorities.ISC, userIsc, ResponseStatus.SENT, AnnotationStatus.NORMAL, expPerm, true);
    }

    // -------------------------------------
    // internal test help functions
    // -------------------------------------
    private void runTest(final String authority, final String username, final ResponseStatus respStatus, 
            final AnnotationStatus annotStatus, final ExpectedPermissions expPerm, final boolean userBelongsToAnnotEntity) {

        // prepare
        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(username);
        final Annotation annot = createAnnotation(authority, respStatus, annotStatus, userBelongsToAnnotEntity);
        final UserInformation userInfo = new UserInformation(LOGIN, authority);
        if (userBelongsToAnnotEntity) {
            userInfo.setConnectedEntity(ENTITY);
        }

        final UserDetails details = new UserDetails(LOGIN, Long.valueOf(1), "first", "last", null, "", null);
        userInfo.setUserDetails(details);

        // act
        final JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, userInfo);

        // verify
        verifyPermissions(converted, username, expPerm);
    }

    private Annotation createAnnotation(final String authority, final ResponseStatus respStatus, final AnnotationStatus annotStatus,
            final boolean userBelongsToAnnotEntity) {

        final Annotation annot = new Annotation();
        annot.setTargetSelectors("[{\"selector\":null,\"source\":\"http://xyz\"}]");
        annot.setShared(true);

        final Group group = new Group("groupName", "group display", "desc", false);
        final Document doc = new Document();
        doc.setUri("http://dummy");

        final Metadata meta = new Metadata(doc, group, authority);
        meta.setResponseStatus(respStatus);
        if (userBelongsToAnnotEntity) {
            meta.setKeyValuePairs("responseId:" + ENTITY);
        }
        annot.setMetadata(meta);
        annot.setStatus(annotStatus);

        final User user = new User(LOGIN);
        annot.setUser(user);

        return annot;
    }

    private void verifyPermissions(final JsonAnnotation converted, final String username, final ExpectedPermissions expPerm) {

        Assert.assertNotNull(converted);
        final JsonAnnotationPermissions perms = converted.getPermissions();
        Assert.assertNotNull(perms);

        verifyPermission(perms.getAdmin(), expPerm.adminPerm, username);
        verifyPermission(perms.getUpdate(), expPerm.editPerm, username);
        verifyPermission(perms.getDelete(), expPerm.deletePerm, username);
        verifyPermission(perms.getRead(), expPerm.readPerm, username);
    }

    // verify a single permission
    @SuppressWarnings("PMD.SwitchStmtsShouldHaveDefault")
    private void verifyPermission(final List<String> actualPerms, final PermType expectedPerm, final String username) {

        Assert.assertEquals(1, actualPerms.size());

        final StringAssert strAss = new StringAssert(actualPerms.get(0));

        switch (expectedPerm) {
            case NONE:
                strAss.isEmpty();
                break;
            case USER:
                strAss.startsWith("acct:");
                strAss.isEqualToIgnoringCase(username);
                break;
            case GROUP:
                strAss.startsWith("group:");
                break;
            case EVERYBODY:
                strAss.isEqualTo("group:__world__");
                break;
        }
    }
}
