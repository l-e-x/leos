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
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.model.entity.Metadata.ResponseStatus;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionService;
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

@RunWith(MockitoJUnitRunner.class)
public class AnnotationPermissionServiceTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @InjectMocks
    private AnnotationPermissionService annotPermMockService;

    @Mock
    private UserServiceImpl userService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that no permissions for viewing an annotation are given in case of invalid parameters
    @Test
    public void testNoPermissionsForSeeingAnnotationWithInvalidArguments() throws Exception {

        final AnnotationPermissionService annotService = new AnnotationPermissionService();

        Assert.assertFalse(callHasUserPermissionToSeeAnnotation(annotService, null, null));
        Assert.assertFalse(callHasUserPermissionToSeeAnnotation(annotService, new Annotation(), null));
        Assert.assertFalse(callHasUserPermissionToSeeAnnotation(annotService, null, new User()));
    }

    // test that annotation is not by default considered as belonging to an undefined user
    @Test
    public void testIsAnnotationOfUserFails() throws Exception {

        final String login = "somebody";

        Mockito.when(userService.findByLogin(login)).thenReturn(null);

        Assert.assertFalse(callIsAnnotationOfUser(annotPermMockService, null, login));
    }

    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    public void testCanAnnotationBeUpdatedNull() throws Exception {

        final Annotation annot = null;
        Assert.assertFalse(callCanAnnotationBeUpdated(annotPermMockService, annot));
    }

    @Test
    public void testCanAnnotationBeUpdatedOtherMetadata() throws Exception {

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        annot.setMetadata(meta);

        meta.setResponseStatus(ResponseStatus.IN_PREPARATION);

        // verify: status is not SENT -> it can be updated
        Assert.assertTrue(callCanAnnotationBeUpdated(annotPermMockService, annot));
    }

    @Test
    public void testCanAnnotationBeUpdatedSuccessful() throws Exception {

        final Annotation annot = new Annotation();
        final Metadata meta = new Metadata();
        annot.setMetadata(meta);

        meta.setResponseStatus(ResponseStatus.SENT);

        // verify: status is SENT -> it cannot be updated any more
        Assert.assertFalse(callCanAnnotationBeUpdated(annotPermMockService, annot));
    }

    // -------------------------------------
    // Test help methods that make private methods callable for the test purposes
    // -------------------------------------

    private boolean callHasUserPermissionToSeeAnnotation(final AnnotationPermissionService annotService, final Annotation annotParam, final User userParam)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Method method = AnnotationPermissionService.class.getDeclaredMethod("hasUserPermissionToSeeAnnotation", Annotation.class, User.class);
        method.setAccessible(true);

        return (boolean) method.invoke(annotService, annotParam, userParam);
    }

    private boolean callIsAnnotationOfUser(final AnnotationPermissionService annotPermService, final Annotation annotParam, final String userLoginParam)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Method method = AnnotationPermissionService.class.getDeclaredMethod("isAnnotationOfUser", Annotation.class, String.class);
        method.setAccessible(true);

        return (boolean) method.invoke(annotPermService, annotParam, userLoginParam);
    }


    private boolean callCanAnnotationBeUpdated(final AnnotationPermissionService annotService, final Annotation annotParam)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Method method = AnnotationPermissionService.class.getDeclaredMethod("canAnnotationBeUpdated", Annotation.class);
        method.setAccessible(true);

        return (boolean) method.invoke(annotService, annotParam);
    }
}
