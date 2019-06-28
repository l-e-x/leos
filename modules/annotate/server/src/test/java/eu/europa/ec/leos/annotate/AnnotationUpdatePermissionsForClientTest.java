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

import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotationPermissions;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.impl.AnnotationConversionServiceImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationUpdatePermissionsForClientTest {

    /**
     * tests for checking that expected permissions are set on annotation at JSON level:
     * if annotation has response status "SENT", it should not have update/delete/admin permissions
     */

    private static final String userLeos = "acct:login@" + Authorities.EdiT;
    private static final String userIsc = "acct:login@" + Authorities.ISC;

    // -------------------------------------
    // Required services
    // -------------------------------------
    @InjectMocks
    private AnnotationConversionServiceImpl conversionService;
    
    @Mock
    private UserService userService;

    // -------------------------------------
    // Mock initialisation before running new test
    // -------------------------------------
    @Before
    public void setupServiceMocks() {

        MockitoAnnotations.initMocks(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // a LEOS annotation was not SENT yet -> normal permissions
    @Test
    public void testUpdatePermissionsNotSentLeos() {

        runTest(Authorities.EdiT, userLeos, ResponseStatus.IN_PREPARATION, true);
    }

    // an ISC annotation was not SENT yet -> normal permissions
    @Test
    public void testUpdatePermissionsNotSentIsc() {

        runTest(Authorities.ISC, userIsc, ResponseStatus.IN_PREPARATION, true);
    }

    // a LEOS annotation was SENT -> should have restricted permissions
    @Test
    public void testUpdatePermissionsSentLeos() {

        runTest(Authorities.EdiT, userLeos, ResponseStatus.SENT, false);
    }

    // a LEOS annotation was SENT -> should have restricted permissions
    @Test
    public void testUpdatePermissionsSentIsc() {

        runTest(Authorities.ISC, userIsc, ResponseStatus.SENT, false);
    }

    // -------------------------------------
    // internal test help functions
    // -------------------------------------
    private void runTest(final String authority, final String username, final ResponseStatus respStatus, final boolean expectedPermissions) {

        // prepare
        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn(username);
        final Annotation annot = createAnnotation(authority, respStatus);

        // act
        final JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, null);

        // verify
        if (expectedPermissions) {
            verifyHasPermissions(converted, username);
        } else {
            verifyHasNoPermissions(converted, username);
        }
    }

    private Annotation createAnnotation(final String authority, final ResponseStatus respStatus) {

        final Annotation annot = new Annotation();
        annot.setTargetSelectors("[{\"selector\":null,\"source\":\"http://xyz\"}]");

        final Group group = new Group("groupName", "group display", "desc", false);
        final Document doc = new Document();
        doc.setUri("http://dummy");

        final Metadata meta = new Metadata(doc, group, authority);
        meta.setResponseStatus(respStatus);
        annot.setMetadata(meta);

        final User user = new User("login");
        annot.setUser(user);

        return annot;
    }

    private void verifyHasPermissions(final JsonAnnotation converted, final String username) {

        Assert.assertNotNull(converted);
        final JsonAnnotationPermissions perms = converted.getPermissions();
        Assert.assertNotNull(perms);

        // read permission is there
        verifyHasPermission(perms.getRead(), username);

        // other permissions also
        verifyHasPermission(perms.getUpdate(), username);
        verifyHasPermission(perms.getAdmin(), username);
        verifyHasPermission(perms.getDelete(), username);
    }

    private void verifyHasNoPermissions(final JsonAnnotation converted, final String username) {

        Assert.assertNotNull(converted);
        final JsonAnnotationPermissions perms = converted.getPermissions();
        Assert.assertNotNull(perms);

        // read permission is there
        verifyHasPermission(perms.getRead(), username);

        // but other permissions are not
        verifyHasPermission(perms.getUpdate(), "");
        verifyHasPermission(perms.getAdmin(), "");
        verifyHasPermission(perms.getDelete(), "");
    }

    private void verifyHasPermission(final List<String> perm, final String username) {

        Assert.assertNotNull(perm);
        Assert.assertEquals(1, perm.size());
        Assert.assertEquals(username, perm.get(0));
    }
}
