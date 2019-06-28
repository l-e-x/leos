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
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.ui.TreeGrid;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.services.content.toc.ProposalTocRulesMap;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.ui.component.toc.TocDropResult;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import freemarker.template.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@SpringComponent
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class ProposalTocEditor extends AbstractTocEditor {
    
    @Autowired
    private ProposalTocRulesMap proposalTocRulesMap;

    @Override
    public void deleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        tocTree.getTreeData().removeItem(tableOfContentItemVO);
        if(tableOfContentItemVO.getParentItem() != null){
            tableOfContentItemVO.getParentItem().removeChildItem(tableOfContentItemVO);
        }
        tocTree.getDataProvider().refreshAll();
        tocTree.deselectAll();
    }

    @Override
    public void undeleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO) {
        throw new UnsupportedOperationException("Soft Delete is possible only for Mandate instance");
    }

    @Override
    public TocDropResult addOrMoveItems(final boolean isAdd, final TreeGrid<TableOfContentItemVO> tocTree, final Map<TocItemType, List<TocItemType>> tableOfContentRules,
            final List<TableOfContentItemVO> droppedItems, final TableOfContentItemVO targetItem, final ItemPosition position) {

        TocDropResult result = validateAction(tocTree, tableOfContentRules, droppedItems, targetItem, position);
        if (result.isSuccess()) {
            TableOfContentItemVO parentItem = tocTree.getTreeData().getParent(targetItem);
            for (TableOfContentItemVO sourceItem : ItemPosition.BEFORE == position ? droppedItems : Lists.reverse(droppedItems)) {
                performAddOrMoveAction(isAdd, tocTree, tableOfContentRules, sourceItem, targetItem, parentItem, position);
            }
            tocTree.deselectAll();
            tocTree.getDataProvider().refreshAll();
        }
        return result;
    }

    @Override
    protected boolean validateAddingToItem(final TocDropResult result, final TableOfContentItemVO sourceItem, final TableOfContentItemVO targetItem,
            final TreeGrid<TableOfContentItemVO> tocTree, final TableOfContentItemVO actualTargetItem, final ItemPosition position) {

        return true;
    }

    @Override
    public boolean isArabicNumberingOnly(String itemType) {
        return proposalTocRulesMap.getArabicNumberingElements().stream().anyMatch(i -> StringUtil.capitalize(i.getName()).equals(itemType));
    }
    
    @Override
    public boolean isRomanNumberingOnly(String itemType) {
        return proposalTocRulesMap.getRomanNumberingElements().stream().anyMatch(i -> StringUtil.capitalize(i.getName()).equals(itemType));
    }
}
