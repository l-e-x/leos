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

import com.google.common.base.Stopwatch;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;
import eu.europa.ec.leos.services.support.xml.vtd.Element;
import eu.europa.ec.leos.services.support.xml.vtd.IntHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_DELETABLE_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.LEOS_EDITABLE_ATTR;
import static eu.europa.ec.leos.services.support.xml.VTDUtils.updateAttributeValue;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.buildElement;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.buildXMLNavigator;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.getContentFragmentAsString;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.getElementFragmentAsString;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.isElementContentEqual;
import static eu.europa.ec.leos.services.support.xml.vtd.VTDHelper.updateElementAttribute;

public abstract class XMLContentComparatorServiceImpl implements ContentComparatorService {

    private final String BLOCK_MODIFIED_LINE_START_TAG = "<span class=\"" + CONTENT_BLOCK_MODIFIED_CLASS + "\">";
    private final String MODIFIED_LINE_MARKER_PART_1 = "<input type=\"hidden\" name=\"modification_";
    private final String MODIFIED_LINE_MARKER_PART_2 = "\"/>";
    private static final String END_TAG = "</span>";
    
    private static final Logger LOG = LoggerFactory.getLogger(XMLContentComparatorServiceImpl.class);

    @Override
    public String compareContents(ContentComparatorContext context) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        context.setResultBuilder(new StringBuilder());

        try {
            context.setOldContentNavigator(buildXMLNavigator(context.getComparedVersions()[0]))
                .setNewContentNavigator(buildXMLNavigator(context.getComparedVersions()[1]));

            //start from the akn-bill element
            context.getNewContentNavigator().toElement(VTDNav.ROOT);//akomaNtoso
            int elementNewContentIndex = context.getNewContentNavigator().getCurrentIndex();

            //start from the akn-bill element
            context.getOldContentNavigator().toElement(VTDNav.ROOT);//akomaNtoso

            //get the offset of the content of the bill element and add all the content until there
            int offsetBegin = (int) context.getNewContentNavigator().getContentFragment();
            context.getResultBuilder().append(new String(context.getNewContentNavigator().getXML().getBytes(0, offsetBegin), Charset.forName("UTF-8")));

            context.setNewContentElements(new HashMap<>())
                .setOldContentElements(new HashMap<>())
                .setOldContentRoot(buildElement(context.getOldContentNavigator(), context.getOldContentNavigator().getCurrentIndex(),
                    new HashMap<>(), context.getOldContentElements()))
                .setNewContentRoot(buildElement(context.getNewContentNavigator(), context.getNewContentNavigator().getCurrentIndex(),
                        new HashMap<>(), context.getNewContentElements()));

            if (context.getThreeWayDiff()) {
                context.setIntermediateContentNavigator(buildXMLNavigator(context.getComparedVersions()[2]))
                    .setIntermediateContentElements(new HashMap<>());
                context.getIntermediateContentNavigator().toElement(VTDNav.ROOT);
                context.setIntermediateContentRoot(buildElement(context.getIntermediateContentNavigator(), context.getIntermediateContentNavigator().getCurrentIndex(),
                        new HashMap<>(), context.getIntermediateContentElements()));
            }

            computeDifferencesAtNodeLevel(context);

            //add all the content as of the end of the content of the bill element until the end of this XML
            context.getNewContentNavigator().recoverNode(elementNewContentIndex);
            int offsetEnd = (int) (context.getNewContentNavigator().getContentFragment()) + (int) (context.getNewContentNavigator().getContentFragment() >> 32);
            int lengthEnd = context.getNewContentNavigator().getXML().length();
            lengthEnd -= offsetEnd;
            context.getResultBuilder().append(new String(context.getNewContentNavigator().getXML().getBytes(offsetEnd, lengthEnd), Charset.forName("UTF-8")));

        } catch (Exception e) {
            LOG.error("Error occured in comparison: ", e);
        }
        LOG.trace("Comparison finished!  ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return context.getResultBuilder().toString();
    }

