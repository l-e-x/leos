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
package eu.europa.ec.leos.web.ui.component.documentCard;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Property.ValueChangeNotifier;
import com.vaadin.ui.Button;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

public class PropertyBoundButton extends Button implements
        Property.Viewer,
        Property.ValueChangeNotifier,
        Property.ValueChangeListener {

    private Property<String> propertyDataSource;

    @Override
    public void setPropertyDataSource(Property newDataSource) {
        if (newDataSource != null && ValueChangeNotifier.class.isAssignableFrom(newDataSource.getClass())) {
            ((ValueChangeNotifier) newDataSource).removeValueChangeListener(this);
        }
        this.propertyDataSource = newDataSource;

        if (propertyDataSource != null) {
            updateValueFromDataSource();
        }
        // Listens the new data source if possible
        if (propertyDataSource != null && ValueChangeNotifier.class.isAssignableFrom(propertyDataSource.getClass())) {
            ((ValueChangeNotifier) propertyDataSource).addValueChangeListener(this);
        }
        markAsDirty();
    }

    protected void fireValueChange() {
        fireEvent(new PropertyBoundButton.ValueChangeEvent(this));
    }

    @Override
    public Property getPropertyDataSource() {
        return propertyDataSource;
    }

    private void updateValueFromDataSource() {
        // Update the internal values from the data source
        String tooltip = propertyDataSource.getValue();
        // update the button caption
        this.setDescription(tooltip);
        if (!StringUtils.isBlank(tooltip)) {
            removeStyleName("hidden");
        } else {
            addStyleName("hidden");
        }
        fireValueChange();
    }

    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        updateValueFromDataSource();
    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
        addListener(PropertyBoundButton.ValueChangeEvent.class, listener, VALUE_CHANGE_METHOD);
    }

    @Override
    public void removeValueChangeListener(ValueChangeListener listener) {
        removeListener(PropertyBoundButton.ValueChangeEvent.class, listener, VALUE_CHANGE_METHOD);
    }

    @Override
    public void addListener(ValueChangeListener listener) {
        addValueChangeListener(listener);
    }

    @Override
    public void removeListener(ValueChangeListener listener) {
        removeValueChangeListener(listener);
    }

    public static class ValueChangeEvent extends Event implements Property.ValueChangeEvent {
        public ValueChangeEvent(PropertyBoundButton source) {
            super(source);
        }

        @Override
        public Property getProperty() {
            return (Property) getSource();
        }
    }

    private static final Method VALUE_CHANGE_METHOD;

    static {
        try {
            VALUE_CHANGE_METHOD = ValueChangeListener.class.getDeclaredMethod("valueChange", new Class[]{Property.ValueChangeEvent.class});

        } catch (final NoSuchMethodException e) {
            // This should never happen
            throw new RuntimeException("Internal error finding methods in ContributorCard");
        }
    }
}
