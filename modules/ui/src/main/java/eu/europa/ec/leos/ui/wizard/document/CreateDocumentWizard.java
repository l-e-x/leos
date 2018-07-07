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
package eu.europa.ec.leos.ui.wizard.document;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.ui.event.CreateDocumentRequestEvent;
import eu.europa.ec.leos.ui.wizard.AbstractWizard;
import eu.europa.ec.leos.ui.wizard.WizardStep;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.support.i18n.LanguageHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class CreateDocumentWizard extends AbstractWizard {

    private static final long serialVersionUID = 1L;

    private LanguageHelper languageHelper;
    private WizardData wizardData;

    public CreateDocumentWizard(List<CatalogItem> templates, MessageHelper messageHelper, LanguageHelper languageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        this.languageHelper = languageHelper;
        init(templates);
    }

    public void init(List<CatalogItem> templates) {
        wizardData = new WizardData();
        registerWizardStep(new TemplateSelectionStep(wizardData, templates, messageHelper));
        registerWizardStep(new MetadataInputStep(wizardData, messageHelper, languageHelper));
        setWizardStep(0);
    }

    @Override
    protected String getWizardTitle() {
        return messageHelper.getMessage("wizard.document.create.title");
    }

    @Override
    protected boolean handleFinishAction(List<WizardStep> stepList) {
        // Currently there is only one use case: create proposal!
        // Ideally the category would be resolved from wizard data,
        // depending on some attribute of the selected template...
        LeosCategory category = LeosCategory.PROPOSAL;

        // Currently the catalog items provide a string concatenation of IDs!
        // Ideally the mapping to templates should be configured somewhere...
        String[] templates = (wizardData.ids != null) ? wizardData.ids.split(";") : new String[0];
        
        eventBus.post(new CreateDocumentRequestEvent(category, templates, wizardData.purpose));
        return true;
    }
}
