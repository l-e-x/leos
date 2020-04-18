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
package eu.europa.ec.leos.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.data.Binder;
import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.Result;
import com.vaadin.data.TreeData;
import com.vaadin.data.ValueContext;
import com.vaadin.data.ValueProvider;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.DescriptionGenerator;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.StyleGenerator;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.GridDragStartListener;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.declarative.Design;
import com.vaadin.ui.renderers.HtmlRenderer;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.ActionType;
import eu.europa.ec.leos.model.action.CheckinElement;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.model.annex.AnnexStructureType;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.event.StateChangeEvent;
import eu.europa.ec.leos.ui.event.toc.CloseEditTocEvent;
import eu.europa.ec.leos.ui.event.toc.DisableEditTocEvent;
import eu.europa.ec.leos.ui.event.toc.ExpandTocSliderPanel;
import eu.europa.ec.leos.ui.event.toc.InlineTocCloseRequestEvent;
import eu.europa.ec.leos.ui.event.toc.InlineTocEditRequestEvent;
import eu.europa.ec.leos.ui.event.toc.RefreshTocEvent;
import eu.europa.ec.leos.ui.event.toc.SaveTocRequestEvent;
import eu.europa.ec.leos.ui.event.toc.TocChangedEvent;
import eu.europa.ec.leos.ui.event.toc.TocResizedEvent;
import eu.europa.ec.leos.ui.extension.dndscroll.TreeGridScrollDropTargetExtension;
import eu.europa.ec.leos.ui.window.toc.MultiSelectTreeGrid;
import eu.europa.ec.leos.ui.window.toc.TocEditor;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.OptionsType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.vo.toc.TocItemUtils;
import eu.europa.ec.leos.web.event.view.document.CheckDeleteLastEditingTypeEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.teemusa.gridextensions.client.tableselection.TableSelectionState.TableSelectionMode;
import org.vaadin.teemusa.gridextensions.tableselection.TableSelectionModel;

import javax.inject.Provider;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.support.TableOfContentHelper.DEFAULT_CAPTION_MAX_SIZE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.EC;

@SpringComponent
@Scope("prototype")
@DesignRoot("TableOfContentDesign.html")
public class TableOfContentComponent extends VerticalLayout implements ContentPane {

    private static final long serialVersionUID = -4752609567267410718L;
    private static final Logger LOG = LoggerFactory.getLogger(TableOfContentComponent.class);
    public static SimpleDateFormat dataFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm");
    private static final float TOC_MIN_WIDTH = 190F;

    private ConfigurationHelper cfgHelper;
    private MessageHelper messageHelper;
    private EventBus eventBus;
    protected Button tocExpandCollapseButton;
    protected TreeAction treeAction;
    protected Button inlineTocEditButton;
    protected Label spacerLabel;
    protected Label tocUserCoEdition;
    protected MultiSelectTreeGrid<TableOfContentItemVO> tocTree;
    protected Label statusLabel;
    protected MenuBar menuBar;
    protected MenuItem tocExpandCollapseMenuItem;
    protected MenuItem inlineTocEditMenuItem;
    protected MenuItem separatorMenuItem;
    protected MenuItem saveMenuItem;
    protected MenuItem saveCloseMenuItem;
    protected MenuItem cancelMenuItem;
    protected boolean expandedNavigationPane = true;

    private Registration dropTargetRegistration;
    private TreeGridScrollDropTargetExtension<TableOfContentItemVO> dropTarget;
    private User user;
    private boolean updateEnabled = true;
    private boolean editionEnabled = false;
    private boolean editorPanelOpened = false;
    private boolean userOriginated = false;
    private final String STATUS_STYLE = "leos-toc-tree-status";
    private Boolean dataChanged = false;
    private VerticalLayout itemEditorLayout = new VerticalLayout(); // dummy implementation to avoid design exception
    private Button saveButton;
    private Button saveCloseButton;
    private Button cancelButton;
    private boolean isToggledByUser;
    private TocEditor tocEditor;
    private TextField itemTypeField = new TextField();
    private TreeDataProvider<TableOfContentItemVO> dataProvider;
    private Registration dataProviderRegistration;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Set<CheckinElement> tocChangedElements = new HashSet<>();
    private Set<String> expandedNodes;

    private Provider<StructureContext> structureContextProvider;
    private List<TocItem> tocItems;
    private List<NumberingConfig> numberingConfigs;

    public TableOfContentComponent() {
    }
    
    public enum TreeAction {
        EXPAND,
        COLLAPSE
    }

    private boolean coEditionSipEnabled;
    private String coEditionSipDomain;

    @Autowired
    public TableOfContentComponent(final MessageHelper messageHelper, final EventBus eventBus, final SecurityContext securityContext,
                                   final ConfigurationHelper cfgHelper, final TocEditor tocEditor, Provider<StructureContext> structureContextProvider) {
        LOG.trace("Initializing table of content...");
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.user = securityContext.getUser();
        Validate.notNull(user, "User must not be null!");
        Validate.notNull(cfgHelper, "cfgHelper must not be null!");
        this.coEditionSipEnabled = Boolean.parseBoolean(cfgHelper.getProperty("leos.coedition.sip.enabled"));
        this.coEditionSipDomain = cfgHelper.getProperty("leos.coedition.sip.domain");
        this.tocEditor = tocEditor;
        this.cfgHelper = cfgHelper;
        this.structureContextProvider = structureContextProvider;

        Design.read(this);
        buildToc();
        this.checkDeleteLastEditingTypeConsumer = new CheckDeleteLastEditingTypeConsumer(tocTree, messageHelper, eventBus);
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        eventBus.unregister(this);
        super.detach();
    }

    private void buildToc() {
        LOG.debug("Building table of contents...");
        setId("tableOfContentComponent");
        buildTocToolbar();
        buildTocTree();
        buildItemEditorLayout();
    }

    private void buildTocToolbar() {
        LOG.debug("Building table of contents toolbar...");
        // create toc expand/collapse button
        tocExpandCollapseButton();
        //inline toc edit button
        inlineTocEditButton();
        // spacer label will use all available space
        spacerLabel.setContentMode(ContentMode.HTML);
        spacerLabel.setValue("&nbsp;");
        tocSaveButton();
        tocSaveCloseButton();
        tocCancelButton();
        tocMenuBar();
    }

