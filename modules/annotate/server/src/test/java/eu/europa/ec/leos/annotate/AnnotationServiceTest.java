/*
 * Copyright 2018 European Commission
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
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import eu.europa.ec.leos.annotate.model.entity.Document;
import eu.europa.ec.leos.annotate.model.web.annotation.JsonAnnotation;
import eu.europa.ec.leos.annotate.services.AnnotationService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationServiceTest {

    /**
     * This class contains a few tests for conversion of Annotations to to-be-JSON-serialized objects 
     * Other business functionality of the AnnotationService is tested in other test classes 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AnnotationService annotService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * Test that an invalid annotation is not converted
     */
    @Test
    public void testMissingAnnotation() {

        // null converts to null
        Assert.assertNull(annotService.convertToJsonAnnotation(null));
    }
    
    /**
     * Test that an annotation with a missing URI is not converted
     * Note: this should not happen in real life as annotations are read from the database, and there must be an URI written there
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testConvertAnnotationWithMissingUri() {

        Annotation annot = new Annotation();
        Document doc = new Document();
        doc.setUri(null);
        annot.setDocument(doc);
        
        // should throw exception
        try {
            annotService.convertToJsonAnnotation(annot);
            Assert.fail("Expected exception due to missing URI not received");
        } catch(Exception e) {
            // OK
        }
    }
    
    /**
     * Test that an annotation with an invalid URI is not converted
     * Note: this should not happen in real life as annotations are read from the database, and there must be a valid URI
     */
    @Test
    public void testConvertAnnotationWithInvalidUri() {

        Annotation annot = new Annotation();
        annot.setTargetSelectors("a");
        
        Document doc = new Document();
        doc.setUri("invalid^uri");
        annot.setDocument(doc);
        
        // should not throw exception
        JsonAnnotation converted = annotService.convertToJsonAnnotation(annot);
        Assert.assertNotNull(converted);
        Assert.assertNull(converted.getUri());
    }
}
