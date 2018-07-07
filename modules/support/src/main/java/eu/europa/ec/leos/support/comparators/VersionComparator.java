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

import java.util.Comparator;
import java.util.StringTokenizer;

public class VersionComparator implements Comparator<String> {
    // use cases 1.9>1.10, 2.0>1.20, v10.10>v1.2 , v1.02>v1.20

    /* returns a negative integer first argument is less than r than the second
     * or zero, if equal
     * or a positive integer as the first argument greater than the second.
     */
    @Override
    public int compare(String version1, String version2) {
        if (version1 == null || version2 == null) {
            return (version1 == null)
                    ? ((version2 == null) ? 0 : -1)
                    : 1; // version1 greater
        }
        version1 = version1.replaceAll("[a-zA-Z]*", ""); //remove all Chars such as V etc
        version2 = version2.replaceAll("[a-zA-Z]*", "");

        StringTokenizer st1 = new StringTokenizer(version1, ".", false);
        StringTokenizer st2 = new StringTokenizer(version2, ".", false);

        while (st1.hasMoreTokens() && st2.hasMoreTokens()) {
            Integer token1 = Integer.parseInt(st1.nextToken());
            Integer token2 = Integer.parseInt(st2.nextToken());
            if (!token1.equals(token2)) return token1.compareTo(token2);
        }

        // a version left with extra token handled here
        if (st1.hasMoreTokens())
            return 1;
        else if (st2.hasMoreTokens())
            return -1;
        else
            return 0;// equal
    }
}