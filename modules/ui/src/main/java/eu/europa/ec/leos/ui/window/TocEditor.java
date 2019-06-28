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
import eu.europa.ec.leos.ui.component.toc.TocDropResult;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;

import java.util.List;
import java.util.Map;

public interface TocEditor {

    void setTocTreeDataFilter(TreeDataProvider<TableOfContentItemVO> dataProvider);

    void setTocTreeStyling(MultiSelectTreeGrid<TableOfContentItemVO> treeGrid, TreeDataProvider<TableOfContentItemVO> dataProvider);

    boolean isDeletableItem(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO);

    boolean isDeletedItem(TableOfContentItemVO tableOfContentItemVO);

    boolean isUndeletableItem(TableOfContentItemVO tableOfContentItemVO);

    boolean checkIfConfirmDeletion(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO tableOfContentItemVO);

    void deleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO);
    
    void undeleteItem(TreeGrid<TableOfContentItemVO> tocTree, TableOfContentItemVO tableOfContentItemVO);

    boolean isArabicNumberingOnly(String itemType);
    
    boolean isRomanNumberingOnly(String itemType);

    /**
     * Depending on the
     * @param isAdd, adds or moves the
     * @param droppedItems in the
     * @param tocTree according to
     * @param tableOfContentRules and relative to
     * @param targetItem, either
     * @param position - AS_CHILDREN, if dropped ON the targetItem
     *                 - BEFORE, if dropped ABOVE targetItem
     *                 - AFTER, if dropped BELOW targetItem
     * and
     * @return the result of the action
     * */
    TocDropResult addOrMoveItems(boolean isAdd, TreeGrid<TableOfContentItemVO> tocTree, Map<TocItemType, List<TocItemType>> tableOfContentRules,
            List<TableOfContentItemVO> droppedItems, TableOfContentItemVO targetItem, ItemPosition position);

    enum ItemPosition {
        BEFORE,
        AS_CHILDREN,
        AFTER
    }
}
