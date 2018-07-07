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
package eu.europa.ec.leos.web.ui.screen.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Container;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.Position;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.Window;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.event.view.document.EnterDocumentViewEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.MenuBarComponent;
import eu.europa.ec.leos.web.ui.component.SharedLockComponent;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import eu.europa.ec.leos.web.ui.window.CompareDocumentVersionWindow;
import eu.europa.ec.leos.web.ui.window.DownloadWindow;
import eu.europa.ec.leos.web.ui.window.EditArticleWindow;
import eu.europa.ec.leos.web.ui.window.EditCitationsWindow;
import eu.europa.ec.leos.web.ui.window.EditMetaDataWindow;
import eu.europa.ec.leos.web.ui.window.EditRecitalsWindow;
import eu.europa.ec.leos.web.ui.window.EditTocWindow;
import eu.europa.ec.leos.web.ui.window.VersionsListWindow;
import eu.europa.ec.leos.web.view.DocumentView;

@Scope("session")
@org.springframework.stereotype.Component(DocumentView.VIEW_ID)
@VaadinView(DocumentView.VIEW_ID)
@JavaScript({"http://cdn.mathjax.org/mathjax/2.3-latest/MathJax.js?config=default"})
public class DocumentViewImpl extends LeosScreen implements DocumentView {

    private static final long serialVersionUID = 4697888482004181236L;
    private static final Logger LOG = LoggerFactory.getLogger(DocumentViewImpl.class);

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private ConfigurationHelper cfgHelper;

    private DocumentViewSettings docViewSettings;
    private final Label docName = new Label();
    private LegalTextPaneComponent legalTextPaneComponent;
    private EditTocWindow editTocWindow;
    private EditArticleWindow editArticleWindow;
    private EditCitationsWindow editCitationWindow;
    private EditRecitalsWindow editRecitalsWindow;
    private CompareDocumentVersionWindow versionCompareWindow;
    private SharedLockComponent lockCompoment;
    private MenuBarComponent menuBarComponent;

    @PostConstruct
    private void init() {
        LOG.trace("Initializing {} view...", VIEW_ID);
        docViewSettings = new DocumentViewSettings();
        eventBus.register(this);
        // initialize document view layout
        initLayout();
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        LOG.debug("Entering {} view...", getViewId());
        eventBus.post(new EnterDocumentViewEvent());
    }

    private void initLayout() {
        addStyleName("leos-document-layout");
        setSizeFull();
        // create layout for document title
        buildTitleAndMenubar();
        buildDocumentPane();

        //non visual component
        lockCompoment= new SharedLockComponent();
        lockCompoment.setLocks(new ArrayList<LockData>(0));
        addComponent(lockCompoment);
    }

    @Override
    public void attach() {
        super.attach();
        setPollingStatus(true);        // enable polling
    }

    @Override
    public void detach() {
        super.detach();
        setPollingStatus(false);        // disable polling
    }

    private HorizontalLayout buildTitleAndMenubar() {

        HorizontalLayout docLayout = new HorizontalLayout();
        docLayout.addStyleName("leos-docview-header-layout");
        docLayout.setWidth(100, Unit.PERCENTAGE);
        docLayout.setSpacing(true);
        addComponent(docLayout);

        Image docIcon = new Image("", LeosTheme.LEOS_DOCUMENT_ICON_32);
        docIcon.addStyleName("leos-viewdoc-icon");
        docLayout.addComponent(docIcon);
        docLayout.setComponentAlignment(docIcon, Alignment.MIDDLE_LEFT);

        docName.addStyleName("leos-docview-doctitle");
        docLayout.addComponent(docName);
        docLayout.setExpandRatio(docName, 1.0f);
        docLayout.setComponentAlignment(docName, Alignment.MIDDLE_LEFT);

        // add menu bar component
        menuBarComponent = new MenuBarComponent(messageHelper, eventBus, cfgHelper, docViewSettings);
        docLayout.addComponent(menuBarComponent);
        docLayout.setComponentAlignment(menuBarComponent, Alignment.TOP_RIGHT);

        return docLayout;
    }

