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

import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AnnotationServiceTest {

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test standard case for a public annotation
    @Test
    public void testIsNoPrivateAnnotation() throws Exception {

        final AnnotationServiceImpl annotService = new AnnotationServiceImpl();

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:user@domain");
        Assert.assertFalse(callIsPrivateAnnotation(annotService, jsAnnot));
    }

    // test standard case for a private annotation
    @Test
    public void testIsPrivateAnnotation() throws Exception {

        final AnnotationServiceImpl annotService = new AnnotationServiceImpl();
        final String user = "acct:user@domain";

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject(user);

        final List<String> userPermission = new ArrayList<String>();
        userPermission.add(user);
        jsAnnot.getPermissions().setRead(userPermission);

        Assert.assertTrue(callIsPrivateAnnotation(annotService, jsAnnot));
    }

    // test that the method for checking a private annotation returns {@literal false} on all other code paths
    @Test
    public void testIsPrivateAnnotationOnIncompleteAnnotation() throws Exception {

        final AnnotationServiceImpl annotService = new AnnotationServiceImpl();

        Assert.assertFalse(callIsPrivateAnnotation(annotService, null));

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("acct:user@domain");

        jsAnnot.getPermissions().setRead(null);
        Assert.assertFalse(callIsPrivateAnnotation(annotService, jsAnnot));

        jsAnnot.getPermissions().setRead(new ArrayList<String>());
        Assert.assertFalse(callIsPrivateAnnotation(annotService, jsAnnot));

        final List<String> tooManyReadPermissions = new ArrayList<String>();
        tooManyReadPermissions.add("firstuser");
        tooManyReadPermissions.add("anotherUser");
        jsAnnot.getPermissions().setRead(tooManyReadPermissions);

        Assert.assertFalse(callIsPrivateAnnotation(annotService, jsAnnot));
    }

    // -------------------------------------
    // Test help methods that make private methods callable for the test purposes
    // -------------------------------------

    private boolean callIsPrivateAnnotation(final AnnotationServiceImpl annotService, final JsonAnnotation annotParam)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        final Method method = AnnotationServiceImpl.class.getDeclaredMethod("isPrivateAnnotation", JsonAnnotation.class);
        method.setAccessible(true);

        return (boolean) method.invoke(annotService, annotParam);
    }

}
