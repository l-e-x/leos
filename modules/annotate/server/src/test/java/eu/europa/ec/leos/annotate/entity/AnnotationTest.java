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
package eu.europa.ec.leos.annotate.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationTest {

    /**
     * This class contains tests for functions operating on annotation entities
     */

    // -------------------------------------
    // Tests
    // -------------------------------------

    // annotation does not denote a reply -> check that respective properties return expected values accordingly
    @Test
    public void testNoReply() {

        Annotation ann = new Annotation();
        ann.setId("id");

        // no reply!
        Assert.assertFalse(ann.isReply());
        Assert.assertNull(ann.getReferences());
        Assert.assertNull(ann.getReferencesList());

        // change value, but still it is no reply!
        ann.setReferences("");
        Assert.assertFalse(ann.isReply());
        Assert.assertEquals("", ann.getReferences());
        Assert.assertNull(ann.getReferencesList());
    }
    
    // set empty values for the references -> annotation should not be considered being a reply
    @SuppressFBWarnings(value = "NP_LOAD_OF_KNOWN_NULL_VALUE", justification = "by intention")
    @Test
    public void testAnnotationEmptyReferences() {
        
        Annotation ann = new Annotation();
        ann.setId("id");
        
        List<String> referencesList = null;
        ann.setReferences(referencesList);
        Assert.assertFalse(ann.isReply());
        Assert.assertNull(ann.getReferencesList());
        
        // different value, but still no references
        referencesList = new ArrayList<String>();
        ann.setReferences(referencesList);
        Assert.assertFalse(ann.isReply());
        Assert.assertNull(ann.getReferencesList());
    }
    
    // set meaningful value to references -> annotation becomes a reply
    @Test
    public void testAnnotationBecomesReply() {
        
        Annotation ann = new Annotation();
        ann.setId("id");
        Assert.assertFalse(ann.isReply());
        
        // now set references to other items
        List<String> referencesList = Arrays.asList("ref1", "ref2");
        ann.setReferences(referencesList);
        
        // now it became a reply
        Assert.assertTrue(ann.isReply());
        Assert.assertNotNull(ann.getReferencesList());
        Assert.assertEquals(2, ann.getReferencesList().size());
        Assert.assertTrue(ann.getReferencesList().contains("ref1"));
        Assert.assertTrue(ann.getReferencesList().contains("ref2"));
    }
}
