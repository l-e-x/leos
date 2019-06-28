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

import com.google.common.collect.Lists;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.HierarchicalQuery;
import com.vaadin.data.provider.TreeDataProvider;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.TreeGrid;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.services.support.xml.VTDUtils;
import eu.europa.ec.leos.ui.component.toc.TocDropResult;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.model.action.SoftActionType.ADD;
import static eu.europa.ec.leos.model.action.SoftActionType.DELETE;
import static eu.europa.ec.leos.model.action.SoftActionType.MOVE_FROM;
import static eu.europa.ec.leos.model.action.SoftActionType.MOVE_TO;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.ARTICLE;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.CN;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.EC;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.ELEMENTS_WITHOUT_CONTENT;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.LIST;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.PARAGRAPH;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.POINT;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.SUBPARAGRAPH;
import static eu.europa.ec.leos.services.support.TableOfContentHelper.SUBPOINT;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.SOFT_DELETE_PLACEHOLDER_ID_PREFIX;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.ELEMENTS_TO_BE_PROCESSED_FOR_NUMBERING;

@SpringComponent
@Instance(InstanceType.COUNCIL)
public class MandateTocEditor extends AbstractTocEditor {

    private static final String TEMP_PREFIX = "temp_";
    private static final int MAX_INDENT_LEVEL = 4;

    @Override
    public void setTocTreeDataFilter(TreeDataProvider<TableOfContentItemVO> dataProvider) {
        dataProvider.setFilter(tableOfContentItemVO -> {
            return !((tableOfContentItemVO.getType().getName().equalsIgnoreCase(SUBPARAGRAPH) || tableOfContentItemVO.getType().getName().equalsIgnoreCase(SUBPOINT))
                    && (tableOfContentItemVO.getParentItem() != null)
                    && tableOfContentItemVO.getParentItem().getChildItemsView().get(0).getId().equals(tableOfContentItemVO.getId()));
        });
    }

    @Override
    public void setTocTreeStyling(MultiSelectTreeGrid<TableOfContentItemVO> tocTree, TreeDataProvider<TableOfContentItemVO> dataProvider) {
        tocTree.setStyleGenerator(tableOfContentItemVO -> {
            String itemSoftStyle = TableOfContentHelper.getItemSoftStyle(tableOfContentItemVO);
            if (tocTree.getTreeData().contains(tableOfContentItemVO) && !tableOfContentItemVO.getChildItemsView().isEmpty()) {
                int numChildren = dataProvider.getChildCount(new HierarchicalQuery<>(dataProvider.getFilter(), tableOfContentItemVO));
                return numChildren > 0 ? itemSoftStyle : (itemSoftStyle + " leos-toc-no-expander-icon").trim();
            }
            return itemSoftStyle;
        });
    }

    @Override
    public boolean isDeletableItem(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO) {
        return !(DELETE.equals(tableOfContentItemVO.getSoftActionAttr()) ||
                MOVE_TO.equals(tableOfContentItemVO.getSoftActionAttr()) ||
                (containsItemOfOrigin(tableOfContentItemVO, EC) &&
                        containsItemOfOrigin(tableOfContentItemVO, CN)));
    }

    @Override
    public boolean isDeletedItem(TableOfContentItemVO tableOfContentItemVO) {
        return DELETE.equals(tableOfContentItemVO.getSoftActionAttr());   
    }

    @Override
    public boolean isUndeletableItem(TableOfContentItemVO tableOfContentItemVO) {
        return ((tableOfContentItemVO.getParentItem() != null) && !(DELETE.equals(tableOfContentItemVO.getParentItem().getSoftActionAttr())));   
    }

    @Override
    public boolean checkIfConfirmDeletion(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO) {
        return super.checkIfConfirmDeletion(treeData, tableOfContentItemVO) && containsNoSoftDeletedItem(treeData, tableOfContentItemVO);
    }
    
    @Override
    public void undeleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        TreeData<TableOfContentItemVO> treeData = tocTree.getTreeData();
        TableOfContentItemVO tempDeletedItem = copyDeletedItemToTempForUndelete(tableOfContentItemVO);
        dropItemAtOriginalPosition(tableOfContentItemVO, tempDeletedItem, treeData);
        treeData.removeItem(tableOfContentItemVO);
        tableOfContentItemVO.getParentItem().removeChildItem(tableOfContentItemVO);
        
        TableOfContentItemVO softDeletedItem = copyTempItemToFinalItem(tempDeletedItem);
        dropItemAtOriginalPosition(tempDeletedItem, softDeletedItem, treeData);
        treeData.removeItem(tempDeletedItem);
        tempDeletedItem.getParentItem().removeChildItem(tempDeletedItem);
        
