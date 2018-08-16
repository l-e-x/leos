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

import com.google.common.base.Stopwatch;
import com.ximpleware.*;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.support.IdGenerator;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.AnnexTocItemType;
import eu.europa.ec.leos.vo.toctype.LegalTextTocItemType;
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
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.*;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class VtdXmlContentProcessor implements XmlContentProcessor {

    public static final String BODY = "body";
    public static final String RECITAL = "recital";
    public static final String ARTICLE = "article";
    public static final String AUTHORIAL_NOTE = "authorialNote";
    public static final String MREF = "mref";
    public static final String REF = "ref";
    public static final String META = "meta";
    public static final String MARKER_ATTRIBUTE = "marker";

    private static final String HEADING = "heading";
    private static final String NUM = "num";

    private static final byte[] NUM_BYTES = "num".getBytes();
    private static final byte[] HEADING_BYTES = "heading".getBytes();
    private static final byte[] NUM_START_TAG = "<num>".getBytes();
    private static final byte[] HEADING_START_TAG = "<heading>".getBytes();

    private static final String LEOS_EDITABLE_ATTR = "leos:editable";
    private static final String LEOS_REF_BROKEN_ATTR = "leos:broken";

    private static final Logger LOG = LoggerFactory.getLogger(VtdXmlContentProcessor.class);

    @Autowired
    private ReferenceLabelProcessor referenceLabelProcessor;

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
    public String getParentElementId(byte[] xmlContent, String tagName, String idAttributeValue)
    {
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
    public String getParentElement(byte[] xmlContent, String tagName, String idAttributeValue)
    {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String element = null;
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

    private Map<String, String> getElementAttributes(VTDNav vtdNav, AutoPilot ap) {
        Map<String, String> attrs = new HashMap<String, String>();
        int i = -1;
        try {
            ap.selectAttr("*");
            while ((i = ap.iterateAttr()) != -1) {
                attrs.put(vtdNav.toString(i), vtdNav.toString(i + 1));
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occured while getting elements", e);
        }
        return attrs;
    }

    public byte[] setOriginAttribute(byte[] xmlContent, String parentTag) throws Exception {
        LOG.trace("Start setOriginAttribute");
        VTDNav vtdNav = setupVTDNav(xmlContent);
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(parentTag);
        XMLModifier xmlModifier = new XMLModifier(vtdNav);
        while (autoPilot.iterate()) {
            int currentIndex = vtdNav.getCurrentIndex();
            if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
                setOriginAttribute(vtdNav, xmlModifier);
                while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                    setOriginAttribute(vtdNav, xmlModifier);
                }
            }
            vtdNav.recoverNode(currentIndex);
        }
        byte[] updatedContent = toByteArray(xmlModifier);
        updatedContent = doXMLPostProcessing(updatedContent);

        return updatedContent;
    }

    private void setOriginAttribute(VTDNav vtdNav, XMLModifier xmlModifier) throws Exception {
        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        if (tagName.equals(META)) {
            return;
        }

        int currentIndex = vtdNav.getCurrentIndex();
        int attIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
        if (attIndex == -1) {
            xmlModifier.insertAttribute(new StringBuilder(" ").append(LEOS_ORIGIN_ATTR).append("=\"ec\"").toString());
        }
        else {
            xmlModifier.updateToken(attIndex, "ec");
        }

        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            setOriginAttribute(vtdNav, xmlModifier);
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                setOriginAttribute(vtdNav, xmlModifier);
            }
        }
        vtdNav.recoverNode(currentIndex);
    }

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
            long authNoteTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);;

            // update refs
            updateMultiRefs(xmlModifier);
            long mrefUpdateTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);

            LOG.trace("Finished doXMLPostProcessing: Ids Injected at ={}, authNote Renumbering at={} ms, mref udpated at={}ms",
                    injectIdTime, authNoteTime, mrefUpdateTime);
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
                    Result<String> labelResult = referenceLabelProcessor.generateLabel(refs, getParentId(vtdNav), vtdNav, "en");
                    vtdNav.recoverNode(currentIndex);
                    if (labelResult.isOk()) {
                        updatedMrefContent = labelResult.get();
                        long token = vtdNav.getContentFragment();
                        int len = (int) (token >> 32);
                        xmlModifier.removeContent((int) token, len);
                        xmlModifier.insertBytesAt((int) token, updatedMrefContent.getBytes(VTDUtils.UTF_8));

                        AutoPilot ap = new AutoPilot(vtdNav);
                        ap.selectAttr("broken");
                        int indexAttr = ap.iterateAttr();
                        if (indexAttr != -1) {
                            xmlModifier.removeAttribute(indexAttr);
                        }
                    } else {
                        int brokenRefAttrIndex = vtdNav.getAttrVal("broken");
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

    @Override
    public byte[] replaceTextInElement(byte[] xmlContent, String origText, String newText, String elementId, int startOffset, int endOffset) {
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            xmlModifier.bind(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectXPath("//*[@GUID = '" + elementId + "']");

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
            if (matchingText.equals(origText)) {
                eltContent.replace(result.middle, result.right, newText);
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

    private String getParentId(VTDNav vtdNav) throws Exception {
        String id = new String();
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                int attrGUIDIndex = vtdNav.getAttrVal(GUID);
                if (attrGUIDIndex != -1) {
                    id = vtdNav.toString(attrGUIDIndex);
                }
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return id;
    }

    private String getParentElement(VTDNav vtdNav) throws Exception {
        String element = null;
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                
                long elementFragment = vtdNav.getElementFragment();
                element= getFragmentAsString(vtdNav, elementFragment, false);
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return element;
    }

    private Ref getRefElement(VTDNav vtdNav) throws Exception {
        String id = null, href = null;
        int index = vtdNav.getAttrVal(GUID);
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

    private boolean navigateToElementByNameAndId(String tagName, String idAttributeValue, VTDNav vtdNav) throws NavException {
        AutoPilot autoPilot = new AutoPilot(vtdNav);

        autoPilot.selectElement(tagName);
        while (autoPilot.iterate()) {
            if (idAttributeValue == null) {
                return true;
            }
            int attIndex = vtdNav.getAttrVal(GUID);
            String elementId;
            if (attIndex != -1) {
                elementId = vtdNav.toString(attIndex);
                if (idAttributeValue.equals(elementId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public byte[] replaceElementByTagNameAndId(byte[] xmlContent, String newContent, String tagName, String idAttributeValue) {
        LOG.trace("Start updating the tag {} having id {} with the updated content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        byte[] updatedContent;

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = setupXMLModifier(vtdNav, tagName, idAttributeValue);
            xmlModifier.remove();

            if (newContent != null) {
                xmlModifier.insertBeforeElement(newContent.getBytes(UTF_8));
            }

            updatedContent = toByteArray(xmlModifier);
            updatedContent = doXMLPostProcessing(updatedContent);
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
    public List<TableOfContentItemVO> buildTableOfContent(String startingNode, Function<String, TocItemType> getTocItemType, byte[] xmlContent) {
        LOG.trace("Start building the table of content");
        long startTime = System.currentTimeMillis();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {
            VTDNav contentNavigator = setupVTDNav(xmlContent);

            if (contentNavigator.toElement(VTDNav.FIRST_CHILD, startingNode)) {
                itemVOList = getAllChildTableOfContentItems(getTocItemType, contentNavigator);
            }

        } catch (Exception e) {
            LOG.error("Unable to build the Table of content item list", e);
            throw new RuntimeException("Unable to build the Table of content item list", e);
        }

        LOG.trace("Build table of content completed in {} ms", (System.currentTimeMillis() - startTime));
        return itemVOList;
    }

    private List<TableOfContentItemVO> getAllChildTableOfContentItems(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator)
            throws NavException {
        int currentIndex = contentNavigator.getCurrentIndex();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {

            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                addTocItemVoToList(getTocItemType, contentNavigator, itemVOList);
                while (contentNavigator.toElement(VTDNav.NEXT_SIBLING)) {
                    addTocItemVoToList(getTocItemType, contentNavigator, itemVOList);
                }
            }
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }

        return itemVOList;
    }

    private void addTocItemVoToList(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, List<TableOfContentItemVO> itemVOList)
            throws NavException {
        TableOfContentItemVO tableOfContentItemVO = buildTableOfContentsItemVO(getTocItemType, contentNavigator);
        if (tableOfContentItemVO != null) {
            if (tableOfContentItemVO.getType().isToBeDisplayed()) {
                itemVOList.add(tableOfContentItemVO);
                tableOfContentItemVO.addAllChildItems(getAllChildTableOfContentItems(getTocItemType, contentNavigator));
            } else {
                if (tableOfContentItemVO.getParentItem() != null) {
                    tableOfContentItemVO.getParentItem().addAllChildItems(getAllChildTableOfContentItems(getTocItemType, contentNavigator));
                } else {
                    getAllChildTableOfContentItems(getTocItemType, contentNavigator).forEach(childItem -> itemVOList.add(childItem));
                }
            }
        }
    }

    private TableOfContentItemVO buildTableOfContentsItemVO(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator) throws NavException {

        int originalNavigationIndex = contentNavigator.getCurrentIndex();

        // get the type
        String tagName = contentNavigator.toString(contentNavigator.getCurrentIndex());
        TocItemType type = getTocItemType.apply(tagName);

        if (type == null) {
            // unsupported tag name
            return null;
        }

        // get the id
        int attIndex = contentNavigator.getAttrVal(GUID);
        String elementId = null;
        if (attIndex != -1) {
            elementId = contentNavigator.toString(attIndex);
        }

        //get the leos:origin attr
        int originAttrIndex = contentNavigator.getAttrVal(LEOS_ORIGIN_ATTR);
        String originAttr = originAttrIndex != -1 ? contentNavigator.toString(originAttrIndex) : null;
        
        // get the num
        String number = null;
        Integer numberTagIndex = null;
        String originNumAttr = null;
        if (contentNavigator.toElement(VTDNav.FIRST_CHILD, NUM)) {
            numberTagIndex = contentNavigator.getCurrentIndex();
            //get the leos:origin attr
            int originNumAttrIndex = contentNavigator.getAttrVal(LEOS_ORIGIN_ATTR);
            originNumAttr = originNumAttrIndex != -1 ? contentNavigator.toString(originNumAttrIndex) : null;
            
            long contentFragment = contentNavigator.getContentFragment();
            number = extractNumber(getFragmentAsString(contentNavigator, contentFragment, true));
            contentNavigator.recoverNode(originalNavigationIndex);
        }

        // get the heading
        String heading = null;
        Integer headingTagIndex = null;
        if (contentNavigator.toElement(VTDNav.FIRST_CHILD, HEADING)) {
            headingTagIndex = contentNavigator.getCurrentIndex();
            long contentFragment = contentNavigator.getContentFragment();
            heading = getFragmentAsString(contentNavigator, contentFragment, true);
            contentNavigator.recoverNode(originalNavigationIndex);
        }

        // build the table of content item and return it
        return new TableOfContentItemVO(type, elementId, originAttr, number, originNumAttr, heading, numberTagIndex, headingTagIndex, contentNavigator.getCurrentIndex());
    }

    @Override
    public byte[] createDocumentContentWithNewTocList(Function<String, TocItemType> getTocItemType, TocItemType type,
            List<TableOfContentItemVO> tableOfContentItemVOs, byte[] content) {
        LOG.trace("Start building the document content for the new toc list");
        long startTime = System.currentTimeMillis();
        try {

            ByteArrayBuilder mergedContent = new ByteArrayBuilder();
            VTDNav contentNavigator = setupVTDNav(content);
            int docLength = content.length;
            int endOfContent = 0;
            // LEOS-2082 we are rebuilding only body tag of document
            if (navigateToElementByNameAndId(type.getName(), null, contentNavigator)) {
                int index = contentNavigator.getCurrentIndex();

                // append everything up until the body tag
                long contentFragment = contentNavigator.getElementFragment();
                int offset = (int) contentFragment;
                int length = (int) (contentFragment >> 32);
                mergedContent.append(contentNavigator.getXML().getBytes(0, offset));

                TableOfContentItemVO body = findContentVoForType(tableOfContentItemVOs, type);
                if (body != null) {
                    mergedContent.append(buildTocItemContent(getTocItemType, contentNavigator, body));
                }
                contentNavigator.recoverNode(index);

                // append everything after the content
                endOfContent = offset + length;
            }

            mergedContent.append(contentNavigator.getXML().getBytes(endOfContent, docLength - (endOfContent)));

            LOG.trace("Build the document content for the new toc list completed in {} ms", (System.currentTimeMillis() - startTime));
            return mergedContent.getContent();

        } catch (Exception e) {
            LOG.error("Unable to save the Table of content item list", e);
            throw new RuntimeException("Unable to save the Table of content item list", e);
        }

    }

    private TableOfContentItemVO findContentVoForType(List<TableOfContentItemVO> tableOfContentItemVOs, TocItemType type) {
        ListIterator<TableOfContentItemVO> iterator = tableOfContentItemVOs.listIterator();
        while (iterator.hasNext()) {
            TableOfContentItemVO nextVO = iterator.next();
            if (nextVO.getType().equals(type)) {
                return nextVO;
            }
        }
        return null;
    }

    private byte[] buildTocItemContent(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO)
            throws NavException, UnsupportedEncodingException {
        ByteArrayBuilder tocItemContent = new ByteArrayBuilder();

        tocItemContent.append(extractOrBuildNumElement(contentNavigator, tableOfContentItemVO));
        tocItemContent.append(extractOrBuildHeaderElement(contentNavigator, tableOfContentItemVO));

        for (TableOfContentItemVO child : tableOfContentItemVO.getChildItemsView()) {
            tocItemContent.append(buildTocItemContent(getTocItemType, contentNavigator, child));
        }

        byte[] startTag = new byte[0];
        String tocTagName = tableOfContentItemVO.getType().getName();

        if (tableOfContentItemVO.getVtdIndex() != null) {
            contentNavigator.recoverNode(tableOfContentItemVO.getVtdIndex());
            startTag = updateOriginAttribute(getStartTag(contentNavigator), tableOfContentItemVO.getOriginAttr());
            tocItemContent.append(extractLevelNonTocItems(getTocItemType, contentNavigator));
        } else if (tableOfContentItemVO.getType().equals(LegalTextTocItemType.ARTICLE)) {
            byte[] articleTag = XmlHelper.getArticleTemplate(tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading()).getBytes(UTF_8);
            return updateOriginAttribute(articleTag, tableOfContentItemVO.getOriginAttr());
        } else if (tableOfContentItemVO.getType().equals(AnnexTocItemType.DIVISION)) { // FIXME: Find better way to handle insertion of template
            byte[] annexTag = XmlHelper.getAnnexTemplate().getBytes(UTF_8);
            return updateOriginAttribute(annexTag, tableOfContentItemVO.getOriginAttr());
        } else {
            String startTagStr = "<" + tocTagName + " GUID=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7) + "\">";
            startTag = updateOriginAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.getOriginAttr());
        }
        return XmlHelper.buildTag(startTag, tocTagName.getBytes(UTF_8), tocItemContent.getContent());
    }

    private byte[] extractLevelNonTocItems(Function<String, TocItemType> getTocItemType, VTDNav vtdNav) throws NavException {
        ByteArrayBuilder nonTocItems = new ByteArrayBuilder();
        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            nonTocItems.append(extractNonTocItemExceptNumAndHeading(getTocItemType, vtdNav));
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                nonTocItems.append(extractNonTocItemExceptNumAndHeading(getTocItemType, vtdNav));
            }
        }

        return nonTocItems.getContent();
    }

    private byte[] extractOrBuildNumElement(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException, UnsupportedEncodingException {
        byte[] element = new byte[0];
        if (parentTOC.getNumber() != null) {
            StringBuilder item = new StringBuilder(StringUtils.capitalize(parentTOC.getType().getName()));
            byte[] numBytes = item.append(" ").append(extractNumber(parentTOC.getNumber())).toString().getBytes(UTF_8);

            if (parentTOC.getNumTagIndex() != null) {
                contentNavigator.recoverNode(parentTOC.getNumTagIndex());
                byte[] numTag = getStartTag(contentNavigator);
                numTag = updateOriginAttribute(numTag, parentTOC.getOriginNumAttr());
                element = XmlHelper.buildTag(numTag, NUM_BYTES, numBytes);
            } else {
                byte[] numStartTag = updateOriginAttribute(NUM_START_TAG, parentTOC.getOriginNumAttr());
                element = XmlHelper.buildTag(numStartTag, NUM_BYTES, numBytes);
            }
        }
        return element;
    }

    private byte[] extractOrBuildHeaderElement(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException, UnsupportedEncodingException {
        byte[] element = new byte[0];
        if (parentTOC.getHeading() != null) {
            if (parentTOC.getHeadingTagIndex() != null) {
                contentNavigator.recoverNode(parentTOC.getHeadingTagIndex());
                byte[] headingTag = getStartTag(contentNavigator);
                element = XmlHelper.buildTag(headingTag, HEADING_BYTES, parentTOC.getHeading().getBytes(UTF_8));
            } else {
                element = XmlHelper.buildTag(HEADING_START_TAG, HEADING_BYTES, parentTOC.getHeading().getBytes(UTF_8));
            }
        }
        return element;
    }

    private byte[] extractNonTocItemExceptNumAndHeading(Function<String, TocItemType> getTocItemType, VTDNav vtdNav) throws NavException {

        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        TocItemType type = getTocItemType.apply(tagName);
        if (type == null && (!tagName.equals(NUM) && (!tagName.equals(HEADING)))) {
            return getTagWithContent(vtdNav);
        }

        return new byte[0];
    }

    private byte[] getTagWithContent(VTDNav vtdNav) throws NavException {
        long fragment = vtdNav.getElementFragment();
        return vtdNav.getXML().getBytes((int) fragment, (int) (fragment >> 32));
    }

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
        int idIndex = vtdNav.getAttrVal(GUID);

        if (idIndex != -1) {// get Id of current Node. If there is no id in root..generate a new id attribute Node.
            idAttrValue = vtdNav.toString(idIndex);

            if (StringUtils.isBlank(idAttrValue)) {// if id is blank then update to generated one
                idAttrValue = IdGenerator.generateId(idPrefix, 7);
                xmlModifier.updateToken(idIndex, idAttrValue.getBytes(UTF_8));
            }
        } else {
            idAttrValue = IdGenerator.generateId(idPrefix, 7);
            String idAttributeNode = new StringBuilder(" ").append(GUID).append("=\"").append(idAttrValue).append("\" ").toString();
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

    // @Override
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

    /**Finds the first element with the id,if there are others, XML is incorrect
     * @param xmlContent
     * @param idAttributeValue id Attribute valyue
     * @return complete Tag or null
     */
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
        String[] element = new String[2];
        Stopwatch watch = Stopwatch.createStarted();
        try {
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectXPath("//*[@GUID = '" + idAttributeValue + "']");
            if (ap.evalXPath() != -1) {
                element[0] = vtdNav.toString(vtdNav.getCurrentIndex());
                element[1] = getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
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
            ap.selectXPath("//*[@GUID = '" + idAttributeValue + "']");
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
                ap.selectAttr("GUID");
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
            VTDNav vtdNav = setupVTDNav(xmlContent.getBytes(UTF_8));
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            if(elementType.equalsIgnoreCase(ARTICLE)) { 
               String editableAttributes = new StringBuilder(" ").append("leos:editable=\"true\"  leos:deletable=\"true\"").toString();
               xmlModifier.insertAttribute(editableAttributes.getBytes(UTF_8));
            }
            int idIndex = vtdNav.getAttrVal(GUID);
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
                elementId = vtdNav.toString(vtdNav.getAttrVal(GUID));
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occoured while finding the element id by path", e);
        }
        LOG.trace("Found last element id in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return elementId;
    }
}
