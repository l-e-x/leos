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
package eu.europa.ec.leos.ui.view.document;

import com.google.common.eventbus.EventBus;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import com.vaadin.v7.data.Container;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextTocItemType;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.event.component.ComparisonResponseEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.model.TocAndAncestorsVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.MenuBarComponent;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import eu.europa.ec.leos.web.ui.window.EditTocWindow;
import eu.europa.ec.leos.web.ui.window.ImportWindow;
import eu.europa.ec.leos.web.ui.window.MajorVersionWindow;
import eu.europa.ec.leos.web.ui.window.TimeLineWindow;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringComponent
@ViewScope
class DocumentScreenImpl extends VerticalLayout implements DocumentScreen {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DocumentScreenImpl.class);

    private final UserHelper userHelper;
    private final SecurityContext securityContext;
    private final EventBus eventBus;
    private final ConfigurationHelper cfgHelper;
    private final MessageHelper messageHelper;
    private final LanguageHelper langHelper;

    private final Label docTitle = new Label();
    private final Label docIcon = new Label("", ContentMode.HTML);
    private LegalTextPaneComponent legalTextPaneComponent;
    private EditTocWindow editTocWindow;
    private TimeLineWindow timeLineWindow;
    private ImportWindow importWindow;

    @Autowired
    DocumentScreenImpl(UserHelper userHelper, SecurityContext securityContext, EventBus eventBus, ConfigurationHelper cfgHelper,
                       MessageHelper messageHelper, LanguageHelper langHelper) {
        LOG.trace("Initializing document screen...");
        Validate.notNull(userHelper, "UserHelper must not be null!");
        this.userHelper = userHelper;
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(cfgHelper, "ConfigurationHelper must not be null!");
        this.cfgHelper = cfgHelper;
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(langHelper, "LanguageHelper must not be null!");
        this.langHelper = langHelper;

        initLayout();
    }

    @Override
    public void attach() {
        super.attach();
        LOG.trace("Attaching document screen...");
    }

    @Override
    public void detach() {
        super.detach();
        LOG.trace("Detaching document screen...");
    }

    @Override
    public void setDocumentTitle(final String documentTitle) {
        docTitle.setValue(documentTitle);
    }

    @Override
    public void setDocumentVersionInfo(VersionInfoVO versionInfoVO) {
        legalTextPaneComponent.setDocumentVersionInfo(versionInfoVO);
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
    public void setToc(final List<TableOfContentItemVO> tableOfContentItemVoList) {
        Container tocContainer = TableOfContentItemConverter.buildTocContainer(tableOfContentItemVoList, messageHelper);
        legalTextPaneComponent.setTableOfContent(tocContainer);
        if (editTocWindow != null) {
            editTocWindow.setTableOfContent(tocContainer);
        }
    }

    @Override
    public void showElementEditor(final String elementId, final String  elementTagName, final String elementFragment) {
        eventBus.post(new EditElementResponseEvent(elementId, elementTagName, elementFragment, LeosCategory.BILL.name(),
                securityContext.getUser(),
                securityContext.hasRole(LeosAuthority.SUPPORT) ? new LeosAuthority[]{LeosAuthority.SUPPORT} : null));
    }

    @Override
    public void refreshElementEditor(final String elementId, final String  elementTagName, final String elementFragment) {
        eventBus.post(new RefreshElementEvent(elementId, elementTagName, elementFragment));
    }

    @Override
    public void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList,
                                  Map<TocItemType, List<TocItemType>> tableOfContentRules) {
        TocItemType[] tocItemTypes = LegalTextTocItemType.getValues();
        editTocWindow = new EditTocWindow(messageHelper, eventBus, cfgHelper, tableOfContentRules, tocItemTypes);
        setToc(tableOfContentItemVoList);
        UI.getCurrent().addWindow(editTocWindow);
        editTocWindow.center();
        editTocWindow.focus();
    }

    @Override
    public void showTimeLineWindow(List documentVersions) {
        timeLineWindow = new TimeLineWindow(messageHelper, eventBus, userHelper, documentVersions);
        UI.getCurrent().addWindow(timeLineWindow);
        timeLineWindow.center();
        timeLineWindow.focus();
    }

    @Override
    public void showMajorVersionWindow() {
        MajorVersionWindow majorVersionWindow = new MajorVersionWindow(messageHelper, eventBus);
        UI.getCurrent().addWindow(majorVersionWindow);
        majorVersionWindow.center();
        majorVersionWindow.focus();
    }
    
    @Override
    public void showImportWindow() {
        importWindow = new ImportWindow(messageHelper, eventBus);
        UI.getCurrent().addWindow(importWindow);
        importWindow.center();
        importWindow.focus();
    }

    @Override
    public void displayComparison(HashMap<Integer, Object> htmlResult){
        eventBus.post(new ComparisonResponseEvent(htmlResult,LeosCategory.BILL.name().toLowerCase()));
    }

    @Override
    public void setTocAndAncestors(List<TableOfContentItemVO> tocItemList, String elementId, List<String> elementAncestorsIds) {
        eventBus.post(new FetchCrossRefTocResponseEvent(new TocAndAncestorsVO(tocItemList, elementAncestorsIds, messageHelper)));
    }

    @Override
    public void setElement( String elementId, String elementType, String elementContent) {
        eventBus.post(new FetchElementResponseEvent(elementId, elementType, elementContent));
    }

    private void initLayout() {
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        addStyleName("leos-document-layout");

        // create layout for document title
        buildTitleAndMenubar();
        buildDocumentPane();
    }

    private HorizontalLayout buildTitleAndMenubar() {
        HorizontalLayout docLayout = new HorizontalLayout();
        docLayout.addStyleName("leos-docview-header-layout");
        docLayout.setWidth(100, Unit.PERCENTAGE);
        docLayout.setSpacing(true);
        addComponent(docLayout);

        docIcon.setValue(VaadinIcons.FILE_TEXT_O.getHtml());
        docIcon.addStyleName("leos-docview-icon");
        docIcon.setWidth("32px");
        docLayout.addComponent(docIcon);
        docLayout.setComponentAlignment(docIcon, Alignment.MIDDLE_LEFT);

        docTitle.addStyleName("leos-docview-doctitle");
        docTitle.setWidth("100%");
        docLayout.addComponent(docTitle);
        docLayout.setExpandRatio(docTitle, 1.0f);
        docLayout.setComponentAlignment(docTitle, Alignment.MIDDLE_LEFT);

        // add menu bar component
        MenuBarComponent menuBarComponent = new MenuBarComponent(messageHelper, eventBus);
        docLayout.addComponent(menuBarComponent);
        docLayout.setComponentAlignment(menuBarComponent, Alignment.TOP_RIGHT);
        return docLayout;
    }

    private void buildDocumentPane() {
        LOG.debug("Building document pane...");

        // Add Legal Text Pane and add content and toc in it
        legalTextPaneComponent = new LegalTextPaneComponent(eventBus, messageHelper, userHelper);
        addComponent(legalTextPaneComponent);
        setExpandRatio(legalTextPaneComponent, 1.0f);
    }

    @Override
    public void setUserGuidance(String userGuidance) {
        eventBus.post(new FetchUserGuidanceResponse(userGuidance));
    }

    @Override
    public void displaySearchedContent(String content) {
        eventBus.post(new SearchActResponseEvent(content));
    }

    @Override
    public void closeImportWindow() {
       if(importWindow != null) {
           importWindow.close();
       }
    }
}