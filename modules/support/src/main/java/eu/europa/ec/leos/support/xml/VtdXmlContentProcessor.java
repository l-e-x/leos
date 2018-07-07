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
package eu.europa.ec.leos.support.xml;

import com.google.common.base.Stopwatch;
import com.ximpleware.*;
import eu.europa.ec.leos.support.ByteArrayBuilder;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO.Type;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.support.xml.XmlHelper.*;

@Component
public class VtdXmlContentProcessor implements XmlContentProcessor {

    public static final String ARTICLE = "article";
    public static final String COMMENT = "popup";
    public static final String AUTHORIAL_NOTE = "authorialNote";
    public static final String MARKER_ATTRIBUTE = "marker";
    public static final String ID = "id";

    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final String BILL = "bill";
    private static final String BODY = "body";

    private static final String HEADING = "heading";
    private static final String NUM = "num";

    private static final byte[] NUM_BYTES = "num".getBytes();
    private static final byte[] HEADING_BYTES = "heading".getBytes();
    private static final byte[] NUM_START_TAG = "<num>".getBytes();
    private static final byte[] HEADING_START_TAG = "<heading>".getBytes();

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(VtdXmlContentProcessor.class);

    @Autowired
    @Qualifier("supportMessageSource")
    private MessageSource supportMessageSource;

    @Override
    public String getElementByNameAndId(byte[] xmlContent, String tagName, String idAttributeValue) {
        LOG.trace("Start extracting the tag {} with id {} from the document content", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        String element;
        try {
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(false);
            VTDNav vtdNav = vtdGen.getNav();
            element = getElementByNameAndId(tagName, idAttributeValue, vtdNav);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the search operation", e);
        }
        LOG.trace("Tag content extract completed in {} ms", (System.currentTimeMillis() - startTime));
        return element;
    }

    public byte[] renumberArticles(byte[] xmlContent, String language) {
        LOG.trace("Start renumberArticles ");
        byte[] element;
        try {
            Locale languageLocale = new Locale(language);
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(false);
            VTDNav vtdNav = vtdGen.getNav();
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(ARTICLE);
            long number = 1L;
            while (autoPilot.iterate()) {
                // get num + update
                byte[] articleNum = supportMessageSource.getMessage("legaltext.article.num", new Object[]{(number++)}, languageLocale).getBytes(UTF_8);

                int currentIndex = vtdNav.getCurrentIndex();
                if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                    byte[] numTag = getStartTag(vtdNav);
                    element = XmlHelper.buildTag(numTag, NUM_BYTES, articleNum);
                    xmlModifier.remove();
                } else {
                    // build num if not exists
                    element = XmlHelper.buildTag(NUM_START_TAG, NUM_BYTES, articleNum);
                }
                vtdNav.recoverNode(currentIndex);
                xmlModifier.insertAfterHead(element);
            }

            byte[] updatedContent = toByteArray(xmlModifier);
            updatedContent = doXMLPostProcessing(updatedContent);

            return updatedContent;
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the renumberArticles operation", e);
        }
    }

    public byte[] doXMLPostProcessing(byte[] xmlContent) {
        LOG.trace("Start doXMLPostProcessing ");
        try {
            long startTime = System.currentTimeMillis();
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);

            long startTimeIdInjects = System.currentTimeMillis();
            injectTagIdsinNode(vtdNav, xmlModifier, IdGenerator.DEFAULT_PREFIX);

            long endTimeIdInjects = System.currentTimeMillis();
            modifyAuthorialNoteMarkers(xmlModifier, vtdNav, 1);
            long endTimeIdAuthRenumber = System.currentTimeMillis();

            LOG.trace("Finished doXMLPostProcessing: TotalTime taken{}, Ids Injected in ={}, authNote Renumbering in={} ms",
                    (endTimeIdAuthRenumber - startTime), (endTimeIdInjects - startTimeIdInjects), (endTimeIdAuthRenumber - endTimeIdInjects));
            return toByteArray(xmlModifier);

        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the doXMLPostProcessing operation", e);
        }
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

