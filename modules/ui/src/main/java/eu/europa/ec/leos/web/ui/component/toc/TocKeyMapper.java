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
package eu.europa.ec.leos.web.ui.component.toc;

import com.vaadin.server.KeyMapper;

import java.util.HashMap;

public class TocKeyMapper<V> extends KeyMapper<V> {
    private int lastKey = 0;

    private final HashMap<V, String> objectKeyMap = new HashMap<V, String>();

    private final HashMap<String, V> keyObjectMap = new HashMap<String, V>();

    /**
     * Gets key for an object.
     *
     * @param o the object.
     */
    public String key(V o) {

        if (o == null) {
            return "null";
        }

        // If the object is already mapped, use existing key
        String key = objectKeyMap.get(o);
        if (key != null) {
            return key;
        }

        // If the object is not yet mapped, map it
        key = String.valueOf(o);
        objectKeyMap.put(o, key);
        keyObjectMap.put(key, o);

        return key;
    }

    /**
     * Retrieves object with the key.
     *
     * @param key the name with the desired value.
     * @return the object with the key.
     */
    public V get(String key) {
        return keyObjectMap.get(key);
    }

    /**
     * Removes object from the mapper.
     *
     * @param removeobj the object to be removed.
     */
    public void remove(V removeobj) {
        final String key = objectKeyMap.get(removeobj);

        if (key != null) {
            objectKeyMap.remove(removeobj);
            keyObjectMap.remove(key);
        }
    }

    /**
     * Removes all objects from the mapper.
     */
    public void removeAll() {
        objectKeyMap.clear();
        keyObjectMap.clear();
    }
}
