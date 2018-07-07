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
package eu.europa.ec.leos.web.ui.wizard.document;

import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.web.ui.component.MetaDataFormComponent;
import eu.europa.ec.leos.web.ui.wizard.WizardStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaDataStep extends CustomComponent implements WizardStep {

    private static final Logger log = LoggerFactory.getLogger(MetaDataStep.class);
    private static final long serialVersionUID = 6884508924788177667L;

    private MessageHelper messageHelper;
    private LanguageHelper langHelper;

    private DocumentCreateWizardVO documentCreateWizardVO;
    private MetaDataFormComponent metaDataFormComponent;

    public MetaDataStep(DocumentCreateWizardVO documentCreateWizardVO, MessageHelper messageHelper, LanguageHelper langHelper) {
        this.messageHelper = messageHelper;
        this.langHelper = langHelper;
        this.documentCreateWizardVO = documentCreateWizardVO;

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
        // KLUGE hack to strip template id prefix from document type
        String docType = documentCreateWizardVO.getTemplateName().substring(9);
        log.trace("Metadata document type = {}", docType);

        MetaDataVO metaDataVO = new MetaDataVO(documentCreateWizardVO.getTemplateId(), documentCreateWizardVO.getTemplateLanguage(),
                null, docType, null, null);
        metaDataFormComponent = new MetaDataFormComponent(messageHelper, langHelper, metaDataVO);
        return metaDataFormComponent;
    }

    @Override
    public boolean validateState() {
        boolean isValid = true;
        MetaDataVO metaDataVO = metaDataFormComponent.getMetaDataValues();
        isValid = metaDataFormComponent.isValid();
        documentCreateWizardVO.setMetaDataVO(metaDataVO);
        return isValid;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

}
