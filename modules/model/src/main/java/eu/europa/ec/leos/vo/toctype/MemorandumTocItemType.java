/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.vo.toctype;

import java.util.ArrayList;

public class MemorandumTocItemType extends TocItemType {
    private static final ArrayList<MemorandumTocItemType> values = new ArrayList();

    public static final MemorandumTocItemType PREFACE =
            new MemorandumTocItemType("PREFACE", true, false);
    public static final MemorandumTocItemType TITLE =
            new MemorandumTocItemType("TITLE", false, true);
    public static final MemorandumTocItemType MAINBODY =
            new MemorandumTocItemType("MAINBODY", true, false, false);
    public static final MemorandumTocItemType TBLOCK =
            new MemorandumTocItemType("TBLOCK", false, false);

     private MemorandumTocItemType(String name, boolean isRoot, boolean draggable, boolean toBeDisplayed) {
        this.name = name;
        this.isRoot = isRoot;
        this.draggable = draggable;
        this.toBeDisplayed = toBeDisplayed;
        values.add(this);
    }

    private MemorandumTocItemType(String name, boolean isRoot, boolean draggable) {
        this.name = name;
        this.isRoot = isRoot;
        this.draggable = draggable;
        this.toBeDisplayed = true;
        values.add(this);
    }

    public static TocItemType[] getValues() {
        return values.toArray(new MemorandumTocItemType[values.size()]);
    }

    public static TocItemType getTocItemTypeFromName(String name) {
        for (TocItemType type : values) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public boolean areChildrenAllowed() {
        return !this.equals(PREFACE);
    }

    @Override
    public String getNumHeadingSeparator() {
        return " ";
    }

}