    private void buildItemEditorLayout() {
        itemEditorLayout.addStyleName("leos-bottom-slider-panel");
        itemEditorLayout.setSpacing(true);
        itemEditorLayout.setMargin(false);
    
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidth(100, Unit.PERCENTAGE);
        toolbar.setSpacing(true);
        toolbar.setMargin(false);
        toolbar.setStyleName("leos-viewdoc-tocbar");
        toolbar.addStyleName("leos-slider-toolbar");
        Label sliderLabel = new Label(messageHelper.getMessage("toc.edit.window.item.selected"), ContentMode.HTML);
        toolbar.addComponent(sliderLabel);
        itemEditorLayout.addComponent(toolbar);

        final Editor tocItemEditor = new Editor(tocEditor);
        tocItemEditor.addStyleName("leos-toc-editor");
        tocItemEditor.setSpacing(true);
        tocItemEditor.setMargin(false);
        tocTree.asMultiSelect().addValueChangeListener(tocItemEditor);
        
        itemEditorLayout.addComponent(tocItemEditor);
    }
    
    // create toc expand/collapse button
    private void tocExpandCollapseButton() {
        treeAction = TreeAction.EXPAND;
        tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.expand.button.description"));
        tocExpandCollapseButton.setIcon(LeosTheme.LEOS_TOC_TREE_ICON_16);
        tocExpandCollapseButton.addClickListener(event -> toggleTreeItems());
    }

    private void toggleTreeItems() {
        if (TreeAction.EXPAND.equals(treeAction)) {
            LOG.debug("Expanding the tree...");
            expandFullTree(tocTree.getTreeData().getRootItems()); //expand items recursively
            treeAction = TreeAction.COLLAPSE;
            tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.collapse.button.description"));
            tocExpandCollapseMenuItem.setText(tocExpandCollapseButton.getDescription());
        } else if (TreeAction.COLLAPSE.equals(treeAction)) {
            LOG.debug("Collapsing the tree...");
            tocTree.collapse(tocTree.getTreeData().getRootItems());
            treeAction = TreeAction.EXPAND;
            tocExpandCollapseButton.setDescription(messageHelper.getMessage("toc.expand.button.description"));
            tocExpandCollapseMenuItem.setText(tocExpandCollapseButton.getDescription());
        } else {
            LOG.debug("Ignoring unknown tree control button click action! [action={}]", treeAction);
        }
    }

