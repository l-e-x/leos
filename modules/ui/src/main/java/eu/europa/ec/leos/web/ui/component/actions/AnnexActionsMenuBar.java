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

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.vaadin.dialogs.ConfirmDialog;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.annex.AnnexStructureType;
import eu.europa.ec.leos.ui.event.view.AddStructureChangeMenuEvent;
import eu.europa.ec.leos.ui.event.view.AnnexStructureChangeEvent;

@Component
@Scope("prototype")
public class AnnexActionsMenuBar extends CommonActionsMenuBar {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(AnnexActionsMenuBar.class);
    
    private MenuItem annexActionSeparator;
    private MenuItem switchStructure;

    @Autowired
    public AnnexActionsMenuBar(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
    }

    @Override
    protected void buildViewActions() {
        LOG.debug("Building View actions group...");
        addCustomSeparator(messageHelper.getMessage("menu.actions.separator.view"));
    }
    
    @Subscribe
    public void buildStructureChangeAction(AddStructureChangeMenuEvent event) {
        LOG.debug("Building annex actions menu item...");
        mainMenuItem.removeChild(annexActionSeparator);
        mainMenuItem.removeChild(switchStructure);
        annexActionSeparator = addCustomSeparator(messageHelper.getMessage("menu.annex.action"));
        AnnexStructureType switchStructureType = getSwitchStructureType(event.getStructureType());
        //Structure change
        switchStructure = createMenuItem(messageHelper.getMessage("menu.actions.separator.structure.change." + switchStructureType.getType()),
                selectedItem -> switchStructure(switchStructureType));
    }
    
    private void switchStructure(AnnexStructureType structureType) {
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("annex.structure.switch.window.title"),
                messageHelper.getMessage("annex.structure.switch.caption", StringUtils.capitalize(structureType.getType())),
                messageHelper.getMessage("annex.structure.switch.button.switch"),
                messageHelper.getMessage("annex.structure.switch.button.cancel"),
                null
        );
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);
        confirmDialog.show(getUI(), dialog -> {
            if (dialog.isConfirmed()) {
                eventBus.post(new AnnexStructureChangeEvent(structureType));
            }
        }, true);
    }
    
    private AnnexStructureType getSwitchStructureType(AnnexStructureType structureType) {
        return (structureType.getType() == AnnexStructureType.LEVEL.getType()) ? AnnexStructureType.ARTICLE : AnnexStructureType.LEVEL;
    }
    
    @Override
    protected void initDropDownMenu() {
        buildVersionActions();
        //buildStructureChangeAction();
        buildViewActions();
    }
    
}