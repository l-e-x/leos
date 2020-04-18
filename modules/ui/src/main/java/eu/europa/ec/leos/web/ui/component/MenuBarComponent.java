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

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.MenuBar;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class MenuBarComponent extends MenuBar {

    private static final long serialVersionUID = -1111730147519706141L;

    private static final Logger LOG = LoggerFactory.getLogger(MenuBarComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;

    @Autowired
    public MenuBarComponent(final MessageHelper messageHelper, final EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        buildViewMenubar();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        eventBus.unregister(this);
        super.detach();
    }

    private void buildViewMenubar() {
        LOG.debug("Building view menubar...");

        addStyleName("leos-document-layout");

        // create close item
        MenuItem closeDocumentItem = addItem(messageHelper.getMessage("menu.close.caption"),
                LeosTheme.LEOS_DOCVIEW_CLOSE,
                new CloseDocumentCommand());
        closeDocumentItem.setDescription(messageHelper.getMessage("menu.close.tooltip"));
    }

    private class CloseDocumentCommand implements Command {

        private static final long serialVersionUID = -2486576157838091413L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            LOG.trace("Close document menu clicked...");
            eventBus.post(new CloseScreenRequestEvent());
        }
    }
}
