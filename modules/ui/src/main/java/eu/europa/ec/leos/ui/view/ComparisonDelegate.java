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
package eu.europa.ec.leos.ui.view;

import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.compare.ContentComparatorContext;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.web.support.UrlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.ATTR_NAME;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.CONTENT_ADDED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.CONTENT_REMOVED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_ADDED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_REMOVED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_RETAIN_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_INTERMEDIATE_STYLE;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_ORIGINAL_STYLE;

@Component
@Scope("prototype")
public class ComparisonDelegate<T extends XmlDocument> {

    private static final Logger LOG = LoggerFactory.getLogger(ComparisonDelegate.class);

    private final TransformationService transformerService;
    private final ContentComparatorService compareService;
    private final UrlBuilder urlBuilder;
    private final SecurityContext securityContext;

    @Autowired
    public ComparisonDelegate(TransformationService transformerService, ContentComparatorService compareService, UrlBuilder urlBuilder, SecurityContext securityContext) {
        this.transformerService = transformerService;
        this.compareService = compareService;
        this.urlBuilder = urlBuilder;
        this.securityContext = securityContext;
    }
    
    public String getMarkedContent(T oldVersion, T newVersion) {
        final String comparedContent = getComparedContent(oldVersion, newVersion);
        return comparedContent.replaceAll("(?i) id=\"", " id=\"marked-");
    }
    
    public HashMap<ComparisonDisplayMode, Object> versionCompare(T oldVersion, T newVersion, ComparisonDisplayMode displayMode) {
        long startTime = System.currentTimeMillis();
        HashMap<ComparisonDisplayMode, Object> htmlCompareResult = new HashMap<>();
        
        switch (displayMode) {
            case SINGLE_COLUMN_MODE:
                final String singleResult = getComparedContent(oldVersion, newVersion);
                htmlCompareResult.put(displayMode, singleResult);
                break;
            case TWO_COLUMN_MODE:
                final String firstItemHtml = getDocumentAsHtml(oldVersion);
                final String secondItemHtml = getDocumentAsHtml(newVersion);
                final String[] doubleResult = compareService.twoColumnsCompareContents(new ContentComparatorContext.Builder(firstItemHtml, secondItemHtml).build());
                htmlCompareResult.put(displayMode, doubleResult);
        }
        LOG.debug("Diff exec time: {} ms", (System.currentTimeMillis() - startTime));
        return htmlCompareResult;
    }
    
    private String getComparedContent(T oldVersion, T newVersion) {
        final String firstItemHtml = getDocumentAsHtml(oldVersion);
        final String secondItemHtml = getDocumentAsHtml(newVersion);
        return compareService.compareContents(new ContentComparatorContext.Builder(firstItemHtml, secondItemHtml)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_REMOVED_CLASS)
                .withAddedValue(CONTENT_ADDED_CLASS)
                .build());
    }
    
    public String doubleCompareHtmlContents(T originalProposal, T intermediateMajor, T current, boolean threeWayEnabled) {
        //FIXME collect list of processing to be done on comparison output and do it at single place.
        final String currentHtml = getDocumentAsHtml(current);
        
        if(threeWayEnabled) {
            final String proposalHtml = getDocumentAsHtml(originalProposal);
            final String intermediateMajorHtml = getDocumentAsHtml(intermediateMajor);
            
            return compareService.compareContents(new ContentComparatorContext.Builder(proposalHtml, currentHtml, intermediateMajorHtml)
                    .withAttrName(ATTR_NAME)
                    .withRemovedValue(DOUBLE_COMPARE_REMOVED_CLASS)
                    .withAddedValue(DOUBLE_COMPARE_ADDED_CLASS)
                    .withRemovedIntermediateValue(DOUBLE_COMPARE_REMOVED_CLASS + DOUBLE_COMPARE_INTERMEDIATE_STYLE)
                    .withAddedIntermediateValue(DOUBLE_COMPARE_ADDED_CLASS + DOUBLE_COMPARE_INTERMEDIATE_STYLE)
                    .withRemovedOriginalValue(DOUBLE_COMPARE_REMOVED_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                    .withAddedOriginalValue(DOUBLE_COMPARE_ADDED_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                    .withRetainOriginalValue(DOUBLE_COMPARE_RETAIN_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                    .withDisplayRemovedContentAsReadOnly(Boolean.TRUE)
                    .withThreeWayDiff(threeWayEnabled)
                    .build())
                    .replaceAll("(?i) id=\"", " id=\"doubleCompare-")
                    .replaceAll("(?i) leos:softmove_from=\"", " leos:softmove_from=\"doubleCompare-")
                    .replaceAll("(?i) leos:softmove_to=\"", " leos:softmove_to=\"doubleCompare-");
        }

        return currentHtml;
    }
    
    public String getDocumentAsHtml(T xmlDocument) {
        final String ctxPath = urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest());
        return transformerService.formatToHtml(xmlDocument, ctxPath, securityContext.getPermissions(xmlDocument))
                .replaceAll("(?i)(href|onClick)=\".*?\"", "");
    }

    public String doubleCompareXmlContents(T originalProposal, T intermediateMajor, T current, boolean threeWayEnabled) {
        final String currentXml = current.getContent().getOrError(() -> "Current document content is required!").getSource().toString();
        
        if(threeWayEnabled) {
            final String proposalXml = originalProposal.getContent().getOrError(() -> "Proposal document content is required!").getSource().toString();
            final String intermediateMajorXml = intermediateMajor.getContent().getOrError(() -> "Intermadiate Major Version document content is required!").getSource().toString();
            
            return compareService.compareContents(new ContentComparatorContext.Builder(proposalXml, currentXml, intermediateMajorXml)
                    .withAttrName(ATTR_NAME)
                    .withRemovedValue(DOUBLE_COMPARE_REMOVED_CLASS)
                    .withAddedValue(DOUBLE_COMPARE_ADDED_CLASS)
                    .withRemovedIntermediateValue(DOUBLE_COMPARE_REMOVED_CLASS + DOUBLE_COMPARE_INTERMEDIATE_STYLE)
                    .withAddedIntermediateValue(DOUBLE_COMPARE_ADDED_CLASS + DOUBLE_COMPARE_INTERMEDIATE_STYLE)
                    .withRemovedOriginalValue(DOUBLE_COMPARE_REMOVED_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                    .withAddedOriginalValue(DOUBLE_COMPARE_ADDED_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                    .withRetainOriginalValue(DOUBLE_COMPARE_RETAIN_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                    .withDisplayRemovedContentAsReadOnly(Boolean.TRUE)
                    .withThreeWayDiff(threeWayEnabled)
                    .build());
        }

        return currentXml;
    }
}
