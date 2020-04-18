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
package eu.europa.ec.leos.ui.window.toc;

import com.vaadin.ui.TreeGrid;
import eu.europa.ec.leos.services.support.xml.VTDUtils;
import eu.europa.ec.leos.ui.component.toc.TocDropResult;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;

import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.CLAUSE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.INDENT;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.LIST;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.POINT;
import static eu.europa.ec.leos.services.support.xml.XmlTableOfContentHelper.getTagValueFromTocItemVo;

public abstract class AbstractTocEditor implements TocEditor {

    protected TocDropResult validateAction(final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItem, List<TocItem>> tableOfContentRules,
            final List<TableOfContentItemVO> droppedItems, final TableOfContentItemVO targetItem, final ItemPosition position) {

        TocDropResult result = new TocDropResult(true, "toc.edit.window.drop.success.message", droppedItems.get(0), targetItem);
        TableOfContentItemVO parentItem = tocTree.getTreeData().getParent(targetItem);
        for (TableOfContentItemVO sourceItem : droppedItems) {
            if (isItemDroppedOnSameTarget(result, sourceItem, targetItem) || ((!targetItem.getTocItem().isChildrenAllowed() || !validateAddingItemAsChildOrSibling(result, sourceItem, targetItem, tocTree, tableOfContentRules, parentItem, position))
                    && !validateAddingItemAsSibling(result, sourceItem, targetItem, tocTree, tableOfContentRules, parentItem, position))) {
                return result;
            }
        }
        return result;
    }

    private boolean isItemDroppedOnSameTarget(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem) {
        if (sourceItem.equals(targetItem)) {
            result.setSuccess(false);
            result.setMessageKey("toc.edit.window.drop.error.same.item.message");
            result.setSourceItem(sourceItem);
            result.setTargetItem(targetItem);
            return true;
        }
        return false;
    }

