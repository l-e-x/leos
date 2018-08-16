/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.services.support.xml;

import java.util.Map;

public interface XmlNodeProcessor {
    /**
     * Sets the String values in the xml, at locations specified by respective Keys
     * Each key must have a Xpath configuration existing. If config not found, values are not updated
     * If configured Xpath is not found, it is created and then values is set.
     * (Key->XPATH from config-> value in XML.)
     * @param xmlContent original xml bytes
     * @param keyValue map of key and values
     * @param configuration TODO:: This config can be read from within class of can be sent with invocation. Needs to be decided while integrating.
     * @return updated XML bytes
     */
    byte[] setValuesInXml(byte[] xmlContent, Map<String,String> keyValue, Map<String, XmlNodeConfig> configuration);

    /**
     * This method returns the map of values found for the key for the configuration.
     * (Key->XPATH from config-> value from XML.)
     * @param xmlContent original xml bytes
     * @param keys of property to be read from Xml
     * @return map of keys with first value found in xml. if value if not found in xml, null is placed in map.
     */
    Map<String, String> getValuesFromXml(byte[] xmlContent, String[] keys, Map<String, XmlNodeConfig> configuration);
}