    private void computeDifferencesAtNodeLevel(ContentComparatorContext context) throws NavException {

        if (shouldIgnoreElement(context.getOldContentRoot())) {
            return;
        }

        int oldContentChildIndex = 0; // current index in oldContentRoot children list
        int newContentChildIndex = 0; // current index in newContentRoot children list

        while (oldContentChildIndex < context.getOldContentRoot().getChildren().size()
                && newContentChildIndex < context.getNewContentRoot().getChildren().size()) {

            context.setOldElement(context.getOldContentRoot().getChildren().get(oldContentChildIndex))
                .setNewElement(context.getNewContentRoot().getChildren().get(newContentChildIndex))
                .setIndexOfOldElementInNewContent(getBestMatchInList(context.getNewContentRoot().getChildren(), context.getOldElement()))
                .setIndexOfNewElementInOldContent(getBestMatchInList(context.getOldContentRoot().getChildren(), context.getNewElement()));

            // at each step, check for a particular structural change in this order
            if (shouldIgnoreElement(context.getNewElement())) {
               newContentChildIndex++;
            } else if (shouldIgnoreElement(context.getOldElement())) {
                oldContentChildIndex++;
            } else if (newContentChildIndex == context.getIndexOfOldElementInNewContent()
                    && (!context.getDisplayRemovedContentAsReadOnly()
                        || shouldCompareElements(context.getOldElement(), context.getNewElement())
                            && shouldCompareElements(context.getNewElement(), context.getOldElement()))) {
                // element did not changed relative position so check if it's content is changed and should be compared

                compareElementContents(context);

                oldContentChildIndex++;
                newContentChildIndex++;
            } else if (context.getIndexOfNewElementInOldContent() < 0 && context.getIndexOfOldElementInNewContent() < 0) {
                // oldElement was completely replaced with newElement
                appendRemovedElementContentIfRequired(context);
                appendAddedElementContentIfRequired(context);
                oldContentChildIndex++;
                newContentChildIndex++;
            } else if (context.getIndexOfNewElementInOldContent() >= oldContentChildIndex && context.getIndexOfOldElementInNewContent() > newContentChildIndex) {
                // newElement appears to be moved backward to newContentChildIndex and oldElement appears to be moved forward from oldContentChildIndex
                // at the same time
                // so display the element that was moved more positions because it's more likely to be the action the user actually made
                if ((context.getIndexOfNewElementInOldContent() - oldContentChildIndex > context.getIndexOfOldElementInNewContent() - newContentChildIndex)
                        || context.getDisplayRemovedContentAsReadOnly() && !shouldCompareElements(context.getOldElement(), context.getNewElement())){
                    // newElement was moved backward to newContentChildIndex more positions than oldElement was moved forward from oldContentChildIndex
                    // or the newElement should not be compared with the oldElement
                    // so display the added newElement in the new location for now
                    appendAddedElementContentIfRequired(context);
                    newContentChildIndex++;
                } else {
                    // oldElement was moved forward from oldContentChildIndex more or just as many positions as newElement was moved backward to newContentChildIndex
                    // so display the removed oldElement in the original location for now
                    appendRemovedElementContentIfRequired(context);
                    oldContentChildIndex++;
                }
            } else if (context.getIndexOfNewElementInOldContent() >= 0 && context.getIndexOfNewElementInOldContent() < oldContentChildIndex) {
                // newElement was moved forward to newContentChildIndex and the removed oldElement is already displayed in the original location
                // so display the added newElement in the new location also
                appendAddedElementContentIfRequired(context);
                newContentChildIndex++;
            } else if (context.getIndexOfOldElementInNewContent() >= 0 && context.getIndexOfOldElementInNewContent() < newContentChildIndex) {
                // oldElement was moved backward from oldContentChildIndex and the added newElement is already displayed in the new location
                // so display the removed oldElement in the original location also
                appendRemovedElementContentIfRequired(context);
                oldContentChildIndex++;
            } else if (context.getIndexOfNewElementInOldContent() < 0) {
                // newElement was added or moved so display the added element
                appendAddedElementContentIfRequired(context);
                newContentChildIndex++;
            } else {
                // oldElement was deleted or moved so only display the removed element
                appendRemovedElementContentIfRequired(context);
                oldContentChildIndex++;
            }
        }

        if (oldContentChildIndex < context.getOldContentRoot().getChildren().size()) {
            // there are still children in the old root that have not been processed
            // it means they were all moved backward or under a different parent or deleted
            // so display the removed children
            for (int i = oldContentChildIndex; i < context.getOldContentRoot().getChildren().size(); i++) {
                Element child = context.getOldContentRoot().getChildren().get(i);
                if(!shouldIgnoreElement(child)) {
                    appendRemovedElementContentIfRequired(context.setOldElement(child));
                }
            }
        } else if (newContentChildIndex < context.getNewContentRoot().getChildren().size()) {
            // obviously, this if test is not necessary, it's only used for clarity
            // there are still children in the new root that have not been processed
            // it means they were all moved forward or from a different parent or added
            // so display the added children
            for (int i = newContentChildIndex; i < context.getNewContentRoot().getChildren().size(); i++) {
                Element child = context.getNewContentRoot().getChildren().get(i);
                if(!shouldIgnoreElement(child)) {
                    appendAddedElementContentIfRequired(context.setIndexOfOldElementInNewContent(i).setNewElement(child));
                }
            }
        }
    }

