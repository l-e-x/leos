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
package eu.europa.ec.leos.vo.toc;

import eu.europa.ec.leos.model.action.SoftActionType;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Stream;

public class TableOfContentItemVO implements Serializable {
    
    public static final long serialVersionUID = -1;

    private TocItem tocItem;
    private String id;
    private String originAttr;
    private String number;
    private String originNumAttr;
    private String heading;
    private String content;
    private Integer numTagIndex;
    private Integer headingTagIndex;
    private Integer introTagIndex;
    private Integer vtdIndex;
    private Integer listTagIndex;
    private String list;
    private boolean movedOnEmptyParent;
    private boolean undeleted;

    private final List<TableOfContentItemVO> childItems = new ArrayList<>();
    private TableOfContentItemVO parentItem;
    private SoftActionType softActionAttr;
    private Boolean isSoftActionRoot;
    private String softMoveTo;
    private String softMoveFrom;
    private String softUserAttr;
    private GregorianCalendar softDateAttr;
    private final List<CoEditionVO> coEditionVos = new ArrayList<>();
    private boolean isAffected;
    private Boolean isNumberingToggled;
    private SoftActionType numSoftActionAttr;
    private Boolean restored;
    private int itemDepth;

    public TableOfContentItemVO(TocItem tocItem, String id, String originAttr, String number, String originNumAttr, String heading, Integer numTagIndex,
            Integer headingTagIndex, Integer introTagIndex, Integer vtdIndex, String content) {
        this.tocItem = tocItem;
        this.id = id;
        this.originAttr = originAttr;
        this.number = number;
        this.originNumAttr = originNumAttr;
        this.heading = heading;
        this.numTagIndex = numTagIndex;
        this.headingTagIndex = headingTagIndex;
        this.introTagIndex = introTagIndex;
        this.vtdIndex = vtdIndex;
        this.content = content;
        this.isAffected = false;
        this.itemDepth = 0;
    }

    public TableOfContentItemVO(TocItem tocItem, String id, String originAttr, String number, String originNumAttr, String heading, Integer numTagIndex,
            Integer headingTagIndex, Integer introTagIndex, Integer vtdIndex, String list, Integer listTagIndex, String content, SoftActionType softActionAttr, Boolean isSoftActionRoot, String softUserAttr, GregorianCalendar softDateAttr) {
        this(tocItem, id, originAttr, number, originNumAttr, heading, numTagIndex, headingTagIndex, introTagIndex, vtdIndex, content);
        this.list = list;
        this.listTagIndex = listTagIndex;
        this.softActionAttr = softActionAttr;
        this.isSoftActionRoot = isSoftActionRoot;
        this.softUserAttr = softUserAttr;
        this.softDateAttr = softDateAttr;
    }

    public TableOfContentItemVO(TocItem tocItem, String id, String originAttr, String number, String originNumAttr, String heading, Integer numTagIndex,
            Integer headingTagIndex, Integer introTagIndex, Integer vtdIndex, String list, Integer listTagIndex, String content, SoftActionType softActionAttr, Boolean isSoftActionRoot, String softUserAttr, GregorianCalendar softDateAttr,
            String softMoveFrom, String softMoveTo, boolean undeleted,SoftActionType numSoftActionAttr) {
        this(tocItem, id, originAttr, number, originNumAttr, heading, numTagIndex, headingTagIndex, introTagIndex, vtdIndex,
                list, listTagIndex, content, softActionAttr, isSoftActionRoot, softUserAttr, softDateAttr);
        this.softMoveFrom = softMoveFrom;
        this.softMoveTo = softMoveTo;
        this.undeleted = undeleted;
        this.numSoftActionAttr = numSoftActionAttr;
    }
    
