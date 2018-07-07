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
package eu.europa.ec.leos.web.support;

import java.util.Date;

public class LeosBuildInfo {
    // immutable values that will be replaced at project build time
    public static final String BUILD_TIMESTAMP_STR = "${leos.build.timestamp}";
    public static final String BUILD_ENVIRONMENT = "${leos.build.env}";
    public static final String BUILD_VERSION = "${leos.build.version}";
    public static final String SOURCE_STATUS = "${leos.source.status}";
    public static final String SOURCE_REVISION = "${leos.source.revision}";

    // immutable values that will be resolved at class load time
    public static final Long BUILD_TIMESTAMP = resolveTimestamp(BUILD_TIMESTAMP_STR);
    public static final Date BUILD_DATE = resolveDate(BUILD_TIMESTAMP);

    private static Long resolveTimestamp(String millis) {
        try {
            return Long.valueOf(millis);
        } catch (Exception ex) {
            throw new RuntimeException("Exception resolving timestamp! [millis=" + millis + "]", ex);
        }
    }

    private static Date resolveDate(Long millis) {
        try {
            return new Date(millis);
        } catch (Exception ex) {
            throw new RuntimeException("Exception resolving date! [millis=" + millis + "]", ex);
        }
    }
}
