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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.impl.AnnotationConversionServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.AnnotationPermissionServiceImpl;
import eu.europa.ec.leos.annotate.services.impl.AnnotationServiceImpl;
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
public class AnnotationServiceTest {

    /**
     * This class contains a few tests for conversion of Annotations to to-be-JSON-serialized objects 
     * Other business functionality of the AnnotationService is tested in other test classes 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @SuppressWarnings({"PMD.UnusedPrivateField"})
    @InjectMocks
    private AnnotationServiceImpl annotService;

    @SuppressWarnings("PMD.UnusedPrivateField")
    @Mock
    private AnnotationPermissionServiceImpl annotPermService;
    
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

    /**
     * Test that an invalid annotation is not converted
     */
    @Test
    public void testMissingAnnotation() {

        // null converts to null
        Assert.assertNull(conversionService.convertToJsonAnnotation(null, null));
    }

    /**
     * Test that an annotation with a missing URI is not converted
     * Note: this should not happen in real life as annotations are read from the database, and there must be an URI written there
     */
    @Test(expected = NullPointerException.class)
    @SuppressFBWarnings(value = SpotBugsAnnotations.ExceptionIgnored, justification = SpotBugsAnnotations.ExceptionIgnored)
    public void testConvertAnnotationWithMissingUri() {

        final Annotation annot = new Annotation();
        final Group group = new Group();
        final Document doc = new Document();
        doc.setUri(null);
        final Metadata meta = new Metadata(doc, group, Authorities.EdiT);
        annot.setMetadata(meta);

        // should throw exception
        conversionService.convertToJsonAnnotation(annot, null);
        Assert.fail("Expected exception due to missing URI not received");
    }

    /**
     * Test that an annotation with an invalid URI is not converted
     * Note: this should not happen in real life as annotations are read from the database, and there must be a valid URI
     */
    @Test
    public void testConvertAnnotationWithInvalidUri() {

        final String login = "somebody";
        
        // prepare
        Mockito.when(userService.getHypothesisUserAccountFromUser(Mockito.any(User.class), Mockito.anyString())).thenReturn("acct:somebody@something");
        
        final Annotation annot = new Annotation();
        annot.setTargetSelectors("a");

        final Group group = new Group();
        final Document doc = new Document();
        doc.setUri("invalid^uri");

        final Metadata meta = new Metadata(doc, group, Authorities.EdiT);
        annot.setMetadata(meta);

        annot.setUser(new User(login));

        // should not throw exception
        final JsonAnnotation converted = conversionService.convertToJsonAnnotation(annot, new UserInformation(login, Authorities.EdiT));
        Assert.assertNotNull(converted);
        Assert.assertNull(converted.getUri());
    }
    
}
