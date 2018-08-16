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

import eu.europa.ec.leos.annotate.model.AnnotationSearchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class AnnotationSearchOptionsTest {

    /**
     * Tests on the AnnotationSearchOptions class' default values and behavior
     */

    // if negative limit is given, options should be prepared to be able to retrieve all items
    @Test
    public void testNegativeLimitSetToMaximum() {

        // giving a negative limit should set the limit to the maximum Integer value
        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                -1,               // limit
                0,                // offset
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given negative limit parameter was not increased to maximum possible value",
                Integer.MAX_VALUE, options.getItemLimit()); // was increased to maximum value

        // repeat, and set an arbitrary offset -> offset should be set to 0, since we want all items without paging
        options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                -1,               // limit
                50,               // offset
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given negative limit parameter was not increased to maximum possible value",
                Integer.MAX_VALUE, options.getItemLimit()); // was increased to maximum value
        Assert.assertEquals("offset was not set to default (due to negative limit value)",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET), options.getItemOffset()); // was increased to maximum value

        // repeat, and set a negative offset -> offset should be set to default
        options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                -1,               // limit
                -5,               // offset
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given negative limit parameter was not increased to maximum possible value",
                Integer.MAX_VALUE, options.getItemLimit()); // was increased to maximum value
        Assert.assertEquals("offset was not set to default (due to negative limit value)",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET), options.getItemOffset()); // was increased to maximum value
    }

    // any given positive maximum limit is kept now
    @Test
    public void testMaximumLimitAccepted() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                5000,             // limit
                0,                // offset
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given positive limit value was not kept", 5000, options.getItemLimit()); // was kept
        Assert.assertEquals(0, options.getItemOffset()); // was kept
    }

    // negative offset is set to zero
    @Test
    public void testNegativeOffsetCorrected() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                200,              // limit
                -1,               // offset
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given invalid negative offset value was not set to 0",
                0, options.getItemOffset()); // was increased to non-negative value
    }

    // default values are used if given limit is 0
    @Test
    public void testDefaultValuesWhenLimitIsZero() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                0,                // limit
                -1,               // offset: will be set to default as well since value is invalid
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given invalid zero limit was not set to default",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_LIMIT), options.getItemLimit()); // was set to default
        Assert.assertEquals("offset value was not set to default",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET), options.getItemOffset()); // was set to default
        
        // offset has a valid value and is thus not set back to default offset value
        options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                0,                // limit
                10,               // offset: will be kept (not set to default) as value is valid
                "asc",            // order
                "created");       // sortColumn
        Assert.assertEquals("given invalid zero limit was not set to default",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_LIMIT), options.getItemLimit()); // was set to default
        Assert.assertEquals("offset value was modified, should not",
                10, options.getItemOffset()); // was set to default
    }

    // check that valid sort columns are accepted as being valid
    @Test
    public void testValidSortColumns() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                "asc",            // order
                "created");       // sortColumn
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getSort());

        options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                "desc",           // order
                "updated");       // sortColumn
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getSort());
        Assert.assertEquals(Direction.DESC, options.getOrder());

        options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                "asc",            // order
                "shared");        // sortColumn
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getSort());
        Assert.assertEquals("shared", options.getSortColumn());
        Assert.assertEquals(Direction.ASC, options.getOrder());
    }

    // check that invalid sort columns are ignored
    @Test
    public void testInvalidSortColumns() {

        AnnotationSearchOptions options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                "asc",            // order
                "text");          // sortColumn: invalid
        Assert.assertNotNull(options);
        Assert.assertNull("invalid sorting column was not ignored", options.getSort());

        options = new AnnotationSearchOptions(
                "http://the.url", // URI
                "__world__",      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                "asc",            // order
                "references");    // sortColumn: invalid
        Assert.assertNotNull(options);
        Assert.assertNull("invalid sorting column was not ignored", options.getSort());
    }

    // check that invalid URIs are not accepted
    @Test
    public void testInvalidUri() {

        try {
            new AnnotationSearchOptions(
                    "invalid^url", // URI, is invalid
                    "__world__",   // group
                    true,          // separateReplies
                    200,           // limit
                    0,             // offset
                    "asc",         // order
                    "text");       // sortColumn
            Assert.fail("Expected exception due to invalid URI not received!");
        } catch (Exception e) {
            // OK
            Assert.assertNotNull(e);
        }

        try {
            AnnotationSearchOptions options = new AnnotationSearchOptions(
                    "http://url.net", // URI, is valid
                    "__world__",      // group
                    true,             // separateReplies
                    200,              // limit
                    0,                // offset
                    "asc",            // order
                    "text");          // sortColumn
            options.setUri("other^url"); // previously valid URI is overwritten -> exception!
            Assert.fail("Expected exception due to invalid URI not received!");
        } catch (Exception e) {
            // OK
            Assert.assertNotNull(e);
        }
    }

}
