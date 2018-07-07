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
package eu.europa.ec.leos.ui.component;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.ui.Component;
import com.vaadin.ui.JavaScriptFunction;
import com.vaadin.util.ReflectTools;
import elemental.json.JsonArray;
import eu.europa.ec.leos.web.support.LeosCacheToken;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@StyleSheet({"vaadin://../lib/noUiSlider_10.0.0/css/nouislider.css" + LeosCacheToken.TOKEN,
             "vaadin://../assets/css/leos-nouislider.css" + LeosCacheToken.TOKEN})
@JavaScript({"vaadin://../js/ui/component/rangeSliderConnector.js"+ LeosCacheToken.TOKEN})
public class RangeSliderComponent extends LeosJavaScriptComponent {

    private static final long serialVersionUID = 1L;

    public RangeSliderComponent() {
        super();
        registerSliderChangeCallback();
    }

    public void setColouredArea(ColouredArea colouredArea) {
        getState(false).colouredArea = colouredArea.name();
    }

    public void setSteps(Set<String> values) {
        getState().stepValues = values;
    }

    public Set<String> getSteps() {
        // state read operation must not trigger a state change event
        return getState(false).stepValues;
    }

    public void setHandles(String[] values) {
        getState().handleValues = values;
    }

    public String[] getHandles() {
        // state read operation must not trigger a state change event
        return getState(false).handleValues;
    }

    @Override
    protected RangeSliderState getState() {
        return (RangeSliderState) super.getState();
    }

    @Override
    protected RangeSliderState getState(boolean markAsDirty) {
        return (RangeSliderState) super.getState(markAsDirty);
    }

    protected void registerSliderChangeCallback() {
        // handle slider changes from client-side
        addFunction("onSliderChange", new JavaScriptFunction() {
            @Override
            public void call(JsonArray args) {
                LOG.debug("Slider change from client-side... {}", args.toJson());
                // FIXME callback comes from client side, but how should state be updated on client side???
                getState(false).handleValues = convertArray(args.getArray(0));
                // FIXME getState() or markAsDirty() should probably be used here,
                // FIXME but will certainly cause change events that will lead to an infinite loop!!!
                // FIXME the SERVER side should probably check if values actually changed to avoid infinite loops
                // FIXME the CLIENT side should probably check if values actually changed to avoid infinite loops
                fireHandlesEvent();
            }
        });
    }

    public void addHandlesListener(HandlesListener listener) {
        addListener(HandlesEvent.class, listener, HandlesListener.VALUES_CHANGED_METHOD);
    }

    public interface HandlesListener extends Serializable {
        Method VALUES_CHANGED_METHOD =
                ReflectTools.findMethod(HandlesListener.class, "valuesChanged", HandlesEvent.class);
        void valuesChanged(HandlesEvent event);
    }

    public static class HandlesEvent extends Event {

        public HandlesEvent(Component source) {
            super(source);
        }

        @Override
        public RangeSliderComponent getComponent() {
            return (RangeSliderComponent) super.getComponent();
        }
    }

    protected void fireHandlesEvent() {
        if (isEnabled()) {
            fireEvent(new HandlesEvent(this));
        }
    }

    protected String[] convertArray(JsonArray input) {
        // FIXME replace with better implementation, maybe using GSON?
        List<String> output = new ArrayList<>();
        for(int i = 0; i < input.length(); i++){
            output.add(input.getString(i));
        }
        return output.toArray(new String[0]);
    }

    public enum ColouredArea {
        LOWER, UPPER, ENCLOSED
    }
}
