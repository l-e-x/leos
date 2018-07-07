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
package eu.europa.ec.leos.support.xml.vtd;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.ParseException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;

/**
 * @author: micleva
 * @date: 4/25/13 8:53 AM
 * @project: ETX
 */
public class VTDHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(VTDHelper.class);

    //text node do not have any name by default in VTD processor.
    //we can not assign the containing node name to text node as that is considered separate node and handled differently.  
    public static final String TEXT_NODE_NAME = "TextNode";
   
    public static boolean isElementContentEqual(VTDNav oldContentNavigator, Element oldElement, VTDNav newContentNavigator, Element newElement) throws NavException {
        byte[] oldContent = getFragment(oldContentNavigator, oldElement, FragmentType.ELEMENT);
        byte[] newContent = getFragment(newContentNavigator, newElement, FragmentType.ELEMENT);

        return Arrays.equals(oldContent, newContent);
    }

    private static byte[] getFragment(VTDNav contentNavigator, Element element, FragmentType fragmentType) throws NavException {
        int currentIndex = contentNavigator.getCurrentIndex();
        try {
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

            return contentNavigator.getXML().getBytes(offSetOldContent, lengthOldContent);
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }
    }

    public static  String getElementFragmentAsString(VTDNav contentNavigator, Element element) throws NavException {
        byte[] content = getFragment(contentNavigator, element, FragmentType.ELEMENT);
        return new String(content, Charset.forName("UTF-8"));
    }

    public static  String getContentFragmentAsString(VTDNav contentNavigator, Element element) throws NavException {
        byte[] content = getFragment(contentNavigator, element, FragmentType.CONTENT);
        return new String(content, Charset.forName("UTF-8"));
    }

    public static  List<Element> getAllChildElements(VTDNav contentNavigator) throws NavException {
        TreeMap <Integer, Element> childElements = new TreeMap <Integer, Element>();//maintaining a sorted list as per order of elements in XML
        int currentIndex = contentNavigator.getCurrentIndex();
        HashMap<String, Integer> hmIndex= new HashMap<String, Integer>();
        try {
            
            if (contentNavigator.toElement(VTDNav.FIRST_CHILD)) {
                Element childElement = buildElement(contentNavigator, contentNavigator.getCurrentIndex(), hmIndex);
                childElements.put(childElement.getNavigationIndex(),childElement);
                
                while (contentNavigator.toElement(VTDNav.NEXT_SIBLING)) {
                    childElement = buildElement(contentNavigator, contentNavigator.getCurrentIndex(), hmIndex);
                    childElements.put(childElement.getNavigationIndex(),childElement);
                }
            }
        } finally {
            contentNavigator.recoverNode(currentIndex);
        }
        return new ArrayList<Element>(childElements.values());
    }

    public static  Element buildElement(VTDNav contentNavigator, int currentIndex, HashMap<String, Integer> hmIndex) throws NavException {
        int offset = (int) contentNavigator.getElementFragment();
        long token = (long) contentNavigator.getContentFragment();
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
            
            Matcher idMatcher = Pattern.compile("\\s(id)(\\s)*=(\\s)*\"(.+?)\"").matcher(tagContent);
            tagId= idMatcher.find()
                            ? tagName + tagContent.substring(idMatcher.start(), idMatcher.end())
                            : null ;
        }
        else if(tokenType==VTDNav.TOKEN_CHARACTER_DATA){//if textNode
            tagName=TEXT_NODE_NAME;
        }

        hmIndex.put(tagName, hmIndex.get(tagName)==null
                ?1
                :1 + (hmIndex.get(tagName)) );//storing the last used index of tagName
        
        boolean hasText = hasChildTextNode(contentNavigator) != -1;
        String innerText = getTextForSimilarityMatch(tagId, tagName, contentNavigator, offsetContent, lengthContent);
        return new Element(currentIndex, tagId, tagName, tagContent, hmIndex.get(tagName), hasText, innerText);
    }
    
    public static VTDNav buildXMLNavigator(String xmlContent) throws ParseException {
        byte[] xmlBinaryContent = xmlContent.trim().getBytes(Charset.forName("UTF-8"));
        VTDGen vtdGen = new VTDGen();
        vtdGen.setDoc(xmlBinaryContent);
        vtdGen.parse(false);

        return vtdGen.getNav();
    }
    public static int hasChildTextNode(VTDNav contentNavigator) throws NavException{
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
    public static final ArrayList<String>  excludedTags=
            new ArrayList<String>(Arrays.asList("bill","preface","akomantoso","preamble","aknbody","conclusions","recitals","citations"));
    
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