    private boolean validateAddingItemAsChildOrSibling(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItem, List<TocItem>> tableOfContentRules,
            final TableOfContentItemVO parentItem, final ItemPosition position) {

        TocItem targetTocItem = targetItem.getTocItem();
        List<TocItem> targetTocItems = tableOfContentRules.get(targetTocItem);
        if (isDroppedOnPointOrIndent(sourceItem, targetItem) || getTagValueFromTocItemVo(sourceItem).equals(getTagValueFromTocItemVo(targetItem))) {
            TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
            return validateAddingToActualTargetItem(result, sourceItem, targetItem, tocTree, tableOfContentRules, actualTargetItem, position);
        } else if (targetTocItems != null && targetTocItems.size() > 0 && targetTocItems.contains(sourceItem.getTocItem())) {
            //If target item type is root, source item will be added as child, else validate dropping item at dragged location
            TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, false);
            return targetTocItem.isRoot() || validateAddingToActualTargetItem(result, sourceItem, targetItem, tocTree, tableOfContentRules, actualTargetItem, position);
        } else { // If child elements not allowed in target validate adding it to its parent
            return validateAddingItemAsSibling(result, sourceItem, targetItem, tocTree, tableOfContentRules, parentItem, position);
        }
    }

    private boolean validateAddingItemAsSibling(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItem, List<TocItem>> tableOfContentRules,
            final TableOfContentItemVO parentItem, final ItemPosition position) {

        TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
        return validateAddingToActualTargetItem(result, sourceItem, targetItem, tocTree, tableOfContentRules, actualTargetItem, position);
    }

    private boolean validateParentAndSourceTypeCompatibility(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO parentItem,
            final TocItem parentTocItem, final List<TocItem> parentTocItems) {

        if (parentTocItems == null || parentTocItems.size() == 0 || !parentTocItems.contains(sourceItem.getTocItem())
                || !sourceItem.getTocItem().isSameParentAsChild() && parentTocItem.getAknTag().value().equals(sourceItem.getTocItem().getAknTag().value())){
            result.setSuccess(false);
            result.setMessageKey("toc.edit.window.drop.error.message");
            result.setSourceItem(sourceItem);
            result.setTargetItem(parentItem);
            return false;
        }
        return true;
    }

    protected boolean validateAddingToActualTargetItem(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItem, List<TocItem>> tableOfContentRules,
            final TableOfContentItemVO actualTargetItem, final ItemPosition position) {

        TocItem parentTocItem = actualTargetItem != null ? actualTargetItem.getTocItem() : null;
        List<TocItem> parentTocItems = tableOfContentRules.get(parentTocItem);
        return validateParentAndSourceTypeCompatibility(result, sourceItem, actualTargetItem, parentTocItem, parentTocItems)
                && validateAddingToItem(result, sourceItem, targetItem, tocTree, actualTargetItem, position);
    }

    protected abstract boolean validateAddingToItem(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO actualTargetItem, final ItemPosition position);

    protected void performAddOrMoveAction(final boolean isAdd, final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItem, List<TocItem>> tableOfContentRules,
            final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem, final TableOfContentItemVO parentItem, final ItemPosition position) {

        if (targetItem.getTocItem().isChildrenAllowed()) {
            TocItem targetTocItem = targetItem.getTocItem();
            List<TocItem> targetTocItems = tableOfContentRules.get(targetTocItem);
            if (isDroppedOnPointOrIndent(sourceItem, targetItem) || getTagValueFromTocItemVo(sourceItem).equals(getTagValueFromTocItemVo(targetItem))
                    || !(targetTocItems != null && targetTocItems.size() > 0 && targetTocItems.contains(sourceItem.getTocItem()))) {
                // If items have the same type or if child elements are not allowed in target add it to its parent
                TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
                addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, actualTargetItem, position);
            } else if (!targetTocItem.isRoot()){
                // item is dropped at dragged location
                TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, false);
                addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, actualTargetItem, position);
            } else {
                //If target item type is root, source item will be added as child before clause item if exists
                if (targetItem.containsItem(CLAUSE)) {
                    TableOfContentItemVO clauseItem = targetItem.getChildItems().stream()
                            .filter(x -> x.getTocItem().getAknTag().value().equals(CLAUSE)).findFirst().orElse(null);
                    TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, clauseItem, clauseItem.getParentItem(), ItemPosition.BEFORE, true);
                    addOrMoveItem(isAdd, sourceItem, clauseItem, tocTree, actualTargetItem, ItemPosition.BEFORE);
                } else {
                    addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, targetItem, ItemPosition.AS_CHILDREN);
                }
            }
        } else {
            TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
            addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, actualTargetItem, position);
        }
    }

    protected void addOrMoveItem(final boolean isAdd, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO actualTargetItem, final ItemPosition position) {

        if (isAdd) {
            tocTree.getTreeData().addItem(null, sourceItem);
            if (actualTargetItem == null) {
                sourceItem.setParentItem(null);
                sourceItem.setItemDepth(1);
            }
        } else if (sourceItem.getParentItem() != null) {
            sourceItem.getParentItem().removeChildItem(sourceItem);
        }

        if (actualTargetItem != null) {
            tocTree.getTreeData().setParent(sourceItem, actualTargetItem);
            sourceItem.setParentItem(actualTargetItem);
            if (!actualTargetItem.equals(targetItem)) {
                int indexSiblings = actualTargetItem.getChildItems().indexOf(targetItem);
                tocTree.getTreeData().moveAfterSibling(sourceItem, targetItem);
                if (ItemPosition.BEFORE == position) {
                    tocTree.getTreeData().moveAfterSibling(targetItem, sourceItem);
                    actualTargetItem.getChildItems().add(indexSiblings, sourceItem);
                } else {
                    actualTargetItem.getChildItems().add(indexSiblings + 1, sourceItem);
                }
            } else {
                actualTargetItem.getChildItems().add(sourceItem);
            }
        }
    }
    
    private TableOfContentItemVO getActualTargetItem(final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem, final TableOfContentItemVO parentItem,
            final ItemPosition position, boolean isTocItemSibling) {

        switch (position) {
            case AS_CHILDREN:
                sourceItem.setItemDepth(targetItem.getItemDepth() + 1);
                if ((isTocItemSibling && !targetItem.getTocItem().isSameParentAsChild()
                        || (targetItem.getTocItem().isSameParentAsChild() && targetItem.containsItem(LIST)))
                        || (targetItem.getId().equals(VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + sourceItem.getId()))) {
                    return parentItem;
                } else if (!sourceItem.equals(targetItem)) {
                    return targetItem;
                }
                break;
            case BEFORE:
                sourceItem.setItemDepth(targetItem.getItemDepth() == 0 ? 1 : targetItem.getItemDepth());
                return parentItem;
            case AFTER:
                if (targetItem.getTocItem().isRoot()) {
                    sourceItem.setItemDepth(1);
                } else {
                    sourceItem.setItemDepth(targetItem.getItemDepth() == 0 ? 1 : targetItem.getItemDepth());
                }
                return isTocItemSibling ? parentItem : targetItem;
        }
        return null;
    }

    private boolean isDroppedOnPointOrIndent(TableOfContentItemVO sourceItem, TableOfContentItemVO targetItem) {
        String sourceTagValue = getTagValueFromTocItemVo(sourceItem);
        String targetTagValue = getTagValueFromTocItemVo(targetItem);
        return (sourceTagValue.equals(POINT) || sourceTagValue.equals(INDENT)) && (targetTagValue.equals(POINT) || targetTagValue.equals(INDENT));
    }

}
