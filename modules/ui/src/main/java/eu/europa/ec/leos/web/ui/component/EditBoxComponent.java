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
package eu.europa.ec.leos.web.ui.component;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.ErrorMessage;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.server.UserError;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.v7.data.Property;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.Property.ValueChangeNotifier;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.TextField;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/*
 * UI component that has a value the user can change through the user interface.
 * It implements the @Property.ValueChangeNotifier interface which notifies the component
 * when value of dataSource @Property propertyDS changes from external source. 
 * The ValueChangeListener added on @Property propertyDS emits @Property.ValueChangeEvent on change of the value.
 * The component also implements @Property.ValueChangeListener which listens to @Property.ValueChangeEvent and call
 * valueChange() method to update the value of textfield.
 */
public class EditBoxComponent extends CustomComponent implements Property.Editor,
        Property.ValueChangeNotifier,
        Property.ValueChangeListener {
    protected HorizontalLayout editboxLayout;
    protected TextField editbox;
    protected Button save;
    protected Button cancel;

    private String value;
    private boolean active;
    private Property<String> propertyDS;

    private final List<ValueChangeListener> listeners = new ArrayList<ValueChangeListener>();

    public EditBoxComponent() {
        buildComponent();
        addListeners();
    }

    public void setValue(String value) {
        this.value = value;
        editbox.setValue(value);
        if (propertyDS != null) {
            propertyDS.setValue(value);
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public void setDescription(String description) {
        editbox.setDescription(description);
    }

    public void setInputPrompt(String inputPrompt) {
        editbox.setInputPrompt(inputPrompt);
    }

    @Override
    public String getDescription() {
        return editbox.getDescription();
    }

    @Override
    public void setPropertyDataSource(Property newDataSource) {
        // Removes listener registered for old data source value
        if (propertyDS != null && ValueChangeNotifier.class.isAssignableFrom(propertyDS.getClass())) {
            ((ValueChangeNotifier) propertyDS).removeValueChangeListener(this);
        }

        propertyDS = newDataSource;
        if (propertyDS != null) {
            // Update value to the edit box
            updateValueFromDS();
        }

        // Listens to the new data source changes.
        if (propertyDS != null && ValueChangeNotifier.class.isAssignableFrom(propertyDS.getClass())) {
            ((ValueChangeNotifier) propertyDS).addValueChangeListener(this);
        }
    }

    @Override
    public Property getPropertyDataSource() {
        return propertyDS;
    }

    @Override
    public void valueChange(Property.ValueChangeEvent event) {
        updateValueFromDS();
    }

    @Override
    public void addValueChangeListener(ValueChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeValueChangeListener(ValueChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void addListener(ValueChangeListener listener) {
        addValueChangeListener(listener);
    }

    @Override
    public void removeListener(ValueChangeListener listener) {
        removeValueChangeListener(listener);
    }

    private void buildComponent() {
        editboxLayout = new HorizontalLayout();
        editboxLayout.setSizeFull();
        editboxLayout.addStyleName("editboxLayout");

        editbox = new TextField();
        editbox.setStyleName("editboxText");
        editbox.setSizeFull();
        editbox.setNullRepresentation("");
        editboxLayout.addComponent(editbox);

        save = new Button();
        save.setIcon(VaadinIcons.CHECK);
        save.setStyleName("save-btn");
        editboxLayout.addComponent(save);

        cancel = new Button();
        cancel.setIcon(VaadinIcons.CLOSE);
        cancel.setStyleName("cancel-btn");
        editboxLayout.addComponent(cancel);

        editboxLayout.setExpandRatio(editbox, 1.0f);
        setSizeFull();
        setCompositionRoot(editboxLayout);

        active = false;
        showButtons();
    }

    private void addListeners() {

        editboxLayout.addLayoutClickListener(event -> activate()); // to handle click on edit icon

        editbox.addFocusListener(event -> activate());

        editbox.addBlurListener(event -> {
            if ((value == null && editbox.isEmpty()) || (value != null && value.equals(editbox.getValue()))) {
                deactivate();
            }
        });

        editbox.addTextChangeListener(event -> {
            String currentValue = event.getText();
            if (editbox.isRequired()) {
                if (!StringUtils.isEmpty(currentValue)) {
                    save.setComponentError(null);
                    editbox.setComponentError(null);
                } else {
                    if (save.getComponentError() == null) {
                        save.setComponentError(
                                new UserError(editbox.getRequiredError(), AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.WARNING));
                    }
                    if (editbox.getComponentError() == null) {
                        editbox.setComponentError(
                                new UserError("", AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.WARNING));
                    }
                }
            }

        });

        save.addClickListener(event -> {            
            if (StringUtils.isEmpty(editbox.getValue()) && editbox.isRequired()) {
                if (save.getComponentError() == null) {
                    save.setComponentError(new UserError(editbox.getRequiredError(), AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.WARNING));
                }
            } else {
                if (editbox.isRequired()) {
                    save.setComponentError(null);
                    editbox.setComponentError(null);
                }
                value = editbox.getValue();
                if (propertyDS != null) {
                    propertyDS.setValue(value);
                }
                deactivate();
                fireValueChange();
            }
        });

        cancel.addClickListener(event -> {
            setValue(value);
            if (editbox.isRequired()) {
                save.setComponentError(null);
                editbox.setComponentError(null);
            }
            deactivate();
        });
    }

    private void activate() {
        if (!active) {
            editbox.focus();
            editbox.selectAll();
            active = true;
            showButtons();
            editbox.addStyleName("editboxTextFocus");
            editboxLayout.removeStyleName("editboxLayout");
        }
    }

    private void showButtons() {
        save.setVisible(active);
        cancel.setVisible(active);
    }

    private void deactivate() {
        active = false;
        showButtons();
        editbox.removeStyleName("editboxTextFocus");
        editboxLayout.addStyleName("editboxLayout");
    }

    private void fireValueChange() {
        ValueChangeEvent event = new ValueChangeEvent() {
            @Override
            public Property getProperty() {
                return propertyDS;
            }
        };

        for (ValueChangeListener listener : listeners) {
            listener.valueChange(event);
        }
    }

    private void updateValueFromDS() {
        value = propertyDS.getValue();
        editbox.setValue(propertyDS.getValue());
    }

    public void setRequired(String errorMessage) {
        editbox.setRequired(true);
        editbox.setRequiredError(errorMessage);
    }

}
