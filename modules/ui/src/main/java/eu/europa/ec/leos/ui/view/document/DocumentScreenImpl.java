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
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.component.AccordionPane;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.component.doubleCompare.DoubleComparisonComponent;
import eu.europa.ec.leos.ui.component.markedText.MarkedTextComponent;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.component.versions.VersionsTab;
import eu.europa.ec.leos.ui.event.security.SecurityTokenRequest;
import eu.europa.ec.leos.ui.event.security.SecurityTokenResponse;
import eu.europa.ec.leos.ui.view.ComparisonDisplayMode;
import eu.europa.ec.leos.ui.view.TriFunction;
import eu.europa.ec.leos.ui.window.milestone.MilestoneExplorer;
import eu.europa.ec.leos.ui.window.toc.TocEditor;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.component.ComparisonResponseEvent;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.view.document.CancelActionElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.CheckDeleteLastEditingTypeEvent;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent.Action;
import eu.europa.ec.leos.web.event.view.document.FetchCrossRefTocResponseEvent;
import eu.europa.ec.leos.web.event.view.document.FetchElementResponseEvent;
import eu.europa.ec.leos.web.event.view.document.FetchUserGuidanceResponse;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsResponse;
import eu.europa.ec.leos.web.event.view.document.InstanceTypeResolver;
import eu.europa.ec.leos.web.event.view.document.ReferenceLabelResponseEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshElementEvent;
import eu.europa.ec.leos.web.event.view.document.SearchActResponseEvent;
import eu.europa.ec.leos.web.model.TocAndAncestorsVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.MenuBarComponent;
import eu.europa.ec.leos.web.ui.component.actions.LegalTextActionsMenuBar;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import eu.europa.ec.leos.web.ui.window.ImportWindow;
import eu.europa.ec.leos.web.ui.window.IntermediateVersionWindow;
import eu.europa.ec.leos.web.ui.window.TimeLineWindow;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.vaadin.dialogs.ConfirmDialog;

