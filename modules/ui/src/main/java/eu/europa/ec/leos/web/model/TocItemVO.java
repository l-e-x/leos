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

import elemental.json.JsonException;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(TocItemVO.class);

    private String text;
    private List <TocItemVO> children;

    public TocItemVO(){

    }

    public TocItemVO(TableOfContentItemVO tableOfContentItemVO, MessageHelper messageHelper) {
        super(tableOfContentItemVO.getType(),
                tableOfContentItemVO.getId(),
                tableOfContentItemVO.getNumber(),
                tableOfContentItemVO.getHeading(),
                tableOfContentItemVO.getNumTagIndex(),
                tableOfContentItemVO.getHeadingTagIndex(),
                tableOfContentItemVO.getVtdIndex());

        this.setText(TableOfContentItemConverter.buildItemDescription(tableOfContentItemVO.getNumber(), tableOfContentItemVO.getHeading(), tableOfContentItemVO.getType(), messageHelper));
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
