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
import com.vaadin.ui.UI;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.ui.window.toc.MultiSelectTreeGrid;
import eu.europa.ec.leos.vo.toc.AknTag;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.view.document.CancelActionElementRequestEvent;
import org.vaadin.dialogs.ConfirmDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static eu.europa.ec.leos.vo.toc.AknTag.ARTICLE;
import static eu.europa.ec.leos.vo.toc.AknTag.CITATION;
import static eu.europa.ec.leos.vo.toc.AknTag.LEVEL;
import static eu.europa.ec.leos.vo.toc.AknTag.RECITAL;

public class CheckDeleteLastEditingTypeConsumer implements BiConsumer<String, Runnable> {

    private static final List<AknTag> NODES_TO_CONSIDER = Arrays.asList(CITATION, RECITAL, ARTICLE, LEVEL);
    private final MultiSelectTreeGrid<TableOfContentItemVO> tocTree;
    private final MessageHelper messageHelper;
    private final EventBus eventBus;

    public CheckDeleteLastEditingTypeConsumer(MultiSelectTreeGrid<TableOfContentItemVO> tocTree, MessageHelper messageHelper, EventBus eventBus) {
        this.tocTree = tocTree;
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
    }

    @Override
    public void accept(String elementId, Runnable deletionAction) {
        if (isDeletingLastEditingType(elementId)) {
            ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                    messageHelper.getMessage("lasteditionelement.confirmation.title"),
                    messageHelper.getMessage("lasteditionelement.confirmation.message"),
                    messageHelper.getMessage("lasteditionelement.confirmation.confirm"),
                    messageHelper.getMessage("lasteditionelement.confirmation.cancel"),
                    null);
            confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
            confirmDialog.getContent().setHeightUndefined();
            confirmDialog.setHeightUndefined();
            confirmDialog.show(UI.getCurrent(), dialog -> {
                if (dialog.isConfirmed()) {
                    deletionAction.run();
                } else {
                    eventBus.post(new CancelActionElementRequestEvent(elementId));
                }
            }, true);
        } else {
            deletionAction.run();
        }
    }

    private boolean isDeletingLastEditingType(String elementId) {
        TableOfContentItemVO item = findElementById(elementId, tocTree.getTreeData().getRootItems()).orElseThrow(() -> new IllegalArgumentException("Element not found by id: " + elementId));
        TableOfContentItemVO root = findRoot(item).orElseThrow(() -> new IllegalArgumentException("Root element not found for the id: " + elementId));
        List<TableOfContentItemVO> allItems = root.flattened().collect(Collectors.toList());
        List<TableOfContentItemVO> toBeDeleted = item.flattened().collect(Collectors.toList());
        List<TableOfContentItemVO> toBeRemain = new ArrayList<>(allItems);
        toBeRemain.removeAll(toBeDeleted);
        Set<AknTag> allElementTypes = getDistinctElementTypes(allItems);
        Set<AknTag> toBeRemainElementTypes = getDistinctElementTypes(toBeRemain);
        return allElementTypes.size() != toBeRemainElementTypes.size();
    }

    private Optional<TableOfContentItemVO> findElementById(String elementId, List<TableOfContentItemVO> children) {
        for (TableOfContentItemVO child : children) {
            if (child.getId().equals(elementId)) {
                return Optional.of(child);
            } else {
                if (child.getChildItems().size() > 0) {
                    Optional<TableOfContentItemVO> item = findElementById(elementId, child.getChildItems());
                    if (item.isPresent()) {
                        return item;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<TableOfContentItemVO> findRoot(TableOfContentItemVO item) {
        if (item.getParentItem().getTocItem().isRoot()) {
            return Optional.of(item.getParentItem());
        } else {
            return findRoot(item.getParentItem());
        }
    }

    private Set<AknTag> getDistinctElementTypes(List<TableOfContentItemVO> allItems) {
        return allItems.stream()
                .filter(p -> NODES_TO_CONSIDER.contains(p.getTocItem().getAknTag()))
                .map(p -> p.getTocItem().getAknTag())
                .collect(Collectors.toSet());
    }

}
