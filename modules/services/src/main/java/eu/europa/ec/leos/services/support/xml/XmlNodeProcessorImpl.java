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

import static eu.europa.ec.leos.services.support.xml.VTDUtils.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.europa.ec.leos.services.support.xml.XmlNodeConfig.Attribute;

import com.google.common.base.Stopwatch;
import com.ximpleware.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringEscapeUtils;

@Component
class XmlNodeProcessorImpl implements XmlNodeProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(XmlNodeProcessorImpl.class);

    @Override
    public Map<String, String> getValuesFromXml(byte[] xmlContent, String[] keys, Map<String, XmlNodeConfig> config) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        Map<String, String> metaDataMap = new HashMap<>();

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent, true);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("leos", "urn:eu:europa:ec:leos");// required

            LOG.trace("Parsed xml in ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
            for (String key : keys) {
                try {
                    if (config.get(key) != null && findNode(vtdNav, autoPilot, config.get(key).xPath)) {
                        String value = StringEscapeUtils.unescapeXml(getValueFromNode(vtdNav));
                        metaDataMap.put(key, value);
                    }
                    // TODO : need to define what needs to be done for not found case. Do we send null or return nothing for key?
                } catch (XPathParseException | XPathEvalException | NavException ex) {
                    LOG.error("Error occurred while finding value for key:{}, continuing", key, ex);
                }
            }
        } catch (VTDException vEx) {
            LOG.error("Error occurred while parsing xml", vEx);
            throw new RuntimeException("VTD Parsing failed", vEx);
        }
        LOG.trace("{} Values retrieved from xml in ({} milliseconds)", metaDataMap.size(), stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return metaDataMap;
    }

    private boolean findNode(VTDNav vtdNav, AutoPilot autoPilot, String xPath) throws VTDException {
        autoPilot.resetXPath();
        autoPilot.selectXPath(xPath);
        return (autoPilot.evalXPath() != -1);// return.. found or not found
    }

    @Override
    public byte[] setValuesInXml(byte[] xmlContent, Map<String, String> keyValue, Map<String, XmlNodeConfig> config) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] modifiedXmlBytes = xmlContent;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent, true);
            vtdNav.toElement(VTDNav.ROOT);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("leos", "urn:eu:europa:ec:leos");// required

            for (Map.Entry<String, String> entry : keyValue.entrySet()) {
                try {
                    String key = entry.getKey();
                    String value = (entry.getValue() == null) ? "" : StringEscapeUtils.escapeXml10(entry.getValue()); //null means nothing in xml.So using ""

                    if (config.get(key) == null) {
                        LOG.error("Configuration not found for:{}, ignoring and continuing", key);
                        continue;
                    }
                    String xPath = config.get(key).xPath;


                    if (findNode(vtdNav, autoPilot, xPath)) {
                        updateNode(vtdNav, xmlModifier, value);
                    } else if (config.get(key).create) {
                        createAndUpdateNode(vtdNav, xmlModifier, autoPilot, xPath, config.get(key).attributes, value);
                        vtdNav = xmlModifier.outputAndReparse();
                        xmlModifier = new XMLModifier(vtdNav);
                        autoPilot = new AutoPilot(vtdNav);
                    }
                } catch (VTDException | IOException ex) {
                    LOG.error("Error occurred while updating value for:{}, continuing", entry, ex);
                }
            }
            modifiedXmlBytes = toByteArray(xmlModifier);
        } catch (VTDException | IOException vEx) {
            LOG.error("Error occurred while parsing xml", vEx);
            throw new RuntimeException("VTD Parsing failed", vEx);
        }
        LOG.trace("Values set in xml ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return modifiedXmlBytes;
    }

    private void updateNode(VTDNav vtdNav, XMLModifier xmlModifier, String value) throws VTDException, UnsupportedEncodingException {
        int index;
        int tokenType = vtdNav.getTokenType(vtdNav.getCurrentIndex());
        if (tokenType == VTDNav.TOKEN_STARTING_TAG) {
            index = vtdNav.getText();
        } else if (tokenType == VTDNav.TOKEN_ATTR_NAME) {
            index = vtdNav.getAttrVal(vtdNav.toString(vtdNav.getCurrentIndex()));
        } else {
            throw new RuntimeException("Xpath evaluated to incorrect token type :" + tokenType);
        }
        xmlModifier.updateToken(index, value.getBytes(UTF_8));
    }

    private void createAndUpdateNode(VTDNav vtdNav, XMLModifier xmlModifier, AutoPilot autoPilot, String xPath,
            List<XmlNodeConfig.Attribute> configAttributes, String value)
            throws VTDException {
        String[] nodes = xPath.split("(?<!/)(?=((/+)))");

        // 1. iterate and break at first non-existing node in xml
        StringBuilder partialXPath = new StringBuilder();
        int currentVtdIndex = 0, index = 0;
        for (; index < nodes.length; index++) {
            partialXPath.append(nodes[index]);
            if (!nodes[index].isEmpty() && !findNode(vtdNav, autoPilot, partialXPath.toString())) {// ignore nodes
                LOG.debug("Node not found:{}", nodes[index]);
                break;
            }
            currentVtdIndex = vtdNav.getCurrentIndex();
        }
        vtdNav.recoverNode(currentVtdIndex);// maintain vtd pointer where new nodes need to be injected

        // 2. create xml structure for remaining absent nodes
        Deque<String> stack = new ArrayDeque<>();
        for (; index < nodes.length; index++) {
            stack.push(nodes[index].replaceAll("//|/", "")); // Strip // and /
        }

        StringBuilder insertFragment = new StringBuilder();
        Set<Attribute> attributes = new HashSet<>();
        // 2.1 handle if final selection is attribute value
        if (stack.peek().startsWith("@")) {
            attributes.add(new Attribute(stack.pop().substring(1), value, parseForTagName(stack.peek())));
        } else {
            insertFragment.append(value);
        }

        // 2.2 create rest of structure
        while (stack.peek() != null) {
            String nextFragment = stack.pop();
            //check if there is attribute path in xpath
            String tagName = parseForTagName(nextFragment);
            Attribute fragmentAttribute = parseForAttribute(nextFragment);
            if (fragmentAttribute != null) {
                attributes.add(fragmentAttribute);
            }
            attributes.addAll(configAttributes.stream().filter(attr -> attr.parent.equals(tagName)).collect(Collectors.toSet()));
            wrapWithTag(insertFragment, tagName, attributes);
            attributes.clear();
        }

        // 3. Inject newly created structure in XML
        LOG.debug("Fragment to be inserted {}", insertFragment.toString());
        xmlModifier.insertBeforeTail(insertFragment.toString().getBytes(UTF_8));
    }

    private void wrapWithTag(StringBuilder existingFragment, String tag, Set<Attribute> attributes) {
        existingFragment
                .insert(0, new StringBuilder("<").append(tag).append(createAttributes(attributes)).append(">").toString())
                .append("</" + tag + ">");
    }

    private String createAttributes(Set<Attribute> attributes) {
        StringBuilder attributesBuilder = new StringBuilder("");
        if (attributes != null) {
            attributes.forEach(attribute -> {
                attributesBuilder.append(" ")
                        .append(attribute.name)
                        .append("=\"")
                        .append(attribute.value)
                        .append("\"");
            });
        }
        return attributesBuilder.toString();
    }

    /*
        parse xpathFragment between // or / for existance of attribute selector,
        we expect attribute selector wil be in format [@attName='attValue']
        returns null if there is no attribute
        returns attribute if there is attribute selector.
     */
    private Attribute parseForAttribute(String xPathFragment) {
        if (!xPathFragment.contains("[")) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?<tagName>[a-zA-Z]+?)\\[@(?<attName>.+?)='(?<attValue>.+?)'\\]");
        Matcher matcher = pattern.matcher(xPathFragment.trim());

        matcher.matches();
        String tagName = matcher.group("tagName");
        String attributeName = matcher.group("attName");
        String attributeValue = matcher.group("attValue");
        return new Attribute(attributeName, attributeValue, tagName);
    }

    private String parseForTagName(String xPathFragment) {
        return xPathFragment != null && xPathFragment.contains("[") ? xPathFragment.substring(0, xPathFragment.indexOf("[")) : xPathFragment;
    }

    private String getValueFromNode(VTDNav vtdNav) throws NavException {
        String value;
        int tokenType = vtdNav.getTokenType(vtdNav.getCurrentIndex());

        if (tokenType == VTDNav.TOKEN_STARTING_TAG) {
            value = getFragmentAsString(vtdNav, vtdNav.getElementFragment(), true);
        } else if (tokenType == VTDNav.TOKEN_ATTR_NAME) {
            int attrValIndex = vtdNav.getAttrVal(vtdNav.toString(vtdNav.getCurrentIndex()));
            value = vtdNav.toString(attrValIndex);
        } else {
            throw new RuntimeException("Xpath evaluated to incorrect token type :" + tokenType);
        }
        return value;
    }
}
