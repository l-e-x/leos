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
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.UserServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("PMD.TooManyMethods")
@RunWith(MockitoJUnitRunner.class)
public class AnnotationPermissionServiceTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationPermissionServiceImpl annotPermMockService;

    @Mock
    private UserServiceImpl userService;

    @Mock
    private GroupService groupService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that annotation is not by default considered as belonging to an undefined user
    @Test
    public void testIsAnnotationOfUserFails() throws Exception {

        final String login = "somebody";

        Mockito.when(userService.findByLogin(login)).thenReturn(null);

        Assert.assertFalse(callIsAnnotationOfUser(annotPermMockService, null, login));
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testHasPermissionToUpdate_AnnotationNull() throws Exception {

        final Annotation annot = null;
        final UserInformation userinfo = new UserInformation("itsme", Authorities.EdiT);
        annotPermMockService.hasUserPermissionToUpdateAnnotation(annot, userinfo);
    }

    @Test(expected = IllegalArgumentException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testHasPermissionToUpdate_UserinfoNull() throws Exception {

        final Annotation annot = new Annotation();
        final UserInformation userinfo = null;

        annotPermMockService.hasUserPermissionToUpdateAnnotation(annot, userinfo);
    }

    @Test
    public void testHasPermissionToUpdate_OtherMetadata_EditUser() throws Exception {

        final String LOGIN = "theuser";

        final User user = new User(LOGIN);
        user.setId(Long.valueOf(2));
        Mockito.when(userService.createUser(LOGIN)).thenReturn(user);
        Mockito.when(userService.findByLogin(LOGIN)).thenReturn(user);

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        annot.setMetadata(meta);
        annot.setUser(user);

        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);

        // verify: status is not SENT, it is the user's annotation -> it can be updated
        Assert.assertTrue(annotPermMockService.hasUserPermissionToUpdateAnnotation(annot,
                new UserInformation(annot.getUser().getLogin(), Authorities.EdiT)));
    }

    // user wanting to update is not the annotation's creator -> refused
    @Test
    public void testHasPermissionToUpdate_OtherMetadata_OtherEditUser() throws Exception {

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        annot.setMetadata(meta);
        annot.setUser(new User("theuser"));

        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);

        // verify: status is not SENT -> ok, but user is not the annotation's creator
        Assert.assertFalse(annotPermMockService.hasUserPermissionToUpdateAnnotation(annot,
                new UserInformation("nottheuser", Authorities.EdiT)));
    }

    @Test
    public void testHasPermissionToUpdate_OtherMetadata_IscUser() throws Exception {

        final String LOGIN = "dave";
        final User user = new User(LOGIN);
        user.setId(Long.valueOf(8));

        Mockito.when(userService.findByLogin(LOGIN)).thenReturn(user);

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        annot.setMetadata(meta);
        annot.setUser(user);

        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);

        // verify: status is not SENT -> it can be updated
        Assert.assertTrue(annotPermMockService.hasUserPermissionToUpdateAnnotation(annot,
                new UserInformation(annot.getUser().getLogin(), Authorities.ISC)));
    }

    @Test
    public void testHasPermissionToUpdateNotSuccessful_Sent_EditUser() throws Exception {

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        annot.setMetadata(meta);
        annot.setUser(new User("hello"));

        meta.setResponseStatus(ResponseStatus.SENT);

        // verify: status is SENT -> it cannot be updated any more
        Assert.assertFalse(annotPermMockService.hasUserPermissionToUpdateAnnotation(annot,
                new UserInformation(annot.getUser().getLogin(), Authorities.EdiT)));
    }

    // an ISC user wants to update a SENT annotation; he belongs to the same group as the annotation
    @Test
    public void testHasPermissionToUpdateSuccessful_IscUserOfSameGroup() throws Exception {

        final String LOGIN_ANNOT = "annotUser";
        final String LOGIN_OTHER = "other";

        final User annotUser = new User(LOGIN_ANNOT);
        final User otherUser = new User(LOGIN_OTHER);
        final Group group = new Group("mygroup", true);

        Mockito.when(userService.findByLogin(LOGIN_ANNOT)).thenReturn(annotUser);
        Mockito.when(userService.findByLogin(LOGIN_OTHER)).thenReturn(otherUser);
        Mockito.when(groupService.isUserMemberOfGroup(annotUser, group)).thenReturn(true);
        Mockito.when(groupService.isUserMemberOfGroup(otherUser, group)).thenReturn(true);

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        meta.setGroup(group);
        annot.setMetadata(meta);
        annot.setUser(annotUser);
        
        meta.setResponseStatus(ResponseStatus.SENT);

        // verify: status is SENT, but user belongs to same group -> it can be updated
        Assert.assertTrue(annotPermMockService.hasUserPermissionToUpdateAnnotation(annot,
                new UserInformation(otherUser, Authorities.ISC)));
    }

    // an ISC user wants to update a SENT annotation; he does not belong to the same group as the annotation
    @Test
    public void testHasPermissionToUpdateSuccessful_IscUserOfOtherGroup() throws Exception {

        final String LOGIN_ANNOT = "annotUser";
        final String LOGIN_OTHER = "other";

        final User annotUser = new User(LOGIN_ANNOT);
        final User otherUser = new User(LOGIN_OTHER);
        final Group group = new Group("mygroup", true);

        Mockito.when(userService.findByLogin(LOGIN_ANNOT)).thenReturn(annotUser);
        Mockito.when(userService.findByLogin(LOGIN_OTHER)).thenReturn(otherUser);
        Mockito.when(groupService.isUserMemberOfGroup(annotUser, group)).thenReturn(true);
        Mockito.when(groupService.isUserMemberOfGroup(otherUser, group)).thenReturn(false); //

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        meta.setGroup(group);
        annot.setMetadata(meta);
        annot.setUser(annotUser);

        meta.setResponseStatus(ResponseStatus.SENT);

        // verify: status is SENT, but user does not belong to same group -> it cannot be updated
        Assert.assertFalse(annotPermMockService.hasUserPermissionToUpdateAnnotation(annot,
                new UserInformation(otherUser, Authorities.ISC)));
    }

 // test that user is not considered belonging to same entity if no information is available at all
    @Test
    public void testIsResponseFromUsersEntity_RespIdNull() throws Exception {

        Assert.assertFalse(callIsResponseFromUsersEntity(annotPermMockService, null, null));
    }

    // test that user is not considered belonging to same entity if no information is available at all
    @Test
    public void testIsResponseFromUsersEntity_RespIdEmpty() throws Exception {

        Assert.assertFalse(callIsResponseFromUsersEntity(annotPermMockService, null, null));
    }

    // test that user is not considered belonging to same entity if no information is available at all
    @Test
    public void testIsResponseFromUsersEntity_UserinfoNull() throws Exception {

        Assert.assertFalse(callIsResponseFromUsersEntity(annotPermMockService, null, "SG"));
    }

    // test that user is not considered belonging to same entity if no information is available about user's connected entity
    @Test
    public void testIsResponseFromUsersEntity_UserinfoConnectedEntityEmpty() throws Exception {

        final UserInformation userInfo = new UserInformation("me", Authorities.ISC);
        Assert.assertFalse(callIsResponseFromUsersEntity(annotPermMockService, userInfo, "SG"));
    }

    // test that user is not considered belonging to same entity if user's connected entity is different
    @Test
    public void testIsResponseFromUsersEntity_NotEquals() throws Exception {

        final UserInformation userInfo = new UserInformation("me", Authorities.ISC);
        userInfo.setConnectedEntity("DIGIT");
        Assert.assertFalse(callIsResponseFromUsersEntity(annotPermMockService, userInfo, "AGRI"));
    }

    // test that user is considered belonging to same entity if user's connected entity is identical
    @Test
    public void testIsResponseFromUsersEntity_Identical() throws Exception {

        final UserInformation userInfo = new UserInformation("me", Authorities.ISC);
        userInfo.setConnectedEntity("SJ");
        Assert.assertTrue(callIsResponseFromUsersEntity(annotPermMockService, userInfo, "SJ"));
    }
    
    // -------------------------------------
    // Test help methods that make private methods callable for the test purposes
    // -------------------------------------

    private boolean callIsAnnotationOfUser(final AnnotationPermissionService annotPermService, final Annotation annotParam, final String userLoginParam)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Method method = AnnotationPermissionServiceImpl.class.getDeclaredMethod("isAnnotationOfUser", Annotation.class, String.class);
        method.setAccessible(true);

        return (boolean) method.invoke(annotPermService, annotParam, userLoginParam);
    }

    private boolean callIsResponseFromUsersEntity(final AnnotationPermissionService annotPermService, final UserInformation userParam, final String respIdParam)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Method method = AnnotationPermissionServiceImpl.class.getDeclaredMethod("isResponseFromUsersEntity", UserInformation.class, String.class);
        method.setAccessible(true);

        return (boolean) method.invoke(annotPermService, userParam, respIdParam);
    }
}