    protected final void compareElementContents(ContentComparatorContext context) throws NavException {

        String content = getElementFragmentAsString(context.getNewContentNavigator(), context.getNewElement());

        if(context.getThreeWayDiff()) {
            context.setIntermediateElement(context.getIntermediateContentElements().get(context.getNewElement().getTagId()));
        }

        if ((isElementContentEqual(context) && !containsIgnoredElements(content)) || (context.getIgnoreRenumbering() && shouldIgnoreRenumbering(context.getNewElement()))) {
            if(context.getThreeWayDiff() && context.getIntermediateElement() != null) {
                context.getResultBuilder().append(updateElementAttribute(content, context.getStartTagAttrName(), context.getStartTagAttrValue()));
            } else {
                context.getResultBuilder().append(content);
            }
        } else if (!shouldIgnoreElement(context.getOldElement()) && (!context.getIgnoreElements() || !shouldIgnoreElement(context.getNewElement()))) {
            //add the start tag
            if(context.getThreeWayDiff()) {
              if(context.getIntermediateElement() != null && !shouldIgnoreElement(context.getIntermediateElement())) { // build start tag for moved/added element with added styles
                  context.getResultBuilder().append(buildStartTagForAddedElement(context.getNewElement(), context.getIntermediateElement(), context.getAttrName(), context.getAddedValue()));
              } else if(shouldIgnoreElement(context.getNewElement())) { //build start tag with removed styles for removed elements
                  context.getResultBuilder().append(buildStartTagForRemovedElement(context.getNewElement(), context.getIntermediateContentElements(), context.getAttrName(), context.getRemovedValue()));
              } else {
                  context.getResultBuilder().append(buildStartTag(context.getNewElement())); //build tag for children without styles
              }
            } else {
                context.getResultBuilder().append(buildStartTag(context.getNewElement(), context.getStartTagAttrName(), context.getStartTagAttrValue()));
            }

            context.resetStartTagAttribute();

            if (context.getNewElement().hasTextChild() || context.getOldElement().hasTextChild()) {
                String oldContent = getContentFragmentAsString(context.getOldContentNavigator(), context.getOldElement());
                String newContent = getContentFragmentAsString(context.getNewContentNavigator(), context.getNewElement());
                String intermediateContent = null;
                if(context.getThreeWayDiff() && context.getIntermediateElement() != null) {
                    intermediateContent = getContentFragmentAsString(context.getIntermediateContentNavigator(), context.getIntermediateElement());
                }
                String result = getTextComparator().compareTextNodeContents(oldContent, newContent, intermediateContent,
                        context.getAttrName(), context.getRemovedValue(), context.getAddedValue(), context.getThreeWayDiff());
                context.getResultBuilder().append(result);
            } else {
                computeDifferencesAtNodeLevel(new ContentComparatorContext.Builder(context)
                        .withOldContentRoot(context.getOldElement())
                        .withNewContentRoot(context.getNewElement())
                        .build());
            }
            //add the end tag
            context.getResultBuilder().append(buildEndTag(context.getNewElement()));
        } else if (shouldDisplayRemovedContent(context.getOldElement(), context.getIndexOfOldElementInNewContent())) {
            //element removed in the new version
            appendRemovedElementContent(context);
        }
    }

    protected abstract Boolean shouldIgnoreRenumbering(Element element);

    protected abstract StringBuilder buildStartTagForAddedElement(Element newElement, Element oldElement, String attrName, String attrValue);

