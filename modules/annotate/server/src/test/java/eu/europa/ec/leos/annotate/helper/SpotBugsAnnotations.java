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
package eu.europa.ec.leos.annotate.helper;

public class SpotBugsAnnotations {

    public static final String FieldNotInitialized = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR";
    public static final String FieldNotInitializedReason = "initialised in before-test setup function called by junit";

    public static final String ExceptionIgnored = "DE_MIGHT_IGNORE";
    public static final String ExceptionIgnoredReason = "Intended for test";

    public static final String KnownNullValue = "NP_LOAD_OF_KNOWN_NULL_VALUE";
    public static final String KnownNullValueReason = "by intention for test purposes";

    public static final String ReturnValueIgnored = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT";
    public static final String ReturnValueIgnoredReason = "Calls verified for test purposes";

}
