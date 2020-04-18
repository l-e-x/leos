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

import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.services.support.xml.vtd.Element;
import org.springframework.stereotype.Service;

import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.EMPTY_STRING;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.getElementFragmentAsString;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.updateElementAttribute;

@Service
@Instance(instances = {InstanceType.COMMISSION, InstanceType.OS})
public class ProposalXMLContentComparatorServiceImpl extends XMLContentComparatorServiceImpl {
    
    @Override
    protected  void appendRemovedElementsContent(ContentComparatorContext context) throws NavException {
        appendChangedElementsContent(Boolean.FALSE, context.getLeftResultBuilder(), context.getRightResultBuilder(), getElementFragmentAsString(context.getOldContentNavigator(), context.getOldElement()));
    }

    @Override
    protected void appendAddedElementsContent(ContentComparatorContext context) throws NavException {
        appendChangedElementsContent(Boolean.TRUE, context.getLeftResultBuilder(), context.getRightResultBuilder(), getElementFragmentAsString(context.getNewContentNavigator(), context.getNewElement()));
    }

    @Override
    protected Boolean shouldDisplayRemovedContent(Element elementOldContent, int indexOfOldElementInNewContent) {
        return isElementRemovedFromContent(indexOfOldElementInNewContent);
    }

    @Override
    protected Boolean containsIgnoredElements(String content){
        return Boolean.FALSE;
    }

    @Override
    protected Boolean containsAddedNonIgnoredElements(String content){
        return Boolean.FALSE;
    }

    @Override
    protected Boolean shouldIgnoreElement(Element element){
        return element == null;
    }

    @Override
    protected Boolean isElementInItsOriginalPosition(Element element){
        return Boolean.TRUE;
    }

    @Override
    protected Boolean shouldCompareElements(Element oldElement, Element newElement) {
        return Boolean.TRUE;
    }

    @Override
    protected Boolean shouldIgnoreRenumbering(Element element) {
        return Boolean.FALSE;
    }

    @Override
    protected Boolean isActionRoot(Element element) {
        return Boolean.FALSE;
    }

    @Override
    protected final StringBuilder buildStartTagForAddedElement(Element newElement, Element oldElement, ContentComparatorContext context) {
        return buildStartTag(newElement);
    }

    @Override
    protected final String getStartTagValueForAddedElement(Element newElement, Element oldElement, ContentComparatorContext context){
        return EMPTY_STRING;
    }

    @Override
    protected final StringBuilder buildStartTagForRemovedElement(Element newElement, ContentComparatorContext context) {
        return buildStartTag(newElement);
    }

    @Override
    protected String getChangedElementContent(VTDNav contentNavigator, Element element, String attrName, String attrValue) throws NavException {
        return updateElementAttribute(getElementFragmentAsString(contentNavigator, element), attrName, attrValue);
    }

    @Override
    protected void appendAddedElementContent(ContentComparatorContext context) throws NavException {
        context.getResultBuilder().append(getChangedElementContent(context.getNewContentNavigator(), context.getNewElement(), context.getAttrName(), context.getAddedValue()));
    }

    @Override
    protected void appendRemovedElementContent(ContentComparatorContext context) throws NavException {
        context.getResultBuilder().append(getChangedElementContent(context.getOldContentNavigator(), context.getOldElement(), context.getAttrName(), context.getRemovedValue()));
    }

    @Override
    protected TextComparator getTextComparator() {
        return new ProposalTextComparator();
    }
}
