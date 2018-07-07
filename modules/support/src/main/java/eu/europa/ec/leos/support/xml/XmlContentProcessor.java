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
package eu.europa.ec.leos.support.xml;

import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;

import java.util.List;
import java.util.Map;

public interface XmlContentProcessor {

	public List<String> getAncestorsIdsForElementId(byte[] xmlContent, String idAttributeValue);
    /**
     * 
     * @param xmlContent
     * @param tagName
     * @param idAttributeValue 
     * @return the element with the tagname and the id, or the first element with the tagname if id is null
     */
    public String getElementByNameAndId(byte[] xmlContent, String tagName, String idAttributeValue);

    public String getElementById(byte[] xmlContent, String idAttributeValue);

    public byte[] replaceElementByTagNameAndId(byte[] xmlContent, String newContent, String tagName, String idAttributeValue);

    public byte[] deleteElementByTagNameAndId(byte[] xmlContent, String tagName, String idAttributeValue);

    public byte[] insertElementByTagNameAndId(byte[] xmlContent, String articleTemplate, String tagName, String idAttributeValue, boolean before);

    public List<TableOfContentItemVO> buildTableOfContent(byte[] xmlContent);

    public byte[] createDocumentContentWithNewTocList(List<TableOfContentItemVO> tableOfContentItemVOs, byte[] contentStream);

    public byte[] replaceElementsWithTagName(byte[] xmlContent, String tagName, String newContent);

    public byte[] appendElementToTag(byte[] xmlContent, String tagName, String newContent);

    public byte[] renumberArticles(byte[] xmlContent, String language);
    
    public byte[] injectTagIdsinXML(byte[] xmlContent) ;
    
    public byte[] doXMLPostProcessing(byte[] xmlContent) ;

    public byte[] updateReferedAttributes(byte[] xmlContent, Map<String, String> referenceValueMap);

    /** Finds the element with the id and updates its content with comment String
    * @param xmlContent
    * @param elementId elementId
    * @param comment comment in xml format
    * @param start true with comment to go in start, false if comment is added in end
    * @return updated document
     * @throws Exception in case element not found/error
    */        
    public byte[] insertCommentInElement(byte[] xmlContent, String elementId, String comment, boolean start) throws Exception;
    
    /** Finds all the comments and creates the structure
    * @param xmlContent
    * @return list of comments in commentVO
    */            
    public List<CommentVO> getAllComments(byte[] xmlContent);

    /** removes all elements selected by xpath supplied
     * @param xmlContent
     * @return xpath to select elements
     */
    byte[] removeElements(byte[] xmlContent, String xpath);
}
