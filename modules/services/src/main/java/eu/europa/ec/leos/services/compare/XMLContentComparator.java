/**
 * Copyright 2016 European Commission
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
package eu.europa.ec.leos.services.compare;

import static eu.europa.ec.leos.support.xml.vtd.VTDHelper.buildXMLNavigator;
import static eu.europa.ec.leos.support.xml.vtd.VTDHelper.getAllChildElements;
import static eu.europa.ec.leos.support.xml.vtd.VTDHelper.getContentFragmentAsString;
import static eu.europa.ec.leos.support.xml.vtd.VTDHelper.getElementFragmentAsString;
import static eu.europa.ec.leos.support.xml.vtd.VTDHelper.isElementContentEqual;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Stopwatch;
import com.ximpleware.NavException;
import com.ximpleware.VTDNav;

import eu.europa.ec.leos.support.xml.vtd.Element;
import eu.europa.ec.leos.support.xml.vtd.IntHolder;

/**
 * @author: micleva
 * @date: 4/18/13 12:16 PM
 * @project: ETX
 */
@Service
public class XMLContentComparator implements ContentComparatorService {

    public static final String BLOCK_REMOVED_LINE_START_TAG = "<span class=\"" + CONTENT_BLOCK_REMOVED_CLASS+ "\">";
    public static final String BLOCK_ADDED_LINE_START_TAG = "<span class=\"" + CONTENT_BLOCK_ADDED_CLASS+ "\">";
    private final String BLOCK_MODIFIED_LINE_START_TAG = "<span class=\"" + CONTENT_BLOCK_MODIFIED_CLASS+ "\">";
    private final String MODIFIED_LINE_MARKER_PART_1 = "<input type=\"hidden\" name=\"modification_";
    private final String MODIFIED_LINE_MARKER_PART_2 = "\"/>";
    private final String REMOVED_LINE_START_TAG = "<span class=\"" + CONTENT_REMOVED_CLASS + "\">";
    private final String ADDED_LINE_START_TAG = "<span class=\"" + CONTENT_ADDED_CLASS + "\">";
    public static final String END_TAG = "</span>";

    final static Set<String> CLASS_WITH_COLORED_TEXT = new HashSet<>();

    private static final Logger LOG = LoggerFactory.getLogger(XMLContentComparator.class);

    static {
        CLASS_WITH_COLORED_TEXT.add("akn-placeholder");
        CLASS_WITH_COLORED_TEXT.add("akn-authorialNote");
    }

    private final JavaDiffUtilContentComparator contentComparator = new JavaDiffUtilContentComparator();

