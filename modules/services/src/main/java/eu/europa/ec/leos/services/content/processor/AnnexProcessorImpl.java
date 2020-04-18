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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlHelper;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.vo.toc.NumberingType;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.vo.toc.TocItemUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Provider;

import java.util.List;
import java.util.stream.Collectors;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;

@Service
class AnnexProcessorImpl implements AnnexProcessor {

    private XmlContentProcessor xmlContentProcessor;
    protected NumberProcessor numberProcessor;
    private final ElementProcessor<Annex> elementProcessor;
    private MessageHelper messageHelper;
    private Provider<StructureContext> structureContextProvider;

    @Autowired
    public AnnexProcessorImpl(XmlContentProcessor xmlContentProcessor, NumberProcessor numberProcessor, ElementProcessor<Annex> elementProcessor, 
            MessageHelper messageHelper, Provider<StructureContext> structureContextProvider) {
        super();
        this.xmlContentProcessor = xmlContentProcessor;
        this.numberProcessor = numberProcessor;
        this.elementProcessor = elementProcessor;
        this.messageHelper = messageHelper;
        this.structureContextProvider = structureContextProvider;
    }
    
    @Override
    public byte[] deleteAnnexBlock(Annex document, String elementId, String tagName) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");
        
        byte[] xmlContent = elementProcessor.deleteElement(document, elementId, tagName);
        
        if(hasDepth(tagName)) {
            xmlContent = xmlContentProcessor.insertDepthAttribute(xmlContent, tagName, elementId);
            xmlContent = numberProcessor.renumberLevel(xmlContent);
        } else {
            xmlContent = numberProcessor.renumberArticles(xmlContent);
        }
        return xmlContentProcessor.doXMLPostProcessing(xmlContent);
    }
    
    @Override
    public byte[] insertAnnexBlock(Annex document, String elementId, String tagName, boolean before) {
        Validate.notNull(document, "Document is required.");
        Validate.notNull(elementId, "Element id is required.");
        byte[] xmlContent;
        if(hasDepth(tagName)) {
            String levelTemplate = XmlHelper.getTemplate(TocItemUtils.getTocItemByNameOrThrow(structureContextProvider.get().getTocItems(), tagName), "#", messageHelper);
            xmlContent =  xmlContentProcessor.insertElementByTagNameAndId(getContent(document), levelTemplate, tagName, elementId, before);
            xmlContent = xmlContentProcessor.insertDepthAttribute(xmlContent, tagName, elementId);
            xmlContent = numberProcessor.renumberLevel(xmlContent);
        } else {
            String articleTemplate = XmlHelper.getTemplate(TocItemUtils.getTocItemByNameOrThrow(structureContextProvider.get().getTocItems(), ARTICLE), "#", "Article heading...", messageHelper);
            xmlContent =  xmlContentProcessor.insertElementByTagNameAndId(getContent(document), articleTemplate, tagName, elementId, before);
            xmlContent = numberProcessor.renumberArticles(xmlContent);
        }
        return xmlContentProcessor.doXMLPostProcessing(xmlContent);
    }
    
    private boolean hasDepth(String tagName) {
        List<TocItem> tocItems = structureContextProvider.get().getTocItems().stream().
        filter(tocItem -> (tocItem.getAknTag().value().equalsIgnoreCase(tagName) && 
                tocItem.getNumberingType().value().equals(NumberingType.ARABIC_POSTFIX_DEPTH.value()))).collect(Collectors.toList());
        return tocItems.size() > 0 && tocItems.get(0).getNumberingType().value().equals(NumberingType.ARABIC_POSTFIX_DEPTH.value());
    }
    
    private byte[] getContent(Annex annex) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        return content.getSource().getBytes();
    }


}
