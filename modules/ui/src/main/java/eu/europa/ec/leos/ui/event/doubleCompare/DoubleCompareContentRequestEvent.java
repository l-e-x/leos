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
package eu.europa.ec.leos.ui.event.doubleCompare;


import eu.europa.ec.leos.domain.cmis.document.XmlDocument;

public class DoubleCompareContentRequestEvent<T extends XmlDocument> {
    private int displayMode = 0;
    private T originalProposal;
    private T intermediateMajor;
    private T current;
    private boolean enabled;

    public DoubleCompareContentRequestEvent(T originalProposal, T intermediateMajor, T current, int displayMode, boolean enabled) {
        this.originalProposal = originalProposal;
        this.intermediateMajor = intermediateMajor;
        this.current = current;
        this.displayMode = displayMode;
        this.enabled = enabled;
    }

    public int getDisplayMode() {
        return displayMode;
    }

    public T getOriginalProposal() {
        return originalProposal;
    }
    
    public T getIntermediateMajor() {
        return intermediateMajor;
    }

    public T getCurrent() {
        return current;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
