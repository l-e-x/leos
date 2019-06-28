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
package eu.europa.ec.leos.services.support.xml;

import com.ximpleware.XMLModifier;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface XmlContentProcessor {

    String getElementValue(byte[] xmlContent, String xPath, boolean namespaceEnabled);

    List<String> getAncestorsIdsForElementId(byte[] xmlContent, String idAttributeValue);

    String getElementByNameAndId(byte[] xmlContent, String tagName, String idAttributeValue);

    List<Map<String, String>> getElementsAttributesByPath(byte[] xmlContent, String xPath);

    Map<String, String> getElementAttributesByPath(byte[] xmlContent, String xPath, boolean namespaceEnabled);

    String getElementContentFragmentByPath(byte[] xmlContent, String xPath, boolean namespaceEnabled);

    String getElementFragmentByPath(byte[] xmlContent, String xPath, boolean namespaceEnabled);

    byte[] replaceElementByTagNameAndId(byte[] xmlContent, String newContent, String tagName, String idAttributeValue);

    byte[] deleteElementByTagNameAndId(byte[] xmlContent, String tagName, String idAttributeValue);

    byte[] insertElementByTagNameAndId(byte[] xmlContent, String articleTemplate, String tagName, String idAttributeValue, boolean before);

    List<TableOfContentItemVO> buildTableOfContent(String startingNode, Function<String, TocItemType> getTocItemType, byte[] xmlContent, boolean simplified);

    byte[] createDocumentContentWithNewTocList(Function<String, TocItemType> getTocItemType, List<TableOfContentItemVO> tableOfContentItemVOs, byte[] content,
            User user);

    byte[] replaceElementsWithTagName(byte[] xmlContent, String tagName, String newContent);

    byte[] appendElementToTag(byte[] xmlContent, String tagName, String newContent);

    byte[] injectTagIdsinXML(byte[] xmlContent);

    byte[] doXMLPostProcessing(byte[] xmlContent);

    byte[] updateReferedAttributes(byte[] xmlContent, Map<String, String> referenceValueMap);
    
    /**Finds the first element with the id,if there are others, XML is incorrect
     * @param xmlContent
     * @param idAttributeValue id Attribute value
     * @return complete Tag or null
     */
    String[] getElementById(byte[] xmlContent, String idAttributeValue);
    
    /** removes all elements selected by xpath supplied
     * @param xmlContent
     *  @param xpath to select elements
     *  @return updated xml
     */
    byte[] removeElements(byte[] xmlContent, String xpath);

    /** removes all elements selected by xpath supplied, Also it removes parent of selected elements specified by parentsToRemove
     * @param xmlContent
     * @param xpath xpath to select elements
     * @param parentsToRemove number of parents to remove
     *  @return updated xml
     */
    byte[] removeElements(byte[] xmlContent, String xpath, int parentsToRemove);

    /**
     * searches the {@param searchText} and replace it with the {@param replaceText}.
     * @param xmlContent
     * @param searchText
     * @param replaceText
     * @return: On success returns updated content. On failure returns null.
     */
    byte[] searchAndReplaceText(byte[] xmlContent, String searchText, String replaceText);

    /**
     * returns a map with new article id as key and the updated content.
     * @param content
     * @param elementType
     * @return updated content
     */
    String doImportedElementPreProcessing(String content, String elementType);

    /**
     * returns the id of the last element based on the xPath present in the xml
     * @param xmlContent
     * @param xPath
     * @return elementId
     */
    String getElementIdByPath(byte[] xmlContent, String xPath);

    /**
     * searches the {@param origText} in the element {@param elementId} and replace it with the {@param newText}.
     * @param byteXmlContent
     * @param origText
     * @param elementId
     * @param startOffset
     * @param endOffset
     * @param newText
     * @return: On success returns updated content. On failure throws exception.
     */
    byte[] replaceTextInElement(byte[] xmlContent, String origText, String newText, String elementId, int startOffset, int endOffset);

    /**
     * adding and attribute {@param attributeName} on all children of an XML element {@param parentTag}.
     * @param xmlContent
     * @param parentTag
     * @param attributeName
     * @param attributeValue
     * @return: On success returns updated content. On failure throws exception.
     */
    byte[] setAttributeForAllChildren(byte[] xmlContent, String parentTag, List<String> elementTags, String attributeName, String value) throws Exception;

    /**
     *  get the parent element id given a child id attribute value
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @return
     */
    String[] getParentElement(byte[] xmlContent, String tagName, String idAttributeValue);

    /**
     * get the parent element id given a child id attribute value
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @return
     */
    String getParentElementId(byte[] xmlContent, String tagName, String idAttributeValue);

    /**
     *  get the sibling element given an element id attribute value, tag name, considering only tag elements provided and before/after element
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @param elementTags
     * @param before
     * @return
     */
    String[] getSiblingElement(byte[] xmlContent, String tagName, String idAttributeValue, List<String> elementTags, boolean before);

    /**
     *  get the child element at specified position given an element id attribute value, tag name, considering only tag elements provided and position
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @param elementTags
     * @param position
     * @return
     */
    String[] getChildElement(byte[] xmlContent, String tagName, String idAttributeValue, List<String> elementTags, int position);

    /**
     * get element from the given document if a split operation is already performed over element passed as argument
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @return
     */
    String[] getSplittedElement(byte[] xmlContent, String tagName, String idAttributeValue);

    /**
     * get element from the given document if a merge operation is performed over element passed as argument
     * @param xmlContent
     * @param content
     * @param tagName
     * @param idAttributeValue
     * @return
     */
    String[] getMergeOnElement(byte[] xmlContent, String content, String tagName, String idAttributeValue);

    /**
     * Merge element from the given document with sibling or parent element 
     * @param xmlContent
     * @param content
     * @param tagName
     * @param idAttributeValue
     * @param user
     * @return
     */
    byte[] mergeElement(byte[] xmlContent, String content, String tagName, String idAttributeValue);

    void specificInstanceXMLPostProcessing(XMLModifier xmlModifier) throws Exception;
}
