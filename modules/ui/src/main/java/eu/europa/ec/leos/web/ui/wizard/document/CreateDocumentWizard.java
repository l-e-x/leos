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

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.wizard.CreateLeosDocumentRequestEvent;
import eu.europa.ec.leos.web.ui.wizard.AbstractWizard;
import eu.europa.ec.leos.web.ui.wizard.WizardStep;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class CreateDocumentWizard extends AbstractWizard {

    private LanguageHelper langHelper;
    private DocumentCreateWizardVO documentCreateWizardVO;

    public CreateDocumentWizard(List<CatalogItem> templates, MessageHelper messageHelper, LanguageHelper langHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        this.langHelper=langHelper;
        init(templates);
    }

    public void init(List<CatalogItem> templates) {
        documentCreateWizardVO = new DocumentCreateWizardVO();
        registerWizardStep(new TemplateSelectionStep(documentCreateWizardVO, templates, messageHelper));
        registerWizardStep(new MetaDataStep(documentCreateWizardVO, messageHelper, langHelper));
        setWizardStep(0);
    }

    @Override
    protected String getWizardTitle() {
        return messageHelper.getMessage("wizard.document.create.title");
    }

    @Override
    protected boolean handleFinishAction(List<WizardStep> stepList) {

        eventBus.post(new CreateLeosDocumentRequestEvent(documentCreateWizardVO));
        return true;
    }
}
