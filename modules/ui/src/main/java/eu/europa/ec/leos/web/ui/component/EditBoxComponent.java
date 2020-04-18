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
package eu.europa.ec.leos.web.ui.component;

import org.apache.commons.lang3.StringUtils;

import com.vaadin.data.Binder;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.shared.ui.ErrorLevel;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import eu.europa.ec.leos.domain.vo.DocumentVO;

public class EditBoxComponent extends CustomField<String> {

	private static final long serialVersionUID = 1L;

	protected HorizontalLayout editboxLayout;
	protected TextField editbox;
	protected Button save;
	protected Button cancel;

	private String value;
	private boolean required = false;
	private String errorMessage;
	private String placeholder;
	private boolean validationEnabled=false;
	private Binder<DocumentVO> binder;

	@Override
	protected Component initContent() {
		buildComponent();
		addListeners();
		return editboxLayout;
	}

	@Override
	public String getValue() {
		return value;
	}

	@Override
	protected void doSetValue(String value) {
		editbox.setValue(value);
		this.value = value;
	}

	private void buildComponent() {
		editboxLayout = new HorizontalLayout();
		editboxLayout.setSpacing(false);
		editboxLayout.setSizeFull();
		editboxLayout.addStyleName("editboxLayout");

		editbox = new TextField();
		editbox.setSizeFull();
		editbox.setStyleName("editboxText");
		editbox.setPlaceholder(placeholder);
		editboxLayout.addComponent(editbox);
		editboxLayout.setExpandRatio(editbox, 1.0f);

		save = new Button();
		save.setIcon(VaadinIcons.CHECK);
		save.setStyleName("save-btn");
		save.setVisible(false);
		editboxLayout.addComponent(save);

		cancel = new Button();
		cancel.setIcon(VaadinIcons.CLOSE);
		cancel.setStyleName("cancel-btn");
		cancel.setVisible(false);
		editboxLayout.addComponent(cancel);
	}

	private void addListeners() {
		editboxLayout.addLayoutClickListener(event -> activate());
		editbox.addFocusListener(event -> activate());
		editbox.addBlurListener(event -> {
			if ((value == null && editbox.isEmpty()) || (value != null && value.equals(editbox.getValue()))) {
				deactivate();
			}
		});
		editbox.addValueChangeListener(event -> {
			if (required) {
				if (!StringUtils.isEmpty(event.getValue())) {
					save.setComponentError(null);
					editbox.setComponentError(null);
				} else {
					save.setComponentError(
							new UserError(errorMessage, AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.WARNING));
					editbox.setComponentError(
							new UserError("", AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.WARNING));
				}
			}
		});
		save.addClickListener(event -> {
			if (StringUtils.isEmpty(editbox.getValue()) && required) {
				save.setComponentError(
						new UserError(errorMessage, AbstractErrorMessage.ContentMode.TEXT, ErrorLevel.WARNING));
			} else {
				if (required) {
					save.setComponentError(null);
					editbox.setComponentError(null);
				}
				setValue(editbox.getValue());
				deactivate();
			}
		});
		cancel.addClickListener(event -> {
			if (required) {
				save.setComponentError(null);
				editbox.setComponentError(null);
			}
			editbox.setValue(value);
			deactivate();
		});
	}

	private void activate() {
		if(validationEnabled) {
			if(!isvalidTitle()) {
				return;
			}
		}
		if (!save.isVisible()) {
			save.setVisible(true);
			cancel.setVisible(true);
			editbox.focus();
			editbox.selectAll();
			editbox.addStyleName("editboxTextFocus");
			editboxLayout.removeStyleName("editboxLayout");
		}
	}

	private void deactivate() {
		save.setVisible(false);
		cancel.setVisible(false);
		editbox.removeStyleName("editboxTextFocus");
		editboxLayout.addStyleName("editboxLayout");
	}

	public void setRequired(String errorMessage) {
		required = true;
		this.errorMessage = errorMessage;
	}

	public void setPlaceholder(String placeholder) {
		this.placeholder = placeholder;
	}
	
	private boolean isvalidTitle() {
		if(binder != null && binder.validate().isOk()) {
			return true;
		}else {
			deactivate();
			return false;
		}
	}
	
	public void setTitleMaxSize(int maxLength ) {
		this.editbox.setMaxLength(maxLength);
	}
	 
}