import javax.inject.Provider;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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

    private TimeLineWindow<Bill> timeLineWindow;
    private ImportWindow importWindow;
    protected TocEditor tocEditor;
    protected VersionsTab<Bill> versionsTab;
    protected LegalTextPaneComponent legalTextPaneComponent;
    protected LegalTextActionsMenuBar legalTextActionMenuBar;
    private MenuBarComponent menuBarComponent;
    private LeosPermissionAuthorityMapHelper authorityMapHelper;
    protected ComparisonComponent<Bill> comparisonComponent;
    protected Provider<StructureContext> structureContextProvider;
    private PackageService packageService;

    @Value("${leos.coedition.sip.enabled}")
    private boolean coEditionSipEnabled;

    @Value("${leos.coedition.sip.domain}")
    private String coEditionSipDomain;

    @Autowired
    DocumentScreenImpl(UserHelper userHelper, SecurityContext securityContext, EventBus eventBus, ConfigurationHelper cfgHelper,
                       MessageHelper messageHelper, TocEditor tocEditor, InstanceTypeResolver instanceTypeResolver,
                       MenuBarComponent menuBarComponent, LeosPermissionAuthorityMapHelper authorityMapHelper, LegalTextActionsMenuBar legalTextActionMenuBar,
                       ComparisonComponent<Bill> comparisonComponent, VersionsTab<Bill> versionsTab, Provider<StructureContext> structureContextProvider, PackageService packageService) {
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
        Validate.notNull(menuBarComponent, "menuBarComponent must not be null!");
        this.menuBarComponent = menuBarComponent;
        Validate.notNull(authorityMapHelper, "authorityMapHelper must not be null!");
        this.authorityMapHelper = authorityMapHelper;
        Validate.notNull(legalTextActionMenuBar, "legalTextActionMenuBar must not be null!");
        this.legalTextActionMenuBar = legalTextActionMenuBar;
        Validate.notNull(comparisonComponent, "comparisonComponent must not be null!");
        this.comparisonComponent = comparisonComponent;
        Validate.notNull(versionsTab, "versionsTab must not be null!");
        this.versionsTab = versionsTab;
        Validate.notNull(structureContextProvider, "structureContextProvider must not be null!");
        this.structureContextProvider = structureContextProvider;
        Validate.notNull(structureContextProvider, "packageService must not be null!");
        this.packageService = packageService;
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

    void init() {
        setSizeFull();
        setMargin(false);
        setSpacing(false);
        addStyleName("leos-document-layout");

        // create layout for document title
        buildTitleAndMenubar();
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
        menuBarComponent.setWidth("73px");
        docLayout.addComponent(menuBarComponent);
        docLayout.setComponentAlignment(menuBarComponent, Alignment.TOP_RIGHT);

        return docLayout;
    }

    protected void buildDocumentPane() {
        LOG.debug("Building document pane...");
        // Add Legal Text Pane and add content and toc in it
        legalTextPaneComponent = new LegalTextPaneComponent(eventBus, messageHelper, cfgHelper, securityContext, legalTextActionMenuBar, tocEditor, versionsTab, structureContextProvider, packageService);
        addComponent(legalTextPaneComponent);
        setExpandRatio(legalTextPaneComponent, 1.0f);
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

    public abstract void showVersion(String content, String versionInfo);
    
    public abstract void populateMarkedContent(String comparedContent, String versionInfo);

    public abstract void populateDoubleComparisonContent(String comparedContent, String versionInfo);
    
    @Override
    public void setToc(final List<TableOfContentItemVO> tableOfContentItemVoList) {
        TreeData<TableOfContentItemVO> treeTocData = TableOfContentItemConverter.buildTocData(tableOfContentItemVoList);
        legalTextPaneComponent.setTableOfContent(treeTocData);
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
        timeLineWindow = new TimeLineWindow<Bill>(securityContext, messageHelper, eventBus, userHelper, documentVersions);
        UI.getCurrent().addWindow(timeLineWindow);
        timeLineWindow.center();
        timeLineWindow.focus();
    }
    
    @Override
    public void updateTimeLineWindow(List documentVersions) {
        if (timeLineWindow != null) { //TODO added avoid NPE. Until Timeline will be cleaned up
            timeLineWindow.updateVersions(documentVersions);
            timeLineWindow.focus();
        }
    }

    @Override
    public void showIntermediateVersionWindow() {
        IntermediateVersionWindow intermediateVersionWindow = new IntermediateVersionWindow(messageHelper, eventBus);
        UI.getCurrent().addWindow(intermediateVersionWindow);
        intermediateVersionWindow.center();
        intermediateVersionWindow.focus();
    }

    @Override
    public void showImportWindow() {
        importWindow = new ImportWindow(messageHelper, eventBus);
        UI.getCurrent().addWindow(importWindow);
        importWindow.center();
        importWindow.focus();
    }

    @Override
    public void displayComparison(HashMap<ComparisonDisplayMode, Object> htmlResult) {
        eventBus.post(new ComparisonResponseEvent(htmlResult, LeosCategory.BILL.name().toLowerCase()));
    }

    @Override
    public void setTocAndAncestors(Map<String, List<TableOfContentItemVO>> tocItemList, List<String> elementAncestorsIds) {
        eventBus.post(new FetchCrossRefTocResponseEvent(new TocAndAncestorsVO(tocItemList, elementAncestorsIds, messageHelper)));
    }

    @Override
    public void setElement(String elementId, String elementType, String elementContent, String documentRef) {
        eventBus.post(new FetchElementResponseEvent(elementId, elementType, elementContent, documentRef));
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
    public void setReferenceLabel(String referenceLabels, String documentRef) {
        eventBus.post(new ReferenceLabelResponseEvent(referenceLabels, documentRef));
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
            if (action == Action.DELETE) {
                eventBus.post(new CheckDeleteLastEditingTypeEvent(elementId, actionEvent));
            } else {
                eventBus.post(actionEvent);
            }
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
                if (action == Action.DELETE) {
                    eventBus.post(new CheckDeleteLastEditingTypeEvent(elementId, actionEvent));
                } else {
                    eventBus.post(actionEvent);
                }
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

    protected void changeLayout(LayoutChangeRequestEvent event, Object obj) {
        if (obj instanceof MarkedTextComponent || obj instanceof DoubleComparisonComponent) {
            if (event.getOriginatingComponent() == ComparisonComponent.class) {
                if (!event.getPosition().equals(ColumnPosition.OFF)) {
                    comparisonComponent.setContent((ContentPane)obj);
                } else {
                    obj = null;
                    comparisonComponent.setContent(null);
                }
            }
            else if (event.getOriginatingComponent() == AccordionPane.class) {
                JavaScript.getCurrent().execute("$(function(){document.defaultView.dispatchEvent(new Event('resize'));})");
            }
            legalTextPaneComponent.changeComponentLayout(event.getPosition(), event.getOriginatingComponent());
        }
    }
    
    @Override
    public boolean isTocEnabled() {
        return legalTextPaneComponent.isTocEnabled();
    }
    
    @Override
    public void setDataFunctions(List<VersionVO> allVersions,
                                 BiFunction<Integer, Integer, List<Bill>> majorVersionsFn, Supplier<Integer> countMajorVersionsFn,
                                 TriFunction<String, Integer, Integer, List<Bill>> minorVersionsFn, Function<String, Integer> countMinorVersionsFn,
                                 BiFunction<Integer, Integer, List<Bill>> recentChangesFn, Supplier<Integer> countRecentChangesFn) {
        legalTextPaneComponent.setDataFunctions(allVersions, majorVersionsFn, countMajorVersionsFn, minorVersionsFn,
                countMinorVersionsFn, recentChangesFn, countRecentChangesFn, true);
    }
    
    public void refreshVersions(List<VersionVO> allVersions, boolean isComparisonMode) {
        versionsTab.refreshVersions(allVersions, isComparisonMode);
    }

    @Override
    public void showMilestoneExplorer(LegDocument legDocument, String milestoneTitle) {
        legalTextPaneComponent.removeAnnotateExtension();
        MilestoneExplorer milestoneExplorer = new MilestoneExplorer(legDocument, milestoneTitle, messageHelper, eventBus, cfgHelper, securityContext, userHelper);
        UI.getCurrent().addWindow(milestoneExplorer);
        milestoneExplorer.center();
        milestoneExplorer.focus();
    }

    @Override
    public boolean isComparisonComponentVisible() {
        return comparisonComponent != null && comparisonComponent.getParent() != null;
    }

}