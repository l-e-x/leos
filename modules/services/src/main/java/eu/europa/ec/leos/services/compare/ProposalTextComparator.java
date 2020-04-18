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

import difflib.Chunk;
import difflib.DeleteDelta;
import difflib.Delta;
import difflib.DiffUtils;
import difflib.InsertDelta;
import difflib.Patch;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.ATTR_NAME;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.CONTENT_ADDED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.CONTENT_REMOVED_CLASS;

class ProposalTextComparator implements TextComparator {

    private static final String TAGS_NOT_TO_SPLIT = "img,authorialnote,inline,mref,ref";
    private static final List<String> TAGS_NOT_TO_SPLIT_LIST = new ArrayList<>(Arrays.asList(TAGS_NOT_TO_SPLIT.split(",")));

    @Override
    public String compareTextNodeContents(String firstContent, String secondContent, String intermediateContent, ContentComparatorContext context) {
        return compareTextNodeContentsTwoWayDiff(firstContent, secondContent, context);
    }
    
    private String compareTextNodeContentsTwoWayDiff(String firstContent, String secondContent, ContentComparatorContext context) {
        List<String> originalList = contentToLines(firstContent);
        List<String> revisedList = contentToLines(secondContent);
        StringBuilder diffBuilder = new StringBuilder();
    
        Patch patch = DiffUtils.diff(originalList, revisedList);
        final List<Delta> deltaList = patch.getDeltas();
    
        int endPos = 0;
    
        for (Delta delta : deltaList) {
    
            Chunk orig = delta.getOriginal();
            Chunk rev = delta.getRevised();
    
            // catch the equal prefix for each chunk
            for (String line : originalList.subList(endPos, orig.getPosition())) {
                // handle equal lines
                writeUnchangedLine(diffBuilder, line);
            }
            // handle Inserted rows
            if (delta.getClass().equals(InsertDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) rev.getLines()) {
                    // handle insert line
                    writeChangedLine(diffBuilder, line, context.getAttrName(), context.getAddedValue());
                }
                continue;
            }
    
            // Deleted DiffRow
            if (delta.getClass().equals(DeleteDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) orig.getLines()) {
                    // handle delete lines
                    writeChangedLine(diffBuilder, line, context.getAttrName(), context.getRemovedValue());
                }
                continue;
            }
    
