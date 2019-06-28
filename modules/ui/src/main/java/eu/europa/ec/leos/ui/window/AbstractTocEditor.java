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

import com.vaadin.data.TreeData;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.ui.TreeGrid;
import eu.europa.ec.leos.services.support.xml.VTDUtils;
import eu.europa.ec.leos.ui.component.toc.TocDropResult;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;

import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.TableOfContentHelper.LIST;

public abstract class AbstractTocEditor implements TocEditor {

    @Override
    public void setTocTreeDataFilter(TreeDataProvider<TableOfContentItemVO> dataProvider) {
    }

    @Override
    public void setTocTreeStyling(MultiSelectTreeGrid<TableOfContentItemVO> tocTree, TreeDataProvider<TableOfContentItemVO> dataProvider) {
    }

    @Override
    public boolean isDeletableItem(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO) {
        return true;
    }

    @Override
    public boolean isDeletedItem(TableOfContentItemVO tableOfContentItemVO) {
        return false;
    }

    @Override
    public boolean isUndeletableItem(TableOfContentItemVO tableOfContentItemVO) {
        return false;
    }

    @Override
    public boolean checkIfConfirmDeletion(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO) {
        return !treeData.getChildren(tableOfContentItemVO).isEmpty();
    }

    @Override
    public boolean isArabicNumberingOnly(String itemType) {
        return false;
    }

    @Override
    public boolean isRomanNumberingOnly(String itemType) {
        return false;
    }

    protected TocDropResult validateAction(final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final List<TableOfContentItemVO> droppedItems, final TableOfContentItemVO targetItem, final ItemPosition position) {

        TocDropResult result = new TocDropResult(true, "toc.edit.window.drop.success.message", droppedItems.get(0), targetItem);
        TableOfContentItemVO parentItem = tocTree.getTreeData().getParent(targetItem);
        for (TableOfContentItemVO sourceItem : droppedItems) {
            if (isItemDroppedOnSameTarget(result, sourceItem, targetItem) || ((!targetItem.areChildrenAllowed() || !validateAddingItemAsChildOrSibling(result, sourceItem, targetItem, tocTree, tableOfContentRules, parentItem, position))
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
            final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final TableOfContentItemVO parentItem, final ItemPosition position) {

        TocItemType targetItemType = targetItem.getType();
        List<TocItemType> targetItemTypes = tableOfContentRules.get(targetItemType);
        if (sourceItem.getType().getName().equals(targetItemType.getName())) {
            TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
            return validateAddingToActualTargetItem(result, sourceItem, targetItem, tocTree, tableOfContentRules, actualTargetItem, position);
        } else if (targetItemTypes.size() > 0 && targetItemTypes.contains(sourceItem.getType())) {
            //If target item type is root, source item will be added as child, else validate dropping item at dragged location
            TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, false);
            return targetItemType.isRoot() || validateAddingToActualTargetItem(result, sourceItem, targetItem, tocTree, tableOfContentRules, actualTargetItem, position);
        } else { // If child elements not allowed in target validate adding it to its parent
            return validateAddingItemAsSibling(result, sourceItem, targetItem, tocTree, tableOfContentRules, parentItem, position);
        }
    }

    private boolean validateAddingItemAsSibling(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final TableOfContentItemVO parentItem, final ItemPosition position) {

        TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
        return validateAddingToActualTargetItem(result, sourceItem, targetItem, tocTree, tableOfContentRules, actualTargetItem, position);
    }

    private boolean validateParentAndSourceTypeCompatibility(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO parentItem,
            final TocItemType parentItemType, final List<TocItemType> parentItemTypes) {

        if (parentItemTypes == null || parentItemTypes.size() == 0 || !parentItemTypes.contains(sourceItem.getType())
                || !sourceItem.getType().isSameParentAsChild() && parentItemType.getName().equals(sourceItem.getType().getName())){
            result.setSuccess(false);
            result.setMessageKey("toc.edit.window.drop.error.message");
            result.setSourceItem(sourceItem);
            result.setTargetItem(parentItem);
            return false;
        }
        return true;
    }

    protected boolean validateAddingToActualTargetItem(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final TableOfContentItemVO actualTargetItem, final ItemPosition position) {

        TocItemType parentItemType = actualTargetItem != null ? actualTargetItem.getType() : null;
        List<TocItemType> parentItemTypes = tableOfContentRules.get(parentItemType);
        return validateParentAndSourceTypeCompatibility(result, sourceItem, actualTargetItem, parentItemType, parentItemTypes)
                && validateAddingToItem(result, sourceItem, targetItem, tocTree, actualTargetItem, position);
    }

    protected abstract boolean validateAddingToItem(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO actualTargetItem, final ItemPosition position);

    protected void performAddOrMoveAction(final boolean isAdd, final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem, final TableOfContentItemVO parentItem, final ItemPosition position) {

        if (targetItem.areChildrenAllowed()) {
            TocItemType targetItemType = targetItem.getType();
            List<TocItemType> targetItemTypes = tableOfContentRules.get(targetItemType);
            if (sourceItem.getType().getName().equals(targetItemType.getName())
                    || !(targetItemTypes.size() > 0 && targetItemTypes.contains(sourceItem.getType()))) {
                // If items have the same type or if child elements are not allowed in target add it to its parent
                TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
                addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, actualTargetItem, position);
            } else if (!targetItemType.isRoot()){
                // item is dropped at dragged location
                TableOfContentItemVO actualTargetItem = getActualTargetItem(sourceItem, targetItem, parentItem, position, false);
                addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, actualTargetItem, position);
            } else {
                //If target item type is root, source item will be added as child
                addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, targetItem, ItemPosition.AS_CHILDREN);
            }
        } else {
            TableOfContentItemVO actualTargetItem =  getActualTargetItem(sourceItem, targetItem, parentItem, position, true);
            addOrMoveItem(isAdd, sourceItem, targetItem, tocTree, actualTargetItem, position);
        }
    }

    protected void addOrMoveItem(final boolean isAdd, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO actualTargetItem, final ItemPosition position) {

        if (isAdd) {
            tocTree.getTreeData().addItem(null, sourceItem);
            if (actualTargetItem == null) {
                sourceItem.setParentItem(null);
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
            final ItemPosition position, boolean isItemTypeSibling) {

        switch (position) {
            case AS_CHILDREN:
                if ((isItemTypeSibling && !targetItem.getType().isSameParentAsChild()
                        || (targetItem.getType().isSameParentAsChild() && targetItem.containsType(LIST)))
                        || (targetItem.getId().equals(VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + sourceItem.getId()))) {
                    return parentItem;
                } else if (!sourceItem.equals(targetItem)) {
                    return targetItem;
                }
                break;
            case BEFORE:
                return parentItem;
            case AFTER:
                return isItemTypeSibling ? parentItem : targetItem;
        }
        return null;
    }
}
