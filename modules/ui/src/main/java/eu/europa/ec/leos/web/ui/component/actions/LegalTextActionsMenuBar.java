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
package eu.europa.ec.leos.web.ui.component.actions;

import com.google.common.eventbus.EventBus;
import eu.europa.ec.leos.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class LegalTextActionsMenuBar extends CommonActionsMenuBar {
    
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextActionsMenuBar.class);

    private MenuItem importerSeparator;
    private MenuItem importerItem;

    @Autowired
    public LegalTextActionsMenuBar(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
    }

    @Override
    protected void initDropDownMenu() {
        buildVersionActions();
        buildImporterActions();
        buildViewActions();
    }

    protected void buildImporterActions() {
        LOG.debug("Building Importer actions group...");
        importerSeparator = addCustomSeparator(messageHelper.getMessage("menu.actions.separator.importer"));

        //Importer
        importerItem = createMenuItem(messageHelper.getMessage("menu.actions.importer"),
                new ImporterCommand());
    }
    
    public void setImporterEnabled(boolean enabled) {
        importerItem.setEnabled(enabled);
    }
    
    public void setImporterVisible(boolean visible) {
        importerSeparator.setVisible(visible);
        importerItem.setVisible(visible);
    }
}
