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
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TableOfContentItemConverter {

    public static final String XML_ID_PROPERTY = "xmlId";
    public static final String TYPE_PROPERTY = "type";
    public static final String NUMBER_PROPERTY = "number";
    public static final String HEADING_PROPERTY = "heading";
    public static final String DESC_PROPERTY = "desc";
    public static final String CAPTION_PROPERTY = "caption";
    public static final String NUM_TAG_INDEX_PROPERTY = "numTagIndex";
    public static final String HEADING_TAG_INDEX_PROPERTY = "headingTagIndex";
    public static final String INDEX_PROPERTY = "index";

    private static final int DEFAULT_CAPTION_MAX_SIZE = 50;

    public static HierarchicalContainer buildTocContainer(List<TableOfContentItemVO> tableOfContentsItemVOList, MessageHelper messageHelper) {
        return buildTocContainer(tableOfContentsItemVOList, DEFAULT_CAPTION_MAX_SIZE, messageHelper);
    }

    public static HierarchicalContainer buildTocContainer(List<TableOfContentItemVO> tableOfContentsItemVOList, int captionMaxSize, MessageHelper messageHelper) {
        // initialize container and its properties
        HierarchicalContainer container = initContainer();

        // populate container with items
        populate(container, null, tableOfContentsItemVOList, captionMaxSize, messageHelper);

        return container;
    }

    public static List<TableOfContentItemVO> buildTocItemVOList(HierarchicalContainer tocContainer) {
        List<?> itemIds = tocContainer.getItemIds();
        return buildTocItemVoList(tocContainer, itemIds, null);
    }

    private static List<TableOfContentItemVO> buildTocItemVoList(HierarchicalContainer tocContainer, Collection<?> itemIds, Object parentId) {
        List<TableOfContentItemVO> list = new ArrayList<TableOfContentItemVO>();
        for (Object itemId : itemIds) {
            Item item = tocContainer.getItem(itemId);
            if (tocContainer.getParent(itemId) == null || tocContainer.getParent(itemId).equals(parentId)) {

                TableOfContentItemVO tocItem = buildTableOfContentItemVO(item);

                if (tocContainer.hasChildren(itemId)) {
                    tocItem.addAllChildItems(buildTocItemVoList(tocContainer, tocContainer.getChildren(itemId), itemId));
                }
                list.add(tocItem);
            }

        }
        return list;
    }

    private static TableOfContentItemVO buildTableOfContentItemVO(Item item) {
        TableOfContentItemVO.Type type = (TableOfContentItemVO.Type) item.getItemProperty(TYPE_PROPERTY).getValue();
        String id = (String) item.getItemProperty(XML_ID_PROPERTY).getValue();
        String number = (String) item.getItemProperty(NUMBER_PROPERTY).getValue();
        String heading = (String) item.getItemProperty(HEADING_PROPERTY).getValue();
        Integer numTagIndex = (Integer) item.getItemProperty(NUM_TAG_INDEX_PROPERTY).getValue();
        Integer headingTagIndex = (Integer) item.getItemProperty(HEADING_TAG_INDEX_PROPERTY).getValue();
        Integer index = (Integer) item.getItemProperty(INDEX_PROPERTY).getValue();

        TableOfContentItemVO tocItem = new TableOfContentItemVO(type, id,
                number, heading, numTagIndex, headingTagIndex, index);
        return tocItem;
    }

    private static HierarchicalContainer initContainer() {
        // create new container
        HierarchicalContainer container = new HierarchicalContainer();

        // create container properties
        container.addContainerProperty(TYPE_PROPERTY, TableOfContentItemVO.Type.class, null);
        container.addContainerProperty(XML_ID_PROPERTY, String.class, null);
        container.addContainerProperty(NUMBER_PROPERTY, String.class, null);
        container.addContainerProperty(HEADING_PROPERTY, String.class, null);
        container.addContainerProperty(NUM_TAG_INDEX_PROPERTY, Integer.class, null);
        container.addContainerProperty(HEADING_TAG_INDEX_PROPERTY, Integer.class, null);
        container.addContainerProperty(INDEX_PROPERTY, Integer.class, null);
        container.addContainerProperty(DESC_PROPERTY, String.class, null);
        container.addContainerProperty(CAPTION_PROPERTY, String.class, null);

        return container;
    }

    private static void populate(HierarchicalContainer container, Object parentId, List<TableOfContentItemVO> tocItems, int captionMaxSize,
                                 MessageHelper messageHelper) {
        if (tocItems != null) {
            for (TableOfContentItemVO tocItem : tocItems) {

                String itemId = getItemId(container, tocItem.getType());
                Item item = container.addItem(itemId);

                populateItemDetails(tocItem, item, captionMaxSize, messageHelper);

                if (parentId != null) {
                    container.setParent(itemId, parentId);
                }

                container.setChildrenAllowed(itemId, tocItem.areChildrenAllowed());

                // recursively populate container with child items
                populate(container, itemId, tocItem.getChildItemsView(), captionMaxSize, messageHelper);
            }
        }
    }

    public static String getItemId(HierarchicalContainer container, TableOfContentItemVO.Type tocItemType) {
        int itemCount = 0;
        String itemId;
        do {
            itemId = itemCount++ + "_" + tocItemType.name();
        } while (container.containsId(itemId));

        return itemId;
    }

    @SuppressWarnings("unchecked")
    public static void populateItemDetails(TableOfContentItemVO tocItem, Item item, int captionMaxSize, MessageHelper messageHelper) {
        item.getItemProperty(XML_ID_PROPERTY).setValue(tocItem.getId());
        item.getItemProperty(TYPE_PROPERTY).setValue(tocItem.getType());
        item.getItemProperty(NUMBER_PROPERTY).setValue(tocItem.getNumber());
        item.getItemProperty(HEADING_PROPERTY).setValue(tocItem.getHeading());
        item.getItemProperty(NUM_TAG_INDEX_PROPERTY).setValue(tocItem.getNumTagIndex());
        item.getItemProperty(HEADING_TAG_INDEX_PROPERTY).setValue(tocItem.getHeadingTagIndex());
        item.getItemProperty(INDEX_PROPERTY).setValue(tocItem.getVtdIndex());

        String itemNumber = StringUtils.trimToNull(tocItem.getNumber());
        String itemHeading = StringUtils.trimToNull(tocItem.getHeading());
        Validate.notNull(tocItem.getType(), "Type should not be null");
        String itemDescription = buildItemDescription(itemNumber, itemHeading, tocItem.getType(), messageHelper);

        item.getItemProperty(DESC_PROPERTY).setValue(itemDescription);
        String itemCaption = StringUtils.abbreviate(itemDescription, captionMaxSize);
        item.getItemProperty(CAPTION_PROPERTY).setValue(itemCaption);
    }

    public static String buildItemDescription(String itemNumber, String itemHeading, TableOfContentItemVO.Type itemType, MessageHelper messageHelper) {
        String itemDescription;

        if (!StringUtils.isEmpty(itemNumber) && !StringUtils.isEmpty(itemHeading)) {
            itemDescription = itemNumber + " - " + itemHeading;
        } else if (!StringUtils.isEmpty(itemNumber)) {
            itemDescription = itemNumber;
        } else if (!StringUtils.isEmpty(itemHeading)) {
            itemDescription = itemHeading;
        } else {
            itemDescription = getDisplayableItemType(itemType, messageHelper);
        }
        return itemDescription;
    }

    public static String getDisplayableItemType(TableOfContentItemVO.Type itemType, MessageHelper messageHelper) {
        return messageHelper.getMessage("toc.item.type." + itemType.name().toLowerCase());
    }
}
