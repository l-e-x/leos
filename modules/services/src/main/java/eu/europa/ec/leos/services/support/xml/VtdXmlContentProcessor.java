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
package eu.europa.ec.leos.services.support.xml;

import com.google.common.base.Stopwatch;
import com.ximpleware.AutoPilot;
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.TranscodeException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.services.support.IdGenerator;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.EMPTY_STRING;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_EDITABLE_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.NON_BREAKING_SPACE;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.WHITESPACE;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.XMLID;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getElementAttributes;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getFragmentAsString;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.navigateToElementByNameAndId;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupXMLModifier;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.toByteArray;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.AUTHORIAL_NOTE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEOS_REF_BROKEN_ATTR;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.MARKER_ATTRIBUTE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.MREF;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.REF;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.determinePrefixForChildren;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.isExcludedNode;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.isParentEditableNode;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.skipNodeAndChildren;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.skipNodeOnly;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.getAllChildTableOfContentItems;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public abstract class VtdXmlContentProcessor implements XmlContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VtdXmlContentProcessor.class);

    @Autowired
    ReferenceLabelProcessor referenceLabelProcessor;

    private enum EditableAttributeValue {
        TRUE, FALSE, UNDEFINED;
    }

    @Override
    public String getElementValue(byte[] xmlContent, String xPath, boolean namespaceEnabled) {
        String elementValue = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent, namespaceEnabled);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectXPath(xPath);
            if (autoPilot.evalXPath() != -1) {
                autoPilot.resetXPath();
                elementValue = autoPilot.evalXPathToString();
            }
        } catch (XPathParseException xPathParseException) {
            throw new RuntimeException("Exception occured while selecting XPath expression", xPathParseException);
        } catch (XPathEvalException xPathEvalException) {
            throw new RuntimeException("Exception occured while evaluating Xpath", xPathEvalException);
        } catch (NavException navException) {
            throw new RuntimeException("Exception occured during navigation", navException);
        }
        return elementValue;
    }

    @Override
    public String getElementByNameAndId(byte[] xmlContent, String tagName, String idAttributeValue) {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String element;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            element = getElementByNameAndId(tagName, idAttributeValue, vtdNav);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the search operation", e);
        }
        LOG.trace("Tag content extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return element;
    }

    @Override
    public String getParentElementId(byte[] xmlContent, String tagName, String idAttributeValue) {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String elementId = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            if (navigateToElementByNameAndId(tagName, idAttributeValue, vtdNav)) {
                elementId = getParentId(vtdNav);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the search operation", e);
        }
        LOG.trace("Tag content extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return elementId;
    }

    @Override
    public String[] getParentElement(byte[] xmlContent, String tagName, String idAttributeValue) {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String[] element = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            if (navigateToElementByNameAndId(tagName, idAttributeValue, vtdNav)) {
                element = getParentElement(vtdNav);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the search operation", e);
        }
        LOG.trace("Tag content extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return element;
    }

    @Override
    public String[] getSiblingElement(byte[] xmlContent, String tagName, String idAttributeValue, List<String> elementTags, boolean before) {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String[] element = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            if (navigateToElementByNameAndId(tagName, idAttributeValue, vtdNav)) {
                element = getSiblingElement(vtdNav, elementTags, before);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the search operation", e);
        }
        LOG.trace("Tag content extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return element;
    }

    private String[] getSiblingElement(VTDNav vtdNav, List<String> elementTags, boolean before) throws Exception {
        String[] element = null;
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(before ? VTDNav.PREV_SIBLING : VTDNav.NEXT_SIBLING)) {
                String elementTagName = null;
                do {
                    currentIndex = vtdNav.getCurrentIndex();
                    elementTagName = vtdNav.toString(currentIndex);
                    if (elementTags.contains(elementTagName) || elementTags.isEmpty()) {
                        String elementId = new String();
                        int attrXMLIDIndex = vtdNav.getAttrVal(XMLID);
                        if (attrXMLIDIndex != -1) {
                            elementId = vtdNav.toString(attrXMLIDIndex);
                        }
                        String elementFragment = getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
                        element = new String[]{elementId, elementTagName, elementFragment};
                    }
                }
                while ((element == null) && (vtdNav.toElement(before ? VTDNav.PREV_SIBLING : VTDNav.NEXT_SIBLING)));
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return element;
    }

    @Override
    public String[] getChildElement(byte[] xmlContent, String tagName, String idAttributeValue, List<String> elementTags, int position) {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String[] element = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            if (navigateToElementByNameAndId(tagName, idAttributeValue, vtdNav)) {
                element = getChildElement(vtdNav, elementTags, position);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the search operation", e);
        }
        LOG.trace("Tag content extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return element;
    }

    private String[] getChildElement(VTDNav vtdNav, List<String> elementTags, int position) throws Exception {
        String[] element = null;
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
                int childProcessed = 0;
                String elementTagName = null;
                do {
                    currentIndex = vtdNav.getCurrentIndex();
                    elementTagName = vtdNav.toString(currentIndex);
                    if (elementTags.contains(elementTagName) || elementTags.isEmpty()) {
                        childProcessed++;
                        if (childProcessed == position) {
                            String elementId = new String();
                            int attrXMLIDIndex = vtdNav.getAttrVal(XMLID);
                            if (attrXMLIDIndex != -1) {
                                elementId = vtdNav.toString(attrXMLIDIndex);
                            }
                            String elementFragment = getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
                            element = new String[]{elementId, elementTagName, elementFragment};
                        }
                    }
                }
                while ((childProcessed < position) && (vtdNav.toElement(VTDNav.NEXT_SIBLING)));
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return element;
    }

    @Override
    public List<Map<String, String>> getElementsAttributesByPath(byte[] xmlContent, String xPath) {
        if (xPath == null) {
            return null;
        }
        LOG.trace("Start extracting ids of the path {} from the document content", xPath);

        long startTime = System.currentTimeMillis();
        List<Map<String, String>> elts = new ArrayList<>();
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectXPath(xPath);
            while (ap.evalXPath() != -1) {
                elts.add(getElementAttributes(vtdNav, ap));
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occured while getting elements", e);
        }
        LOG.trace("Elements value extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return elts;
    }

    @Override
    public Map<String, String> getElementAttributesByPath(byte[] xmlContent, String xPath, boolean namespaceEnabled) {
        Map<String, String> attributes = new HashMap<>();
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent, namespaceEnabled);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            autoPilot.selectXPath(xPath);
            if (autoPilot.evalXPath() != -1) {
                autoPilot.resetXPath();
                autoPilot.selectAttr("*");
                int numAttribute;
                while ((numAttribute = autoPilot.iterateAttr()) != -1) {
                    attributes.put(vtdNav.toString(numAttribute), vtdNav.toString(numAttribute + 1));
                }
            }
        } catch (XPathParseException xPathParseException) {
            throw new RuntimeException("Exception occured while selecting XPath expression", xPathParseException);
        } catch (XPathEvalException xPathEvalException) {
            throw new RuntimeException("Exception occured while evaluating Xpath", xPathEvalException);
        } catch (NavException navException) {
            throw new RuntimeException("Exception occured during navigation", navException);
        }
        return attributes;
    }

    @Override
    public String getElementContentFragmentByPath(byte[] xmlContent, String xPath, boolean namespaceEnabled) {
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent, namespaceEnabled);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            autoPilot.selectXPath(xPath);
            if (autoPilot.evalXPath() != -1) {
                return getFragmentAsString(vtdNav, vtdNav.getContentFragment(), false);
            }
        } catch (XPathParseException xPathParseException) {
            throw new RuntimeException("Exception occured while selecting XPath expression", xPathParseException);
        } catch (XPathEvalException xPathEvalException) {
            throw new RuntimeException("Exception occured while evaluating Xpath", xPathEvalException);
        } catch (NavException navException) {
            throw new RuntimeException("Exception occured during navigation", navException);
        }
        return null;
    }

    @Override
    public String getElementFragmentByPath(byte[] xmlContent, String xPath, boolean namespaceEnabled) {
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent, namespaceEnabled);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            autoPilot.selectXPath(xPath);
            if (autoPilot.evalXPath() != -1) {
                return getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
            }
        } catch (XPathParseException xPathParseException) {
            throw new RuntimeException("Exception occured while selecting XPath expression", xPathParseException);
        } catch (XPathEvalException xPathEvalException) {
            throw new RuntimeException("Exception occured while evaluating Xpath", xPathEvalException);
        } catch (NavException navException) {
            throw new RuntimeException("Exception occured during navigation", navException);
        }
        return null;
    }

    @Override
    public byte[] setAttributeForAllChildren(byte[] xmlContent, String parentTag, List<String> elementTags, String attributeName, String value)
            throws Exception {
        byte[] updatedContent = setAttribute(xmlContent, parentTag, elementTags, attributeName, value);
        return doXMLPostProcessing(updatedContent);
    }

    @Override
    public byte[] doXMLPostProcessing(byte[] xmlContent) {
        LOG.trace("Start doXMLPostProcessing ");
        try {
            long startTime = System.currentTimeMillis();
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);

            // Inject Ids
            Stopwatch stopwatch = Stopwatch.createStarted();
            injectTagIdsinNode(vtdNav, xmlModifier, IdGenerator.DEFAULT_PREFIX);
            long injectIdTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            // modify Authnote markers
            modifyAuthorialNoteMarkers(xmlModifier, vtdNav, 1);
            long authNoteTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            // update refs
            updateMultiRefs(xmlModifier);
            long mrefUpdateTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            specificInstanceXMLPostProcessing(xmlModifier);

            LOG.trace("Finished doXMLPostProcessing: Ids Injected at ={}, authNote Renumbering at={} ms, mref udpated at={}ms, Total time elapsed = {}ms",
                    injectIdTime, authNoteTime, mrefUpdateTime, (System.currentTimeMillis() - startTime));
            return toByteArray(xmlModifier);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the doXMLPostProcessing operation", e);
        }
    }

    void updateMultiRefs(XMLModifier xmlModifier) throws Exception {
        VTDNav vtdNav = xmlModifier.outputAndReparse();
        xmlModifier.bind(vtdNav);
        vtdNav.toElement(VTDNav.ROOT);
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(MREF);
        int currentIndex;
        while (autoPilot.iterate()) {
            currentIndex = vtdNav.getCurrentIndex();
            try {
                List<Ref> refs = findReferences(vtdNav);
                String updatedMrefContent;
                if (!refs.isEmpty()) {
                    Result<String> labelResult = referenceLabelProcessor.generateLabel(refs, getParentId(vtdNav), vtdNav);
                    vtdNav.recoverNode(currentIndex);
                    if (labelResult.isOk()) {
                        updatedMrefContent = labelResult.get();
                        long token = vtdNav.getContentFragment();
                        int len = (int) (token >> 32);
                        xmlModifier.removeContent((int) token, len);
                        xmlModifier.insertBytesAt((int) token, updatedMrefContent.getBytes(VTDUtils.UTF_8));

                        AutoPilot ap = new AutoPilot(vtdNav);
                        ap.selectAttr(LEOS_REF_BROKEN_ATTR);
                        int indexAttr = ap.iterateAttr();
                        if (indexAttr != -1) {
                            xmlModifier.removeAttribute(indexAttr);
                        }
                    } else {
                        int brokenRefAttrIndex = vtdNav.getAttrVal(LEOS_REF_BROKEN_ATTR);
                        if (brokenRefAttrIndex == -1) {// if leos:broken attr is present.
                            xmlModifier.insertAttribute(new StringBuilder(" ").append(LEOS_REF_BROKEN_ATTR).append("=\"true\"").toString());
                        }
                    }
                }
            } catch (Exception ex) {
                String mrefContent = getFragmentAsString(vtdNav, vtdNav.getContentFragment(), false);
                LOG.error("mref can not be updated.Skipping..Mref Content: {},", mrefContent, ex);
            }
        }
    }

    private ImmutableTriple<String, Integer, Integer> getSubstringAvoidingTags(String text, int txtStartOffset, int txtEndOffset) {
        int xmlStartIndex = 0;
        int textCounter = 0;
        boolean stopCounting = false;
        for (char c : text.toCharArray()) {
            if (textCounter == txtStartOffset) {
                break;
            }
            if (c == '<') {
                stopCounting = true;
            } else if (c == '>') {
                stopCounting = false;
            } else if (!stopCounting) {
                textCounter++;
            }
            xmlStartIndex++;
        }
        text = text.substring(xmlStartIndex);

        int xmlEndIndex = xmlStartIndex;
        int textCounterI = txtStartOffset;
        stopCounting = false;
        for (char c : text.toCharArray()) {
            if (textCounterI == txtEndOffset) {
                break;
            }
            if (c == '<') {
                stopCounting = true;
            } else if (c == '>') {
                stopCounting = false;
            } else if (!stopCounting) {
                textCounterI++;
            }
            xmlEndIndex++;
        }
        String matchingText = text.substring(0, xmlEndIndex - xmlStartIndex);
        return new ImmutableTriple<String, Integer, Integer>(matchingText, xmlStartIndex, xmlEndIndex);
    }

    private String normalizeNewText(String origText, String newText) {
        return new StringBuilder(origText.startsWith(WHITESPACE) ? WHITESPACE : EMPTY_STRING)
                .append(StringUtils.normalizeSpace(newText))
                .append(origText.endsWith(WHITESPACE) ? WHITESPACE : EMPTY_STRING).toString();
    }

    @Override
    public byte[] replaceTextInElement(byte[] xmlContent, String origText, String newText, String elementId, int startOffset, int endOffset) {
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            xmlModifier.bind(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            autoPilot.selectXPath("//*[@xml:id = '" + elementId + "']");

            String elementContent;
            if (autoPilot.evalXPath() != -1) {
                elementContent = getFragmentAsString(vtdNav, vtdNav.getContentFragment(), false);
            } else {
                LOG.debug("Element with Id {} not found", elementId);
                return null;
            }
            StringBuilder eltContent = new StringBuilder(elementContent);
            ImmutableTriple<String, Integer, Integer> result = getSubstringAvoidingTags(elementContent, startOffset, endOffset);
            String matchingText = result.left;
            if (matchingText.replace(NON_BREAKING_SPACE, WHITESPACE).equals(StringEscapeUtils.escapeXml10(origText.replace(NON_BREAKING_SPACE, WHITESPACE)))) {
                eltContent.replace(result.middle, result.right, StringEscapeUtils.escapeXml10(normalizeNewText(origText, newText)));
                long token = vtdNav.getContentFragment();
                int len = (int) (token >> 32);
                xmlModifier.removeContent((int) token, len);
                xmlModifier.insertBytesAt((int) token, eltContent.toString().getBytes(VTDUtils.UTF_8));
            } else {
                LOG.debug("Text not matching {}, original text:{}, matched text:{}", elementId, origText, matchingText);
                return null;
            }
            return toByteArray(xmlModifier);
        } catch (Exception ex) {
            LOG.error("Text cannot be replaced", ex);
            return null;
        }
    }

    private List<Ref> findReferences(VTDNav vtdNav) throws Exception {
        List<Ref> refs = new ArrayList<>();
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, REF)) {
                refs.add(getRefElement(vtdNav));
                while (vtdNav.toElement(VTDNav.NEXT_SIBLING, REF)) {
                    refs.add(getRefElement(vtdNav));
                }
            } else {
                return refs;
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return refs;
    }

    String getParentId(VTDNav vtdNav) throws Exception {
        String id = new String();
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                int attrXMLIDIndex = vtdNav.getAttrVal(XMLID);
                if (attrXMLIDIndex != -1) {
                    id = vtdNav.toString(attrXMLIDIndex);
                }
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return id;
    }

    String[] getParentElement(VTDNav vtdNav) throws Exception {
        String[] element = null;
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                String elementTagName = vtdNav.toString(vtdNav.getCurrentIndex());
                String elementId = new String();
                int attrXMLIDIndex = vtdNav.getAttrVal(XMLID);
                if (attrXMLIDIndex != -1) {
                    elementId = vtdNav.toString(attrXMLIDIndex);
                }
                String elementFragment = getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
                element = new String[]{elementId, elementTagName, elementFragment};
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return element;
    }

    private Ref getRefElement(VTDNav vtdNav) throws Exception {
        String id = null, href = null;
        int index = vtdNav.getAttrVal(XMLID);
        if (index != -1) {
            id = vtdNav.toString(index);
        }
        index = vtdNav.getAttrVal("href");
        if (index != -1) {
            href = vtdNav.toString(index);
        }
        return new Ref(id, href);
    }

    private void modifyAuthorialNoteMarkers(XMLModifier xmlModifier, VTDNav vtdNav, int startMarker) throws Exception {

        vtdNav.toElement(VTDNav.ROOT);// this will reset the vtdNav to Root so that all the authorialNotes in Doc are considered.
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(AUTHORIAL_NOTE);
        int number = startMarker;

        while (autoPilot.iterate()) {
            int attIndex = vtdNav.getAttrVal(MARKER_ATTRIBUTE);
            if (attIndex != -1) {
                xmlModifier.updateToken(attIndex, Integer.toString(number).getBytes(UTF_8));
                number++;
            }
        }
    }

    private String getElementByNameAndId(String tagName, String idAttributeValue, VTDNav vtdNav) throws NavException {

        if (navigateToElementByNameAndId(tagName, idAttributeValue, vtdNav)) {
            long elementFragment = vtdNav.getElementFragment();
            return getFragmentAsString(vtdNav, elementFragment, false);
        }
        return null;
    }

    @Override
    public byte[] replaceElementsWithTagName(byte[] xmlContent, String tagName, String newContent) {

        XMLModifier xmlModifier = new XMLModifier();
        boolean found = false;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(tagName);

            while (autoPilot.iterate()) {
                xmlModifier.bind(vtdNav);
                xmlModifier.insertAfterElement(newContent.getBytes(UTF_8));
                xmlModifier.remove();
                found = true;
            }
            if (!found) {
                throw new IllegalArgumentException("No tag found with name " + tagName);
            }
            return toByteArray(xmlModifier);

        } catch (NavException | ModifyException | TranscodeException | IOException e) {
            throw new RuntimeException("Unable to perform the replace operation", e);
        }
    }

    @Override
    public byte[] appendElementToTag(byte[] xmlContent, String tagName, String newContent) {
        XMLModifier xmlModifier = new XMLModifier();
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(tagName);
            while (autoPilot.iterate()) {

                vtdNav.toElement(VTDNav.LAST_CHILD);

                xmlModifier.bind(vtdNav);

                xmlModifier.insertAfterElement(newContent.getBytes(UTF_8));
                return toByteArray(xmlModifier);
            }
        } catch (NavException | ModifyException | TranscodeException | IOException e) {
            throw new RuntimeException("Unable to perform the replace operation", e);
        }
        throw new IllegalArgumentException("No tag found with name " + tagName);
    }

    @Override
    public byte[] replaceElementByTagNameAndId(byte[] xmlContent, String newContent, String tagName, String idAttributeValue) {
        LOG.trace("Start updating the tag {} having id {} with the updated content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        byte[] updatedContent = null;

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = setupXMLModifier(vtdNav, tagName, idAttributeValue);
            if (xmlModifier != null) {
                xmlModifier.remove();

                if (newContent != null) {
                    xmlModifier.insertBeforeElement(newContent.getBytes(UTF_8));
                }

                updatedContent = toByteArray(xmlModifier);
                updatedContent = doXMLPostProcessing(updatedContent);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occoured during updation of element", e);
        }
        LOG.trace("Tag content replacement completed in {} ms", (System.currentTimeMillis() - startTime));

        return updatedContent;
    }

    @Override
    public byte[] deleteElementByTagNameAndId(byte[] xmlContent, String tagName, String idAttributeValue) {
        LOG.trace("Start deleting the tag {} having id {}", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        byte[] updatedContent;

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = setupXMLModifier(vtdNav, tagName, idAttributeValue);
            xmlModifier.remove();

            updatedContent = toByteArray(xmlModifier);

        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occoured during deletion of element", e);
        }
        LOG.trace("Tag content replacement completed in {} ms", (System.currentTimeMillis() - startTime));

        return updatedContent;
    }

    @Override
    public byte[] insertElementByTagNameAndId(byte[] xmlContent, String elementTemplate, String tagName, String idAttributeValue, boolean before) {
        LOG.trace("Start inserting the tag {} having id {} before/after the selected element", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        byte[] updatedContent;

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xm = setupXMLModifier(vtdNav, tagName, idAttributeValue);

            if (before) {
                xm.insertBeforeElement(elementTemplate.getBytes(UTF_8));
            } else {
                xm.insertAfterElement(elementTemplate.getBytes(UTF_8));
            }

            // get the updated XML content
            updatedContent = toByteArray(xm);

        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occoured during insert of element", e);
        }
        LOG.trace("Tag content insert completed in {} ms", (System.currentTimeMillis() - startTime));

        return updatedContent;
    }

    @Override
    public List<TableOfContentItemVO> buildTableOfContent(String startingNode, Function<String, TocItemType> getTocItemType, byte[] xmlContent,
            boolean simplified) {
        LOG.trace("Start building the table of content");
        long startTime = System.currentTimeMillis();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {
            VTDNav contentNavigator = setupVTDNav(xmlContent);

            if (contentNavigator.toElement(VTDNav.FIRST_CHILD, startingNode)) {
                itemVOList = getAllChildTableOfContentItems(getTocItemType, contentNavigator, simplified);
            }

        } catch (Exception e) {
            LOG.error("Unable to build the Table of content item list", e);
            throw new RuntimeException("Unable to build the Table of content item list", e);
        }

        LOG.trace("Build table of content completed in {} ms", (System.currentTimeMillis() - startTime));
        return itemVOList;
    }

    @Override
    public byte[] injectTagIdsinXML(byte[] xmlContent) {
        LOG.trace("Start generateTagIds ");
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);

            injectTagIdsinNode(vtdNav, xmlModifier, IdGenerator.DEFAULT_PREFIX);

            return toByteArray(xmlModifier);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the generateTagIds operation", e);
        }
    }

    // LEOS-2639: replace XML self-closing tags not supported in HTML
    private String removeSelfClosingElements(String fragment) {
        String removeSelfClosingRegex = "<([^>^\\s]+)([^>]*)/>";
        return fragment.replaceAll(removeSelfClosingRegex, "<$1$2></$1>");
    }

    // dfs
    private void injectTagIdsinNode(VTDNav vtdNav, XMLModifier xmlModifier, String idPrefix) {
        int currentIndex = vtdNav.getCurrentIndex();
        String idAttrValue = null;

        try {
            String tagName = vtdNav.toString(currentIndex);

            if (skipNodeAndChildren(tagName)) {// skipping node processing along with children
                return;
            }

            if (!skipNodeOnly(tagName)) {// do not update id for this tag
                idAttrValue = updateNodewithId(vtdNav, xmlModifier, idPrefix);
            }

            idPrefix = determinePrefixForChildren(tagName, idAttrValue, idPrefix);

            // update ids for children
            if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {// move to first child if there are children
                injectTagIdsinNode(vtdNav, xmlModifier, idPrefix);
                while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                    injectTagIdsinNode(vtdNav, xmlModifier, idPrefix);
                } // end while f
            } // end first child

            vtdNav.recoverNode(currentIndex);// get back to current node after processing nodes
        } catch (Exception e) {
            LOG.error("Consuming and continuing", e);
        }
    }

    private String updateNodewithId(VTDNav vtdNav, XMLModifier xmlModifier, String idPrefix) throws Exception {
        String idAttrValue = null;
        int idIndex = vtdNav.getAttrVal(XMLID);

        if (idIndex != -1) {// get Id of current Node. If there is no id in root..generate a new id attribute Node.
            idAttrValue = vtdNav.toString(idIndex);

            if (StringUtils.isBlank(idAttrValue)) {// if id is blank then update to generated one
                idAttrValue = IdGenerator.generateId(idPrefix, 7);
                xmlModifier.updateToken(idIndex, idAttrValue.getBytes(UTF_8));
            }
        } else {
            idAttrValue = IdGenerator.generateId(idPrefix, 7);
            String idAttributeNode = new StringBuilder(" ").append(XMLID).append("=\"").append(idAttrValue).append("\" ").toString();
            xmlModifier.insertAttribute(idAttributeNode.getBytes(UTF_8));
        }
        return idAttrValue;
    }

    @Override
    public byte[] searchAndReplaceText(byte[] xmlContent, String searchText, String replaceText) {
        byte[] updatedContent = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);

            boolean found = processSearchAndReplace(vtdNav, xmlModifier, searchText, replaceText);
            if (found) {
                updatedContent = toByteArray(xmlModifier);
            } else {
                // TODO: do something...return proper value instead of null.
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the replaceContent operation", e);
        }
        return updatedContent;
    }

    private boolean processSearchAndReplace(VTDNav vtdNav, XMLModifier xmlModifier, String searchText, String replaceText) {
        boolean found = false;
        try {
            AutoPilot ap = new AutoPilot(vtdNav);
            String xPath = String.format("//*[contains(lower-case(text()), %s)]", wrapXPathWithQuotes(searchText.toLowerCase()));
            ap.selectXPath(xPath);
            while (ap.evalXPath() != -1) {
                int p = vtdNav.getText();
                if (p != -1 && isEditableElement(vtdNav)) {
                    String updatedContent = vtdNav.toNormalizedString(p).replaceAll("(?i)" + Pattern.quote(searchText), Matcher.quoteReplacement(replaceText));
                    xmlModifier.updateToken(p, StringEscapeUtils.escapeXml10(updatedContent).getBytes(UTF_8));
                    found = true;
                }
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occoured during replaceText operation", e);
            throw new RuntimeException("Unable to perform the replaceText operation", e);
        }
        return found;
    }

    private boolean isEditableElement(VTDNav vtdNav) throws NavException {
        EditableAttributeValue editableAttrVal = getEditableAttributeForNode(vtdNav);
        while (EditableAttributeValue.UNDEFINED.equals(editableAttrVal) && vtdNav.toElement(VTDNav.PARENT)) {
            editableAttrVal = getEditableAttributeForNode(vtdNav);
        }
        return Boolean.parseBoolean(editableAttrVal.name());
    }

    private EditableAttributeValue getEditableAttributeForNode(VTDNav vtdNav) throws NavException {
        AutoPilot ap = new AutoPilot(vtdNav);
        Map<String, String> attrs = getElementAttributes(vtdNav, ap);
        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        String attrVal = attrs.get(LEOS_EDITABLE_ATTR);

        if (isExcludedNode(tagName)) {
            return EditableAttributeValue.FALSE; // editable = false;
        } else if (attrVal != null) {
            return attrVal.equalsIgnoreCase("true") ? EditableAttributeValue.TRUE : EditableAttributeValue.FALSE;
        } else if (isParentEditableNode(tagName)) {
            return EditableAttributeValue.FALSE; // editable = false;
        } else {
            return EditableAttributeValue.UNDEFINED; // editable not present;
        }
    }

    private String wrapXPathWithQuotes(String value) {
        String wrappedValue = value;

        String apostrophe = "'";
        String quote = "\"";

        if (value.contains(quote)) {
            wrappedValue = apostrophe + value + apostrophe;
        } else {
            wrappedValue = quote + value + quote;
        }
        return wrappedValue;
    }

    @Override
    public byte[] updateReferedAttributes(byte[] xmlContent, Map<String, String> referenceValueMap) {
        byte[] updatedContent = xmlContent;
        Stopwatch watch = Stopwatch.createStarted();

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot ap = new AutoPilot(vtdNav);

            for (String reference : referenceValueMap.keySet()) {
                try {
                    ap.resetXPath();
                    ap.selectXPath("//*[@refersTo='~" + reference + "']");
                    while (ap.evalXPath() != -1) {
                        int p = vtdNav.getText();
                        xmlModifier.updateToken(p, referenceValueMap.get(reference).getBytes(UTF_8));
                    }
                } catch (Exception e) {
                    LOG.error("Unexpected error occoured during reference update:{}. Consuming and continuing", reference, e);
                }
            } // End For
              // get the updated XML content
            updatedContent = toByteArray(xmlModifier);
        } catch (Exception e) {
            LOG.error("Unexpected error occoured during reference updates", e);
        }
        LOG.trace("References Updated in  {} ms", watch.elapsed(TimeUnit.MILLISECONDS));

        return updatedContent;
    }

    @Override
    public String[] getElementById(byte[] xmlContent, String idAttributeValue) {
        Validate.isTrue(idAttributeValue != null, "Id can not be null");
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            vtdNav.toElement(VTDNav.ROOT);
            return getElementById(vtdNav, idAttributeValue);
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while finding element in getElementFragmentById", e);
        }
        return null;
    }

    private String[] getElementById(VTDNav vtdNav, String idAttributeValue) {
        Validate.isTrue(idAttributeValue != null, "Id can not be null");
        String[] element = new String[3];
        Stopwatch watch = Stopwatch.createStarted();
        try {
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            ap.selectXPath("//*[@xml:id = '" + idAttributeValue + "']");
            if (ap.evalXPath() != -1) {
                element[0] = idAttributeValue;
                element[1] = vtdNav.toString(vtdNav.getCurrentIndex());
                element[2] = getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
            } else {
                LOG.debug("Element with Id {} not found", idAttributeValue);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occoured finding element in getElementFragmentById", e);
        }
        LOG.trace("Found tag in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return element;
    }

    @Override
    public List<String> getAncestorsIdsForElementId(byte[] xmlContent,
            String idAttributeValue) {
        Validate.isTrue(idAttributeValue != null, "Id can not be null");
        Stopwatch watch = Stopwatch.createStarted();
        LinkedList<String> ancestorsIds = new LinkedList<String>();
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            ap.selectXPath("//*[@xml:id = '" + idAttributeValue + "']");
            if (ap.evalXPath() == -1) {
                String errorMsg = String.format(
                        "Element with id: %s does not exists.",
                        idAttributeValue);
                LOG.error(errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }
            /* Skip current element */
            if (!vtdNav.toElement(VTDNav.PARENT)) {
                return ancestorsIds;
            }
            do {
                ap.selectAttr("xml:id");
                int i = -1;
                String idValue = "";
                if ((i = ap.iterateAttr()) != -1) {
                    // Get the value of the 'id' attribute
                    idValue = vtdNav.toRawString(i + 1);
                    ancestorsIds.addFirst(idValue);
                }
            }
            while (vtdNav.toElement(VTDNav.PARENT));

        } catch (VTDException e) {
            String errorMsg = String
                    .format("Not able to retrieve ancestors ids for given element id: %s",
                            idAttributeValue);
            LOG.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
        LOG.trace("Retrieved ancestors ids in {} ms",
                watch.elapsed(TimeUnit.MILLISECONDS));
        return ancestorsIds;
    }

    @Override
    public byte[] removeElements(byte[] xmlContent, String xpath, int levelsToRemove) {
        byte[] updatedContent = xmlContent;
        Stopwatch watch = Stopwatch.createStarted();

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectXPath(xpath);
            while (ap.evalXPath() != -1) {
                xmlModifier.remove();

                for (int level = 0; level < levelsToRemove; level++) {
                    boolean moved = vtdNav.toElement(VTDNav.PARENT);
                    if (moved && vtdNav.getRootIndex() != vtdNav.getCurrentIndex()) {// Must not remove root node.
                        LOG.trace("Removing parent:{}", vtdNav.toString(vtdNav.getCurrentIndex()));
                        xmlModifier.remove();
                    } else {
                        LOG.warn("Parent does't exist in Xml or reached at root, at node:{}, level :{}", vtdNav.toString(vtdNav.getCurrentIndex()), level);
                        break;
                    }
                }
                vtdNav = xmlModifier.outputAndReparse();
                xmlModifier = new XMLModifier(vtdNav);
                ap = new AutoPilot(vtdNav);
                ap.selectXPath(xpath);
            }

            // get the updated XML content
            updatedContent = toByteArray(xmlModifier);
        } catch (Exception e) {
            LOG.error("Unexpected error occoured during removal of elements", e);
        }
        LOG.trace("Removed Elements in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));

        return updatedContent;
    }

    @Override
    public byte[] removeElements(byte[] xmlContent, String xpath) {
        return removeElements(xmlContent, xpath, 0);
    }

    @Override
    public String doImportedElementPreProcessing(String xmlContent, String elementType) {
        LOG.trace("Start import article preprocessing ");
        try {
            Stopwatch watch = Stopwatch.createStarted();
            VTDNav vtdNav = setupVTDNav(StringUtils.normalizeSpace(xmlContent).getBytes(UTF_8));
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            String editableAttributes = new StringBuilder(" ").append("leos:editable=\"true\"  leos:deletable=\"true\"").toString();
            xmlModifier.insertAttribute(editableAttributes.getBytes(UTF_8));
            int idIndex = vtdNav.getAttrVal(XMLID);
            String idAttrValue = vtdNav.toString(idIndex);
            String idPrefix = new StringBuilder("").append("imp_").append(idAttrValue).toString();
            String newIdAttrValue = IdGenerator.generateId(idPrefix, 7);
            xmlModifier.updateToken(idIndex, newIdAttrValue.getBytes(UTF_8));
            LOG.trace("Finished doImportedElementPreProcessing: TotalTime taken{}", watch.elapsed(TimeUnit.MILLISECONDS));
            String updatedElement = removeSelfClosingElements(new String(toByteArray(xmlModifier), UTF_8));
            return updatedElement;
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the doImportedElementPreProcessing operation", e);
        }
    }

    @Override
    public String getElementIdByPath(byte[] xmlContent, String xPath) {
        String elementId = null;
        Stopwatch watch = Stopwatch.createStarted();
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectXPath(xPath);
            if (autoPilot.evalXPath() != -1) {
                elementId = vtdNav.toString(vtdNav.getAttrVal(XMLID));
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occoured while finding the element id by path", e);
        }
        LOG.trace("Found last element id in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return elementId;
    }
}
