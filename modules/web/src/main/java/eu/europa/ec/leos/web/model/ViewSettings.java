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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.web.ui.screen.document.TocPosition;

public class ViewSettings{

    protected TocPosition tocPosition = TocPosition.RIGHT;
    protected final float defaultSplitterPosition = 80.0f;
    protected float maxSplitterPosition = 100.0f;
    
    protected boolean previewEnabled = true;
    protected boolean compareEnabled = true;
    protected boolean tocEditEnabled = true;
    
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

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public void setPreviewEnabled(boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    public boolean isCompareEnabled() {
        return compareEnabled;
    }

    public boolean isTocEditEnabled() {
        return tocEditEnabled;
    }
}