            // catch now changed line
            catchChangedLine(orig, rev, diffBuilder, diffBuilder, context.getAttrName(), context.getRemovedValue(), context.getAddedValue());
            endPos = orig.last() + 1;
    
        }
    
        // Copy the final matching chunk if any.
        for (String line : originalList.subList(endPos, originalList.size())) {
            writeUnchangedLine(diffBuilder, line);
        }
    
        return diffBuilder.toString();
    }

    /**
     * We consider some elements as a whole. like img, authorialnote, see TAGS_NOT_TO_SPLIT.
     *
     * @param content to be split into lines
     * @return List<String>
     */
    private List<String> contentToLines(String content) {
        List<String> listAllElements = new ArrayList<>();
        Document doc = Jsoup.parse(content, "", Parser.xmlParser());
        doc.outputSettings().indentAmount(0).prettyPrint(false);
        Node root = doc.root();
        List<Node> contentNodes = root.childNodes();
        for (Node node : contentNodes) {
            if (node.getClass().isAssignableFrom(TextNode.class)) {
                Collections.addAll(listAllElements, splitTextInWords(((TextNode) node).getWholeText()));
            } else if (node.getClass().isAssignableFrom(Element.class)) {
                Element nodeEl = (Element) node;
                if (TAGS_NOT_TO_SPLIT_LIST.contains(node.nodeName().toLowerCase())) {
                    // element that must not be splited
                    if (nodeEl.tag().isSelfClosing() && nodeEl.tag().isEmpty()) {
                        listAllElements.add(
                                "<" + node.nodeName() + ((node.attributes().size() == 0) ? "" : node.attributes().toString()) + "></" + node.nodeName() + ">");
                    } else {
                        listAllElements.add(node.toString());
                    }
                } else {
                    // element with one or more children.
                    listAllElements.add("<" + node.nodeName() + ((node.attributes().size() == 0) ? "" : node.attributes().toString()) + ">");
                    listAllElements.addAll(contentToLines(((Element) node).html()));
                    listAllElements.add("</" + node.nodeName() + ">");
                }
            }
        }
        return listAllElements;
    }

    /**
     *  Provide further splitting at word level
     * @param text to be split into words
     * @return String[]
     */
    private String[] splitTextInWords(String text) {
        return text.split("(?=\\s)");
    }

    @Override
    public String[] twoColumnsCompareTextNodeContents(String firstContent, String secondContent) {

        List<String> originalList = contentToLines(firstContent);
        List<String> revisedList = contentToLines(secondContent);

        Patch patch = DiffUtils.diff(originalList, revisedList);

        StringBuilder trackChangesOriginalBuilder = new StringBuilder((int) (firstContent.length() * 1.5));
        StringBuilder trackChangesAmendmentBuilder = new StringBuilder((int) (secondContent.length() * 1.5));

        int endPos = 0;
        final List<Delta> deltaList = patch.getDeltas();

        for (Delta delta : deltaList) {

            Chunk orig = delta.getOriginal();
            Chunk rev = delta.getRevised();

            // catch the equal prefix for each chunk
            for (String line : originalList.subList(endPos, orig.getPosition())) {
                // handle equal lines
                // System.out.println("EQUAL detected..");
                writeUnchangedLine(trackChangesOriginalBuilder, line);
                writeUnchangedLine(trackChangesAmendmentBuilder, line);
            }

            // handle Inserted rows
            if (delta.getClass().equals(InsertDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) rev.getLines()) {

                    // handle insert line
                    writeChangedLine(trackChangesAmendmentBuilder, line, ATTR_NAME, CONTENT_ADDED_CLASS);

                    // in the left side, write empty spaces corresponding to the content of the previous line
                    // writeEmptySpacesForLine(trackChangesOriginalBuilder, line);
                }
                continue;
            }

            // Deleted Diff row
            if (delta.getClass().equals(DeleteDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) orig.getLines()) {
                    // handle delete lines

                    writeChangedLine(trackChangesOriginalBuilder, line, ATTR_NAME, CONTENT_REMOVED_CLASS);

                    // in the right side, write empty spaces corresponding to the content of the previous line
                    // writeEmptySpacesForLine(trackChangesAmendmentBuilder, line);
                }
                continue;
            }

            // catch now changed line
            catchChangedLine(orig, rev, trackChangesOriginalBuilder, trackChangesAmendmentBuilder, ATTR_NAME, CONTENT_REMOVED_CLASS, CONTENT_ADDED_CLASS);
            endPos = orig.last() + 1;

        }

        // Copy the final matching chunk if any.
        for (String line : originalList.subList(endPos, originalList.size())) {
            writeUnchangedLine(trackChangesOriginalBuilder, line);
            writeUnchangedLine(trackChangesAmendmentBuilder, line);
        }

        String trackChangesOriginal = trackChangesOriginalBuilder.toString();
        String trackChangesAmendment = trackChangesAmendmentBuilder.toString();

        return new String[]{trackChangesOriginal, trackChangesAmendment};
    }

    private void writeUnchangedLine(StringBuilder out, String line) {
        if (!line.isEmpty()) {
            out.append(line);
        }
    }

    private void writeChangedLine(StringBuilder out, String newLine, String attrName, String attrValue) {
        if (!newLine.isEmpty()) {
            if (isHtmlLine(newLine)) {
                if (isHtmlStartTag(newLine)) {
                    out.append("<span ").append(attrName).append("=\"").append(attrValue).append("\">");
                }
                out.append(newLine);

                if (isHtmlEndTag(newLine) || isHtmlStartToEndLine(newLine)) {
                    out.append("</span>");
                }
            } else {
                out.append("<span ").append(attrName).append("=\"").append(attrValue).append("\">");
                out.append(newLine);
                out.append("</span>");
            }
        }
    }

    private boolean isHtmlLine(String line) {
        return line.length() > 0 && line.charAt(0) == '<';
    }

    private boolean isHtmlStartToEndLine(String line) {
        return line.length() > 0 && line.charAt(0) == '<' && (line.contains("</") || "/>".equals(getLastTwoChars(line)));
    }

    private boolean isHtmlStartTag(String line) {
        String twoChars = getFirstTwoChars(line);
        return twoChars.length() > 1 && twoChars.charAt(0) == '<' && twoChars.charAt(1) != '/';
    }

    private boolean isHtmlEndTag(String line) {
        return "</".equals(getFirstTwoChars(line)) || "/>".equals(getLastTwoChars(line));
    }

    private String getFirstTwoChars(final String line) {
        int index = 0;
        StringBuilder sb = new StringBuilder();
        while (line.length() >= index && sb.length() < 2) {
            if (!Character.isWhitespace(line.charAt(index))) {
                sb.append(line.charAt(index));
            }
            index++;
        }
        return sb.toString();
    }

    private String getLastTwoChars(final String line) {
        int index = line.length() - 1;
        StringBuilder sb = new StringBuilder();
        while (index >= 0 && sb.length() < 2) {
            if (!Character.isWhitespace(line.charAt(index))) {
                sb.append(line.charAt(index));
            }
            index--;
        }
        return sb.reverse().toString();
    }

    private void catchChangedLine(Chunk orig, Chunk rev, StringBuilder trackChangesOriginalBuilder, StringBuilder trackChangesAmendmentBuilder, String attrName,
            String removedValue, String addedValue) {
        if (orig.size() == rev.size()) {
            for (int j = 0; j < orig.size(); j++) {
                handleChangedLine((String) orig.getLines().get(j),
                        (String) rev.getLines().get(j),
                        trackChangesOriginalBuilder, trackChangesAmendmentBuilder, attrName, removedValue, addedValue);
            }
        } else if (orig.size() > rev.size()) {
            for (int j = 0; j < orig.size(); j++) {
                handleChangedLine((String) orig.getLines().get(j),
                        rev.getLines().size() > j ? (String) rev.getLines().get(j) : "",
                        trackChangesOriginalBuilder, trackChangesAmendmentBuilder, attrName, removedValue, addedValue);
            }
        } else {
            for (int j = 0; j < rev.size(); j++) {
                handleChangedLine(orig.getLines().size() > j ? (String) orig.getLines().get(j) : "",
                        (String) rev.getLines().get(j),
                        trackChangesOriginalBuilder, trackChangesAmendmentBuilder, attrName, removedValue, addedValue);
            }
        }
    }

    private void handleChangedLine(String oldLine, String newLine, StringBuilder trackChangesOriginalBuilder, StringBuilder trackChangesAmendmentBuilder,
            String attrName, String removedValue, String addedValue) {

        // write in the left side the removed line
        writeChangedLine(trackChangesOriginalBuilder, oldLine, attrName, removedValue);
        // in the right side, write empty spaces corresponding to the content of the previous line
        // writeEmptySpacesForLine(trackChangesAmendmentBuilder, oldLine);

        // write in the right side the new line
        writeChangedLine(trackChangesAmendmentBuilder, newLine, attrName, addedValue);
        // in the left side, write empty spaces corresponding to the content of the previous line
        // writeEmptySpacesForLine(trackChangesOriginalBuilder, newLine);
    }
}