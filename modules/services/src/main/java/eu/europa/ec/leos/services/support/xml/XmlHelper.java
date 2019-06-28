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

import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.support.IdGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

public class XmlHelper {
    private final static byte[] OPEN_END_TAG = "</".getBytes(UTF_8);
    private final static byte[] CLOSE_TAG = ">".getBytes(UTF_8);

    private static final String BILL = "bill";
    public static final String BODY = "body";
    public static final String AUTHORIAL_NOTE = "authorialNote";
    public static final String MREF = "mref";
    public static final String REF = "ref";
    public static final String MARKER_ATTRIBUTE = "marker";
    public static final String FORMULA = "formula";
    public static final String INTRO = "intro";
    public static final String CLAUSE = "clause";

    public static final String HEADING = "heading";
    public static final String NUM = "num";
    
    public static final String ARTICLE = "article";
    public static final String RECITALS = "recitals";
    public static final String RECITAL = "recital";
    public static final String CITATION = "citation";
    public static final String CITATIONS = "citations";
    public static final String BLOCKCONTAINER = "blockContainer";
    public static final String PREFACE = "preface";
    public static final String AKOMANTOSO = "akomaNtoso";
    public static final String META = "meta";
    
    public static final String LEOS_REF_BROKEN_ATTR = "leos:broken";

    public static final String PARAGRAPH = "paragraph";
    public static final String SUBPARAGRAPH = "subparagraph";
    public static final String LIST = "list";
    public static final String POINT = "point";
    public static final String SUBPOINT = "alinea";

    public static final String LEOS_ORIGIN_ATTR_CN = "cn";
    public static final String LEOS_ORIGIN_ATTR_EC = "ec";
    
    public static final List<String> ELEMENTS_TO_BE_PROCESSED_FOR_NUMBERING = Arrays.asList(ARTICLE, PARAGRAPH, POINT); 

    public static byte[] buildTag(byte[] startTag, byte[] tagName, byte[] value) {
        ByteArrayBuilder tag = new ByteArrayBuilder();
        tag.append(startTag);
        tag.append(value);
        if(tagName != null) {
            tag.append(OPEN_END_TAG);
            tag.append(tagName);
            tag.append(CLOSE_TAG);
        }
        return tag.getContent();
    }

    public static String buildTag(String tagName, String value) {
        StringBuilder tag = new StringBuilder();
        tag.append("<").append(tagName).append(">");
        tag.append(value);
        tag.append("</").append(tagName).append(">");
        return tag.toString();
    }

    public static String getCitationTemplate() {
        String id = IdGenerator.generateId("cit_",7);
        StringBuilder template = new StringBuilder();
        template.append("            <citation refersTo=\"~legalBasis\" xml:id=\"").append(id).append("\" leos:editable=\"true\">");
        template.append("               <p>Citation...</p>");
        template.append("            </citation>");
        return template.toString();
    }
    
    public static String getRecitalTemplate(String num) {
        String id = IdGenerator.generateId("rec_",7);
        StringBuilder template = new StringBuilder();
        template.append("            <recital xml:id=\"").append(id).append("\" leos:editable=\"true\">");
        template.append("              <num leos:editable=\"false\">").append((num != null) ? num : "").append("</num>");
        template.append("                  <p>Recital...</p>");
        template.append("            </recital>");
        return template.toString();
    }
    
    public static String getArticleTemplate(String num, String heading) {
        String id = IdGenerator.generateId("akn_art",7);
        StringBuilder template = new StringBuilder();
        template.append("            <article xml:id=\"").append(id).append("\" leos:editable=\"true\"  leos:deletable=\"true\">");
        template.append("              <num leos:editable=\"false\">").append("Article ").append((num != null) ? num : "").append("</num>");
        template.append("              <heading>").append((heading != null) ? heading : "").append("</heading>");
        template.append("              <paragraph xml:id=\"").append(id).append("-par1\">");
        template.append("                <num>1.</num>");
        template.append("                <content>");
        template.append("                  <p>Text...</p>");
        template.append("                </content>");
        template.append("              </paragraph>");
        template.append("            </article>");
        return template.toString();
    }

