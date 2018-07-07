/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.ui.screen.document;

public class DocumentViewSettings {

    private TocPosition tocPosition = TocPosition.RIGHT;
    private final float defaultSplitterPosition = 80.0f;
    private float maxSplitterPosition = 100.0f;

    public TocPosition getTocPosition() {
        return tocPosition;
    }

    public void setTocPosition(TocPosition tocPosition) {
        this.tocPosition = tocPosition;
    }

    public float getDefaultSplitterPosition() {
        return tocPosition.equals(TocPosition.RIGHT) ? defaultSplitterPosition : maxSplitterPosition - defaultSplitterPosition;
    }

    public float getMaxSplitterPosition() {
        return maxSplitterPosition;
    }

}
