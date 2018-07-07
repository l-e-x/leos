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

public class LegalTextTocItemType extends TocItemType {
    private static final ArrayList<LegalTextTocItemType> values = new ArrayList();

    public static final LegalTextTocItemType PREFACE =
            new LegalTextTocItemType("PREFACE", true, false);
    public static final LegalTextTocItemType TITLE =
            new LegalTextTocItemType("TITLE", false, true);
    public static final LegalTextTocItemType BODY =
            new LegalTextTocItemType("BODY", true, false);
    public static final LegalTextTocItemType PREAMBLE =
            new LegalTextTocItemType("PREAMBLE", true, false);
    public static final LegalTextTocItemType PART =
            new LegalTextTocItemType("PART", false, true);
    public static final LegalTextTocItemType CHAPTER =
            new LegalTextTocItemType("CHAPTER", false, true);
    public static final LegalTextTocItemType SECTION =
            new LegalTextTocItemType("SECTION", false, true);
    public static final LegalTextTocItemType SUBSECTION =
            new LegalTextTocItemType("SUBSECTION", false, true);
    public static final LegalTextTocItemType ARTICLE =
            new LegalTextTocItemType("ARTICLE", false, true);
    public static final LegalTextTocItemType CITATIONS =
            new LegalTextTocItemType("CITATIONS", false, false);
    public static final LegalTextTocItemType RECITALS =
            new LegalTextTocItemType("RECITALS", false, false);
    public static final LegalTextTocItemType CONCLUSIONS =
            new LegalTextTocItemType("CONCLUSIONS", true, true);

    private String numHeadingSeparator = " - ";

    private LegalTextTocItemType(String name, boolean isRoot, boolean draggable) {
        this.name = name;
        this.isRoot = isRoot;
        this.draggable = draggable;
        this.toBeDisplayed = true;
        values.add(this);
    }

    public static TocItemType[] getValues() {
        return values.toArray(new LegalTextTocItemType[values.size()]);
    }

    public static TocItemType getTocItemTypeFromName(String name) {
        for (TocItemType type : values) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    public String getNumHeadingSeparator() {
        return numHeadingSeparator;
    }

    @Override
    public boolean areChildrenAllowed() {
        if (this.equals(ARTICLE) || this.equals(CITATIONS) ||
                this.equals(RECITALS) || (this.isRoot()
                && !(this.equals(BODY) || this.equals(PREAMBLE)))) {
            return false;
        }
        return true;
    }

}
