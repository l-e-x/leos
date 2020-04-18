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
package eu.europa.ec.leos.annotate.entity;

import eu.europa.ec.leos.annotate.model.entity.Metadata;
import org.assertj.core.api.StringAssert;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SuppressWarnings("PMD.TooManyMethods")
public class MetadataTest {

    /**
     * simple tests for methods of the Metadata class
     */

    @Test
    public void testIscReferenceEmpty() {

        final Metadata meta = new Metadata();
        Assert.assertEquals("", meta.getIscReference());
    }

    @Test
    public void testIscReferenceFound() {

        final String IscReference = "ISC/2015/048";
        final Metadata meta = new Metadata();
        meta.setKeyValuePairs("ISCReference:" + IscReference);

        Assert.assertEquals(IscReference, meta.getIscReference());
    }

    @Test
    public void testIsResponseSent() {

        final Metadata meta = new Metadata();
        meta.setResponseStatus(Metadata.ResponseStatus.SENT);

        Assert.assertTrue(meta.isResponseStatusSent());
    }

    @Test
    public void testIsNotResponseSent1() {

        final Metadata meta = new Metadata();
        meta.setResponseStatus(Metadata.ResponseStatus.IN_PREPARATION);

        Assert.assertFalse(meta.isResponseStatusSent());
    }

    @Test
    public void testIsNotResponseSent2() {

        final Metadata meta = new Metadata();
        meta.setResponseStatus(Metadata.ResponseStatus.UNKNOWN);

        Assert.assertFalse(meta.isResponseStatusSent());
    }

    @Test
    public void testGetResponseId_Empty() {

        final Metadata meta = new Metadata();

        final StringAssert strAss = new StringAssert(meta.getResponseId());
        strAss.isNull();
    }

    @Test
    public void testGetResponseId() {

        final Metadata meta = new Metadata();
        meta.setKeyValuePairs("responseId:something");

        final StringAssert strAss = new StringAssert(meta.getResponseId());
        strAss.isEqualTo("something");
    }

    @Test
    public void testGetResponseVersion_Empty() {

        final Metadata meta = new Metadata();

        final long respVers = meta.getResponseVersion();
        Assert.assertEquals(-1L, respVers);
    }

    @Test
    public void testGetResponseVersion() {

        final Metadata meta = new Metadata();
        meta.setKeyValuePairs("responseVersion:4");

        final long respVers = meta.getResponseVersion();
        Assert.assertEquals(4L, respVers);
    }

    @Test
    public void testRemoveResponseVersion() {

        final Metadata meta = new Metadata();
        meta.setKeyValuePairs("responseVersion:4\nsomething:a");

        // act
        meta.removeResponseVersion();

        Assert.assertEquals("something:a\n", meta.getKeyValuePairs());
    }

    @Test
    public void testRemoveResponseVersion_noResponseVersionSet() {

        final Metadata meta = new Metadata();

        // act
        meta.removeResponseVersion();

        Assert.assertEquals("", meta.getKeyValuePairs());
    }
    
    @Test
    public void testSetResponseVersion() {
        
        final Metadata meta = new Metadata();
        
        // act
        meta.setResponseVersion(4);
        
        Assert.assertEquals(4, meta.getResponseVersion());
        Assert.assertEquals("4", meta.getAllMetadataAsSimpleMetadata().get(Metadata.PROP_RESPONSE_VERSION));
    }
}
