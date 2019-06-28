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
package eu.europa.ec.leos.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.TreeData;
import com.vaadin.data.provider.DataCommunicator;
import com.vaadin.shared.ui.grid.DropLocation;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.components.grid.TreeGridDropEvent;
import com.vaadin.ui.components.grid.TreeGridDropListener;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.ui.event.toc.TocStatusUpdateEvent;
import eu.europa.ec.leos.ui.window.TocEditor;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.i18n.MessageHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class EditTocDropHandler implements TreeGridDropListener<TableOfContentItemVO> {

    private static final long serialVersionUID = -804544966570987338L;
    private final TreeGrid<TableOfContentItemVO> tocTree;
    private final MessageHelper messageHelper;
    private final EventBus eventBus;
    private final Map<TocItemType, List<TocItemType>> tableOfContentRules;
    private final TocEditor tocEditor;

    public EditTocDropHandler(TreeGrid<TableOfContentItemVO> tocTree, MessageHelper messageHelper, EventBus eventBus, Map<TocItemType, List<TocItemType>> tableOfContentRules, TocEditor tocEditor) {
        this.tocTree = tocTree;
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.tableOfContentRules = tableOfContentRules;
        this.tocEditor = tocEditor;
    }

    @Override
    public void drop(TreeGridDropEvent<TableOfContentItemVO> dropEvent) {
        final Collection<TableOfContentItemVO> droppedItemCollection = (Collection<TableOfContentItemVO>) dropEvent.getDragData().orElse(null);
        if (droppedItemCollection == null || droppedItemCollection.size() == 0) {
            return;
        }
        List<TableOfContentItemVO> droppedItems = new ArrayList<>(droppedItemCollection);
        TableOfContentItemVO targetItem = dropEvent.getDropTargetRow().orElse(null);
        TocEditor.ItemPosition position = getPositionFromLocation(dropEvent.getDropLocation());
        if (position != null) {
            boolean isAdd = dropEvent.getDragSourceComponent().orElse(null) instanceof Label;
            TocDropResult tocDropResult = tocEditor.addOrMoveItems(isAdd, tocTree, tableOfContentRules, droppedItems, targetItem, position);
            if (tocDropResult.isSuccess()) {
                scrollToDroppedItem(tocTree.getTreeData(), droppedItems.get(0));
            }
            setStatusMessage(tocDropResult);
        } else {
            setStatusMessage(new TocDropResult(false, "toc.edit.window.drop.error.message", droppedItems.get(0), targetItem));
        }
    }

    private TocEditor.ItemPosition getPositionFromLocation(DropLocation location) {
        if(location == null) {
            return null;
        }
        switch (location) {
            case ON_TOP: return TocEditor.ItemPosition.AS_CHILDREN;
            case ABOVE: return TocEditor.ItemPosition.BEFORE;
            case BELOW: return TocEditor.ItemPosition.AFTER;
            default: return null;
        }
    }

    private void setStatusMessage(TocDropResult tocDropResult) {
        String message;
        String srcItemType = TableOfContentHelper.getDisplayableItemType(tocDropResult.getSourceItem().getType(), messageHelper);
        if (tocDropResult.getTargetItem() != null) {
            String targetItemType = TableOfContentHelper.getDisplayableItemType(tocDropResult.getTargetItem().getType(), messageHelper);
            message = messageHelper.getMessage(tocDropResult.getMessageKey(), srcItemType, targetItemType);
            eventBus.post(new TocStatusUpdateEvent(message, tocDropResult.isSuccess() ? TocStatusUpdateEvent.Result.SUCCESSFUL :
                    TocStatusUpdateEvent.Result.ERROR));
        } else {
            message = messageHelper.getMessage("toc.edit.window.drop.error.root.message", srcItemType);
            eventBus.post(new TocStatusUpdateEvent(message, TocStatusUpdateEvent.Result.ERROR));
        }
    }

    private void scrollToDroppedItem(TreeData<TableOfContentItemVO> container, TableOfContentItemVO dropData) {
        tocTree.expand(container.getParent(dropData));
        DataCommunicator<TableOfContentItemVO> dataCommunicator = tocTree.getDataCommunicator();
        List<TableOfContentItemVO> treeItems = dataCommunicator.fetchItemsWithRange(0, dataCommunicator.getDataProviderSize());
        tocTree.scrollTo(treeItems.indexOf(dropData));
        tocTree.select(dropData);
    }
}
