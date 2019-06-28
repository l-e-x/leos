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

import eu.europa.ec.leos.annotate.model.SimpleMetadata;
import eu.europa.ec.leos.annotate.model.SimpleMetadataWithStatuses;
import eu.europa.ec.leos.annotate.model.entity.Annotation.AnnotationStatus;
import eu.europa.ec.leos.annotate.model.web.IncomingSearchOptions;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
public class AnnotationSearchOptionsTest {

    /**
     * Tests on the AnnotationSearchOptions class' default values and behaviour
     */

    private static final String URL = "http://the.url";
    private static final String WORLD_GROUP = "__world__";
    private static final String CREATED = "created";
    private static final String ASC = "asc";

    // if negative limit is given, options should be prepared to be able to retrieve all items
    @Test
    public void testNegativeLimitSetToMaximum_Offset0() {

        // giving a negative limit should set the limit to the maximum Integer value
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                -1,          // limit
                0,           // offset
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given negative limit parameter was not increased to maximum possible value",
                Integer.MAX_VALUE, options.getItemLimit()); // was increased to maximum value
    }

    // if negative limit is given, options should be prepared to be able to retrieve all items
    @Test
    public void testNegativeLimitSetToMaximum_OffsetPositive() {

        // negative limit, set an arbitrary offset -> offset should be set to 0, since we want all items without paging
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                -1,          // limit
                50,          // offset
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given negative limit parameter was not increased to maximum possible value",
                Integer.MAX_VALUE, options.getItemLimit()); // was increased to maximum value
        Assert.assertEquals("offset was not set to default (due to negative limit value)",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET), options.getItemOffset()); // was increased to maximum value
    }

    // if negative limit is given, options should be prepared to be able to retrieve all items
    @Test
    public void testNegativeLimitSetToMaximum_OffsetNegative() {

        // negative limit, set a negative offset -> offset should be set to default
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                -1,          // limit
                -5,          // offset
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given negative limit parameter was not increased to maximum possible value",
                Integer.MAX_VALUE, options.getItemLimit()); // was increased to maximum value
        Assert.assertEquals("offset was not set to default (due to negative limit value)",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET), options.getItemOffset()); // was increased to maximum value
    }

    // any given positive maximum limit is kept now
    @Test
    public void testMaximumLimitAccepted() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                5000,        // limit
                0,           // offset
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given positive limit value was not kept", 5000, options.getItemLimit()); // was kept
        Assert.assertEquals(0, options.getItemOffset()); // was kept
    }

    // negative offset is set to zero
    @Test
    public void testNegativeOffsetCorrected() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                200,         // limit
                -1,          // offset
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given invalid negative offset value was not set to 0",
                0, options.getItemOffset()); // was increased to non-negative value
    }

    // default values are used if given limit is 0
    @Test
    public void testDefaultValuesWhenLimitIsZero_NegativeOffset() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                0,           // limit
                -1,          // offset: will be set to default as well since value is invalid
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given invalid zero limit was not set to default",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_LIMIT), options.getItemLimit()); // was set to default
        Assert.assertEquals("offset value was not set to default",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_OFFSET), options.getItemOffset()); // was set to default
    }

    // default values are used if given limit is 0
    @Test
    public void testDefaultValuesWhenLimitIsZero_PositiveOffset() {

        // offset has a valid value and is thus not set back to default offset value
        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                0,           // limit
                10,          // offset: will be kept (not set to default) as value is valid
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertEquals("given invalid zero limit was not set to default",
                Integer.parseInt(AnnotationSearchOptions.DEFAULT_SEARCH_LIMIT), options.getItemLimit()); // was set to default
        Assert.assertEquals("offset value was modified, should not",
                10, options.getItemOffset()); // was set to default
    }

    // check that valid sort columns are accepted as being valid
    @Test
    public void testValidSortColumnCreated() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                200,         // limit
                0,           // offset
                ASC,         // order
                CREATED);    // sortColumn
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getSort());
    }

    // check that valid sort columns are accepted as being valid
    @Test
    public void testValidSortColumnUpdated() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                200,         // limit
                0,           // offset
                "desc",      // order
                "updated");  // sortColumn
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getSort());
        Assert.assertEquals(Direction.DESC, options.getOrder());
    }

    // check that valid sort columns are accepted as being valid
    @Test
    public void testValidSortColumnShared() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                200,         // limit
                0,           // offset
                ASC,         // order
                "shared");   // sortColumn
        Assert.assertNotNull(options);
        Assert.assertNotNull(options.getSort());
        Assert.assertEquals("shared", options.getSortColumn());
        Assert.assertEquals(Direction.ASC, options.getOrder());
    }

    // check that invalid sort columns are ignored
    @Test
    public void testInvalidSortColumnText() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,         // URI
                WORLD_GROUP, // group
                true,        // separateReplies
                200,         // limit
                0,           // offset
                ASC,         // order
                "text");     // sortColumn: invalid
        Assert.assertNotNull(options);
        Assert.assertNull("invalid sorting column was not ignored", options.getSort());
    }

    // check that invalid sort columns are ignored
    @Test
    public void testInvalidSortColumnReferences() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,            // URI
                WORLD_GROUP,    // group
                true,           // separateReplies
                200,            // limit
                0,              // offset
                ASC,            // order
                "references");  // sortColumn: invalid
        Assert.assertNotNull(options);
        Assert.assertNull("invalid sorting column was not ignored", options.getSort());

        options.setSortColumn("");
        Assert.assertEquals("", options.getSortColumn());
        Assert.assertNull(options.getSort());
    }

    // check that invalid URIs are not accepted in constructor
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUriInConstructor() {

        new AnnotationSearchOptions(
                "invalid^url", // URI, is invalid
                WORLD_GROUP,   // group
                true,          // separateReplies
                200,           // limit
                0,             // offset
                ASC,           // order
                "text2");       // sortColumn
    }

    // check that invalid URIs are not accepted during updating URI
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidUriInSetter() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,              // URI, is valid
                WORLD_GROUP,      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                ASC,              // order
                "text2");          // sortColumn
        options.setUri("other^url"); // previously valid URI is overwritten -> exception!
    }

    // check that setting status null still keeps the NORMAL status
    @Test
    public void testSetStatuses() {

        final AnnotationSearchOptions options = new AnnotationSearchOptions(
                URL,              // URI, is valid
                WORLD_GROUP,      // group
                true,             // separateReplies
                200,              // limit
                0,                // offset
                ASC,              // order
                "text4");          // sortColumn

        // create two metadata sets
        final SimpleMetadataWithStatuses smws1 = new SimpleMetadataWithStatuses();
        smws1.getMetadata().put("key1", "value1");
        smws1.setStatuses(Arrays.asList(AnnotationStatus.DELETED));

        final SimpleMetadataWithStatuses smws2 = new SimpleMetadataWithStatuses();
        smws2.getMetadata().put("key2", "value2");
        smws2.getMetadata().put("key3", "value3");
        smws2.setStatuses(Arrays.asList(AnnotationStatus.ACCEPTED, AnnotationStatus.NORMAL));

        options.setMetadataMapsWithStatusesList(Arrays.asList(smws1, smws2));

        // act
        options.setStatuses(Arrays.asList(AnnotationStatus.NORMAL, AnnotationStatus.REJECTED));

        // verify that the new statuses have been applied to all metadata sets
        Assert.assertEquals(2, options.getMetadataMapsWithStatusesList().size());
        options.getMetadataMapsWithStatusesList().forEach(
                smws -> Assert.assertThat(smws.getStatuses(),
                        org.hamcrest.Matchers.containsInAnyOrder(AnnotationStatus.NORMAL, AnnotationStatus.REJECTED)));
    }

    // check that creation of AnnotationSearchOptions from IncomingSearchOptions fails when no input is given
    @Test(expected = IllegalArgumentException.class)
    public void testInitFromIncomingSearchOptionsFails() {

        AnnotationSearchOptions.fromIncomingSearchOptions(null, true);
    }

    // check successful that creation of AnnotationSearchOptions from IncomingSearchOptions
    @Test
    public void testInitFromIncomingSearchOptions() {

        final IncomingSearchOptions incOpts = new IncomingSearchOptions();
        incOpts.setAny("any");
        incOpts.setGroup("myGroup");
        incOpts.setLimit(48);
        incOpts.setOffset(5);
        incOpts.setOrder(ASC);
        incOpts.setSort("created");
        incOpts.setMetadatasets("[{\"prop1\":\"val1\", \"status\":[\"DELETED\"]}," +
                "{\"set2prop1\":\"val2\", \"status\": [\"ACCEPTED\",\"REJECTED\"]}]");
        incOpts.setTag("tag");
        incOpts.setUri("https://leos/84");
        incOpts.setUser("user");

        final AnnotationSearchOptions opts = AnnotationSearchOptions.fromIncomingSearchOptions(incOpts, true);
        Assert.assertNotNull(opts);
        Assert.assertEquals(true, opts.isSeparateReplies());
        Assert.assertEquals(incOpts.getGroup(), opts.getGroup());

        Assert.assertEquals(incOpts.getLimit(), opts.getItemLimit());
        Assert.assertEquals(incOpts.getOffset(), opts.getItemOffset());

        Assert.assertEquals("created", opts.getSortColumn());
        Assert.assertEquals("ASC", opts.getOrder().name());

        Assert.assertEquals(2, opts.getMetadataMapsWithStatusesList().size());
        final SimpleMetadata meta0 = opts.getMetadataMapsWithStatusesList().get(0).getMetadata();
        Assert.assertEquals(1, meta0.size());
        Assert.assertEquals("val1", meta0.get("prop1"));

        final List<AnnotationStatus> status0 = opts.getMetadataMapsWithStatusesList().get(0).getStatuses();
        Assert.assertEquals(1, status0.size());
        Assert.assertEquals(AnnotationStatus.DELETED, status0.get(0));

        final SimpleMetadata meta1 = opts.getMetadataMapsWithStatusesList().get(1).getMetadata();
        Assert.assertEquals(1, meta1.size());
        Assert.assertEquals("val2", meta1.get("set2prop1"));

        final List<AnnotationStatus> status1 = opts.getMetadataMapsWithStatusesList().get(1).getStatuses();
        Assert.assertEquals(2, status1.size());
        Assert.assertEquals(AnnotationStatus.ACCEPTED, status1.get(0));
        Assert.assertEquals(AnnotationStatus.REJECTED, status1.get(1));

        Assert.assertEquals(incOpts.getUri(), opts.getUri().toString());
    }
}
