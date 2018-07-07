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

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Container;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.event.view.document.EnterDocumentViewEvent;
import eu.europa.ec.leos.web.support.LockNotificationManager;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.MenuBarComponent;
import eu.europa.ec.leos.web.ui.component.SharedLockComponent;
import eu.europa.ec.leos.web.ui.converter.StageIconConverter;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.ui.window.*;
import eu.europa.ec.leos.web.view.DocumentView;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.xpoft.vaadin.VaadinView;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scope("session")
@org.springframework.stereotype.Component(DocumentView.VIEW_ID)
@VaadinView(DocumentView.VIEW_ID)
@JavaScript({"http://cdn.mathjax.org/mathjax/2.3-latest/MathJax.js?config=default"})
public class DocumentViewImpl extends LeosScreen implements DocumentView {

    private static final long serialVersionUID = 4697888482004181236L;
    private static final Logger LOG = LoggerFactory.getLogger(DocumentViewImpl.class);

    @Autowired
    private ConfigurationHelper cfgHelper;

    private DocumentViewSettings docViewSettings;
    private final Label docTitle = new Label();
    private final Label docIcon = new Label("", ContentMode.HTML);//default
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
        LOG.debug("Entering {} view...param {}", getViewId(), event.getParameters());
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

        docIcon.addStyleName("leos-docview-icon");
        docIcon.setWidth("32px");
        docLayout.addComponent(docIcon);
        docLayout.setComponentAlignment(docIcon, Alignment.MIDDLE_LEFT);

        docTitle.addStyleName("leos-docview-doctitle");
        docLayout.addComponent(docTitle);
        docLayout.setExpandRatio(docTitle, 1.0f);
        docLayout.setComponentAlignment(docTitle, Alignment.MIDDLE_LEFT);

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
    public void setDocumentTitle(final String documentTitle) {
        docTitle.setValue(documentTitle);
    }

    @Override
    public void setDocumentStage(final LeosDocumentProperties.Stage value) {
        LeosDocumentProperties.Stage stage = (value == null) ? LeosDocumentProperties.Stage.DRAFT : value;
        docIcon.setValue(new StageIconConverter().convertToPresentation(stage, null, null));
        docIcon.setStyleName(stage.toString().toLowerCase());//to clear the already set styles
        docIcon.addStyleName("leos-docview-icon");
    }

    @Override
    public void refreshContent(final String documentContent) {
        legalTextPaneComponent.populateContent(documentContent);
        forceMathJaxRendering();
    }

    @Override
    public void showArticleEditor(final String articleId, final String article) {
        editArticleWindow = new EditArticleWindow(messageHelper, eventBus, articleId, article, cfgHelper, securityContext.getUser());
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
        editCitationWindow = new EditCitationsWindow(messageHelper, eventBus, citationsId, citations, cfgHelper, securityContext.getUser());
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
        editRecitalsWindow = new EditRecitalsWindow(messageHelper, eventBus, recitalsId, recitals, cfgHelper, securityContext.getUser());
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
        Validate.notNull(docTitle);
        EditMetaDataWindow editMetaDataWindow = new EditMetaDataWindow(messageHelper, langHelper, eventBus, metaDataVO);

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
        
        DownloadWindow downloadWindow = new DownloadWindow(messageHelper, eventBus, document.getTitle(), document.getContentStream(), msgKey);
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
            LockNotificationManager.notifyUser(messageHelper,lockActionInfo);
        }
    }

    @Override
    public void setCrossReferenceToc(List<TableOfContentItemVO> tocItemList, List<String> ancestorsIds, String windowName) {
        if (editArticleWindow != null && editArticleWindow.getWindowName().equalsIgnoreCase(windowName)) {
            editArticleWindow.setCrossReferenceTableOfContent(tocItemList, ancestorsIds);
        } else if (editCitationWindow != null && editCitationWindow.getWindowName().equalsIgnoreCase(windowName)) {
            editCitationWindow.setCrossReferenceTableOfContent(tocItemList, ancestorsIds);
        } else if (editRecitalsWindow != null && editRecitalsWindow.getWindowName().equalsIgnoreCase(windowName)) {
            editRecitalsWindow.setCrossReferenceTableOfContent(tocItemList, ancestorsIds);
        }
    }
    
    @Override
    public void setElementContent(String elementContent, String windowName) {
        if(editArticleWindow != null && editArticleWindow.getWindowName().equalsIgnoreCase(windowName)) {
            editArticleWindow.setElementContent(elementContent);
        } else if(editCitationWindow != null && editCitationWindow.getWindowName().equalsIgnoreCase(windowName)) {
            editCitationWindow.setElementContent(elementContent);
        } else if(editRecitalsWindow != null && editRecitalsWindow.getWindowName().equalsIgnoreCase(windowName)) {
            editRecitalsWindow.setElementContent(elementContent);
        } 
    }  

}

