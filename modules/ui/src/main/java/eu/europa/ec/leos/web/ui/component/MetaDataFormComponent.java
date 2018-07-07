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
package eu.europa.ec.leos.web.ui.component;

import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.vaadin.server.AbstractErrorMessage;
import com.vaadin.server.ErrorMessage;
import com.vaadin.server.UserError;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import eu.europa.ec.leos.vo.MetaDataVO;
import org.apache.commons.lang3.StringUtils;

public class MetaDataFormComponent extends FormLayout {

    private static final long serialVersionUID = 2636423615350597957L;

    private TextField templateField;
    private TextField languageField;
    private TextField docStageField;
    private TextField docTypField;
    private TextField docPurposeField;
    private TextField internalRefField;
    private TextField secLevelField;
    private TextField interInstitutionalRefField;
    private MessageHelper messageHelper;
    private LanguageHelper languageHelper;
    public MetaDataFormComponent(MessageHelper messageHelper, LanguageHelper languageHelper,  MetaDataVO metaDataVO) {

        this.messageHelper = messageHelper;
        this.languageHelper=languageHelper;
        setSizeFull();
        setSpacing(true);
        setMargin(true);
        buildFormLayout(metaDataVO);
    }

    private void buildFormLayout(MetaDataVO metaDataVO) {

        templateField = new TextField(messageHelper.getMessage("metadata.textfield.template.caption"));
        languageField = new TextField(messageHelper.getMessage("metadata.textfield.language.caption"));
        docStageField = new TextField(messageHelper.getMessage("metadata.textfield.docStage.caption"));
        docTypField = new TextField(messageHelper.getMessage("metadata.textfield.docType.caption"));
        docPurposeField = new TextField(messageHelper.getMessage("metadata.textfield.docTitle.caption"));
        internalRefField = new TextField(messageHelper.getMessage("metadata.textfield.internalref.caption"));

        templateField.setValue(StringUtils.defaultString(metaDataVO.getTemplate()));

        languageField.setValue(languageHelper.getLanguageDescription(StringUtils.defaultString(metaDataVO.getLanguage(), "EN")));
        languageField.setData(StringUtils.defaultString(metaDataVO.getLanguage(), "EN"));

        docStageField.setValue(StringUtils.defaultString(metaDataVO.getDocStage()));
        docTypField.setValue(metaDataVO.getDocStage() == null ? "" : (metaDataVO.getDocStage()+" ")  
                                            + StringUtils.defaultString(metaDataVO.getDocType()));
        docTypField.setData(metaDataVO.getDocType());
        docPurposeField.setValue(StringUtils.defaultString(metaDataVO.getDocPurpose(), "on [...]"));
        internalRefField.setValue(StringUtils.defaultString(metaDataVO.getInternalRef()));

        templateField.setReadOnly(true);
        languageField.setReadOnly(true);
        docStageField.setReadOnly(true);
        docTypField.setReadOnly(true);
        internalRefField.setEnabled(false);


        templateField.setWidth(100, Unit.PERCENTAGE);
        languageField.setWidth(100, Unit.PERCENTAGE);
        docStageField.setWidth(100, Unit.PERCENTAGE);
        docTypField.setWidth(100, Unit.PERCENTAGE);
        docPurposeField.setWidth(100, Unit.PERCENTAGE);
        internalRefField.setWidth(100, Unit.PERCENTAGE);

        addComponent(templateField);
        addComponent(languageField);
        //addComponent(docStageField);
        addComponent(docTypField);
        addComponent(docPurposeField);
        addComponent(internalRefField);

        secLevelField = new TextField(messageHelper.getMessage("metadata.textfield.seclevel.caption"));
        secLevelField.setValue("Standard treatment");
        secLevelField.setReadOnly(true);
        addComponent(secLevelField);

        interInstitutionalRefField = new TextField(messageHelper.getMessage("metadata.textfield.interinstitutionalref.caption"));
        interInstitutionalRefField.setEnabled(false);
        interInstitutionalRefField.setWidth(100, Unit.PERCENTAGE);
        addComponent(interInstitutionalRefField);

        CheckBox pkgTitle = new CheckBox();
        pkgTitle.setReadOnly(true);

        TextField pkgTitleField = new TextField();
        pkgTitleField.setEnabled(false);
        pkgTitleField.setWidth(100, Unit.PERCENTAGE);

        HorizontalLayout pkgTitleLayout = new HorizontalLayout();
        pkgTitleLayout.setWidth(100, Unit.PERCENTAGE);
        pkgTitleLayout.setSpacing(true);
        pkgTitleLayout.setCaption(messageHelper.getMessage("metadata.textfield.pkgtitle.caption"));
        pkgTitleLayout.addComponent(pkgTitle);
        pkgTitleLayout.addComponent(pkgTitleField);
        pkgTitleLayout.setExpandRatio(pkgTitleField, 1);
        addComponent(pkgTitleLayout);

        CheckBox eeaRelevance = new CheckBox();
        eeaRelevance.setReadOnly(true);

        TextField eeaRelevanceField = new TextField();
        eeaRelevanceField.setValue("(Text with EEA relevance)");
        eeaRelevanceField.setEnabled(false);
        eeaRelevanceField.setWidth(100, Unit.PERCENTAGE);

        HorizontalLayout eeaRelevanceLayout = new HorizontalLayout();
        eeaRelevanceLayout.setWidth(100, Unit.PERCENTAGE);
        eeaRelevanceLayout.addComponent(eeaRelevance);
        eeaRelevanceLayout.addComponent(eeaRelevanceField);
        eeaRelevanceLayout.setExpandRatio(eeaRelevanceField, 1);
        eeaRelevanceLayout.setSpacing(true);
        eeaRelevanceLayout.setCaption(messageHelper.getMessage("metadata.textfield.eearelevance.caption"));
        addComponent(eeaRelevanceLayout);
    }

    public MetaDataVO getMetaDataValues() {
        MetaDataVO metaDataVO = new MetaDataVO(templateField.getValue(), (String)languageField.getData(),docStageField.getValue(), (String)docTypField.getData(),
                docPurposeField.getValue(), internalRefField.getValue());
        return metaDataVO;
    }

    public boolean isValid() {
        Boolean isValid = true;
        if (StringUtils.isEmpty(docPurposeField.getValue())) {
            docPurposeField.setComponentError(new UserError(messageHelper.getMessage("metadata.textfield.docTitle.error"),
                    AbstractErrorMessage.ContentMode.TEXT,
                    ErrorMessage.ErrorLevel.WARNING));
            isValid = false;
        } else {
            docPurposeField.setComponentError(null);
        }
        return isValid;
    }

}
