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
package eu.europa.ec.leos.annotate.model.web.annotation;

import eu.europa.ec.leos.annotate.helper.TestData;
import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;

@RunWith(SpringRunner.class)
public class JsonAnnotationTest {

    /**
     * simple tests for helper functions of {@link JsonAnnotation} class
     */

    @Test
    public void testIsReply() throws Exception {

        final JsonAnnotation jsAnnot = TestData.getTestReplyToAnnotation("username", new URI("uri"), Arrays.asList("parentId"));
        Assert.assertTrue(jsAnnot.isReply());
    }

    @Test
    public void testIsNoReply() throws Exception {

        final JsonAnnotation jsAnnot = TestData.getTestAnnotationObject("username");
        Assert.assertFalse(jsAnnot.isReply());
    }

    @Test
    public void testGetRootAnnotationId() {

        final String ROOT_ID = "rootId";
        final String REPLY_ID = "firstReplyId";

        final JsonAnnotation jsAnnot = new JsonAnnotation();
        jsAnnot.setReferences(null);

        Assert.assertNull(jsAnnot.getRootAnnotationId());

        jsAnnot.setReferences(new ArrayList<String>());
        Assert.assertNull(jsAnnot.getRootAnnotationId());

        jsAnnot.setReferences(Arrays.asList(ROOT_ID));
        Assert.assertEquals(ROOT_ID, jsAnnot.getRootAnnotationId());

        jsAnnot.setReferences(Arrays.asList(ROOT_ID, REPLY_ID));
        Assert.assertEquals(ROOT_ID, jsAnnot.getRootAnnotationId());
    }
    
    @Test
    public void testHasMetadata() {
        
        final JsonAnnotation jsAnnot = new JsonAnnotation();
        Assert.assertFalse(jsAnnot.hasMetadata());
        
        final JsonAnnotationDocument jsDoc = new JsonAnnotationDocument();
        jsAnnot.setDocument(jsDoc);
        Assert.assertFalse(jsAnnot.hasMetadata());
        
        // finally initialize the metadata, now it should say {@literal true}
        jsDoc.setMetadata(new SimpleMetadata());
        Assert.assertTrue(jsAnnot.hasMetadata());
    }
}
