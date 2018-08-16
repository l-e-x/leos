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
package eu.europa.ec.leos.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.TreeData;
import com.vaadin.shared.ui.grid.DropLocation;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.TreeGridDropEvent;
import com.vaadin.ui.components.grid.TreeGridDropListener;
import eu.europa.ec.leos.ui.event.toc.TocStatusUpdateEvent;
import eu.europa.ec.leos.ui.window.TocEditor;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EditTocDropHandler<T extends TocItemType> implements TreeGridDropListener<TableOfContentItemVO> {

    private static final long serialVersionUID = -804544966570987338L;
    private final TreeGrid<TableOfContentItemVO> tocTree;
    private final MessageHelper messageHelper;
    private final EventBus eventBus;
    private final Map<T, List<T>> tableOfContentRules;
    private final TocEditor tocEditor;

    public EditTocDropHandler(TreeGrid<TableOfContentItemVO> tocTree, MessageHelper messageHelper, EventBus eventBus, Map<T, List<T>> tableOfContentRules, TocEditor tocEditor) {
        this.tocTree = tocTree;
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.tableOfContentRules = tableOfContentRules;
        this.tocEditor = tocEditor;
    }

    @Override
    public void drop(TreeGridDropEvent<TableOfContentItemVO> dropEvent) {

        Component sourceComponent = dropEvent.getDragSourceComponent().orElse(null);
        final Collection<TableOfContentItemVO> droppedItems = (Collection<TableOfContentItemVO>) dropEvent.getDragData().orElse(null);
        TableOfContentItemVO targetItemVO = dropEvent.getDropTargetRow().orElse(null);

        // Location describes on which part of the node the drop took place
        final DropLocation location = dropEvent.getDropLocation();

        if (droppedItems == null || droppedItems.size() == 0) {
            return;
        }

        if (sourceComponent instanceof Label) {
            tocTree.getTreeData().addItems(null, droppedItems);// adding the dropped item to the tree at root level
        }

        boolean success = moveNode(droppedItems, targetItemVO, location);
        
        droppedItems.forEach(dropData -> {
            if (sourceComponent instanceof Label && !success) {
                tocTree.getTreeData().removeItem(dropData);
            } else if (success) {
                dropData = tocEditor.moveOriginAttribute(dropData, targetItemVO);
                tocTree.deselectAll();
                tocTree.select(dropData);
                tocTree.focus();
            }
            if (tocTree.getTreeData().contains(dropData)) {
                tocTree.expand(tocTree.getTreeData().getParent(dropData));
                
                //FIXME: Temporary fix to scroll to the latest drop item if not in the view. (This should be re-implementated once Vaadin TreeGrid updates the api for scrollTo)
                List<TableOfContentItemVO> treeItems = new ArrayList<>();
                treeItems = createTreeDataList(tocTree.getTreeData().getRootItems(), treeItems);
                if(treeItems.indexOf(dropData) != -1) {
                 tocTree.scrollTo(treeItems.indexOf(dropData));
                }
                treeItems = null;
            }
        });
        setStatusMessage(droppedItems.iterator().next(), targetItemVO, success);
        tocTree.getDataProvider().refreshAll();
    }
    
    private List<TableOfContentItemVO> createTreeDataList(List<TableOfContentItemVO> items, List<TableOfContentItemVO> treeItems) {
        for(TableOfContentItemVO item : items) {
            treeItems.add(item);
            List<TableOfContentItemVO> childItems = tocTree.getTreeData().getChildren(item);
            if (!childItems.isEmpty()) {
                createTreeDataList(childItems, treeItems);
            }
        }
        return treeItems;
    }

    private void setStatusMessage(TableOfContentItemVO sourceItemVO, TableOfContentItemVO targetItemVO, boolean success) {
        String message;
        
        String srcItemType = TableOfContentItemConverter.getDisplayableItemType(sourceItemVO.getType(), messageHelper);
        String targetItemType = TableOfContentItemConverter.getDisplayableItemType(targetItemVO.getType(), messageHelper);

        if (success) {
            message = messageHelper.getMessage("toc.edit.window.drop.success.message", srcItemType, targetItemType);
            eventBus.post(new TocStatusUpdateEvent(message, TocStatusUpdateEvent.Result.SUCCESSFUL));

        } else {
            message = messageHelper.getMessage("toc.edit.window.drop.error.message", srcItemType, targetItemType);
            eventBus.post(new TocStatusUpdateEvent(message, TocStatusUpdateEvent.Result.ERROR));
        }
    }

    private boolean moveNode(final Collection<TableOfContentItemVO> droppedItems, final TableOfContentItemVO targetItemId, final DropLocation location) {
        boolean success = false;
        final TreeData<TableOfContentItemVO> container = tocTree.getTreeData();
        TableOfContentItemVO parentId = container.getParent(targetItemId);
        TocItemType parentItemType = parentId != null ? parentId.getType() : null;
        List<T> parentItemTypes = tableOfContentRules.get(parentItemType);
        TocItemType targetItemType = targetItemId.getType();

        for(TableOfContentItemVO sourceItemId : droppedItems) {
            TocItemType sourceItemType = sourceItemId.getType();

            // Sorting goes as
            // - If dropped ON a node, we append it as a child
            // - If dropped on the ABOVE part of a node, we move/add it before
            // the node
            // - If dropped on the BELOW part of a node, we move/add it after the node
            switch (location) {
                case ON_TOP:
                    if (targetItemId.areChildrenAllowed()) {
                        success = handleDrop(sourceItemId, targetItemId, container, parentId, targetItemType, sourceItemType, location);
                    } else { // If child elements not allowed in target add it to its parent
                        if (parentItemTypes != null && parentItemTypes.contains(sourceItemId.getType()) &&
                                !parentItemType.getName().equals(sourceItemType.getName())) {
                            container.setParent(sourceItemId, parentId);
                            container.moveAfterSibling(sourceItemId, targetItemId);
                            success = true;
                        }
                    }

                    break;
                case ABOVE:
                    if (targetItemId.areChildrenAllowed()) {
                        success = handleDrop(sourceItemId, targetItemId, container, parentId, targetItemType, sourceItemType, location);
                    } else if (parentItemTypes != null && parentItemTypes.size() > 0 && !parentItemType.getName().equals(sourceItemType.getName()) &&
                            parentItemTypes.contains(sourceItemId.getType())) {
                        container.setParent(sourceItemId, parentId);
                        // reorder only the two items, moving source above target
                        container.moveAfterSibling(sourceItemId, targetItemId);
                        container.moveAfterSibling(targetItemId, sourceItemId);
                        success = true;
                    }

                    break;
                case BELOW:
                    if (targetItemId.areChildrenAllowed()) {
                        success = handleDrop(sourceItemId, targetItemId, container, parentId, targetItemType, sourceItemType, location);
                    } else if (parentItemTypes != null && parentItemTypes.size() > 0 && !parentItemType.getName().equals(sourceItemType.getName()) &&
                            parentItemTypes.contains(sourceItemId.getType())) {
                        container.setParent(sourceItemId, parentId);
                        container.moveAfterSibling(sourceItemId, targetItemId);
                        success = true;
                    }
                    break;
                default:
                    success = false;
                    return success;
            }
        }
        return success;
    }

    private boolean handleDrop(final TableOfContentItemVO sourceItemId, final TableOfContentItemVO targetItemId, final TreeData<TableOfContentItemVO> container,
            TableOfContentItemVO parentId, TocItemType targetItemType, TocItemType sourceItemType, final DropLocation location) {
        boolean success = false;
        List<T> targetItemTypes = tableOfContentRules.get(targetItemType);
        if (sourceItemType.getName().equals(targetItemType.getName())) { // If source and target both are same, add source as sibling
            success = addItemAtDropLocation(sourceItemId, targetItemId, container, parentId, location,true);
        } else if (targetItemTypes.size() > 0 && targetItemTypes.contains(sourceItemId.getType())) {
            //If target item type is root, source item will be added as child, else item is dropped at dragged location
            if (targetItemType.isRoot()) {
                container.setParent(sourceItemId, targetItemId);
                success = true;
            } else {
                success = addItemAtDropLocation(sourceItemId, targetItemId, container, parentId, location, false);
            }
        }
        return success;
    }

    private boolean addItemAtDropLocation(final TableOfContentItemVO sourceItemId, final TableOfContentItemVO targetItemId,
            final TreeData<TableOfContentItemVO> container, TableOfContentItemVO parentId, final DropLocation location, boolean isItemTypeSibling) {
        container.setParent(sourceItemId, parentId);
        switch (location) {
            case ABOVE:
                //there is not moveBefore, so source is first moved after target and then target and source are swapped
                container.moveAfterSibling(sourceItemId, targetItemId);
                container.moveAfterSibling(targetItemId, sourceItemId);
                break;
            case BELOW:
                container.moveAfterSibling(sourceItemId, targetItemId);
                break;
            case ON_TOP:
                if (isItemTypeSibling) {
                    container.moveAfterSibling(sourceItemId, targetItemId);
                } else {
                    container.setParent(sourceItemId, targetItemId);
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
