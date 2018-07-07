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
package eu.europa.ec.leos.web.ui.component.toc;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Item;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.DataBoundTransferable;
import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.DropHandler;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.Tree;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;

import java.util.List;
import java.util.Map;

public class EditTocDropHandler implements DropHandler {

    private final Tree tocTree;
    private final MessageHelper messageHelper;
    private final EventBus eventBus;
    private final Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules;

    public EditTocDropHandler(Tree tocTree, MessageHelper messageHelper, EventBus eventBus, Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules) {
        this.tocTree = tocTree;
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.tableOfContentRules = tableOfContentRules;
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

        if (sourceComponent instanceof DragAndDropWrapper) {
            TableOfContentItemVO.Type sourceItemType = (TableOfContentItemVO.Type) ((DragAndDropWrapper) sourceComponent).getData();

            String itemId = TableOfContentItemConverter.getItemId(container, sourceItemType);
            Item item = container.addItem(itemId);

            String typeName = sourceItemType.name().toLowerCase();
            String number = messageHelper.getMessage("toc.item.type." + typeName + ".number"); // LEOS-787 Put default text for number & heading
            String heading = messageHelper.getMessage("toc.item.type." + typeName + ".heading");

            TableOfContentItemVO itemVO = new TableOfContentItemVO(sourceItemType, null, number, heading, null, null, null);
            TableOfContentItemConverter.populateItemDetails(itemVO, item, Integer.MAX_VALUE, messageHelper);

            container.setChildrenAllowed(itemId, itemVO.areChildrenAllowed());

            moveNode(itemId, targetItemId, location);
            tocTree.select(itemId);
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
        return new TocRulesClientSideCriterion(tableOfContentRules);
    }

}
