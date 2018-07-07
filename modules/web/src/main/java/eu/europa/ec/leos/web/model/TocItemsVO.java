/**
 * Copyright 2015 European Commission
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

import java.util.ArrayList;
import java.util.List;

import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

/**
 * Aggregates the tocItems and the ancestors ids for selected toc item
 */
public class TocItemsVO {

    private List<TocItemVO> tocItems;
    private List<String> ancestorsIds;

    /* This empty constructor is provided merely for successful json serialization.*/
    public TocItemsVO() {
    }

    public TocItemsVO(List<TableOfContentItemVO> tocItemList,
            List<String> ancestorsIds, MessageHelper messageHelper) {
        tocItems = new ArrayList<TocItemVO>(tocItemList.size());
        for (TableOfContentItemVO tableOfContentItemVO : tocItemList) {
            TocItemVO tocItemVO = new TocItemVO(tableOfContentItemVO,
                    messageHelper);
            tocItems.add(tocItemVO);
        }
        this.ancestorsIds = ancestorsIds;
    }

    public List<TocItemVO> getTocItems() {
        return tocItems;
    }

    public void setTocItems(List<TocItemVO> tocItems) {
        this.tocItems = tocItems;
    }

    public List<String> getAncestorsIds() {
        return ancestorsIds;
    }

    public void setAncestorsIds(List<String> ancestorsIds) {
        this.ancestorsIds = ancestorsIds;
    }

}
