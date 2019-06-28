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

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.Group;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
public class GroupComparatorTest {

    /**
     * Class testing correct sorting of our custom group comparator
     */

    // check that equally visible groups are ordered by display name
    @Test
    public void testOrderByDisplayname() {

        // order should be switched after sorting
        final Group grp1 = new Group("grp1", "bdisplayname", "desc1", true);
        final Group grp2 = new Group("grp2", "adisplayname", "desc2", true);

        // act
        final List<Group> groups = Arrays.asList(grp1, grp2);
        groups.sort(new GroupComparator(TestDbHelper.DEFAULT_GROUP_INTERNALNAME));

        // verify
        Assert.assertEquals(grp2, groups.get(0));
        Assert.assertEquals(grp1, groups.get(1));
    }

    // check ordering of group visibility
    @Test
    public void testOrderByWorldPublicPrivate() {

        // order should be switched after sorting
        final Group grp1 = new Group("grp1", "b displayname", "descPriv", false);
        final Group grp2 = new Group("grp2", "a displayname", "descPub", true);
        final Group grp3 = new Group(TestDbHelper.DEFAULT_GROUP_INTERNALNAME, "default group", "descDef", true);

        // act
        final List<Group> groups = Arrays.asList(grp1, grp2, grp3);
        groups.sort(new GroupComparator(TestDbHelper.DEFAULT_GROUP_INTERNALNAME));

        // verify
        Assert.assertEquals(grp3, groups.get(0));
        Assert.assertEquals(grp2, groups.get(1));
        Assert.assertEquals(grp1, groups.get(2));
    }

    // check total ordering
    @Test
    public void testEntireOrdering() {

        // insert in completely opposite order
        final Group grp1 = new Group("grp1", "bdisplayname", "descPrivB", false);
        final Group grp2 = new Group("grp2", "adisplayname", "descPrivA", false);
        final Group grp3 = new Group("grp3", "bdisplayname", "descPubB", true);
        final Group grp4 = new Group("grp4", "adisplayname", "descPubA", true);
        final Group grp5 = new Group(TestDbHelper.DEFAULT_GROUP_INTERNALNAME, "default group", "descDef", true);

        // act
        final List<Group> groups = Arrays.asList(grp1, grp2, grp3, grp4, grp5);
        groups.sort(new GroupComparator(TestDbHelper.DEFAULT_GROUP_INTERNALNAME));

        // verify
        Assert.assertEquals(grp5, groups.get(0));
        Assert.assertEquals(grp4, groups.get(1));
        Assert.assertEquals(grp3, groups.get(2));
        Assert.assertEquals(grp2, groups.get(3));
        Assert.assertEquals(grp1, groups.get(4));
    }
}
