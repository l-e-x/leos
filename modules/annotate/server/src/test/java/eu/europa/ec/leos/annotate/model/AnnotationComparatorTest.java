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

import eu.europa.ec.leos.annotate.model.entity.Annotation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
/**
 * Class testing correct sorting of our custom comparator using random data
 */
public class AnnotationComparatorTest {

    private List<Annotation> annotationList;

    // -------------------------------------
    // Test setup: generate random test data
    // -------------------------------------
    @Before
    public void generateTestData() throws ParseException {

        // fill list with random test data
        annotationList = new ArrayList<Annotation>();

        Annotation ann;
        for (int i = 0; i < 100; i++) {
            ann = new Annotation();
            ann.setCreated(getRandomDateTime());
            ann.setUpdated(getRandomDateTime());

            final LocalDateTime dummyDate = getRandomDateTime();
            ann.setShared(dummyDate.getSecond() % 2 == 0);

            annotationList.add(ann);
        }
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    @Test
    public void testSortCreatedAscending() throws ParseException {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.ASC, "created")));

        for (int i = 0; i < annotationList.size() - 1; i++) {
            assertDatesAscending(annotationList.get(i).getCreated(), annotationList.get(i + 1).getCreated());
        }
    }

    @Test
    public void testSortCreatedDescending() throws ParseException {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.DESC, "created")));

        for (int i = 0; i < annotationList.size() - 1; i++) {
            assertDatesDescending(annotationList.get(i).getCreated(), annotationList.get(i + 1).getCreated());
        }
    }

    @Test
    public void testSortUpdatedAscending() throws ParseException {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.ASC, "updated")));

        for (int i = 0; i < annotationList.size() - 1; i++) {
            assertDatesAscending(annotationList.get(i).getUpdated(), annotationList.get(i + 1).getUpdated());
        }
    }

    @Test
    public void testSortUpdatedDescending() throws ParseException {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.DESC, "updated")));

        for (int i = 0; i < annotationList.size() - 1; i++) {
            assertDatesDescending(annotationList.get(i).getUpdated(), annotationList.get(i + 1).getUpdated());
        }
    }

    @Test
    public void testSortSharedAscending() throws ParseException {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.ASC, "shared")));

        boolean nowAllShared = annotationList.get(0).isShared();
        for (int i = 0; i < annotationList.size() - 1; i++) {

            // we have to find the entry before which all items are unshared and after which all items are shared
            if (nowAllShared) {
                Assert.assertTrue(annotationList.get(i).isShared());
            } else {
                Assert.assertFalse(annotationList.get(i).isShared());
                if (annotationList.get(i + 1).isShared()) {
                    nowAllShared = true;
                }
            }
        }
    }

    @Test
    public void testSortSharedDescending() throws ParseException {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.DESC, "shared")));

        boolean nowAllShared = annotationList.get(0).isShared();
        for (int i = 0; i < annotationList.size() - 1; i++) {

            // we have to find the entry before which all items are shared and after which all items are unshared
            if (nowAllShared) {
                Assert.assertTrue(annotationList.get(i).isShared());
                if (!annotationList.get(i + 1).isShared()) {
                    nowAllShared = false;
                }
            } else {
                Assert.assertFalse(annotationList.get(i).isShared());
            }
        }
    }

    // test that our custom comparator does not throw an error when being told to sort for undefined column
    @Test
    public void testNoErrorForInvalidColumn() {

        annotationList.sort(new AnnotationComparator(new Sort(Direction.ASC, "somecolumn")));
        // no error should occur
    }

    // -------------------------------------
    // Help functions
    // -------------------------------------
    private LocalDateTime getRandomDateTime() throws ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH);
        final long offset = sdf.parse("2010-01-01T00:00:00Z").getTime();
        final long end = sdf.parse("2013-01-01T00:00:00Z").getTime();
        final long diff = end - offset + 1;
        final Date randomDate = new Date(offset + (long) (Math.random() * diff));
        return LocalDateTime.ofInstant(randomDate.toInstant(), ZoneId.systemDefault());
    }

    // helper function for uncomfortable Java Date comparison
    private void assertDatesAscending(final LocalDateTime older, final LocalDateTime newer) {
        Assert.assertTrue(older.isBefore(newer));
    }

    // helper function for uncomfortable Java Date comparison
    private void assertDatesDescending(final LocalDateTime older, final LocalDateTime newer) {
        Assert.assertTrue(older.isAfter(newer));
    }
}
