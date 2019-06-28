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
package eu.europa.ec.leos.ui.view.document;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.TreeData;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.event.security.SecurityTokenRequest;
import eu.europa.ec.leos.ui.event.security.SecurityTokenResponse;
import eu.europa.ec.leos.ui.event.toc.RefreshTocWindowEvent;
import eu.europa.ec.leos.ui.window.TocEditor;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.component.ComparisonResponseEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent.Action;
import eu.europa.ec.leos.web.model.TocAndAncestorsVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.MenuBarComponent;
import eu.europa.ec.leos.web.ui.window.ImportWindow;
import eu.europa.ec.leos.web.ui.window.MajorVersionWindow;
import eu.europa.ec.leos.web.ui.window.TimeLineWindow;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.WebApplicationContext;
import org.vaadin.dialogs.ConfirmDialog;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@SpringComponent
@ViewScope
abstract class DocumentScreenImpl extends VerticalLayout implements DocumentScreen {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(DocumentScreenImpl.class);
    
    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    protected final UserHelper userHelper;
    protected final SecurityContext securityContext;
    protected final EventBus eventBus;
    protected final ConfigurationHelper cfgHelper;
    protected final MessageHelper messageHelper;
    protected final InstanceTypeResolver instanceTypeResolver;

    private final Label docTitle = new Label();
    private final Label docIcon = new Label("", ContentMode.HTML);
    
    private TimeLineWindow timeLineWindow;
    private ImportWindow importWindow;
    protected TocEditor tocEditor;
    protected LegalTextPaneComponent legalTextPaneComponent;
    private WebApplicationContext webAppContext;
    private LeosPermissionAuthorityMapHelper authorityMapHelper;

    @Value("${leos.coedition.sip.enabled}")
    private boolean coEditionSipEnabled;

    @Value("${leos.coedition.sip.domain}")
    private String coEditionSipDomain;

    @Autowired
    DocumentScreenImpl(UserHelper userHelper, SecurityContext securityContext, EventBus eventBus, ConfigurationHelper cfgHelper,
                       MessageHelper messageHelper, TocEditor tocEditor, InstanceTypeResolver instanceTypeResolver,
                       WebApplicationContext webAppContext, LeosPermissionAuthorityMapHelper authorityMapHelper) {
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
        Validate.notNull(tocEditor, "TocEditor must not be null!");
        this.tocEditor = tocEditor;
        Validate.notNull(instanceTypeResolver, "instanceTypeResolver must not be null!");
        this.instanceTypeResolver = instanceTypeResolver;
        Validate.notNull(webAppContext, "webAppContext must not be null!");
        this.webAppContext = webAppContext;
        Validate.notNull(authorityMapHelper, "authorityMapHelper must not be null!");
        this.authorityMapHelper = authorityMapHelper;

        init();
    }

    @Override
    public void attach() {
        LOG.trace("Attaching document screen...");
        eventBus.register(this);
        super.attach();
    }

    @Override
    public void detach() {
        LOG.trace("Detaching document screen...");
        super.detach();
        eventBus.unregister(this);
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
    }

    @Override
    public void populateDoubleComparisonContent(String doubleComparisonContent) {
    }
    
    @Override
    public void setToc(final List<TableOfContentItemVO> tableOfContentItemVoList) {
        TreeData<TableOfContentItemVO> treeTocData = TableOfContentItemConverter.buildTocData(tableOfContentItemVoList);
        legalTextPaneComponent.setTableOfContent(treeTocData);
    }

    @Override
    public void setTocEditWindow(final List<TableOfContentItemVO> tableOfContentItemVoList) {
        TreeData<TableOfContentItemVO> treeTocData = TableOfContentItemConverter.buildTocData(tableOfContentItemVoList);
        eventBus.post(new RefreshTocWindowEvent(treeTocData));
    }

    @Override
    public void showElementEditor(final String elementId, final String  elementTagName, final String elementFragment, String alternatives) {
        eventBus.post(instanceTypeResolver.createEvent(elementId, elementTagName, elementFragment, LeosCategory.BILL.name(),
                securityContext.getUser(),authorityMapHelper.getPermissionsForRoles(securityContext.getUser().getRoles()),alternatives));
    }

    @Override
    public void refreshElementEditor(final String elementId, final String elementTagName, final String elementFragment) {
        eventBus.post(new RefreshElementEvent(elementId, elementTagName, elementFragment));
    }

    @Override
    public void showTimeLineWindow(List documentVersions) {
        timeLineWindow = new TimeLineWindow(securityContext, messageHelper, eventBus, userHelper, documentVersions);
        UI.getCurrent().addWindow(timeLineWindow);
        timeLineWindow.center();
        timeLineWindow.focus();
    }

