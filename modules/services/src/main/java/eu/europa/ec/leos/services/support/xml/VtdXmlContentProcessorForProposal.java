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
import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.support.IdGenerator;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.OptionsType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_DEPTH_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.getStartTag;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.insertOrUpdateAttributeValue;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.setupVTDNav;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateOriginAttribute;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractIndexedNonTocElements;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractLevelNonTocItems;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractOrBuildHeaderElement;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.extractOrBuildNumElement;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.navigateToFirstTocElment;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class VtdXmlContentProcessorForProposal extends VtdXmlContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VtdXmlContentProcessorForProposal.class);
    
    @Override
    public byte[] createDocumentContentWithNewTocList(List<TableOfContentItemVO> tableOfContentItemVOs, byte[] content, User user) {
        LOG.trace("Start building the document content for the new toc list");
        long startTime = System.currentTimeMillis();
        try {
            List<TocItem> tocItems = structureContextProvider.get().getTocItems();
            List<NumberingConfig> numberingConfigs = structureContextProvider.get().getNumberingConfigs();
            Map<TocItem, List<TocItem>> tocRules = structureContextProvider.get().getTocRules();

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
                    mergedContent.append(buildTocItemContent(tocItems, numberingConfigs, tocRules, contentNavigator, tocVo, user));
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

    private byte[] buildTocItemContent(List<TocItem> tocItems, List<NumberingConfig> numberingConfigs, Map<TocItem, List<TocItem>> tocRules, VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO, User user)
            throws NavException, UnsupportedEncodingException {
        ByteArrayBuilder tocItemContent = new ByteArrayBuilder();

        tocItemContent.append(extractOrBuildNumElement(contentNavigator, tableOfContentItemVO));
        tocItemContent.append(extractOrBuildHeaderElement(contentNavigator, tableOfContentItemVO));
        if (tableOfContentItemVO.getIntroTagIndex() != null) {
            tocItemContent.append(extractIndexedNonTocElements(contentNavigator, tableOfContentItemVO.getIntroTagIndex()));
        }

        for (TableOfContentItemVO child : tableOfContentItemVO.getChildItemsView()) {
            tocItemContent.append(buildTocItemContent(tocItems, numberingConfigs, tocRules, contentNavigator, child, user));
        }

        byte[] startTag = new byte[0];
        String tocTagName = tableOfContentItemVO.getTocItem().getAknTag().value();

        if (tableOfContentItemVO.getVtdIndex() != null) {
            contentNavigator.recoverNode(tableOfContentItemVO.getVtdIndex());
            startTag = getStartTag(contentNavigator);
            tocItemContent.append(extractLevelNonTocItems(tocItems, tocRules, contentNavigator));
            if (tableOfContentItemVO.getItemDepth() > 0) {
               startTag = insertOrUpdateAttributeValue(new StringBuilder(new String(startTag)), LEOS_DEPTH_ATTR, tableOfContentItemVO.getItemDepth()).toString().getBytes(UTF_8);
            }
        } else if (tableOfContentItemVO.getChildItemsView().isEmpty()) {
            byte[] tocItemTag;
            if (tableOfContentItemVO.getItemDepth() > 0) {
                tocItemTag = XmlHelper.getTemplate(tableOfContentItemVO.getTocItem(), tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading(), messageHelper).getBytes(UTF_8);
                tocItemTag = insertOrUpdateAttributeValue(new StringBuilder(new String(tocItemTag)), LEOS_DEPTH_ATTR, tableOfContentItemVO.getItemDepth()).toString().getBytes(UTF_8);
            } else {
                tocItemTag = XmlHelper.getTemplate(tableOfContentItemVO.getTocItem(), tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading(), messageHelper).getBytes(UTF_8);
            }
            if (tableOfContentItemVO.getTocItem().getItemHeading() == OptionsType.OPTIONAL) {
                tocItemTag = removeEmptyHeading(new String(tocItemTag, UTF_8)).getBytes(UTF_8);
            }
            return tocItemTag;
        } else {
            String startTagStr = "<" + tocTagName + " xml:id=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7) + "\">";
            startTag = updateOriginAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.getOriginAttr());
        }
        return XmlHelper.buildTag(startTag, tocTagName.getBytes(UTF_8), tocItemContent.getContent());
    }

    @Override
    public void specificInstanceXMLPostProcessing(XMLModifier xmlModifier) {
    }

    @Override
    public String[] getSplittedElement(byte[] xmlContent, String tagName, String idAttributeValue) {
        return null;
    }

    @Override
    public String[] getMergeOnElement(byte[] xmlContent, String content, String tagName, String idAttributeValue) {
        return null;
    }

    @Override
    public byte[] mergeElement(byte[] xmlContent, String content, String tagName, String idAttributeValue) {
        return null;
    }
}