    private void buildDocumentPane() {

        LOG.debug("Building document pane...");

        // Add Legal Text Pane and add content and toc in it
        legalTextPaneComponent = new LegalTextPaneComponent(eventBus, messageHelper, docViewSettings);
        addComponent(legalTextPaneComponent);
        setExpandRatio(legalTextPaneComponent, 1.0f);

    }

    @Override
    public void setDocumentName(final String documentName) {
        docName.setValue(documentName);
    }

    @Override
    public void refreshContent(final String documentContent) {
        legalTextPaneComponent.populateContent(documentContent);
        forceMathJaxRendering();
    }

    @Override
    public void showArticleEditor(final String articleId, final String article) {
        editArticleWindow = new EditArticleWindow(messageHelper, eventBus, articleId, article, cfgHelper);
        UI.getCurrent().addWindow(editArticleWindow);
        editArticleWindow.center();
        editArticleWindow.focus();
    }

    @Override
    public void refreshArticleEditor(final String articleContent) {
        if(editArticleWindow!=null){
            editArticleWindow.updateContent(articleContent);
        }
    }
    
    @Override
    public void showCitationsEditor(final String citationsId, final String citations) {
        editCitationWindow = new EditCitationsWindow(messageHelper, eventBus, citationsId, citations, cfgHelper);
        UI.getCurrent().addWindow(editCitationWindow);
        editCitationWindow.center();
        editCitationWindow.focus();
    }
    
    @Override
    public void refreshCitationsEditor(final String citationsContent) {
        if(editCitationWindow!=null){
            editCitationWindow.updateContent(citationsContent);
        }
    }
    
    @Override
    public void showRecitalsEditor(final String recitalsId, final String recitals) {
        editRecitalsWindow = new EditRecitalsWindow(messageHelper, eventBus, recitalsId, recitals, cfgHelper);
        UI.getCurrent().addWindow(editRecitalsWindow);
        editRecitalsWindow.center();
        editRecitalsWindow.focus();
    }
    
    @Override
    public void refreshRecitalsEditor(final String recitalsContent) {
        if(editRecitalsWindow!=null){
            editRecitalsWindow.updateContent(recitalsContent);
        }
    }
    
    @Override
    public void showMetadataEditWindow(MetaDataVO metaDataVO) {
        Validate.notNull(docName);
        EditMetaDataWindow editMetaDataWindow = new EditMetaDataWindow(messageHelper, eventBus, metaDataVO);

        UI.getCurrent().addWindow(editMetaDataWindow);
        editMetaDataWindow.center();
        editMetaDataWindow.focus();
    }

