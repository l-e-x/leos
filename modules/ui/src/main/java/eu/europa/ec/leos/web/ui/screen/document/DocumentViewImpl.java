/**
 * Copyright 2016 European Commission
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
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.model.TocAndAncestorsVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.LockNotificationManager;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.MenuBarComponent;
import eu.europa.ec.leos.web.ui.converter.StageIconConverter;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import eu.europa.ec.leos.web.ui.extension.LockManagerExtension;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Scope("session")
@org.springframework.stereotype.Component(DocumentView.VIEW_ID)
@VaadinView(DocumentView.VIEW_ID)
@JavaScript({"vaadin://../js/web/documentViewWrapper.js" + LeosCacheToken.TOKEN})
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
    private CompareDocumentVersionWindow versionCompareWindow;
    private MenuBarComponent menuBarComponent;

    private LockManagerExtension lockManager;

    @PostConstruct
    private void init() {
        LOG.trace("Initializing {} view...", VIEW_ID);
        docViewSettings = new DocumentViewSettings();
        eventBus.register(this);

        lockManager = new LockManagerExtension(securityContext.getUser());
        lockManager.extend(this);

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
    }

    @Override
    public void populateMarkedContent(final String markedContent) {
        legalTextPaneComponent.populateMarkedContent(markedContent);
    }

    @Override
    public void refreshElementEditor(final String elementId, final String  elementTagName, final String elementFragment) {
        eventBus.post(new RefreshElementEvent(elementId, elementTagName, elementFragment));
    }

    @Override
    public void showElementEditor(final String elementId, final String  elementTagName, final String elementFragment) {
        eventBus.post(new EditElementResponseEvent(elementId, elementTagName, elementFragment, securityContext.getUser()));
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
        String fileName = document.getTitle() +".xml";
        DownloadWindow downloadWindow = new DownloadWindow(messageHelper, eventBus, fileName, document.getContentStream(), msgKey);
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
    public void displayComparison(HashMap<Integer, Object> htmlResult){
        if (versionCompareWindow!=null){
            versionCompareWindow.setComparisonContent(htmlResult);
            versionCompareWindow.center();
            versionCompareWindow.focus();
        }
    }

    @Override
    public void updateLocks(final LockActionInfo lockActionInfo){
        getUI().access(new Runnable() {
            @Override
            public void run() {
                //1. set updated locks in Javascript world at client end
                lockManager.updateLocks(lockActionInfo.getCurrentLocks());
                //2. do update on serverSide
                legalTextPaneComponent.updateLocks(lockActionInfo);
                //3. update the Menubar 
                menuBarComponent.updateLocks(lockActionInfo);
                //4. show Notification
                showTrayNotificationForLockUpdate(lockActionInfo);
            }
        });
    }

    private void showTrayNotificationForLockUpdate(LockActionInfo lockActionInfo){
        if(! VaadinSession.getCurrent().getSession().getId().equals(lockActionInfo.getLock().getSessionId())){
            LockNotificationManager.notifyUser(messageHelper,lockActionInfo);
        }
    }

    @Override
    public void setTocAndAncestors(List<TableOfContentItemVO> tocItemList, String elementId, List<String> elementAncestorsIds) {
        eventBus.post(new FetchCrossRefTocResponseEvent(new TocAndAncestorsVO(tocItemList, elementAncestorsIds, messageHelper)));
    }

    @Override
    public void setElement( String elementId, String elementType, String elementContent) {
        eventBus.post(new FetchElementResponseEvent(elementId, elementType, elementContent));
    }
}