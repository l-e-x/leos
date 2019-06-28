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
package eu.europa.ec.leos.ui.window;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.Binder;
import com.vaadin.data.Converter;
import com.vaadin.data.HasValue;
import com.vaadin.data.Result;
import com.vaadin.data.TreeData;
import com.vaadin.data.ValueContext;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.data.validator.RegexpValidator;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.AbstractField;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.RadioButtonGroup;
import com.vaadin.ui.RichTextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.GridDragStartListener;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DragSourceExtension;
import com.vaadin.ui.dnd.event.DragStartListener;
import com.vaadin.ui.renderers.HtmlRenderer;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.services.support.xml.XmlHelper;
import eu.europa.ec.leos.ui.component.toc.EditTocDropHandler;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.event.toc.RefreshTocWindowEvent;
import eu.europa.ec.leos.ui.event.toc.SaveTocRequestEvent;
import eu.europa.ec.leos.ui.event.toc.TocStatusUpdateEvent;
import eu.europa.ec.leos.ui.extension.dndscroll.TreeGridScrollDropTargetExtension;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.window.CloseTocEditorEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.window.AbstractEditChangeMonitorWindow;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.vaadin.dialogs.ConfirmDialog;
import org.vaadin.teemusa.gridextensions.client.tableselection.TableSelectionState.TableSelectionMode;
import org.vaadin.teemusa.gridextensions.tableselection.TableSelectionModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.content.toc.ProposalTocRulesMap.ARABIC_REGEX;
import static eu.europa.ec.leos.services.content.toc.ProposalTocRulesMap.ROMAN_REGEX;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.DEFAULT_CAPTION_MAX_SIZE;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.EC;

public class EditTocWindow extends AbstractEditChangeMonitorWindow {

    private static final long serialVersionUID = -5533529522081984L;
    private static final Logger LOG = LoggerFactory.getLogger(EditTocWindow.class);
    
    private TextField itemTypeField;
    private MultiSelectTreeGrid<TableOfContentItemVO> tocTree;
    private final Map<TocItemType, List<TocItemType>> tableOfContentRules;
    private final TocItemType[] tocItemTypes;
    private final TocEditor tocEditor;

    private boolean userOriginated = false;// Kludge as dataProviderListener is not smart enough
    private Label statusLabel;
    private final String STATUS_STYLE = "leos-toc-tree-status";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private boolean isToggledByUser;

