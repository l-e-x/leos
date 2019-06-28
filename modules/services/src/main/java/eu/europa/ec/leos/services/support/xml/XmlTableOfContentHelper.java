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
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.function.Function;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.*;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.*;
import static java.nio.charset.StandardCharsets.UTF_8;

class XmlTableOfContentHelper {

    private static final Logger LOG = LoggerFactory.getLogger(XmlTableOfContentHelper.class);
    
    static List<TableOfContentItemVO> getAllChildTableOfContentItems(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, boolean simplified)
            throws NavException {
        int currentIndex = contentNavigator.getCurrentIndex();
        List<TableOfContentItemVO> itemVOList = new ArrayList<>();
        try {
            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                addTocItemVoToList(getTocItemType, contentNavigator, itemVOList, simplified);
                while (contentNavigator.toElement(VTDNav.NEXT_SIBLING)) {
                    addTocItemVoToList(getTocItemType, contentNavigator, itemVOList, simplified);
                }
            }
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }

        return itemVOList;
    }

    static boolean navigateToFirstTocElment(List<TableOfContentItemVO> tableOfContentItemVOs, VTDNav vtdNav) throws NavException {
        TableOfContentItemVO firstTocVO = tableOfContentItemVOs.listIterator().next();
        return navigateToElementByNameAndId(firstTocVO.getType().getName(), null, vtdNav);
    }

    private static void addTocItemVoToList(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, List<TableOfContentItemVO> itemVOList, boolean simplified)
            throws NavException {
        TableOfContentItemVO tableOfContentItemVO = buildTableOfContentsItemVO(getTocItemType, contentNavigator);
        if (tableOfContentItemVO != null) {
        	List<TableOfContentItemVO> itemVOChildrenList = getAllChildTableOfContentItems(getTocItemType, contentNavigator, simplified);
            if (tableOfContentItemVO.getType().isToBeDisplayed()) {
            	if (simplified && tableOfContentItemVO.getType().getName().equalsIgnoreCase(LIST) && !itemVOList.isEmpty()) {
            		tableOfContentItemVO = itemVOList.get(itemVOList.size() - 1);
            		tableOfContentItemVO.addAllChildItems(itemVOChildrenList);
            		return;
            	} else if (simplified && (tableOfContentItemVO.getType().getName().equalsIgnoreCase(PARAGRAPH) || tableOfContentItemVO.getType().getName().equalsIgnoreCase(POINT))) {
            		if ((itemVOChildrenList.size() > 1) && (itemVOChildrenList.get(0).getChildItems().isEmpty())) {
            		    tableOfContentItemVO.setId(itemVOChildrenList.get(0).getId());
            			itemVOChildrenList.remove(0);
            		} else if (itemVOChildrenList.size() == 1) {
            		    tableOfContentItemVO.setId(itemVOChildrenList.get(0).getId());
            			itemVOChildrenList = itemVOChildrenList.get(0).getChildItems();
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

    private static TableOfContentItemVO buildTableOfContentsItemVO(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator) throws NavException {

        int originalNavigationIndex = contentNavigator.getCurrentIndex();

        // get the type
        String tagName = contentNavigator.toString(contentNavigator.getCurrentIndex());
        TocItemType type = getTocItemType.apply(tagName);

        if (type == null) {
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
        
        //get the preamble formula1
        Integer formula1TagIndex = getPreambleNonTocElements(VTDNav.FIRST_CHILD, FORMULA, contentNavigator);
        contentNavigator.recoverNode(originalNavigationIndex);
        
        //get the preamble formula2
        Integer formula2TagIndex = getPreambleNonTocElements(VTDNav.LAST_CHILD, FORMULA, contentNavigator);
        contentNavigator.recoverNode(originalNavigationIndex);
        
        //get the recitals intro
        Integer introTagIndex = getPreambleNonTocElements(VTDNav.FIRST_CHILD, INTRO, contentNavigator);
        contentNavigator.recoverNode(originalNavigationIndex);
        
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
        
        //get the content
        String content = extractContentForTocItemsExceptNumAndHeading(getTocItemType, contentNavigator);
        contentNavigator.recoverNode(originalNavigationIndex);

        // build the table of content item and return it
        return new TableOfContentItemVO(type, elementId, originAttr, number, originNumAttr, heading, numberTagIndex, headingTagIndex,
                contentNavigator.getCurrentIndex(), formula1TagIndex, formula2TagIndex, introTagIndex, list, listTagIndex, content, softActionAttr,
                isSoftActionRoot, softUserAttr, softDateAttr, softMovedFrom, softMovedTo, false, numSoftActionAttribute);
    }
    
    private static Integer getPreambleNonTocElements(int direction, String element, VTDNav contentNavigator) throws NavException {
        Integer elementIndex = null;
        if (contentNavigator.toElement(direction, element)) {
            elementIndex = contentNavigator.getCurrentIndex();
        }
        return elementIndex;
    }
    
    static byte[] extractLevelNonTocItems(Function<String, TocItemType> getTocItemType, VTDNav vtdNav, TableOfContentItemVO tableOfContentItemVO) throws NavException {
        byte[] contentTag = extractLevelNonTocItems(getTocItemType, vtdNav);
        if (tableOfContentItemVO.isUndeleted()) {
            contentTag = updateXMLIDAttributesInElementContent(contentTag, "", true);
        }
        return contentTag;
    }
    
    static byte[] extractLevelNonTocItems(Function<String, TocItemType> getTocItemType, VTDNav vtdNav) throws NavException {
        ByteArrayBuilder nonTocItems = new ByteArrayBuilder();
        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            nonTocItems.append(extractNonTocItemExceptNumAndHeading(getTocItemType, vtdNav));
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING)) {
                nonTocItems.append(extractNonTocItemExceptNumAndHeading(getTocItemType, vtdNav));
            }
        }

        return nonTocItems.getContent();
    }

    static byte[] extractIndexedNonTocElements(VTDNav contentNavigator, int itemIndex) throws NavException, UnsupportedEncodingException {
        contentNavigator.recoverNode(itemIndex);
        return getTagWithContent(contentNavigator);
    }

    static Boolean checkIfNumberingIsToggled(TableOfContentItemVO tableOfContentItemVO) {
        if (PARAGRAPH.equals(tableOfContentItemVO.getType().getName()) && tableOfContentItemVO.getParentItem() != null
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

            if (LEOS_ORIGIN_ATTR_CN.equals(tableOfContentItemVO.getOriginAttr())) {
                tableOfContentItemVO.setNumber("#");
                tableOfContentItemVO.setOriginNumAttr(LEOS_ORIGIN_ATTR_CN);

            } else {
                List<TableOfContentItemVO> proposalParaVO = new ArrayList();
                for (TableOfContentItemVO itemVO : tableOfContentItemVO.getParentItem().getChildItems()) {
                    if (LEOS_ORIGIN_ATTR_EC.equals(itemVO.getOriginAttr())) {
                        proposalParaVO.add(itemVO);
                    }
                }
                tableOfContentItemVO.setNumber((proposalParaVO.indexOf(tableOfContentItemVO) + 1) + ".");
                tableOfContentItemVO.setOriginNumAttr(LEOS_ORIGIN_ATTR_EC);
            }

        } else if ((toggleFlag != null && !toggleFlag)
                && (tableOfContentItemVO.getNumSoftActionAttr() != null && SoftActionType.ADD.equals(tableOfContentItemVO.getNumSoftActionAttr())
                || LEOS_ORIGIN_ATTR_CN.equals(tableOfContentItemVO.getOriginNumAttr() != null ? tableOfContentItemVO.getOriginNumAttr() : tableOfContentItemVO.getOriginAttr()))) {
            tableOfContentItemVO.setNumber(null);
            return element;
        }
        if (tableOfContentItemVO.getNumber() != null) {
            element = createNumBytes(contentNavigator, tableOfContentItemVO);
        }
        return element;
    }

    private static byte[] createNumBytes(VTDNav contentNavigator, TableOfContentItemVO parentTOC) throws NavException {
        byte[] element;
        StringBuilder item = new StringBuilder(StringUtils.capitalize(parentTOC.getType().getName()));
        byte[] numBytes;
        if (parentTOC.getType().hasNumWithType()) {
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
        if (parentTOC.getHeading() != null) {
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
    
    private static byte[] extractNonTocItemExceptNumAndHeading(Function<String, TocItemType> getTocItemType, VTDNav vtdNav) throws NavException {

        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        TocItemType type = getTocItemType.apply(tagName);
        if (type == null && (!tagName.equals(NUM) && (!tagName.equals(HEADING)))) {
            return getTagWithContent(vtdNav);
        }

        return new byte[0];
    }

    static byte[] getTagWithContent(VTDNav vtdNav) throws NavException {
        long fragment = vtdNav.getElementFragment();
        return vtdNav.getXML().getBytes((int) fragment, (int) (fragment >> 32));
    }
    
    private static String extractContentForTocItemsExceptNumAndHeading(Function<String, TocItemType> getTocItemType, VTDNav vtdNav) throws NavException {
        String tagName = vtdNav.toString(vtdNav.getCurrentIndex());
        TocItemType type = getTocItemType.apply(tagName);
        String elementName = vtdNav.toString(vtdNav.getCurrentIndex());
        
        if (elementName.equalsIgnoreCase(type.getName()) &&
                (vtdNav.toElement(VTDNav.FIRST_CHILD, HEADING) || vtdNav.toElement(VTDNav.FIRST_CHILD, NUM))) {
            vtdNav.toElement(VTDNav.NEXT_SIBLING);
            LOG.trace("extractContentForTocItemsExceptNumAndHeading - Skipping {} tag for {}", vtdNav.toString(vtdNav.getCurrentIndex()), type.getName());
        } 
        
        return new String(getTagWithContent(vtdNav), UTF_8);
    }
}