    @Override
    public void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList,
            Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules) {
        editTocWindow = new EditTocWindow(messageHelper, eventBus, cfgHelper, tableOfContentRules);
        setToc(tableOfContentItemVoList);
        UI.getCurrent().addWindow(editTocWindow);
        editTocWindow.center();
        editTocWindow.focus();
    }

    @Override
    public void showDownloadWindow(final LeosDocument document, String msgKey) {

        DownloadWindow downloadWindow = new DownloadWindow(messageHelper, eventBus, document.getName(), document.getContentStream(), msgKey);
        downloadWindow.addCloseListener(new Window.CloseListener() {
            @Override
            public void windowClose(Window.CloseEvent e) {
                try {
                    document.getContentStream().close();
                } catch (IOException e1) {
                    LOG.warn("Unable to close the document content stream");
                    // nothing more to do, we tried our best
                }
            }
        });

        UI.getCurrent().addWindow(downloadWindow);
        downloadWindow.center();
        downloadWindow.focus();
    }

    @Override
    public void setToc(final List<TableOfContentItemVO> tableOfContentItemVoList) {
        Container tocContainer = TableOfContentItemConverter.buildTocContainer(tableOfContentItemVoList, messageHelper);
        legalTextPaneComponent.setTableOfContent(tocContainer);
        if (editTocWindow != null) {
            editTocWindow.setTableOfContent(tocContainer);
        }
    }

    @Override
    public @Nonnull
    String getViewId() {
        return VIEW_ID;
    }

    private void forceMathJaxRendering() {
        LOG.trace("Forcing MathJax rendering of formulas...");
        // KLUGE hack to force MathJax rendering of formulas when document content is refreshed
        Page.getCurrent().getJavaScript().execute("MathJax.Hub.Queue([\"Typeset\",MathJax.Hub]);");
        Page.getCurrent().getJavaScript().execute("MathJax.Hub.Config({\"HTML-CSS\": {imageFont: null}});");
    }

    @Override
    public void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL){
        legalTextPaneComponent.setDocumentPreviewURLs(documentId,  pdfURL, htmlURL);
    }

    @Override
    public void showVersionListWindow(List<LeosDocumentProperties> versions) {
        VersionsListWindow docVersionWindow = new VersionsListWindow(messageHelper, eventBus, versions);
        UI.getCurrent().addWindow(docVersionWindow);
        docVersionWindow.center();
        docVersionWindow.focus();
    }

    @Override
    public void showVersionCompareWindow(LeosDocumentProperties oldVersion,  LeosDocumentProperties newVersion) {
        versionCompareWindow = new CompareDocumentVersionWindow(messageHelper, eventBus, oldVersion, newVersion);
        UI.getCurrent().addWindow(versionCompareWindow);
        versionCompareWindow.center();
        versionCompareWindow.focus();
    }

    @Override
    public void displayComparision(HashMap<Integer, Object> htmlResult){
        if (versionCompareWindow!=null){
            versionCompareWindow.setComparisionContent(htmlResult);
            versionCompareWindow.center();
            versionCompareWindow.focus();
            
            forceMathJaxRendering();
        }
    }

    public void updateLocks(final LockActionInfo lockActionInfo){
        getUI().access(new Runnable() {
            @Override
            public void run() {
                final List<LockData> lstLocks=lockActionInfo.getCurrentLocks();
                //1. set updated locks in Javascript world at client end 
                lockCompoment.setLocks(lstLocks);
                //2. do update on serverSide 
                legalTextPaneComponent.updateLocks(lockActionInfo);
                //3. update the Menubar 
                menuBarComponent.updateLocks(lockActionInfo);
                //4. show Notification
                showTrayNotfnForLockUpdate(lockActionInfo);
            }

        });
    }

    private void showTrayNotfnForLockUpdate(LockActionInfo lockActionInfo){

        if(! VaadinSession.getCurrent().getSession().getId().equals(lockActionInfo.getLock().getSessionId())){
            LockData updatedLock=lockActionInfo.getLock();
            String messageHeading=messageHelper.getMessage( "document.message.tray.heading");
            String message=null;

            switch (lockActionInfo.getLock().getLockLevel()) {
                case READ_LOCK:
                    message = messageHelper.getMessage( "document.locked.read."+lockActionInfo.getOperation().getValue(), updatedLock.getUserName(), updatedLock.getUserLoginName());
                    break;
                case DOCUMENT_LOCK:
                    message = messageHelper.getMessage( "document.locked."+ lockActionInfo.getOperation().getValue(), updatedLock.getUserName(), updatedLock.getUserLoginName());
                    break;
                case ELEMENT_LOCK:
                    message = messageHelper.getMessage( "document.locked.article."+ lockActionInfo.getOperation().getValue(), updatedLock.getUserName(), updatedLock.getUserLoginName(), updatedLock.getElementId());
                    break;
            }

            Notification notfn= new Notification(messageHeading, message, Type.TRAY_NOTIFICATION);
            notfn.setPosition(Position.BOTTOM_RIGHT);
            notfn.setDelayMsec(3000);
            notfn.show(Page.getCurrent());
        }
    }

}