    public static String getParagraphTemplate(String num) {
        String id = IdGenerator.generateId("akn_art_para",7);
        StringBuilder template = new StringBuilder();
        template.append("              <paragraph xml:id=\"").append(id).append("\">");
        if(num != null) {
            template.append("                <num leos:editable=\"false\">").append(num).append("</num>");
        }
        template.append("                <content>");
        template.append("                  <p>Text...</p>");
        template.append("                </content>");
        template.append("              </paragraph>");
        return template.toString();
    }

    public static String getSubParagraphTemplate(String content) {
        String id = IdGenerator.generateId("akn_art_subpara",7);
        StringBuilder template = new StringBuilder();
        template.append("              <subparagraph xml:id=\"").append(id).append("\">");
        
        if (content != null) {
        	if (content.indexOf("<content") != -1) {
        		template.append(content, content.indexOf("<content"), content.indexOf("</content>") + "</content>".length());
        	} else {
                template.append("                <content>");
                template.append("                  <p></p>");
                template.append("                </content>");
        	}
        } else {
            template.append("                <content>");
            template.append("                  <p>Text...</p>");
            template.append("                </content>");
        }
        
        template.append("              </subparagraph>");
        return template.toString();
    }

    public static String getSubpointTemplate(String content) {
        String id = IdGenerator.generateId("akn_art_alinea",7);
        StringBuilder template = new StringBuilder();
        template.append("              <alinea xml:id=\"").append(id).append("\">");

        if (content != null) {
        	if (content.indexOf("<content") != -1) {
        		template.append(content, content.indexOf("<content"), content.indexOf("</content>") + "</content>".length());
        	} else {
                template.append("                <content>");
                template.append("                  <p></p>");
                template.append("                </content>");
        	}
        } else {
            template.append("                <content>");
            template.append("                  <p>Text...</p>");
            template.append("                </content>");
        }
        
        template.append("              </alinea>");
        return template.toString();
    }

    public static String getPointTemplate(String num) {
        String id = IdGenerator.generateId("akn_art_point",7);
        StringBuilder template = new StringBuilder();
        template.append("              <point xml:id=\"").append(id).append("\">");
        template.append("                <num leos:editable=\"false\">").append(num).append("</num>");
        template.append("                <content>");
        template.append("                  <p>Text...</p>");
        template.append("                </content>");
        template.append("              </point>");
        return template.toString();
    }
    
    public static String getAnnexTemplate() {
        String id = IdGenerator.generateId("akn_annex",7);
        StringBuilder template = new StringBuilder();
        template.append("         <division xml:id=\"").append(id).append("\" leos:editable=\"true\"  leos:deletable=\"true\">");
        template.append("                <content>");
        template.append("                  <p xml:id=\"").append(id).append("_par1\">");
        template.append("                     Text...");
        template.append("                  </p>");
        template.append("                </content>");
        template.append("         </division>");
        return template.toString();
    }
    
    public static String stripAllTags(String tag) {
        return tag.replaceAll("<[^>]+>", " ");
    }

    private static final ArrayList<String> prefixTobeUsedForChildren= new ArrayList<String>(Arrays.asList(ARTICLE, RECITALS, CITATIONS));
    public static String determinePrefixForChildren(String tagName,String idOfNode,String parentPrefix){
        return prefixTobeUsedForChildren.contains(tagName)?idOfNode: parentPrefix;  //if(root Node Name is in Article/Reictals/Citations..set the prefix)
    }

    private static final ArrayList<String> nodeToSkip= new ArrayList<String>(Arrays.asList(META));
    public static  boolean skipNodeAndChildren(String tagName){
        return nodeToSkip.contains(tagName)?true: false;
    }
    
    private static final ArrayList<String> tagNamesToSkip= new ArrayList<String>(Arrays.asList(AKOMANTOSO, BILL, "documentCollection", "doc", "attachments"));
    public static  boolean skipNodeOnly(String tagName){
        return tagNamesToSkip.contains(tagName)?true: false;
    }
    
    private static final ArrayList<String> parentEditableNodes= new ArrayList<String>(Arrays.asList(ARTICLE, RECITALS, CITATIONS, BLOCKCONTAINER));
    public static  boolean isParentEditableNode(String tagName){
        return parentEditableNodes.contains(tagName)?true: false;
    }
    
    private static final ArrayList<String> exclusionList= new ArrayList<String>(Arrays.asList(AUTHORIAL_NOTE, NUM, CLAUSE));
    public static  boolean isExcludedNode(String tagName) {
        return exclusionList.contains(tagName)?true: false;
    }
}
