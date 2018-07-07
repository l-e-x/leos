/*
 * Copyright 2017 European Commission
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import com.ximpleware.*;

class VTDUtils {

    static final String GUID = "GUID";
    static final Charset UTF_8 = Charset.forName("UTF-8");

    static VTDNav setupVTDNav(byte[] xmlContent) throws NavException {
        return setupVTDNav(xmlContent, false);
    }

    static VTDNav setupVTDNav(byte[] xmlContent, boolean namespaceEnabled) throws NavException {
        VTDNav vtdNav = null;
        try {
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(namespaceEnabled);
            vtdNav = vtdGen.getNav();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occoured during setup of VTDNav", e);
        }
        return vtdNav;
    }

    static XMLModifier setupXMLModifier(VTDNav vtdNav, String tagName, String idAttributeValue) throws NavException {

        XMLModifier xmlModifier = null;
        try {
            xmlModifier = new XMLModifier();
            xmlModifier.bind(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);

            autoPilot.selectElement(tagName);
            while (autoPilot.iterate()) {
                int attIndex = vtdNav.getAttrVal(GUID);
                String elementId;
                if (attIndex != -1) {
                    elementId = vtdNav.toString(attIndex);
                    if (idAttributeValue.equals(elementId)) {
                        return xmlModifier;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occoured during setup of XML Modifier", e);
        }
        return xmlModifier;
    }

    static byte[] toByteArray(XMLModifier xmlModifier) throws ModifyException, TranscodeException, IOException {
        // get the updated XML content
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xmlModifier.output(baos);
        return baos.toByteArray();
    }

    static String getFragmentAsString(VTDNav contentNavigator, long fragmentLocation, boolean removeTags) throws NavException {
        String fragmentContent = null;
        if (fragmentLocation > -1) {
            int offSet = (int) fragmentLocation;
            int length = (int) (fragmentLocation >> 32);
            byte[] elementContent = contentNavigator.getXML().getBytes(offSet, length);
            fragmentContent = new String(elementContent, UTF_8);

            if (removeTags) {
                // remove all tags and replace multiple space occurrences with a single space
                fragmentContent = fragmentContent.replaceAll("<[^>]+>", "");
                fragmentContent = fragmentContent.replaceAll("\\s+", " ").trim();
            }
        }
        return fragmentContent;
    }
}
