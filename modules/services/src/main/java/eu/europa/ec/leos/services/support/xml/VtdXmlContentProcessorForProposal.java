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
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.support.IdGenerator;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.AnnexTocItemType;
import eu.europa.ec.leos.vo.toctype.LegalTextProposalTocItemType;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.function.Function;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.*;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class VtdXmlContentProcessorForProposal extends VtdXmlContentProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(VtdXmlContentProcessorForProposal.class);
    
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

    static byte[] buildTocItemContent(Function<String, TocItemType> getTocItemType, VTDNav contentNavigator, TableOfContentItemVO tableOfContentItemVO, User user)
            throws NavException, UnsupportedEncodingException {
        ByteArrayBuilder tocItemContent = new ByteArrayBuilder();

        tocItemContent.append(extractOrBuildNumElement(contentNavigator, tableOfContentItemVO));
        tocItemContent.append(extractOrBuildHeaderElement(contentNavigator, tableOfContentItemVO));
        if(tableOfContentItemVO.getPreambleFormula1TagIndex() != null) {
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
            startTag = getStartTag(contentNavigator);
            if (tableOfContentItemVO.getType().equals(LegalTextProposalTocItemType.PREAMBLE) && tableOfContentItemVO.getPreambleFormula2TagIndex() != null) {
                tocItemContent.append(extractIndexedNonTocElements(contentNavigator, tableOfContentItemVO.getPreambleFormula2TagIndex()));
            } else if (!tableOfContentItemVO.getType().equals(LegalTextProposalTocItemType.RECITALS)) { // Recitals contains intro non TOC item and it already has been added
                tocItemContent.append(extractLevelNonTocItems(getTocItemType, contentNavigator));
            }
        } else if (tableOfContentItemVO.getType().equals(LegalTextProposalTocItemType.CITATION)) {
            return XmlHelper.getCitationTemplate().getBytes(UTF_8);
        } else if (tableOfContentItemVO.getType().equals(LegalTextProposalTocItemType.RECITAL)) {
            return XmlHelper.getRecitalTemplate(tableOfContentItemVO.getNumber()).getBytes(UTF_8);
        } else if (tableOfContentItemVO.getType().equals(LegalTextProposalTocItemType.ARTICLE)) {
            return XmlHelper.getArticleTemplate(tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading()).getBytes(UTF_8);
        } else if (tableOfContentItemVO.getType().equals(AnnexTocItemType.DIVISION)) {
            return XmlHelper.getAnnexTemplate().getBytes(UTF_8);
        } else {
            String startTagStr = "<" + tocTagName + " xml:id=\"" + IdGenerator.generateId(tocTagName.substring(0, 3), 7) + "\">";
            startTag = updateOriginAttribute(startTagStr.getBytes(UTF_8), tableOfContentItemVO.getOriginAttr());
        }
        return XmlHelper.buildTag(startTag, tocTagName.getBytes(UTF_8), tocItemContent.getContent());
    }

    @Override
    public void specificInstanceXMLPostProcessing(XMLModifier xmlModifier) throws Exception {
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
