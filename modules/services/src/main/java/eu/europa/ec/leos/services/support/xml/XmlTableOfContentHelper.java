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

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.vo.toc.OptionsType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.vo.toc.TocItemUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.HEADING_BYTES;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.HEADING_START_TAG;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.NUM_BYTES;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.NUM_START_TAG;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.XMLID;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.extractNumber;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getFragmentAsString;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getNumSoftActionAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getOriginAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getSoftActionAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getSoftActionRootAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getSoftDateAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getSoftMovedFromAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getSoftMovedToAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getSoftUserAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.navigateToElementByNameAndId;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateOriginAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateXMLIDAttributesInElementContent;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CN;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.EC;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.HEADING;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.INDENT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.INTRO;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEVEL;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEVEL_NUM_SEPARATOR;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LIST;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.NUM;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.POINT;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class XmlTableOfContentHelper {

    private static final Logger LOG = LoggerFactory.getLogger(XmlTableOfContentHelper.class);
    
    @Autowired
    protected Provider<StructureContext> structureContextProvider;
    
    public static List<TableOfContentItemVO> getAllChildTableOfContentItems(List<TocItem> tocItems, Map<TocItem, List<TocItem>> tocRules, VTDNav contentNavigator, TocMode mode)
            throws NavException {
        int currentIndex = contentNavigator.getCurrentIndex();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {
            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                addTocItemVoToList(tocItems, tocRules, contentNavigator, itemVOList, mode);
                while (contentNavigator.toElement(VTDNav.NEXT_SIBLING)) {
                    addTocItemVoToList(tocItems, tocRules, contentNavigator, itemVOList, mode);
                }
            }
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }

        return itemVOList;
    }

    static boolean navigateToFirstTocElment(List<TableOfContentItemVO> tableOfContentItemVOs, VTDNav vtdNav) throws NavException {
        TableOfContentItemVO firstTocVO = tableOfContentItemVOs.listIterator().next();
        return navigateToElementByNameAndId(firstTocVO.getTocItem().getAknTag().value(), null, vtdNav);
    }

    private static void addTocItemVoToList(List<TocItem> tocItems, Map<TocItem, List<TocItem>> tocRules, VTDNav contentNavigator, List<TableOfContentItemVO> itemVOList, TocMode mode)
            throws NavException {
        TableOfContentItemVO tableOfContentItemVO = buildTableOfContentsItemVO(tocItems, contentNavigator);
        if (tableOfContentItemVO != null) {
            List<TableOfContentItemVO> itemVOChildrenList = getAllChildTableOfContentItems(tocItems, tocRules, contentNavigator, mode);
            if ((!TocMode.SIMPLIFIED_CLEAN.equals(mode) || (TocMode.SIMPLIFIED_CLEAN.equals(mode) && tableOfContentItemVO.getTocItem().isDisplay()))
                    && shouldItemBeAddedToToc(tocItems, tocRules, contentNavigator, tableOfContentItemVO.getTocItem())) {
                if (TocMode.SIMPLIFIED.equals(mode) || TocMode.SIMPLIFIED_CLEAN.equals(mode)) {
                    if (getTagValueFromTocItemVo(tableOfContentItemVO).equals(LIST) && !itemVOList.isEmpty()) {
                        tableOfContentItemVO = itemVOList.get(itemVOList.size() - 1);
                        tableOfContentItemVO.addAllChildItems(itemVOChildrenList);
                        return;
                    } else if (Arrays.asList(PARAGRAPH, POINT, INDENT, LEVEL).contains(getTagValueFromTocItemVo(tableOfContentItemVO))) {
                        if ((itemVOChildrenList.size() > 1) && (itemVOChildrenList.get(0).getChildItems().isEmpty())) {
                            tableOfContentItemVO.setId(itemVOChildrenList.get(0).getId());
                            itemVOChildrenList.remove(0);
                        } else if (itemVOChildrenList.size() == 1) {
                            tableOfContentItemVO.setId(itemVOChildrenList.get(0).getId());
                            itemVOChildrenList = itemVOChildrenList.get(0).getChildItems();
                        }
                    }
                }
                itemVOList.add(tableOfContentItemVO);
                tableOfContentItemVO.addAllChildItems(itemVOChildrenList);
            } else if (tableOfContentItemVO.getParentItem() != null) {
                tableOfContentItemVO.getParentItem().addAllChildItems(itemVOChildrenList);
            } else {
                itemVOChildrenList.forEach(childItem -> itemVOList.add(childItem));
            }
        }
    }

    private static TableOfContentItemVO buildTableOfContentsItemVO(List<TocItem> tocItems, VTDNav contentNavigator) throws NavException {

        int originalNavigationIndex = contentNavigator.getCurrentIndex();

        // get the tocItem
        String tagName = contentNavigator.toString(contentNavigator.getCurrentIndex());
        TocItem tocItem = TocItemUtils.getTocItemByName(tocItems, tagName);

        if (tocItem == null) {
            // unsupported tag name
            return null;
        }

        // get the id
        int attIndex = contentNavigator.getAttrVal(XMLID);
        String elementId = null;
        if (attIndex != -1) {
            elementId = contentNavigator.toString(attIndex);
        }

        //get the leos:origin attr
        String originAttr = getOriginAttribute(contentNavigator);
        SoftActionType softActionAttr = getSoftActionAttribute(contentNavigator);
        Boolean isSoftActionRoot = getSoftActionRootAttribute(contentNavigator);
        String softUserAttr = getSoftUserAttribute(contentNavigator);
        GregorianCalendar softDateAttr = getSoftDateAttribute(contentNavigator);
        String softMovedFrom = getSoftMovedFromAttribute(contentNavigator);
        String softMovedTo = getSoftMovedToAttribute(contentNavigator);

        // get the intro
        Integer introTagIndex = null;
        if (contentNavigator.toElement(VTDNav.FIRST_CHILD, INTRO)) {
            introTagIndex = contentNavigator.getCurrentIndex();
            contentNavigator.recoverNode(originalNavigationIndex);
        }

        // get the num
        String number = null;
        Integer numberTagIndex = null;
        String originNumAttr = null;
        SoftActionType numSoftActionAttribute=null;
        if (contentNavigator.toElement(VTDNav.FIRST_CHILD, NUM)) {
            numberTagIndex = contentNavigator.getCurrentIndex();
            //get the leos:origin attr
            originNumAttr =  getOriginAttribute(contentNavigator);
            numSoftActionAttribute =  SoftActionType.of(getNumSoftActionAttribute(contentNavigator));
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
        
        String list = null;
        Integer listTagIndex = null;
        if(contentNavigator.toElement(VTDNav.FIRST_CHILD, LIST)) {
            listTagIndex = contentNavigator.getCurrentIndex();
            long contentFragment = contentNavigator.getContentFragment();
            list = getFragmentAsString(contentNavigator, contentFragment, true);
            contentNavigator.recoverNode(originalNavigationIndex);
        }
        
        int elementDepth = getElementDepth(contentNavigator);
        contentNavigator.recoverNode(originalNavigationIndex);
        
        //get the content
        String content = extractContentForTocItemsExceptNumAndHeadingAndIntro(tocItems, contentNavigator);
        contentNavigator.recoverNode(originalNavigationIndex);

        // build the table of content item and return it
        return new TableOfContentItemVO(tocItem, elementId, originAttr, number, originNumAttr, heading, numberTagIndex, headingTagIndex,
                introTagIndex, contentNavigator.getCurrentIndex(), list, listTagIndex, content, softActionAttr,
                isSoftActionRoot, softUserAttr, softDateAttr, softMovedFrom, softMovedTo, false, numSoftActionAttribute, elementDepth);
    }
    
    private static int getElementDepth(VTDNav vtdNav) throws NavException {
        int depth = 0;
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
            long contentFragment = vtdNav.getContentFragment();
            String elementNumber = new String(vtdNav.getXML().getBytes((int) contentFragment, (int) (contentFragment >> 32)));
            if(elementNumber.contains(".")) {
                String[] levelArr = StringUtils.split(elementNumber, LEVEL_NUM_SEPARATOR);
                depth = levelArr.length;
            }
        }
        return depth;
    }

    private static boolean shouldItemBeAddedToToc(List<TocItem> tocItems, Map<TocItem, List<TocItem>> tocRules, VTDNav vtdNav, TocItem tocItem) throws NavException {
        boolean addItemToToc = false;
        if (tocItem.isRoot()) {
            addItemToToc = tocItem.isDisplay();
        } else {        
            TocItem parentTocItem = TocItemUtils.getTocItemByName(tocItems, getParentTagName(vtdNav));
            if ((parentTocItem != null) && (tocRules.get(parentTocItem) != null)) {
                addItemToToc = tocRules.get(parentTocItem).contains(tocItem);
            }
        }
        return addItemToToc;
    }

    private static String getParentTagName(VTDNav vtdNav) throws NavException {
        String elementTagName = null;
        int currentIndex = vtdNav.getCurrentIndex();
        try {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                elementTagName = vtdNav.toString(vtdNav.getCurrentIndex());
            }
        } finally {
            vtdNav.recoverNode(currentIndex);
        }
        return elementTagName;
    }

    static byte[] extractLevelNonTocItems(List<TocItem> tocItems, Map<TocItem, List<TocItem>> tocRules, VTDNav vtdNav, TableOfContentItemVO tableOfContentItemVO) throws NavException {
        byte[] contentTag = extractLevelNonTocItems(tocItems, tocRules, vtdNav);
        if (tableOfContentItemVO.isUndeleted()) {
            contentTag = updateXMLIDAttributesInElementContent(contentTag, "", true);
        }
        return contentTag;
    }
    
    static byte[] extractLevelNonTocItems(List<TocItem> tocItems, Map<TocItem, List<TocItem>> tocRules, VTDNav vtdNav) throws NavException {
        ByteArrayBuilder nonTocItems = new ByteArrayBuilder();
        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            nonTocItems.append(extractNonTocItemExceptNumAndHeadingAndIntro(tocItems, tocRules, vtdNav));
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                nonTocItems.append(extractNonTocItemExceptNumAndHeadingAndIntro(tocItems, tocRules, vtdNav));
            }
        }

        return nonTocItems.getContent();
    }

    static byte[] extractIndexedNonTocElements(VTDNav contentNavigator, int itemIndex) throws NavException, UnsupportedEncodingException {
        contentNavigator.recoverNode(itemIndex);
        return getTagWithContent(contentNavigator);
    }

    static Boolean checkIfNumberingIsToggled(TableOfContentItemVO tableOfContentItemVO) {
        if (PARAGRAPH.equals(tableOfContentItemVO.getTocItem().getAknTag().value()) && tableOfContentItemVO.getParentItem() != null
                && tableOfContentItemVO.getParentItem().isNumberingToggled() != null) {
            return tableOfContentItemVO.getParentItem().isNumberingToggled();
        }
        return null;
    }

    static byte[] extractOrBuildNumElement(VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO) throws NavException {
        byte[] element = new byte[0];
        Boolean toggleFlag = checkIfNumberingIsToggled(tableOfContentItemVO);

        if (toggleFlag != null && toggleFlag
                && (tableOfContentItemVO.getNumber() == null || tableOfContentItemVO.getNumber() == "")) {
            List<TableOfContentItemVO> proposalParaVO = new ArrayList<TableOfContentItemVO>();
            for (TableOfContentItemVO itemVO : tableOfContentItemVO.getParentItem().getChildItems()) {
                if (EC.equals(itemVO.getOriginAttr())) {
                    proposalParaVO.add(itemVO);
                }
            }
            if (CN.equals(tableOfContentItemVO.getOriginAttr())
                    || (EC.equals(tableOfContentItemVO.getOriginAttr())
                    && SoftActionType.MOVE_FROM.equals(tableOfContentItemVO.getSoftActionAttr()))) {
                tableOfContentItemVO.setNumber("#");
                tableOfContentItemVO.setOriginNumAttr(CN);
            } else {
                tableOfContentItemVO.setNumber((proposalParaVO.indexOf(tableOfContentItemVO) + 1) + ".");
                tableOfContentItemVO.setOriginNumAttr(EC);
                tableOfContentItemVO.setNumSoftActionAttr(SoftActionType.ADD);
            }
        } else if ((toggleFlag != null && !toggleFlag)
                && (tableOfContentItemVO.getNumSoftActionAttr() != null && SoftActionType.ADD.equals(tableOfContentItemVO.getNumSoftActionAttr())
                || CN.equals(tableOfContentItemVO.getOriginNumAttr() != null ? tableOfContentItemVO.getOriginNumAttr() : tableOfContentItemVO.getOriginAttr()))) {
            tableOfContentItemVO.setNumber(null);
            return element;
        }
        if (tableOfContentItemVO.getNumber() != null) {
            element = createNumBytes(contentNavigator, tableOfContentItemVO);
        }
        return element;
    }

    public static String getTagValueFromTocItemVo(TableOfContentItemVO tableOfContentItemVO) {
        return tableOfContentItemVO.getTocItem().getAknTag().value();
    }

    private static byte[] createNumBytes(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException {
        byte[] element;
        StringBuilder item = new StringBuilder(StringUtils.capitalize(parentTOC.getTocItem().getAknTag().value()));
        byte[] numBytes;
        if (parentTOC.getTocItem().isNumWithType()) {
            numBytes = item.append(" ").append(extractNumber(parentTOC.getNumber())).toString().getBytes(UTF_8);
        } else {
            numBytes = (extractNumber(parentTOC.getNumber())).getBytes(UTF_8);
        }
        if (parentTOC.getNumTagIndex() != null) {
            contentNavigator.recoverNode(parentTOC.getNumTagIndex());
            byte[] numTag = getStartTagAndRemovePrefix(contentNavigator, parentTOC);
            numTag = updateOriginAttribute(numTag, parentTOC.getOriginNumAttr());
            element = XmlHelper.buildTag(numTag, NUM_BYTES, numBytes);
        } else {
            byte[] numStartTag = updateOriginAttribute(NUM_START_TAG, parentTOC.getOriginNumAttr());
            element = XmlHelper.buildTag(numStartTag, NUM_BYTES, numBytes);
        }
        return element;
    }

    static byte[] extractOrBuildHeaderElement(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException, UnsupportedEncodingException {
        byte[] element = new byte[0];
        if (parentTOC.getHeading() != null && (parentTOC.getTocItem().getItemHeading() != OptionsType.OPTIONAL || (parentTOC.getTocItem().getItemHeading() == OptionsType.OPTIONAL && !parentTOC.getHeading().trim().isEmpty()))) {
            if (parentTOC.getHeadingTagIndex() != null) {
                contentNavigator.recoverNode(parentTOC.getHeadingTagIndex());
                byte[] headingTag = getStartTagAndRemovePrefix(contentNavigator, parentTOC);
                element = XmlHelper.buildTag(headingTag, HEADING_BYTES, parentTOC.getHeading().getBytes(UTF_8));
            } else {
                element = XmlHelper.buildTag(HEADING_START_TAG, HEADING_BYTES, parentTOC.getHeading().getBytes(UTF_8));
            }
        }
        return element;
    }
    
    static byte[] getStartTagAndRemovePrefix(VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO) throws NavException {
        byte[] xmlTag = VTDUtils.getStartTag(contentNavigator);
        if (tableOfContentItemVO.isUndeleted()) {
            xmlTag = updateXMLIDAttributesInElementContent(xmlTag, "", true);
        }
        return xmlTag;
    }
    
    private static byte[] extractNonTocItemExceptNumAndHeadingAndIntro(List<TocItem> tocItems, Map<TocItem, List<TocItem>> tocRules, VTDNav vtdNav) throws NavException {
        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        TocItem tocItem = TocItemUtils.getTocItemByName(tocItems, tagName);
        if ((tocItem == null || !shouldItemBeAddedToToc(tocItems, tocRules, vtdNav, tocItem)) &&
                (!tagName.equals(NUM) && !tagName.equals(HEADING) && !tagName.equals(INTRO))) {
            return getTagWithContent(vtdNav);
        }
        return new byte[0];
    }

    static byte[] getTagWithContent(VTDNav vtdNav) throws NavException {
        long fragment = vtdNav.getElementFragment();
        return vtdNav.getXML().getBytes((int) fragment, (int) (fragment >> 32));
    }
    
    private static String extractContentForTocItemsExceptNumAndHeadingAndIntro(List<TocItem> tocItems, VTDNav vtdNav) throws NavException {
        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        TocItem tocItem = TocItemUtils.getTocItemByName(tocItems, tagName);
        String elementName = vtdNav.toString(vtdNav.getCurrentIndex());
        
        if (elementName.equalsIgnoreCase(tocItem.getAknTag().value()) &&
                (vtdNav.toElement(VTDNav.FIRST_CHILD, HEADING) || vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)
                        || vtdNav.toElement(VTDNav.FIRST_CHILD, INTRO))) {
            if(!vtdNav.toElement(VTDNav.NEXT_SIBLING))
                return StringUtils.EMPTY;
            LOG.trace("extractContentForTocItemsExceptNumAndHeading - Skipping {} tag for {}", vtdNav.toString(vtdNav.getCurrentIndex()), tocItem.getAknTag().value());
        } 
        return new String(getTagWithContent(vtdNav), UTF_8);
    }
    
    public List<TableOfContentItemVO> buildTableOfContent(String startingNode, byte[] xmlContent, TocMode mode) {
        LOG.trace("Start building the table of content");
        long startTime = System.currentTimeMillis();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {
            VTDNav contentNavigator = setupVTDNav(xmlContent);
            
            if (contentNavigator.toElement(VTDNav.FIRST_CHILD, startingNode)) {
                List<TocItem> tocItems = structureContextProvider.get().getTocItems();
                Map<TocItem, List<TocItem>> tocRules = structureContextProvider.get().getTocRules();
                itemVOList = getAllChildTableOfContentItems(tocItems, tocRules, contentNavigator, mode);
            }
            
        } catch (Exception e) {
            LOG.error("Unable to build the Table of content item list", e);
            throw new RuntimeException("Unable to build the Table of content item list", e);
        }
        
        LOG.trace("Build table of content completed in {} ms", (System.currentTimeMillis() - startTime));
        return itemVOList;
    }
}
