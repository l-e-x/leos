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
package eu.europa.ec.leos.ui.window.milestone;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import eu.europa.ec.leos.ui.component.LeosJavaScriptComponent;
import eu.europa.ec.leos.web.support.LeosCacheToken;

@StyleSheet({"vaadin://../lib/jqTree_1.4.9/css/jqtree.css" + LeosCacheToken.TOKEN, 
    "vaadin://../assets/css/leos-toc-rendition.css" + LeosCacheToken.TOKEN})
@JavaScript({"vaadin://../js/ui/component/milestoneTocConnector.js"+ LeosCacheToken.TOKEN,
    "vaadin://../lib/jqTree_1.4.9/jqtree.js" + LeosCacheToken.TOKEN})
public class MilestoneTocComponent extends LeosJavaScriptComponent {

    private static final long serialVersionUID = -6271664521480550491L;
    
    public void setTocData(String tocData) {
        getState().tocData = tocData;
    }

    @Override
    protected MilestoneTocState getState() {
        return (MilestoneTocState) super.getState();
    }
    
    @Override
    protected MilestoneTocState getState(boolean markAsDirty) {
        return (MilestoneTocState) super.getState(markAsDirty);
    }
}
