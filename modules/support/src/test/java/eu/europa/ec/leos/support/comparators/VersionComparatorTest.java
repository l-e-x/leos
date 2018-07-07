/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.support.comparators;

import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class VersionComparatorTest extends LeosTest {

    @Test
    public void test_Compare_v1_less_than_v2() {
        //setup
        String version1="1.1";
        String version2="1.10";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, lessThan(0));
    }

    @Test
    public void test_Compare_v1_less_than_v2_with_String() {
        //setup
        String version1="v1.1";
        String version2="v1.10";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, lessThan(0));
    }
    @Test
    public void test_Compare_v1_greater_than_v2() {
        //setup
        String version1="v1.10";
        String version2="v1.1";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, greaterThan(0));
    }

    @Test
    public void test_Compare_v1_greater_than_v2_digitChange() {
        //setup
        String version1="1.10";
        String version2="1.1";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, greaterThan(0));
    }

    @Test
    public void test_Compare_v1_less_than_v2_digit_change() {
        //setup
        String version1="v1.2";
        String version2="v1.100";//100 is higher version

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, lessThan(0));
    }
    @Test
    public void test_Compare_v1_greater_than_v2_diff_digits() {
        //setup
        String version1="v2";
        String version2="v1.100";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, greaterThan(0));
    }
    @Test
    public void test_Compare_v1_greater_than_v2_diff_digits1() {
        //setup
        String version1="v2.0";
        String version2="v1.0";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, greaterThan(0));
    }
    @Test
    public void test_Compare_v1_greater_than_v2_diff_digits2() {
        //setup
        String version1="v2.0";
        String version2="v1";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, greaterThan(0));
    }

    @Test
    public void test_Compare_v1_is_blank() {
        //setup
        String version1="";
        String version2="v2";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, lessThan(0));
    }
    @Test
    public void test_Compare_v2_is_blank() {
        //setup
        String version1="v1";//higher version
        String version2="";

        //do the actual call
        int result = new VersionComparator().compare(version1,version2);

        //check the result
        assertThat(result, greaterThan(0));
    }
}
