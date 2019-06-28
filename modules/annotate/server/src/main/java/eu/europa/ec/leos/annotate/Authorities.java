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
package eu.europa.ec.leos.annotate;

import org.springframework.util.StringUtils;

/**
 * class for hosting the known systems (aka. "authorities") for which specific logic exists
 */
public final class Authorities {

    // -------------------------------------
    // Public authority constants
    // -------------------------------------
    public static final String ISC = "ISC";
    public static final String EdiT = "LEOS";

    // -------------------------------------
    // Constructors
    // -------------------------------------
    private Authorities() {
        // prevent instantiation using a private constructor
    }

    // -------------------------------------
    // Help functions
    // -------------------------------------

    // check if a given authority represents the ISC
    public static boolean isIsc(final String authority) {

        return !StringUtils.isEmpty(authority) && ISC.equals(authority);
    }

    // check if a given authority represents the LEOS / EdiT
    public static boolean isLeos(final String authority) {

        return !StringUtils.isEmpty(authority) && EdiT.equals(authority);
    }
}
