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
package eu.europa.ec.leos.services.support.xml;

import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface XmlContentProcessor {
	
	/**
	 * @param xmlContent Pass xml which contains attribute to be evaluated
	 * @param xPath Xpath of element to be evaluated
	 * @param namespaceEnabled TODO
	 * @return the value of the tag in xPath
	 */
	public String getElementValue(byte[] xmlContent,String xPath, boolean namespaceEnabled);

	public List<String> getAncestorsIdsForElementId(byte[] xmlContent, String idAttributeValue);
    /**
     * 
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue 
     * @return the element with the tagname and the id, or the first element with the tagname if id is null
     */
    String getElementByNameAndId(byte[] xmlContent, String tagName, String idAttributeValue);

    /**
     * 
     * @param xmlContent
     * @param xPath
     * @return List containing for each found element a Map with the attributes' name and the attributes' value
     */
    List<Map<String, String>> getElementsAttributesByPath(byte[] xmlContent, String xPath);

    /**
     * 
     * @param xmlContent
     * @param idAttributeValue
     * @return a String array containing elementName and element content
     */
    String[] getElementById(byte[] xmlContent, String idAttributeValue);

    byte[] replaceElementByTagNameAndId(byte[] xmlContent, String newContent, String tagName, String idAttributeValue);

    byte[] deleteElementByTagNameAndId(byte[] xmlContent, String tagName, String idAttributeValue);

    byte[] insertElementByTagNameAndId(byte[] xmlContent, String articleTemplate, String tagName, String idAttributeValue, boolean before);

    List<TableOfContentItemVO> buildTableOfContent(String startingNode, Function<String, TocItemType> getTocItemType, byte[] xmlContent);

    byte[] createDocumentContentWithNewTocList(Function<String, TocItemType> getTocItemType, TocItemType type, List<TableOfContentItemVO> tableOfContentItemVOs, byte[] content);

    byte[] replaceElementsWithTagName(byte[] xmlContent, String tagName, String newContent);

    byte[] appendElementToTag(byte[] xmlContent, String tagName, String newContent);
    
    byte[] injectTagIdsinXML(byte[] xmlContent) ;
    
    byte[] doXMLPostProcessing(byte[] xmlContent) ;

    byte[] updateReferedAttributes(byte[] xmlContent, Map<String, String> referenceValueMap);

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
    byte[] setOriginAttribute(byte[] xmlContent, String parentTag) throws Exception;

    /**
     *  get the parent element id given a child id attribute value
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @return
     */
    String getParentElement(byte[] xmlContent, String tagName, String idAttributeValue);

    /**
     * get the parent element id given a child id attribute value
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue
     * @return
     */
    String getParentElementId(byte[] xmlContent, String tagName, String idAttributeValue);
}
