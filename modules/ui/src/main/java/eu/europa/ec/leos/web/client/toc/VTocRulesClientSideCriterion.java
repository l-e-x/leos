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
package eu.europa.ec.leos.web.client.toc;

import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ComponentConnector;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.VTree;
import com.vaadin.client.ui.dd.VAcceptCriterion;
import com.vaadin.client.ui.dd.VDragAndDropManager;
import com.vaadin.client.ui.dd.VDragEvent;
import com.vaadin.client.ui.dd.VTransferable;
import com.vaadin.client.ui.draganddropwrapper.DragAndDropWrapperConnector;
import com.vaadin.client.ui.tree.TreeConnector;
import com.vaadin.shared.ui.dd.AcceptCriterion;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import eu.europa.ec.leos.web.ui.component.toc.TocRulesClientSideCriterion;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@AcceptCriterion(TocRulesClientSideCriterion.class)
final public class VTocRulesClientSideCriterion extends VAcceptCriterion {

    private static final Logger logger = Logger.getLogger(VTocRulesClientSideCriterion.class.getName());

    @Override
    protected boolean accept(VDragEvent drag, UIDL configuration) {

        VTransferable vTransferable = drag.getTransferable();

        String draggedTocType = null;

        if (vTransferable.getDragSource() instanceof DragAndDropWrapperConnector) {
            //check if we drag a new TOC type from the new TOC element's component
            ComponentConnector componentConnector = (ComponentConnector) vTransferable.getData("component");
            String tocTypeAsId = componentConnector.getState().id;
            draggedTocType = tocTypeAsId;
        } else if (vTransferable.getDragSource() instanceof TreeConnector) {
            //check if we drag an existing item from the tree
            String itemId = (String) vTransferable.getData("itemId");
            draggedTocType = extractTocTypeFromTreeItemId(itemId);
        }

        Map<String, Object> dropDetails = drag.getDropDetails();

        //the drop area is inside the tree, so we have the itemIdOver and its vertical drop location as detail
        String itemIdOver = (String) dropDetails.get("itemIdOver");
        VerticalDropLocation verticalDropLocation = (VerticalDropLocation) dropDetails.get("detail");

        String dropTocType = getDropTocType(itemIdOver, verticalDropLocation);

        List<String> allowedChildrenForDropType = Collections.emptyList();
        if (dropTocType != null) {
            String[] dropAllowedChildren = configuration.getStringArrayAttribute(dropTocType);
            allowedChildrenForDropType = Arrays.asList(dropAllowedChildren);

        }

        logger.log(Level.FINE, "draggedTocType = " + draggedTocType +
                "; verticalDropLocation: " + verticalDropLocation +
                "; dropTocType: " + dropTocType +
                "; allowedChildrenForDroppedType: " + allowedChildrenForDropType
        );

        return allowedChildrenForDropType.contains(draggedTocType);
    }

    private String getDropTocType(String itemIdOver, VerticalDropLocation verticalDropLocation) {
        String dropTocType = null;
        switch (verticalDropLocation) {
            case MIDDLE: {
                dropTocType = extractTocTypeFromTreeItemId(itemIdOver);
                break;
            }
            case BOTTOM: {
                //in case we drop on the bottom of an item, we need to check if we can append the dragged item as
                // a child of the item's parent for which we are located on the bottom
                VTree tree = (VTree) VDragAndDropManager.get().getCurrentDropHandler().getConnector().getWidget();
                VTree.TreeNode treeNode = tree.getNodeByKey(itemIdOver);
                //this is how we get the parent tree node
                Widget parent2 = treeNode.getParent().getParent();

                if (parent2 instanceof VTree.TreeNode) {
                    String itemIdInto = ((VTree.TreeNode) parent2).key;
                    dropTocType = extractTocTypeFromTreeItemId(itemIdInto);
                } else {
                    logger.log(Level.FINE, "the parent is not a treeNode?? ");
                }
                break;
            }
            default:
                dropTocType = null;
        }
        return dropTocType;
    }

    private String extractTocTypeFromTreeItemId(String treeItemId) {
        int delimiterPosition = treeItemId.indexOf('_');
        String tocType = delimiterPosition >= 0 ? treeItemId.substring(delimiterPosition + 1) : null;
        return tocType;
    }
}
