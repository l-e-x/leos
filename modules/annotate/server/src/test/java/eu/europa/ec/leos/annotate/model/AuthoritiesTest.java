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
package eu.europa.ec.leos.annotate.model;

import eu.europa.ec.leos.annotate.Authorities;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AuthoritiesTest {

    /**
     * simple tests for the simple logic contained in {@link Authorities} class
     */
    
    @Test
    public void testIsLeosOk() {
        
        Assert.assertTrue(Authorities.isLeos("LEOS"));
        Assert.assertTrue(Authorities.isLeos(Authorities.EdiT));
    }
    
    @Test
    public void testIsLeosNotOk() {
        
        Assert.assertFalse(Authorities.isLeos(null));
        Assert.assertFalse(Authorities.isLeos(""));
        Assert.assertFalse(Authorities.isLeos("leos"));
        Assert.assertFalse(Authorities.isLeos("Leos"));
        Assert.assertFalse(Authorities.isLeos("LEOS2"));
    }
    
    @Test
    public void testIsIscOk() {
        
        Assert.assertTrue(Authorities.isIsc("ISC"));
        Assert.assertTrue(Authorities.isIsc(Authorities.ISC));
    }
    
    @Test
    public void testIsIscNotOk() {
        
        Assert.assertFalse(Authorities.isIsc(null));
        Assert.assertFalse(Authorities.isIsc(""));
        Assert.assertFalse(Authorities.isIsc("isc"));
        Assert.assertFalse(Authorities.isIsc("Isc"));
        Assert.assertFalse(Authorities.isIsc("ISC2"));
    }
}
