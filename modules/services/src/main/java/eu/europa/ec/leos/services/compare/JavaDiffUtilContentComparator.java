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
package eu.europa.ec.leos.services.compare;

import difflib.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: micleva
 * @date: 4/2/13 2:52 PM
 * @project: ETX
 */

public class JavaDiffUtilContentComparator implements ContentComparatorService {

    @Override
    public String compareHtmlContents(String firstContent, String secondContent) {
        
        //removing the authorial notes and replacing them with single XHTML node.
        HashMap<String, String> hm=new HashMap<String, String>(); //<single XHTML node, original AuthNote> note mapping
        firstContent=encodeAuthNotes(firstContent,hm);
        secondContent=encodeAuthNotes(secondContent,hm);
        
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
                //handle equal lines
                writeUnchangedLine(diffBuilder, line);
            }

            // handle Inserted rows
            if (delta.getClass().equals(InsertDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) rev.getLines()) {
                    //handle insert line
                    writeInsertLine(diffBuilder, line);
                }
                continue;
            }

            // Deleted DiffRow
            if (delta.getClass().equals(DeleteDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) orig.getLines()) {
                    //handle delete lines
                    writeRemovedLine(diffBuilder, line);
                }
                continue;
            }

            //catch now changed line
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
        String result = diffBuilder.toString();
        
        result= decodeAuthNotes(result, hm);
        
        return result;
    }

    @Override
    public String[] twoColumnsCompareHtmlContents(String firstContent, String secondContent) {

        //removing the authorial notes and replacing them with single XHTML node.
        HashMap<String, String> hm=new HashMap<String, String>();
        firstContent=encodeAuthNotes(firstContent,hm);
        secondContent=encodeAuthNotes(secondContent,hm);
        
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
                //handle equal lines
//                System.out.println("EQUAL detected..");
                writeUnchangedLine(trackChangesOriginalBuilder, line);
                writeUnchangedLine(trackChangesAmendmentBuilder, line);
            }

            // handle Inserted rows
            if (delta.getClass().equals(InsertDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) rev.getLines()) {

                    //handle insert line
                    writeInsertLine(trackChangesAmendmentBuilder, line);

                    //in the left side, write empty spaces corresponding to the content of the previous line
//                        writeEmptySpacesForLine(trackChangesOriginalBuilder, line);
                }
                continue;
            }

            // Deleted Diff row
            if (delta.getClass().equals(DeleteDelta.class)) {
                endPos = orig.last() + 1;
                for (String line : (List<String>) orig.getLines()) {
                    //handle delete lines

                    writeRemovedLine(trackChangesOriginalBuilder, line);

                    //in the right side, write empty spaces corresponding to the content of the previous line
//                        writeEmptySpacesForLine(trackChangesAmendmentBuilder, line);
                }
                continue;
            }

            //catch now changed line
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
        
        //puttin back authnotes
        trackChangesOriginal=decodeAuthNotes(trackChangesOriginal, hm);
        trackChangesAmendment=decodeAuthNotes(trackChangesAmendment, hm);
        
        return new String[]{trackChangesOriginal, trackChangesAmendment};
    }

    private void writeUnchangedLine(StringBuilder out, String line) {
        if (!line.isEmpty()) {
            boolean isHtmlLine = isHtmlLine(line);
            if (isHtmlLine) {
                out.append(line);
                out.append('\n');
            } else {
                out.append(line);
                out.append(' ');
            }
        }
    }

    private void writeInsertLine(StringBuilder out, String newLine) {
        if (!newLine.isEmpty()) {
            if (isHtmlLine(newLine)) {
                if(isHtmlStartTag(newLine)){
                    out.append("<span class=\"" + CONTENT_ADDED_CLASS + "\">");
                }
                out.append(newLine);

                if(isHtmlEndTag(newLine)){
                    out.append("</span>");
                }
                out.append('\n');
            } else {
                out.append("<span class=\"" + CONTENT_ADDED_CLASS + "\">");
                out.append(' ');
                out.append(newLine);
                out.append(' ');
                out.append("</span>");
                out.append(' ');
            }
        }
    }

    private void writeRemovedLine(StringBuilder out, String oldLine) {
        if (!oldLine.isEmpty()) {
            if (isHtmlLine(oldLine)) {
                if(isHtmlStartTag(oldLine)){
                    out.append("<span class=\"" + CONTENT_REMOVED_CLASS + "\">"); 
                }
                out.append(oldLine);
                if(isHtmlEndTag(oldLine)){
                    out.append("</span>");
                }
                out.append('\n');
            } else {
                out.append("<span class=\"" + CONTENT_REMOVED_CLASS + "\">");
                out.append(' ');
                out.append(oldLine);
                out.append(' ');
                out.append("</span>");
                out.append(' ');
            }
        }

    }

    private boolean isHtmlLine(String line) {
        return line.length() > 0 && line.charAt(0) == '<';
    }

    private boolean isHtmlStartTag(String line) {
        String twoChars=getFirstTwoChars(line);
        return twoChars.length() > 1 && twoChars.charAt(0)=='<' &&  twoChars.charAt(1) != '/';
    }

    
    private boolean isHtmlEndTag(String line) {
        return "</".equals(getFirstTwoChars(line)) || "/>".equals(getLastTwoChars(line));
    }
    
    private String getFirstTwoChars(final String line){
        int index=0;
        StringBuffer sb=new StringBuffer();
        while(line.length()>=index && sb.length() < 2){
            if(!Character.isWhitespace(line.charAt(index))){
                sb.append(line.charAt(index));
            }
            index++;
        }
        return sb.toString();
    }

    private String getLastTwoChars(final String line){
        int index=line.length()-1;
        StringBuffer sb=new StringBuffer();
        while(index >= 0 && sb.length() < 2){
            if(!Character.isWhitespace(line.charAt(index))){
                sb.append(line.charAt(index));
            }
            index--;
        }
        return sb.reverse().toString();
    }
    
    private void handleChangedLine(String oldLine, String newLine, StringBuilder trackChangesOriginalBuilder, StringBuilder trackChangesAmendmentBuilder) {

        //write in the left side the removed line
        writeRemovedLine(trackChangesOriginalBuilder, oldLine);
        //in the right side, write empty spaces corresponding to the content of the previous line
//            writeEmptySpacesForLine(trackChangesAmendmentBuilder, oldLine);

        // write in the right side the new line
        writeInsertLine(trackChangesAmendmentBuilder, newLine);
        //in the left side, write empty spaces corresponding to the content of the previous line
//            writeEmptySpacesForLine(trackChangesOriginalBuilder, newLine);
    }

    private void writeEmptySpacesForLine(StringBuilder trackChangesBuilder, String line) {
        for(int i = 0; i < line.length(); i++) {
            trackChangesBuilder.append(line).append("&nbsp;");
        }
    }

    /*private void completeWithEmptySpaces(StringBuilder trackChangesOriginalBuilder, String oldLine, StringBuilder trackChangesAmendmentBuilder, String newLine) {
        int newLineLength = newLine.length();
        int oldLineLength = oldLine.length();
        if(newLineLength > oldLineLength) {
            for(int i = oldLineLength; i < newLineLength + 1; i++) {
                trackChangesOriginalBuilder.append("&nbsp;");
            }
        } else {
            for(int i = newLineLength; i < oldLineLength + 1; i++) {
                trackChangesAmendmentBuilder.append("&nbsp;");
            }
        }
    }*/

    private List<String> contentToLines(String content) {
        List<String> linesList = new ArrayList<>();

        String str = content;
        str = str.replaceAll("<", "\n<");
        str = str.replaceAll(">", ">\n");
        String[] lines = str.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.equals("")) {
                if (isHtmlLine(trimmed)) {
                    linesList.add(trimmed);
                } else {
                    //provide further splitting at word level for non-html lines
                    String[] words = trimmed.split(" ");
                    Collections.addAll(linesList, words);
                }
            }
        }

        return linesList;
    }
    
    private String encodeAuthNotes(String str, HashMap<String, String> hm){
        StringBuffer sb=new StringBuffer();
        Pattern idPattern = Pattern.compile("id(\\s)*=(\\s)*\"(.+?)\"");
        Pattern textPattern= Pattern.compile("data-tooltip(\\s)*=(\\s)*\"(.+?)\"");
        Pattern markerPattern = Pattern.compile(">(\\s)*(.+?)(\\s)*<");

        Matcher authNoteMatcher = Pattern.compile("<authorialNote.*?</authorialNote>").matcher(str);
        int indexLastHandled=0;
        while(authNoteMatcher.find()){
                sb.append(str.substring(indexLastHandled, authNoteMatcher.start()));
                String aNote=str.substring(authNoteMatcher.start(), authNoteMatcher.end());
                Matcher idMatcher=idPattern.matcher(aNote);
                if(idMatcher.find()){
                    String id=idMatcher.group(3);
                    
                    Matcher textMatcher=textPattern.matcher(aNote);
                    String textContent="";
                    if(textMatcher.find()){
                        textContent=textMatcher.group(3);
                    }
                    
                    Matcher markerMatcher=markerPattern.matcher(aNote);
                    String marker="";
                    if(markerMatcher.find()){
                        marker=markerMatcher.group(2);
                    }
                    
                    String replacement="<Anode"+id+marker+textContent+"/>";
                    hm.put(replacement,aNote);
                    sb.append(replacement);
                }
                indexLastHandled=authNoteMatcher.end();
        }
        sb.append(str.substring(indexLastHandled, str.length()));
        
        return sb.toString();
    }
    
    private String decodeAuthNotes(String str, HashMap<String, String> hm) {
        StringBuffer sb=new StringBuffer();
        for(String key:hm.keySet()){
            str=str.replaceAll(key, hm.get(key));
        }
        return str;
    }
}
