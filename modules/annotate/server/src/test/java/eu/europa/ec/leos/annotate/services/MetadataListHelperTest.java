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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.model.entity.Metadata;
import eu.europa.ec.leos.annotate.services.impl.MetadataListHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class MetadataListHelperTest {

    // test that retrieving IDs of list of Metadata objects works as expected for empty inputs
    @Test
    public void testGetMetadataSetIds_EmptyInput() {

        Assert.assertNull(MetadataListHelper.getMetadataSetIds(null));

        // empty list should also return null
        final List<Metadata> testList = new ArrayList<Metadata>();
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(testList));

        // null entries should be discarded -> empty list -> return null
        testList.add(null);
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(testList));
    }

    // test that retrieving IDs of list of Metadata objects works as expected: ignoring null, filtering duplicates
    @Test
    public void testGetMetadataSetIds_Filtered() {

        final List<Metadata> testList = new ArrayList<Metadata>();

        final Metadata firstItem = new Metadata();
        firstItem.setId(4);
        testList.add(firstItem);

        testList.add(null);

        final Metadata secondItem = new Metadata();
        secondItem.setId(8);
        testList.add(secondItem);

        final Metadata thirdItem = new Metadata();
        thirdItem.setId(4);
        testList.add(thirdItem);

        testList.add(null);

        // null entries should be discarded, duplicates be filtered -> 2 items remain
        final List<Long> resultList = MetadataListHelper.getMetadataSetIds(testList);
        Assert.assertEquals(2, resultList.size());
        Assert.assertTrue(resultList.contains(Long.valueOf(4)));
        Assert.assertTrue(resultList.contains(Long.valueOf(8)));
    }

    // test that retrieving IDs of lists of Metadata objects works as expected for empty inputs
    @Test
    public void testGetMetadataSetIds_Lists_EmptyInput() {

        Assert.assertNull(MetadataListHelper.getMetadataSetIds(null, null));

        // empty list should also return null
        final List<Metadata> testList = new ArrayList<Metadata>();
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(testList, null));
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(null, testList));

        // null entries should be discarded -> empty list -> return null
        testList.add(null);
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(testList, null));
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(null, testList));
        Assert.assertNull(MetadataListHelper.getMetadataSetIds(testList, testList));
    }

    // test that retrieving IDs of lists of Metadata objects works as expected: ignoring null, filtering duplicates - applies to both lists
    @Test
    public void testGetMetadataSetIds_Lists_Filtered() {

        final List<Metadata> testList1 = new ArrayList<Metadata>();

        final Metadata firstItem = new Metadata();
        firstItem.setId(4);
        testList1.add(firstItem);

        testList1.add(null);

        final Metadata secondItem = new Metadata();
        secondItem.setId(8);
        testList1.add(secondItem);

        final Metadata thirdItem = new Metadata();
        thirdItem.setId(4);
        testList1.add(thirdItem);

        testList1.add(null);

        final List<Metadata> testList2 = new ArrayList<Metadata>();
        testList2.add(null);

        final Metadata item1 = new Metadata();
        item1.setId(4);
        testList2.add(item1);

        final Metadata item2 = new Metadata();
        item2.setId(1);
        testList2.add(item2);

        // null entries should be discarded, duplicates be filtered -> 3 items remain (4, 8 from first list; 1 from second list)
        final List<Long> resultList = MetadataListHelper.getMetadataSetIds(testList1, testList2);
        Assert.assertEquals(3, resultList.size());
        Assert.assertTrue(resultList.contains(Long.valueOf(1)));
        Assert.assertTrue(resultList.contains(Long.valueOf(4)));
        Assert.assertTrue(resultList.contains(Long.valueOf(8)));
    }

    // test that retrieving IDs of list of Metadata objects always returns a list, even if empty
    @Test
    public void testGetNonNullMetadataSetIds() {

        Assert.assertNotNull(MetadataListHelper.getNonNullMetadataSetIds(null));

        // empty list should also return null
        final List<Metadata> testList = new ArrayList<Metadata>();
        Assert.assertNotNull(MetadataListHelper.getNonNullMetadataSetIds(testList));

        // null entries should be discarded -> empty list -> still not null
        testList.add(null);
        Assert.assertNotNull(MetadataListHelper.getNonNullMetadataSetIds(testList));

        // valid content -> still not null as result
        final Metadata meta = new Metadata();
        meta.setId(2);
        testList.add(meta);

        final List<Long> resultList = MetadataListHelper.getNonNullMetadataSetIds(testList);
        Assert.assertNotNull(resultList);
        Assert.assertEquals(1, resultList.size());
    }

}