    public EditTocWindow(final MessageHelper messageHelper, final EventBus eventBus, final ConfigurationHelper cfgHelper,
            final Map<TocItemType, List<TocItemType>> tableOfContentRules, final TocItemType[] tocItemTypes, TocEditor numberEditor) {
        super(messageHelper, eventBus);
        this.tableOfContentRules = tableOfContentRules;
        this.tocItemTypes = tocItemTypes;
        this.tocEditor = numberEditor;

        setWidth(800, Unit.PIXELS);
        setHeight(750, Unit.PIXELS);
        setCaption(messageHelper.getMessage("toc.edit.window.title"));

        VerticalLayout verticalLayout = buildWindowBody();
        addButtonOnLeft(buildDapButton(cfgHelper.getProperty("leos.dap.edit.toc.url")));
        setBodyComponent(verticalLayout);
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

    public void setTableOfContent(TreeData<TableOfContentItemVO> newTocData) {
        userOriginated = false;
        TreeData<TableOfContentItemVO> tocData = tocTree.getTreeData();
        tocData.removeItem(null);// remove old
        tocData.addItems(newTocData.getRootItems(), TableOfContentItemVO::getChildItemsView);// add all new
        tocTree.getDataProvider().refreshAll();
        userOriginated = true;
        // this tree is going to be always extended by default
        expandDefaultTreeNodes(tocTree.getTreeData().getRootItems());
    }

    @Subscribe
    public void refreshTocData(RefreshTocWindowEvent event) {
        setTableOfContent(event.getTocData());
    }

    private void expandDefaultTreeNodes(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            if(item.getType().isExpandedByDefault()){
                tocTree.expand(item);
                if (!item.getChildItems().isEmpty()) {
                    expandDefaultTreeNodes(item.getChildItems());
                }
            }
        });
    }

    private VerticalLayout buildWindowBody() {

        HorizontalLayout treeAndItemsArea = new HorizontalLayout();
        treeAndItemsArea.setSizeFull();
        treeAndItemsArea.setSpacing(true);

        Panel tocPanel = buildTocTree();
        treeAndItemsArea.addComponent(tocPanel);
        treeAndItemsArea.setExpandRatio(tocPanel, 0.7f);

        Panel itemPanel = buildTocItemPanel();
        treeAndItemsArea.addComponent(itemPanel);
        treeAndItemsArea.setExpandRatio(itemPanel, 0.3f);

        VerticalLayout windowBodyArea = new VerticalLayout();
        windowBodyArea.setSizeFull();
        windowBodyArea.setMargin(true);
        windowBodyArea.setSpacing(true);
        windowBodyArea.addComponent(treeAndItemsArea);
        windowBodyArea.setExpandRatio(treeAndItemsArea, 1.0f);
        windowBodyArea.addComponent(buildTocItemEditionPanel());
        return windowBodyArea;
    }

    private Panel buildTocItemEditionPanel() {
        final Editor tocItemEditor = new Editor(tocEditor);
        tocItemEditor.setMargin(true);
        tocTree.asMultiSelect().addValueChangeListener(tocItemEditor);

        Panel itemEditionPanel = new Panel(messageHelper.getMessage("toc.edit.window.item.selected"));
        itemEditionPanel.addStyleName("leos-toc-panel");
        itemEditionPanel.setContent(tocItemEditor);
        itemEditionPanel.setWidth(100, Unit.PERCENTAGE);
        itemEditionPanel.setHeight(180, Unit.PIXELS);

        return itemEditionPanel;
    }

    /**
     * @return panel
     */
    private Panel buildTocItemPanel() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.setWidth(100, Unit.PERCENTAGE);
        gridLayout.setHeight(100, Unit.PERCENTAGE);
        gridLayout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        gridLayout.setSpacing(false);
        gridLayout.setMargin(true);
        for (TocItemType type : tocItemTypes) {
            if (!type.isRoot() && type.isDraggable()) {
                Label itemLabel = new Label(TableOfContentHelper.getDisplayableItemType(type, messageHelper));
                itemLabel.setWidth(91, Unit.PIXELS);
                itemLabel.setStyleName("leos-dragItem");

                DragSourceExtension<Label> dragSourceExtension = new DragSourceExtension<>(itemLabel);
                dragSourceExtension.addDragStartListener((DragStartListener<Label>) event -> {
                    String number = null, heading = null, content = "";
                    if (type.hasItemNumber()) {
                        number = messageHelper.getMessage("toc.item.type.number");
                    }
                    if (type.hasItemHeading()) {
                        heading = messageHelper.getMessage("toc.item.type." + type.getName().toLowerCase() + ".heading");
                    }
                    if(type.isContentDisplayed()) { // TODO: Use a message property to compose the default content text here and in the XMLHelper templates for each element
                        content = (type.getName().equalsIgnoreCase(XmlHelper.RECITAL) || type.getName().equalsIgnoreCase(XmlHelper.CITATION))
                                ? StringUtils.capitalize(type.getName() + "...") : "Text...";
                    }
                    TableOfContentItemVO dragData = new TableOfContentItemVO(type, Cuid.createCuid(), null, number, null, heading, null, 
                            null, null, content);
                    Set<TableOfContentItemVO> draggedItems = new HashSet<>();
                    draggedItems.add(dragData);
                    dragSourceExtension.setDragData(draggedItems);
                });

                dragSourceExtension.setEffectAllowed(EffectAllowed.COPY_MOVE);
                gridLayout.addComponent(itemLabel);
            }
        }
        Panel panel = new Panel(messageHelper.getMessage("toc.edit.window.items"));
        panel.addStyleName("leos-toc-panel");
        panel.setSizeFull();
        panel.setContent(gridLayout);
        return panel;
    }

    private Panel buildTocTree() {
        tocTree = new MultiSelectTreeGrid<>();
        tocTree.addColumn(tableOfContentItemVO -> {
            return TableOfContentHelper.buildItemCaption(tableOfContentItemVO,
                    DEFAULT_CAPTION_MAX_SIZE, messageHelper);
            }, new HtmlRenderer());

        TableSelectionModel<TableOfContentItemVO> model = new TableSelectionModel<>();
        model.setMode(TableSelectionMode.CTRL);
        tocTree.setSelectionModel(model);

        tocTree.removeHeaderRow(tocTree.getDefaultHeaderRow());
        tocTree.addStyleName("leos-toc-tree");
        tocTree.setSizeFull();

        model.addMultiSelectionListener(listener -> handleMultiSelection(listener));

        setDragData();

        addDropTargetListener();

        TreeDataProvider<TableOfContentItemVO> dataProvider = new TreeDataProvider<TableOfContentItemVO>(tocTree.getTreeData());
        dataProvider.addDataProviderListener(event -> {
            if (userOriginated) {
                LOG.debug("data changed logged in DataProviderListener");
                enableSave(true);
            }
        });
        tocTree.setDataProvider(dataProvider);

        tocEditor.setTocTreeDataFilter(dataProvider);
        tocEditor.setTocTreeStyling(tocTree, dataProvider);

        VerticalLayout treeArea = new VerticalLayout();
        treeArea.setSizeFull();
        treeArea.setSpacing(false);
        treeArea.setMargin(false);

        treeArea.addComponent(tocTree);
        statusLabel = buildStatusLabel();
        treeArea.addComponent(statusLabel);
        treeArea.setExpandRatio(tocTree, 0.95f);
        treeArea.setExpandRatio(statusLabel, 0.05f);

        final Panel treePanel = new Panel(messageHelper.getMessage("toc.edit.window.toc"));
        treePanel.addStyleName("leos-toc-panel");
        treePanel.setContent(treeArea);
        treePanel.setSizeFull();

        return treePanel;
    }

    private Label buildStatusLabel() {
        Label statusLabel = new Label("&nbsp;", ContentMode.HTML);//To display empty label permanently
        statusLabel.setStyleName(STATUS_STYLE);
        statusLabel.setSizeFull();
        return statusLabel;
    }
    
    @Subscribe
    public void handleStatus(TocStatusUpdateEvent event) {
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
    }

    private void handleMultiSelection(MultiSelectionEvent<TableOfContentItemVO> listener) {
        if (listener.isUserOriginated()) {
            Set<TableOfContentItemVO> newSelectedItems = listener.getAddedSelection();
            Set<TableOfContentItemVO> oldSelectedItems = listener.getOldSelection();

            if (!oldSelectedItems.isEmpty() && !newSelectedItems.isEmpty()) {
                TableOfContentItemVO oldSelectedItem = oldSelectedItems.iterator().next();
                TableOfContentItemVO newSelectedItem = newSelectedItems.iterator().next();

                if (!oldSelectedItem.getType().getName().equalsIgnoreCase(newSelectedItem.getType().getName())) {
                    tocTree.deselect(newSelectedItem);
                    String newSelectedItemType = messageHelper.getMessage("toc.item.type." + newSelectedItem.getType().getName().toLowerCase());
                    String oldSelectedItemType = messageHelper.getMessage("toc.item.type." + oldSelectedItem.getType().getName().toLowerCase());
                    String statusMsg = messageHelper.getMessage("toc.item.cross.item.selection.error.message", newSelectedItemType, oldSelectedItemType);
                    eventBus.post(new TocStatusUpdateEvent(statusMsg, TocStatusUpdateEvent.Result.ERROR));
                }
            }
        }
    }

    private void setDragData() {
        TreeGridDragSource<TableOfContentItemVO> dragSource = new TreeGridDragSource<>(tocTree);
        dragSource.setEffectAllowed(EffectAllowed.MOVE);
        dragSource.addGridDragStartListener((GridDragStartListener<TableOfContentItemVO>) event -> {
            List<TableOfContentItemVO> draggedItems = event.getDraggedItems();
            if (draggedItems.size() > 0) {
                dragSource.setDragData(draggedItems);
            }
        });

        dragSource.setDragDataGenerator("nodetype", tocVO -> {// for client side
            String dragType = tocVO.getType().getName();

            dragSource.clearDataTransferData();
            dragSource.setDataTransferData("nodetype", dragType);

            dragSource.setDataTransferText(dragType);// backup text/plain
            return dragType;
        });
    }

    private void addDropTargetListener() {
        TreeGridScrollDropTargetExtension<TableOfContentItemVO> dropTarget = new TreeGridScrollDropTargetExtension<>(tocTree, DropMode.ON_TOP_OR_BETWEEN);
        dropTarget.setDropEffect(DropEffect.MOVE);
        // server side handler
        dropTarget.addTreeGridDropListener(new EditTocDropHandler(tocTree, messageHelper, eventBus, tableOfContentRules, tocEditor));
    }

    @Override
    protected void onSave() {
        List<TableOfContentItemVO> list = TableOfContentItemConverter.buildTocItemVOList(tocTree.getTreeData());
        eventBus.post(new SaveTocRequestEvent(list));
        super.onSave();
    }

    @Override
    public void close() {
        super.close();
        eventBus.post(new RefreshDocumentEvent());
        eventBus.post(new CloseTocEditorEvent());
    }

    /* ************************************************************************************
    *   This inner class handles the lower panel for editing and deletions
    ************************************************************************************ */
    class Editor extends VerticalLayout implements HasValue.ValueChangeListener<Set<TableOfContentItemVO>> {

        Binder<TableOfContentItemVO> formBinder = new Binder<>();

        private Button deleteButton;
        private RadioButtonGroup<String> numberToggleButtons;
        private TextField headingField;
        private TextField numberField;
        private RichTextArea contentField;
        private Label spacer;
        private HorizontalLayout numberHeadingLayout;
        private HorizontalLayout toggleButtonLayout;
        private HorizontalLayout contentLayout;

        private final TocEditor tocEditor;

        public Editor(TocEditor tocEditor) {
            this.tocEditor = tocEditor;
            initEditor();
        }

        private void initEditor() {
            itemTypeField = buildTocTypeField(formBinder);
            numberField = buildNumberField(formBinder);
            headingField = buildHeadingField(formBinder);
            spacer = new Label("&nbsp;", ContentMode.HTML);
            deleteButton = buildDeleteButton(formBinder);
            numberHeadingLayout = new HorizontalLayout(itemTypeField, numberField, spacer, headingField, deleteButton);
            numberHeadingLayout.setSizeFull();
            numberHeadingLayout.setExpandRatio(headingField, 1.0f);
            numberHeadingLayout.setComponentAlignment(deleteButton, Alignment.BOTTOM_RIGHT);
            this.addComponent(numberHeadingLayout);


            contentField = buildContentField(formBinder);
            contentLayout = new HorizontalLayout(contentField);
            contentLayout.setStyleName("leos-toc-content");
            contentLayout.setSizeFull();
            contentField.setHeight(55, Unit.PIXELS);
            contentField.setWidth(100, Unit.PERCENTAGE);

            numberToggleButtons =  buildNumToggleGroupButton(formBinder);
            toggleButtonLayout = new HorizontalLayout(numberToggleButtons);
            toggleButtonLayout.setStyleName("leos-toc-content");
            contentLayout.setSizeFull();
            numberToggleButtons.setHeight(55, Unit.PIXELS);
            numberToggleButtons.setWidth(55, Unit.PERCENTAGE);
        }

        // selection value change for tree
        @Override
        public void valueChange(HasValue.ValueChangeEvent<Set<TableOfContentItemVO>> event) {
            boolean editionEnabled = false;
            boolean numberFieldEnabled = true;
            boolean hasItemNumber = true;
            boolean hasItemHeading = true;
            boolean isContentDisplayed = false;
            boolean isNumberToggleEnabled = false;
            boolean isDeleteButtonEnabled = false;

            // is mandatory to set it first to null!
            // the set value in the textField will trigger a value change event which will be ignored if the tocItemForm data is null
            deleteButton.setData(null);
            formBinder.removeBean();

            Set<TableOfContentItemVO> items = event.getValue();
            if (items != null) {
                TableOfContentItemVO item = items.iterator().hasNext() ? items.iterator().next() : null;
                LOG.trace("ToC selection changed to:'{}'", item);

                if (items.size() > 1) { // disable edition for multi-selection
                    editionEnabled = false;
                    isDeleteButtonEnabled = false;
                } else if (item != null) {
                    formBinder.setBean(item);
                    TocItemType type = item.getType();
                    numberFieldEnabled = type.isNumberEditable();
                    hasItemNumber = type.hasItemNumber();
                    hasItemHeading = type.hasItemHeading();
                    editionEnabled = !type.isRoot() && type.isDraggable();
                    isContentDisplayed = type.isContentDisplayed();
                    boolean isDeletedItem = tocEditor.isDeletedItem(item);
                    // If toc item is configured to be deletable, then check:
                    // - if the item has already been deleted => check if it can be undelete
                    // - if has not been deleted => check if it can be deleted (Ex: when mixed EC/CN element are present)
                    isDeleteButtonEnabled = type.isDeletable() &&
                            (isDeletedItem ? tocEditor.isUndeletableItem(item) : tocEditor.isDeletableItem(tocTree.getTreeData(), item));
                    final String caption = isDeletedItem ? messageHelper.getMessage("toc.edit.window.item.selected.undelete") : messageHelper.getMessage("toc.edit.window.item.selected.delete");
                    final String description = isDeletedItem ? messageHelper.getMessage("toc.edit.window.undelete.confirmation.not") : messageHelper.getMessage("toc.edit.window.delete.confirmation.not");
                    renderDeleteButton(caption, description, isDeleteButtonEnabled);
                    deleteButton.setData(item);
                    // num/UnNum to be enabled only for 1.article from 2.proposal and are 3.not soft deleted
                    if (item.getOriginAttr() != null && EC.equals(item.getOriginAttr()) && ARTICLE.equals(type.getName()) && !item.getChildItems().isEmpty()
                            && !(SoftActionType.DELETE.equals(item.getSoftActionAttr()) || SoftActionType.MOVE_TO.equals(item.getSoftActionAttr()))) {
                        isNumberToggleEnabled = true;
                    }
                    //toggle button
                    renderNumberToggleButtons(item, numberToggleButtons, isNumberToggleEnabled);
                }
            }

            // Number
            renderNumberField(numberField, hasItemNumber, editionEnabled && numberFieldEnabled);
            // Heading
            headingField.setVisible(hasItemHeading);
            spacer.setVisible(!hasItemHeading);
            if(hasItemHeading){
                headingField.setEnabled(editionEnabled);
            } else {
                numberHeadingLayout.setExpandRatio(spacer, 1.0f);
            }
            // Content
            renderContentField(contentField, isContentDisplayed, false);

            //Delete button
            deleteButton.setEnabled(isDeleteButtonEnabled);
        }

        private void renderNumberField(AbstractField field, boolean display, boolean enable) {
            if (display) {
                field.setVisible(true);
                field.setEnabled(enable);
            } else {
                field.setVisible(false);
            }
        }

        private void renderContentField(AbstractField field, boolean display, boolean enable) {
            if (display) {
                this.addComponent(contentLayout);
                field.setVisible(true);
                field.setEnabled(enable);
            } else {
                field.setVisible(false);
                this.removeComponent(contentLayout);
            }
        }

        private void renderNumberToggleButtons(TableOfContentItemVO item, RadioButtonGroup<String> radioButtons, boolean display) {
            if (display) {
                this.addComponent(toggleButtonLayout);
                radioButtons.setVisible(true);
                radioButtons.setEnabled(true);
                TableOfContentItemVO firstChild = item.getChildItems().get(0);
                isToggledByUser = false;
                if (firstChild.getNumber() != null
                        && !SoftActionType.DELETE.equals(firstChild.getNumSoftActionAttr())) {
                    radioButtons.setSelectedItem("Numbered");
                } else {
                    radioButtons.setSelectedItem("Unnumbered");
                }
                List<TableOfContentItemVO> listOfArticle = tocTree.getTreeData().getParent(item).getChildItems();
                for (TableOfContentItemVO itemVO : listOfArticle) {
                    if (ARTICLE.equals(itemVO.getType().getName()) && !itemVO.equals(item) && itemVO.isNumberingToggled() != null) {
                        itemVO.setNumberingToggled(null);
                    }
                }
            } else {
                radioButtons.setVisible(false);
                this.removeComponent(toggleButtonLayout);
            }
        }

        private void renderDeleteButton(String caption, String description, boolean enable) {
            deleteButton.setEnabled(enable);
            deleteButton.setCaption(caption);
            deleteButton.setDescription(enable ? "" : description);
        }
    }

    private void undeleteItem(TableOfContentItemVO item) {
        tocEditor.undeleteItem(tocTree, item);
        tocTree.getDataProvider().refreshAll();
        tocTree.deselectAll();
        eventBus.post(new TocStatusUpdateEvent(messageHelper.getMessage("toc.edit.window.undelete.confirmation.success",
                TableOfContentHelper.getDisplayableItemType(item.getType(), messageHelper)), TocStatusUpdateEvent.Result.SUCCESSFUL));
        enableSave(true);
    }

    private void deleteItem(TableOfContentItemVO item) {
        tocEditor.deleteItem(tocTree, item);
        eventBus.post(new TocStatusUpdateEvent(messageHelper.getMessage("toc.edit.window.delete.message",
                TableOfContentHelper.getDisplayableItemType(item.getType(), messageHelper)), TocStatusUpdateEvent.Result.SUCCESSFUL));
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
                deleteItem(item);
            }
        }, true);
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
                    if (tocEditor.checkIfConfirmDeletion(tocTree.getTreeData(), item)) {
                        confirmItemDeletion(item);
                    } else {
                        deleteItem(item);
                    }
                }
            }
        });
        return button;
    }

    private RadioButtonGroup<String> buildNumToggleGroupButton(Binder<TableOfContentItemVO> binder) {
        RadioButtonGroup<String> buttons = new RadioButtonGroup<>();
        buttons.setCaption("Paragraph Numbering");
        buttons.setItems("Numbered", "Unnumbered");

        buttons.addSelectionListener(event -> {
            TableOfContentItemVO item = binder.getBean();

            if (tocTree.getTreeData().contains(item) && EC.equals(item.getOriginAttr()) && !item.getChildItems().isEmpty()) {

                TableOfContentItemVO updatedItemVO = tocTree.getTreeData().getChildren(item).get(0).getParentItem();
                TableOfContentItemVO firstChild = updatedItemVO.getChildItems().get(0);
                boolean flag = false;

                for(TableOfContentItemVO itemVo : updatedItemVO.getChildItems()){
                    if(itemVo.getNumSoftActionAttr() != null && SoftActionType.DELETE.equals(itemVo.getNumSoftActionAttr())){
                        flag = true;
                        break;
                    }
                }

                if ("Numbered".equals(buttons.getSelectedItem().get())) {
                    if ((firstChild.getNumber() == null || firstChild.getNumber() == "")
                            ||  flag) {
                        updatedItemVO.setNumberingToggled(true);
                        tocTree.getDataProvider().refreshItem(updatedItemVO);
                        enableSave(true);
                    }else if(isToggledByUser){// check if save is not enabled before
                        enableSave(false);
                    }
                } else if ("Unnumbered".equals(buttons.getSelectedItem().get())) {
                    if ((firstChild.getNumber() != null && firstChild.getNumber() != "")
                            && !flag){
                        updatedItemVO.setNumberingToggled(false);
                        tocTree.getDataProvider().refreshItem(updatedItemVO);
                        enableSave(true);
                    }else if(isToggledByUser){
                        enableSave(false);
                    }
                } else {
                    enableSave(false);
                }
                if(!isToggledByUser){
                    isToggledByUser = true;
                }
            }
        });

        return buttons;
    }

    private TextField buildTocTypeField(Binder<TableOfContentItemVO> binder) {
        TextField itemTypeField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.type"));
        itemTypeField.setReadOnly(true);
        binder.forField(itemTypeField)
                .withConverter(new Converter<String, TocItemType>() {
                    @Override
                    public Result<TocItemType> convertToModel(String value, ValueContext context) {
                        return value == null
                                ? Result.ok(null)
                                : Result.ok(getTypeName(value));
                    }

                    @Override
                    public String convertToPresentation(TocItemType value, ValueContext context) {
                        return TableOfContentHelper.getDisplayableItemType(value, messageHelper);
                    }
                })
                .bind(TableOfContentItemVO::getType, TableOfContentItemVO::setType);

        return itemTypeField;
    }

    private RichTextArea buildContentField(Binder<TableOfContentItemVO> binder) {
        RichTextArea contentField = new RichTextArea(messageHelper.getMessage("toc.edit.window.item.selected.content"));
        contentField.setStyleName("toc-content-area");
        contentField.setReadOnly(true);
        binder.forField(contentField)
                .withNullRepresentation("")
                .bind(TableOfContentItemVO::getContent, TableOfContentItemVO::setContent);
        return contentField;
    }

    private TextField buildNumberField(Binder<TableOfContentItemVO> binder) {
        final TextField numberField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.number"));
        numberField.setEnabled(false);
        
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
                if (tocEditor.isArabicNumberingOnly(itemTypeField.getValue())) {
                    binder.forField(numberField)
                            .withNullRepresentation("")
                            .withValidator(new RegexpValidator(messageHelper.getMessage("toc.edit.window.item.number.arabic.error", itemTypeField.getValue()), ARABIC_REGEX))
                            .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)
                            .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);
                } else if (tocEditor.isRomanNumberingOnly(itemTypeField.getValue())) {
                    binder.forField(numberField)
                            .withNullRepresentation("")
                            .withValidator(new RegexpValidator(messageHelper.getMessage("toc.edit.window.item.number.roman.error", itemTypeField.getValue()), ROMAN_REGEX))
                            .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)
                            .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);
                } else {
                    binder.forField(numberField)
                            .withNullRepresentation("")
                            .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)// TODO unescapeXml might need to be removed ones LEOS-515 is resolved
                            .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);
                }
                
                enableSave(binder.validate().isOk());
            }
        });
        return numberField;
    }

    private TextField buildHeadingField(Binder<TableOfContentItemVO> binder) {
        final TextField headingField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.heading"));
        headingField.setEnabled(false);
        headingField.setWidth(100, Unit.PERCENTAGE);
        binder.forField(headingField)
                .withNullRepresentation("")
                .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)// TODO unescapeXml might need to be removed ones LEOS-515
                                                                                                    // is resolved
                .bind(TableOfContentItemVO::getHeading, TableOfContentItemVO::setHeading);

        headingField.addValueChangeListener(event -> {
            if (event.isUserOriginated()) {
                TableOfContentItemVO item = binder.getBean();
                item.setHeading(event.getValue());
                tocTree.getDataProvider().refreshItem(item);

                enableSave(true);
            }
        });
        return headingField;
    }

    private TocItemType getTypeName(String name) {
        for (TocItemType type : tocItemTypes) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}