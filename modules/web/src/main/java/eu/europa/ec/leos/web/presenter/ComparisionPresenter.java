/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.presenter;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;

import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.format.FormatterService;
import eu.europa.ec.leos.support.web.UrlBuilder;
import eu.europa.ec.leos.web.event.view.document.CompareVersionEvent;
import eu.europa.ec.leos.web.event.view.document.CompareVersionRequestEvent;
import eu.europa.ec.leos.web.event.window.ShowVersionsEvent;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.RepositoryView;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class ComparisionPresenter extends AbstractPresenter<DocumentView> {

    @Autowired
    private DocumentView documentView;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private FormatterService formatterService;

    @Autowired
    private ContentComparatorService compareService;

    @Override
    public DocumentView getView() {
        return documentView;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ComparisionPresenter.class);

    @Subscribe
    public void showVersionListWindow(ShowVersionsEvent event) {
        String strDocId = getDocumentId();
        List<LeosDocumentProperties>  lstDocumentVersions = documentService.getDocumentVersions(strDocId);
        documentView.showVersionListWindow(lstDocumentVersions);
    }

    @Subscribe
    public void showVersionCompareWindow(CompareVersionRequestEvent event) {
        LeosDocumentProperties oldVersion = event.getOldVersion();
        LeosDocumentProperties newVersion = event.getNewVersion();

        documentView.showVersionCompareWindow(oldVersion, newVersion );
    }

    @Subscribe
    public void versionCompare(CompareVersionEvent event) {
        final int SINGLE_COLUMN_MODE=1;
        final int TWO_COLUMN_MODE=2;

        LeosDocumentProperties oldVersion = event.getOldVersion();
        LeosDocumentProperties newVersion = event.getNewVersion();
        long startTime = System.currentTimeMillis();
        HashMap<Integer, Object> htmlCompareResult = new HashMap<Integer, Object>();

        String cxtPath = new UrlBuilder().getWebAppPath(VaadinServletService.getCurrentServletRequest());
        String firstItemHtml = formatterService.formatToHtml(oldVersion.getVersionId(), cxtPath).replaceAll("(?i)(href|onClick)=\".*?\"", "");//removing the links
        String secondItemHtml = formatterService.formatToHtml(newVersion.getVersionId(), cxtPath).replaceAll("(?i)(href|onClick)=\".*?\"", ""); 

        if(event.getDisplayMode()==SINGLE_COLUMN_MODE){
            htmlCompareResult.put(SINGLE_COLUMN_MODE, compareService.compareHtmlContents(firstItemHtml, secondItemHtml));
        }
        else if(event.getDisplayMode()==TWO_COLUMN_MODE){
            htmlCompareResult .put(TWO_COLUMN_MODE, compareService.twoColumnsCompareHtmlContents(firstItemHtml,secondItemHtml));
        }
        LOG.debug("Diff exec time: {} ms", (System.currentTimeMillis() - startTime));

        documentView.displayComparision(htmlCompareResult);
    }

    private String getDocumentId() {
        String strDocId = null;
        Object docId = session.getAttribute(SessionAttribute.DOCUMENT_ID.name());
        if (docId == null) {
            rejectView(RepositoryView.VIEW_ID, "document.id.missing");
        } else {
            strDocId = (String) docId;
        }
        return strDocId;
    }
}
