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
package eu.europa.ec.leos.web.ui.converter;

import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.server.ThemeResource;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class CatalogUtil {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogUtil.class);

    public static final String TYPE_PROPERTY = "type";
    public static final String NAME_PROPERTY = "name";
    public static final String DESC_PROPERTY = "desc";
    public static final String LANG_PROPERTY = "lang";
    public static final String ENABLED_PROPERTY = "enabled";
    public static final String ICON_PROPERTY = "icon";

    public static final String DEFAULT_LANGUAGE = "EN";

    public static HierarchicalContainer getCatalogContainer(List<CatalogItem> templateItems) {

        HierarchicalContainer container = new HierarchicalContainer();

        // create container properties
        container.addContainerProperty(TYPE_PROPERTY, CatalogItem.ItemType.class, null);
        container.addContainerProperty(NAME_PROPERTY, String.class, null);
        container.addContainerProperty(DESC_PROPERTY, String.class, null);
        container.addContainerProperty(LANG_PROPERTY, Map.class, null);
        container.addContainerProperty(ENABLED_PROPERTY, Boolean.class, Boolean.FALSE);
        container.addContainerProperty(ICON_PROPERTY, ThemeResource.class, null);

        populate(container, null, templateItems);
        return container;
    }

    @SuppressWarnings("unchecked")
    private static void populate(HierarchicalContainer container, CatalogItem parent, List<CatalogItem> itemList) {
        if ((container != null) && (itemList != null)) {
            String lang = DEFAULT_LANGUAGE;
            for (CatalogItem ctgItem : itemList) {
                Item item = container.addItem(ctgItem.getId());
                item.getItemProperty(TYPE_PROPERTY).setValue(ctgItem.getType());
                item.getItemProperty(NAME_PROPERTY).setValue(ctgItem.getName(lang));
                item.getItemProperty(DESC_PROPERTY).setValue(ctgItem.getDescription(lang));
                item.getItemProperty(ENABLED_PROPERTY).setValue(ctgItem.isEnabled());

                if (parent != null) {
                    container.setParent(ctgItem.getId(), parent.getId());
                }

                switch (ctgItem.getType()) {
                    case CATEGORY:
                        item.getItemProperty(ICON_PROPERTY).setValue(LeosTheme.TREE_CATEGORY_ICON_16);
                        boolean hasChildren = ctgItem.getItems().size() > 0;
                        container.setChildrenAllowed(ctgItem.getId(), hasChildren);
                        // recursively populate container with child items
                        populate(container, ctgItem, ctgItem.getItems());
                        break;
                    case TEMPLATE:
                        item.getItemProperty(LANG_PROPERTY).setValue(ctgItem.getLanguages());
                        item.getItemProperty(ICON_PROPERTY).setValue(LeosTheme.TREE_TEMPLATE_ICON_16);
                        container.setChildrenAllowed(ctgItem.getId(), false);
                        break;
                    default:
                        LOG.warn("Unknown catalog item type: {}", ctgItem.getType());
                }
            }
        }
    }
}