    private void expandFullTree(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            if (!item.getChildItems().isEmpty()) {
                tocTree.expand(item);
                expandFullTree(item.getChildItems());
            }
        });
    }

    private void expandTree(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            if (!item.getChildItems().isEmpty()) {
                TableOfContentItemVO tocItem = editionEnabled ? tocEditor.getSimplifiedTocItem(item) : item;
                if (expandedNodes.contains(tocItem.getId())) {
                    tocTree.expand(item);
                } else {
                    tocTree.collapse(item);
                }
                expandTree(item.getChildItems());
            }
        });
    }

    private void expandDefaultTreeNodes(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            if (!item.getChildItems().isEmpty() && item.getTocItem().isExpandedByDefault()) {
                tocTree.expand(item);
                expandDefaultTreeNodes(item.getChildItems());
            }
        });
    }

    // Toc edit button
    private void inlineTocEditButton() {
        inlineTocEditButton.setDescription(messageHelper.getMessage("toc.inline.edit.button.description"));
        inlineTocEditButton.setIcon(LeosTheme.LEOS_TOC_EDIT_ICON_16);
        inlineTocEditButton.addClickListener(event -> {
           if (!editionEnabled) {
               if (isTocCoEditionActive()) {
                   ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                           messageHelper.getMessage("coedition.edit.element.confirmation.title"),
                           messageHelper.getMessage("coedition.edit.element.confirmation.message", tocUserCoEdition.getDescription().replace("leos-toc-user-coedition-lync", "")),
                           messageHelper.getMessage("coedition.edit.element.confirmation.confirm"),
                           messageHelper.getMessage("coedition.edit.element.confirmation.cancel"), null);
                   confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
                   confirmDialog.getContent().setHeightUndefined();
                   confirmDialog.setHeightUndefined();
                   confirmDialog.show(getUI(), dialog -> {
                       if (dialog.isConfirmed()) {
                           enableTocEdition();
                       }
                   }, true);
               } else {
                   enableTocEdition();
               }
           } else {
               disableTocEdition();
           }
        });
    }

    private void enableTocEdition() {
        editionEnabled = true;
        tocTree.addStyleName("leos-toc-tree-editable");
        inlineTocEditButton.setEnabled(false);
        inlineTocEditMenuItem.setEnabled(false);
        setVisibility();
        eventBus.post(new InlineTocEditRequestEvent());
    }

    private void disableTocEdition() {
        if (dataChanged) {
            ConfirmDialog.show(getUI(),
                    messageHelper.getMessage("edit.close.not.saved.title"),
                    messageHelper.getMessage("edit.close.not.saved.message"),
                    messageHelper.getMessage("edit.close.not.saved.confirm"),
                    messageHelper.getMessage("edit.close.not.saved.close"),
                    new ConfirmDialog.Listener() {
                        private static final long serialVersionUID = -1441968814274639475L;
                        public void onClose(ConfirmDialog dialog) {
                            if (dialog.isConfirmed()) {
                                onSaveToc(TocMode.SIMPLIFIED);
                                closeTocEditor();
                                // no need to refresh the toc since save will fire the full refresh document
                            } else if (dialog.isCanceled()) {
                                closeTocEditor();
                                eventBus.post(new RefreshTocEvent(TocMode.SIMPLIFIED));
                            }
                        }
                    });
        } else {
            closeTocEditor();
            eventBus.post(new RefreshDocumentEvent()); //to refresh the annotations
        }
    }

    @Subscribe
    void closeEditToc(CloseEditTocEvent event) {
        if (dataChanged) {
            ConfirmDialog.show(getUI(),
                    messageHelper.getMessage("edit.close.not.saved.title"),
                    messageHelper.getMessage("edit.close.not.saved.message"),
                    messageHelper.getMessage("edit.close.not.saved.confirm"),
                    messageHelper.getMessage("edit.close.not.saved.close"),
                    new ConfirmDialog.Listener() {
                        private static final long serialVersionUID = -1741968814231539431L;
                        public void onClose(ConfirmDialog dialog) {
                            if (dialog.isConfirmed() || dialog.isCanceled()) {
                                if (dialog.isConfirmed()) {
                                    eventBus.post(new SaveTocRequestEvent(TableOfContentItemConverter
                                            .buildTocItemVOList(tocTree.getTreeData()), tocChangedElements));
                                }
                                eventBus.post(new InlineTocCloseRequestEvent());
                                eventBus.post(new CloseDocumentEvent());
                            }
                        }
                    });
        } else {
            eventBus.post(new InlineTocCloseRequestEvent());
            eventBus.post(new CloseDocumentEvent());
        }
    }

    private void closeTocEditor() {
        editionEnabled = false;
        editorPanelOpened = false;
        tocTree.removeStyleName("leos-toc-tree-editable");
        enableSave(false);
        inlineTocEditButton.setEnabled(true);
        inlineTocEditMenuItem.setEnabled(true);
        dropTargetRegistration.remove();
        dropTarget.setDropEffect(DropEffect.NONE);
        dataProviderRegistration.remove();
        tocEditor.setTocTreeDataFilter(false, dataProvider);
        setVisibility();
        eventBus.post(new DisableEditTocEvent());
        removeComponent(itemEditorLayout);
    }

    private void setVisibility() {
        menuBar.setVisible(!expandedNavigationPane);
        tocExpandCollapseButton.setVisible(expandedNavigationPane);
        inlineTocEditButton.setVisible(updateEnabled && expandedNavigationPane);
        saveButton.setVisible(editionEnabled && expandedNavigationPane);
        saveCloseButton.setVisible(editionEnabled && expandedNavigationPane);
        cancelButton.setVisible(editionEnabled && expandedNavigationPane);
        inlineTocEditMenuItem.setVisible(updateEnabled);
        separatorMenuItem.setVisible(editionEnabled);
        saveMenuItem.setVisible(editionEnabled);
        saveCloseMenuItem.setVisible(editionEnabled);
        cancelMenuItem.setVisible(editionEnabled);
    }

    private MenuItem addMenuItem(MenuItem parentMenuItem, Button button) {
        MenuItem menuItem = parentMenuItem.addItem(button.getDescription(), button.getIcon(), item -> button.click());
        menuItem.setStyleName("leos-actions-sub-menu-item");
        menuItem.setEnabled(button.isEnabled());
        return menuItem;
    }

    private void tocMenuBar() {
        menuBar.addStyleName("leos-actions-menu");
        menuBar.setEnabled(true);

        MenuItem mainMenuItem = menuBar.addItem("", LeosTheme.LEOS_HAMBURGUER_16, null);
        mainMenuItem.setStyleName("leos-actions-menu-selector");
        mainMenuItem.setDescription(messageHelper.getMessage("toc.title"));

        tocExpandCollapseMenuItem = addMenuItem(mainMenuItem, tocExpandCollapseButton);
        inlineTocEditMenuItem = addMenuItem(mainMenuItem, inlineTocEditButton);

        separatorMenuItem = mainMenuItem.addSeparator();

        saveMenuItem = addMenuItem(mainMenuItem, saveButton);
        saveCloseMenuItem = addMenuItem(mainMenuItem, saveCloseButton);
        cancelMenuItem = addMenuItem(mainMenuItem, cancelButton);

        setVisibility();
    }

    @Subscribe
    public void tocResized(TocResizedEvent event) {
        expandedNavigationPane = event.getPaneSize() > TOC_MIN_WIDTH;
        setVisibility();
    }

    private void tocSaveButton() {
        saveButton.setDescription(messageHelper.getMessage("leos.button.save"));
        saveButton.setIcon(LeosTheme.LEOS_TOC_SAVE_ICON_16);
        saveButton.setEnabled(false);
        saveButton.addClickListener(event -> onSaveToc(TocMode.NOT_SIMPLIFIED));
    }

    private void tocSaveCloseButton() {
        saveCloseButton.setDescription(messageHelper.getMessage("leos.button.save.and.close"));
        saveCloseButton.setIcon(LeosTheme.LEOS_TOC_SAVE_CLOSE_ICON_16);
        saveCloseButton.setEnabled(false);
        saveCloseButton.addClickListener(event -> {
            onSaveToc(TocMode.SIMPLIFIED);
            closeTocEditor();
        });
    }

    private void tocCancelButton() {
        cancelButton.setDescription(messageHelper.getMessage("leos.button.cancel"));
        cancelButton.setIcon(LeosTheme.LEOS_TOC_CANCEL_ICON_16);
        cancelButton.addClickListener(event -> disableTocEdition());
    }

    private void onSaveToc(TocMode mode) {
        enableSave(false);
        editionEnabled = TocMode.NOT_SIMPLIFIED.equals(mode);
        List<TableOfContentItemVO> list = TableOfContentItemConverter.buildTocItemVOList(tocTree.getTreeData());
        eventBus.post(new SaveTocRequestEvent(list, tocChangedElements));
        eventBus.post(new RefreshDocumentEvent(mode));
    }

    /**
     * On read only mode, it is populating only tocItems.
     * TocRules and numConfigs are populating on runtime through handleEditTocRequest() when user decides to edit.
     */
    public void handleEditTocRequest(TocEditor tocEditor) {
        dataProviderRegistration = dataProvider.addDataProviderListener(event -> {
            if (userOriginated) {
                LOG.debug("data changed logged in DataProviderListener");
                enableSave(true);
            }
        });
        tocEditor.setTocTreeDataFilter(true, dataProvider);

        dropTarget = new TreeGridScrollDropTargetExtension<>(tocTree, DropMode.ON_TOP_OR_BETWEEN);
        dropTarget.setDropEffect(DropEffect.MOVE);
        dropTargetRegistration = dropTarget.addTreeGridDropListener(new EditTocDropHandler(tocTree, messageHelper, eventBus, structureContextProvider.get().getTocRules(), tocEditor));

        eventBus.post(new ExpandTocSliderPanel());
        tocChangedElements = new HashSet<>();
        tocItems = structureContextProvider.get().getTocItems();
        numberingConfigs = structureContextProvider.get().getNumberingConfigs();
    }

    private void enableSave(boolean enable) {
        dataChanged = enable;
        saveButton.setEnabled(enable);
        saveMenuItem.setEnabled(enable);
        saveButton.setDisableOnClick(enable);
        saveCloseButton.setEnabled(enable);
        saveCloseMenuItem.setEnabled(enable);
        saveCloseButton.setDisableOnClick(enable);
    }

    @Subscribe
    public void handleTocChange(TocChangedEvent event) {
        statusLabel.setValue(messageHelper.getMessage(event.getMessage()));
        StringBuffer styleName = new StringBuffer(STATUS_STYLE)
                     .append(" ").append(event.getResult().toString().toLowerCase());
        statusLabel.setStyleName(styleName.toString());
        scheduler.schedule(new Runnable() {
            public void run() {
                statusLabel.setValue("&nbsp;");
                statusLabel.setStyleName(STATUS_STYLE);
            }
        }, 3, TimeUnit.SECONDS);
        
        if(TocChangedEvent.Result.SUCCESSFUL.equals(event.getResult())) {
            userOriginated = true;
            enableSave(true);
            
            if(event.getCheckinElements() != null && event.getCheckinElements().size() > 0){
                tocChangedElements.addAll(event.getCheckinElements());
            }
        } else {
            userOriginated = false;
        }
    }
    
    /**
     * Creates a CheckinElement object and add it to the list of the changed element
     */
    private void addElementInTocChangedList(ActionType actionType, String elementId, String elementTagName) {
        tocChangedElements.add(new CheckinElement(actionType, elementId, elementTagName));
    }
    
    private ValueProvider<TableOfContentItemVO, String> getColumnHtml() {
        return this::getColumnItemHtml;
    }

    private String getColumnItemHtml(TableOfContentItemVO tableOfContentItemVO) {
        StringBuilder itemHtml = new StringBuilder(StringUtils.stripEnd(TableOfContentHelper.buildItemCaption(tableOfContentItemVO, DEFAULT_CAPTION_MAX_SIZE, messageHelper), null));
        if (isItemCoEditionActive(tableOfContentItemVO)) {
            if (tableOfContentItemVO.getCoEditionVos().stream().anyMatch(x -> !x.getUserLoginName().equals(user.getLogin()))) {
                itemHtml.insert(0, "<span class=\"leos-toc-user-coedition Vaadin-Icons\">&#xe80d</span>");
            } else {
                itemHtml.insert(0, "<span class=\"leos-toc-user-coedition leos-toc-user-coedition-self-user Vaadin-Icons\">&#xe80d</span>");
            }
        }
        return itemHtml.toString();
    }

    private StyleGenerator<TableOfContentItemVO> getColumnStyle() {
        return TableOfContentHelper::getItemSoftStyle;
    }

    private DescriptionGenerator<TableOfContentItemVO> getColumnDescription() {
        return this::getColumnItemDescription;
    }

    private String getColumnItemDescription(TableOfContentItemVO tableOfContentItemVO) {
        StringBuilder itemDescription = new StringBuilder();
        for (CoEditionVO coEditionVO : tableOfContentItemVO.getCoEditionVos()) {
            StringBuilder userDescription = new StringBuilder();
            if (!coEditionVO.getUserLoginName().equals(user.getLogin())) {
                userDescription.append("<a class=\"leos-toc-user-coedition-lync\" href=\"")
                        .append(StringUtils.isEmpty(coEditionVO.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(coEditionVO.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                    : new StringBuilder("mailto:").append(coEditionVO.getUserEmail()).toString()))
                        .append("\">").append(coEditionVO.getUserName()).append(" (").append(StringUtils.isEmpty(coEditionVO.getEntity()) ? "-" : coEditionVO.getEntity())
                        .append(")</a>");
            } else {
                userDescription.append(coEditionVO.getUserName()).append(" (").append(StringUtils.isEmpty(coEditionVO.getEntity()) ? "-" : coEditionVO.getEntity()).append(")");
            }
            itemDescription.append(messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(coEditionVO.getEditionTime()))) + "<br>");
        }
        return itemDescription.toString();
    }

    /**
     * Build elements of tocLayout:
     * - tocTree
     * - statusLabel
     */
    private void buildTocTree() {
        TableSelectionModel<TableOfContentItemVO> model = new TableSelectionModel<>();
        model.setMode(TableSelectionMode.CTRL);
        model.addMultiSelectionListener(this::handleMultiSelection);

        tocTree.setSelectionModel(model);
        tocTree.addColumn(getColumnHtml(), new HtmlRenderer());
        tocTree.setStyleGenerator(getColumnStyle());
        tocTree.setDescriptionGenerator(getColumnDescription(), ContentMode.HTML);
        tocTree.removeHeaderRow(tocTree.getDefaultHeaderRow());
        tocTree.setSizeFull();

        dataProvider = new TreeDataProvider<TableOfContentItemVO>(tocTree.getTreeData());
        tocTree.setDataProvider(dataProvider);
        tocEditor.setTocTreeStyling(tocTree, dataProvider);
        tocEditor.setTocTreeDataFilter(false, dataProvider);

        tocTree.addExpandListener(event -> {
            TableOfContentItemVO expandedItem = editionEnabled ? tocEditor.getSimplifiedTocItem(event.getExpandedItem()) : event.getExpandedItem();
            if (expandedItem.getVtdIndex() != null) {
                expandedNodes.add(expandedItem.getId());
            }
        });

        tocTree.addCollapseListener(event -> {
            TableOfContentItemVO collapsedItem = editionEnabled ? tocEditor.getSimplifiedTocItem(event.getCollapsedItem()) : event.getCollapsedItem();
            if (collapsedItem.getVtdIndex() != null) {
                expandedNodes.remove(collapsedItem.getId());
            }
        });

        TreeGridDragSource<TableOfContentItemVO> dragSource = new TreeGridDragSource<>(tocTree);
        dragSource.setEffectAllowed(EffectAllowed.MOVE);
        dragSource.addGridDragStartListener((GridDragStartListener<TableOfContentItemVO>) event -> {
            List<TableOfContentItemVO> draggedItems = event.getDraggedItems();
            if (draggedItems.size() > 0) {
                dragSource.setDragData(draggedItems);
                userOriginated = true;
            }
        });
        dragSource.setDragDataGenerator("nodetype", tocVO -> {
            String dragType = tocVO.getTocItem().getAknTag().value();
            dragSource.clearDataTransferData();
            dragSource.setDataTransferData("nodetype", dragType);
            dragSource.setDataTransferText(dragType);
            return dragType;
        });

        statusLabel.setContentMode(ContentMode.HTML);
        statusLabel.setHeight(21, Unit.PIXELS);
    }

    private void handleMultiSelection(MultiSelectionEvent<TableOfContentItemVO> listener) {
        if (listener.isUserOriginated()) {
            Set<TableOfContentItemVO> newSelectedItems = listener.getAddedSelection();
            Set<TableOfContentItemVO> oldSelectedItems = listener.getOldSelection();

            if (!oldSelectedItems.isEmpty() && !newSelectedItems.isEmpty()) {
                TableOfContentItemVO oldSelectedItem = oldSelectedItems.iterator().next();
                TableOfContentItemVO newSelectedItem = newSelectedItems.iterator().next();

                if (!oldSelectedItem.getTocItem().getAknTag().value().equalsIgnoreCase(newSelectedItem.getTocItem().getAknTag().value())) {
                    tocTree.deselect(newSelectedItem);
                    final String newSelectedTocItem = messageHelper.getMessage("toc.item.type." + newSelectedItem.getTocItem().getAknTag().value().toLowerCase());
                    final String oldSelectedTocItem = messageHelper.getMessage("toc.item.type." + oldSelectedItem.getTocItem().getAknTag().value().toLowerCase());
                    final String statusMsg = messageHelper.getMessage("toc.item.cross.item.selection.error.message", newSelectedTocItem, oldSelectedTocItem);
                    eventBus.post(new TocChangedEvent(statusMsg, TocChangedEvent.Result.ERROR, null));
                }
            }
        }
        
        Optional<TableOfContentItemVO> itemId = listener.getFirstSelectedItem();
        LOG.trace("ToC selection changed: id='{}'", itemId);
        itemId.ifPresent(tableOfContentItemVO -> {
            String id = tableOfContentItemVO.getId();
            LOG.trace("ToC navigating to (id={})...", id);
            com.vaadin.ui.JavaScript.getCurrent().execute("LEOS.scrollTo('" + id + "');");
        });
    }

    public void setTableOfContent(TreeData<TableOfContentItemVO> newTocData) {
        userOriginated = false;
        TreeData<TableOfContentItemVO> tocData = tocTree.getTreeData();
        tocData.removeItem(null);//remove all old data
        tocData.addItems(newTocData.getRootItems(), TableOfContentItemVO::getChildItemsView);//add all new data
        tocTree.getDataProvider().refreshAll();
        if (expandedNodes != null) {
            expandTree(tocTree.getTreeData().getRootItems());
        } else {
            expandedNodes = new HashSet<>();
            expandDefaultTreeNodes(tocTree.getTreeData().getRootItems()); //expand default items recursively
        }
    }

    public void setPermissions(boolean visible) {
        updateEnabled = visible;
        setVisibility();
    }
    
    @Override
    public float getDefaultPaneWidth(int numberOfFeatures, boolean tocPresent) {
        final float featureWidth;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
                break;
            default:
                featureWidth = 20f;
                break;
        }//end switch
        return featureWidth;
    }

    @Subscribe
    public void handleElementState(StateChangeEvent event) {
        if (event.getState() != null) {
            inlineTocEditButton.setEnabled(event.getState().isState());
            inlineTocEditMenuItem.setEnabled(event.getState().isState());
        }
    }
    
    private void updateItemsUserCoEditionInfo(List<TableOfContentItemVO> tableOfContentItemVOList, List<CoEditionVO> coEditionVos, String presenterId) {
        tableOfContentItemVOList.forEach(tableOfContentItemVO -> {
            tableOfContentItemVO.removeAllUserCoEdition();
            coEditionVos.stream()
                    .filter((x) -> InfoType.ELEMENT_INFO.equals(x.getInfoType()) && x.getElementId().replace("__blockcontainer", "").equals(tableOfContentItemVO.getId()) &&
                            !x.getPresenterId().equals(presenterId))
                    .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
                    .forEach(x -> {
                        tableOfContentItemVO.addUserCoEdition(x);
                    });
            if (!tableOfContentItemVO.getChildItems().isEmpty()) {
                updateItemsUserCoEditionInfo(tableOfContentItemVO.getChildItems(), coEditionVos, presenterId);
            }
        });
    }
    
    private void updateTreeUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        updateItemsUserCoEditionInfo(tocTree.getTreeData().getRootItems(), coEditionVos, presenterId);
        tocTree.getDataProvider().refreshAll();
    }

    private boolean isTocCoEditionActive() {
        return tocUserCoEdition.getIcon() != null;
    }

    private boolean isItemCoEditionActive(TableOfContentItemVO item) {
        return !item.getCoEditionVos().isEmpty();
    }

    private void updateToolbarUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        tocUserCoEdition.setIcon(null);
        tocUserCoEdition.setDescription("");
        tocUserCoEdition.removeStyleName("leos-toolbar-user-coedition-self-user");
        coEditionVos.stream()
            .filter((x) -> InfoType.TOC_INFO.equals(x.getInfoType()) && !x.getPresenterId().equals(presenterId))
            .sorted(Comparator.comparing(CoEditionVO::getUserName).thenComparingLong(CoEditionVO::getEditionTime))
            .forEach(x -> {
                StringBuilder userDescription = new StringBuilder();
                if (!x.getUserLoginName().equals(user.getLogin())) {
                    userDescription.append("<a class=\"leos-toc-user-coedition-lync\" href=\"")
                            .append(StringUtils.isEmpty(x.getUserEmail()) ? "" : (coEditionSipEnabled ? new StringBuilder("sip:").append(x.getUserEmail().replaceFirst("@.*", "@" + coEditionSipDomain)).toString()
                                    : new StringBuilder("mailto:").append(x.getUserEmail()).toString()))
                            .append("\">").append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity())
                            .append(")</a>");
                } else {
                    userDescription.append(x.getUserName()).append(" (").append(StringUtils.isEmpty(x.getEntity()) ? "-" : x.getEntity()).append(")");
                }
                tocUserCoEdition.setDescription(
                        tocUserCoEdition.getDescription() +
                            messageHelper.getMessage("coedition.tooltip.message", userDescription, dataFormat.format(new Date(x.getEditionTime()))) +
                            "<br>",
                            ContentMode.HTML);
                });
        if (!tocUserCoEdition.getDescription().isEmpty()) {
            tocUserCoEdition.setIcon(VaadinIcons.USER);
            if (!tocUserCoEdition.getDescription().contains("href=\"")) {
                tocUserCoEdition.addStyleName("leos-toolbar-user-coedition-self-user");
            }
        }
    }
    
    public void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        updateToolbarUserCoEditionInfo(coEditionVos, presenterId);
        updateTreeUserCoEditionInfo(coEditionVos, presenterId);
    }
    
    private void showTocItemEditor() {
        if (editionEnabled && !editorPanelOpened) {
            addComponent(itemEditorLayout);
            editorPanelOpened = true;
        }
    }
    
    private TocItem getTypeName(String name) {
        return TocItemUtils.getTocItemByName(tocItems, name);
    }
    
    /**
     * This inner class handles the lower panel for editing and deletions
     */
    class Editor extends VerticalLayout implements HasValue.ValueChangeListener<Set<TableOfContentItemVO>> {
        
        private Binder<TableOfContentItemVO> formBinder = new Binder<>();
        private final TocEditor tocEditor;
        
        private TextField headingField;
        private TextField numberField;
        private RadioButtonGroup<String> numberToggleButtons;
        private Button deleteButton;
        private Button dapButton;
        
        public Editor(TocEditor tocEditor) {
            this.tocEditor = tocEditor;
            initEditor();
        }
        
        private void initEditor() {
            itemTypeField = buildTocTypeField(formBinder);
            itemTypeField.setWidth(50, Unit.PERCENTAGE);
            numberField = buildNumberField(formBinder);
            numberField.setWidth(50, Unit.PERCENTAGE);
            headingField = buildHeadingField(formBinder);
            headingField.setWidth(100, Unit.PERCENTAGE);
            FormLayout formLayout = new FormLayout(itemTypeField, numberField, headingField);
            addComponent(formLayout);
            
            numberToggleButtons = buildNumToggleGroupButton(formBinder);
            numberToggleButtons.setHeight(55, Unit.PIXELS);
            numberToggleButtons.setWidth(55, Unit.PERCENTAGE);
            HorizontalLayout toggleButtonLayout = new HorizontalLayout(numberToggleButtons);
            addComponent(toggleButtonLayout);
            toggleButtonLayout.setMargin(false);

            dapButton = buildDapButton(cfgHelper.getProperty("leos.dap.edit.toc.url"));
            deleteButton = buildDeleteButton(formBinder);

            HorizontalLayout buttonsLayout = new HorizontalLayout(dapButton, deleteButton);
            buttonsLayout.setSpacing(true);
            buttonsLayout.setWidth(100, Unit.PERCENTAGE);
            buttonsLayout.setComponentAlignment(dapButton, Alignment.BOTTOM_LEFT);
            buttonsLayout.setComponentAlignment(deleteButton, Alignment.BOTTOM_RIGHT);
            addComponent(buttonsLayout);
            setComponentAlignment(buttonsLayout, Alignment.BOTTOM_CENTER);
        }
        
        // selection value change for tree
        @Override
        public void valueChange(HasValue.ValueChangeEvent<Set<TableOfContentItemVO>> event) {
            if(editionEnabled) {
                boolean isEditionEnabled = false;
                boolean isNumberFieldEnabled = true;
                boolean hasItemNumber = false;
                boolean hasItemHeading = false;
                boolean showNumberToggle = false;
                boolean isDeleteButtonEnabled = false;
                String numberToggleValue = "";
                // is mandatory to set it first to null!
                // the set value in the textField will trigger a value change event which will be ignored if the tocItemForm data is null
                deleteButton.setData(null);
                formBinder.removeBean();
    
                Set<TableOfContentItemVO> items = event.getValue();
                if (items != null) {
                    TableOfContentItemVO item = items.iterator().hasNext() ? items.iterator().next() : null;
                    LOG.trace("ToC selection changed to:'{}'", item);
    
                    if (items.size() > 1) { // disable edition for multi-selection
                        isEditionEnabled = false;
                        isDeleteButtonEnabled = false;
                    } else if (item != null) {
                        showTocItemEditor();
                        formBinder.setBean(item);
        
                        TocItem tocItem = item.getTocItem();
                        isNumberFieldEnabled = tocItem.isNumberEditable();
                        hasItemNumber = OptionsType.MANDATORY.equals(tocItem.getItemNumber()) || OptionsType.OPTIONAL.equals(tocItem.getItemNumber());
                        hasItemHeading = OptionsType.MANDATORY.equals(tocItem.getItemHeading()) || OptionsType.OPTIONAL.equals(tocItem.getItemHeading());
                        isEditionEnabled = !tocItem.isRoot() && tocItem.isDraggable();
                        boolean isDeletedItem = tocEditor.isDeletedItem(item);
                        // If toc item is configured to be deletable, then check:
                        // - if the item has already been deleted => check if it can be undelete
                        // - if has not been deleted => check if it can be deleted (Ex: when mixed EC/CN element are present)
                        isDeleteButtonEnabled = tocItem.isDeletable() &&
                                (isDeletedItem ? tocEditor.isUndeletableItem(item) : tocEditor.isDeletableItem(tocTree.getTreeData(), item));
                        final String caption = isDeletedItem ? messageHelper.getMessage("toc.edit.window.item.selected.undelete") : messageHelper.getMessage("toc.edit.window.item.selected.delete");
                        final String description = isDeletedItem ? messageHelper.getMessage("toc.edit.window.undelete.confirmation.not") : messageHelper.getMessage(tocEditor.getDeleteKey());
                        renderDeleteButton(caption, description, isDeleteButtonEnabled);
                        deleteButton.setData(item);
        
                        // num/UnNum to be shown only for 1.article from 2.proposal and are 3.not soft deleted
                        if (item.getOriginAttr() != null && EC.equals(item.getOriginAttr()) && ARTICLE.equals(tocItem.getAknTag().value()) && !item.getChildItems().isEmpty()
                                && !(SoftActionType.DELETE.equals(item.getSoftActionAttr()) || SoftActionType.MOVE_TO.equals(item.getSoftActionAttr()))) {
                            showNumberToggle = true;
                            numberToggleValue = getNumberToggleValue(item);
                            unToggleSiblings(item);
                        }
                    }
                }

                // Number
                renderField(numberField, hasItemNumber, isEditionEnabled && isNumberFieldEnabled);
                // Heading
                renderField(headingField, hasItemHeading, hasItemHeading);
                // Number toggle button
                renderNumberToggle(numberToggleButtons, showNumberToggle, numberToggleValue);
                //Delete button
                deleteButton.setEnabled(isDeleteButtonEnabled);
            }
        }
        
        private TextField buildTocTypeField(Binder<TableOfContentItemVO> binder) {
            TextField itemTypeField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.type"));
            itemTypeField.setEnabled(false);
            binder.forField(itemTypeField)
                    .withConverter(new Converter<String, TocItem>() {
                        @Override
                        public Result<TocItem> convertToModel(String value, ValueContext context) {
                            return value == null
                                    ? Result.ok(null)
                                    : Result.ok(getTypeName(value));
                        }
                        
                        @Override
                        public String convertToPresentation(TocItem value, ValueContext context) {
                            return TableOfContentHelper.getDisplayableTocItem(value, messageHelper);
                        }
                    })
                    .bind(TableOfContentItemVO::getTocItem, TableOfContentItemVO::setTocItem);
            
            return itemTypeField;
        }
        
        private TextField buildNumberField(Binder<TableOfContentItemVO> binder) {
            final TextField numberField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.number"));
            numberField.setVisible(false);
            
            //by default we just populate the field without validating. We trust the data inserted previously is correct
            binder.forField(numberField)
                    .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);
            
            numberField.addValueChangeListener(event -> {
                if (event.isUserOriginated()) {
                    TableOfContentItemVO item = binder.getBean();
                    item.setNumber(event.getValue()); //new value
                    tocTree.getDataProvider().refreshItem(item);
    
                    // remove old binder added previously to the field numberField, and add a specific one depending on the type
                    binder.removeBinding(numberField);
                    switch (item.getTocItem().getNumberingType()) {
                        case NONE:
                            binder.forField(numberField)
                                    .withNullRepresentation("")
                                    .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)
                                    .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);
                            break;
                        default:
                            NumberingConfig config = TocItemUtils.getNumberingByName(numberingConfigs, item.getTocItem().getNumberingType());
                            binder.forField(numberField)
                                    .withNullRepresentation("")
                                    .withValidator(new RegexpValidator(messageHelper.getMessage(config.getMsgValidationError()), config.getRegex()))
                                    .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)
                                    .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);
                            
                    }
                    
                    addElementInTocChangedList(ActionType.UPDATED, item.getId(), item.getTocItem().getAknTag().name());
                    enableSave(binder.validate().isOk());
                }
            });
            return numberField;
        }
        
        private TextField buildHeadingField(Binder<TableOfContentItemVO> binder) {
            final TextField headingField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.heading"));
            headingField.setVisible(false);
            headingField.setWidth(100, Unit.PERCENTAGE);
            binder.forField(headingField)
                    .withNullRepresentation("")
                    .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)// TODO unescapeXml might need to be removed ones LEOS-515
                    .withValidator(heading -> {
                        if (binder.getBean().getTocItem().getItemHeading() == OptionsType.MANDATORY || 
                                isElementHeadingOrContentEmpty(binder,AnnexStructureType.LEVEL.getType())) {
                            return !heading.trim().isEmpty();
                        } 
                            return true;
                    }, messageHelper.getMessage("toc.edit.window.item.selected.heading.error.message"))
                    // is resolved
                    .bind(TableOfContentItemVO::getHeading, TableOfContentItemVO::setHeading);
            
            headingField.addValueChangeListener(event -> {
                if (event.isUserOriginated()) {
                    TableOfContentItemVO item = binder.getBean();
                    item.setHeading(event.getValue());
                    tocTree.getDataProvider().refreshItem(item);
                    addElementInTocChangedList(ActionType.UPDATED, item.getId(), item.getTocItem().getAknTag().name());
                    enableSave(binder.validate().isOk());
                }
            });
            return headingField;
        }

        private boolean isElementHeadingOrContentEmpty(Binder<TableOfContentItemVO> binder, String element) {
            if (binder.getBean().getTocItem().getAknTag().value().equalsIgnoreCase(element) &&
                    binder.getBean().getTocItem().getItemHeading() == OptionsType.OPTIONAL) {
                return binder.getBean().getHeading().isEmpty() && binder.getBean().getContent().isEmpty();
            } else
                return false;
        }
        
        private RadioButtonGroup<String> buildNumToggleGroupButton(Binder<TableOfContentItemVO> binder) {
            RadioButtonGroup<String> buttons = new RadioButtonGroup<>();
            buttons.setCaption("Paragraph Numbering");
            buttons.setItems("Numbered", "Unnumbered");
            buttons.setVisible(false);
            
            buttons.addSelectionListener(event -> {
                TableOfContentItemVO item = binder.getBean();
                
                if (tocTree.getTreeData().contains(item) && EC.equals(item.getOriginAttr()) && !item.getChildItems().isEmpty()) {
                    
                    TableOfContentItemVO updatedItemVO = tocTree.getTreeData().getChildren(item).get(0).getParentItem();
                    TableOfContentItemVO firstChild = updatedItemVO.getChildItems().get(0);
                    boolean flag = false;
                    
                    for (TableOfContentItemVO itemVo : updatedItemVO.getChildItems()) {
                        if (itemVo.getNumSoftActionAttr() != null && SoftActionType.DELETE.equals(itemVo.getNumSoftActionAttr())) {
                            flag = true;
                            break;
                        }
                    }
                    
                    if ("Numbered".equals(buttons.getSelectedItem().get())) {
                        if ((firstChild.getNumber() == null || firstChild.getNumber() == "")
                                || flag) {
                            updatedItemVO.setNumberingToggled(true);
                            tocTree.getDataProvider().refreshItem(updatedItemVO);
                            enableSave(true);
                        } else if (isToggledByUser) {// check if save is not enabled before
                            enableSave(false);
                        }
                    } else if ("Unnumbered".equals(buttons.getSelectedItem().get())) {
                        if ((firstChild.getNumber() != null && firstChild.getNumber() != "")
                                && !flag) {
                            updatedItemVO.setNumberingToggled(false);
                            tocTree.getDataProvider().refreshItem(updatedItemVO);
                            enableSave(true);
                        } else if (isToggledByUser) {
                            enableSave(false);
                        }
                    } else {
                        enableSave(false);
                    }
                    if (!isToggledByUser) {
                        isToggledByUser = true;
                    }
                }
            });
            
            return buttons;
        }

        private void DeleteWithConfirmationCheck(TableOfContentItemVO item) {
            if (tocEditor.checkIfConfirmDeletion(tocTree.getTreeData(), item)) {
                confirmItemDeletion(item);
            } else {
                checkDeleteLastEditingTypeConsumer.accept(item.getId(), () -> deleteItem(item));
            }
        }

        private Button buildDeleteButton(Binder<TableOfContentItemVO> binder) {
            final Button button = new Button(messageHelper.getMessage("toc.edit.window.item.selected.delete"));
            button.setEnabled(false);
            button.addClickListener(event -> {
                TableOfContentItemVO item = binder.getBean();
                if (tocTree.getTreeData().contains(item)) {
                    if (tocEditor.isDeletedItem(item)) {
                        undeleteItem(item);
                    } else {
                        if (isTocCoEditionActive() || isItemCoEditionActive(item)) {
                            String description=tocUserCoEdition.getDescription() + getColumnItemDescription(item);
                            description = description.replace("leos-toc-user-coedition-lync", "");
                            ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                                    messageHelper.getMessage("coedition.delete.element.confirmation.title"),
                                    messageHelper.getMessage("coedition.delete.element.confirmation.message", description),
                                    messageHelper.getMessage("coedition.delete.element.confirmation.confirm"),
                                    messageHelper.getMessage("coedition.delete.element.confirmation.cancel"), null);
                            confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
                            confirmDialog.getContent().setHeightUndefined();
                            confirmDialog.setHeightUndefined();
                            confirmDialog.show(getUI(), dialog -> {
                                if (dialog.isConfirmed()) {
                                    DeleteWithConfirmationCheck(item);
                                }
                            }, true);
                        } else {
                            DeleteWithConfirmationCheck(item);
                        }
                    }
                }
            });
            return button;
        }

        private void undeleteItem(TableOfContentItemVO item) {
            tocEditor.undeleteItem(tocTree, item);
            tocTree.getDataProvider().refreshAll();
            tocTree.deselectAll();
            final CheckinElement checkinElement = new CheckinElement(ActionType.UNDELETED, item.getId(), item.getTocItem().getAknTag().name());
            final String statusMsg = messageHelper.getMessage("toc.edit.window.undelete.confirmation.success", TableOfContentHelper.getDisplayableTocItem(item.getTocItem(), messageHelper));
            eventBus.post(new TocChangedEvent(statusMsg, TocChangedEvent.Result.SUCCESSFUL, Arrays.asList(checkinElement)));
            enableSave(true);
        }
        
        private void deleteItem(TableOfContentItemVO item) {
            final ActionType actionType = tocEditor.deleteItem(tocTree, item);
            final CheckinElement checkinElement = new CheckinElement(actionType, item.getId(), item.getTocItem().getAknTag().name());
            final String statusMsg = messageHelper.getMessage("toc.edit.window.delete.message", TableOfContentHelper.getDisplayableTocItem(item.getTocItem(), messageHelper));
            eventBus.post(new TocChangedEvent(statusMsg, TocChangedEvent.Result.SUCCESSFUL, Arrays.asList(checkinElement)));
            enableSave(true);
        }
        
        private void confirmItemDeletion(TableOfContentItemVO item) {
            ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                    messageHelper.getMessage("toc.edit.window.delete.confirmation.title"),
                    messageHelper.getMessage("toc.edit.window.delete.confirmation.message"),
                    messageHelper.getMessage("toc.edit.window.delete.confirmation.confirm"),
                    messageHelper.getMessage("toc.edit.window.delete.confirmation.cancel"), null);
            confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
            confirmDialog.show(getUI(), dialog -> {
                if (dialog.isConfirmed()) {
                    checkDeleteLastEditingTypeConsumer.accept(item.getId(), () -> deleteItem(item));
                }
            }, true);
        }
        
        private Button buildDapButton(final String dapUrl) {
            Button button = new Button(messageHelper.getMessage("leos.button.dap"));
            button.setDescription(messageHelper.getMessage("leos.button.dap.description"));
            button.setIcon(LeosTheme.LEOS_DAP_ICON_16);
            button.addClickListener(new Button.ClickListener() {
                private static final long serialVersionUID = -5633348109667050418L;
                
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    Page.getCurrent().open(dapUrl, "_new");
                }
            });
            return button;
        }
        
        private void renderDeleteButton(String caption, String description, boolean enable) {
            deleteButton.setEnabled(enable);
            deleteButton.setCaption(caption);
            deleteButton.setDescription(enable ? "" : description);
        }
        
        private void renderField(AbstractField field, boolean display, boolean enable) {
            if (display) {
                field.setVisible(true);
                field.setEnabled(enable);
            } else {
                field.setVisible(false);
            }
        }
        
        private void renderNumberToggle(RadioButtonGroup<String> radioButtons, boolean display, String toggleValue) {
            if (display) {
                radioButtons.setStyleName("leos-toc-content-hide",false);
                radioButtons.setVisible(true);
                radioButtons.setEnabled(true);
                radioButtons.setSelectedItem(toggleValue);
                isToggledByUser = false;
            } else {
                radioButtons.setStyleName("leos-toc-content-hide",true);
            }
        }
        
        private String getNumberToggleValue(TableOfContentItemVO item) {
            final String toggleValue;
            final TableOfContentItemVO firstChild = item.getChildItems().get(0);
            if (firstChild.getNumber() != null
                    && !SoftActionType.DELETE.equals(firstChild.getNumSoftActionAttr())) {
                toggleValue = "Numbered";
            } else {
                toggleValue = "Unnumbered";
            }
        
            return toggleValue;
        }
        
        /**
         * UnToggle all NumberingToggle for other articles.
         */
        private void unToggleSiblings(TableOfContentItemVO item) {
            List<TableOfContentItemVO> listOfArticle = tocTree.getTreeData().getParent(item).getChildItems();
            for (TableOfContentItemVO itemVO : listOfArticle) {
                if (ARTICLE.equals(itemVO.getTocItem().getAknTag().value()) && !itemVO.equals(item) && itemVO.isNumberingToggled() != null) {
                    itemVO.setNumberingToggled(null);
                }
            }
        }
    }

    private CheckDeleteLastEditingTypeConsumer checkDeleteLastEditingTypeConsumer;

    @Subscribe
    public void checkDeleteLastEditingType(CheckDeleteLastEditingTypeEvent event) {
        checkDeleteLastEditingTypeConsumer.accept(event.getElementId(), () -> eventBus.post(event.getActionEvent()));
    }

}
