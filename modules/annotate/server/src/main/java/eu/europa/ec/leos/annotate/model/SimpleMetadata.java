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

import java.util.HashMap;

/**
 * This class is just a wrapper for a hashmap, making code easier to read.
 * It is mainly used for denoting a map of simple metadata values
 */
public class SimpleMetadata extends HashMap<String, String>{

    private static final long serialVersionUID = 5497040203360830277L;

    public SimpleMetadata() {
        super();
    }

    // copy constructor
    public SimpleMetadata(final SimpleMetadata otherMap) {
        super(otherMap);
    }

    // constructor that directly adds an item
    public SimpleMetadata(final String key, final String value) {
        super();
        put(key, value);
    }

}
