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
package eu.europa.ec.leos.services.support.xml.vtd;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.europa.ec.leos.services.compare.ContentComparatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.*;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.AKNBODY;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.AKOMANTOSO;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.BILL;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CITATIONS;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.CONCLUSIONS;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PREAMBLE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.PREFACE;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.RECITALS;

public class VTDHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(VTDHelper.class);

    //text node do not have any name by default in VTD processor.
    //we can not assign the containing node name to text node as that is considered separate node and handled differently.  
    public static final String TEXT_NODE_NAME = "TextNode";
   
    public static boolean isElementContentEqual(ContentComparatorContext context) throws NavException {

        byte[] oldContent = getFragment(context.getOldContentNavigator(), context.getOldElement(), FragmentType.ELEMENT);
        byte[] newContent = getFragment(context.getNewContentNavigator(), context.getNewElement(), FragmentType.ELEMENT);

        if(context.getThreeWayDiff() && context.getIntermediateElement() != null) {
            byte[] intermediateContent = getFragment(context.getIntermediateContentNavigator(), context.getIntermediateElement(), FragmentType.ELEMENT);
            return (Arrays.equals(oldContent, newContent) && Arrays.equals(intermediateContent, newContent));
        }
        
        return Arrays.equals(oldContent, newContent);
    }

    private static byte[] getFragment(VTDNav contentNavigator, Element element, FragmentType fragmentType) throws NavException {
        int currentIndex = contentNavigator.getCurrentIndex();
        try {
            if (element == null) {
                return new byte[0];
            }
            contentNavigator.recoverNode(element.getNavigationIndex());
            long fragmentElementContent;
            if (fragmentType.equals(FragmentType.ELEMENT) || TEXT_NODE_NAME.equals(element.getTagName())) {
                fragmentElementContent = contentNavigator.getElementFragment();
            } else if (fragmentType.equals(FragmentType.CONTENT)) {
                fragmentElementContent = contentNavigator.getContentFragment();
            } else {
                throw new IllegalArgumentException("Unknown fragment type: " + fragmentType);
            }
            int offSetOldContent = (int) fragmentElementContent;
            int lengthOldContent = (int) (fragmentElementContent >> 32);
            return (offSetOldContent != -1 && lengthOldContent != -1) ? contentNavigator.getXML().getBytes(offSetOldContent, lengthOldContent) 
                    : new byte[0];
        } catch(NavException e) {
            LOG.debug("Navigation exception occured - " + e);
            return new byte[0];
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }
    }

    public static String updateElementAttribute(String content, String attrName, String attrValue) {
        if(content.isEmpty()){
            return content;
        }
        byte[] elementContent = content.getBytes(Charset.forName("UTF-8"));
        if (attrValue != null && attrName != null) {
            try {
                VTDNav fragmentNavigator = buildXMLNavigator(content);
                int oldClassValIndex = fragmentNavigator.getAttrVal(attrName);
                XMLModifier fragmentModifier = new XMLModifier(fragmentNavigator);
                if (oldClassValIndex >= 0 && !fragmentNavigator.toRawString(oldClassValIndex).isEmpty()) {
                    fragmentModifier.updateToken(oldClassValIndex, attrValue.concat(" ").concat(fragmentNavigator.toRawString(oldClassValIndex)));
                } else {
                    fragmentModifier.insertAttribute(" ".concat(attrName).concat("=\"").concat(attrValue).concat("\""));
                }
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                fragmentModifier.output(os);
                elementContent = os.toByteArray();
                os.close();
            } catch (Exception e) {
                LOG.error("Error updating/creating attribute in element: ", e);
            }
        }
        return new String(elementContent, Charset.forName("UTF-8"));
    }

    public static  String getElementFragmentAsString(VTDNav contentNavigator, Element element) throws NavException {
        byte[] content = getFragment(contentNavigator, element, FragmentType.ELEMENT);
        return new String(content, Charset.forName("UTF-8"));
    }

    public static  String getContentFragmentAsString(VTDNav contentNavigator, Element element) throws NavException {
        byte[] content = getFragment(contentNavigator, element, FragmentType.CONTENT);
        return new String(content, Charset.forName("UTF-8"));
    }

    private static  List<Element> getAllChildElements(VTDNav contentNavigator, Map<String, Element> elementsMap, Map<String, Integer> hmIndex) throws NavException {
        Map<Integer, Element> childElements = new TreeMap<>();//maintaining a sorted list as per order of elements in XML
        int currentIndex = contentNavigator.getCurrentIndex();
        try {
            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                Element childElement = buildElement(contentNavigator, contentNavigator.getCurrentIndex(), hmIndex, elementsMap);
                childElements.put(childElement.getNavigationIndex(), childElement);

                while (contentNavigator.toElement(VTDNav.NEXT_SIBLING)) {
                    childElement = buildElement(contentNavigator, contentNavigator.getCurrentIndex(), hmIndex, elementsMap);
                    childElements.put(childElement.getNavigationIndex(), childElement);
                }
            }
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }
        return new ArrayList<>(childElements.values());
    }

    public static  Element buildElement(VTDNav contentNavigator, int currentIndex, Map<String, Integer> hmIndex, Map<String, Element> elementsMap) throws NavException {
        int offset = (int) contentNavigator.getElementFragment();
        long token = contentNavigator.getContentFragment();
        int offsetContent = (int) token;
        int lengthContent = (int) (token >> 32);
        int tokenType=contentNavigator.getTokenType(currentIndex);
        String tagContent = (offsetContent>0) 
                ?new String(contentNavigator.getXML().getBytes(offset, (offsetContent - offset)))
                :"";
        String tagName=null;
        String tagId=null;

        if(tokenType==VTDNav.TOKEN_STARTING_TAG){
            tagName=contentNavigator.toString(currentIndex);
            
            Matcher idMatcher = Pattern.compile("\\s(xml:)*(id)(\\s)*=(\\s)*\"(.+?)\"").matcher(tagContent);
            tagId= idMatcher.find()
                            ? tagContent.substring(idMatcher.start(), idMatcher.end()).concat("_").concat(tagName)
                            : null ;
            if(tagId != null){
                int attrValStartPos = tagId.indexOf("\"") + 1; //pos + 2 to get the next index after ' or "
                int attrValEndPos = tagId.indexOf("\"", attrValStartPos);
                tagId = tagId.substring(attrValStartPos, attrValEndPos);
            }
        }
        else if(tokenType==VTDNav.TOKEN_CHARACTER_DATA){//if textNode
            tagName=TEXT_NODE_NAME;
        }

        hmIndex.put(tagName, hmIndex.get(tagName)==null
                ?1
                :1 + (hmIndex.get(tagName)) );//storing the last used index of tagName
        
        boolean hasText = hasChildTextNode(contentNavigator) != -1;
        Integer nodeIndex = hmIndex.get(tagName);
        String innerText = getTextForSimilarityMatch(tagId, tagName, contentNavigator, offsetContent, lengthContent);
        List<Element> children = getAllChildElements(contentNavigator, elementsMap, hmIndex);
        if(tagId == null){
            tagId = !TEXT_NODE_NAME.equals(tagName) ? tagName.concat(nodeIndex.toString()) : null;
        }
        Element element = new Element(currentIndex, tagId, tagName, tagContent, nodeIndex, hasText, innerText, children);
        if(tagId != null){
            elementsMap.put(tagId, element);
        }
        return element;
    }
    
    public static VTDNav buildXMLNavigator(String xmlContent) throws ParseException {
        byte[] xmlBinaryContent = xmlContent.trim().getBytes(Charset.forName("UTF-8"));
        return buildXMLNavigator(xmlBinaryContent);
    }

    public static VTDNav buildXMLNavigator(byte[] xmlBinaryContent) throws ParseException {
        VTDGen vtdGen = new VTDGen();
        vtdGen.setDoc(xmlBinaryContent);
        vtdGen.parse(false);

        return vtdGen.getNav();
    }
    
    private static int hasChildTextNode(VTDNav contentNavigator) throws NavException{
        int currentIndex= contentNavigator.getCurrentIndex();
        AutoPilot ap = new AutoPilot(contentNavigator);
        int textNodeIndex=-1; 
        try{
            ap.selectXPath("text()");
            textNodeIndex=ap.evalXPath();//check for any text node.
        }catch(Exception e){
            LOG.error("Something bad happened" + contentNavigator.toString(contentNavigator.getCurrentIndex()) , e);  
        }finally{
            contentNavigator.recoverNode(currentIndex);
        }
        return textNodeIndex;
    }
    
    //tags which occur only one time in xml and contains lot of text.We avoid these tags for text similarity search by not setting the full text
    private static final ArrayList<String>  excludedTags=
            new ArrayList<>(Arrays.asList(BILL, PREFACE, AKOMANTOSO, PREAMBLE, AKNBODY, CONCLUSIONS, RECITALS, CITATIONS));
    
    private static String getTextForSimilarityMatch(String tagId, String tagName, VTDNav contentNavigator,int offsetContent, int lengthContent){
        String innerText ="";
        if( tagId==null //optimization block.. do not generate complete content in cases content matching is not performed.
                && !excludedTags.contains(tagName.toLowerCase())
                && offsetContent>0 ){ 
            innerText= new String(contentNavigator.getXML().getBytes(offsetContent, lengthContent ));
        }
        return innerText;
    }
}
