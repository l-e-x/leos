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

import elemental.json.JsonException;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.i18n.MessageHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/** This file is almost a copy of file TableOfContentItemVO class as
 * 1. this contains additional fields
 * 3. keeping the two structures as this structure is used for client side only
 * 4. This structure is going to be extended soon
 */
public class TocItemVO extends TableOfContentItemVO implements Serializable {

    private static final long serialVersionUID = -3005288753552780530L;

    private String text;
    private List <TocItemVO> children;

    public TocItemVO(TableOfContentItemVO tableOfContentItemVO, MessageHelper messageHelper) {
        super(tableOfContentItemVO.getType(),
                tableOfContentItemVO.getId(),
                tableOfContentItemVO.getOriginAttr(),
                tableOfContentItemVO.getNumber(),
                tableOfContentItemVO.getOriginNumAttr(),
                tableOfContentItemVO.getHeading(),
                tableOfContentItemVO.getNumTagIndex(),
                tableOfContentItemVO.getHeadingTagIndex(),
                tableOfContentItemVO.getVtdIndex(),
                tableOfContentItemVO.getContent());

        this.setText(TableOfContentHelper.buildItemCaption(tableOfContentItemVO, TableOfContentHelper.DEFAULT_CAPTION_MAX_SIZE, messageHelper));
        this.setChildren( convertChildren(tableOfContentItemVO.getChildItemsView(),messageHelper));
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<TocItemVO> getChildren() {
        return children;
    }

    public void setChildren(List<TocItemVO> children) {
        this.children = children;
    }

    //helper methods to convert easily
    private List<TocItemVO> convertChildren(List<TableOfContentItemVO> tableOfContentItemVOList, MessageHelper messageHelper) throws JsonException {
        List<TocItemVO> tocItemListVOList = new ArrayList<TocItemVO>();
        for (TableOfContentItemVO tableOfContentItemVO: tableOfContentItemVOList) {
            TocItemVO tocItemVO = new TocItemVO(tableOfContentItemVO, messageHelper);
            tocItemListVOList.add(tocItemVO);
        }
        return tocItemListVOList;
    }

}
