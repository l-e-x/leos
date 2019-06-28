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
package eu.europa.ec.leos.ui.wizard.document;

import com.vaadin.ui.Component;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.ui.wizard.WizardStep;
import eu.europa.ec.leos.i18n.LanguageHelper;
import eu.europa.ec.leos.i18n.MessageHelper;

class MetadataInputStep implements WizardStep {

    private MessageHelper messageHelper;
    private MetadataInputForm metadataInputForm;

    MetadataInputStep(final DocumentVO document, MessageHelper messageHelper, LanguageHelper languageHelper) {
        this.messageHelper = messageHelper;
        this.metadataInputForm = new MetadataInputForm(document, messageHelper, languageHelper);
    }

    @Override
    public String getStepTitle() {
        return messageHelper.getMessage("wizard.document.create.metadata.title");
    }

    @Override
    public String getStepDescription() {
        return messageHelper.getMessage("wizard.document.create.metadata.desc");
    }

    @Override
    public Component getComponent() {
        return metadataInputForm;
    }

    @Override
    public boolean validateState() {
        return metadataInputForm.isValid();
    }

    @Override
    public boolean canFinish() {
        return true;
    }
}
