/*
 * Copyright 2017 European Commission
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

import static eu.europa.ec.leos.services.compare.ContentComparatorService.CONTENT_ADDED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.CONTENT_REMOVED_CLASS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import difflib.*;

class ContentComparatorHelper {

    private static final String TAGS_NOT_TO_SPLIT = "img,authorialnote,inline,ref";
    private static final List<String> TAGS_NOT_TO_SPLIT_LIST = new ArrayList<>(Arrays.asList(TAGS_NOT_TO_SPLIT.split(",")));

    public String compareHtmlContents(String firstContent, String secondContent) {

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
                    writeInsertLine(diffBuilder, line);
                }
                continue;
            }

            // Deleted DiffRow
            if (delta.getClass().equals(DeleteDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) orig.getLines()) {
                    // handle delete lines
                    writeRemovedLine(diffBuilder, line);
                }
                continue;
            }

            // catch now changed line
            if (orig.size() == rev.size()) {
                for (int j = 0; j < orig.size(); j++) {
                    String oldLine = (String) orig.getLines().get(j);
                    String newLine = (String) rev.getLines().get(j);

                    handleChangedLine(oldLine, newLine, diffBuilder, diffBuilder);
                }
            } else if (orig.size() > rev.size()) {
                for (int j = 0; j < orig.size(); j++) {
                    String oldLine = (String) orig.getLines().get(j);
                    String newLine = rev.getLines().size() > j ? (String) rev.getLines().get(j) : "";
                    handleChangedLine(oldLine, newLine, diffBuilder, diffBuilder);
                }
            } else {
                for (int j = 0; j < rev.size(); j++) {
                    String oldLine = orig.getLines().size() > j ? (String) orig.getLines().get(j) : "";
                    String newLine = (String) rev.getLines().get(j);
                    handleChangedLine(oldLine, newLine, diffBuilder, diffBuilder);
                }
            }
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
     * @param content
     * @return List<String>
     */
    private List<String> contentToLines(String content) {
        List<String> listAllElements = new ArrayList<>();
        Document doc = Jsoup.parse(content);
        doc.outputSettings().indentAmount(0).prettyPrint(false);
        Element body = doc.body();
        List<Node> contentNodes = body.childNodes();
        for (Node node : contentNodes) {
            if (node.getClass().isAssignableFrom(TextNode.class)) {
                Collections.addAll(listAllElements, splitTextInWords(((TextNode) node).getWholeText()));
            } else if (node.getClass().isAssignableFrom(Element.class)) {
                Element nodeEl = (Element) node;
                if (TAGS_NOT_TO_SPLIT_LIST.contains(node.nodeName())) {
                    // element that must not be splited
                    if (nodeEl.tag().isSelfClosing() && nodeEl.tag().isEmpty()) {
                        listAllElements.add(node.toString() + "</" + nodeEl.tagName() + ">");
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
     * @param text
     * @return String[]
     */
    private String[] splitTextInWords(String text) {
        return text.split("(?=\\s)");
    }

    public String[] twoColumnsCompareHtmlContents(String firstContent, String secondContent) {

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
                    writeInsertLine(trackChangesAmendmentBuilder, line);

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

                    writeRemovedLine(trackChangesOriginalBuilder, line);

                    // in the right side, write empty spaces corresponding to the content of the previous line
                    // writeEmptySpacesForLine(trackChangesAmendmentBuilder, line);
                }
                continue;
            }

            // catch now changed line
            if (orig.size() == rev.size()) {
                for (int j = 0; j < orig.size(); j++) {
                    String oldLine = (String) orig.getLines().get(j);
                    String newLine = (String) rev.getLines().get(j);

                    handleChangedLine(oldLine, newLine, trackChangesOriginalBuilder, trackChangesAmendmentBuilder);
                }
            } else if (orig.size() > rev.size()) {
                for (int j = 0; j < orig.size(); j++) {
                    String oldLine = (String) orig.getLines().get(j);
                    String newLine = rev.getLines().size() > j ? (String) rev.getLines().get(j) : "";
                    handleChangedLine(oldLine, newLine, trackChangesOriginalBuilder, trackChangesAmendmentBuilder);
                }
            } else {
                for (int j = 0; j < rev.size(); j++) {
                    String oldLine = orig.getLines().size() > j ? (String) orig.getLines().get(j) : "";
                    String newLine = (String) rev.getLines().get(j);
                    handleChangedLine(oldLine, newLine, trackChangesOriginalBuilder, trackChangesAmendmentBuilder);
                }
            }
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

    private void writeInsertLine(StringBuilder out, String newLine) {
        if (!newLine.isEmpty()) {
            if (isHtmlLine(newLine)) {
                if (isHtmlStartTag(newLine)) {
                    out.append("<span class=\"" + CONTENT_ADDED_CLASS + "\">");
                }
                out.append(newLine);

                if (isHtmlEndTag(newLine) || isHtmlStartToEndLine(newLine)) {
                    out.append("</span>");
                }
            } else {
                out.append("<span class=\"" + CONTENT_ADDED_CLASS + "\">");
                out.append(newLine);
                out.append("</span>");
            }
        }
    }

    private void writeRemovedLine(StringBuilder out, String oldLine) {
        if (!oldLine.isEmpty()) {
            if (isHtmlLine(oldLine)) {
                if (isHtmlStartTag(oldLine)) {
                    out.append("<span class=\"" + CONTENT_REMOVED_CLASS + "\">");
                }
                out.append(oldLine);
                if (isHtmlEndTag(oldLine) || isHtmlStartToEndLine(oldLine)) {
                    out.append("</span>");
                }
            } else {
                out.append("<span class=\"" + CONTENT_REMOVED_CLASS + "\">");
                out.append(oldLine);
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
        StringBuffer sb = new StringBuffer();
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
        StringBuffer sb = new StringBuffer();
        while (index >= 0 && sb.length() < 2) {
            if (!Character.isWhitespace(line.charAt(index))) {
                sb.append(line.charAt(index));
            }
            index--;
        }
        return sb.reverse().toString();
    }

    private void handleChangedLine(String oldLine, String newLine, StringBuilder trackChangesOriginalBuilder, StringBuilder trackChangesAmendmentBuilder) {

        // write in the left side the removed line
        writeRemovedLine(trackChangesOriginalBuilder, oldLine);
        // in the right side, write empty spaces corresponding to the content of the previous line
        // writeEmptySpacesForLine(trackChangesAmendmentBuilder, oldLine);

        // write in the right side the new line
        writeInsertLine(trackChangesAmendmentBuilder, newLine);
        // in the left side, write empty spaces corresponding to the content of the previous line
        // writeEmptySpacesForLine(trackChangesOriginalBuilder, newLine);
    }
}