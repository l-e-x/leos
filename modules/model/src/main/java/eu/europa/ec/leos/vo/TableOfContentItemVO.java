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
package eu.europa.ec.leos.vo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableOfContentItemVO {

    public static enum Type {
        PREFACE(true),
        PREAMBLE(true),
        BODY(true),
        PART(false),
        TITLE(false),
        CHAPTER(false),
        SECTION(false),
        SUBSECTION(false),
        ARTICLE(false),
        CONCLUSIONS(true);

        private boolean isRoot;

        Type(boolean isRoot) {
            this.isRoot = isRoot;
        }

        public boolean isRoot() {
            return isRoot;
        }

        public static Type forName(String name) {
            for (Type type : Type.values()) {
                if (type.name().equalsIgnoreCase(name)) {
                    return type;
                }
            }
            return null;
        }
    }

    private final Type type;
    private final String id;

    private final String number;
    private final String heading;

    private final Integer numTagIndex;
    private final Integer headingTagIndex;
    private final Integer vtdIndex;

    private final List<TableOfContentItemVO> childItems = new ArrayList<>();
    private TableOfContentItemVO parentItem;

    public TableOfContentItemVO(Type type, String id, String number, String heading, Integer numTagIndex, Integer headingTagIndex, Integer vtdIndex) {
        this.type = type;
        this.id = id;
        this.number = number;
        this.heading = heading;
        this.numTagIndex = numTagIndex;
        this.headingTagIndex = headingTagIndex;
        this.vtdIndex = vtdIndex;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public String getHeading() {
        return heading;
    }

    public Integer getNumTagIndex() {
        return numTagIndex;
    }

    public Integer getHeadingTagIndex() {
        return headingTagIndex;
    }

    public Integer getVtdIndex() {
        return vtdIndex;
    }

    public TableOfContentItemVO getParentItem() {
        return parentItem;
    }

    public void addChildItem(TableOfContentItemVO tableOfContentItemVO) {
        if (tableOfContentItemVO.getType().isRoot()) {
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

    public boolean areChildrenAllowed() {
        if (type.equals(TableOfContentItemVO.Type.ARTICLE) ||
                (type.isRoot() && !type.equals(TableOfContentItemVO.Type.BODY))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableOfContentItemVO that = (TableOfContentItemVO) o;

        if (heading != null ? !heading.equals(that.heading) : that.heading != null) return false;
        if (headingTagIndex != null ? !headingTagIndex.equals(that.headingTagIndex) : that.headingTagIndex != null)
            return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (numTagIndex != null ? !numTagIndex.equals(that.numTagIndex) : that.numTagIndex != null) return false;
        if (number != null ? !number.equals(that.number) : that.number != null) return false;
        if (type != that.type) return false;
        if (vtdIndex != null ? !vtdIndex.equals(that.vtdIndex) : that.vtdIndex != null) return false;
        if (childItems != null ? !childItems.equals(that.childItems) : that.childItems != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (heading != null ? heading.hashCode() : 0);
        result = 31 * result + (numTagIndex != null ? numTagIndex.hashCode() : 0);
        result = 31 * result + (headingTagIndex != null ? headingTagIndex.hashCode() : 0);
        result = 31 * result + (vtdIndex != null ? vtdIndex.hashCode() : 0);
        result = 31 * result + (childItems != null ? childItems.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TableOfContentItemVO{");
        sb.append("type=").append(type);
        sb.append(", id='").append(id).append('\'');
        sb.append(", number='").append(number).append('\'');
        sb.append(", heading='").append(heading).append('\'');
        sb.append(", numTagIndex=").append(numTagIndex);
        sb.append(", headingTagIndex=").append(headingTagIndex);
        sb.append(", vtdIndex=").append(vtdIndex);
        sb.append(", childItems=").append(childItems);
        sb.append('}');
        return sb.toString();
    }
}