        tocTree.getDataProvider().refreshAll();
        tocTree.deselectAll();
    }
    
    @Override
    public void deleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        if (!containsItemOfOrigin(tableOfContentItemVO, EC)) {
            tocTree.getTreeData().removeItem(tableOfContentItemVO);
            if (tableOfContentItemVO.getParentItem() != null) {
                tableOfContentItemVO.getParentItem().removeChildItem(tableOfContentItemVO);
            }
        } else {
            softDeleteItem(tocTree, tableOfContentItemVO);
        }
        tocTree.getDataProvider().refreshAll();
        tocTree.deselectAll();
    }

    private boolean containsItemOfOrigin(TableOfContentItemVO tableOfContentItemVO, String origin) {
        if ((!StringUtils.isEmpty(tableOfContentItemVO.getOriginAttr()) && tableOfContentItemVO.getOriginAttr().equals(origin)) ||
                (StringUtils.isEmpty(tableOfContentItemVO.getOriginAttr()) && origin.equals(CN))) {
            return true;
        }
        boolean containsItem = false;
        for (TableOfContentItemVO item : tableOfContentItemVO.getChildItems()) {
            containsItem = containsItemOfOrigin(item, origin);
            if (containsItem) break;
        }
        return containsItem;
    }

    @Override
    public TocDropResult addOrMoveItems(final boolean isAdd, final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final List<TableOfContentItemVO> droppedItems, final TableOfContentItemVO targetItem, final ItemPosition position) {

        TocDropResult result = validateAction(tocTree, tableOfContentRules, droppedItems, targetItem, position);
        if (result.isSuccess()) {
            TableOfContentItemVO parentItem = tocTree.getTreeData().getParent(targetItem);
            List<TableOfContentItemVO> sourceItems =  ItemPosition.BEFORE == position ? droppedItems : Lists.reverse(droppedItems);
            TableOfContentItemVO originalForTarget = sourceItems.stream().filter(sourceItem -> isPlaceholderForDroppedItem(targetItem, sourceItem)).findFirst().orElse(null);
            if (originalForTarget !=  null && !isAdd) {
                //first move the original over placeholder and then the rest over the original
                performAddOrMoveAction(false, tocTree, tableOfContentRules, originalForTarget, targetItem, parentItem, position);
                parentItem = tocTree.getTreeData().getParent(originalForTarget);
                for (TableOfContentItemVO sourceItem : sourceItems) {
                    //all objects come from the same list of dropped items, so no need to use equals
                    if (sourceItem != originalForTarget) {
                        performAddOrMoveAction(false, tocTree, tableOfContentRules, sourceItem, originalForTarget, parentItem, position);
                    }
                }
            } else {
                for (TableOfContentItemVO sourceItem : sourceItems) {
                    performAddOrMoveAction(isAdd, tocTree, tableOfContentRules, sourceItem, targetItem, parentItem, position);
                }
            }
            tocTree.deselectAll();
            tocTree.getDataProvider().refreshAll();
        }
        return result;
    }

    @Override
    protected void addOrMoveItem(final boolean isAdd, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO actualTargetItem, final ItemPosition position) {
        if (isAdd) {
            super.addOrMoveItem(true, sourceItem, targetItem, tocTree, actualTargetItem, position);
            moveOriginAttribute(sourceItem, targetItem);
            setNumber(sourceItem, targetItem);
            sourceItem.setSoftActionAttr(ADD);
            sourceItem.setSoftActionRoot(Boolean.TRUE);
        } else {
            updateMovedOnEmptyParent(sourceItem, actualTargetItem, PARAGRAPH, SUBPARAGRAPH);
            handleMoveAction(sourceItem, tocTree);
            super.addOrMoveItem(false, sourceItem, targetItem, tocTree, actualTargetItem, position);
            restoreMovedItemOrSetNumber(tocTree, sourceItem, targetItem, position);
        }
        setAffectedAttribute(sourceItem, tocTree.getTreeData());
    }

    protected TocDropResult validateAction(final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final List<TableOfContentItemVO> droppedItems, final TableOfContentItemVO targetItem, final ItemPosition position) {

        TocDropResult result = validateAgainstSoftDeletedOrMoveToItems(droppedItems, targetItem, tocTree.getTreeData().getParent(targetItem));
        if (result.isSuccess()){
            return super.validateAction(tocTree, tableOfContentRules, droppedItems, targetItem, position);
        }
        return result;
    }

    private TocDropResult validateAgainstSoftDeletedOrMoveToItems(List<TableOfContentItemVO> droppedItems, TableOfContentItemVO targetItem, TableOfContentItemVO parentItem) {

        // Check if there are no soft deleted items at first level(not in children) in dropped items
        TocDropResult tocDropResult = new TocDropResult(true, "toc.edit.window.drop.success.message", droppedItems.get(0), targetItem);
        boolean originalFound = false;
        for (TableOfContentItemVO sourceItem : droppedItems) {
            if (isSoftDeletedOrMoveToItem(sourceItem)) {
                tocDropResult.setSuccess(false);
                tocDropResult.setMessageKey("toc.edit.window.drop.error.softdeleted.source.message");
                tocDropResult.setSourceItem(sourceItem);
                return tocDropResult;
            } else if (isPlaceholderForDroppedItem(targetItem, sourceItem)) {
                //if the target is the placeholder for one of the source items skip target validation
                originalFound = true;
            }
        }
        if(!originalFound) {
            tocDropResult.setSuccess(!isSoftDeletedOrMoveToItem( targetItem.areChildrenAllowed() ? targetItem : parentItem));
            tocDropResult.setMessageKey("toc.edit.window.drop.error.softdeleted.target.message");
        }
        return tocDropResult;
    }

    private boolean isSoftDeletedOrMoveToItem(TableOfContentItemVO item) {
        return item != null && item.getSoftActionAttr() != null
                && (item.getSoftActionAttr().equals(DELETE) || item.getSoftActionAttr().equals(MOVE_TO));
    }

    @Override
    protected boolean validateAddingToItem(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO actualTargetItem, final ItemPosition position) {

        String droppedElementTagName = sourceItem.getType().getName();
        String targetName = actualTargetItem.getType().getName();
        result.setSourceItem(sourceItem);
        result.setTargetItem(actualTargetItem);
        boolean indentAllowed = false;

        switch (droppedElementTagName) {
            case SUBPARAGRAPH:
                if (!isNumbered(sourceItem, actualTargetItem) && !actualTargetItem.containsType(SUBPARAGRAPH)) {
                    result.setSuccess(false);
                    result.setMessageKey("toc.edit.window.drop.error.subparagraph.message");
                    return false;
                }
                break;
            case POINT:
                indentAllowed = isIndentAllowed(tocTree.getTreeData(), actualTargetItem, MAX_INDENT_LEVEL - getIndentLevel(sourceItem));
                if (!indentAllowed || (!targetName.equals(PARAGRAPH) && !targetName.equals(LIST) && !targetName.equals(POINT)) ||
                        (targetName.equals(PARAGRAPH) && actualTargetItem.containsType(POINT)) ||
                        (targetName.equals(PARAGRAPH) && actualTargetItem.containsType(LIST)) ||
                        (targetName.equals(POINT) && actualTargetItem.containsType(LIST))) {
                    result.setSuccess(false);
                    result.setMessageKey(!indentAllowed ? "toc.edit.window.drop.error.indentation.message" : "toc.edit.window.drop.error.message");
                    return false;
                }
                break;
            case LIST:
                indentAllowed = isIndentAllowed(tocTree.getTreeData(), actualTargetItem, MAX_INDENT_LEVEL - getIndentLevel(sourceItem));
                if (!indentAllowed || (targetName.equals(PARAGRAPH) && actualTargetItem.containsType(LIST)) ||
                        (targetName.equals(POINT) && actualTargetItem.containsType(LIST))) {
                    result.setSuccess(false);
                    result.setMessageKey(!indentAllowed ? "toc.edit.window.drop.error.indentation.message" : "toc.edit.window.drop.error.list.message");
                    return false;
                }
                break;
        }
        return true;
    }

    private boolean isIndentAllowed(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO targetElement, int indentLevel) {
        boolean isAllowed = true;
        if (indentLevel < 0) {
            isAllowed = false;
        } else if (targetElement != null) {
            isAllowed = isIndentAllowed(treeData, treeData.getParent(targetElement),
                    targetElement.getType().getName().equals(POINT) ? indentLevel - 1 : indentLevel);
        }
        return isAllowed;
    }

    private int getIndentLevel(TableOfContentItemVO element) {
        int identLevel = 0;
        for (TableOfContentItemVO child : element.getChildItems()) {
            identLevel = Math.max(identLevel, getIndentLevel(child));
        }
        return element.getType().getName().equals(POINT) ? identLevel + 1 : identLevel;
    }

    private void moveOriginAttribute(final TableOfContentItemVO droppedElement, final TableOfContentItemVO targetElement) {
        if (isElementAndTargetOriginDifferent(droppedElement, targetElement)) {
            droppedElement.setOriginAttr(CN);
        }
        droppedElement.setOriginNumAttr(CN);
    }

    private boolean isElementAndTargetOriginDifferent(TableOfContentItemVO element, TableOfContentItemVO parent) {
        boolean isDifferent = false;
        if (element.getOriginAttr() == null) {
            isDifferent = true;
        } else if (!element.getOriginAttr().equals(parent.getOriginAttr())) {
            isDifferent = true;
        }
        return isDifferent;
    }
    
    private void setNumber(final TableOfContentItemVO droppedElement, final TableOfContentItemVO targetElement) {
        if(isNumbered(droppedElement, targetElement)) {
            droppedElement.setNumber("#");
            if(SoftActionType.DELETE.equals(droppedElement.getNumSoftActionAttr())){
                droppedElement.setNumSoftActionAttr(null);
            }
        } else {
            droppedElement.setNumber(null);
        }
    }

    private boolean isNumSoftDeleted(final SoftActionType numSoftACtionAttr){
        if(numSoftACtionAttr != null){
            return DELETE.equals(numSoftACtionAttr);
        }else{
            return false;
        }
    }

    private boolean isNumbered(TableOfContentItemVO droppedElement, TableOfContentItemVO targetElement) {
        String targetElementTagName = targetElement.getType().getName();
        String droppedElementTagName = droppedElement.getType().getName();

        switch (targetElementTagName) {
            case ARTICLE:
                if (droppedElementTagName.equalsIgnoreCase(PARAGRAPH) &&
                        targetElement.getChildItems() != null && targetElement.getChildItems().size() > 0) {
                    TableOfContentItemVO childItem = targetElement.getChildItems().get(0);
                    boolean flag = false;

                    for(TableOfContentItemVO itemVO : targetElement.getChildItems()){
                        if(isNumSoftDeleted(itemVO.getNumSoftActionAttr())){
                            flag = true;
                            break;
                        }
                    }

                    if ((childItem.getType().getName().equalsIgnoreCase(PARAGRAPH) && childItem.getNumber() == null)
                            || flag) {
                        return false;
                    }
                }
                break;
            case PARAGRAPH:
                if ((targetElement.getNumber() == null && PARAGRAPH.equals(droppedElement.getType().getName()))
                        || (isNumSoftDeleted(targetElement.getNumSoftActionAttr()))) {
                    return false;
                }
                break;
            default:
                return true;
        }
        return true;
    }


    private void softDeleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        Boolean wasRoot = isRootElement(tableOfContentItemVO);
        Boolean wasMoved = MOVE_FROM.equals(tableOfContentItemVO.getSoftActionAttr());
        softDeleteMovedRootItems(tocTree, tableOfContentItemVO);
        if (wasMoved) {
            if (!wasRoot) {
                // all its moved children are now restored to their original position and deleted,
                // but the tableOfContentItemVO element still needs to be restored to its original position and deleted
                revertMoveAndTransformToSoftDeleted(tocTree, tableOfContentItemVO);
            }
        } else {
            // all its moved children are now restored to their original position and deleted,
            // and the tableOfContentItemVO only needs to be deleted
            transformToSoftDeleted(tocTree.getTreeData(), tableOfContentItemVO);
        }
    }

    private int softDeleteMovedRootItems(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        int index = 0;
        while (index < tableOfContentItemVO.getChildItems().size()) {
            index += softDeleteMovedRootItems(tocTree, tableOfContentItemVO.getChildItems().get(index));
        }
        if (isRootElement(tableOfContentItemVO) && MOVE_FROM.equals(tableOfContentItemVO.getSoftActionAttr())){
            revertMoveAndTransformToSoftDeleted(tocTree, tableOfContentItemVO);
            return 0;
        }
        return 1;
    }

    private void revertMoveAndTransformToSoftDeleted(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        TableOfContentItemVO originalPosition = getTableOfContentItemVOById(tableOfContentItemVO.getSoftMoveFrom(), tocTree.getTreeData().getRootItems());
        if (originalPosition != null) {
            TableOfContentItemVO movedItem = moveItem(tableOfContentItemVO, originalPosition, tocTree.getTreeData());
            restoreOriginal(movedItem, originalPosition, tocTree);
            if (originalPosition.getParentItem() != null) {
                originalPosition.getParentItem().removeChildItem(originalPosition);
            }
            transformToSoftDeleted(tocTree.getTreeData(), movedItem);
        } else {
            throw new IllegalStateException("Soft-moved element was later hard-deleted or its id was not set in its placeholder");
        }
    }

    private void transformToSoftDeleted(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO) {
        TableOfContentItemVO tempDeletedItem = copyDeletedItemToTemp(tableOfContentItemVO, Boolean.TRUE);
        dropItemAtOriginalPosition(tableOfContentItemVO, tempDeletedItem, treeData);
        treeData.removeItem(tableOfContentItemVO);
        if (tableOfContentItemVO.getParentItem() != null) {
            tableOfContentItemVO.getParentItem().removeChildItem(tableOfContentItemVO);
        }

        TableOfContentItemVO softDeletedItem = copyTempItemToFinalItem(tempDeletedItem);
        dropItemAtOriginalPosition(tempDeletedItem, softDeletedItem, treeData);
        treeData.removeItem(tempDeletedItem);
        if (tempDeletedItem.getParentItem() != null) {
            tempDeletedItem.getParentItem().removeChildItem(tempDeletedItem);
        }
    }

    private boolean containsNoSoftDeletedItem(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO) {
        boolean containsItem = false;
        for (TableOfContentItemVO item : treeData.getChildren(tableOfContentItemVO)) {
            if (item.getSoftActionAttr() == null ||
                    (item.getSoftActionAttr() != null && !item.getSoftActionAttr().equals(DELETE))) {
                containsItem = true;
            } else {
                containsItem = containsNoSoftDeletedItem(treeData, item);
            }
            if (containsItem) break;
        }
        return containsItem;
    }

    private void setAffectedAttribute(TableOfContentItemVO dropData, TreeData<TableOfContentItemVO> treeData) {
        if (PARAGRAPH.equals(dropData.getType().getName()) || POINT.equals(dropData.getType().getName())) {
            TableOfContentItemVO parentItemVO = treeData.getParent(dropData);
            while (parentItemVO != null) {
                if (ELEMENTS_TO_BE_PROCESSED_FOR_NUMBERING.contains(parentItemVO.getType().getName())) {
                    parentItemVO.setAffected(true);
                    if (ARTICLE.equals(parentItemVO.getType().getName())) {
                        break;
                    }
                }
                parentItemVO = treeData.getParent(parentItemVO);
            }
        }
    }

    private TableOfContentItemVO copyDeletedItemToTemp(TableOfContentItemVO originalItem, Boolean isSoftActionRoot){
        TableOfContentItemVO tempDeletedItem;

        if (!MOVE_TO.equals(originalItem.getSoftActionAttr()) && !DELETE.equals(originalItem.getSoftActionAttr())) {
            tempDeletedItem = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX + SOFT_DELETE_PLACEHOLDER_ID_PREFIX + originalItem.getId(),
                    originalItem.getOriginAttr(), originalItem.getNumber(),
                    EC, originalItem.getHeading(), originalItem.getNumTagIndex(), originalItem.getHeadingTagIndex(), originalItem.getVtdIndex(),
                    originalItem.getPreambleFormula1TagIndex(),
                    originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(),
                    originalItem.getContent(),
                    DELETE, isSoftActionRoot, null, null, originalItem.getSoftMoveFrom(), originalItem.getSoftMoveTo(), originalItem.isUndeleted(), originalItem.getNumSoftActionAttr());

        } else {
            tempDeletedItem = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX + originalItem.getId(),
                    originalItem.getOriginAttr(), originalItem.getNumber(),
                    originalItem.getOriginNumAttr(), originalItem.getHeading(), originalItem.getNumTagIndex(), originalItem.getHeadingTagIndex(), originalItem.getVtdIndex(),
                    originalItem.getPreambleFormula1TagIndex(),
                    originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(),
                    originalItem.getContent(),
                    originalItem.getSoftActionAttr(), originalItem.isSoftActionRoot(), originalItem.getSoftUserAttr(), originalItem.getSoftDateAttr(),
                    originalItem.getSoftMoveFrom(), originalItem.getSoftMoveTo(), originalItem.isUndeleted(), originalItem.getNumSoftActionAttr());
        }

        tempDeletedItem.setContent(originalItem.getContent());
        originalItem.getChildItems().forEach(child -> tempDeletedItem.addChildItem(copyDeletedItemToTemp(child, Boolean.FALSE)));
        return tempDeletedItem;
    }
    
    private TableOfContentItemVO copyDeletedItemToTempForUndelete(TableOfContentItemVO originalItem){
        TableOfContentItemVO tempDeletedItem;
        if (DELETE.equals(originalItem.getSoftActionAttr())) {
            tempDeletedItem = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX + originalItem.getId().replace(SOFT_DELETE_PLACEHOLDER_ID_PREFIX, ""),
                    originalItem.getOriginAttr(), originalItem.getNumber(),
                    EC, originalItem.getHeading(), originalItem.getNumTagIndex(), originalItem.getHeadingTagIndex(), originalItem.getVtdIndex(),
                    originalItem.getPreambleFormula1TagIndex(),
                    originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(),
                    originalItem.getContent(),
                    null, null, null, null,
                    null, null, true, originalItem.getNumSoftActionAttr());
        } else {
            tempDeletedItem = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX +originalItem.getId(),
                    originalItem.getOriginAttr(), originalItem.getNumber(),
                    originalItem.getOriginNumAttr(), originalItem.getHeading(), originalItem.getNumTagIndex(), originalItem.getHeadingTagIndex(), originalItem.getVtdIndex(),
                    originalItem.getPreambleFormula1TagIndex(),
                    originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(),
                    originalItem.getContent(),
                    originalItem.getSoftActionAttr(), originalItem.isSoftActionRoot(), originalItem.getSoftUserAttr(), originalItem.getSoftDateAttr(),
                    originalItem.getSoftMoveFrom(), originalItem.getSoftMoveTo(), originalItem.isUndeleted(), originalItem.getNumSoftActionAttr());
        }
        
        tempDeletedItem.setContent(originalItem.getContent());
        originalItem.getChildItems().forEach(child -> tempDeletedItem.addChildItem(copyDeletedItemToTempForUndelete(child)));
        return tempDeletedItem;
    }

    private TableOfContentItemVO copyTempItemToFinalItem(TableOfContentItemVO tempItem){
        TableOfContentItemVO finalItem = new TableOfContentItemVO(tempItem.getType(), tempItem.getId().replace(TEMP_PREFIX, ""),
            tempItem.getOriginAttr(), tempItem.getNumber(),
            tempItem.getOriginNumAttr(), tempItem.getHeading(), tempItem.getNumTagIndex(), tempItem.getHeadingTagIndex(), tempItem.getVtdIndex(),
            tempItem.getPreambleFormula1TagIndex(),
            tempItem.getPreambleFormula2TagIndex(), tempItem.getRecitalsIntroIndex(), tempItem.getList(), tempItem.getListTagIndex(),
            tempItem.getContent(),
            tempItem.getSoftActionAttr(), tempItem.isSoftActionRoot(), tempItem.getSoftUserAttr(), tempItem.getSoftDateAttr(),
            tempItem.getSoftMoveFrom(), tempItem.getSoftMoveTo(), tempItem.isUndeleted(), tempItem.getNumSoftActionAttr());

        finalItem.setContent(tempItem.getContent());
        tempItem.getChildItems().forEach(child -> finalItem.addChildItem(copyTempItemToFinalItem(child)));
        return finalItem;
    }

    private TableOfContentItemVO copyMovingItemToTemp(TableOfContentItemVO originalItem, Boolean isSoftActionRoot, TreeGrid<TableOfContentItemVO> tocTree) {
        TableOfContentItemVO moveToItem;

        if (!ELEMENTS_WITHOUT_CONTENT.contains(originalItem.getType().getName().toLowerCase())) {
            moveToItem = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX + VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + originalItem.getId(), originalItem.getOriginAttr(), originalItem.getNumber(),
                    EC, originalItem.getHeading(), originalItem.getNumTagIndex(), originalItem.getHeadingTagIndex(), originalItem.getVtdIndex(), originalItem.getPreambleFormula1TagIndex(),
                    originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(), originalItem.getContent(),
                    MOVE_TO, isSoftActionRoot, null, null);

            moveToItem.setNumSoftActionAttr(originalItem.getNumSoftActionAttr());
            moveToItem.setContent(originalItem.getContent());
            Iterator<TableOfContentItemVO> iterator = originalItem.getChildItems().iterator();
            while (iterator.hasNext()) {
                TableOfContentItemVO child = iterator.next();
                if (EC.equals(child.getOriginAttr()) &&
                        (!MOVE_FROM.equals(child.getSoftActionAttr()) && !MOVE_TO.equals(child.getSoftActionAttr())
                                && !ADD.equals(child.getSoftActionAttr()) && !DELETE.equals(child.getSoftActionAttr()))) {
                    moveToItem.addChildItem(copyMovingItemToTemp(child, Boolean.FALSE, tocTree));
                } else if (EC.equals(child.getOriginAttr()) && (MOVE_TO.equals(child.getSoftActionAttr()) || DELETE.equals(child.getSoftActionAttr()))) {
                    moveToItem.addChildItem(copyItemToTemp(child));
                    setAffectedAttribute(child, tocTree.getTreeData());
                    iterator.remove();
                    tocTree.getTreeData().removeItem(child);
                }
            }
        } else {
            moveToItem = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX + VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + originalItem.getId(), originalItem.getOriginAttr(), originalItem.getNumber(),
                    EC, null, originalItem.getNumTagIndex(), null, originalItem.getVtdIndex(), originalItem.getPreambleFormula1TagIndex(),
                    originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(), originalItem.getContent(),
                    MOVE_TO, isSoftActionRoot,null, null);

        }
        moveToItem.setSoftMoveTo(originalItem.getId());
        originalItem.setSoftActionAttr(MOVE_FROM);
        originalItem.setSoftActionRoot(isSoftActionRoot);
        originalItem.setSoftMoveFrom(VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + originalItem.getId());
        return moveToItem;
    }

    private TableOfContentItemVO copyItemToTemp(TableOfContentItemVO originalItem) {
        TableOfContentItemVO temp = new TableOfContentItemVO(originalItem.getType(), TEMP_PREFIX + originalItem.getId(),
                originalItem.getOriginAttr(), originalItem.getNumber(),
                originalItem.getOriginNumAttr(), originalItem.getHeading(), originalItem.getNumTagIndex(), originalItem.getHeadingTagIndex(), originalItem.getVtdIndex(),
                originalItem.getPreambleFormula1TagIndex(),
                originalItem.getPreambleFormula2TagIndex(), originalItem.getRecitalsIntroIndex(), originalItem.getList(), originalItem.getListTagIndex(),
                originalItem.getContent(),
                originalItem.getSoftActionAttr(), originalItem.isSoftActionRoot(), originalItem.getSoftUserAttr(), originalItem.getSoftDateAttr(),
                originalItem.getSoftMoveFrom(), originalItem.getSoftMoveTo(), originalItem.isUndeleted(), originalItem.getNumSoftActionAttr());
        temp.setContent(originalItem.getContent());
        originalItem.getChildItems().forEach(child -> temp.addChildItem(copyItemToTemp(child)));
        return temp;
    }

    private void dropItemAtOriginalPosition(TableOfContentItemVO originalPosition, TableOfContentItemVO item, TreeData<TableOfContentItemVO> container) {
        TableOfContentItemVO parentItem = originalPosition.getParentItem();
        if (parentItem != null && item != null) {
            int indexSiblings = parentItem.getChildItems().indexOf(originalPosition);
            if (indexSiblings >= 0) {
                container.addItem(parentItem, item);
                item.setParentItem(parentItem);
                parentItem.getChildItems().add(indexSiblings, item);
                container.moveAfterSibling(item, originalPosition);
                container.moveAfterSibling(originalPosition, item);
            } else {
                throw new IllegalStateException("Original element not found in its parent list of children");
            }
            dropChildrenAtOriginalPosition(item, container);
        }
    }

    private void dropChildrenAtOriginalPosition(TableOfContentItemVO parentItem, TreeData<TableOfContentItemVO> container) {
        List<TableOfContentItemVO> children = parentItem.getChildItems();
        if (children != null) {
            for (TableOfContentItemVO child: children) {
                container.addItem(parentItem, child);
                dropChildrenAtOriginalPosition(child, container);
            }
        }
    }

    private void handleMoveAction(TableOfContentItemVO moveFromItem, TreeGrid<TableOfContentItemVO> tocTree) {
        final TreeData<TableOfContentItemVO> container = tocTree.getTreeData();
        if ((moveFromItem.getOriginAttr() != null && moveFromItem.getOriginAttr().equals(EC)) &&
           ((moveFromItem.getSoftActionAttr() == null) || ((!moveFromItem.getSoftActionAttr().equals(MOVE_FROM)) && (!moveFromItem.getSoftActionAttr().equals(MOVE_TO))
           && (!moveFromItem.getSoftActionAttr().equals(ADD)) && (!moveFromItem.getSoftActionAttr().equals(DELETE))))) {
   
            TableOfContentItemVO moveToTemp = copyMovingItemToTemp(moveFromItem, Boolean.TRUE, tocTree);

            // Handles specific case while moving unnumbered paragraph together with numbered paragraphs
            if (moveFromItem.getType().getName().equalsIgnoreCase(PARAGRAPH) && moveFromItem.getNumber() == null) {
                List<TableOfContentItemVO> moveFromSiblings = container.getParent(moveFromItem).getChildItems();
                if (moveFromSiblings.size()>0) {
                    TableOfContentItemVO refItem = moveFromSiblings.get(0);
                    if (refItem.getNumber() != null) {
                        moveFromItem.setNumber("#");
                    }
                }
            }

            dropItemAtOriginalPosition(moveFromItem, moveToTemp, container);
            moveFromItem.setOriginNumAttr(CN);
            moveFromItem.setSoftActionRoot(Boolean.TRUE);

            TableOfContentItemVO moveToFinal = copyTempItemToFinalItem(moveToTemp);
            dropItemAtOriginalPosition(moveToTemp, moveToFinal, container);
            container.removeItem(moveToTemp);
            if (moveToTemp.getParentItem() != null) {
                moveToTemp.getParentItem().removeChildItem(moveToTemp);
            }
        }
        moveFromItem.setOriginNumAttr(CN);
        moveFromItem.setSoftActionRoot(Boolean.TRUE);

        setAffectedAttribute(moveFromItem, tocTree.getTreeData());
        if (MOVE_FROM.equals(moveFromItem.getSoftActionAttr())) {
            moveFromItem.setSoftMoveFrom(VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + moveFromItem.getId());
            TableOfContentItemVO moveToItem = getTableOfContentItemVOById(moveFromItem.getSoftMoveFrom(), container.getRootItems());
            if (moveToItem != null) {
                moveToItem.setSoftActionRoot(Boolean.TRUE);
                setAffectedAttribute(moveToItem, tocTree.getTreeData());
            }
        }
    }

    private static TableOfContentItemVO getTableOfContentItemVOById(String id, List<TableOfContentItemVO> tableOfContentItemVOS) {
        for (TableOfContentItemVO tableOfContentItemVO: tableOfContentItemVOS){
            if (tableOfContentItemVO.getId().equals(id)) {
                return tableOfContentItemVO;
            } else {
                TableOfContentItemVO childResult = getTableOfContentItemVOById(id, tableOfContentItemVO.getChildItems());
                if (childResult != null) {
                    return childResult;
                }
            }
        }
        return null;
    }

    private void restoreOriginal(final TableOfContentItemVO dropData, final TableOfContentItemVO targetItemVO, final TreeGrid<TableOfContentItemVO> tocTree) {
        setAttributesToOriginal(dropData, targetItemVO);
        TreeData<TableOfContentItemVO> container = tocTree.getTreeData();
        List<TableOfContentItemVO> dropDataChildElements = dropData.getChildItems();
        int position = getPositionOfNextNonRootChild(dropDataChildElements, 0);
        int index = 0;
        TableOfContentItemVO targetItemChildElement;
        while (index < targetItemVO.getChildItems().size()) {
            targetItemChildElement= targetItemVO.getChildItems().get(index);
            if (targetItemChildElement.isSoftActionRoot()) {
                if (position >= 0) {
                    moveItem(targetItemChildElement, dropDataChildElements.get(position), tocTree.getTreeData());
                } else {
                    moveItemToLastChild(targetItemChildElement, dropData, container);
                }
                position = getPositionOfNextNonRootChild(dropDataChildElements, dropDataChildElements.indexOf(targetItemChildElement));
            } else {
                if (position >= 0 && position < dropDataChildElements.size()) {
                    restoreOriginal(dropDataChildElements.get(position), targetItemChildElement, tocTree);
                    position = getPositionOfNextNonRootChild(dropDataChildElements, position + 1);
                }
                index++;
            }
        }
        tocTree.getTreeData().removeItem(targetItemVO);
    }
    
    private void moveItemToLastChild(TableOfContentItemVO item, TableOfContentItemVO parent, TreeData<TableOfContentItemVO> container) {
        TableOfContentItemVO tempDeletedItem = copyItemToTemp(item);
        container.removeItem(item);
        container.addItem(parent, tempDeletedItem);
        if (item.getParentItem() != null) {
            item.getParentItem().removeChildItem(item);
        }

        TableOfContentItemVO finalItem = copyTempItemToFinalItem(tempDeletedItem);
        container.removeItem(tempDeletedItem);
        container.addItem(parent, finalItem);
        parent.getChildItems().add(finalItem);
    }

    private TableOfContentItemVO moveItem(TableOfContentItemVO item, TableOfContentItemVO moveBefore, TreeData<TableOfContentItemVO> treeData) {
        TableOfContentItemVO tempDeletedItem = copyItemToTemp(item);
        dropItemAtOriginalPosition(moveBefore, tempDeletedItem, treeData);
        treeData.removeItem(item);
        if (item.getParentItem() != null) {
            item.getParentItem().removeChildItem(item);
        }

        TableOfContentItemVO finalItem = copyTempItemToFinalItem(tempDeletedItem);
        dropItemAtOriginalPosition(tempDeletedItem, finalItem, treeData);
        treeData.removeItem(tempDeletedItem);
        if (tempDeletedItem.getParentItem() != null) {
            tempDeletedItem.getParentItem().removeChildItem(tempDeletedItem);
        }
        return finalItem;
    }

    private int getPositionOfNextNonRootChild(List<TableOfContentItemVO> childrenElements, int position) {
        while (position < childrenElements.size()){
            if (!isRootElement(childrenElements.get(position))) {
                return position;
            }
            position++;
        }
        return -1;
    }

    private boolean isRootElement(TableOfContentItemVO element) {
        return Boolean.TRUE.equals(element.isSoftActionRoot());
    }

    private void setAttributesToOriginal(TableOfContentItemVO dropElement, TableOfContentItemVO targetItemVO) {
        if(targetItemVO.getNumSoftActionAttr() != null){
            dropElement.setNumber(targetItemVO.getNumber());
            dropElement.setNumSoftActionAttr(targetItemVO.getNumSoftActionAttr());
        }else if(targetItemVO.getNumber() != null && !targetItemVO.getNumber().isEmpty()) {
            dropElement.setNumber(targetItemVO.getNumber());
        }
        dropElement.setOriginNumAttr(EC);
        dropElement.setSoftActionAttr(null);
        dropElement.setSoftMoveTo(null);
        dropElement.setSoftMoveFrom(null);
        dropElement.setSoftUserAttr(null);
        dropElement.setSoftDateAttr(null);
        dropElement.setSoftActionRoot(null);
        dropElement.setRestored(true);
    }

    private void updateMovedOnEmptyParent(final TableOfContentItemVO dropData, final TableOfContentItemVO targetItemVO,
            final String movedOntoType, final String movedElementType) {

        if (targetItemVO != null && targetItemVO.getType().getName().equals(movedOntoType) &&
                dropData != null && dropData.getType().getName().equals(movedElementType) &&
                !containsMovedElement(targetItemVO.getChildItems(), movedElementType)) {
            dropData.setMovedOnEmptyParent(true);
        }
    }

    private boolean containsMovedElement( List<TableOfContentItemVO> childItems, String movedElementType) {
        for(TableOfContentItemVO child: childItems) {
            if(child.getType().getName().equals(movedElementType) && (child.getVtdIndex() != null)) {
                return true;
            }
        }
        return false;
    }

    private void restoreMovedItemOrSetNumber(final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO droppedItem, final TableOfContentItemVO newPosition, final ItemPosition position) {
        List<TableOfContentItemVO> siblings = ItemPosition.AS_CHILDREN.equals(position) ? tocTree.getTreeData().getChildren(newPosition) : tocTree.getTreeData().getChildren(tocTree.getTreeData().getParent(newPosition));
        Integer droppedItemIndex = siblings.indexOf(droppedItem);
        TableOfContentItemVO previousSibling = droppedItemIndex > 0 ? siblings.get(droppedItemIndex - 1) : null;
        TableOfContentItemVO nextSibling = droppedItemIndex < siblings.size() -1 ? siblings.get(droppedItemIndex + 1) : null;
        if (isPlaceholderForDroppedItem(newPosition, droppedItem)) {
            restoreOriginal(droppedItem, newPosition, tocTree);
            if (newPosition.getParentItem() != null) {
                newPosition.getParentItem().removeChildItem(newPosition);
            }
        } else if (isPlaceholderForDroppedItem(previousSibling, droppedItem)) {
            restoreOriginal(droppedItem, previousSibling, tocTree);
            if (newPosition.getParentItem() != null) {
                newPosition.getParentItem().removeChildItem(previousSibling);
            }
        } else if (isPlaceholderForDroppedItem(nextSibling, droppedItem)) {
            restoreOriginal(droppedItem, nextSibling, tocTree);
            if (newPosition.getParentItem() != null) {
                newPosition.getParentItem().removeChildItem(nextSibling);
            }
        } else {
            setNumber(droppedItem, newPosition);
        }
    }

    private Boolean isPlaceholderForDroppedItem(TableOfContentItemVO candidate, TableOfContentItemVO droppedItem) {
        return candidate != null && candidate.getId().equals(VTDUtils.SOFT_MOVE_PLACEHOLDER_ID_PREFIX + droppedItem.getId());
    }
}
