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
package eu.europa.ec.leos.web.ui.component.card;

import com.vaadin.data.Property;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import java.lang.reflect.Method;
import java.util.List;

public abstract class ListTypeProperty<BEAN> extends CustomComponent implements
        Property<List<BEAN>>,
        Property.Viewer,
        Property.ValueChangeNotifier,
        Property.ValueChangeListener {
    private static final long serialVersionUID = 3751840321432178003L;
    private static final Method VALUE_CHANGE_METHOD;

    static {
        try {
            VALUE_CHANGE_METHOD = Property.ValueChangeListener.class.getDeclaredMethod(
                    "valueChange",
                    new Class[]{Property.ValueChangeEvent.class});
        } catch (final java.lang.NoSuchMethodException e) {
            // This should never happen
            throw new java.lang.RuntimeException("Internal error finding methods in ContributorCard");
        }
    }

    private Property<List<BEAN>> propertyDataSource;
    private VerticalLayout layout;;

    public ListTypeProperty() {
        layout = new VerticalLayout();
        setCompositionRoot(layout);
    }

    /** this method should create a component for the bean*/
    protected abstract Component createRepresentation(BEAN bean);

    @Override
    public void attach() {
        super.attach();
        if (propertyDataSource != null && Property.ValueChangeNotifier.class.isAssignableFrom(propertyDataSource.getClass())) {
            ((Property.ValueChangeNotifier) propertyDataSource).addValueChangeListener(this);
        }
    }

    @Override
    public void detach() {
        super.detach();
        // Stop listening to data source events on detach to avoid a potential
        // memory leak. See #6155.
        if (propertyDataSource != null && Property.ValueChangeNotifier.class.isAssignableFrom(propertyDataSource.getClass())) {
            ((Property.ValueChangeNotifier) propertyDataSource).removeValueChangeListener(this);
        }
    }

    @Override
    public void setPropertyDataSource(Property newDataSource) {
        if (newDataSource != null && Property.ValueChangeNotifier.class.isAssignableFrom(newDataSource.getClass())) {
            ((Property.ValueChangeNotifier) newDataSource).removeValueChangeListener(this);
        }
        this.propertyDataSource = newDataSource;

        if (propertyDataSource != null) {
            updateValueFromDataSource();
        }

        // Listens the new data source if possible
        if (propertyDataSource != null && Property.ValueChangeNotifier.class.isAssignableFrom(propertyDataSource.getClass())) {
            ((Property.ValueChangeNotifier) propertyDataSource).addValueChangeListener(this);
        }
        markAsDirty();
    }

    protected void fireValueChange() {
        fireEvent(new ListTypeProperty.ValueChangeEvent(this));
    }

    @Override
    public Property getPropertyDataSource() {
        return propertyDataSource;
    }

    private void updateValueFromDataSource() {
        // Update the internal values from the data source
        List<BEAN> beanList = propertyDataSource.getValue();
        layout.removeAllComponents();

        if (beanList != null) {
            for (BEAN bean : beanList) {
                layout.addComponent(createRepresentation(bean));
            }
        }
        fireValueChange();
    }

    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        updateValueFromDataSource();
    }

    @Override
    public void addValueChangeListener(Property.ValueChangeListener listener) {
        addListener(ListTypeProperty.ValueChangeEvent.class, listener, VALUE_CHANGE_METHOD);
    }

    @Override
    public void addListener(Property.ValueChangeListener listener) {
        addValueChangeListener(listener);
    }

    @Override
    public void removeValueChangeListener(Property.ValueChangeListener listener) {
        removeListener(ListTypeProperty.ValueChangeEvent.class, listener,
                VALUE_CHANGE_METHOD);
    }

    @Override
    public void removeListener(Property.ValueChangeListener listener) {
        removeValueChangeListener(listener);
    }

    @Override
    public List<BEAN> getValue() {
        return propertyDataSource.getValue();
    }

    @Override
    public void setValue(List<BEAN> newValue) throws ReadOnlyException {
        throw new ReadOnlyException("can not set value directly");
    }

    @Override
    public Class<? extends List<BEAN>> getType() {
        return null;
    }

    public static class ValueChangeEvent extends Component.Event implements Property.ValueChangeEvent {
        public ValueChangeEvent(ListTypeProperty source) {
            super(source);
        }

        @Override
        public Property getProperty() {
            return (Property) getSource();
        }
    }
}
