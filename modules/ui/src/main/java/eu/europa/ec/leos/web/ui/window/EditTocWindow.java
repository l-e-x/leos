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
package eu.europa.ec.leos.web.ui.window;

import java.util.List;
import java.util.Map;

import com.vaadin.ui.*;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Container;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.shared.ui.MarginInfo;

import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.window.CloseTocEditorEvent;
import eu.europa.ec.leos.web.event.window.SaveTocRequestEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.toc.EditTocDropHandler;
import eu.europa.ec.leos.web.ui.component.toc.EditTocTree;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;

public class EditTocWindow extends AbstractEditChangeMonitorWindow {

    private static final long serialVersionUID = -5533884503302959595L;

    private static final Logger LOG = LoggerFactory.getLogger(EditTocWindow.class);

    private Tree tocTree;
    private Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules;

    public EditTocWindow(MessageHelper messageHelper, EventBus eventBus, ConfigurationHelper cfgHelper, Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules) {
        super(messageHelper, eventBus);
        this.tableOfContentRules = tableOfContentRules;

        setWidth(800, Unit.PIXELS);
        setHeight(700, Unit.PIXELS);
        setCaption(messageHelper.getMessage("toc.edit.window.title"));

        VerticalLayout verticalLayout = buildWindowBody();
        addButtonOnLeft(buildDapButton(cfgHelper.getProperty("leos.dap.edit.toc.url")));
        setBodyComponent(verticalLayout);
    }

    public void setTableOfContent(Container tocContainer) {
        tocTree.setContainerDataSource(tocContainer);

        // this tree is going to be always extended by default
        for (Object rootId : tocTree.rootItemIds()) {
            tocTree.expandItemsRecursively(rootId);
        }
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
        final FormLayout tocItemForm = new FormLayout();

        final TextField itemTypeField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.type"));
        itemTypeField.setNullRepresentation("");
        itemTypeField.setReadOnly(true);
        tocItemForm.addComponent(itemTypeField);

        final TextField numberField = buildNumberField(tocItemForm);
        numberField.setEnabled(false);
        tocItemForm.addComponent(numberField);

        final TextField headingField = buildHeadingField(tocItemForm);
        headingField.setEnabled(false);
        tocItemForm.addComponent(headingField);

        final Button deleteButton = buildDeleteButton();

        tocTree.addValueChangeListener(new Property.ValueChangeListener() {

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                // is mandatory to set it first to null!
                // the set value in the textField will trigger a value change event which will be ignored if the tocItemForm data is null
                tocItemForm.setData(null);

                Object itemId = event.getProperty().getValue();
                LOG.trace("ToC selection changed: id='{}'", itemId);
                Item item = tocTree.getItem(event.getProperty().getValue());
                itemTypeField.setReadOnly(false);
                boolean editionEnabled = false;
                boolean numberFieldEnabled = true;

                if (item != null) {
                    TableOfContentItemVO.Type type = (TableOfContentItemVO.Type) item.getItemProperty(TableOfContentItemConverter.TYPE_PROPERTY).getValue();
                    editionEnabled = !type.isRoot();
                    if (type.name().equalsIgnoreCase(TableOfContentItemVO.Type.ARTICLE.name())) {
                        numberFieldEnabled = false;
                    }
                    itemTypeField.setValue(TableOfContentItemConverter.getDisplayableItemType(type, messageHelper));
                    String number = (String) item.getItemProperty(TableOfContentItemConverter.NUMBER_PROPERTY).getValue();
                    numberField.setValue(StringEscapeUtils.unescapeXml(number)); // TODO unescapeXml might need to be removed ones LEOS-515 is resolved
                    String heading = (String) item.getItemProperty(TableOfContentItemConverter.HEADING_PROPERTY).getValue();
                    headingField.setValue(StringEscapeUtils.unescapeXml(heading)); // TODO unescapeXml might need to be removed ones LEOS-515 is resolved
                    deleteButton.setData(itemId);
                } else {
                    itemTypeField.setValue(null);
                    numberField.setValue(null);
                    headingField.setValue(null);
                    deleteButton.setData(null);
                }

                numberField.setEnabled(editionEnabled && numberFieldEnabled);
                headingField.setEnabled(editionEnabled);
                deleteButton.setEnabled(editionEnabled);
                itemTypeField.setReadOnly(true);
                tocItemForm.setData(itemId);
            }
        });

        VerticalLayout selectedItemBody = new VerticalLayout();
        selectedItemBody.setMargin(new MarginInfo(false, false, true, true));
        selectedItemBody.addComponent(tocItemForm);
        selectedItemBody.setExpandRatio(tocItemForm, 1.0f);
        selectedItemBody.addComponent(deleteButton);

        Panel itemEditionPanel = new Panel(messageHelper.getMessage("toc.edit.window.item.selected"));
        itemEditionPanel.setContent(selectedItemBody);

