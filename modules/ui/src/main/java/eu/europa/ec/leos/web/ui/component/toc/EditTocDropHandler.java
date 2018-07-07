/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.web.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.v7.data.Item;
import com.vaadin.v7.data.util.HierarchicalContainer;
import com.vaadin.v7.event.DataBoundTransferable;
import com.vaadin.v7.ui.Tree;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EditTocDropHandler implements DropHandler {

    private static final long serialVersionUID = -804544966570987338L;
    private final Tree tocTree;
    private final MessageHelper messageHelper;
    private final Map<TocItemType, List<TocItemType>> tableOfContentRules;
    private final Function<String, TocItemType> getTocItemType;

    public EditTocDropHandler(Tree tocTree, MessageHelper messageHelper, EventBus eventBus, Map<TocItemType, List<TocItemType>> tableOfContentRules, Function<String, TocItemType> getTocItemType) {
        this.tocTree = tocTree;
        this.messageHelper = messageHelper;
        this.tableOfContentRules = tableOfContentRules;
        this.getTocItemType = getTocItemType;
    }

    @Override
    public void drop(DragAndDropEvent dropEvent) {
        final Transferable transferable = dropEvent.getTransferable();
        final Component sourceComponent = transferable.getSourceComponent();

        final Tree.TreeTargetDetails dropData = ((Tree.TreeTargetDetails) dropEvent.getTargetDetails());

        final HierarchicalContainer container = (HierarchicalContainer) tocTree.getContainerDataSource();
        // Location describes on which part of the node the drop took place
        final VerticalDropLocation location = dropData.getDropLocation();
        
        final Object targetItemId = dropData.getItemIdOver();
        Item targetItem = container.getItem(targetItemId);
        TableOfContentItemVO targetItemVO = TableOfContentItemConverter.buildTableOfContentItemVO(targetItem);
        container.setChildrenAllowed(targetItemId, targetItemVO.areChildrenAllowed()); //Set children allowed for targetItem
        
        if (sourceComponent instanceof DragAndDropWrapper) {
            TocItemType sourceItemType = getTocItemType.apply((String) ((DragAndDropWrapper) sourceComponent).getData());

            String sourceItemId = TableOfContentItemConverter.getItemId(container, sourceItemType);
            Item sourceItem = container.addItem(sourceItemId);

            String typeName = sourceItemType.getName().toLowerCase();
            String number = messageHelper.getMessage("toc.item.type." + typeName + ".number"); // LEOS-787 Put default text for number & heading
            String heading = messageHelper.getMessage("toc.item.type." + typeName + ".heading");

            TableOfContentItemVO sourceItemVO = new TableOfContentItemVO(sourceItemType, null, number, heading, null, null, null);
            TableOfContentItemConverter.populateItemDetails(sourceItemVO, sourceItem, Integer.MAX_VALUE, messageHelper);
            
            container.setChildrenAllowed(sourceItemId, sourceItemVO.areChildrenAllowed()); //Set children allowed for sourceItem

            moveNode(sourceItemId, targetItemId, location);
            tocTree.select(sourceItemId);
        } else if (sourceComponent instanceof Tree) {
            final Object sourceItemId = ((DataBoundTransferable) transferable).getItemId();
            moveNode(sourceItemId, targetItemId, location);
            tocTree.select(sourceItemId);
        }
    }

    private void moveNode(final Object sourceItemId, final Object targetItemId, final VerticalDropLocation location) {
        final HierarchicalContainer container = (HierarchicalContainer) tocTree.getContainerDataSource();

        // Sorting goes as
        // - If dropped ON a node, we append it as a child
        // - If dropped on the TOP part of a node, we move/add it before
        // the node
        // - If dropped on the BOTTOM part of a node, we move/add it
        // after the node

        if (location == VerticalDropLocation.MIDDLE) {
            if (container.setParent(sourceItemId, targetItemId)
                    && container.hasChildren(targetItemId)) {
                // move first in the container
                container.moveAfterSibling(sourceItemId, null);
            }
        } else if (location == VerticalDropLocation.TOP) {
            final Object parentId = container.getParent(targetItemId);
            if (container.setParent(sourceItemId, parentId)) {
                // reorder only the two items, moving source above target
                container.moveAfterSibling(sourceItemId, targetItemId);
                container.moveAfterSibling(targetItemId, sourceItemId);
            }
        } else if (location == VerticalDropLocation.BOTTOM) {
            final Object parentId = container.getParent(targetItemId);
            if (container.setParent(sourceItemId, parentId)) {
                container.moveAfterSibling(sourceItemId, targetItemId);
            }
        }
    }

    @Override
    public AcceptCriterion getAcceptCriterion() {
        return new TocRulesClientSideCriterion(tableOfContentRules, getTocItemType);
    }

}