    protected abstract StringBuilder buildStartTagForRemovedElement(Element newElement, Map<String, Element> intermediateContentElements, String attrName, String attrValue);

    protected final StringBuilder buildStartTag(Element newElement, String attrName, String attrValue){
        StringBuilder tagContent = buildStartTag(newElement);
        updateAttributeValue(tagContent, attrName, attrValue);
        return tagContent;
    }

    protected final StringBuilder buildStartTag(Element newElement){
        StringBuilder tagContent = new StringBuilder(newElement.getTagContent());
        if(shouldIgnoreElement(newElement)) {
            // add read-only attributes
            updateAttributeValue(tagContent, LEOS_EDITABLE_ATTR, Boolean.FALSE.toString());
            updateAttributeValue(tagContent, LEOS_DELETABLE_ATTR, Boolean.FALSE.toString());
        }
        return tagContent;
    }

    protected final String buildEndTag(Element element) {
        String tagContent = element.getTagContent();
        String endTag=null;
        try {
            if (tagContent.startsWith("<span")) {
                endTag = END_TAG;
            } else {
                //LOG.debug("TAG {}", tagContent);
                endTag = "</" + tagContent.substring(1, tagContent.indexOf(' ')>0 ?tagContent.indexOf(' '):(tagContent.length()-1)) + ">";
            }
        } catch(Exception e){
        	LOG.debug("TAG index {} {}",tagContent,  tagContent.indexOf(' '));
        }
        return endTag;
    }

    @Override
    public String[] twoColumnsCompareContents(ContentComparatorContext context) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        context.setLeftResultBuilder(new StringBuilder())
                .setRightResultBuilder(new StringBuilder());

