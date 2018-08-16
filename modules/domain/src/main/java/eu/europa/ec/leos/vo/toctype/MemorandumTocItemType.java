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


public enum MemorandumTocItemType implements TocItemType {
    

    PREFACE ("PREFACE", true, false,true,false,false,false, true, false),
    TITLE("TITLE", false, true,true,false,false,true, true, false),
    MAINBODY ("MAINBODY", true, false, false,false,false,true, true, false),
    TBLOCK ("TBLOCK", false, false,true,false,false,true, false, false);

    protected String name;
    protected boolean isRoot;
    protected boolean draggable;
    protected boolean toBeDisplayed;
    protected boolean hasItemNumber;
    protected boolean hasItemHeading;
    protected boolean childrenAllowed;
    protected boolean hasItemDescription;
    protected boolean isNumberEditable;
    
    private final String NUM_HEADING_SEPERATOR = " ";

     MemorandumTocItemType(String name, boolean isRoot, boolean draggable, boolean toBeDisplayed,boolean hasItemNumber, boolean hasItemHeading, boolean childrenAllowed,boolean hasItemDescription, boolean isNumberEditable) {
        this.name = name;
        this.isRoot = isRoot;
        this.draggable = draggable;
        this.toBeDisplayed = toBeDisplayed;
        this.hasItemNumber = hasItemNumber;
        this.hasItemHeading = hasItemHeading;
        this.childrenAllowed = childrenAllowed;
        this.hasItemDescription = hasItemDescription;
        this.isNumberEditable = isNumberEditable;
    }

    public static TocItemType[] getValues() {
        return MemorandumTocItemType.values();
    }

    public static TocItemType getTocItemTypeFromName(String name) {
        for (TocItemType type : MemorandumTocItemType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    @Override
    public boolean areChildrenAllowed() {
        return childrenAllowed;
    }

    @Override
    public String getNumHeadingSeparator() {
        return NUM_HEADING_SEPERATOR;
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