        VTDGen vtdGen = new VTDGen();
        XMLModifier xmlModifier = new XMLModifier();
        vtdGen.setDoc(xmlContent);
        boolean found = false;
        try {
            vtdGen.parse(false);
            VTDNav vtdNav = vtdGen.getNav();
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

        } catch (ParseException | NavException | ModifyException | TranscodeException | IOException e) {
            throw new RuntimeException("Unable to perform the replace operation", e);
        }
    }

    @Override
    public byte[] appendElementToTag(byte[] xmlContent, String tagName, String newContent) {
        VTDGen vtdGen = new VTDGen();
        XMLModifier xmlModifier = new XMLModifier();
        vtdGen.setDoc(xmlContent);
        try {
            vtdGen.parse(false);

            VTDNav vtdNav = vtdGen.getNav();
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(tagName);
            while (autoPilot.iterate()) {

                vtdNav.toElement(VTDNav.LAST_CHILD);

                xmlModifier.bind(vtdNav);

                xmlModifier.insertAfterElement(newContent.getBytes(UTF_8));
                return toByteArray(xmlModifier);
            }
        } catch (ParseException | NavException | ModifyException | TranscodeException | IOException e) {
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
            int attIndex = vtdNav.getAttrVal(ID);
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

    private VTDNav setupVTDNav(byte[] xmlContent) throws NavException {
        VTDNav vtdNav = null;
        try {
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(false);
            vtdNav = vtdGen.getNav();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error occoured during setup of VTDNav", e);
        }
        return vtdNav;
    }

    private XMLModifier setupXMLModifier(VTDNav vtdNav, String tagName, String idAttributeValue) throws NavException {

        XMLModifier xmlModifier = null;
        try {
            xmlModifier = new XMLModifier();
            xmlModifier.bind(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);

            autoPilot.selectElement(tagName);
            while (autoPilot.iterate()) {
                int attIndex = vtdNav.getAttrVal(ID);
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

    private byte[] toByteArray(XMLModifier xmlModifier) throws ModifyException, TranscodeException, IOException {
        // get the updated XML content
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xmlModifier.output(baos);
        return baos.toByteArray();
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
    public byte[] insertElementByTagNameAndId(byte[] xmlContent, String articleTemplate, String tagName, String idAttributeValue, boolean before) {

        LOG.trace("Start inserting the tag {} having id {} before/after the selected element", tagName, idAttributeValue);
        long startTime = System.currentTimeMillis();
        byte[] updatedContent;

        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xm = setupXMLModifier(vtdNav, tagName, idAttributeValue);

            if (before) {
                xm.insertBeforeElement(articleTemplate.getBytes(UTF_8));
            } else {
                xm.insertAfterElement(articleTemplate.getBytes(UTF_8));
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
    public List<TableOfContentItemVO> buildTableOfContent(byte[] xmlContent) {
        LOG.trace("Start building the table of content");
        long startTime = System.currentTimeMillis();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {
            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(xmlContent);
            vtdGen.parse(false);
            VTDNav contentNavigator = vtdGen.getNav();

            if (contentNavigator.toElement(VTDNav.FIRST_CHILD, BILL)) {
                itemVOList = getAllChildTableOfContentItems(contentNavigator);
            }

        } catch (Exception e) {
            LOG.error("Unable to build the Table of content item list", e);
            throw new RuntimeException("Unable to build the Table of content item list", e);
        }

        LOG.trace("Build table of content completed in {} ms", (System.currentTimeMillis() - startTime));
        return itemVOList;
    }

    private List<TableOfContentItemVO> getAllChildTableOfContentItems(VTDNav contentNavigator) throws NavException {
        int currentIndex = contentNavigator.getCurrentIndex();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {

            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                addTocItemVoToList(contentNavigator, itemVOList);
                while (contentNavigator.toElement(VTDNav.NEXT_SIBLING)) {
                    addTocItemVoToList(contentNavigator, itemVOList);
                }
            }
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }

        return itemVOList;
    }

    private void addTocItemVoToList(VTDNav contentNavigator, List<TableOfContentItemVO> itemVOList) throws NavException {
        TableOfContentItemVO tableOfContentItemVO = buildTableOfContentsItemVO(contentNavigator);
        if (tableOfContentItemVO != null) {
            itemVOList.add(tableOfContentItemVO);
            tableOfContentItemVO.addAllChildItems(getAllChildTableOfContentItems(contentNavigator));
        }
    }

    private TableOfContentItemVO buildTableOfContentsItemVO(VTDNav contentNavigator) throws NavException {

        int originalNavigationIndex = contentNavigator.getCurrentIndex();

        // get the type
        String tagName = contentNavigator.toString(contentNavigator.getCurrentIndex());
        TableOfContentItemVO.Type type = TableOfContentItemVO.Type.forName(tagName);

        if (type == null) {
            // unsupported tag name
            return null;
        }

        // get the id
        int attIndex = contentNavigator.getAttrVal(ID);
        String elementId = null;
        if (attIndex != -1) {
            elementId = contentNavigator.toString(attIndex);
        }

        // ge the num
        String number = null;
        Integer numberTagIndex = null;
        if (contentNavigator.toElement(VTDNav.FIRST_CHILD, NUM)) {
            numberTagIndex = contentNavigator.getCurrentIndex();
            long contentFragment = contentNavigator.getContentFragment();
            number = getFragmentAsString(contentNavigator, contentFragment, true);
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
        return new TableOfContentItemVO(type, elementId, number, heading, numberTagIndex, headingTagIndex, contentNavigator.getCurrentIndex());

    }

    private String getFragmentAsString(VTDNav contentNavigator, long fragmentLocation, boolean removeTags) throws NavException {
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

    @Override
    public byte[] createDocumentContentWithNewTocList(List<TableOfContentItemVO> tableOfContentItemVOs, byte[] content) {
        LOG.trace("Start building the document content for the new toc list");
        long startTime = System.currentTimeMillis();
        try {

            ByteArrayBuilder mergedContent = new ByteArrayBuilder();

            VTDGen vtdGen = new VTDGen();
            vtdGen.setDoc(content);
            vtdGen.parse(false);
            VTDNav contentNavigator = vtdGen.getNav();

            int docLength = content.length;

            int endOfBillContent = 0;
            //fix 2082 // we are rebuilding only body tag of document
            if (navigateToElementByNameAndId(BODY, null, contentNavigator)) { //Body
                int index = contentNavigator.getCurrentIndex();

                // append everything up until the body tag
                long billContentFragment = contentNavigator.getElementFragment();
                int offset = (int) billContentFragment;
                int length = (int) (billContentFragment >> 32);
                mergedContent.append(contentNavigator.getXML().getBytes(0, offset));

                TableOfContentItemVO body = findContentVoForType(tableOfContentItemVOs, Type.BODY);
                if (body != null) {
                    mergedContent.append(buildTocItemContent(contentNavigator, body));
                }
                contentNavigator.recoverNode(index);
                //mergedContent.append(extractLevelNonTocItems(contentNavigator));

                // append everything after the bill content
                endOfBillContent = offset + length;
            }

            mergedContent.append(contentNavigator.getXML().getBytes(endOfBillContent, docLength - (endOfBillContent)));

            LOG.trace("Build the document content for the new toc list completed in {} ms", (System.currentTimeMillis() - startTime));
            return mergedContent.getContent();

        } catch (Exception e) {
            LOG.error("Unable to save the Table of content item list", e);
            throw new RuntimeException("Unable to save the Table of content item list", e);
        }

    }

    private TableOfContentItemVO findContentVoForType(List<TableOfContentItemVO> tableOfContentItemVOs, Type type) {
        ListIterator<TableOfContentItemVO> iterator = tableOfContentItemVOs.listIterator();
        while (iterator.hasNext()) {
            TableOfContentItemVO nextVO = iterator.next();
            if (nextVO.getType().equals(type)) {
                return nextVO;
            }
        }
        return null;
    }

    private byte[] buildTocItemContent(VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO) throws NavException {
        ByteArrayBuilder tocItemContent = new ByteArrayBuilder();

        tocItemContent.append(extractOrBuildNumElement(contentNavigator, tableOfContentItemVO));
        tocItemContent.append(extractOrBuildHeaderElement(contentNavigator, tableOfContentItemVO));

        for (TableOfContentItemVO child : tableOfContentItemVO.getChildItemsView()) {
            tocItemContent.append(buildTocItemContent(contentNavigator, child));
        }

        String tocTagName = tableOfContentItemVO.getType().name().toLowerCase();
        byte[] startTag;
        if (tableOfContentItemVO.getVtdIndex() != null) {
            contentNavigator.recoverNode(tableOfContentItemVO.getVtdIndex());

            startTag = getStartTag(contentNavigator);

            tocItemContent.append(extractLevelNonTocItems(contentNavigator));
        } else if (tableOfContentItemVO.getType().equals(Type.ARTICLE)) {
            return XmlHelper.getArticleTemplate(tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading()).getBytes(UTF_8);
        } else {
            String startTagStr = "<" + tocTagName + " id=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7) + "\">";
            startTag = startTagStr.getBytes(UTF_8);
        }

        return XmlHelper.buildTag(startTag, tocTagName.getBytes(), tocItemContent.getContent());
    }

    private byte[] extractLevelNonTocItems(VTDNav vtdNav) throws NavException {
        ByteArrayBuilder nonTocItems = new ByteArrayBuilder();
        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            nonTocItems.append(extractNonTocItemExceptNumAndHeading(vtdNav));
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                nonTocItems.append(extractNonTocItemExceptNumAndHeading(vtdNav));
            }
        }

        return nonTocItems.getContent();
    }

    private byte[] extractOrBuildNumElement(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException {
        byte[] element = new byte[0];
        if (parentTOC.getNumber() != null) {
            if (parentTOC.getNumTagIndex() != null) {
                contentNavigator.recoverNode(parentTOC.getNumTagIndex());
                byte[] numTag = getStartTag(contentNavigator);
                element = XmlHelper.buildTag(numTag, NUM_BYTES, parentTOC.getNumber().getBytes(UTF_8));
            } else {
                element = XmlHelper.buildTag(NUM_START_TAG, NUM_BYTES, parentTOC.getNumber().getBytes(UTF_8));
            }
        }
        return element;
    }

    private byte[] extractOrBuildHeaderElement(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException {
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

    private byte[] extractNonTocItemExceptNumAndHeading(VTDNav vtdNav) throws NavException {

        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        TableOfContentItemVO.Type type = TableOfContentItemVO.Type.forName(tagName);
        if (type == null && (!tagName.equals(NUM) && (!tagName.equals(HEADING)))) {
            return getTagWithContent(vtdNav);
        }

        return new byte[0];
    }

    private byte[] getTagWithContent(VTDNav vtdNav) throws NavException {
        long fragment = vtdNav.getElementFragment();
        return vtdNav.getXML().getBytes((int) fragment, (int) (fragment >> 32));
    }

    private byte[] getStartTag(VTDNav vtdNav) throws NavException {
        long token = (long) vtdNav.getElementFragment();
        int offsetContent = (int) vtdNav.getContentFragment();
        int offset = (int) token;
        int taglength = offsetContent != -1 ? (offsetContent - offset) : (int) (token >> 32);
        return vtdNav.getXML().getBytes(offset, taglength);
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

    //dfs
    private void injectTagIdsinNode(VTDNav vtdNav, XMLModifier xmlModifier, String idPrefix) {
        int currentIndex = vtdNav.getCurrentIndex();
        String idAttrValue = null;

        try {
            String tagName = vtdNav.toString(currentIndex);

            if (skipNodeAndChildren(tagName)) {//skipping node processing along with children
                return;
            }

            if (!skipNodeOnly(tagName)) {//do not update id for this tag
                idAttrValue = updateNodewithId(vtdNav, xmlModifier, idPrefix);
            }

            idPrefix = determinePrefixForChildren(tagName, idAttrValue, idPrefix);

            //update ids for children
            if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {//move to first child if there are children
                injectTagIdsinNode(vtdNav, xmlModifier, idPrefix);
                while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                    injectTagIdsinNode(vtdNav, xmlModifier, idPrefix);
                }//end while f
            }//end first child

            vtdNav.recoverNode(currentIndex);//get back to current node after processing nodes
        } catch (Exception e) {
            LOG.error("Consuming and continuing", e);
        }
    }

    private String updateNodewithId(VTDNav vtdNav, XMLModifier xmlModifier, String idPrefix) throws Exception {
        String idAttrValue = null;
        int idIndex = vtdNav.getAttrVal(ID);

        if (idIndex != -1) {//get Id of current Node. If there is no id in root..generate a new id attribute Node.
            idAttrValue = vtdNav.toString(idIndex);

            if (StringUtils.isBlank(idAttrValue)) {//if id is blank then update to generated one
                idAttrValue = IdGenerator.generateId(idPrefix, 7);
                xmlModifier.updateToken(idIndex, idAttrValue.getBytes(UTF_8));
            }
        } else {
            idAttrValue = IdGenerator.generateId(idPrefix, 7);
            String idAttributeNode = new StringBuilder(" ").append(ID).append("=\"").append(idAttrValue).append("\" ").toString();
            xmlModifier.insertAttribute(idAttributeNode.getBytes(UTF_8));
        }
        return idAttrValue;
    }

    //@Override
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
            }//End For
            // get the updated XML content
            updatedContent = toByteArray(xmlModifier);
        } catch (Exception e) {
            LOG.error("Unexpected error occoured during reference updates", e);
        }
        LOG.trace("References Updated in  {} ms", watch.elapsed(TimeUnit.MILLISECONDS));

        return updatedContent;
    }

    /** Finds the first element with the id,if there are others, XML is incorrect
     * @param xmlContent
     * @param idAttributeValue id Attribute valyue
     * @return complete Tag or null
     */
    @Override
    public String getElementById(byte[] xmlContent, String idAttributeValue) {
        Validate.isTrue(idAttributeValue != null, "Id can not be null");
        String element = null;
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            vtdNav.toElement(VTDNav.ROOT);
            element = getElementById(vtdNav, idAttributeValue);
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while finding element", e);
        }
        return element;
    }

    private String getElementById(VTDNav vtdNav, String idAttributeValue) {
        Validate.isTrue(idAttributeValue != null, "Id can not be null");

        Stopwatch watch = Stopwatch.createStarted();
        try {
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectXPath("//*[@id = '" + idAttributeValue + "']");
            if (ap.evalXPath() != -1) {
                return getFragmentAsString(vtdNav, vtdNav.getElementFragment(), false);
            } else {
                LOG.debug("Element with Id {} not found", idAttributeValue);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occoured finding element", e);
        }
        LOG.trace("Found tag in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return null;
    }

    @Override
    public byte[] insertCommentInElement(byte[] xmlContent, String idAttributeValue, String comment, boolean start) throws Exception {
        Validate.isTrue(idAttributeValue != null, "Id can not be null");

        Stopwatch watch = Stopwatch.createStarted();
        byte[] updatedContent;
        VTDNav vtdNav = setupVTDNav(xmlContent);
        vtdNav.toElement(VTDNav.ROOT);
        XMLModifier xmlModifier = new XMLModifier(vtdNav);

        String elementContent = getElementById(vtdNav, idAttributeValue);//find the elementd

        if (StringUtils.isNotEmpty(elementContent)) {
            if (start) {
                AutoPilot ap = new AutoPilot(vtdNav);
                ap.selectXPath("(popup[@refersTo='~leosSuggestion' or @refersto='~leosSuggestion'])[last()]");
                if (ap.evalXPath() != -1) {
                    xmlModifier.insertAfterElement(comment.toString().getBytes(UTF_8));
                } else {
                    xmlModifier.insertAfterHead(comment.toString().getBytes(UTF_8));
                }
            } else {
                xmlModifier.insertBeforeTail(comment.toString().getBytes(UTF_8));
            }
            updatedContent = toByteArray(xmlModifier);
            updatedContent = doXMLPostProcessing(updatedContent);
        } else {
            throw new RuntimeException("Element with Id:" + idAttributeValue + " not found");
        }
        LOG.trace("inserted tag in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return updatedContent;
    }

    @Autowired
    private XmlCommentProcessor xmlCommentProcessor;

    @Override
    public List<CommentVO> getAllComments(byte[] xmlContent) {

        Stopwatch watch = Stopwatch.createStarted();
        List<CommentVO> comments = new ArrayList<CommentVO>();
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            vtdNav.toElement(VTDNav.ROOT);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.selectElement(COMMENT);
            int skip = 0;
            while (autoPilot.iterate()) {
                if (skip > 0) {
                    skip--;
                    continue;
                }//dont do anything for skipped popups
                int currentIndex = vtdNav.getCurrentIndex();//index of Comment node  

                vtdNav.toElement(VTDNav.PARENT);
                String parentTag = new String(getTagWithContent(vtdNav), UTF_8);
                List<CommentVO> innerComments = xmlCommentProcessor.fromXML(parentTag);
                skip = (innerComments.size() > 1) ? (innerComments.size() - 1) : 0;//this is done, not to  process twice multiple comments in same tag

                comments.addAll(innerComments);
                vtdNav.recoverNode(currentIndex);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error", e);
        }
        LOG.trace("Found all Comments in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));
        return comments;
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
            ap.selectXPath("//*[@id = '" + idAttributeValue + "']");
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
                ap.selectAttr("id");
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
    public byte[] removeElements(byte[] xmlContent, String xpath) {
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
            }
            // get the updated XML content
            updatedContent = toByteArray(xmlModifier);
        } catch (Exception e) {
            LOG.error("Unexpected error occoured during removal of elements", e);
        }
        LOG.trace("Removed Elements in {} ms", watch.elapsed(TimeUnit.MILLISECONDS));

        return updatedContent;
    }

}

