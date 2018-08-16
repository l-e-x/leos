/*
 * Copyright 2018 European Commission
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
import com.vaadin.data.*;
import com.vaadin.event.selection.MultiSelectionEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.dnd.DropEffect;
import com.vaadin.shared.ui.dnd.EffectAllowed;
import com.vaadin.shared.ui.grid.DropMode;
import com.vaadin.ui.*;
import com.vaadin.ui.components.grid.GridDragStartListener;
import com.vaadin.ui.components.grid.TreeGridDragSource;
import com.vaadin.ui.dnd.DragSourceExtension;
import com.vaadin.ui.dnd.event.DragStartListener;
import com.vaadin.ui.renderers.HtmlRenderer;
import cool.graph.cuid.Cuid;
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
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.window.AbstractEditChangeMonitorWindow;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.teemusa.gridextensions.client.tableselection.TableSelectionState.TableSelectionMode;
import org.vaadin.teemusa.gridextensions.tableselection.TableSelectionModel;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EditTocWindow<T extends TocItemType> extends AbstractEditChangeMonitorWindow {

    private static final long serialVersionUID = -5533529522081984L;

    private static final Logger LOG = LoggerFactory.getLogger(EditTocWindow.class);

    private MultiSelectTreeGrid<TableOfContentItemVO> tocTree;
    private final Map<T, List<T>> tableOfContentRules;
    private final T[] tocItemTypes;
    private final TocEditor tocEditor;
    
    private boolean userOriginated = false;// Kludge as dataProviderListener is not smart enough
    private Label statusLabel;
    private final String STATUS_STYLE = "leos-toc-tree-status";
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public EditTocWindow(final MessageHelper messageHelper, final EventBus eventBus, final ConfigurationHelper cfgHelper,
            final Map<T, List<T>> tableOfContentRules, final T[] tocItemTypes, TocEditor numberEditor) {
        super(messageHelper, eventBus);
        this.tableOfContentRules = tableOfContentRules;
        this.tocItemTypes = tocItemTypes;
        this.tocEditor = numberEditor;

        setWidth(800, Unit.PIXELS);
        setHeight(700, Unit.PIXELS);
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
        tocTree.select(newTocData.getRootItems().get(0));
        userOriginated = true;
        // this tree is going to be always extended by default
        expandTree(tocTree.getTreeData().getRootItems());
    }

    @Subscribe
    public void refreshTocData(RefreshTocWindowEvent event) {
        setTableOfContent(event.getTocData());
    }

    private void expandTree(List<TableOfContentItemVO> items) {
        items.forEach(item -> {
            tocTree.expand(item);
            if (!item.getChildItems().isEmpty()) {
                expandTree(item.getChildItems());
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
        final Editor tocItemEditor = new Editor();
        tocItemEditor.setMargin(true);
        tocTree.asMultiSelect().addValueChangeListener(tocItemEditor);

        Panel itemEditionPanel = new Panel(messageHelper.getMessage("toc.edit.window.item.selected"));
        itemEditionPanel.addStyleName("leos-toc-panel");
        itemEditionPanel.setContent(tocItemEditor);

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
                Label itemLabel = new Label(TableOfContentItemConverter.getDisplayableItemType(type, messageHelper));
                itemLabel.setWidth(80, Unit.PIXELS);
                itemLabel.setStyleName("leos-dragItem");

                DragSourceExtension<Label> dragSourceExtension = new DragSourceExtension<>(itemLabel);
                dragSourceExtension.addDragStartListener((DragStartListener<Label>) event -> {
                    String number = null, heading = null;
                    if (type.hasItemNumber()) {
                        number = messageHelper.getMessage("toc.item.type.number");
                    }
                    if (type.hasItemHeading()) {
                        heading = messageHelper.getMessage("toc.item.type." + type.getName().toLowerCase() + ".heading");
                    }
                    TableOfContentItemVO dragData = new TableOfContentItemVO(type, Cuid.createCuid(), null, number, null, heading, null, null, null);
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
            String iconHtml = "";
            if(tableOfContentItemVO.areChildrenAllowed()) {
                iconHtml = VaadinIcons.FOLDER_O.getHtml();
            }
            return iconHtml + " " + TableOfContentItemConverter.buildItemCaption(tableOfContentItemVO,
                    TableOfContentItemConverter.DEFAULT_CAPTION_MAX_SIZE, messageHelper);
            }, new HtmlRenderer());

        TableSelectionModel<TableOfContentItemVO> model = new TableSelectionModel();
        model.setMode(TableSelectionMode.CTRL);
        tocTree.setSelectionModel(model);

        tocTree.removeHeaderRow(tocTree.getDefaultHeaderRow());
        tocTree.addStyleName("leos-toc-tree");
        tocTree.setSizeFull();

        model.addMultiSelectionListener(listener -> handleMultiSelection(listener));

        setDragData();

        addDropTargetListener();

        tocTree.getDataProvider().addDataProviderListener(event -> {
            if (userOriginated) {
                LOG.debug("data changed logged in DataProviderListener");
                enableSave();
            }
        });

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
        dropTarget.addTreeGridDropListener(new EditTocDropHandler<>(tocTree, messageHelper, eventBus, tableOfContentRules, tocEditor));
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
    class Editor extends HorizontalLayout implements HasValue.ValueChangeListener<Set<TableOfContentItemVO>> {

        Binder<TableOfContentItemVO> formBinder = new Binder<>();

        private Button deleteButton;
        private TextField itemTypeField;
        private TextField headingField;
        private TextField numberField;

        public Editor() {
            initEditor();
        }

        private void initEditor() {
            setSizeFull();

            itemTypeField = buildTocTypeField(formBinder);
            this.addComponent(itemTypeField);
            numberField = buildNumberField(formBinder);
            this.addComponent(numberField);
            headingField = buildHeadingField(formBinder);
            this.addComponent(headingField);
            this.setExpandRatio(headingField, 1.0f);
            deleteButton = buildDeleteButton(formBinder);
            this.addComponent(deleteButton);
            this.setComponentAlignment(deleteButton, Alignment.BOTTOM_RIGHT);
        }

        // selection value change for tree
        @Override
        public void valueChange(HasValue.ValueChangeEvent<Set<TableOfContentItemVO>> event) {
            boolean editionEnabled = false;
            boolean numberFieldEnabled = true;
            boolean headingFieldEnabled = true;
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
                } else if (item != null) {
                    formBinder.setBean(item);
                    TocItemType type = item.getType();
                    numberFieldEnabled = tocEditor.isNumFieldEditable(item);
                    headingFieldEnabled = type.hasItemHeading();
                    editionEnabled = !type.isRoot() && type.isDraggable();

                    deleteButton.setData(item);
                }
            }
            numberField.setEnabled(editionEnabled && numberFieldEnabled);
            headingField.setEnabled(editionEnabled && headingFieldEnabled);
            deleteButton.setEnabled(editionEnabled);
        }
    }

    private Button buildDeleteButton(Binder<TableOfContentItemVO> binder) {
        final Button button = new Button(messageHelper.getMessage("toc.edit.window.item.selected.delete"));
        button.setEnabled(false);
        button.addClickListener(event -> {
            TableOfContentItemVO item = binder.getBean();
            if (tocTree.getTreeData().contains(item)) {
                tocTree.getTreeData().removeItem(item);
                tocTree.getDataProvider().refreshAll();
                tocTree.deselectAll();
                eventBus.post(new TocStatusUpdateEvent(messageHelper.getMessage("toc.edit.window.delete.message",
                        TableOfContentItemConverter.getDisplayableItemType(item.getType(), messageHelper)), TocStatusUpdateEvent.Result.SUCCESSFUL));
                enableSave();
            }
        });
        return button;
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
                        return TableOfContentItemConverter.getDisplayableItemType(value, messageHelper);
                    }
                })
                .bind(TableOfContentItemVO::getType, TableOfContentItemVO::setType);

        return itemTypeField;
    }

    private TextField buildNumberField(Binder<TableOfContentItemVO> binder) {
        final TextField numberField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.number"));
        numberField.setEnabled(false);

        binder.forField(numberField)
                .withNullRepresentation("")
                .withConverter(StringEscapeUtils::unescapeXml, StringEscapeUtils::escapeXml10, null)// TODO unescapeXml might need to be removed ones LEOS-515
                                                                                                    // is resolved
                .bind(TableOfContentItemVO::getNumber, TableOfContentItemVO::setNumber);

        numberField.addValueChangeListener(event -> {
            if (event.isUserOriginated()) {
                TableOfContentItemVO item = binder.getBean();
                item.setNumber(event.getValue());
                tocTree.getDataProvider().refreshItem(item);

                enableSave();
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

                enableSave();
            }
        });
        return headingField;
    }

    private T getTypeName(String name) {
        for (T type : tocItemTypes) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