    @Override
    public void updateTimeLineWindow(List documentVersions) {
        timeLineWindow.updateVersions(documentVersions);
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
    public void displayComparison(HashMap<Integer, Object> htmlResult) {
        eventBus.post(new ComparisonResponseEvent(htmlResult, LeosCategory.BILL.name().toLowerCase()));
    }

    @Override
    public void setTocAndAncestors(List<TableOfContentItemVO> tocItemList, List<String> elementAncestorsIds) {
        eventBus.post(new FetchCrossRefTocResponseEvent(new TocAndAncestorsVO(tocItemList, elementAncestorsIds, messageHelper)));
    }

    @Override
    public void setElement(String elementId, String elementType, String elementContent) {
        eventBus.post(new FetchElementResponseEvent(elementId, elementType, elementContent));
    }

    void init() {
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
        MenuBarComponent menuBarComponent = webAppContext.getBean(MenuBarComponent.class);
        menuBarComponent.setWidth("73px");
        docLayout.addComponent(menuBarComponent);
        docLayout.setComponentAlignment(menuBarComponent, Alignment.TOP_RIGHT);
        return docLayout;
    }

    private void buildDocumentPane() {
        LOG.debug("Building document pane...");

        // Add Legal Text Pane and add content and toc in it
        legalTextPaneComponent = new LegalTextPaneComponent(eventBus, messageHelper, cfgHelper, securityContext);
        addComponent(legalTextPaneComponent);
        setExpandRatio(legalTextPaneComponent, 1.0f);
    }

    @Override
    public void setUserGuidance(String userGuidance) {
        eventBus.post(new FetchUserGuidanceResponse(userGuidance));
    }

    @Override
    public void sendUserPermissions(List<LeosPermission> userPermissions) {
        eventBus.post(new FetchUserPermissionsResponse(userPermissions));
    }

    @Override
    public void displaySearchedContent(String content) {
        eventBus.post(new SearchActResponseEvent(content));
    }

    @Override
    public void closeImportWindow() {
        if (importWindow != null) {
            importWindow.close();
        }
    }

    @Override
    public void setPermissions(DocumentVO bill) {
        legalTextPaneComponent.setPermissions(bill, instanceTypeResolver.getInstanceType());
    }

    @Subscribe
    public void fetchToken(SecurityTokenRequest event) {
        eventBus.post(new SecurityTokenResponse(securityContext.getAnnotateToken(event.getUrl())));
    }

    @Override
    public void scrollTo(String elementId) {
    	com.vaadin.ui.JavaScript.getCurrent().execute("LEOS.scrollTo('" + elementId + "');");
    }

    @Override
    public void setReferenceLabel(String referenceLabels) {
        eventBus.post(new ReferenceLabelResponseEvent(referenceLabels));
    }

    @Override
    public void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        this.getUI().access(() -> {
            legalTextPaneComponent.updateTocUserCoEditionInfo(coEditionVos, presenterId);
            legalTextPaneComponent.getUserCoEditionExtension().updateUserCoEditionInfo(coEditionVos, presenterId);
        });
    }

    @Override
    public void displayDocumentUpdatedByCoEditorWarning() {
        this.getUI().access(() -> {
            legalTextPaneComponent.displayDocumentUpdatedByCoEditorWarning();
        });    
    }

    @Override
    public void checkElementCoEdition(List<CoEditionVO> coEditionVos, User user, final String elementId, final String elementTagName, final Action action, final Object actionEvent) {
        StringBuilder coEditorsList = new StringBuilder();
        coEditionVos.stream().filter((x) -> InfoType.ELEMENT_INFO.equals(x.getInfoType()) && x.getElementId().equals(elementId))
                .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime)).forEach(x -> {
                    StringBuilder userDescription = new StringBuilder();
                    if (!x.getUserLoginName().equals(user.getLogin())) {
                        userDescription.append("<a href=\"")
                                .append(StringUtils.isEmpty(x.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(x.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                        : new StringBuilder("mailto:").append(x.getUserEmail()).toString()))
                                .append("\">").append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity())
                                .append(")</a>");
                    } else {
                        userDescription.append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity()).append(")");
                    }
                    coEditorsList.append("&nbsp;&nbsp;-&nbsp;")
                            .append(messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(x.getEditionTime()))))
                            .append("<br>");
                });
        if (!StringUtils.isEmpty(coEditorsList)) {
            confirmCoEdition(coEditorsList.toString(), elementId, action, actionEvent);
        } else {
            eventBus.post(actionEvent);
        }
    }

    private void confirmCoEdition(String coEditorsList, String elementId, Action action, Object actionEvent) {
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("coedition." + action.getValue() + ".element.confirmation.title"),
                messageHelper.getMessage("coedition." + action.getValue() + ".element.confirmation.message", coEditorsList),
                messageHelper.getMessage("coedition." + action.getValue() + ".element.confirmation.confirm"),
                messageHelper.getMessage("coedition." + action.getValue() + ".element.confirmation.cancel"), null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
        confirmDialog.getContent().setHeightUndefined();
        confirmDialog.setHeightUndefined();
        confirmDialog.show(getUI(), dialog -> {
            if (dialog.isConfirmed()) {
                eventBus.post(actionEvent);
            } else {
                eventBus.post(new CancelActionElementRequestEvent(elementId));
            }
        }, true);
    }

    @Override
    public void showAlertDialog(String messageKey) {
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage(messageKey + ".alert.title"),
                messageHelper.getMessage(messageKey + ".alert.message"),
                messageHelper.getMessage(messageKey + ".alert.confirm"), null, null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
        confirmDialog.getContent().setHeightUndefined();
        confirmDialog.setHeightUndefined();
        confirmDialog.getCancelButton().setVisible(false);
        confirmDialog.show(getUI(), dialog -> {}, true);
    }
    
    @Override
    public void setDownloadStreamResource(Resource downloadstreamResource) {
    }
}