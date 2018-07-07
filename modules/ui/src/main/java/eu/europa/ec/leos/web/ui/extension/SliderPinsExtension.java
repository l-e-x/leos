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
package eu.europa.ec.leos.web.ui.extension;

import com.vaadin.annotations.StyleSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Property;
import com.vaadin.server.ClientConnector;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.web.support.LeosCacheToken;

import java.util.Map;
import com.google.common.eventbus.EventBus;

@StyleSheet({"vaadin://../js/web/sliderPins/css/sliderPins.css" + LeosCacheToken.TOKEN,
             "vaadin://../assets/css/leos-sliderPins.css"+ LeosCacheToken.TOKEN})
@JavaScript({"vaadin://../js/web/SliderPinsConnector.js"+ LeosCacheToken.TOKEN })
public class SliderPinsExtension extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SliderPinsExtension.class);
    private Map<String, String > configMap;
    
    public SliderPinsExtension(EventBus eventBus, Map<String, String> configMap){
        super();
        this.configMap = configMap;
    }

    public void extend(Label target) {
        super.extend(target);
        setStateValues(target);

        // handle target's value change
        target.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                LOG.trace("Target's value changed...");
                // Mark that this connector's state might have changed.
                // There is no need to send new data to the client-side,
                // since we just want to trigger a state change event...
                markAsDirty();
            }
        });
    }

    public void navigateSliderPins(String direction) {
        callFunction("navigateSliderPins", direction);
    }
    
    @Override
    protected SliderPinState getState() {
        return (SliderPinState) super.getState();
    }

    @Override
    protected Class<? extends ClientConnector> getSupportedParentType() {
        return Label.class;
    }

    private void setStateValues(Label target){
        getState().targetId = target.getId();
        getState().configMap = configMap;
    }
}