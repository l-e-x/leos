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
import eu.europa.ec.leos.annotate.services.impl.OffsetBasedPageRequest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class OffsetBasedPageRequestTest {

    /**
     * Basic tests for our OffsetBasedPageRequest object
     * (we check that basic functionality is given to assure that it operates smoothly when used for DB access)
     */
    
    /**
     * Test that creation of {@link OffsetBasedPageRequest} with illegal parameters fails
     */
    @Test
    @SuppressFBWarnings(value = "DE_MIGHT_IGNORE", justification = "Intended for test")
    public void testIllegalParameters() {

        try {
            new OffsetBasedPageRequest(-1, 100, null); // negative offset is not allowed 
            Assert.fail("Expected exception not received during creation of invalid page request");
        } catch(Exception e) {
            // OK
        }
        
        try {
            new OffsetBasedPageRequest(0, 0, null); // limit <=0 is not allowed 
            Assert.fail("Expected exception not received during creation of invalid page request");
        } catch(Exception e) {
            // OK
        }
    }
    
    /**
     * Test basic functionality
     */
    @Test
    public void testPageRequest() {
        
        OffsetBasedPageRequest req = new OffsetBasedPageRequest(51, 10, new Sort(Direction.ASC, "thecolumn"));
        Assert.assertEquals(51, req.getOffset());
        Assert.assertEquals(10, req.getPageSize()); // by design, a page has the length of the limit
        Assert.assertEquals(5, req.getPageNumber()); // we are on fifth page...
        Assert.assertNotNull(req.getSort());
        
        // ... so there is a preceding page...
        Assert.assertTrue(req.hasPrevious());
        OffsetBasedPageRequest prevPage = req.previous();
        Assert.assertNotNull(prevPage);
        Assert.assertEquals(4, prevPage.getPageNumber());
        
        // ... and there is a next
        OffsetBasedPageRequest nextPage = (OffsetBasedPageRequest)req.next();
        Assert.assertNotNull(nextPage);
        Assert.assertEquals(6, nextPage.getPageNumber());
        
        // check that remaining accesses are also valid
        Assert.assertNotNull(req.first());
        Assert.assertNotNull(req.previousOrFirst());
    }
    
    /**
     * Check behavior when page is the first page already
     */
    @Test
    public void testFirstPage() {
        
        OffsetBasedPageRequest req = new OffsetBasedPageRequest(0, 20, null);
        
        // ... so there is a no preceding page...
        Assert.assertFalse(req.hasPrevious());
        OffsetBasedPageRequest prevPage = req.previous();
        Assert.assertNotNull(prevPage);
        Assert.assertEquals(req, prevPage); // when there is no previous page, we receive the current page again...
        Assert.assertEquals(req, req.first()); // .... which is also the first page
        Assert.assertEquals(req, req.previousOrFirst());
    }
}
