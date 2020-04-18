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

import com.google.common.collect.ImmutableMap;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.support.ByteArrayBuilder;
import eu.europa.ec.leos.services.support.IdGenerator;
import eu.europa.ec.leos.vo.toc.OptionsType;
import eu.europa.ec.leos.vo.toc.TocItem;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

public class XmlHelper {
    private final static byte[] OPEN_END_TAG = "</".getBytes(UTF_8);
    private final static byte[] CLOSE_TAG = ">".getBytes(UTF_8);

    public static final String OPEN_START_TAG = "<";
    public static final String MREF = "mref";
    public static final String REF = "ref";
    public static final String MARKER_ATTRIBUTE = "marker";
    
    public static final String AKNBODY = "aknbody";
    public static final String DOC = "doc";
    public static final String BILL = "bill";
    public static final String AKOMANTOSO = "akomaNtoso";
    public static final String META = "meta";
    public static final String BLOCKCONTAINER = "blockContainer";
    public static final String AUTHORIAL_NOTE = "authorialNote";
    public static final String INLINE = "inline";
    public static final String FORMULA = "formula";
    public static final String INTRO = "intro";
    public static final String HEADING = "heading";
    public static final String NUM = "num";
    
    public static final String EC = "ec";
    public static final String CN = "cn";
    
    public static final String PREFACE = "preface";
    public static final String PREAMBLE = "preamble";
    public static final String CITATIONS = "citations";
    public static final String CITATION = "citation";
    public static final String RECITALS = "recitals";
    public static final String RECITAL = "recital";
    public static final String BODY = "body";
    public static final String PART = "part";
    public static final String TITLE = "title";
    public static final String CHAPTER = "chapter";
    public static final String SECTION = "section";
    public static final String ARTICLE = "article";
    public static final String PARAGRAPH = "paragraph";
    public static final String SUBPARAGRAPH = "subparagraph";
    public static final String LIST = "list";
    public static final String POINT = "point";
    public static final String INDENT = "indent";
    public static final String SUBPOINT = "alinea";
    public static final String SUBPOINT_LABEL = "sub-point";
    public static final String CLAUSE = "clause";
    public static final String CONCLUSIONS = "conclusions";
    public static final String MAINBODY = "mainBody";
    public static final String TBLOCK = "tblock";
    public static final String LEVEL = "level";
    public static final String CONTENT = "content";
    
    private static final String ID_PLACEHOLDER = "${id}";
    private static final String ID_PLACEHOLDER_ESCAPED = "\\Q${id}\\E";
    private static final String NUM_PLACEHOLDER = "${num}";
    private static final String NUM_PLACEHOLDER_ESCAPED = "\\Q${num}\\E";
    private static final String HEADING_PLACEHOLDER = "${heading}";
    private static final String HEADING_PLACEHOLDER_ESCAPED = "\\Q${heading}\\E";
    private static final String CONTENT_TEXT_PLACEHOLDER = "${default.content.text}";
    private static final String CONTENT_TEXT_PLACEHOLDER_ESCAPED = "\\Q${default.content.text}\\E";
    
    public static final String LEVEL_NUM_SEPARATOR = ".";

    public static final List<String> ELEMENTS_TO_BE_PROCESSED_FOR_NUMBERING = Arrays.asList(ARTICLE, PARAGRAPH, POINT, INDENT);
    
    public static final String XML_NAME = "name";
    public static final String XML_DOC_TYPE = "docType";
    public static final String XML_TLC_REFERENCE = "TLCReference";
    public static final String XML_SHOW_AS = "showAs";
    public static final String ANNEX = "Annex";

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

    public static String getTemplateWithExtractedContent(TocItem tocItem, String content, MessageHelper messageHelper) {
        String template;
        if ((content != null) && (content.indexOf("<content") != -1)) {
            template = getTemplate(tocItem, ImmutableMap.of(NUM, Collections.emptyMap(), HEADING, Collections.emptyMap(), CONTENT,
                    Collections.singletonMap("<content.*?" + CONTENT_TEXT_PLACEHOLDER_ESCAPED + "</p></content>", content.substring(content.indexOf("<content"), content.indexOf("</content>") + "</content>".length()))));
        } else {
            template = getTemplate(tocItem, ImmutableMap.of(NUM, Collections.emptyMap(), HEADING, Collections.emptyMap(), CONTENT,
                    Collections.singletonMap(CONTENT_TEXT_PLACEHOLDER_ESCAPED, getDefaultContentText(tocItem.getAknTag().value(), messageHelper))));
        }
        return template;
    }

    public static String getTemplate(TocItem tocItem, MessageHelper messageHelper) {
        return getTemplate(tocItem, ImmutableMap.of(NUM, Collections.emptyMap(), HEADING, Collections.emptyMap(),
                CONTENT, Collections.singletonMap(CONTENT_TEXT_PLACEHOLDER_ESCAPED, getDefaultContentText(tocItem.getAknTag().value(), messageHelper))));
    }

    public static String getTemplate(TocItem tocItem, String num, MessageHelper messageHelper) {
        return getTemplate(tocItem, ImmutableMap.of(NUM, Collections.singletonMap(NUM_PLACEHOLDER_ESCAPED, StringUtils.isNotEmpty(num) && tocItem.isNumWithType() ? StringUtils.capitalize(tocItem.getAknTag().value()) + " " + num : num),
                HEADING, Collections.singletonMap(HEADING_PLACEHOLDER_ESCAPED, StringUtils.EMPTY), CONTENT, Collections.singletonMap(CONTENT_TEXT_PLACEHOLDER_ESCAPED, getDefaultContentText(tocItem.getAknTag().value(), messageHelper))));
    }