    @Override
    public String compareHtmlContents(String oldContent, String newContent) {
        StringBuilder mergeBuilder = new StringBuilder();
        
        try {
            VTDNav oldContentNavigator = buildXMLNavigator(oldContent);
            VTDNav newContentNavigator = buildXMLNavigator(newContent);

            //start from the akn-bill element
            String s =null;
            newContentNavigator.toElement(VTDNav.FIRST_CHILD);//head
            newContentNavigator.toElement(VTDNav.NEXT_SIBLING);//body
            
            int elementNewContentIndex = newContentNavigator.getCurrentIndex();

            //start from the akn-bill element
            oldContentNavigator.toElement(VTDNav.FIRST_CHILD);//head
            oldContentNavigator.toElement(VTDNav.NEXT_SIBLING);//body
//            
            int elementOldContentIndex = oldContentNavigator.getCurrentIndex();

            //get the offset of the content of the bill element and add all the content until there
            int offsetBegin = (int) newContentNavigator.getContentFragment();
            mergeBuilder.append(new String(newContentNavigator.getXML().getBytes(0, offsetBegin), Charset.forName("UTF-8")));

            computeDifferencesAtNodeLevel(oldContentNavigator, elementOldContentIndex, newContentNavigator, elementNewContentIndex, mergeBuilder);

            //add all the content as of the end of the content of the bill element until the end of this XML
            newContentNavigator.recoverNode(elementNewContentIndex);
            int offsetEnd = (int) (newContentNavigator.getContentFragment()) + (int) (newContentNavigator.getContentFragment() >> 32);
            int lengthEnd = newContentNavigator.getXML().length();
            lengthEnd -= offsetEnd;
            mergeBuilder.append(new String(newContentNavigator.getXML().getBytes(offsetEnd, lengthEnd), Charset.forName("UTF-8")));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return mergeBuilder.toString();
    }

    private void computeDifferencesAtNodeLevel(VTDNav oldContentNavigator, int elementOldContentIndex,
                                               VTDNav newContentNavigator, int elementNewContentIndex,
                                               StringBuilder mergeBuilder) throws NavException {

        oldContentNavigator.recoverNode(elementOldContentIndex);
        newContentNavigator.recoverNode(elementNewContentIndex);

        //get the child elements from old content as well as from the new content and compare them
        List<Element> childElementsOldContent = getAllChildElements(oldContentNavigator);
        List<Element> childElementsNewContent = getAllChildElements(newContentNavigator);

        if (childElementsOldContent.isEmpty()) {
            //all child elements on the new content is newly added content as there are no children on the old content
            if (!childElementsNewContent.isEmpty()) {
                mergeBuilder.append(ADDED_LINE_START_TAG);
                for (Element element : childElementsNewContent) {
                    mergeBuilder.append(getElementFragmentAsString(newContentNavigator, element));
                }
                mergeBuilder.append(END_TAG);
            }
        } else {
            int previousElementFoundInNewContentIndex = -1;
            ArrayList<Element> handledNewElements = new ArrayList<Element>();
            for (Element elementOldContent : childElementsOldContent) {
                //if (childElementsNewContent.contains(elementOldContent)) {
                int bestMatchPosition=getBestMatchInRemainingList(childElementsNewContent,elementOldContent, previousElementFoundInNewContentIndex+1);
                if (bestMatchPosition != -1){
                    //old element found on the new content
                    //everything which is between the previous old element found on the new content and this old element
                    // found on the new content is newly added content

                    //get the position of the old element in the new content
                    //int positionOfOldElementInNewContent = childElementsNewContent.indexOf(elementOldContent);
                    int positionOfOldElementInNewContent = bestMatchPosition;
                    Element sameElementNewContent = childElementsNewContent.get(positionOfOldElementInNewContent);
                    
                    if (previousElementFoundInNewContentIndex + 1 < positionOfOldElementInNewContent) {
                        mergeBuilder.append(ADDED_LINE_START_TAG);
                        for (int newElIdx = previousElementFoundInNewContentIndex + 1; newElIdx < positionOfOldElementInNewContent; newElIdx++) {
                            String addedContent = getElementFragmentAsString(newContentNavigator, childElementsNewContent.get(newElIdx));
                            mergeBuilder.append(addedContent);
                            handledNewElements.add(childElementsNewContent.get(newElIdx));
                        }
                        mergeBuilder.append(END_TAG);
                    }
                    //now check if the content of these two elements is really the same
                    boolean isContentEqual = isElementContentEqual(oldContentNavigator, elementOldContent, newContentNavigator, sameElementNewContent);
                    if (isContentEqual) {
                        mergeBuilder.append(getElementFragmentAsString(newContentNavigator, sameElementNewContent));
                    } else {
                        //add the start tag
                        mergeBuilder.append(sameElementNewContent.getTagContent());

                        if (sameElementNewContent.hasTextChild() ||elementOldContent.hasTextChild()) {
                            String oldContent = getContentFragmentAsString(oldContentNavigator, elementOldContent);
                            String newContent = getContentFragmentAsString(newContentNavigator, sameElementNewContent);
                            String result = contentComparator.compareHtmlContents(oldContent, newContent);
                            mergeBuilder.append(result);
                        } else {
                            computeDifferencesAtNodeLevel(oldContentNavigator, elementOldContent.getNavigationIndex(),
                                    newContentNavigator, sameElementNewContent.getNavigationIndex(),
                                    mergeBuilder);
                        }
                        //add the end tag
                        mergeBuilder.append(buildEndTag(sameElementNewContent.getTagContent()));
                    }
                    handledNewElements.add(sameElementNewContent);
                    previousElementFoundInNewContentIndex = positionOfOldElementInNewContent;
                } else {
                    //element removed in the new version
                    mergeBuilder.append(REMOVED_LINE_START_TAG);
                    mergeBuilder.append(getElementFragmentAsString(oldContentNavigator, elementOldContent));
                    mergeBuilder.append(END_TAG);
                }
            }
            //at the end, simply add all the remaining elements from the new content (if any)
            if (previousElementFoundInNewContentIndex + 1 < childElementsNewContent.size()) {
                mergeBuilder.append(ADDED_LINE_START_TAG);
                for (int i = previousElementFoundInNewContentIndex + 1; i < childElementsNewContent.size(); i++) {
                    String addedContent = getElementFragmentAsString(newContentNavigator, childElementsNewContent.get(i));
                    mergeBuilder.append(addedContent);
                }
                mergeBuilder.append(END_TAG);
            }
        }
    }

    private String buildEndTag(String tagContent) {
        String endTag=null;
        try{
        if (tagContent.startsWith("<span")) {
            endTag = END_TAG;
        } else {
        	//LOG.debug("TAG {}", tagContent);
            endTag = "</" + tagContent.substring(1, tagContent.indexOf(' ')>0 ?tagContent.indexOf(' '):(tagContent.length()-1)) + ">";
        }
        }
        catch(Exception e){
        	LOG.debug("TAG index {} {}",tagContent,  tagContent.indexOf(' '));
        }
        return endTag;
    }

    @Override
    public String[] twoColumnsCompareHtmlContents(String oldContent, String newContent) {
        StringBuilder leftSideBuilder = new StringBuilder();
        StringBuilder rightSideBuilder = new StringBuilder();

        try {
            VTDNav oldContentNavigator = buildXMLNavigator(oldContent);
            VTDNav newContentNavigator = buildXMLNavigator(newContent);

            //start from the akn-bill element
            newContentNavigator.toElement(VTDNav.FIRST_CHILD);
            newContentNavigator.toElement(VTDNav.NEXT_SIBLING);//body
            
            int elementNewContentIndex = newContentNavigator.getCurrentIndex();

            //start from the akn-bill element
            oldContentNavigator.toElement(VTDNav.FIRST_CHILD);
            oldContentNavigator.toElement(VTDNav.NEXT_SIBLING);//body
            int elementOldContentIndex = oldContentNavigator.getCurrentIndex();

            //get the offset of the content of the bill element and add all the content until there
            int offsetBegin = (int) newContentNavigator.getContentFragment();
            String beginningContent = new String(newContentNavigator.getXML().getBytes(0, offsetBegin), Charset.forName("UTF-8"));
            leftSideBuilder.append(beginningContent);
            rightSideBuilder.append(beginningContent);

            IntHolder modifications = new IntHolder();
            modifications.setValue( 0);;
            computeTwoColumnDifferencesAtNodeLevel(oldContentNavigator, elementOldContentIndex,
                    newContentNavigator, elementNewContentIndex,
                    leftSideBuilder, rightSideBuilder, modifications);

            //add all the content as of the end of the content of the bill element until the end of this XML
            newContentNavigator.recoverNode(elementNewContentIndex);
            int offsetEnd = (int) (newContentNavigator.getContentFragment()) + (int) (newContentNavigator.getContentFragment() >> 32);
            int lengthEnd = newContentNavigator.getXML().length();
            lengthEnd -= offsetEnd;
            String endContent = new String(newContentNavigator.getXML().getBytes(offsetEnd, lengthEnd), Charset.forName("UTF-8"));
            leftSideBuilder.append(endContent);
            rightSideBuilder.append(endContent);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new String[]{leftSideBuilder.toString(), rightSideBuilder.toString()};
    }

    private void computeTwoColumnDifferencesAtNodeLevel(VTDNav oldContentNavigator, int elementOldContentIndex,
                                                        VTDNav newContentNavigator, int elementNewContentIndex,
                                                        StringBuilder leftSideBuilder, StringBuilder rightSideBuilder, IntHolder modifications) throws NavException {
        oldContentNavigator.recoverNode(elementOldContentIndex);
        newContentNavigator.recoverNode(elementNewContentIndex);

        //get the child elements from old content as well as from the new content and compare them
        List<Element> childElementsOldContent = getAllChildElements(oldContentNavigator);
        List<Element> childElementsNewContent = getAllChildElements(newContentNavigator);

        if (childElementsOldContent.isEmpty()) {
            //all child elements on the new content is newly added content as there are no children on the old content
            if (!childElementsNewContent.isEmpty()) {
                rightSideBuilder.append(ADDED_LINE_START_TAG);
                leftSideBuilder.append(BLOCK_ADDED_LINE_START_TAG);
                for (Element element : childElementsNewContent) {
                    String elementContent = getElementFragmentAsString(newContentNavigator, element);
                    rightSideBuilder.append(elementContent);

                    //on the left side, add this element for keeping the same occupied space, but make it transparent
                    leftSideBuilder.append(removeStylesWithColoredText(elementContent));
                }
                rightSideBuilder.append(END_TAG);
                leftSideBuilder.append(END_TAG);
            }
        } else {
            int previousElementFoundInNewContentIndex = -1;
            ArrayList<Element> handledNewElements = new ArrayList<Element>();

            for (Element elementOldContent : childElementsOldContent) {
                int bestMatchPosition=getBestMatchInRemainingList(childElementsNewContent,elementOldContent, previousElementFoundInNewContentIndex+1);
                if (bestMatchPosition != -1){
                //if (childElementsNewContent.contains(elementOldContent)) {
                    //old element found on the new content
                    //everything which is between the previous old element found on the new content and this old element
                    // found on the new content is newly added content. Add this content in the right side
                    // and as transparent on the left side

                    //get the position of the old element in the new content
                    //int positionOfOldElementInNewContent = childElementsNewContent.indexOf(elementOldContent);
                    int positionOfOldElementInNewContent = bestMatchPosition;
                    Element sameElementNewContent = childElementsNewContent.get(positionOfOldElementInNewContent);
                    
                    if (previousElementFoundInNewContentIndex + 1 < positionOfOldElementInNewContent) {
                        rightSideBuilder.append(ADDED_LINE_START_TAG);
                        leftSideBuilder.append(BLOCK_ADDED_LINE_START_TAG);
                        for (int newElIdx = previousElementFoundInNewContentIndex + 1; newElIdx < positionOfOldElementInNewContent; newElIdx++) {
                            String addedContent = getElementFragmentAsString(newContentNavigator, childElementsNewContent.get(newElIdx));
                            
                            handledNewElements.add(childElementsNewContent.get(newElIdx));
                            
                            rightSideBuilder.append(addedContent);
                            leftSideBuilder.append(removeStylesWithColoredText(addedContent));
                        }
                        rightSideBuilder.append(END_TAG);
                        leftSideBuilder.append(END_TAG);
                    }
                    //now check if the content of these two elements is really the same
                    boolean isContentEqual = isElementContentEqual(oldContentNavigator, elementOldContent, newContentNavigator, sameElementNewContent);
                    if (isContentEqual) {
                        String equalContent = getElementFragmentAsString(newContentNavigator, sameElementNewContent);
                        rightSideBuilder.append(equalContent);
                        leftSideBuilder.append(equalContent);
                    } else {

                        //add the start tag
                        rightSideBuilder.append(sameElementNewContent.getTagContent());
                        leftSideBuilder.append(sameElementNewContent.getTagContent());

                        if (sameElementNewContent.hasTextChild()||elementOldContent.hasTextChild()) {
                            rightSideBuilder.append(BLOCK_MODIFIED_LINE_START_TAG);
                            //do this specifically for IE8 as it doesn't support querying by name if the name is part of the span. it must be part of an input type element
                            rightSideBuilder.append(MODIFIED_LINE_MARKER_PART_1).append(modifications.getValue()).append(MODIFIED_LINE_MARKER_PART_2);
                            leftSideBuilder.append(BLOCK_MODIFIED_LINE_START_TAG);
                            leftSideBuilder.append(MODIFIED_LINE_MARKER_PART_1).append(modifications.getValue()).append(MODIFIED_LINE_MARKER_PART_2);
                            modifications.setValue(modifications.getValue()+1);

                            String oldContent = getContentFragmentAsString(oldContentNavigator, elementOldContent);
                            String newContent = getContentFragmentAsString(newContentNavigator, sameElementNewContent);
                            String[] result = contentComparator.twoColumnsCompareHtmlContents(oldContent, newContent);

                            leftSideBuilder.append(result[0]);
                            rightSideBuilder.append(result[1]);

                            rightSideBuilder.append(END_TAG);
                            leftSideBuilder.append(END_TAG);
                        } else {
                            computeTwoColumnDifferencesAtNodeLevel(oldContentNavigator, elementOldContent.getNavigationIndex(),
                                    newContentNavigator, sameElementNewContent.getNavigationIndex(),
                                    leftSideBuilder, rightSideBuilder, modifications);
                        }
                        //add the end tag
                        String endTag = buildEndTag(sameElementNewContent.getTagContent());
                        rightSideBuilder.append(endTag);
                        leftSideBuilder.append(endTag);
                    }
                   handledNewElements.add(sameElementNewContent);
                    previousElementFoundInNewContentIndex = positionOfOldElementInNewContent;
                } else {
                    String removedContent = getElementFragmentAsString(oldContentNavigator, elementOldContent);

                    //element removed in the new version, put it as transparent
                    rightSideBuilder.append(BLOCK_REMOVED_LINE_START_TAG);
                    rightSideBuilder.append(removeStylesWithColoredText(removedContent));
                    rightSideBuilder.append(END_TAG);

                    //element removed from the old version
                    leftSideBuilder.append(REMOVED_LINE_START_TAG);
                    leftSideBuilder.append(removedContent);
                    leftSideBuilder.append(END_TAG);
                }
            }
            //at the end, simply add all the remaining elements from the new content (if any)
            // on the left side, add the same content as transparent
            if (previousElementFoundInNewContentIndex + 1 < childElementsNewContent.size()) {
                rightSideBuilder.append(ADDED_LINE_START_TAG);
                leftSideBuilder.append(BLOCK_ADDED_LINE_START_TAG);
                for (int i = previousElementFoundInNewContentIndex + 1; i < childElementsNewContent.size(); i++) {
                    String addedContent = getElementFragmentAsString(newContentNavigator, childElementsNewContent.get(i));
                    rightSideBuilder.append(addedContent);
                    leftSideBuilder.append(removeStylesWithColoredText(addedContent));
                }
                rightSideBuilder.append(END_TAG);
                leftSideBuilder.append(END_TAG);
            }
        }
    }

    private String removeStylesWithColoredText(String elementContent) {
        String result = elementContent;
        for (String style : CLASS_WITH_COLORED_TEXT) {
            result = result.replaceAll(style, "");
        }
        return result;
    }
    
    private int getBestMatchInRemainingList(List<Element> childElementsNewContent, Element element, int searchFromPostion){
        int foundPosition=-1;
        int rank []=new int[childElementsNewContent.size()];
        Stopwatch stopwatch=Stopwatch.createStarted();

        for(int iCount=searchFromPostion; iCount < childElementsNewContent.size(); iCount++){
            Element listElement=childElementsNewContent.get(iCount);

            if(listElement.getTagId() != null && element.getTagId() != null 
                    && listElement.getTagId().equals(element.getTagId())){
                rank[iCount] = 1000;
                break;            
            }
            else if( (listElement.getTagId()==null && element.getTagId()==null) 
                    && listElement.getTagName().equals(element.getTagName())){//only try to find match if tagID is present

                // compute node distance
                int maxDistance = 100;
                int distanceWeight = maxDistance / 5; //after distance of 5 nodes it is discarded
                int nodeDistance = Math.abs(listElement.getNodeIndex()-element.getNodeIndex());
                nodeDistance = Math.min(nodeDistance*distanceWeight, maxDistance); // 0...maxDistance 

                // compute node similarity
                int similarityWeight = 2;           
                int similarity = (int) (100 * listElement.contentSimilarity(element)); //0...100
                similarity = similarity * similarityWeight; 
                
                // compute node rank
                rank[iCount] = (maxDistance -nodeDistance)  //distance 0=100, 1=80,2=60,..5=0
                               + similarity  ;             
            }
            else{
                rank[iCount] = 0;
            }
        }
        
        int bestRank=0;
        for(int iCount=searchFromPostion; iCount < rank.length; iCount++){
            if(bestRank < rank[iCount]){
                foundPosition=iCount;
                bestRank = rank[iCount];
            }
        }
        LOG.trace("found best match time for elemnt {}:{} ms", element, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bestRank>0 ? foundPosition:-1;
    }
}
