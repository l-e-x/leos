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
package eu.europa.ec.leos.ui.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.data.HasValue;
import com.vaadin.shared.Registration;
import com.vaadin.ui.*;
import eu.europa.ec.leos.web.support.LeosCacheToken;

import java.util.Map;
import java.util.WeakHashMap;

@StyleSheet({"vaadin://../js/lib/sliderPins/css/sliderPins.css" + LeosCacheToken.TOKEN,
             "vaadin://../assets/css/leos-sliderPins.css"+ LeosCacheToken.TOKEN})
@JavaScript({"vaadin://../js/ui/extension/SliderPinsConnector.js"+ LeosCacheToken.TOKEN })
public class SliderPinsExtension<T extends AbstractComponent> extends LeosJavaScriptExtension {

    private static final long serialVersionUID = 1L;
    
    private Map<Component, Registration> listenersMap;

    public SliderPinsExtension(T target, Map<String, String> configMap){
        super();
        listenersMap = new WeakHashMap();
        getState().configMap = configMap;
        extend(target);
    }

    protected void extend(T target) {
        super.extend(target);
        // handle target's value change
        if (target instanceof ComponentContainer){
            ComponentContainer layout = (ComponentContainer) target;
            //FIXME If the field value is set before adding the component the "ValueChangeListener" has no effect
            layout.addComponentAttachListener(event -> {
                addValueChangeListener(event.getAttachedComponent());
            });
            layout.addComponentDetachListener(event -> {
                Component detachedComponent = event.getDetachedComponent();
                if (listenersMap.containsKey(detachedComponent)) {
                    listenersMap.get(detachedComponent).remove();
                    listenersMap.remove(detachedComponent);
                }
            });
            
            layout.forEach(component -> addValueChangeListener(component));
        }
        else {
            addValueChangeListener(target);
        }
    }

    private void addValueChangeListener(Component component) {
        if (component instanceof HasValue) {
            Registration valueChangeListener = ((HasValue) component).addValueChangeListener(event -> {
                LOG.trace("Target's value changed...");
                // Mark that this connector's state might have changed.
                // There is no need to send new data to the client-side,
                // since we just want to trigger a state change event...
                forceDirty();
            });
            listenersMap.put(component, valueChangeListener);
        }
    }

    public void navigateSliderPins(String direction) {
        callFunction("navigateSliderPins", direction);
    }
    
    @Override
    protected SliderPinsState getState() {
        return (SliderPinsState) super.getState();
    }

    @Override
    protected SliderPinsState getState(boolean markAsDirty) {
        return (SliderPinsState) super.getState(markAsDirty);
    }
}