        try {
            context.setOldContentNavigator(buildXMLNavigator(context.getComparedVersions()[0]))
                .setNewContentNavigator(buildXMLNavigator(context.getComparedVersions()[1]));

            //start from the akomaNtosol element
            context.getNewContentNavigator().toElement(VTDNav.ROOT);//akomaNtoso
            int elementNewContentIndex = context.getNewContentNavigator().getCurrentIndex();

            //start from the akomaNtoso element
            context.getOldContentNavigator().toElement(VTDNav.ROOT);//akomaNtoso

            //get the offset of the content of the bill element and add all the content until there
            int offsetBegin = (int) context.getNewContentNavigator().getContentFragment();
            String beginningContent = new String(context.getNewContentNavigator().getXML().getBytes(0, offsetBegin), Charset.forName("UTF-8"));
            context.getLeftResultBuilder().append(beginningContent);
            context.getRightResultBuilder().append(beginningContent);
            context.setModifications(new IntHolder());
            context.getModifications().setValue(0);

            context.setOldContentElements(new HashMap<>())
                .setNewContentElements(new HashMap<>())
                .setOldContentRoot(buildElement(context.getOldContentNavigator(), context.getOldContentNavigator().getCurrentIndex(),
                    new HashMap<>(), context.getOldContentElements()))
                .setNewContentRoot(buildElement(context.getNewContentNavigator(), context.getNewContentNavigator().getCurrentIndex(),
                    new HashMap<>(), context.getNewContentElements()));

            computeTwoColumnDifferencesAtNodeLevel(context);

            //add all the content as of the end of the content of the bill element until the end of this XML
            context.getNewContentNavigator().recoverNode(elementNewContentIndex);
            int offsetEnd = (int) (context.getNewContentNavigator().getContentFragment()) + (int) (context.getNewContentNavigator().getContentFragment() >> 32);
            int lengthEnd = context.getNewContentNavigator().getXML().length();
            lengthEnd -= offsetEnd;
            String endContent = new String(context.getNewContentNavigator().getXML().getBytes(offsetEnd, lengthEnd), Charset.forName("UTF-8"));
            context.getLeftResultBuilder().append(endContent);
            context.getRightResultBuilder().append(endContent);

        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.trace("Comparison finished!  ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return new String[]{context.getLeftResultBuilder().toString(), context.getRightResultBuilder().toString()};
    }

    private void computeTwoColumnDifferencesAtNodeLevel(ContentComparatorContext context) throws NavException {

        if (shouldIgnoreElement(context.getOldContentRoot())) {
            return;
        }

        int oldContentChildIndex = 0; // current index in oldContentRoot children list
        int newContentChildIndex = 0; // current index in newContentRoot children list

        while (oldContentChildIndex < context.getOldContentRoot().getChildren().size()
                && newContentChildIndex < context.getNewContentRoot().getChildren().size()) {

            context.setOldElement(context.getOldContentRoot().getChildren().get(oldContentChildIndex))
                .setNewElement(context.getNewContentRoot().getChildren().get(newContentChildIndex))
                .setIndexOfOldElementInNewContent(getBestMatchInList(context.getNewContentRoot().getChildren(), context.getOldElement()))
                .setIndexOfNewElementInOldContent(getBestMatchInList(context.getOldContentRoot().getChildren(), context.getNewElement()));

            // at each step, check for a particular structural change in this order
            if (shouldIgnoreElement(context.getNewElement())) {
                newContentChildIndex++;
            } else if (shouldIgnoreElement(context.getOldElement())) {
                oldContentChildIndex++;
            } else if (newContentChildIndex == context.getIndexOfOldElementInNewContent()) {
                // element did not changed relative position so check if it's content is changed
                twoColumnsCompareElementContents(context);
                oldContentChildIndex++;
                newContentChildIndex++;
            } else if (context.getIndexOfNewElementInOldContent() < 0 && context.getIndexOfOldElementInNewContent() < 0) {
                // oldElement was completely replaced with newElement
                appendRemovedElementsContent(context);
                appendAddedElementsContent(context);
                oldContentChildIndex++;
                newContentChildIndex++;
            } else if (context.getIndexOfNewElementInOldContent() >= oldContentChildIndex && context.getIndexOfOldElementInNewContent() > newContentChildIndex) {
                // newElement appears to be moved backward to newContentChildIndex and oldElement appears to be moved forward from oldContentChildIndex
                // at the same time
                // so display the element that was moved more positions because it's more likely to be the action the user actually made
                if( context.getIndexOfNewElementInOldContent() - oldContentChildIndex > context.getIndexOfOldElementInNewContent() - newContentChildIndex ){
                    // newElement was moved backward to newContentChildIndex more positions than oldElement was moved forward from oldContentChildIndex
                    // so display the added newElement in the new location for now
                    appendAddedElementsContent(context);
                    newContentChildIndex++;
                } else {
                    // oldElement was moved forward from oldContentChildIndex more or just as many positions as newElement was moved backward to newContentChildIndex
                    // so display the removed oldElement in the original location for now
                    appendRemovedElementsContent(context);
                    oldContentChildIndex++;
                }
            } else if (context.getIndexOfNewElementInOldContent() >= 0 && context.getIndexOfNewElementInOldContent() < oldContentChildIndex) {
                // newElement was moved forward to newContentChildIndex and the removed oldElement is already displayed in the original location
                // so display the added newElement in the new location also
                appendAddedElementsContent(context);
                newContentChildIndex++;
            } else if (context.getIndexOfOldElementInNewContent() >= 0 && context.getIndexOfOldElementInNewContent() < newContentChildIndex) {
                // oldElement was moved backward from oldContentChildIndex and the added newElement is already displayed in the new location
                // so display the removed oldElement in the original location also
                appendRemovedElementsContent(context);
                oldContentChildIndex++;
            } else if (context.getIndexOfNewElementInOldContent() < 0) {
                // newElement was added or moved from a different parent so only display the added element
                appendAddedElementsContent(context);
                newContentChildIndex++;
            } else {
                // oldElement was deleted or moved under a different parent so only display the removed element
                appendRemovedElementsContent(context);
                oldContentChildIndex++;
            }
        }

        if (oldContentChildIndex < context.getOldContentRoot().getChildren().size()) {
            // there are still children in the old root that have not been processed
            // it means they were all moved backward or under a different parent or deleted
            // so display the removed children
            for (int i = oldContentChildIndex; i < context.getOldContentRoot().getChildren().size(); i++) {
                Element child = context.getOldContentRoot().getChildren().get(i);
                if(!shouldIgnoreElement(child)) {
                    appendRemovedElementsContent(context.setOldElement(child));
                }
            }
        } else if (newContentChildIndex < context.getNewContentRoot().getChildren().size()) {
            //obviously, this if test is not necessary, it's only used for clarity
            // there are still children in the new root that have not been processed
            // it means they were all moved forward or from a different parent or added
            // so display the added children
            for (int i = newContentChildIndex; i < context.getNewContentRoot().getChildren().size(); i++) {
                Element child = context.getNewContentRoot().getChildren().get(i);
                if(!shouldIgnoreElement(child)) {
                    appendAddedElementsContent(context.setIndexOfOldElementInNewContent(i).setNewElement(child));
                }
            }
        }
    }

    protected final void twoColumnsCompareElementContents(ContentComparatorContext context) throws NavException {

        String content = getElementFragmentAsString(context.getNewContentNavigator(), context.getNewElement());

        if (isElementContentEqual(context) && !containsIgnoredElements(content)) {
            context.getRightResultBuilder().append(content);
            context.getLeftResultBuilder().append(content);
        } else if (!shouldIgnoreElement(context.getOldElement()) && !shouldIgnoreElement(context.getNewElement())){

            //add the start tag
            appendChangedElementsStartTag(null, context.getLeftResultBuilder(), context.getRightResultBuilder(), context.getNewElement());

            if (context.getNewElement().hasTextChild() || context.getOldElement().hasTextChild()) {
                //we keep using this blocks for displaying the results aligned, but we won't show the yellow pins.
                context.getRightResultBuilder().append(BLOCK_MODIFIED_LINE_START_TAG);
                //do this specifically for IE8 as it doesn't support querying by name if the name is part of the span. it must be part of an input type element
                context.getRightResultBuilder().append(MODIFIED_LINE_MARKER_PART_1).append(context.getModifications().getValue()).append(MODIFIED_LINE_MARKER_PART_2);
                context.getLeftResultBuilder().append(BLOCK_MODIFIED_LINE_START_TAG);
                context.getLeftResultBuilder().append(MODIFIED_LINE_MARKER_PART_1).append(context.getModifications().getValue()).append(MODIFIED_LINE_MARKER_PART_2);
                context.getModifications().setValue(context.getModifications().getValue()+1);

                String oldContent = getContentFragmentAsString(context.getOldContentNavigator(), context.getOldElement());
                String newContent = getContentFragmentAsString(context.getNewContentNavigator(), context.getNewElement());
                String[] result = getTextComparator().twoColumnsCompareTextNodeContents(oldContent, newContent);

                context.getLeftResultBuilder().append(result[0]);
                context.getRightResultBuilder().append(result[1]);

                context.getRightResultBuilder().append(END_TAG);
                context.getLeftResultBuilder().append(END_TAG);
            } else {
                computeTwoColumnDifferencesAtNodeLevel(new ContentComparatorContext.Builder(context)
                        .withOldContentRoot(context.getOldElement())
                        .withNewContentRoot(context.getNewElement())
                        .build());
            }
            //add the end tag
            appendElementsEndTag(context.getLeftResultBuilder(), context.getRightResultBuilder(), context.getNewElement());
        } else if(shouldDisplayRemovedContent(context.getOldElement(), context.getIndexOfOldElementInNewContent())){
            appendRemovedElementsContent(context);
        }
    }

    protected final void appendChangedElementsStartTag(Boolean isAdded, StringBuilder leftSideBuilder, StringBuilder rightSideBuilder, Element element){
        if (isAdded != null) {
            //on the right side, if isAdded, add the new element start tag otherwise put it as transparent
            rightSideBuilder.append(updateAttributeValue(new StringBuilder(element.getTagContent()), ATTR_NAME, isAdded ? CONTENT_ADDED_CLASS : CONTENT_BLOCK_REMOVED_CLASS).toString());
            //on the left side, if isAdded, add this element start tag for keeping the same occupied space, but make it transparent, otherwise put it as removed
            leftSideBuilder.append(updateAttributeValue(new StringBuilder(element.getTagContent()), ATTR_NAME, isAdded ? CONTENT_BLOCK_ADDED_CLASS : CONTENT_REMOVED_CLASS).toString());
        } else {
            //element was not changed
            leftSideBuilder.append(element.getTagContent());
            rightSideBuilder.append(element.getTagContent());
        }
    }

    protected final void appendElementsEndTag(StringBuilder leftSideBuilder, StringBuilder rightSideBuilder, Element element) {
        String endTag = buildEndTag(element);
        leftSideBuilder.append(endTag);
        rightSideBuilder.append(endTag);
    }

    protected final void appendChangedElementsContent(Boolean isAdded, StringBuilder leftSideBuilder, StringBuilder rightSideBuilder, String changedContent){
        if (isAdded != null) {
            //on the right side, if isAdded, add the new element content otherwise put it as transparent
            rightSideBuilder.append(updateElementAttribute(changedContent, ATTR_NAME, isAdded ? CONTENT_ADDED_CLASS : CONTENT_BLOCK_REMOVED_CLASS));
            //on the left side, if isAdded, add this element content for keeping the same occupied space, but make it transparent, otherwise put it as removed
            leftSideBuilder.append(updateElementAttribute(changedContent, ATTR_NAME, isAdded ? CONTENT_BLOCK_ADDED_CLASS : CONTENT_REMOVED_CLASS));
        } else {
            //element was not changed
            rightSideBuilder.append(changedContent);
            leftSideBuilder.append(changedContent);
        }
    }

    protected final void appendRemovedElementContentIfRequired(ContentComparatorContext context) throws NavException {
        if (shouldIgnoreElement(context.getNewContentRoot()) || isElementInItsOriginalPosition(context.getNewContentRoot())) {
            appendRemovedElementContent(context);
        }
    }

    protected final void appendAddedElementContentIfRequired(ContentComparatorContext context) throws NavException {
        appendAddedElementContent(context);
    }

    protected final Boolean isElementRemovedFromContent(int indexOfOldElementInNewContent){
        return indexOfOldElementInNewContent == -1;
    }

    protected abstract void appendRemovedElementsContent(ContentComparatorContext context) throws NavException;

    protected abstract void appendAddedElementsContent(ContentComparatorContext context) throws NavException;

    protected abstract Boolean shouldDisplayRemovedContent(Element elementOldContent, int indexOfOldElementInNewContent);

    protected abstract Boolean containsIgnoredElements(String content);

    protected abstract Boolean shouldIgnoreElement(Element element);

    protected abstract Boolean isElementInItsOriginalPosition(Element element);

    protected abstract Boolean shouldCompareElements(Element oldElement, Element newElement);

    protected abstract String getChangedElementContent(VTDNav contentNavigator, Element element, String attrName, String attrValue) throws NavException;

    protected abstract void appendAddedElementContent(ContentComparatorContext context) throws NavException;

    protected abstract void appendRemovedElementContent(ContentComparatorContext context) throws NavException;
    
    protected abstract TextComparator getTextComparator();

    protected final int getBestMatchInList(List<Element> childElements, Element element){
        if (shouldIgnoreElement(element)) {
            return -2;
        }
        int foundPosition = -1;
        int rank[] = new int[childElements.size()];
        for (int iCount = 0; iCount < childElements.size(); iCount++) {
            Element listElement = childElements.get(iCount);

            if (listElement.getTagId() != null && element.getTagId() != null
                    && listElement.getTagId().equals(element.getTagId())) {
                rank[iCount] = 1000;
                break;
            }
            else if((listElement.getTagId()==null && element.getTagId()==null)
                    && listElement.getTagName().equals(element.getTagName())) {//only try to find match if tagID is not present

                // compute node distance
                int maxDistance = 100;
                int distanceWeight = maxDistance / 5; //after distance of 5 nodes it is discarded
                int nodeDistance = Math.abs(listElement.getNodeIndex() - element.getNodeIndex());
                nodeDistance = Math.min(nodeDistance * distanceWeight, maxDistance); // 0...maxDistance

                // compute node similarity
                int similarityWeight = 2;           
                int similarity = (int) (100 * listElement.contentSimilarity(element)); //0...100
                similarity = similarity * similarityWeight; 
                
                // compute node rank
                rank[iCount] = (maxDistance - nodeDistance)  //distance 0=100, 1=80,2=60,..5=0
                               + similarity;
            } else {
                rank[iCount] = 0;
            }
        }
        
        int bestRank=0;
        for (int iCount = 0; iCount < rank.length; iCount++) {
            if(bestRank < rank[iCount]){
                foundPosition=iCount;
                bestRank = rank[iCount];
            }
        }
        return bestRank > 0 ? foundPosition : -1;
    }
}