    public static String getTemplate(TocItem tocItem, String num, String heading, MessageHelper messageHelper) {
        return getTemplate(tocItem, ImmutableMap.of(NUM, Collections.singletonMap(NUM_PLACEHOLDER_ESCAPED, StringUtils.isNotEmpty(num) && tocItem.isNumWithType() ? StringUtils.capitalize(tocItem.getAknTag().value()) + " " + num : num),
                HEADING, Collections.singletonMap(HEADING_PLACEHOLDER_ESCAPED, heading), CONTENT, Collections.singletonMap(CONTENT_TEXT_PLACEHOLDER_ESCAPED, getDefaultContentText(tocItem.getAknTag().value(), messageHelper))));
    }

    private static String getTemplate(TocItem tocItem, Map<String, Map<String, String>> templateItems) {
        StringBuilder template = tocItem.getTemplate() != null ? new StringBuilder(tocItem.getTemplate()) : getDefaultTemplate(tocItem);
        replaceAll(template, ID_PLACEHOLDER_ESCAPED, IdGenerator.generateId("akn_" + tocItem.getAknTag().value(), 7));

        replaceTemplateItems(template, NUM, tocItem.getItemNumber(), templateItems.get(NUM));
        replaceTemplateItems(template, HEADING, tocItem.getItemHeading(), templateItems.get(HEADING));
        replaceTemplateItems(template, CONTENT, OptionsType.MANDATORY, templateItems.get(CONTENT));

        return template.toString();
    }

    private static StringBuilder getDefaultTemplate(TocItem tocItem) {
        StringBuilder defaultTemplate = new StringBuilder("<" + tocItem.getAknTag().value() + " xml:id=\"" + ID_PLACEHOLDER + "\">");
        if (OptionsType.MANDATORY.equals(tocItem.getItemNumber()) || OptionsType.OPTIONAL.equals(tocItem.getItemNumber())) {
            defaultTemplate.append(tocItem.isNumberEditable() ? "<num>" + NUM_PLACEHOLDER + "</num>" : "<num leos:editable=\"false\">" + NUM_PLACEHOLDER + "</num>");
        }
        if (OptionsType.MANDATORY.equals(tocItem.getItemHeading()) || OptionsType.OPTIONAL.equals(tocItem.getItemHeading())) {
            defaultTemplate.append("<heading>" + HEADING_PLACEHOLDER + "</heading>");
        }
        defaultTemplate.append("<content><p>" + CONTENT_TEXT_PLACEHOLDER + "</p></content></" + tocItem.getAknTag().value() + ">");
        return defaultTemplate;
    }

    private static void replaceTemplateItems(StringBuilder template, String itemName, OptionsType itemOption, Map<String, String> templateItem) {
        if (OptionsType.MANDATORY.equals(itemOption)) {
            templateItem.forEach((itemPlaceHolder, itemValue) -> {
                replaceAll(template, itemPlaceHolder, StringUtils.isEmpty(itemValue) ? "" : itemValue);
            });
        } else if (OptionsType.OPTIONAL.equals(itemOption)) {
            templateItem.forEach((itemPlaceHolder, itemValue) -> {
                if (StringUtils.isEmpty(itemValue)) {
                    replaceAll(template, "<" + itemName + ".*?" + itemPlaceHolder + "</" + itemName + ">", "");
                } else {
                    replaceAll(template, itemPlaceHolder, itemValue);
                }
            });
        }
    }

    private static void replaceAll(StringBuilder sb, String toReplace, String replacement) {
        int start = 0;
        Matcher m = Pattern.compile(toReplace).matcher(sb);
        while (m.find(start)) {
            sb.replace(m.start(), m.end(), replacement);
            start = m.start() + replacement.length();
        }
    }

    private static String getDefaultContentText(String tocTagName, MessageHelper messageHelper) {
        String defaultTextContent = messageHelper.getMessage("toc.item.template." + tocTagName + ".content.text");
        if (defaultTextContent.equals("toc.item.template." + tocTagName + ".content.text")) {
            defaultTextContent = messageHelper.getMessage("toc.item.template.default.content.text");
        }
        return defaultTextContent;
    }

    private static final ArrayList<String> prefixTobeUsedForChildren = new ArrayList<String>(Arrays.asList(ARTICLE, RECITALS, CITATIONS));
    public static String determinePrefixForChildren(String tagName, String idOfNode, String parentPrefix){
        return prefixTobeUsedForChildren.contains(tagName) ? idOfNode : parentPrefix;  //if(root Node Name is in Article/Reictals/Citations..set the prefix)
    }

    private static final ArrayList<String> nodeToSkip = new ArrayList<String>(Arrays.asList(META));
    public static  boolean skipNodeAndChildren(String tagName){
        return nodeToSkip.contains(tagName) ? true : false;
    }
    
    private static final ArrayList<String> tagNamesToSkip = new ArrayList<String>(Arrays.asList(AKOMANTOSO, BILL, "documentCollection", "doc", "attachments"));
    public static  boolean skipNodeOnly(String tagName) {
        return tagNamesToSkip.contains(tagName) ? true : false;
    }
    
    private static final ArrayList<String> parentEditableNodes= new ArrayList<String>(Arrays.asList(ARTICLE, RECITALS, CITATIONS, BLOCKCONTAINER));
    public static  boolean isParentEditableNode(String tagName) {
        return parentEditableNodes.contains(tagName) ? true : false;
    }
    
    private static final ArrayList<String> exclusionList= new ArrayList<String>(Arrays.asList(AUTHORIAL_NOTE, NUM, CLAUSE));
    public static  boolean isExcludedNode(String tagName) {
        return exclusionList.contains(tagName) ? true : false;
    }
}
