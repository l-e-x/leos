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

public abstract class TocItemType {
    protected String name;
    protected boolean isRoot;
    protected boolean draggable;
    protected boolean toBeDisplayed;

    public boolean isRoot() {
        return isRoot;
    }

    public boolean isDraggable() {
        return draggable;
    }

    public boolean isToBeDisplayed() {
        return toBeDisplayed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getNumHeadingSeparator();

    public abstract boolean areChildrenAllowed();

    @Override
    public boolean equals(Object tocItemType) {
        return (this.name.equals(((TocItemType)tocItemType).name));
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 17 + name.hashCode();
        return hash;
    }
}
