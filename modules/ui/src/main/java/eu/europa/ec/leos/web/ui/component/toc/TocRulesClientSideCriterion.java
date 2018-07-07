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

import com.vaadin.event.Transferable;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.ClientSideCriterion;
import com.vaadin.server.PaintException;
import com.vaadin.server.PaintTarget;
import com.vaadin.shared.ui.dd.VerticalDropLocation;
import com.vaadin.ui.Component;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.v7.ui.Tree;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TocRulesClientSideCriterion extends ClientSideCriterion {
    private static final long serialVersionUID = -1818909780865191L;

    private static final Logger LOG = LoggerFactory.getLogger(TocRulesClientSideCriterion.class);

    private final Map<TocItemType, List<TocItemType>> tableOfContentRules;
    private final Function<String, TocItemType> getTocItemType;

    public TocRulesClientSideCriterion(Map<TocItemType, List<TocItemType>> tableOfContentRules, Function<String, TocItemType> getTocItemType) {
        this.tableOfContentRules = tableOfContentRules;
        this.getTocItemType = getTocItemType;
    }

    @Override
    public void paintContent(PaintTarget target) throws PaintException {
        super.paintContent(target);

        for (Map.Entry<TocItemType, List<TocItemType>> typeListEntry : tableOfContentRules.entrySet()) {

            TocItemType parent = (TocItemType) typeListEntry.getKey();
            List<TocItemType> allowedChildTypes = typeListEntry.getValue();
            List<String> allowedChildNames = allowedChildTypes.stream()
                    .map(TocItemType::toString)
                    .collect( Collectors.toList());

            target.addAttribute(parent.getName(), allowedChildNames.toArray());
        }
    }

    @Override
    public boolean accept(DragAndDropEvent dragEvent) {

        Transferable transferable = dragEvent.getTransferable();

        TocItemType draggedTocType = null;

        if (transferable.getSourceComponent() instanceof DragAndDropWrapper) {
            //check if we drag a new TOC type
            Component component = (Component) transferable.getData("component");
            String tocTypeAsId = component.getId();
            draggedTocType = getTocItemType.apply(tocTypeAsId);
        } else if (transferable.getSourceComponent() instanceof Tree) {
            //check if we drag an existing item from the tree
            String itemId = (String) transferable.getData("itemId");
            draggedTocType = extractTocTypeFromTreeItemId(itemId);
        }

        LOG.debug("draggedType: " + draggedTocType);

        Tree.TreeTargetDetails targetDetails = (Tree.TreeTargetDetails) dragEvent.getTargetDetails();

        VerticalDropLocation verticalDropLocation = targetDetails.getDropLocation();
        String itemIdOver = (String) targetDetails.getItemIdOver(); //get the id of the target node
        TocItemType dropTocType = getDropTocType(itemIdOver, verticalDropLocation);
        //LEOS-2334
        List<TocItemType> allowedChildrenForDropType = Collections.emptyList();
        if (dropTocType != null) {
            allowedChildrenForDropType = tableOfContentRules.get(dropTocType);
            
            if(allowedChildrenForDropType.isEmpty() || !allowedChildrenForDropType.contains(draggedTocType)) {
                String itemIdInto = (String) targetDetails.getItemIdInto(); //look for the parent node if children not allowed for target node
                dropTocType = getDropTocType(itemIdInto, verticalDropLocation);
                allowedChildrenForDropType = tableOfContentRules.get(dropTocType);
            }
        }
        
        LOG.debug("draggedTocType = " + draggedTocType +
                "; verticalDropLocation: " + verticalDropLocation +
                "; dropTocType: " + dropTocType +
                "; allowedChildrenForDroppedType: " + allowedChildrenForDropType
        );

        return allowedChildrenForDropType.contains(draggedTocType);
    }

    private TocItemType getDropTocType(String itemIdInto, VerticalDropLocation verticalDropLocation) {
        TocItemType droppedType = null;

        switch (verticalDropLocation) {
            case MIDDLE:
            case BOTTOM:
                //the item idInto gives us:
                // - either the ID of the item when we are with the mouse in the MIDDLE of the item (itemIdOver)
                // - either the ID of the parent item when we are with the mouse in the BOTTOM of the item
                droppedType = extractTocTypeFromTreeItemId(itemIdInto);
                break;
            default:
                droppedType = null;
        }
        return droppedType;
    }

    private TocItemType extractTocTypeFromTreeItemId(String itemId) {
        TocItemType tocType = null;
        if (itemId != null) {
            tocType = getTocItemType.apply(itemId.substring(itemId.indexOf('_') + 1));
        }
        return tocType;
    }

}
