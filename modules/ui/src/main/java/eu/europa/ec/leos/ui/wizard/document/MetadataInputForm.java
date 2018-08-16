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
package eu.europa.ec.leos.ui.wizard.document;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import java.util.Objects;

class MetadataInputForm extends FormLayout {

    private static final long serialVersionUID = 2636423615350597957L;

    private static final String EMPTY_TEMPLATE = "&nbsp;";

    private final DocumentVO document;
    private MessageHelper messageHelper;
    private LanguageHelper languageHelper;

    private TextField purposeField;
    private Label templateLabel;
    private Binder<MetadataVO> formBinder = new Binder<>();

    MetadataInputForm(final DocumentVO document, MessageHelper messageHelper, LanguageHelper languageHelper) {
        this.document = document;
        this.messageHelper = messageHelper;
        this.languageHelper = languageHelper;
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        buildFormLayout();
    }

    private void buildFormLayout() {
        addComponent(buildTemplate());
        addComponent(buildPurpose());
        addComponent(buildLanguage());
        addComponent(buildSecurityLevel());
        addComponent(buildInternalRef());
        addComponent(buildInterInstRef());
        addComponent(buildPackageTitle());
        addComponent(buildEeaRelevance());
    }

    private Component buildTemplate() {
        templateLabel = new Label(EMPTY_TEMPLATE);
        templateLabel.setCaption(messageHelper.getMessage("wizard.document.create.metadata.template.caption"));
        templateLabel.setSizeUndefined();
        templateLabel.setWidth(100, Unit.PERCENTAGE);
        return templateLabel;
    }

    private Component buildPurpose() {
        purposeField = new  TextField(messageHelper.getMessage("wizard.document.create.metadata.purpose.caption"));
        purposeField.setWidth(100, Unit.PERCENTAGE);
        formBinder.forField(purposeField).asRequired(messageHelper.getMessage("wizard.document.create.metadata.purpose.error")).bind( MetadataVO::getDocPurpose, MetadataVO::setDocPurpose);
        return purposeField;
    }

    private Component buildLanguage() {
        String languageCode = Objects.toString(document.getMetadata().getLanguage(), messageHelper.getMessage("wizard.document.create.metadata.language.default"));
        TextField textField = new TextField(messageHelper.getMessage("wizard.document.create.metadata.language.caption"));
        textField.setValue(languageHelper.getLanguageDescription(languageCode));
        textField.setData(languageCode);
        textField.setReadOnly(true);
        return textField;
    }

    private Component buildInternalRef() {
        TextField textField = new TextField(messageHelper.getMessage("wizard.document.create.metadata.internalRef.caption"));
        textField.setWidth(100, Unit.PERCENTAGE);
        textField.setEnabled(false);
        return textField;
    }

    private Component buildSecurityLevel() {
        TextField textField = new TextField(messageHelper.getMessage("wizard.document.create.metadata.securityLevel.caption"));
        textField.setValue(messageHelper.getMessage("wizard.document.create.metadata.securityLevel.default"));
        textField.setReadOnly(true);
        return textField;
    }

    private Component buildInterInstRef() {
        TextField textField = new TextField(messageHelper.getMessage("wizard.document.create.metadata.interInstRef.caption"));
        textField.setWidth(100, Unit.PERCENTAGE);
        textField.setEnabled(false);
        return textField;
    }

    private Component buildPackageTitle() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setCaption(messageHelper.getMessage("wizard.document.create.metadata.packageTitle.caption"));
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setSpacing(true);

        CheckBox checkBox = new CheckBox();
        checkBox.setReadOnly(true);
        layout.addComponent(checkBox);

        TextField textField = new TextField();
        textField.setWidth(100, Unit.PERCENTAGE);
        textField.setEnabled(false);
        layout.addComponent(textField);
        layout.setExpandRatio(textField, 1);
        return layout;
    }

    private Component buildEeaRelevance() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setCaption(messageHelper.getMessage("wizard.document.create.metadata.eeaRelevance.caption"));
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setSpacing(true);

        CheckBox checkBox = new CheckBox();
        checkBox.setReadOnly(true);
        layout.addComponent(checkBox);

        TextField textField = new TextField();
        textField.setValue(messageHelper.getMessage("wizard.document.create.metadata.eeaRelevance.default"));
        textField.setWidth(100, Unit.PERCENTAGE);
        textField.setEnabled(false);
        layout.addComponent(textField);
        layout.setExpandRatio(textField, 1);
        return layout;
    }

    boolean isValid() {
        return !purposeField.isEmpty();
    }

    @Override
    public void attach() {
        formBinder.setBean(document.getMetadata());
        templateLabel.setValue(document.getMetadata().getTemplateName());
        super.attach();
    }
}
