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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.TreeData;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.component.AccordionPane;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.component.versions.VersionsTab;
import eu.europa.ec.leos.ui.extension.UserCoEditionExtension;
import eu.europa.ec.leos.ui.view.ScreenLayoutHelper;
import eu.europa.ec.leos.ui.view.TriFunction;
import eu.europa.ec.leos.ui.window.toc.TocEditor;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.ui.component.actions.LegalTextActionsMenuBar;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/*------------------------------------------------------------------------

View

---------------------------------------------------------------------------------------
| TableOfContentComponent   ||   LegalTextComponent      ||  MarkedTextComponent      ||
| Mandatory                 ||   mandatory               ||  Optional                 ||
|                           ||                           ||                           ||
|                           ||                           ||                           ||
|            tocSplitter--> ||         contentSplitter-->||                           ||
---------------------------------------------------------------------------------------

ComponentArrangement in horizontal split panel
[ [TableOfContentComponent|LegalTextComponent] | MarkedTextComponent]
-------------------------------------------------------------------------*/
public class LegalTextPaneComponent extends CustomComponent {

    private static final long serialVersionUID = 2667950258861202550L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextPaneComponent.class);

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private ConfigurationHelper cfgHelper;
    private SecurityContext securityContext;
    private Provider<StructureContext> structureContextProvider;
    private PackageService packageService;

    private LegalTextComponent legalTextComponent;
    private TableOfContentComponent tableOfContentComponent;
    private VersionsTab<Bill> versionsTab;
    private LegalTextActionsMenuBar legalTextActionMenuBar;
    private AccordionPane accordionPane;
    private Accordion accordion;    

    public ScreenLayoutHelper screenLayoutHelper;

    private HorizontalSplitPanel tocSplitter = new HorizontalSplitPanel();
    private HorizontalSplitPanel contentSplitter = new HorizontalSplitPanel();
    private TocEditor tocEditor;

    public LegalTextPaneComponent(EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper, SecurityContext securityContext,
                                  LegalTextActionsMenuBar legalTextActionsMenuBar, TocEditor tocEditor, VersionsTab<Bill> versionsTab,
                                  Provider<StructureContext> structureContextProvider, PackageService packageService) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.cfgHelper = cfgHelper;
        contentSplitter.setId(ScreenLayoutHelper.CONTENT_SPLITTER);
        tocSplitter.setId(ScreenLayoutHelper.TOC_SPLITTER);
        this.screenLayoutHelper = new ScreenLayoutHelper(eventBus, Arrays.asList(contentSplitter, tocSplitter));
        this.securityContext = securityContext;
        this.legalTextActionMenuBar = legalTextActionsMenuBar;
        this.tocEditor = tocEditor;
        this.versionsTab = versionsTab;
        this.structureContextProvider = structureContextProvider;
        this.packageService = packageService;
        buildLegalTextPane();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
        eventBus.register(screenLayoutHelper);
    }

    @Override
    public void detach() {
        eventBus.unregister(this);
        eventBus.unregister(screenLayoutHelper);
        super.detach();
    }

    private void buildLegalTextPane() {
        LOG.debug("Building legal text pane...");
        setId("legalTextPaneComponent");

        final VerticalLayout legalTextPane = new VerticalLayout();
        legalTextPane.setId("legalTextPane");
        legalTextPane.setSpacing(false);
        legalTextPane.setMargin(false);
        setCompositionRoot(legalTextPane);
        setSizeFull();
        legalTextPane.setSizeFull();

        createChildComponents();

        legalTextPane.addComponent(tocSplitter);
        legalTextPane.setExpandRatio(tocSplitter, 1.0f);
    }

    public void populateContent(final String docContent) {
        legalTextComponent.populateContent(docContent);
    }

    public LeosDisplayField getContent() {
        return legalTextComponent.getContent();
    }

    public void setTableOfContent(final TreeData<TableOfContentItemVO> treeTocData) {
        if(componentEnabled(AccordionPane.class)) {
            tableOfContentComponent.setTableOfContent(treeTocData);
        }
    }

    public void handleTocEditRequestEvent(List<TableOfContentItemVO> tocItemVoList, TocEditor tocEditor) {
        tableOfContentComponent.handleEditTocRequest(tocEditor);
        tableOfContentComponent.setTableOfContent(TableOfContentItemConverter.buildTocData(tocItemVoList));
    }

    public void setDocumentVersionInfo(VersionInfoVO versionInfoVO) {
        legalTextComponent.setDocumentVersionInfo(versionInfoVO);
    }
    
    private boolean componentEnabled(Class className){
        return screenLayoutHelper.isPaneEnabled(className);
    }

    private void createChildComponents() {
        // initialise the child components
        legalTextComponent = new LegalTextComponent(eventBus, messageHelper, cfgHelper, securityContext, legalTextActionMenuBar, structureContextProvider, packageService);
        addPaneToLayout(legalTextComponent, 1, true);

        accordionPane = new AccordionPane();
        accordionPane.setId("leosAccordionPane");
        accordionPane.setSizeFull();
        accordionPane.setMargin(false);
        accordionPane.setSpacing(false);
        accordion = new Accordion();
        accordion.setSizeFull();
        accordion.setId("leosAccordion");
        tableOfContentComponent = new TableOfContentComponent(messageHelper, eventBus, securityContext, cfgHelper, tocEditor, structureContextProvider);
        accordion.addTab(tableOfContentComponent, messageHelper.getMessage("toc.title"), VaadinIcons.CHEVRON_DOWN);
        accordion.addTab(versionsTab, messageHelper.getMessage("document.accordion.versions"), VaadinIcons.CHEVRON_RIGHT);
        accordionPane.addComponent(accordion);
        addPaneToLayout(accordionPane, 0, true);

        accordion.addListener(event -> {
            final Component selected = ((Accordion) event.getSource()).getSelectedTab();
            for (int i = 0; i < accordion.getComponentCount(); i++) {
                TabSheet.Tab tab = accordion.getTab(i);
                if(tab.getComponent().getClass().equals(selected.getClass())){
                    tab.setIcon(VaadinIcons.CHEVRON_DOWN);
                } else {
                    tab.setIcon(VaadinIcons.CHEVRON_RIGHT);
                }
            }
        });
    }
    
    public void addPaneToLayout(ContentPane pane, Integer position, Boolean isEnabled) {
        screenLayoutHelper.addPane(pane, position, isEnabled);
    }
    
    public void layoutChildComponents() {
        screenLayoutHelper.layoutComponents();
    }

    public void changeComponentLayout(ColumnPosition position, Class componentClass) {
        screenLayoutHelper.changePosition(position, componentClass);
    }
    
    public UserCoEditionExtension<LeosDisplayField, String> getUserCoEditionExtension() {
        return legalTextComponent.getUserCoEditionExtension();
    }

    public void setPermissions(DocumentVO bill, String instanceType){
        boolean enableUpdate = securityContext.hasPermission(bill, LeosPermission.CAN_UPDATE);
        tableOfContentComponent.setPermissions(enableUpdate);
        legalTextComponent.setPermissions(enableUpdate, instanceType, bill);
    }
    
    public void updateTocUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        if (componentEnabled(AccordionPane.class)) {
            tableOfContentComponent.updateUserCoEditionInfo(coEditionVos, presenterId);
        }
    }
    
    public void displayDocumentUpdatedByCoEditorWarning() {
        legalTextComponent.displayDocumentUpdatedByCoEditorWarning();
    }
    
    public boolean isTocEnabled() {
        return screenLayoutHelper.isTocPaneEnabled();
    }

    public void setDataFunctions(List<VersionVO> allVersions,
                         BiFunction<Integer, Integer, List<Bill>> majorVersionsFn, Supplier<Integer> countMajorVersionsFn,
                         TriFunction<String, Integer, Integer, List<Bill>> minorVersionsFn, Function<String, Integer> countMinorVersionsFn,
                         BiFunction<Integer, Integer, List<Bill>> recentChangesFn, Supplier<Integer> countRecentChangesFn,
                         boolean isComparisonAvailable) {
        versionsTab.setDataFunctions(allVersions, minorVersionsFn, countMinorVersionsFn, recentChangesFn, countRecentChangesFn, isComparisonAvailable);
    }

    public void removeAnnotateExtension() {
        legalTextComponent.removeAnnotateExtension();
    }

}