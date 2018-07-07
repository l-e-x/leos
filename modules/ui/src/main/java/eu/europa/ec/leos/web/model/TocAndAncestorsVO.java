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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates the tocItems and the ancestors ids for selected toc item
 */
public class TocAndAncestorsVO {

    private List<TocItemVO> tocItems;
    private List<String> elementAncestorsIds;

    public TocAndAncestorsVO(List<TableOfContentItemVO> tocItemList,
            List<String> elementAncestorsIds, MessageHelper messageHelper) {
        tocItems = new ArrayList<TocItemVO>(tocItemList.size());
        for (TableOfContentItemVO tableOfContentItemVO : tocItemList) {
            TocItemVO tocItemVO = new TocItemVO(tableOfContentItemVO,
                    messageHelper);
            tocItems.add(tocItemVO);
        }
        this.elementAncestorsIds = elementAncestorsIds;
    }

    public List<TocItemVO> getTocItems() {
        return tocItems;
    }

    public void setTocItems(List<TocItemVO> tocItems) {
        this.tocItems = tocItems;
    }

    public List<String> getElementAncestorsIds() {
        return elementAncestorsIds;
    }

    public void setElementAncestorsIds(List<String> elementAncestorsIds) {
        this.elementAncestorsIds = elementAncestorsIds;
    }
}
