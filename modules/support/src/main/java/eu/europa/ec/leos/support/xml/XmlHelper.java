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

import java.util.ArrayList;
import java.util.Arrays;

import eu.europa.ec.leos.support.ByteArrayBuilder;

public class XmlHelper {
    private final static byte[] OPEN_END_TAG = "</".getBytes();
    private final static byte[] CLOSE_TAG = ">".getBytes();

    public static final String ARTICLE = "article";
    public static final String RECITALS = "recitals";
    public static final String CITATIONS = "citations";
    public static final String PREFACE = "preface";
    public static final String AKOMANTOSO = "akomaNtoso";
    public static final String META = "meta";
    private static final String BILL = "bill";

    public static byte[] buildTag(byte[] startTag, byte[] tagName, byte[] value) {
        ByteArrayBuilder tag = new ByteArrayBuilder();
        tag.append(startTag);
        tag.append(value);
        tag.append(OPEN_END_TAG);
        tag.append(tagName);
        tag.append(CLOSE_TAG);
        return tag.getContent();
    }

    public static String buildTag(String tagName, String value) {
        StringBuilder tag = new StringBuilder();
        tag.append("<").append(tagName).append(">");
        tag.append(value);
        tag.append("</").append(tagName).append(">");
        return tag.toString();
    }

    public static String getArticleTemplate(String num, String heading) {
        String id = IdGenerator.generateId("akn_art",7);
        StringBuilder template = new StringBuilder();
        template.append("            <article id=\"").append(id).append("\">");
        template.append("              <num>").append((num != null) ? num : "").append("</num>");
        template.append("              <heading>").append((heading != null) ? heading : "").append("</heading>");
        template.append("              <paragraph id=\"").append(id).append("-par1\">");
        template.append("                <num>1.</num>");
        template.append("                <content>");
        template.append("                  <p>Text...</p>");
        template.append("                </content>");
        template.append("              </paragraph>");
        template.append("            </article>");
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
    
    private static final ArrayList<String> tagNamesToSkip= new ArrayList<String>(Arrays.asList(AKOMANTOSO, BILL));
    public static  boolean skipNodeOnly(String tagName){
        return tagNamesToSkip.contains(tagName)?true: false;
    }

}