    public TableOfContentItemVO(TocItem tocItem, String id, String originAttr, String number, String originNumAttr, String heading, Integer numTagIndex,
            Integer headingTagIndex, Integer introTagIndex, Integer vtdIndex, String list, Integer listTagIndex, String content, SoftActionType softActionAttr, Boolean isSoftActionRoot, String softUserAttr, GregorianCalendar softDateAttr,
            String softMoveFrom, String softMoveTo, boolean undeleted,SoftActionType numSoftActionAttr, int itemDepth) {
        this(tocItem, id, originAttr, number, originNumAttr, heading, numTagIndex, headingTagIndex, introTagIndex, vtdIndex,
                list, listTagIndex, content, softActionAttr, isSoftActionRoot, softUserAttr, softDateAttr);
        this.softMoveFrom = softMoveFrom;
        this.softMoveTo = softMoveTo;
        this.undeleted = undeleted;
        this.numSoftActionAttr = numSoftActionAttr;
        this.itemDepth = itemDepth;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public Integer getNumTagIndex() {
        return numTagIndex;
    }

    public void setNumTagIndex(Integer numTagIndex) {
        this.numTagIndex = numTagIndex;
    }

    public Integer getHeadingTagIndex() {
        return headingTagIndex;
    }

    public void setHeadingTagIndex(Integer headingTagIndex) {
        this.headingTagIndex = headingTagIndex;
    }

    public String getId() {
        return id;
    }

    public String getOriginAttr() {
        return originAttr;
    }

    public void setOriginAttr(String originAttr) {
        this.originAttr = originAttr;
    }

    public String getOriginNumAttr() {
        return originNumAttr;
    }

    public void setOriginNumAttr(String originNumAttr) {
        this.originNumAttr = originNumAttr;
    }

    public TocItem getTocItem() {
        return tocItem;
    }

    public void setTocItem(TocItem tocItem) {
        this.tocItem = tocItem;
    }

    public Integer getVtdIndex() {
        return vtdIndex;
    }

    public Integer getIntroTagIndex() {
        return introTagIndex;
    }

    public String getList() {
        return list;
    }
    
    public Integer getListTagIndex() {
        return listTagIndex;
    }
    
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isAffected() {
        return isAffected;
    }

    public void setAffected(boolean affected) {
        isAffected = affected;
    }

    /**
     * @return the softActionAttr
     */
    public SoftActionType getSoftActionAttr() {
        return softActionAttr;
    }

    /**
     * @param softActionAttr the softActionAttr to set
     */
    public void setSoftActionAttr(SoftActionType softActionAttr) {
        this.softActionAttr = softActionAttr;
    }

    public Boolean isSoftActionRoot() {
        return isSoftActionRoot;
    }

    public void setSoftActionRoot(Boolean softActionRoot) {
        isSoftActionRoot = softActionRoot;
    }

    /**
     * @return the softUserAttr
     */
    public String getSoftUserAttr() {
        return softUserAttr;
    }

    /**
     * @param softUserAttr the softUserAttr to set
     */
    public void setSoftUserAttr(String softUserAttr) {
        this.softUserAttr = softUserAttr;
    }

    /**
     * @return the softDateAttr
     */
    public GregorianCalendar getSoftDateAttr() {
        return softDateAttr;
    }

    /**
     * @param softDateAttr the softDateAttr to set
     */
    public void setSoftDateAttr(GregorianCalendar softDateAttr) {
        this.softDateAttr = softDateAttr;
    }

    /**
     * @return the softMoveTo
     */
    public String getSoftMoveTo() {
        return softMoveTo;
    }

    /**
     * @param softMoveTo the softMoveTo to set
     */
    public void setSoftMoveTo(String softMoveTo) {
        this.softMoveTo = softMoveTo;
    }

    /**
     * @return the softMoveFrom
     */
    public String getSoftMoveFrom() {
        return softMoveFrom;
    }

    /**
     * @param softMoveFrom the softMoveFrom to set
     */
    public void setSoftMoveFrom(String softMoveFrom) {
        this.softMoveFrom = softMoveFrom;
    }

    public TableOfContentItemVO getParentItem() {
        return parentItem;
    }

    public void setParentItem(TableOfContentItemVO parentItem) {
        this.parentItem = parentItem;
    }

    public List<TableOfContentItemVO> getChildItems() {
        return childItems;
    }

    public boolean isMovedOnEmptyParent() {
        return movedOnEmptyParent;
    }

    public void setMovedOnEmptyParent(boolean movedOnEmptyParent) {
        this.movedOnEmptyParent = movedOnEmptyParent;
    }

    public Boolean isNumberingToggled() {
        return isNumberingToggled;
    }

    public void setNumberingToggled(Boolean numberingToggled) {
        this.isNumberingToggled = numberingToggled;
    }

    public Boolean isRestored() {
        return restored;
    }

    public void setRestored(Boolean restored) {
        this.restored = restored;
    }

    public void addChildItem(TableOfContentItemVO tableOfContentItemVO) {
        if (tableOfContentItemVO.getTocItem().isRoot()) {
            throw new IllegalArgumentException("Cannot add a root item as a child!");
        }
        childItems.add(tableOfContentItemVO);
        tableOfContentItemVO.parentItem = this;
    }

    public void addAllChildItems(List<TableOfContentItemVO> tableOfContentItemVOList) {
        for (TableOfContentItemVO item : tableOfContentItemVOList) {
            addChildItem(item);
        }
    }

    public void removeChildItem(TableOfContentItemVO tableOfContentItemVO) {
        childItems.remove(tableOfContentItemVO);
    }

    public void removeAllChildItems() {
        childItems.clear();
    }

    public List<TableOfContentItemVO> getChildItemsView() {
        return Collections.unmodifiableList(new ArrayList<>(childItems));
    }
    
    public boolean containsItem(String aknTag) {
        List<TableOfContentItemVO> childItems = this.childItems;
        for(TableOfContentItemVO child : childItems) {
            if(child.getTocItem().getAknTag().value().equals(aknTag)) {
                return true;
            }
        }
        return false;
    }
    
    public List<CoEditionVO> getCoEditionVos() {
        return coEditionVos;
    }
    
    public void addUserCoEdition(CoEditionVO coEditionVO) {
        coEditionVos.add(coEditionVO);
    }
    
    public void removeAllUserCoEdition() {
        coEditionVos.clear();
    }
    
    public boolean isUndeleted() {
        return undeleted;
    }
    
    public void setUndeleted(boolean undeleted) {
        this.undeleted = undeleted;
    }
    
    public int getItemDepth() {
        return itemDepth;
    }

    public void setItemDepth(int depth) {
        this.itemDepth = depth;
    }

    public SoftActionType  getNumSoftActionAttr() {
        return numSoftActionAttr;
    }

    public void setNumSoftActionAttr(SoftActionType numSoftActionAttr ) {
        this.numSoftActionAttr = numSoftActionAttr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableOfContentItemVO that = (TableOfContentItemVO) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (tocItem != that.tocItem) return false;
        if (vtdIndex != null ? !vtdIndex.equals(that.vtdIndex) : that.vtdIndex != null) return false;

        return true;
    }
    
    @Override
    public int hashCode() {
        int result = tocItem != null ? tocItem.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (vtdIndex != null ? vtdIndex.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TableOfContentItemVO{");
        sb.append("tocItem=").append(tocItem);
        sb.append(", xml:id='").append(id).append('\'');
        sb.append(", number='").append(number).append('\'');
        sb.append(", heading='").append(heading).append('\'');
        sb.append(", numTagIndex=").append(numTagIndex);
        sb.append(", headingTagIndex=").append(headingTagIndex);
        sb.append(", vtdIndex=").append(vtdIndex);
        sb.append(", childItems=").append(childItems);
        sb.append('}');
        return sb.toString();
    }

    public Stream<TableOfContentItemVO> flattened() {
        return Stream.concat(
                Stream.of(this),
                childItems.stream().flatMap(TableOfContentItemVO::flattened)
        );
    }

}
