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
package eu.europa.ec.leos.web.event.view.document;

public class MergeSuggestionRequest {
    private final String origText;
    private final String newText;
    private final String elementId;
    private final int startOffset;
    private final int endOffset;

    public MergeSuggestionRequest(String origText, String newText, String elementId, int startOffset, int endOffset) {
        this.origText = origText;
        this.elementId = elementId;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.newText = newText;
    }

    /**
     * @return the origText
     */
    public String getOrigText() {
        return origText;
    }

    /**
     * @return the newText
     */
    public String getNewText() {
        return newText;
    }

    /**
     * @return the elementId
     */
    public String getElementId() {
        return elementId;
    }

    /**
     * @return the startOffset
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * @return the endOffset
     */
    public int getEndOffset() {
        return endOffset;
    }
}
