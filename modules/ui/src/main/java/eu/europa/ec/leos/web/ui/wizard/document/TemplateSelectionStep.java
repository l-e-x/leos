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
package eu.europa.ec.leos.web.ui.wizard.document;

import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.vaadin.data.Item;
import com.vaadin.data.Property;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.FieldEvents;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.ui.converter.CatalogUtil;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import eu.europa.ec.leos.web.ui.wizard.WizardStep;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TemplateSelectionStep extends CustomComponent implements WizardStep {

    private static final long serialVersionUID = 6601694410770223043L;
    private static final Logger LOG = LoggerFactory.getLogger(TemplateSelectionStep.class);
    private static final String DEFAULT_ITEM_DESCRIPTION = "&nbsp;";

    private VerticalLayout mainLayout;
    private HierarchicalContainer catalogContainer;
    private Label descriptionLabel;
    private ListSelect langSelector;
    private Tree tree;
    private String selectedItemId;

    private MessageHelper messageHelper;
    private DocumentCreateWizardVO documentCreateWizardVO;

    public TemplateSelectionStep(DocumentCreateWizardVO documentCreateWizardVO, List<CatalogItem> templateItems, MessageHelper messageHelper) {
        catalogContainer = CatalogUtil.getCatalogContainer(templateItems);

        this.messageHelper = messageHelper;
        this.documentCreateWizardVO = documentCreateWizardVO;

        initLayout();
        buildContent();
    }

    @Override
    public String getStepTitle() {
        return messageHelper.getMessage("wizard.document.create.template.title");
    }

    @Override
    public String getStepDescription() {
        return messageHelper.getMessage("wizard.document.create.template.desc");
    }

    @Override
    public Component getComponent() {
        return this;
    }

    private void initLayout() {
        // component will use all available space
        setSizeFull();

        // create layout
        mainLayout = new VerticalLayout();
        setCompositionRoot(mainLayout);

        // layout will use all available space
        mainLayout.setSizeFull();

        // set margins and spacing
        mainLayout.setMargin(new MarginInfo(false, true, false, true));
        mainLayout.setSpacing(true);
    }

    private void buildContent() {
        mainLayout.addComponent(buildTemplateFilter());

        Component tree = buildTemplateTree();

        // wrap tree in panel to provide scroll bars
        Panel treePanel = new Panel(tree);
        treePanel.setSizeFull();

        mainLayout.addComponent(treePanel);
        mainLayout.setExpandRatio(treePanel, 1.0f);

        mainLayout.addComponent(buildTemplateDescription());
        mainLayout.addComponent(buildDocumentName());
    }

    private Component buildTemplateFilter() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("100%");

        // set margins and spacing
        layout.setMargin(false);
        layout.setSpacing(true);

        Button expandButton = buildExpandButton();
        layout.addComponent(expandButton);
        layout.setComponentAlignment(expandButton, Alignment.BOTTOM_LEFT);

        TextField filterField = buildFilterField();
        layout.addComponent(filterField);
        layout.setComponentAlignment(filterField, Alignment.BOTTOM_LEFT);

        buildLanguageSelector();
        layout.addComponent(langSelector);
        layout.setComponentAlignment(langSelector, Alignment.BOTTOM_LEFT);

        // filter and language will expand
        layout.setExpandRatio(filterField, 1.0f);
        layout.setExpandRatio(langSelector, 0.5f);

        return layout;
    }

    private Button buildExpandButton() {
        // expand tree button
        final Button expandButton = new Button();
        expandButton.setData(Boolean.TRUE);
        expandButton.setDescription(messageHelper.getMessage("wizard.document.create.template.expand"));
        expandButton.setIcon(LeosTheme.TREE_EXPAND_ICON_16);
        expandButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = -8583154936312644542L;

            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                LOG.trace("Expand button was clicked!");
                if (Boolean.TRUE.equals(expandButton.getData())) {
                    // if data is true, expand it. Next click will trigger a collapse
                    expandEnabledItems(tree.rootItemIds());
                    expandButton.setData(Boolean.FALSE);
                    expandButton.setDescription(messageHelper.getMessage("wizard.document.create.template.collapse"));
                } else {
                    // if data is false, collapse it. Next click will trigger an expand
                    for (Object rootId : tree.rootItemIds()) {
                        tree.collapseItemsRecursively(rootId);
                    }
                    expandButton.setData(Boolean.TRUE);
                    expandButton.setDescription(messageHelper.getMessage("wizard.document.create.template.expand"));
                }
            }

            private void expandEnabledItems(Collection<?> itemIds) {
                for (Object itemId : itemIds) {
                    if (itemId != null) {
                        Item item = catalogContainer.getItem(itemId);
                        if (item != null) {
                            Boolean enabled = (Boolean) item.getItemProperty(CatalogUtil.ENABLED_PROPERTY).getValue();
                            if (Boolean.TRUE.equals(enabled)) {
                                // expand only enabled items
                                boolean expanded = tree.expandItem(itemId);
                                if (expanded) {
                                    // recursively expand item children
                                    expandEnabledItems(tree.getChildren(itemId));
                                }
                            } else {
                                // collapse disabled items
                                tree.collapseItemsRecursively(itemId);
                            }
                        }
                    }
                }
            }
        });

        return expandButton;
    }

    private TextField buildFilterField() {
        // filter input field
        TextField filterField = new TextField(messageHelper.getMessage("wizard.document.create.template.type"));
        filterField.setWidth("100%");

        // instant filtering on text input
        filterField.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.LAZY);
        filterField.setTextChangeTimeout(200);
        filterField.addTextChangeListener(new FieldEvents.TextChangeListener() {
            private static final long serialVersionUID = -1463898400639639733L;

            @Override
            public void textChange(FieldEvents.TextChangeEvent event) {
                String filter = event.getText();
                LOG.trace("Filter text changed: text='{}'", filter);
                // filter catalog container
                catalogContainer.removeAllContainerFilters();
                if (StringUtils.isNotEmpty(filter)) {
                    LOG.trace("Filtering item container: {} like %{}%", CatalogUtil.NAME_PROPERTY, filter);
                    catalogContainer.addContainerFilter(CatalogUtil.NAME_PROPERTY, filter, true, false);
                    // if adding additional filters, only items accepted by all filters are visible.
                }
            }
        });
        return filterField;
    }

    private void buildLanguageSelector() {
        // language selection
        langSelector = new ListSelect(messageHelper.getMessage("wizard.document.create.template.language"));
        langSelector.addContainerProperty(CatalogUtil.LANG_PROPERTY, String.class, null);
        langSelector.addContainerProperty(CatalogUtil.NAME_PROPERTY, String.class, null);
        langSelector.setItemCaptionPropertyId(CatalogUtil.NAME_PROPERTY);
        langSelector.setNullSelectionAllowed(false);
        langSelector.setEnabled(false);
        langSelector.setRows(1);
        langSelector.setWidth("100%");
    }

    private Component buildTemplateTree() {

        tree = new Tree();
        tree.setRequired(true);

        // initialize container
        tree.setContainerDataSource(catalogContainer);

        // react immediately to user interaction
        tree.setImmediate(true);

        // show name property as caption for items
        tree.setItemCaptionPropertyId(CatalogUtil.NAME_PROPERTY);
        tree.setItemCaptionMode(AbstractSelect.ItemCaptionMode.PROPERTY);

        // show icon property as icon for items
        tree.setItemIconPropertyId(CatalogUtil.ICON_PROPERTY);

        // tree item style generator
        tree.setItemStyleGenerator(new Tree.ItemStyleGenerator() {
            private static final long serialVersionUID = 8877302973597223335L;

            @Override
            public String getStyle(Tree tree, Object itemId) {
                String itemStyle = null;
                if (itemId != null) {
                    Item item = catalogContainer.getItem(itemId);
                    if (item != null) {
                        // check if the
                        Boolean isItemDisabled = Boolean.FALSE.equals(item.getItemProperty(CatalogUtil.ENABLED_PROPERTY).getValue());

                        Object parentItemId = tree.getParent(itemId);
                        Item parentItem = tree.getItem(parentItemId);
                        Boolean isParentDisabled = parentItem != null &&
                                Boolean.FALSE.equals(parentItem.getItemProperty(CatalogUtil.ENABLED_PROPERTY).getValue());

                        // as the styles are cascading, set the disabled style only if the parent is not disabled
                        if (isItemDisabled && !isParentDisabled) {
                            itemStyle = "disabled";
                        }
                    }
                }
                return itemStyle;
            }
        });

        // tree selection change listener
        tree.addValueChangeListener(new Property.ValueChangeListener() {
            private static final long serialVersionUID = 1811224448936527668L;

            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                selectedItemId = ObjectUtils.toString(event.getProperty().getValue(), null);
                LOG.trace("Tree selection changed: id='{}'", selectedItemId);

                String itemDesc = null;
                Map<String, String> itemLangs = null;

                if (selectedItemId != null) {
                    Item item = catalogContainer.getItem(selectedItemId);
                    if (item != null) {
                        CatalogItem.ItemType itemType = (CatalogItem.ItemType) item.getItemProperty(CatalogUtil.TYPE_PROPERTY).getValue();
                        Boolean enabled = (Boolean) item.getItemProperty(CatalogUtil.ENABLED_PROPERTY).getValue();
                        if (Boolean.TRUE.equals(enabled)) {
                            // handle item description
                            itemDesc = ObjectUtils.toString(item.getItemProperty(CatalogUtil.DESC_PROPERTY).getValue(), null);
                            if (StringUtils.isBlank(itemDesc)) {
                                itemDesc = ObjectUtils.toString(item.getItemProperty(CatalogUtil.NAME_PROPERTY).getValue(), null);
                            }
                            if (CatalogItem.ItemType.TEMPLATE == itemType) {
                                // handle item languages
                                itemLangs = (Map<String, String>) item.getItemProperty(CatalogUtil.LANG_PROPERTY).getValue();
                            }
                        }
                    } else {
                        LOG.warn("Item not found in container: id='{}'", selectedItemId);
                    }
                }

                // update item description
                itemDesc = StringUtils.defaultIfBlank(itemDesc, DEFAULT_ITEM_DESCRIPTION);
                LOG.trace("Updating selected item description: {}", itemDesc);
                descriptionLabel.setValue(itemDesc);

                // update item languages
                langSelector.removeAllItems();
                if ((itemLangs != null) && !itemLangs.isEmpty()) {
                    for (Map.Entry<String, String> entry : itemLangs.entrySet()) {
                        Item item = langSelector.addItem(entry.getKey());
                        item.getItemProperty(CatalogUtil.LANG_PROPERTY).setValue(entry.getKey());
                        item.getItemProperty(CatalogUtil.NAME_PROPERTY).setValue(entry.getValue());
                    }
                    langSelector.select(CatalogUtil.DEFAULT_LANGUAGE);
                    langSelector.setEnabled(true);
                } else {
                    langSelector.setEnabled(false);
                }
            }
        });

        // make tree scrollable by wrapping it in a panel
        Panel container = new Panel();
        container.setContent(tree);
        container.setSizeFull();
        return container;
    }

    private Component buildTemplateDescription() {
        descriptionLabel = new Label(DEFAULT_ITEM_DESCRIPTION, ContentMode.HTML);
        return descriptionLabel;
    }

    private Component buildDocumentName() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidth("100%");

        // no margins and no spacing
        layout.setMargin(false);
        layout.setSpacing(true);

        return layout;
    }

    @Override
    public boolean validateState() {
        boolean isValid = true;

        // in this state we validate that the name was filled in and that an item was selected from the tree
        if (selectedItemId == null || Boolean.FALSE.equals(tree.getItem(selectedItemId).getItemProperty(CatalogUtil.ENABLED_PROPERTY).getValue())) {
            tree.setComponentError(new UserError(messageHelper.getMessage("wizard.document.create.template.error.item"), AbstractErrorMessage.ContentMode.TEXT,
                    ErrorMessage.ErrorLevel.WARNING));
            isValid = false;
        } else {
            tree.setComponentError(null);
        }

        if (isValid) {
            Item item = tree.getItem(selectedItemId);

            documentCreateWizardVO.setTemplateId(selectedItemId);
            documentCreateWizardVO.setTemplateName((String) item.getItemProperty(CatalogUtil.NAME_PROPERTY).getValue());
            documentCreateWizardVO.setTemplateDescription((String) item.getItemProperty(CatalogUtil.DESC_PROPERTY).getValue());
            documentCreateWizardVO.setTemplateLanguage((String)langSelector.getValue());
        }

        return isValid;
    }

    @Override
    public boolean canFinish() {
        return false;
    }
}
