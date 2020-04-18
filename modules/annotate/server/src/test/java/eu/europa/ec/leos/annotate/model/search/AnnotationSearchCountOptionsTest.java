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
package eu.europa.ec.leos.annotate.model.search;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AnnotationSearchCountOptionsTest {

    @Test
    public void testValidUri() {

        final AnnotationSearchCountOptions asco = new AnnotationSearchCountOptions("http://val.id", "group", "");
        Assert.assertNotNull(asco);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUri() {
        
        new AnnotationSearchCountOptions("http://in^val.id", "group", "");
    }
    
    @Test
    public void testNothingToDecodeWhenMetadataEmpty() {
        
        final AnnotationSearchCountOptions asco = new AnnotationSearchCountOptions("http://some.url", "thegroup", "");
        Assert.assertEquals("", asco.getMetadatasets());
        
        // act
        asco.decodeEscapedBrackets();
        Assert.assertEquals("", asco.getMetadatasets());
        
    }
    
    @Test
    public void testDecodeMetadataEncoding() {
        
        final String metadata = "%7B\"prop\":\"val\\end\"%7D";
        final String metadataDecoded = "{\"prop\":\"valend\"}";
        final AnnotationSearchCountOptions asco = new AnnotationSearchCountOptions("http://some.url", "thegroup", metadata);
        Assert.assertEquals(metadata, asco.getMetadatasets());
        
        // act
        asco.decodeEscapedBrackets();
        Assert.assertEquals(metadataDecoded, asco.getMetadatasets());
        
    }
}
