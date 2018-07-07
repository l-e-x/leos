/*
 * Copyright 2017 European Commission
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
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.web.support.UrlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
@Scope("prototype")
public class ComparisonDelegate<T extends LeosDocument.XmlDocument> {

    private static final Logger LOG = LoggerFactory.getLogger(ComparisonDelegate.class);

    private final TransformationService transformerService;
    private final ContentComparatorService compareService;
    private final UrlBuilder urlBuilder;

    @Autowired
    public ComparisonDelegate(TransformationService transformerService, ContentComparatorService compareService, UrlBuilder urlBuilder) {
        this.transformerService = transformerService;
        this.compareService = compareService;
        this.urlBuilder = urlBuilder;
    }

    public String getMarkedContent(T oldVersion, T newVersion) {
        //FIXME collect list of processing to be done on comparison output and do it at single place.
        String cxtPath = urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest());
        String firstItemHtml = transformerService.formatToHtml(oldVersion, cxtPath).replaceAll("(?i)(href|onClick)=\".*?\"", "");// removing the links
        String secondItemHtml = transformerService.formatToHtml(newVersion, cxtPath).replaceAll("(?i)(href|onClick)=\".*?\"", "");

        //FIXME Shortcut to replace all the original Ids in document. Need a discussion.
        return compareService.compareHtmlContents(firstItemHtml, secondItemHtml).replaceAll("(?i) id=\"", " id=\"marked-");
    }

    public HashMap<Integer, Object> versionCompare(T oldVersion, T newVersion, int displayMode) {
        final int SINGLE_COLUMN_MODE = 1;
        final int TWO_COLUMN_MODE = 2;

        long startTime = System.currentTimeMillis();
        HashMap<Integer, Object> htmlCompareResult = new HashMap<>();

        String cxtPath = urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest());
        String firstItemHtml = transformerService.formatToHtml(oldVersion, cxtPath).replaceAll("(?i)(href|onClick)=\".*?\"", "");//removing the links
        String secondItemHtml = transformerService.formatToHtml(newVersion, cxtPath).replaceAll("(?i)(href|onClick)=\".*?\"", "");

        if(displayMode==SINGLE_COLUMN_MODE){
            htmlCompareResult.put(SINGLE_COLUMN_MODE, compareService.compareHtmlContents(firstItemHtml, secondItemHtml));
        }
        else if(displayMode==TWO_COLUMN_MODE){
            htmlCompareResult.put(TWO_COLUMN_MODE, compareService.twoColumnsCompareHtmlContents(firstItemHtml,secondItemHtml));
        }

        LOG.debug("Diff exec time: {} ms", (System.currentTimeMillis() - startTime));
        return htmlCompareResult;
    }
}
