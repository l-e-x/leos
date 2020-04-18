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

import com.vaadin.data.TreeData;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;

public class TableOfContentItemConverter {

    public static TreeData<TableOfContentItemVO> buildTocData(List<TableOfContentItemVO> tableOfContentsItemVOList) {
        // Initialise container and its properties
        TreeData<TableOfContentItemVO> treeData = new TreeData<>();
        treeData.addItems(tableOfContentsItemVOList, TableOfContentItemVO::getChildItems);
        return treeData;
    }

    public static List<TableOfContentItemVO> buildTocItemVOList(TreeData<TableOfContentItemVO> treeData) {
        buildTocItemVOList(treeData, null);
        return treeData.getRootItems();
    }

    public static void buildTocItemVOList(TreeData<TableOfContentItemVO> treeData, TableOfContentItemVO parent) {
        // we need to set children in parent explicitly as vaadin maintains its internal structure and we need to update out own VO
        // 1. update all children
        if (parent != null) {
            parent.removeAllChildItems();// update all in vaadin order
            parent.addAllChildItems(treeData.getChildren(parent));
        }

        // 2. update self /treeData.getChildren(null) returns all root nodes. recursively populate structure
        treeData.getChildren(parent).forEach(child -> buildTocItemVOList(treeData, child));
    }
}
