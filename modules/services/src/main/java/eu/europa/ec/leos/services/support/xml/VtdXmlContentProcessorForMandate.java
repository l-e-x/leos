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

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.support.IdGenerator;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.services.support.xml.ref.Ref;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.AnnexTocItemType;
import eu.europa.ec.leos.vo.toctype.LegalTextMandateTocItemType;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.CONTENT;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.EMPTY_STRING;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_AFFECTED_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_DELETABLE_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_EDITABLE_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_ORIGIN_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_ACTION_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_ACTION_ROOT_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_DATE_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_MOVED_LABEL_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_MOVE_FROM;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_MOVE_TO;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_TRANS_FROM;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_USER_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.SOFT_DELETE_PLACEHOLDER_ID_PREFIX;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.SOFT_TRANSFORM_PLACEHOLDER_ID_PREFIX;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.XMLID;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.TOGGLED_TO_NUM;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getFragmentAsString;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getStartTag;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.insertAffectedAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.removeAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.toByteArray;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.restoreOldId;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateAttributeValue;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateOriginAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateSoftInfo;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateSoftTransFromAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateXMLIDAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateXMLIDAttributesInElementContent;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.ELEMENTS_TO_BE_PROCESSED_FOR_NUMBERING;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEOS_ORIGIN_ATTR_CN;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEOS_ORIGIN_ATTR_EC;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LIST;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.POINT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.RECITAL;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SUBPARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.SUBPOINT;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractIndexedNonTocElements;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractLevelNonTocItems;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractOrBuildHeaderElement;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractOrBuildNumElement;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.getTagWithContent;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.navigateToFirstTocElment;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Instance(InstanceType.COUNCIL)
public class VtdXmlContentProcessorForMandate extends VtdXmlContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VtdXmlContentProcessorForMandate.class);

    @Override
    public byte[] createDocumentContentWithNewTocList(Function<String, TocItemType> getTocItemType, List<TableOfContentItemVO> tableOfContentItemVOs,
            byte[] content, User user) {
        LOG.trace("Start building the document content for the new toc list");
        long startTime = System.currentTimeMillis();
        try {

            ByteArrayBuilder mergedContent = new ByteArrayBuilder();
            VTDNav contentNavigator = setupVTDNav(content);
            int docLength = content.length;
            int endOfContent = 0;

            if (!tableOfContentItemVOs.isEmpty() && navigateToFirstTocElment(tableOfContentItemVOs, contentNavigator)) {
                int index = contentNavigator.getCurrentIndex();

                // append everything up until the first toc element
                long contentFragment = contentNavigator.getElementFragment();
                int offset = (int) contentFragment;
                int length = (int) (contentFragment >> 32);
                mergedContent.append(contentNavigator.getXML().getBytes(0, offset));

                for (TableOfContentItemVO tocVo : tableOfContentItemVOs) {
                    index = tocVo.getVtdIndex();
                    mergedContent.append(buildTocItemContent(getTocItemType, contentNavigator, tocVo, user));
                }

                contentNavigator.recoverNode(index);
                contentFragment = contentNavigator.getElementFragment();
                offset = (int) contentFragment;
                length = (int) (contentFragment >> 32);

                endOfContent = offset + length;
            }
            // append everything after the content
            mergedContent.append(contentNavigator.getXML().getBytes(endOfContent, docLength - (endOfContent)));

            LOG.trace("Build the document content for the new toc list completed in {} ms", (System.currentTimeMillis() - startTime));
            return mergedContent.getContent();

        } catch (Exception e) {
            LOG.error("Unable to save the Table of content item list", e);
            throw new RuntimeException("Unable to save the Table of content item list", e);
        }
    }

    static boolean isNumberSoftDeleted(TableOfContentItemVO tableOfContentItemVO) {
        if(tableOfContentItemVO.getNumSoftActionAttr() != null && SoftActionType.DELETE.equals(tableOfContentItemVO.getNumSoftActionAttr())){
            return true;
        }
        return false;
    }

    private byte[] buildTocItemContent(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO,
            User user)
            throws NavException, UnsupportedEncodingException {
        ByteArrayBuilder tocItemContent = new ByteArrayBuilder();
        if (!(tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.PARAGRAPH) && skipParagraphContent(tableOfContentItemVO)) &&
                !(tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.POINT) && skipPointContent(tableOfContentItemVO))) {
            byte[] numTag = extractOrBuildNumElement(contentNavigator, tableOfContentItemVO);

            //this method does the num toggle processing
            numTag = numberElementToggleProcessing(tableOfContentItemVO, numTag);

            tocItemContent.append(numTag);
        }
        tocItemContent.append(extractOrBuildHeaderElement(contentNavigator, tableOfContentItemVO));
        if (tableOfContentItemVO.getPreambleFormula1TagIndex() != null) {
            tocItemContent.append(extractIndexedNonTocElements(contentNavigator, tableOfContentItemVO.getPreambleFormula1TagIndex()));
        }
        if (tableOfContentItemVO.getRecitalsIntroIndex() != null) {
            tocItemContent.append(extractIndexedNonTocElements(contentNavigator, tableOfContentItemVO.getRecitalsIntroIndex()));
        }

        for (TableOfContentItemVO child : tableOfContentItemVO.getChildItemsView()) {
            tocItemContent.append(buildTocItemContent(getTocItemType, contentNavigator, child, user));
        }

        byte[] startTag = new byte[0];
        String tocTagName = tableOfContentItemVO.getType().getName();

        if (tableOfContentItemVO.getVtdIndex() != null) {
            contentNavigator.recoverNode(tableOfContentItemVO.getVtdIndex());
            if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.PARAGRAPH) && skipParagraphContent(tableOfContentItemVO)) {
                startTag = updateOriginAttribute(getStartTag(contentNavigator), tableOfContentItemVO.getOriginAttr());
                tocItemContent = buildParagraphContent(contentNavigator, tableOfContentItemVO, user, tocItemContent);
            } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.POINT)) {
                startTag = updateOriginAttribute(getStartTag(contentNavigator), tableOfContentItemVO.getOriginAttr());
                if (skipPointContent(tableOfContentItemVO)) {
                    tocItemContent = buildPointContent(contentNavigator, tableOfContentItemVO, user, tocItemContent);
                } else if (shouldWrapWithList(tableOfContentItemVO.getParentItem())) {
                    tocItemContent.append(tableOfContentItemVO.getContent().getBytes(UTF_8));
                    byte[] pointTag = wrapWithPoint(tableOfContentItemVO, user, tocItemContent.getContent());

                    pointTag = updateSoftInfo(pointTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                            tableOfContentItemVO.getOriginAttr(), tableOfContentItemVO.getSoftMoveFrom(), false, tableOfContentItemVO.getType());
                    return constructListStructure(tableOfContentItemVO, user, pointTag);
                } else {
                    startTag = buildExistingXmlNode(getTocItemType, contentNavigator, tableOfContentItemVO, tocItemContent);
                }
            } else if ((tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.SUBPARAGRAPH) ||
                    tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.SUBPOINT)) && isSingleSubElement(tableOfContentItemVO) &&
                    !isSoftDeletedOrMoved(tableOfContentItemVO)) {
                return extractSubElementContent(contentNavigator, tableOfContentItemVO);
            } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.LIST) && isEmptyElement(tableOfContentItemVO)) {
                return "".getBytes(UTF_8); // remove list content if there is no child
            } else {
                startTag = buildExistingXmlNode(getTocItemType, contentNavigator, tableOfContentItemVO, tocItemContent);
            }

            if (SoftActionType.MOVE_TO.equals(tableOfContentItemVO.getSoftActionAttr())) {
                startTag = updateXMLIDAttribute(startTag, tableOfContentItemVO.getId());
                tocItemContent = updateXMLIDAttributesInElementContent(tocItemContent, SOFT_MOVE_PLACEHOLDER_ID_PREFIX, false);
            } else if (SoftActionType.DELETE.equals(tableOfContentItemVO.getSoftActionAttr())) {
                startTag = updateXMLIDAttribute(startTag, tableOfContentItemVO.getId());
                tocItemContent = updateXMLIDAttributesInElementContent(tocItemContent, SOFT_DELETE_PLACEHOLDER_ID_PREFIX, false);
            } else if ((tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.PARAGRAPH) || tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.POINT)) &&
                    !isEmptyElement(tableOfContentItemVO) && isSingleSubElement(tableOfContentItemVO.getChildItems().get(0)) &&
                    !isSoftDeletedOrMoved(tableOfContentItemVO.getChildItems().get(0))) {
                startTag = updateSoftTransFromAttribute(startTag, tableOfContentItemVO.getChildItems().get(0).getId());
            }
        } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.CITATION)) {
            byte[] citationTag = XmlHelper.getCitationTemplate().getBytes(UTF_8);
            citationTag = updateSoftInfo(citationTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                    tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
            return updateOriginAttribute(citationTag, tableOfContentItemVO.getOriginAttr());
        } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.RECITAL)) {
            byte[] recitalTag = XmlHelper.getRecitalTemplate(tableOfContentItemVO.getNumber()).getBytes(UTF_8);
            recitalTag = updateSoftInfo(recitalTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                    tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
            return updateOriginAttribute(recitalTag, tableOfContentItemVO.getOriginAttr());
        } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.ARTICLE)) {
            if(tableOfContentItemVO.getChildItemsView().isEmpty()) {
                byte[] articleTag = XmlHelper.getArticleTemplate(tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading()).getBytes(UTF_8);
                articleTag = updateSoftInfo(articleTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                        tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
                return updateOriginAttribute(articleTag, tableOfContentItemVO.getOriginAttr());
            } else {
                String startTagStr = "<" + tocTagName + " xml:id=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7)
                        + "\" leos:editable=\"true\"  leos:deletable=\"true\">";
                startTag = updateOriginAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.getOriginAttr());
            }
        } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.PARAGRAPH)) {
            if (tableOfContentItemVO.getChildItemsView().isEmpty()) {
                byte[] paragraphTag = XmlHelper.getParagraphTemplate(tableOfContentItemVO.getNumber()).getBytes(UTF_8);
                paragraphTag = updateSoftInfo(paragraphTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                        tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
                return updateOriginAttribute(paragraphTag, tableOfContentItemVO.getOriginAttr());
            } else {
                String startTagStr = "<" + tocTagName + " xml:id=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7) + "\">";
                startTag = updateOriginAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.getOriginAttr());
                tocItemContent = buildParagraphContent(contentNavigator, tableOfContentItemVO, user, tocItemContent);
            }
        } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.SUBPARAGRAPH)) {
            byte[] subParagraphTag = XmlHelper.getSubParagraphTemplate(null).getBytes(UTF_8);
            subParagraphTag = updateSoftInfo(subParagraphTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                    tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
            return updateOriginAttribute(subParagraphTag, tableOfContentItemVO.getOriginAttr());
        } else if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.POINT)) {
            if (tableOfContentItemVO.getChildItemsView().isEmpty()) {
                byte[] pointTag = XmlHelper.getPointTemplate(tableOfContentItemVO.getNumber()).getBytes(UTF_8);
                pointTag = updateSoftInfo(pointTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                        tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
                pointTag = updateOriginAttribute(pointTag, tableOfContentItemVO.getOriginAttr());

                return constructListStructure(tableOfContentItemVO, user, pointTag);
            } else {
                byte[] pointTag = buildPointContent(contentNavigator, tableOfContentItemVO, user, tocItemContent).getContent();
                pointTag = wrapWithPoint(tableOfContentItemVO, user, pointTag);
                pointTag = updateSoftInfo(pointTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                        tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
                return constructListStructure(tableOfContentItemVO, user, pointTag);
            }
        } else if (tableOfContentItemVO.getType().equals(AnnexTocItemType.DIVISION)) {
            byte[] annexTag = XmlHelper.getAnnexTemplate().getBytes(UTF_8);
            annexTag = updateSoftInfo(annexTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                    tableOfContentItemVO.getOriginAttr(), null, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
            return updateOriginAttribute(annexTag, tableOfContentItemVO.getOriginAttr());
        } else {
            String startTagStr = "<" + tocTagName + " xml:id=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7) + "\">";
            startTag = updateOriginAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.getOriginAttr());
        }
        
        final String moveId;
        if (tableOfContentItemVO.getSoftActionAttr() != null && tableOfContentItemVO.getSoftActionAttr().equals(SoftActionType.MOVE_TO)) {
            moveId = tableOfContentItemVO.getSoftMoveTo();
        } else if (tableOfContentItemVO.getSoftActionAttr() != null && tableOfContentItemVO.getSoftActionAttr().equals(SoftActionType.MOVE_FROM)) {
            moveId = tableOfContentItemVO.getSoftMoveFrom();
        } else {
            moveId = null;
        }
        startTag = updateSoftInfo(startTag, tableOfContentItemVO.getSoftActionAttr(), tableOfContentItemVO.isSoftActionRoot(), user,
                            tableOfContentItemVO.getOriginAttr(), moveId, tableOfContentItemVO.isUndeleted(), tableOfContentItemVO.getType());
        
        startTag = insertAffectedAttribute(startTag, tableOfContentItemVO.isAffected());
        return XmlHelper.buildTag(startTag, tocTagName.getBytes(UTF_8), tocItemContent.getContent());
    }

    private byte[] numberElementToggleProcessing(TableOfContentItemVO tableOfContentItemVO, byte[] numTag) {
        if (tableOfContentItemVO.getType().equals(LegalTextMandateTocItemType.PARAGRAPH)) {
            if (tableOfContentItemVO.getParentItem().isNumberingToggled() != null) {
                if (tableOfContentItemVO.getParentItem().isNumberingToggled()) {
                    if (isNumberSoftDeleted(tableOfContentItemVO)) {// if a para is soft deleted and numbering is toggled
                        numTag = updateSoftActionOnNumElement(new String(numTag), null, TOGGLED_TO_NUM);
                    } else {
                        numTag = updateSoftActionOnNumElement(new String(numTag), SoftActionType.ADD, TOGGLED_TO_NUM);
                    }
                } else {
                    numTag = updateSoftActionOnNumElement(new String(numTag), SoftActionType.DELETE, null);
                }
            } else if (tableOfContentItemVO.getNumSoftActionAttr() != null){//in case paragraph is moved which was toggled to num before, removing soft attributes from num element
                    numTag = updateSoftActionOnNumElement(new String(numTag), tableOfContentItemVO.getNumSoftActionAttr(), "");
            } else {
                numTag = updateSoftActionOnNumElement(new String(numTag), null, "");
            }
        }
        return numTag;
    }

    @Override
    public void specificInstanceXMLPostProcessing(XMLModifier xmlModifier) throws Exception {
        updateSoftMoveLabelAttribute(xmlModifier, LEOS_SOFT_MOVE_TO);
        updateSoftMoveLabelAttribute(xmlModifier, LEOS_SOFT_MOVE_FROM);
        updateNewElements(xmlModifier, RECITAL, null);
        updateNewElements(xmlModifier, ARTICLE, null);
        updateNewElements(xmlModifier, PARAGRAPH, SUBPARAGRAPH);
        updateNewElements(xmlModifier, POINT, SUBPOINT);
    }

    private void updateSoftMoveLabelAttribute(XMLModifier xmlModifier, String attr) throws Exception {
        VTDNav vtdNav = xmlModifier.outputAndReparse();
        xmlModifier.bind(vtdNav);
        vtdNav.toElement(VTDNav.ROOT);
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.declareXPathNameSpace("leos", "urn:eu:europa:ec:leos");// required
        autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
        autoPilot.selectXPath(String.format("//*[@%s]", attr));
        int currentIndex;
        if (autoPilot.evalXPathToBoolean()) {
            while (autoPilot.evalXPath() != -1) {
                currentIndex = vtdNav.getCurrentIndex();
                try {
                    String updatedMovedReferenceContent;
                    Result<String> labelResult = referenceLabelProcessor.generateSoftmoveLabel(getRefFromSoftMovedElt(vtdNav, attr),
                            getParentId(vtdNav), vtdNav, attr);
                    vtdNav.recoverNode(currentIndex);
                    if (labelResult.isOk()) {
                        updatedMovedReferenceContent = labelResult.get();
                        autoPilot.selectAttr(LEOS_SOFT_MOVED_LABEL_ATTR);
                        int indexAttr = autoPilot.iterateAttr();
                        if (indexAttr != -1) {
                            xmlModifier.removeAttribute(indexAttr);
                        }
                        xmlModifier.insertAttribute(new StringBuilder(" ").append(LEOS_SOFT_MOVED_LABEL_ATTR).append("=\"")
                                .append(updatedMovedReferenceContent).append("\"").toString());
                    }
                } catch (Exception ex) {
                    String movedEltContent = getFragmentAsString(vtdNav, vtdNav.getContentFragment(), false);
                    LOG.error("Soft moved element can not be updated.Skipping. Soft Moved Element Content: {},", movedEltContent, ex);
                }
            }
        }
    }

    private Ref getRefFromSoftMovedElt(VTDNav vtdNav, String attr) throws Exception {
        String id = null, href = null;
        int index = vtdNav.getAttrVal(XMLID);
        if (index != -1) {
            id = vtdNav.toString(index);
        }
        index = vtdNav.getAttrVal(attr);
        if (index != -1) {
            href = vtdNav.toString(index);
        }
        return new Ref(id, href);
    }

    private static byte[] constructListStructure(TableOfContentItemVO tableOfContentItemVO, User user, byte[] pointTag) {
        TableOfContentItemVO parentItem = tableOfContentItemVO.getParentItem();
        List<TableOfContentItemVO> childItems = parentItem.getChildItems();
        List<TableOfContentItemVO> childItemsOfType = constructChildListWithType(childItems, tableOfContentItemVO.getType().getName());

        if (tableOfContentItemVO.getId().equals(childItemsOfType.get(0).getId()) && shouldWrapWithList(parentItem)) {
            return wrapWithList(tableOfContentItemVO, user, childItemsOfType, pointTag);
        } else if (tableOfContentItemVO.getId().equals(childItemsOfType.get(childItemsOfType.size() - 1).getId()) && (parentItem.getVtdIndex() == null ||
                !parentItem.getType().getName().equals(LIST))) {
            return closeListTag(pointTag);
        } else {
            return pointTag;
        }
    }

    private static List<TableOfContentItemVO> constructChildListWithType(List<TableOfContentItemVO> childItems, String type) {
        List<TableOfContentItemVO> childItemsOfType = new ArrayList<>();
        for (TableOfContentItemVO child : childItems) {
            if (child.getType().getName().equals(type)) {
                childItemsOfType.add(child);
            }
        }
        return childItemsOfType;
    }

    private static byte[] closeListTag(byte[] pointTag) {
        ByteArrayBuilder composedList = new ByteArrayBuilder(pointTag);
        String closeListTag = "</" + LIST + ">";
        composedList.append(closeListTag.getBytes(UTF_8));
        return composedList.getContent();
    }

    private static byte[] wrapWithList(TableOfContentItemVO tableOfContentItemVO, User user, List<TableOfContentItemVO> childItems, byte[] pointTag) {
        // Build the list block
        String startTagStr = "<" + LIST + " xml:id=\"" + IdGenerator.generateId(LIST.substring(0, 3), 7) + "\">";
        byte[] listTag = updateSoftInfo(startTagStr.getBytes(UTF_8), SoftActionType.ADD, Boolean.TRUE, user,
                tableOfContentItemVO.getOriginAttr(), null, false, tableOfContentItemVO.getType());
        listTag = updateOriginAttribute(listTag, LEOS_ORIGIN_ATTR_CN);
        byte[] listCloseTag = childItems.size() == 1 ? LIST.getBytes(UTF_8) : null;
        return XmlHelper.buildTag(listTag, listCloseTag, pointTag);
    }

    private static boolean shouldWrapWithList(TableOfContentItemVO parentItem) {
        boolean wrapWithList = true;
        List<TableOfContentItemVO> childItems = parentItem.getChildItems();
        if (!childItems.isEmpty()) {
            switch (parentItem.getType().getName()) {
                case PARAGRAPH:
                    wrapWithList = !parentItem.containsType(LIST);
                    break;
                case LIST:
                    wrapWithList = false;
                    break;
            }
        }
        return wrapWithList;
    }

    private static ByteArrayBuilder buildParagraphContent(VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO, User user,
            ByteArrayBuilder tocItemContent)
            throws NavException, UnsupportedEncodingException {
        ByteArrayBuilder composedContent = new ByteArrayBuilder();
        composedContent.append(extractOrBuildNumElement(contentNavigator, tableOfContentItemVO));
        composedContent.append(convertToSubparagraph(tableOfContentItemVO, user));
        composedContent.append(tocItemContent.getContent());
        return composedContent;
    }

    private static byte[] convertToSubparagraph(TableOfContentItemVO tableOfContentItemVO, User user) throws UnsupportedEncodingException {
        byte[] subParaTag = XmlHelper.getSubParagraphTemplate(tableOfContentItemVO.getContent()).getBytes(UTF_8);
        return updateSoftInfo(subParaTag, SoftActionType.ADD, Boolean.TRUE, user, LEOS_ORIGIN_ATTR_CN, null, false, tableOfContentItemVO.getType());
    }

    private static ByteArrayBuilder buildPointContent(VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO, User user,
            ByteArrayBuilder tocItemContent)
            throws NavException, UnsupportedEncodingException {
        ByteArrayBuilder composedContent = new ByteArrayBuilder();
        composedContent.append(extractOrBuildNumElement(contentNavigator, tableOfContentItemVO));
        composedContent.append(convertToSubpoint(tableOfContentItemVO, user));
        composedContent.append(tocItemContent.getContent());
        return composedContent;
    }

    private static byte[] convertToSubpoint(TableOfContentItemVO tableOfContentItemVO, User user) throws UnsupportedEncodingException {
        byte[] subPointTag = XmlHelper.getSubpointTemplate(tableOfContentItemVO.getContent()).getBytes(UTF_8);
        return updateSoftInfo(subPointTag, SoftActionType.ADD, Boolean.TRUE, user, LEOS_ORIGIN_ATTR_CN, null, false, tableOfContentItemVO.getType());
    }

    private static byte[] extractSubElementContent(VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO)
            throws NavException, UnsupportedEncodingException {
        contentNavigator.recoverNode(tableOfContentItemVO.getVtdIndex());
        contentNavigator.toElement(VTDNav.FIRST_CHILD, CONTENT);
        return getTagWithContent(contentNavigator);
    }

    private static byte[] wrapWithPoint(TableOfContentItemVO tableOfContentItemVO, User user, byte[] pointContent) {
        // Build the point block
        String startTagStr = "<" + POINT + " xml:id=\"" + tableOfContentItemVO.getId() + "\">";
        byte[] pointTag = insertAffectedAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.isAffected());
        pointTag = updateOriginAttribute(pointTag, tableOfContentItemVO.getOriginAttr());
        return XmlHelper.buildTag(pointTag, POINT.getBytes(UTF_8), pointContent);
    }

    private static byte[] buildExistingXmlNode(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO,
            ByteArrayBuilder tocItemContent) throws NavException, UnsupportedEncodingException {
        TocItemType tocType = tableOfContentItemVO.getType();
        byte[] startTag = updateOriginAttribute(XmlTableOfContentHelper.getStartTagAndRemovePrefix(contentNavigator, tableOfContentItemVO), tableOfContentItemVO.getOriginAttr());
        if (tocType.equals(LegalTextMandateTocItemType.PREAMBLE) && tableOfContentItemVO.getPreambleFormula2TagIndex() != null) {
            tocItemContent.append(extractIndexedNonTocElements(contentNavigator, tableOfContentItemVO.getPreambleFormula2TagIndex()));
        } else if (!tocType.equals(LegalTextMandateTocItemType.RECITALS)) { // Recitals contains intro non TOC item and it already has been added
            tocItemContent.append(extractLevelNonTocItems(getTocItemType, contentNavigator, tableOfContentItemVO));
        }
        return startTag;
    }

    private static boolean skipPointContent(TableOfContentItemVO tableOfContentItemVO) {
        List<TableOfContentItemVO> childList = tableOfContentItemVO.getChildItems();
        if (childList != null && !childList.isEmpty()) {
            TableOfContentItemVO child = childList.get(0);
            if (child.getType().equals(LegalTextMandateTocItemType.POINT) || child.getType().equals(LegalTextMandateTocItemType.LIST)) {
                return true;
            }
        }
        return false;
    }

    private static boolean skipParagraphContent(TableOfContentItemVO tableOfContentItemVO) {
        List<TableOfContentItemVO> childList = tableOfContentItemVO.getChildItems();
        if (childList != null && !childList.isEmpty()) {
            for (TableOfContentItemVO child : childList) {
                if ((child.getVtdIndex() != null) && !child.isMovedOnEmptyParent() && child.getType().equals(LegalTextMandateTocItemType.SUBPARAGRAPH)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private static boolean isSingleSubElement(TableOfContentItemVO tableOfContentItemVO) {
        boolean isSingle = false;
        List<TableOfContentItemVO> childList = tableOfContentItemVO.getParentItem().getChildItems();
        if (childList != null && !childList.isEmpty()) {
            TableOfContentItemVO firstChild = childList.get(0);
            switch (childList.size()) {
                case 1:
                    // If only single subparagraph or subpoint is remaining in the paragraph
                    if ((firstChild.getVtdIndex() != null && !firstChild.isMovedOnEmptyParent()) &&
                            (firstChild.getType().equals(LegalTextMandateTocItemType.SUBPARAGRAPH) ||
                                    firstChild.getType().equals(LegalTextMandateTocItemType.SUBPOINT))) {
                        isSingle = true;
                    }
                    break;
                case 2:
                    // If point inside list is deleted and empty list remaining in the paragraph.
                    TableOfContentItemVO secondChild = childList.get(1);
                    if (secondChild.getVtdIndex() != null && secondChild.getType().equals(LegalTextMandateTocItemType.LIST) &&
                            secondChild.getChildItems().isEmpty()) {
                        isSingle = true;
                    }
                    break;
                default:
                    isSingle = false;
            }
        }
        return isSingle;
    }

    private static boolean isSoftDeletedOrMoved(TableOfContentItemVO tableOfContentItemVO) {
        return SoftActionType.DELETE.equals(tableOfContentItemVO.getSoftActionAttr()) ||
                SoftActionType.MOVE_FROM.equals(tableOfContentItemVO.getSoftActionAttr()) ||
                SoftActionType.MOVE_TO.equals(tableOfContentItemVO.getSoftActionAttr());
    }

    private static boolean isEmptyElement(TableOfContentItemVO tableOfContentItemVO) {
        List<TableOfContentItemVO> childList = tableOfContentItemVO.getChildItems();
        return childList == null || childList.isEmpty();
    }

    private void updateNewElements(XMLModifier xmlModifier, String elementTagName, String subElementTagName) throws Exception {
        VTDNav vtdNav = xmlModifier.outputAndReparse();
        xmlModifier.bind(vtdNav);
        vtdNav.toElement(VTDNav.ROOT);

        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(elementTagName);

        while (autoPilot.iterate()) {
            int currentIndex = vtdNav.getCurrentIndex();
            String elementId = vtdNav.toString(vtdNav.getAttrVal(XMLID));
            String elementOrigin = (vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) != -1) ? vtdNav.toString(vtdNav.getAttrVal(LEOS_ORIGIN_ATTR)) : LEOS_ORIGIN_ATTR_CN;
            if (elementOrigin.equals(LEOS_ORIGIN_ATTR_CN) &&
                    ((vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) == -1) || (vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR) == -1))) {
                xmlModifier.insertAttribute((vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) == -1 ? generateOriginInfo(LEOS_ORIGIN_ATTR_CN) : EMPTY_STRING) +
                        (vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR) == -1 ? generateSoftInfo(SoftActionType.ADD) : EMPTY_STRING));
            }
            if ((subElementTagName != null) && (vtdNav.toElement(VTDNav.FIRST_CHILD, subElementTagName))) {
                boolean isFirstSubElement = true;
                do {
                    if (isFirstSubElement && elementOrigin.equals(LEOS_ORIGIN_ATTR_EC) && (vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) == -1)) {
                        if (vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR) == -1) {
                            xmlModifier.insertAttribute(generateOriginInfo(LEOS_ORIGIN_ATTR_EC) + generateSoftInfo(SoftActionType.TRANSFORM));
                        } else {
                            xmlModifier.insertAttribute(generateOriginInfo(LEOS_ORIGIN_ATTR_EC));
                            xmlModifier.updateToken(vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR), SoftActionType.TRANSFORM.getSoftAction());
                        }
                        xmlModifier.updateToken(vtdNav.getAttrVal(XMLID), SOFT_TRANSFORM_PLACEHOLDER_ID_PREFIX + elementId);
                    } else {
                        String subElementOrigin = (vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) != -1) ? vtdNav.toString(vtdNav.getAttrVal(LEOS_ORIGIN_ATTR))
                                : LEOS_ORIGIN_ATTR_CN;
                        if (subElementOrigin.equals(LEOS_ORIGIN_ATTR_CN) &&
                                ((vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) == -1) || (vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR) == -1))) {
                            xmlModifier.insertAttribute((vtdNav.getAttrVal(LEOS_ORIGIN_ATTR) == -1 ? generateOriginInfo(LEOS_ORIGIN_ATTR_CN) : EMPTY_STRING) +
                                    (vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR) == -1 ? generateSoftInfo(SoftActionType.ADD) : EMPTY_STRING));
                        }
                    }
                    isFirstSubElement = false;
                } while (vtdNav.toElement(VTDNav.NEXT_SIBLING, subElementTagName));
                vtdNav.recoverNode(currentIndex); // Remove attribute LEOS_SOFT_TRANS_FROM from parent
                removeAttribute(vtdNav, xmlModifier, LEOS_SOFT_TRANS_FROM);
            }
            vtdNav.recoverNode(currentIndex);
        }
    }

    private String generateOriginInfo(String origin) {
        return new StringBuffer(" ").append(LEOS_ORIGIN_ATTR).append("=\"").append(origin).append("\"").toString();
    }

    private String generateSoftInfo(SoftActionType softAction) throws DatatypeConfigurationException {
        return new StringBuffer(" ").append(LEOS_SOFT_ACTION_ATTR).append("=\"").append(softAction.getSoftAction()).append("\"")
                .append(" ").append(LEOS_SOFT_USER_ATTR).append("=\"").append(getUserName()).append("\"")
                .append(" ").append(LEOS_SOFT_DATE_ATTR).append("=\"").append(getXMLFormatDate()).append("\"").toString();
    }

    private String getUserName() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    public String getXMLFormatDate() throws DatatypeConfigurationException {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat();
    }

    @Override
    public String[] getSplittedElement(byte[] xmlContent, String tagName, String idAttributeValue) {
        String[] element = null;
        if (Arrays.asList(SUBPARAGRAPH, SUBPOINT).contains(tagName)) {
            element = getSiblingElement(xmlContent, tagName, idAttributeValue, Collections.emptyList(), false);
        } else {
            element = getChildElement(xmlContent, tagName, idAttributeValue, Arrays.asList(SUBPARAGRAPH, SUBPOINT), 2);
        }
        return element;
    }

    @Override
    public String[] getMergeOnElement(byte[] xmlContent, String content, String tagName, String idAttributeValue) {
        Map<String, String> attributes = getElementAttributesByPath(content.getBytes(UTF_8), "/" + tagName, false);
        if (isSoftDeletedOrMovedTo(attributes) || !isPContent(content, tagName)) {
            return null;
        }

        String[] mergeOnElement = getSiblingElement(xmlContent, tagName, idAttributeValue, Arrays.asList(tagName, LIST), true);
        if ((mergeOnElement == null) || ((mergeOnElement != null) &&
                (isSoftDeletedOrMovedTo(getElementAttributesByPath(mergeOnElement[2].getBytes(UTF_8), "/" + mergeOnElement[1], false)) ||
                        !isPContent(mergeOnElement[2], mergeOnElement[1])))) {
            return null;
        }

        if (!isProposalElement(attributes)) {
            String[] parentElement = getParentElement(xmlContent, mergeOnElement[1], mergeOnElement[0]);
            if (Arrays.asList(PARAGRAPH, POINT).contains(parentElement[1]) && getChildElement(xmlContent, parentElement[1], parentElement[0], Arrays.asList(SUBPARAGRAPH, SUBPOINT, LIST), 3) == null) {
                return parentElement;
            }
        }

        return mergeOnElement;
    }

    @Override
    public byte[] mergeElement(byte[] xmlContent, String content, String tagName, String idAttributeValue) {
        String[] mergeOnElement = getSiblingElement(xmlContent, tagName, idAttributeValue, Arrays.asList(tagName, LIST), true);
        String contentFragment = getElementContentFragmentByPath(content.getBytes(UTF_8), "/" + tagName + "/content/p", false);
        String contentFragmentMergeOn = getElementContentFragmentByPath(mergeOnElement[2].getBytes(UTF_8), "/" + mergeOnElement[1] + "/content/p", false);
        byte[] updatedXmlContent = replaceElementByTagNameAndId(xmlContent, mergeOnElement[2].replace(contentFragmentMergeOn, contentFragmentMergeOn + " " + contentFragment), mergeOnElement[1], mergeOnElement[0]);

        Map<String, String> attributes = getElementAttributesByPath(content.getBytes(UTF_8), "/" + tagName, false);
        if (isProposalElement(attributes) && !isSoftMovedFrom(attributes)) {
            updatedXmlContent = replaceElementByTagNameAndId(updatedXmlContent, softDeleteElement(content), tagName, idAttributeValue);
        } else {
            updatedXmlContent = deleteElementByTagNameAndId(updatedXmlContent, tagName, idAttributeValue);
            if (isSoftMovedFrom(attributes)) {
                String[] softMovedToElement = getElementById(updatedXmlContent, getSoftMovedFromAttribute(attributes));
                updatedXmlContent = replaceElementByTagNameAndId(updatedXmlContent, softDeleteElement(softMovedToElement[2]), softMovedToElement[1], softMovedToElement[0]);
            }
            String[] parentElement = getParentElement(updatedXmlContent, mergeOnElement[1], mergeOnElement[0]);
            if (Arrays.asList(PARAGRAPH, POINT).contains(parentElement[1]) && getChildElement(updatedXmlContent, parentElement[1], parentElement[0], Arrays.asList(SUBPARAGRAPH, SUBPOINT, LIST), 2) == null) {
                Map<String, String> mergedElementAttributes = getElementAttributesByPath(parentElement[2].getBytes(UTF_8), "/" + parentElement[1] + "/" + mergeOnElement[1], false);
                if (isSoftMovedFrom(mergedElementAttributes)) {
                    String[] softMovedToMergedElement = getElementById(updatedXmlContent, getSoftMovedFromAttribute(mergedElementAttributes));
                    updatedXmlContent = replaceElementByTagNameAndId(updatedXmlContent, softDeleteElement(softMovedToMergedElement[2]), softMovedToMergedElement[1], softMovedToMergedElement[0]);
                }
                String mergedElementFragment = getElementFragmentByPath(parentElement[2].getBytes(UTF_8), "/" + parentElement[1] + "/" + mergeOnElement[1], false);
                String mergedContentFragment = getElementFragmentByPath(parentElement[2].getBytes(UTF_8), "/" + parentElement[1] + "/" + mergeOnElement[1] + "/content", false);
                String parentElementFragment = parentElement[2].replace(mergedElementFragment, mergedContentFragment);
                updatedXmlContent = replaceElementByTagNameAndId(updatedXmlContent, new String(updateSoftTransFromAttribute(parentElementFragment.getBytes(UTF_8), mergeOnElement[0]), UTF_8), parentElement[1], parentElement[0]);
            } else if (Arrays.asList(PARAGRAPH, POINT).contains(mergeOnElement[1])) {
                updatedXmlContent = insertAffectedAttributeIntoParentElements(updatedXmlContent, mergeOnElement[0]);
            }
        }

        return updatedXmlContent;
    }

    private boolean isSoftDeletedOrMovedTo(Map<String, String> attributes) {
        return ((attributes.get(LEOS_SOFT_ACTION_ATTR) != null) && (attributes.get(LEOS_SOFT_ACTION_ATTR).equals(SoftActionType.DELETE.getSoftAction()) ||
                attributes.get(LEOS_SOFT_ACTION_ATTR).equals(SoftActionType.MOVE_TO.getSoftAction())));
    }

    private boolean isSoftMovedFrom(Map<String, String> attributes) {
        return ((attributes.get(LEOS_SOFT_ACTION_ATTR) != null) && attributes.get(LEOS_SOFT_ACTION_ATTR).equals(SoftActionType.MOVE_FROM.getSoftAction()));
    }

    private boolean isProposalElement(Map<String, String> attributes) {
        return ((attributes.get(LEOS_ORIGIN_ATTR) != null) && attributes.get(LEOS_ORIGIN_ATTR).equals(LEOS_ORIGIN_ATTR_EC));
    }

    private boolean isPContent(String content, String tagName) {
        return getElementContentFragmentByPath(content.getBytes(UTF_8), "/" + tagName + "/content/p", false) != null;
    }

    private String getSoftMovedFromAttribute(Map<String, String> attributes) {
        return (attributes.get(LEOS_SOFT_MOVE_FROM) != null) ? attributes.get(LEOS_SOFT_MOVE_FROM) : null;
    }

    private String softDeleteElement(String content) {
        StringBuilder tagStr = new StringBuilder(content);

        updateAttributeValue(tagStr, LEOS_EDITABLE_ATTR, Boolean.FALSE.toString());
        updateAttributeValue(tagStr, LEOS_DELETABLE_ATTR, Boolean.FALSE.toString());
        updateSoftAttributes(SoftActionType.DELETE, tagStr);

        removeAttribute(tagStr, LEOS_SOFT_MOVED_LABEL_ATTR);
        removeAttribute(tagStr, LEOS_SOFT_MOVE_TO);

        return new String(updateXMLIDAttributesInElementContent(new ByteArrayBuilder(tagStr.toString().getBytes(UTF_8)),
                SOFT_DELETE_PLACEHOLDER_ID_PREFIX, true).getContent(), UTF_8);
    }

    private byte[] insertAffectedAttributeIntoParentElements(byte[] xmlContent, String idAttributeValue) {
        try {
            VTDNav vtdNav = setupVTDNav(xmlContent);
            XMLModifier xmlModifier = new XMLModifier(vtdNav);
            xmlModifier.bind(vtdNav);
            vtdNav.toElement(VTDNav.ROOT);

            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");
            autoPilot.selectXPath("//*[@xml:id = '" + idAttributeValue + "']");
            if ((autoPilot.evalXPath() != -1) && vtdNav.toElement(VTDNav.PARENT)) {
                do {
                    if (ELEMENTS_TO_BE_PROCESSED_FOR_NUMBERING.contains(vtdNav.toString(vtdNav.getCurrentIndex()))) {
                        if (vtdNav.getAttrVal(LEOS_AFFECTED_ATTR) == -1) {
                            xmlModifier.insertAttribute(new StringBuffer(" ").append(LEOS_AFFECTED_ATTR).append("=\"true\"").toString());
                        } else {
                            xmlModifier.updateToken(vtdNav.getAttrVal(LEOS_AFFECTED_ATTR), "true");
                        }
                    }
                }
                while (!vtdNav.toString(vtdNav.getCurrentIndex()).equals(ARTICLE) && vtdNav.toElement(VTDNav.PARENT));
            }
            return toByteArray(xmlModifier);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the insertAffectedAttribute operation", e);
        }
    }

    private byte[] updateSoftActionOnNumElement(String content,SoftActionType softAction, String setToggledToNum) {
        StringBuilder tagStr = new StringBuilder(content);

        if (softAction != null) {
            updateSoftAttributes(softAction, tagStr);
            if(SoftActionType.DELETE.equals(softAction)){
                tagStr = new StringBuilder(new String(updateXMLIDAttributesInElementContent(new ByteArrayBuilder(tagStr.toString().getBytes(UTF_8)), SOFT_DELETE_PLACEHOLDER_ID_PREFIX, true).getContent(), UTF_8));
            }
        } else if (setToggledToNum != null) {
            removeSoftAttributes(tagStr);
        }

        if(TOGGLED_TO_NUM.equals(setToggledToNum)){
            updateAttributeValue(tagStr, setToggledToNum, Boolean.TRUE.toString());
        }

        return new String(tagStr).getBytes(UTF_8);
    }

    private static void removeSoftAttributes(StringBuilder tagStr) {
        removeAttribute(tagStr, LEOS_SOFT_ACTION_ATTR);
        removeAttribute(tagStr, LEOS_SOFT_ACTION_ROOT_ATTR);
        removeAttribute(tagStr, LEOS_SOFT_USER_ATTR);
        removeAttribute(tagStr, LEOS_SOFT_DATE_ATTR);
        restoreOldId(tagStr);
    }

    private void updateSoftAttributes(SoftActionType softAction, StringBuilder tagStr) {
        updateAttributeValue(tagStr, LEOS_SOFT_ACTION_ATTR, softAction.getSoftAction());
        updateAttributeValue(tagStr, LEOS_SOFT_ACTION_ROOT_ATTR, Boolean.TRUE.toString());
        updateAttributeValue(tagStr, LEOS_SOFT_USER_ATTR, getUserName());
        try {
            updateAttributeValue(tagStr, LEOS_SOFT_DATE_ATTR, getXMLFormatDate());
        } catch (DatatypeConfigurationException e) {
            updateAttributeValue(tagStr, LEOS_SOFT_DATE_ATTR, null);
        }
    }

}