        return itemEditionPanel;
    }

    private Button buildDeleteButton() {
        final Button button = new Button(messageHelper.getMessage("toc.edit.window.item.selected.delete"));
        button.setEnabled(false);
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                Object itemId = event.getButton().getData();
                final HierarchicalContainer container = (HierarchicalContainer) tocTree.getContainerDataSource();
                container.removeItemRecursively(itemId);
                tocTree.select(null);
            }
        });
        return button;
    }

    private TextField buildNumberField(final FormLayout tocItemForm) {
        final TextField numberField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.number"));
        numberField.setNullRepresentation("");
        numberField.setImmediate(true);
        numberField.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                String updatedNumber = (String) event.getProperty().getValue();
                Object itemId = tocItemForm.getData();
                updateItemProperty(StringEscapeUtils.escapeXml(updatedNumber), TableOfContentItemConverter.NUMBER_PROPERTY, itemId);
            }
        });
        return numberField;
    }

    private TextField buildHeadingField(final FormLayout tocItemForm) {
        final TextField headingField = new TextField(messageHelper.getMessage("toc.edit.window.item.selected.heading"));
        headingField.setImmediate(true);
        headingField.setNullRepresentation("");
        headingField.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                String updatedHeading = (String) event.getProperty().getValue();
                Object itemId = tocItemForm.getData();
                updateItemProperty(StringEscapeUtils.escapeXml(updatedHeading), TableOfContentItemConverter.HEADING_PROPERTY, itemId);
            }
        });
        return headingField;
    }

    private void updateItemProperty(String updatedValue, String updatedProperty, Object itemId) {
        if (itemId != null) {
            Item item = tocTree.getItem(itemId);

            item.getItemProperty(updatedProperty).setValue(updatedValue);

            TableOfContentItemVO.Type type = (TableOfContentItemVO.Type) item.getItemProperty(TableOfContentItemConverter.TYPE_PROPERTY).getValue();
            String heading = (String) item.getItemProperty(TableOfContentItemConverter.HEADING_PROPERTY).getValue();
            String number = (String) item.getItemProperty(TableOfContentItemConverter.NUMBER_PROPERTY).getValue();

            String itemDescription = TableOfContentItemConverter.buildItemDescription(number, heading, type, messageHelper);

            item.getItemProperty(TableOfContentItemConverter.CAPTION_PROPERTY).setValue(itemDescription);
            item.getItemProperty(TableOfContentItemConverter.DESC_PROPERTY).setValue(itemDescription);

            enableSave();
        }
    }

    /**
     * @return
     */
    private Panel buildTocItemPanel() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.setWidth(100, Unit.PERCENTAGE);
        gridLayout.setHeight(80, Unit.PERCENTAGE);
        gridLayout.setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);
        gridLayout.setSpacing(true);
        gridLayout.setMargin(true);
        for (TableOfContentItemVO.Type type : TableOfContentItemVO.Type.values()) {
            if (!type.isRoot() && type.isDraggable()) {
                Label itemLabel = new Label(TableOfContentItemConverter.getDisplayableItemType(type, messageHelper));
                itemLabel.setWidth(80, Unit.PIXELS);
                itemLabel.setStyleName("leos-dragItem");
                itemLabel.setId(type.name());
                DragAndDropWrapper dragAndDropWrapper = new DragAndDropWrapper(itemLabel);
                dragAndDropWrapper.setData(type);
                dragAndDropWrapper.setSizeUndefined();
                dragAndDropWrapper.setDragStartMode(DragAndDropWrapper.DragStartMode.WRAPPER);
                gridLayout.addComponent(dragAndDropWrapper);
            }
        }
        Panel panel = new Panel(messageHelper.getMessage("toc.edit.window.items"));
        panel.setSizeFull();
        panel.setContent(gridLayout);
        return panel;
    }

    private Panel buildTocTree() {

        tocTree = new EditTocTree();

        tocTree.setDragMode(Tree.TreeDragMode.NODE);
        tocTree.setDropHandler(new EditTocDropHandler(tocTree, messageHelper, eventBus, tableOfContentRules));

        // configure toc tree
        tocTree.setImmediate(true);
        tocTree.setItemCaptionPropertyId(TableOfContentItemConverter.CAPTION_PROPERTY);
        tocTree.setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);

        // this tree is going to be always extended by default
        for (Object rootId : tocTree.rootItemIds()) {
            tocTree.expandItemsRecursively(rootId);
        }
        tocTree.addItemSetChangeListener(new ItemSetChangeListener() {
            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                LOG.debug("data changed logged in ItemSetChangeListener");
                enableSave();
            }
        });

        // wrap tree in panel to provide scroll bars
        final Panel treePanel = new Panel(messageHelper.getMessage("toc.edit.window.toc"));
        treePanel.setContent(tocTree);
        treePanel.setSizeFull();

        return treePanel;
    }

    @Override
    protected void onSave() {
        List<TableOfContentItemVO> list = TableOfContentItemConverter.buildTocItemVOList((HierarchicalContainer) tocTree.getContainerDataSource());
        eventBus.post(new SaveTocRequestEvent(list));
        super.onSave();
    }

    @Override
    public void close() {
        super.close();
        eventBus.post(new RefreshDocumentEvent());
        eventBus.post(new CloseTocEditorEvent());
    }

}