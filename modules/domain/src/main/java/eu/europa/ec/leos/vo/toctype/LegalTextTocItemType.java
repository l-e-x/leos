/*
 * Copyright 2018 European Commission
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


public enum LegalTextTocItemType implements TocItemType {
    
   PREFACE("preface", true, false, false, false, false, true, false),
   BODY("body", true, false,false,false, true, true, false),
   PREAMBLE("preamble", true, false,false,false, true, true, false),
   PART ("part", false, true,true,true, true, true, true),
   TITLE ("title", false, true,true,true, true, true, true),
   CHAPTER ("chapter", false, true,true,true, true, true, true),
   SECTION("section", false, true,true,true, true, true, true),
   ARTICLE ("article", false, true, true, true,false, true, false),
   CITATIONS("citations", false, false,false,false, false, true, false),
   RECITALS ("recitals", false, false,false,false, false, true, false),
   CONCLUSIONS("conclusions", true, true,false,false, false, true, false);
    
    protected String name;
    protected boolean isRoot;
    protected boolean draggable;
    protected boolean toBeDisplayed;
    protected boolean hasItemNumber;
    protected boolean hasItemHeading;
    protected boolean childrenAllowed;
    protected boolean hasItemDescription;
    protected boolean isNumberEditable;

    private final String NUM_HEADING_SEPERATOR = " - ";

    LegalTextTocItemType(String name, boolean isRoot, boolean draggable, boolean hasItemNumber,boolean hasItemHeading, boolean childrenAllowed, boolean hasItemDescription, boolean isNumberEditable) {
        this.name = name;
        this.isRoot = isRoot;
        this.draggable = draggable;
        this.toBeDisplayed = true;
        this.hasItemNumber = hasItemNumber;
        this.hasItemHeading = hasItemHeading;
        this.childrenAllowed = childrenAllowed;
        this.hasItemDescription = hasItemDescription;
        this.isNumberEditable = isNumberEditable;
    }
    

    public static TocItemType[] getValues() {
        return LegalTextTocItemType.values();
    }

    public static TocItemType getTocItemTypeFromName(String name) {
        for (TocItemType type : LegalTextTocItemType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String getNumHeadingSeparator() {
        return NUM_HEADING_SEPERATOR;
    }

    @Override
    public boolean areChildrenAllowed() {
        return childrenAllowed;
    }

	@Override
	public boolean isRoot() {
		return isRoot;
	}

	@Override
	public boolean isDraggable() {
		return draggable;
	}

	@Override
	public boolean isToBeDisplayed() {
		return toBeDisplayed;
	}

	@Override
	public boolean hasItemNumber() {
		return hasItemNumber;
	}

	@Override
	public boolean hasItemHeading() {
		return hasItemHeading;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
		
	}

	@Override
	public boolean hasItemDescription() {
		return hasItemDescription;
	}


    @Override
    public boolean isNumberEditable() {
        return isNumberEditable;
    }

}