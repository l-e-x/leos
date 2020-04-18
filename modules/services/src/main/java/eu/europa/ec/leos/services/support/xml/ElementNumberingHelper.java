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
import com.ximpleware.ModifyException;
import com.ximpleware.NavException;
import com.ximpleware.TranscodeException;
import com.ximpleware.VTDNav;
import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.NumberingType;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_AFFECTED_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_DEPTH_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_ORIGIN_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_SOFT_ACTION_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.TOGGLED_TO_NUM;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.XMLID;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.buildNumElement;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.removeAttribute;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.toByteArray;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.EC;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.INDENT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEVEL;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LIST;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.NUM;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.POINT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.RECITAL;
import static eu.europa.ec.leos.vo.toc.TocItemUtils.getNumberingByName;
import static eu.europa.ec.leos.vo.toc.TocItemUtils.getNumberingConfig;
import static eu.europa.ec.leos.vo.toc.TocItemUtils.getNumberingTypeByDepth;
import static eu.europa.ec.leos.vo.toc.TocItemUtils.getTocItemByName;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.nCopies;

@Component
public class ElementNumberingHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ElementNumberingHelper.class);
    private boolean isDefaultEditable = false;
    private List<TocItem> tocItems;
    private List<NumberingConfig> numberingConfigs;
    private MessageHelper messageHelper;
    private Provider<StructureContext> structureContextProvider;
    
    @Autowired
    public ElementNumberingHelper(MessageHelper messageHelper, Provider<StructureContext> structureContextProvider) {
        this.messageHelper = messageHelper;
        this.structureContextProvider = structureContextProvider;
    }
    
    public byte[] renumberElements(String element, String xmlContent, boolean namespaceEnabled) throws Exception{
        return renumberElements(element, xmlContent.getBytes(UTF_8), namespaceEnabled);
    }
    
    public byte[] renumberElements(String element, byte[] xmlContent, boolean namespaceEnabled) throws Exception {
        VTDNav vtdNav = setupVTDNav(xmlContent, namespaceEnabled);
        XMLModifier xmlModifier = new XMLModifier(vtdNav);
        tocItems = structureContextProvider.get().getTocItems();
        numberingConfigs = structureContextProvider.get().getNumberingConfigs();
        return elementNumberProcess(element, vtdNav, xmlModifier);
    }
    
    public byte[] renumberElements(String element, byte[] xmlContent, MessageHelper messageHelper) {
        tocItems = structureContextProvider.get().getTocItems();
        numberingConfigs = structureContextProvider.get().getNumberingConfigs();
        
        final TocItem tocItem = getTocItemByName(tocItems, element);
        final NumberingConfig numberingConfig = getNumberingByName(numberingConfigs, tocItem.getNumberingType());
        final NumberingType numberingType = numberingConfig.getType();
        final String prefix = numberingConfig.getPrefix();
        final String suffix = numberingConfig.getSuffix();
        
        final VTDNav vtdNav = setupVTDNav(xmlContent, true);

        long number = -1L;
        if (numberingType != null && !numberingType.value().isEmpty()) {
            number = 1L;
        }
        byte[] updatedElement;
        XMLModifier xmlModifier = null;
        try {
            xmlModifier = new XMLModifier(vtdNav);
            AutoPilot autoPilot = new AutoPilot(vtdNav);
            autoPilot.declareXPathNameSpace("leos", "urn:eu:europa:ec:leos");// required
            autoPilot.selectElement(element);
            byte[] elementeNum = null;
            LevelVO lastLevelVo = null;

            while (autoPilot.iterate()) {
                switch(element) {
                    case ARTICLE:
                        String articleNum = prefix + getElementNumeral(number++, numberingType) + suffix;
                        elementeNum = messageHelper.getMessage("legaltext.article.num", articleNum).getBytes(UTF_8);
                        break;
                    case RECITAL:
                        String recitalNum = prefix + getElementNumeral(number++, numberingType) + suffix;
                        elementeNum = recitalNum.getBytes(UTF_8);
                        break;
                    case LEVEL:

                        //1.set previous level
                        int index = vtdNav.getCurrentIndex();
                        LevelVO prevLevelVo = lastLevelVo;
                        vtdNav.recoverNode(index);

                        //2.set current level
                        LevelVO currLevelVo = getLevelVo(vtdNav, xmlModifier);
                        vtdNav.recoverNode(index);

                        //3.get level num of current level
                        String levelNum = prefix + getLevelNumeral(prevLevelVo, currLevelVo, vtdNav, xmlModifier, numberingType) + suffix;

                        //4.update previous level by current level
                        lastLevelVo = currLevelVo;
                        lastLevelVo.setLevelNum(levelNum);

                        elementeNum = levelNum.getBytes(UTF_8);
                        break;
                    default:
                        break;
                }
                int currentIndex = vtdNav.getCurrentIndex();
                updatedElement = buildNumElement(vtdNav, xmlModifier, elementeNum);
                vtdNav.recoverNode(currentIndex);
                xmlModifier.insertAfterHead(updatedElement);
            }
            if(element.equalsIgnoreCase(LEVEL)) {
                removeDepthAttribute(xmlModifier); // cleanup leos depth attr
            }
            return toByteArray(xmlModifier);
        } catch (Exception e) {
            throw new RuntimeException("Unable to perform the renumberElement operation", e);
        }
    }
    
    private String getLevelNumeral(LevelVO prevLevelVo,LevelVO currLevelVo, VTDNav vtdNav, XMLModifier xmlModifier, NumberingType numberingType) throws Exception {
        int index = vtdNav.getCurrentIndex();
        String levelNum = "";
        if (prevLevelVo != null) {
            int prevLevelDepth = prevLevelVo.getLevelDepth();
            String prevLevelNum = prevLevelVo.getLevelNum();
            int currLevelDepth = currLevelVo.getLevelDepth();
            if(prevLevelDepth == currLevelDepth) {
                levelNum = getNextLevelNum(prevLevelDepth, prevLevelNum);
            } else if(currLevelDepth > prevLevelDepth) {
                levelNum = prevLevelNum.concat("1");
            } else if(currLevelDepth < prevLevelDepth) {
                levelNum = getNextNumForDepth(currLevelDepth, prevLevelNum);
            }
        } else { // first level element
            levelNum = getElementNumeral(1L, numberingType);
        }
        vtdNav.recoverNode(index);
        return levelNum;
    }

    private LevelVO getLevelVo(VTDNav vtdNav, XMLModifier xmlModifier) throws Exception {
        LevelVO levelVo = new LevelVO();
        int currentIndex = vtdNav.getCurrentIndex();
        int idAttrIndex = vtdNav.getAttrVal(XMLID);
        String elementId = vtdNav.toNormalizedString(idAttrIndex);
        
        byte[] xmlContent = toByteArray(xmlModifier);
        VTDNav currentNavigator = setupVTDNav(xmlContent);
        AutoPilot autoPilot = new AutoPilot(currentNavigator);
        autoPilot.declareXPathNameSpace("xml", "http://www.w3.org/XML/1998/namespace");// required
        autoPilot.selectXPath("//*[@xml:id = '" + elementId + "']");
        
        if(autoPilot.evalXPath() != -1) {
            int attributeIndex = currentNavigator.getAttrVal(LEOS_DEPTH_ATTR);
            if (attributeIndex != -1) {
                String depth = currentNavigator.toNormalizedString(attributeIndex);
                levelVo.setLevelDepth(Integer.parseInt(depth));
                if(currentNavigator.toElement(VTDNav.FIRST_CHILD, NUM)) {
                    long contentFragment = currentNavigator.getContentFragment();
                    String elementNumber = new String(currentNavigator.getXML().getBytes((int) contentFragment, (int) (contentFragment >> 32)));
                    levelVo.setLevelNum(elementNumber);
                }
            }
        }
        vtdNav.recoverNode(currentIndex);
        return levelVo;
    }
    
    private String getNextLevelNum(int levelDepth, String levelNum) {
        String[] numArr = getNextNum(levelDepth, levelNum);
        return String.join(".", numArr);
    }

    private String getNextNumForDepth(int levelDepth, String levelNum) {
        String[] numArr = getNextNum(levelDepth, levelNum);
        String[] copyArr = Arrays.copyOfRange(numArr, 0, levelDepth);
        return String.join(".", copyArr);
    }
    
    private String[] getNextNum(int levelDepth, String levelNum) {
        String[] numArr = StringUtils.split(levelNum, ".");
        String numStr = numArr[levelDepth - 1];
        int currNum = Integer.parseInt(numStr) + 1;
        numArr[levelDepth - 1] = Integer.toString(currNum);
        return numArr;
    }
    
    private byte[] elementNumberProcess(String element, VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, ModifyException, TranscodeException, IOException {
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(element);
        char alphaNumber = 'a';
        List<Integer> indexList = new ArrayList<>();
        boolean foundProposalElement = false;
        int parentIndex = getParentIndex(vtdNav, element);
        
        if (!isProposalElement(vtdNav) && (Arrays.asList(PARAGRAPH,POINT,INDENT).contains(element))) {
            // Default numbering applied to Mandate elements (no EC elements present)
            while (autoPilot.iterate()) {
                updatePointAndIndentNumbersDefault(vtdNav, xmlModifier, numberingConfigs);
                if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                    if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                        if (checkFirstChildType(vtdNav, INDENT)) {
                            elementNumberProcess(INDENT, vtdNav, xmlModifier);
                        } else {
                            elementNumberProcess(POINT, vtdNav, xmlModifier);
                        }
                    }
                }
            }
        } else {
            // EC elements, means we have mixed elements
            // Mandate rules applied when we have mixed elements CN and EC
            String elementNumber = "";
            while (autoPilot.iterate()) {
                int tempIndex = getParentIndex(vtdNav, element);
                if (tempIndex != parentIndex && (Arrays.asList(PARAGRAPH,POINT,INDENT).contains(element))) {
                    continue;
                }
                final TocItem tocItem = getTocItemByName(tocItems, element);
                final NumberingConfig numberingConfig = getNumberingByName(numberingConfigs, tocItem.getNumberingType());
                if (foundProposalElement || isElementOriginEC(vtdNav)) {
                    if (isElementOriginEC(vtdNav)) {
                        foundProposalElement = true; // once true, we now we are in the positive side, from now on.
                        alphaNumber = 'a';
                        //set the series number for manual numbering of Articles
                        int index = vtdNav.getCurrentIndex();
                        if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                            long contentFragment = vtdNav.getContentFragment();
                            elementNumber = new String(vtdNav.getXML().getBytes((int) contentFragment, (int) (contentFragment >> 32)));
                        }
                        vtdNav.recoverNode(index);
                        //Check if elements added to proposal elements
                        if (hasAffectedAttribute(vtdNav, xmlModifier) || hasNumToggledAttribute(vtdNav, xmlModifier)) {
                            switch (element) {
                                case ARTICLE:
                                    elementNumberProcess(PARAGRAPH, vtdNav, xmlModifier);
                                    break;
                                case PARAGRAPH:
                                case POINT:
                                case INDENT:
                                    goOnWithChildren(vtdNav, xmlModifier);
                                    break;
                            }
                        }
                    } else { //adding alpha number series to added article
                        switch (element) {
                            case ARTICLE:
                                alphaNumber = updateArticleNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, false);
                                if (hasAffectedAttribute(vtdNav, xmlModifier) || hasNumToggledAttribute(vtdNav, xmlModifier)) {
                                    if (!containsProposalElement(PARAGRAPH, vtdNav)) {
                                        updateParagraphNumbersDefault(vtdNav, xmlModifier, tocItems);
                                    }
                                    elementNumberProcess(PARAGRAPH, vtdNav, xmlModifier);
                                }
                                break;
                            case PARAGRAPH:
                                alphaNumber = updateParagraphNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, numberingConfig, false);
                                if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                                    goOnWithChildren(vtdNav, xmlModifier);
                                }
                                break;
                            case POINT:
                            case INDENT:
                                alphaNumber = updatePointNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, false);
                                if (hasAffectedAttribute(vtdNav, xmlModifier)) {
                                    goOnWithChildren(vtdNav, xmlModifier);
                                }
                                break;
                            case RECITAL:
                                alphaNumber = updateRecitalNumbers(elementNumber, vtdNav, xmlModifier, alphaNumber, numberingConfig, false);
                                break;
                        }
                    }
                } else {
                    // if no EC element present until now, and the current iterated is not an EC, it means the current iterated is in the negative side, add it to the list.
                    indexList.add(vtdNav.getCurrentIndex());
                }
            }
            
            // do the calculation for the the negative elements found in the previous iteration
            calculateNegativeSideNumbers(element, vtdNav, xmlModifier, alphaNumber, indexList);
        }
        return toByteArray(xmlModifier);
    }

    private void goOnWithChildren(VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, ModifyException, TranscodeException, IOException {
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
            updateMandatePointsNumbering(vtdNav, xmlModifier, numberingConfigs);
            if(checkFirstChildType(vtdNav, INDENT)){
                elementNumberProcess(INDENT, vtdNav, xmlModifier);
            }else if(checkFirstChildType(vtdNav, POINT)){
                elementNumberProcess(POINT, vtdNav, xmlModifier);
            }
        }
    }


    private void calculateNegativeSideNumbers(String element, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, List<Integer> indexList) throws NavException, ModifyException, IOException, TranscodeException {
        //-ve numbering
        long negativeNumber = -(indexList.size());

        for (int negativeNumIndex : indexList) {
            vtdNav.recoverNode(negativeNumIndex);
            final TocItem tocItem = getTocItemByName(tocItems, element);
            final NumberingConfig numberingConfig = getNumberingByName(numberingConfigs, tocItem.getNumberingType());
            switch (element) {
                case ARTICLE:
                    final String articleName = messageHelper.getMessage("toc.item.type." + tocItem.getAknTag().value().toLowerCase()) + " ";
                    updateArticleNumbers(articleName + negativeNumber++, vtdNav, xmlModifier, Character.MIN_VALUE, true);
                    if (hasAffectedAttribute(vtdNav, xmlModifier) || hasNumToggledAttribute(vtdNav, xmlModifier)) {
                        if (!containsProposalElement(PARAGRAPH, vtdNav)) {
                            updateParagraphNumbersDefault(vtdNav, xmlModifier, tocItems);
                        }
                        elementNumberProcess(PARAGRAPH, vtdNav, xmlModifier);
                    }
                    break;
                case PARAGRAPH:
                    updateParagraphNumbers(""+negativeNumber++, vtdNav, xmlModifier, Character.MIN_VALUE, numberingConfig, true);
                    pointNumProcessing(vtdNav, xmlModifier);
                    break;
                case POINT:
                case INDENT:
                    updatePointNumbers(""+negativeNumber++, vtdNav, xmlModifier, alphaNumber, true);
                    pointNumProcessing(vtdNav, xmlModifier);
                    break;
                case RECITAL:
                    updateRecitalNumbers(""+negativeNumber++, vtdNav, xmlModifier, Character.MIN_VALUE, numberingConfig, true);
                    break;
            }
        }
    }
    
    private void pointNumProcessing(VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, ModifyException, TranscodeException, IOException {
        if (hasAffectedAttribute(vtdNav, xmlModifier)) {
            if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
                int currentIndex = vtdNav.getCurrentIndex();
                String elementType = checkFirstChildType(vtdNav, INDENT) ? INDENT : POINT;
                if (!containsProposalElement(elementType, vtdNav)) {
                    renumberPointsOrIndents(vtdNav, xmlModifier, numberingConfigs, elementType);
                }
                vtdNav.recoverNode(currentIndex);
                elementNumberProcess(elementType, vtdNav, xmlModifier);
            }
        }
    }

    private void updateMandatePointsNumbering(VTDNav vtdNav, XMLModifier xmlModifier, List<NumberingConfig> numberingConfigs)
            throws NavException, ModifyException {
        if (!isProposalElement(vtdNav)) {
            int currentIndex = vtdNav.getCurrentIndex();
            renumberPointsOrIndents(vtdNav, xmlModifier, numberingConfigs, checkFirstChildType(vtdNav, INDENT) ? INDENT : POINT);
            vtdNav.recoverNode(currentIndex);
        }
    }
    
    private boolean isProposalElement(VTDNav vtdNav) throws NavException {
        boolean isProposalElement = false;
        int attributeIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
        if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(EC)) {
            isProposalElement = true;
        }
        return isProposalElement;
    }

    private boolean containsProposalElement(String element, VTDNav vtdNav) throws NavException {
        boolean containsProposalElement = false;
        int currentIndex = vtdNav.getCurrentIndex();
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(element);
        while (!containsProposalElement && autoPilot.iterate()) {
            containsProposalElement = isProposalElement(vtdNav);
        }
        vtdNav.recoverNode(currentIndex);
        return containsProposalElement;
    }

    /**
     * Default numbering which do only the incremental. Ex: 1, 2,3 etc, or a, b, c, etc.
     */
    private byte[] updatePointAndIndentNumbersDefault(VTDNav vtdNav, XMLModifier xmlModifier, List<NumberingConfig> numberingConfigs)
            throws NavException, ModifyException, IOException, TranscodeException {
        LOG.debug("points default numbering");
        int currentIndex = vtdNav.getCurrentIndex();
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, LIST)) {
            String elementType = checkFirstChildType(vtdNav, INDENT) ? INDENT : POINT;
            if (!containsProposalElement(elementType, vtdNav)) {
                renumberPointsOrIndents(vtdNav, xmlModifier, numberingConfigs, elementType);
            }
        }
        vtdNav.recoverNode(currentIndex);
        return toByteArray(xmlModifier);
    }

    /**
     * Based on the depth we apply the rules for numbering
     */
    private void renumberPointsOrIndents(VTDNav vtdNav, XMLModifier xmlModifier, List<NumberingConfig> numberingConfigs, String point) throws NavException, ModifyException {
        long number = 1L;
        int parentIndex = getParentIndex(vtdNav, point);
        NumberingConfig numberingConfig = getNumberingConfigForMultilevel(vtdNav, numberingConfigs);
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, point)) {
            do {
                //if next sibling is not child of the same parent -> exit
                int tempIndex = getParentIndex(vtdNav, point);
                if (tempIndex != parentIndex) {
                    break;
                }
                // check if it has num element
                final String num;
                switch (point){
                    case POINT:
                        num = getPointNumber((int) number++, numberingConfig.getType());
                        break;
                    case INDENT:
                        num = numberingConfig.getSequence();
                        break;
                    default:
                        num = ""; //cannot ever happen
                }

                byte[] elementeNum = (numberingConfig.getPrefix() + num + numberingConfig.getSuffix()).getBytes(UTF_8);
                int index = vtdNav.getCurrentIndex();
                elementeNum = buildNumElement(vtdNav, xmlModifier, elementeNum);
                vtdNav.recoverNode(index);
                xmlModifier.insertAfterHead(elementeNum);
            }
            while (vtdNav.toElement(VTDNav.NEXT_SIBLING));
        }
    }

    private int getParentIndex(VTDNav vtdNav, String element) throws NavException {
        int index = vtdNav.getCurrentIndex();
        AutoPilot ap = new AutoPilot(vtdNav);
        ap.selectElement(element);
        int parentIndex = 0;
        while (ap.iterate()) {
            if (vtdNav.toElement(VTDNav.PARENT)) {
                parentIndex = vtdNav.getCurrentIndex();
                break;
            }
        }
        vtdNav.recoverNode(index);
        return parentIndex;
    }

    private boolean checkFirstChildType(VTDNav vtdNav, String type) throws NavException {
        boolean result = false;
        if (vtdNav.toElement(VTDNav.FIRST_CHILD)) {
            if (vtdNav.toRawString(vtdNav.getCurrentIndex()).equalsIgnoreCase(type)) {
                result = true;
            }
            vtdNav.toElement(VTDNav.PARENT);
        }
        return result;
    }
    
    /**
     * Based on the depth of the actual element we get the appropriate numberingConfig.
     * POINT has numberingType goOnWithChildren MULTILEVEL, but the indent points have different numberingType based on the depth.
     * Example of point structure:
     * 1.
     *  (a) first depth of the list
     *      (1) second depth of the list
     *          (i) thirst depth of the list
     *              - fourth depth of the list
     *
     * <tocItem>
     *     <aknTag>point</aknTag>
     *     <numberingType>MULTILEVEL</numberingType>
     * </tocItem>
     *
     * <numberingConfig>
     *     <type>MULTILEVEL</type>
     *     <levels>
     *         <level>
     *             <depth>1</depth>
     *             <numberingType>ALPHA</numberingType>
     *         </level>
     *         <level>
     *             <depth>2</depth>
     *             <numberingType>ARABIC-PARENTHESIS</numberingType>
     *         </level>
     *         <level>
     *             <depth>3</depth>
     *             <numberingType>ROMAN-LOWER</numberingType>
     *         </level>
     *         <level>
     *             <depth>4</depth>
     *             <numberingType>INDENT</numberingType>
     *         </level>
     *     </levels>
     * </numberingConfig>
     *
     * If the point is not MULTILEVEL then all the levels will have same numbering configuration
     */
    private NumberingConfig getNumberingConfigForMultilevel(VTDNav vtdNav, List<NumberingConfig> numberingConfigs) throws NavException {
        int index = vtdNav.getCurrentIndex();
        int pointDepth = VTDUtils.getPointDepth(vtdNav,1);
        final NumberingConfig numberingConfig;
        final int calculatedDepth = pointDepth;
        NumberingType numberingType = getNumberingTypeByDepth(getNumberingConfig(numberingConfigs, NumberingType.MULTILEVEL), calculatedDepth);
        numberingConfig = getNumberingConfig(numberingConfigs, numberingType);
        vtdNav.recoverNode(index);
        return numberingConfig;
    }

    private byte[] updateParagraphNumbersDefault(VTDNav vtdNav, XMLModifier xmlModifier, List<TocItem> tocItems)
            throws NavException, ModifyException, IOException, TranscodeException {
        int currentIndex = vtdNav.getCurrentIndex();
        NumberingType numberingType = getTocItemByName(tocItems, PARAGRAPH).getNumberingType();
        NumberingConfig numberingConfig = getNumberingConfig(numberingConfigs, numberingType);
        long number = 1L;
        AutoPilot autoPilot = new AutoPilot(vtdNav);
        autoPilot.selectElement(PARAGRAPH);
        while (autoPilot.iterate()) {
            if (isNumElementExists(vtdNav)) {
                byte[] elementeNum = (numberingConfig.getPrefix() + getElementNumeral(number++, numberingType) + numberingConfig.getSuffix()).getBytes(UTF_8);
                int index = vtdNav.getCurrentIndex();
                elementeNum = buildNumElement(vtdNav, xmlModifier, elementeNum);
                vtdNav.recoverNode(index);
                xmlModifier.insertAfterHead(elementeNum);
            }
        }
        vtdNav.recoverNode(currentIndex);
        return toByteArray(xmlModifier);
    }
    
    private char updateArticleNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, boolean onNegativeSide)
            throws NavException, ModifyException {
        byte[] articleNum;
        if (onNegativeSide) {
            articleNum = elementNumber.getBytes(UTF_8);
        } else {
            articleNum = (elementNumber + (alphaNumber <= 'z' ? alphaNumber++ : '#')).getBytes(UTF_8);
        }
        updateNumElement(vtdNav, xmlModifier, articleNum);
        return alphaNumber;
    }
    
    private char updateRecitalNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, NumberingConfig numberingConfig, boolean onNegativeSide)
            throws NavException, ModifyException {
        byte[] recitalNum;
        final String prefix = numberingConfig.getPrefix();
        final String suffix = numberingConfig.getSuffix();
        if (onNegativeSide) {
            recitalNum = (prefix + elementNumber + suffix).getBytes(UTF_8);
        } else {
            recitalNum = (elementNumber.replace(suffix.charAt(0), (alphaNumber <= 'z' ? alphaNumber++ : '#')) + suffix).getBytes(UTF_8);
        }
        updateNumElement(vtdNav, xmlModifier, recitalNum);
        return alphaNumber;
    }
    
    private char updateParagraphNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, NumberingConfig numberingConfig, boolean onNegativeSide) throws NavException, ModifyException {
        byte[] paraNum;
        final String suffix = numberingConfig.getSuffix();
        if (onNegativeSide) {
            paraNum = (elementNumber + suffix).getBytes(UTF_8);
        } else {
            paraNum = (elementNumber.replace(suffix.charAt(0), (alphaNumber <= 'z' ? alphaNumber++ : '#')) + suffix).getBytes(UTF_8);
        }
        if (isNumElementExists(vtdNav)) {
            updateNumElement(vtdNav, xmlModifier, paraNum);
        }
        return alphaNumber;
    }

    private char updatePointNumbers(String elementNumber, VTDNav vtdNav, XMLModifier xmlModifier, char alphaNumber, boolean onNegativeSide)
            throws NavException, ModifyException {
        String pointNum;
        //actual point number configuration is extracted based on the point depth, using the MULTILEVEL num. type
        NumberingType numberingType = getNumberingTypeByDepth(getNumberingConfig(numberingConfigs, NumberingType.MULTILEVEL), VTDUtils.getPointDepth(vtdNav, 0));
        NumberingConfig pointNumConfig = getNumberingConfig(numberingConfigs, numberingType);

        final String prefix = pointNumConfig.getPrefix();
        final String suffix = pointNumConfig.getSuffix();
        
        if (onNegativeSide) {
            String num = getPointNumber(Math.abs(Integer.parseInt(elementNumber)), pointNumConfig.getType());
            pointNum = (prefix + (num == "-" ? num : "-" + num + suffix));
        } else {
            if (elementNumber != null && elementNumber != "") {
                pointNum = suffix.length() != 0 ? elementNumber.replace(suffix.charAt(0), (alphaNumber <= 'z' ? alphaNumber++ : '#')) + suffix : elementNumber;
                if (elementNumber.indexOf("-") != -1 && !pointNumConfig.getType().equals(NumberingType.INDENT)) {
                    pointNum = pointNum.substring(0, pointNum.length() - 1);
                }
            } else {
                pointNum = (prefix + (alphaNumber <= 'z' ? alphaNumber++ : '#') + suffix);
            }
        }
        updateNumElement(vtdNav, xmlModifier, pointNum.getBytes(UTF_8));
        return alphaNumber;
    }
    
    private String getPointNumber(int number, NumberingType numberingType) {
        String pointNum = "#";
        switch (numberingType) {
            case ROMAN_LOWER:
                pointNum = getRomanNumeral(number);
                break;
            case ALPHA:
                pointNum = number > 0 && number < 27 ? String.valueOf((char) (number + 96)) : "#";
                break;
            case INDENT:
                pointNum = "-";
                break;
            case ARABIC:
            case ARABIC_PARENTHESIS:
            case ARABIC_POSTFIX:
                pointNum = "" + number;
                break;
            default:
                break;
        }
        return pointNum;
    }
    
    private String getRomanNumeral(int number) {
        String pointNum;
        pointNum = join("", nCopies(number, "i"))
                .replace("iiiii", "v")
                .replace("iiii", "iv")
                .replace("vv", "x")
                .replace("viv", "ix")
                .replace("xxxxx", "l")
                .replace("xxxx", "xl")
                .replace("ll", "c")
                .replace("lxl", "xc")
                .replace("ccccc", "d")
                .replace("cccc", "cd")
                .replace("dd", "m")
                .replace("dcd", "cm");
        return pointNum;
    }
    
    private void updateNumElement(VTDNav vtdNav, XMLModifier xmlModifier, byte[] elementNum) throws NavException, ModifyException {
        byte[] element;
        int currentIndex = vtdNav.getCurrentIndex();
        element = buildNumElement(vtdNav, xmlModifier, elementNum);
        vtdNav.recoverNode(currentIndex);
        xmlModifier.insertAfterHead(element);
    }
    
    private boolean isElementOriginEC(VTDNav vtdNav) throws NavException {
        int currentIndex = vtdNav.getCurrentIndex();
        boolean isOriginEC = false;
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
            int attributeIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
            if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(EC)) {
                isOriginEC = true;
            }
        } else {
            int attributeIndex = vtdNav.getAttrVal(LEOS_ORIGIN_ATTR);
            if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(EC)) {
                isOriginEC = true;
            }
        }
        vtdNav.recoverNode(currentIndex);
        return isOriginEC;
    }
    
    private boolean hasAffectedAttribute(VTDNav vtdNav, XMLModifier xmlModifier)
            throws NavException, ModifyException {
        int index = vtdNav.getCurrentIndex();
        boolean flag;
        int attributeIndex = vtdNav.getAttrVal(LEOS_AFFECTED_ATTR);
        flag = setFlagAndRemoveAttribute(vtdNav, xmlModifier, LEOS_AFFECTED_ATTR, attributeIndex);
        
        vtdNav.recoverNode(index);
        return flag;
    }
    
    private boolean hasNumToggledAttribute(VTDNav vtdNav, XMLModifier xmlModifier) throws NavException, ModifyException {
        int index = vtdNav.getCurrentIndex();
        boolean flag = this.isDefaultEditable;
        int attributeIndex;
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, PARAGRAPH)) {
            do {
                int paraIndex = vtdNav.getCurrentIndex();
                if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
                    attributeIndex = vtdNav.getAttrVal(TOGGLED_TO_NUM);
                    if (!flag) {
                        flag = setFlagAndRemoveAttribute(vtdNav, xmlModifier, TOGGLED_TO_NUM, attributeIndex);
                    } else {
                        setFlagAndRemoveAttribute(vtdNav, xmlModifier, TOGGLED_TO_NUM, attributeIndex);
                    }
                }
                vtdNav.recoverNode(paraIndex);
            } while (vtdNav.toElement(VTDNav.NEXT_SIBLING, PARAGRAPH));
        }
        
        vtdNav.recoverNode(index);
        return flag;
    }
    
    private boolean setFlagAndRemoveAttribute(VTDNav vtdNav, XMLModifier xmlModifier, String affectedAttribute, int attributeIndex) throws NavException, ModifyException {
        boolean flag = false;
        if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals("true")) {
            flag = true;
            AutoPilot ap = new AutoPilot(vtdNav);
            ap.selectAttr(affectedAttribute);
            int attrIndex = ap.iterateAttr();
            xmlModifier.removeAttribute(attrIndex);
        }
        return flag;
    }
    
    private void removeDepthAttribute(XMLModifier xmlModifier) throws Exception {
        byte[] xmlContent = toByteArray(xmlModifier);
        VTDNav vtdNav = setupVTDNav(xmlContent);
        AutoPilot ap = new AutoPilot(vtdNav);
        ap.selectElement(LEVEL);
        while (ap.iterate()) {
            removeAttribute(vtdNav, xmlModifier, LEOS_DEPTH_ATTR);
        }
    }
    
    private boolean isNumElementExists(VTDNav vtdNav) throws NavException {
        int index = vtdNav.getCurrentIndex();
        boolean exists = false;
        if (vtdNav.toElement(VTDNav.FIRST_CHILD, NUM)) {
            //check if Num is soft deleted
            exists = true;
            int attributeIndex = vtdNav.getAttrVal(LEOS_SOFT_ACTION_ATTR);
            if (attributeIndex != -1 && vtdNav.toNormalizedString(attributeIndex).equals(SoftActionType.DELETE.getSoftAction())) {
                exists = false;
            }
        }
        vtdNav.recoverNode(index);
        return exists;
    }
    
    private String getElementNumeral(long number, NumberingType numberingType) {
        switch (numberingType) {
            case ROMAN_LOWER:
                return getRomanNumeral(new Long(number).intValue());
            case ROMAN_UPPER:
                return getRomanNumeral(new Long(number).intValue()).toUpperCase();
            case ALPHA:
                return  number > 0 && number < 27 ? String.valueOf((char) (number + 96)) : "#";
            case ARABIC:
            case ARABIC_POSTFIX:
            case ARABIC_PARENTHESIS:
                return new Long(number).toString();
            default:
                return new Long(number).toString();
        }
    }
    
    void setImportAticleDefaultProperties() {
        this.isDefaultEditable = true;
    }
    
    void resetImportAticleDefaultProperties() {
        this.isDefaultEditable = false;
    }
    
    class LevelVO {
        String levelNum;
        int levelDepth;
        
        public String getLevelNum() {
            return levelNum;
        }
        public void setLevelNum(String levelNum) {
            this.levelNum = levelNum;
        }
        public int getLevelDepth() {
            return levelDepth;
        }
        public void setLevelDepth(int levelDepth) {
            this.levelDepth = levelDepth;
        }
    }
}
