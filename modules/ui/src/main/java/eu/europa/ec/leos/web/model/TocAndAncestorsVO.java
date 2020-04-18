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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.i18n.MessageHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates the tocItems and the ancestors ids for selected toc item
 */
public class TocAndAncestorsVO {

    private Map<String, List<TocItemVO>> tocItemsMap;
    private List<String> elementAncestorsIds;

    public TocAndAncestorsVO(Map<String, List<TableOfContentItemVO>> tocItemList,
                             List<String> elementAncestorsIds, MessageHelper messageHelper) {
        tocItemsMap = new HashMap<>(tocItemList.size());
        for (String ref : tocItemList.keySet()) {
            List<TocItemVO> tocItems = new ArrayList<>();
            for (TableOfContentItemVO tableOfContentItemVO : tocItemList.get(ref)) {
                TocItemVO tocItemVO = new TocItemVO(tableOfContentItemVO,
                        messageHelper);
                tocItems.add(tocItemVO);
            }
            tocItemsMap.put(ref, tocItems);
        }
        this.elementAncestorsIds = elementAncestorsIds;
    }

    public Map<String, List<TocItemVO>> getTocItemsMap() {
        return tocItemsMap;
    }

    public void setTocItemsMap(Map<String, List<TocItemVO>> tocItemsMap) {
        this.tocItemsMap = tocItemsMap;
    }

    public List<String> getElementAncestorsIds() {
        return elementAncestorsIds;
    }

    public void setElementAncestorsIds(List<String> elementAncestorsIds) {
        this.elementAncestorsIds = elementAncestorsIds;
    }
}