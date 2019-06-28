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
package eu.europa.ec.leos.services.compare;

import com.ximpleware.VTDNav;
import eu.europa.ec.leos.services.support.xml.vtd.Element;
import eu.europa.ec.leos.services.support.xml.vtd.IntHolder;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class ContentComparatorContext {

    private StringBuilder leftResultBuilder;
    private StringBuilder rightResultBuilder;
    private IntHolder modifications;
    private Integer indexOfOldElementInNewContent;
    private Integer indexOfNewElementInOldContent;
    private Element oldElement;
    private Element newElement;
    private Element intermediateElement;
    private String startTagAttrName;
    private String startTagAttrValue;
    private Boolean ignoreElements = Boolean.TRUE;
    private VTDNav oldContentNavigator;
    private Element oldContentRoot;
    private Map<String, Element> oldContentElements;
    private VTDNav newContentNavigator;
    private Element newContentRoot;
    private Map<String, Element> newContentElements;
    private VTDNav intermediateContentNavigator;
    private Element intermediateContentRoot;
    private Map<String, Element> intermediateContentElements;
    private StringBuilder resultBuilder;
    private Boolean ignoreRenumbering = Boolean.FALSE;
    private String attrName;
    private String removedValue;
    private String addedValue;
    private Boolean displayRemovedContentAsReadOnly = Boolean.FALSE;
    private Boolean threeWayDiff = Boolean.FALSE;
    private final String[] comparedVersions;

    private ContentComparatorContext(String[] comparedVersions) {
        this.comparedVersions = comparedVersions;
    }

    public ContentComparatorContext resetStartTagAttribute(){
        this.startTagAttrName = null;
        this.startTagAttrValue = null;
        return this;
    }

    public StringBuilder getLeftResultBuilder() {
        return leftResultBuilder;
    }

    public ContentComparatorContext setLeftResultBuilder(StringBuilder leftResultBuilder) {
        this.leftResultBuilder = leftResultBuilder;
        return this;
    }

    public StringBuilder getRightResultBuilder() {
        return rightResultBuilder;
    }

    public ContentComparatorContext setRightResultBuilder(StringBuilder rightResultBuilder) {
        this.rightResultBuilder = rightResultBuilder;
        return this;
    }

    public IntHolder getModifications() {
        return modifications;
    }

    public ContentComparatorContext setModifications(IntHolder modifications) {
        this.modifications = modifications;
        return this;
    }

    public Element getIntermediateElement() {
        return intermediateElement;
    }

    public ContentComparatorContext setIntermediateElement(Element intermediateElement) {
        this.intermediateElement = intermediateElement;
        return this;
    }

    public Integer getIndexOfOldElementInNewContent() {
        return indexOfOldElementInNewContent;
    }

    public ContentComparatorContext setIndexOfOldElementInNewContent(Integer indexOfOldElementInNewContent) {
        this.indexOfOldElementInNewContent = indexOfOldElementInNewContent;
        return this;
    }

    public Integer getIndexOfNewElementInOldContent() {
        return indexOfNewElementInOldContent;
    }

    public ContentComparatorContext setIndexOfNewElementInOldContent(Integer indexOfNewElementInOldContent) {
        this.indexOfNewElementInOldContent = indexOfNewElementInOldContent;
        return this;
    }

    public Element getOldElement() {
        return oldElement;
    }

    public ContentComparatorContext setOldElement(Element oldElement) {
        this.oldElement = oldElement;
        return this;
    }

    public Element getNewElement() {
        return newElement;
    }

    public ContentComparatorContext setNewElement(Element newElement) {
        this.newElement = newElement;
        return this;
    }

    public String getStartTagAttrName() {
        return startTagAttrName;
    }

    public ContentComparatorContext setStartTagAttrName(String startTagAttrName) {
        this.startTagAttrName = startTagAttrName;
        return this;
    }

    public String getStartTagAttrValue() {
        return startTagAttrValue;
    }

    public ContentComparatorContext setStartTagAttrValue(String startTagAttrValue) {
        this.startTagAttrValue = startTagAttrValue;
        return this;
    }

    public Boolean getIgnoreElements() {
        return ignoreElements;
    }

    public ContentComparatorContext setIgnoreElements(Boolean ignoreElements) {
        this.ignoreElements = ignoreElements;
        return this;
    }

    public VTDNav getOldContentNavigator() {
        return oldContentNavigator;
    }

    public ContentComparatorContext setOldContentNavigator(VTDNav oldContentNavigator) {
        this.oldContentNavigator = oldContentNavigator;
        return this;
    }

    public Element getOldContentRoot() {
        return oldContentRoot;
    }

    public ContentComparatorContext setOldContentRoot(Element oldContentRoot) {
        this.oldContentRoot = oldContentRoot;
        return this;
    }

    public Map<String, Element> getOldContentElements() {
        return oldContentElements;
    }

    public ContentComparatorContext setOldContentElements(Map<String, Element> oldContentElements) {
        this.oldContentElements = oldContentElements;
        return this;
    }

    public VTDNav getNewContentNavigator() {
        return newContentNavigator;
    }

    public ContentComparatorContext setNewContentNavigator(VTDNav newContentNavigator) {
        this.newContentNavigator = newContentNavigator;
        return this;
    }

    public Element getNewContentRoot() {
        return newContentRoot;
    }

    public ContentComparatorContext setNewContentRoot(Element newContentRoot) {
        this.newContentRoot = newContentRoot;
        return this;
    }
    public Element getIntermediateContentRoot() {
        return intermediateContentRoot;
    }

    public ContentComparatorContext setIntermediateContentRoot(Element intermediateContentRoot) {
        this.intermediateContentRoot = intermediateContentRoot;
        return this;
    }

    public Map<String, Element> getNewContentElements() {
        return newContentElements;
    }

    public ContentComparatorContext setNewContentElements(Map<String, Element> newContentElements) {
        this.newContentElements = newContentElements;
        return this;
    }

    public VTDNav getIntermediateContentNavigator() {
        return intermediateContentNavigator;
    }

    public ContentComparatorContext setIntermediateContentNavigator(VTDNav intermediateContentNavigator) {
        this.intermediateContentNavigator = intermediateContentNavigator;
        return this;
    }

    public Map<String, Element> getIntermediateContentElements() {
        return intermediateContentElements;
    }

    public ContentComparatorContext setIntermediateContentElements(Map<String, Element> intermediateContentElements) {
        this.intermediateContentElements = intermediateContentElements;
        return this;
    }

    public StringBuilder getResultBuilder() {
        return resultBuilder;
    }

    public ContentComparatorContext setResultBuilder(StringBuilder resultBuilder) {
        this.resultBuilder = resultBuilder;
        return this;
    }

    public Boolean getIgnoreRenumbering() {
        return ignoreRenumbering;
    }

    public ContentComparatorContext setIgnoreRenumbering(Boolean ignoreRenumbering) {
        this.ignoreRenumbering = ignoreRenumbering;
        return this;
    }

    public String getAttrName() {
        return attrName;
    }

    public ContentComparatorContext setAttrName(String attrName) {
        this.attrName = attrName;
        return this;
    }

    public String getRemovedValue() {
        return removedValue;
    }

    public ContentComparatorContext setRemovedValue(String removedValue) {
        this.removedValue = removedValue;
        return this;
    }

    public String getAddedValue() {
        return addedValue;
    }

    public ContentComparatorContext setAddedValue(String addedValue) {
        this.addedValue = addedValue;
        return this;
    }

    public Boolean getDisplayRemovedContentAsReadOnly() {
        return displayRemovedContentAsReadOnly;
    }

    public ContentComparatorContext setDisplayRemovedContentAsReadOnly(Boolean displayRemovedContentAsReadOnly) {
        this.displayRemovedContentAsReadOnly = displayRemovedContentAsReadOnly;
        return this;
    }

    public Boolean getThreeWayDiff() {
        return threeWayDiff;
    }

    public ContentComparatorContext setThreeWayDiff(Boolean threeWayDiff) {
        this.threeWayDiff = threeWayDiff;
        return this;
    }

    public String[] getComparedVersions() {
        return comparedVersions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ContentComparatorContext that = (ContentComparatorContext) o;
        return Objects.equals(leftResultBuilder.toString(), that.leftResultBuilder.toString()) &&
                Objects.equals(rightResultBuilder.toString(), that.rightResultBuilder.toString()) &&
                Objects.equals(modifications, that.modifications) &&
                Objects.equals(indexOfOldElementInNewContent, that.indexOfOldElementInNewContent) &&
                Objects.equals(indexOfNewElementInOldContent, that.indexOfNewElementInOldContent) &&
                Objects.equals(oldElement, that.oldElement) &&
                Objects.equals(newElement, that.newElement) &&
                Objects.equals(intermediateElement, that.intermediateElement) &&
                Objects.equals(startTagAttrName, that.startTagAttrName) &&
                Objects.equals(startTagAttrValue, that.startTagAttrValue) &&
                Objects.equals(ignoreElements, that.ignoreElements) &&
                Objects.equals(oldContentNavigator, that.oldContentNavigator) &&
                Objects.equals(oldContentRoot, that.oldContentRoot) &&
                Objects.equals(oldContentElements, that.oldContentElements) &&
                Objects.equals(newContentNavigator, that.newContentNavigator) &&
                Objects.equals(newContentRoot, that.newContentRoot) &&
                Objects.equals(newContentElements, that.newContentElements) &&
                Objects.equals(intermediateContentNavigator, that.intermediateContentNavigator) &&
                Objects.equals(intermediateContentRoot, that.intermediateContentRoot) &&
                Objects.equals(intermediateContentElements, that.intermediateContentElements) &&
                Objects.equals(resultBuilder.toString(), that.resultBuilder.toString()) &&
                Objects.equals(ignoreRenumbering, that.ignoreRenumbering) &&
                Objects.equals(attrName, that.attrName) &&
                Objects.equals(removedValue, that.removedValue) &&
                Objects.equals(addedValue, that.addedValue) &&
                Objects.equals(displayRemovedContentAsReadOnly, that.displayRemovedContentAsReadOnly) &&
                Objects.equals(threeWayDiff, that.threeWayDiff) &&
                Arrays.equals(comparedVersions, that.comparedVersions);
    }

    @Override
    public int hashCode() {
        int result = Objects
                .hash(leftResultBuilder.toString(), rightResultBuilder.toString(), modifications, indexOfOldElementInNewContent, indexOfNewElementInOldContent,
                        oldElement, newElement,
                        intermediateElement, startTagAttrName, startTagAttrValue, ignoreElements, oldContentNavigator, oldContentRoot, oldContentElements,
                        newContentNavigator, newContentRoot, newContentElements, intermediateContentNavigator, intermediateContentRoot,
                        intermediateContentElements,
                        resultBuilder.toString(), ignoreRenumbering, attrName, removedValue, addedValue, displayRemovedContentAsReadOnly, threeWayDiff);
        result = 31 * result + Arrays.hashCode(comparedVersions);
        return result;
    }

    @Override
    public String toString() {
        return "ContentComparatorContext{" +
                "leftResultBuilder=" + leftResultBuilder +
                ", rightResultBuilder=" + rightResultBuilder +
                ", modifications=" + modifications +
                ", indexOfOldElementInNewContent=" + indexOfOldElementInNewContent +
                ", indexOfNewElementInOldContent=" + indexOfNewElementInOldContent +
                ", oldElement=" + oldElement +
                ", newElement=" + newElement +
                ", intermediateElement=" + intermediateElement +
                ", startTagAttrName='" + startTagAttrName + '\'' +
                ", startTagAttrValue='" + startTagAttrValue + '\'' +
                ", ignoreElements=" + ignoreElements +
                ", oldContentNavigator=" + oldContentNavigator +
                ", oldContentRoot=" + oldContentRoot +
                ", oldContentElements=" + oldContentElements +
                ", newContentNavigator=" + newContentNavigator +
                ", newContentRoot=" + newContentRoot +
                ", newContentElements=" + newContentElements +
                ", intermediateContentNavigator=" + intermediateContentNavigator +
                ", intermediateContentRoot=" + intermediateContentRoot +
                ", intermediateContentElements=" + intermediateContentElements +
                ", resultBuilder=" + resultBuilder +
                ", ignoreRenumbering=" + ignoreRenumbering +
                ", attrName='" + attrName + '\'' +
                ", removedValue='" + removedValue + '\'' +
                ", addedValue='" + addedValue + '\'' +
                ", displayRemovedContentAsReadOnly=" + displayRemovedContentAsReadOnly +
                ", threeWayDiff=" + threeWayDiff +
                ", comparedVersions=" + Arrays.toString(comparedVersions) +
                '}';
    }

    public static class Builder {

        private StringBuilder leftResultBuilder;
        private StringBuilder rightResultBuilder;
        private IntHolder modifications;
        private Integer indexOfOldElementInNewContent;
        private Integer indexOfNewElementInOldContent;
        private Element oldElement;
        private Element newElement;
        private Element intermediateElement;
        private String startTagAttrName;
        private String startTagAttrValue;
        private Boolean ignoreElements = Boolean.TRUE;
        private VTDNav oldContentNavigator;
        private Element oldContentRoot;
        private Map<String, Element> oldContentElements;
        private VTDNav newContentNavigator;
        private Element newContentRoot;
        private Element intermediateContentRoot;
        private Map<String, Element> newContentElements;
        private VTDNav intermediateContentNavigator;
        private Map<String, Element> intermediateContentElements;
        private StringBuilder resultBuilder;
        private Boolean ignoreRenumbering = Boolean.FALSE;
        private String attrName;
        private String removedValue;
        private String addedValue;
        private Boolean displayRemovedContentAsReadOnly = Boolean.FALSE;
        private Boolean threeWayDiff = Boolean.FALSE;
        private final String[] comparedVersions;

        public Builder(ContentComparatorContext anotherContext) {
            this.leftResultBuilder = anotherContext.leftResultBuilder;
            this.rightResultBuilder = anotherContext.rightResultBuilder;
            this.modifications = anotherContext.modifications;
            this.indexOfOldElementInNewContent = anotherContext.indexOfOldElementInNewContent;
            this.indexOfNewElementInOldContent = anotherContext.indexOfNewElementInOldContent;
            this.oldElement = anotherContext.oldElement;
            this.newElement = anotherContext.newElement;
            this.intermediateElement = anotherContext.intermediateElement;
            this.startTagAttrName = anotherContext.startTagAttrName;
            this.startTagAttrValue = anotherContext.startTagAttrValue;
            this.ignoreElements = anotherContext.ignoreElements;
            this.oldContentNavigator = anotherContext.oldContentNavigator;
            this.oldContentRoot = anotherContext.oldContentRoot;
            this.oldContentElements = anotherContext.oldContentElements;
            this.newContentNavigator = anotherContext.newContentNavigator;
            this.newContentRoot = anotherContext.newContentRoot;
            this.newContentElements = anotherContext.newContentElements;
            this.intermediateContentNavigator = anotherContext.intermediateContentNavigator;
            this.intermediateContentRoot = anotherContext.intermediateContentRoot;
            this.intermediateContentElements = anotherContext.intermediateContentElements;
            this.resultBuilder = anotherContext.resultBuilder;
            this.ignoreRenumbering = anotherContext.ignoreRenumbering;
            this.attrName = anotherContext.attrName;
            this.removedValue = anotherContext.removedValue;
            this.addedValue = anotherContext.addedValue;
            this.displayRemovedContentAsReadOnly = anotherContext.displayRemovedContentAsReadOnly;
            this.threeWayDiff = anotherContext.threeWayDiff;
            this.comparedVersions = anotherContext.comparedVersions;
        }

        public Builder(String firstVersion, String lastVersion, String... intermediateVersions) {
            this.comparedVersions = new String[]{firstVersion, lastVersion, intermediateVersions.length > 0 ? intermediateVersions[0] : null};
        }

        public Builder withNoStartTagAttribute(){
            this.startTagAttrName = null;
            this.startTagAttrValue = null;
            return this;
        }

        public Builder withLeftResultBuilder(StringBuilder leftResultBuilder) {
            this.leftResultBuilder = leftResultBuilder;
            return this;
        }

        public Builder withRightResultBuilder(StringBuilder rightResultBuilder) {
            this.rightResultBuilder = rightResultBuilder;
            return this;
        }

        public Builder withModifications(IntHolder modifications) {
            this.modifications = modifications;
            return this;
        }

        public Builder withIndexOfOldElementInNewContent(Integer indexOfOldElementInNewContent) {
            this.indexOfOldElementInNewContent = indexOfOldElementInNewContent;
            return this;
        }

        public Builder withIndexOfNewElementInOldContent(Integer indexOfNewElementInOldContent) {
            this.indexOfNewElementInOldContent = indexOfNewElementInOldContent;
            return this;
        }

        public Builder withOldElement(Element oldElement) {
            this.oldElement = oldElement;
            return this;
        }

        public Builder withNewElement(Element newElement) {
            this.newElement = newElement;
            return this;
        }

        public Builder withIntermediateElement(Element intermediateElement) {
            this.intermediateElement = intermediateElement;
            return this;
        }

        public Builder withStartTagAttrName(String startTagAttrName) {
            this.startTagAttrName = startTagAttrName;
            return this;
        }

        public Builder withStartTagAttrValue(String startTagAttrValue) {
            this.startTagAttrValue = startTagAttrValue;
            return this;
        }

        public Builder withIgnoreElements(Boolean ignoreElements) {
            this.ignoreElements = ignoreElements;
            return this;
        }

        public Builder withOldContentNavigator(VTDNav oldContentNavigator) {
            this.oldContentNavigator = oldContentNavigator;
            return this;
        }

        public Builder withOldContentRoot(Element oldContentRoot) {
            this.oldContentRoot = oldContentRoot;
            return this;
        }

        public Builder withOldContentElements(Map<String, Element> oldContentElements) {
            this.oldContentElements = oldContentElements;
            return this;
        }

        public Builder withNewContentNavigator(VTDNav newContentNavigator) {
            this.newContentNavigator = newContentNavigator;
            return this;
        }

        public Builder withNewContentRoot(Element newContentRoot) {
            this.newContentRoot = newContentRoot;
            return this;
        }

        public Builder withIntermediateContentRoot(Element intermediateContentRoot) {
            this.intermediateContentRoot = intermediateContentRoot;
            return this;
        }

        public Builder withNewContentElements(Map<String, Element> newContentElements) {
            this.newContentElements = newContentElements;
            return this;
        }

        public Builder withIntermediateContentNavigator(VTDNav intermediateContentNavigator) {
            this.intermediateContentNavigator = intermediateContentNavigator;
            return this;
        }

        public Builder withIntermediateContentElements(Map<String, Element> intermediateContentElements) {
            this.intermediateContentElements = intermediateContentElements;
            return this;
        }

        public Builder withResultBuilder(StringBuilder resultBuilder) {
            this.resultBuilder = resultBuilder;
            return this;
        }

        public Builder withIgnoreRenumbering(Boolean ignoreRenumbering) {
            this.ignoreRenumbering = ignoreRenumbering;
            return this;
        }

        public Builder withAttrName(String attrName) {
            this.attrName = attrName;
            return this;
        }

        public Builder withRemovedValue(String removedValue) {
            this.removedValue = removedValue;
            return this;
        }

        public Builder withAddedValue(String addedValue) {
            this.addedValue = addedValue;
            return this;
        }

        public Builder withDisplayRemovedContentAsReadOnly(Boolean displayRemovedContentAsReadOnly) {
            this.displayRemovedContentAsReadOnly = displayRemovedContentAsReadOnly;
            return this;
        }

        public Builder withThreeWayDiff(Boolean threeWayDiff) {
            this.threeWayDiff = threeWayDiff;
            return this;
        }

        public ContentComparatorContext build() {
            ContentComparatorContext context = new ContentComparatorContext(this.comparedVersions);

            context.leftResultBuilder = this.leftResultBuilder;
            context.rightResultBuilder = this.rightResultBuilder;
            context.modifications = this.modifications;
            context.indexOfOldElementInNewContent = this.indexOfOldElementInNewContent;
            context.indexOfNewElementInOldContent = this.indexOfNewElementInOldContent;
            context.oldElement = this.oldElement;
            context.newElement = this.newElement;
            context.intermediateElement = this.intermediateElement;
            context.startTagAttrName = this.startTagAttrName;
            context.startTagAttrValue = this.startTagAttrValue;
            context.ignoreElements = this.ignoreElements;
            context.oldContentNavigator = this.oldContentNavigator;
            context.oldContentRoot = this.oldContentRoot;
            context.oldContentElements = this.oldContentElements;
            context.newContentNavigator = this.newContentNavigator;
            context.newContentRoot = this.newContentRoot;
            context.newContentElements = this.newContentElements;
            context.intermediateContentNavigator = this.intermediateContentNavigator;
            context.intermediateContentRoot = this.intermediateContentRoot;
            context.intermediateContentElements = this.intermediateContentElements;
            context.resultBuilder = this.resultBuilder;
            context.ignoreRenumbering = this.ignoreRenumbering;
            context.attrName = this.attrName;
            context.removedValue = this.removedValue;
            context.addedValue = this.addedValue;
            context.displayRemovedContentAsReadOnly = this.displayRemovedContentAsReadOnly;
            context.threeWayDiff = this.threeWayDiff;

            return context;
        }
    }
}
