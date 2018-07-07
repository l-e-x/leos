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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;

public abstract class AbstractEditWindow extends AbstractWindow {

    protected Button saveButton;

    protected AbstractEditWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);

        addButton(buildSaveButton());
    }

    private Button buildSaveButton() {
        saveButton = new Button(messageHelper.getMessage("leos.button.save"));
        saveButton.addStyleName("primary");
        saveButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                onSave();
            }
        });
        return saveButton;
    }

    protected abstract void onSave();

    protected Button buildDapButton(final String dapUrl) {
        Button button = new Button(messageHelper.getMessage("leos.button.dap"));
        button.setDescription(messageHelper.getMessage("leos.button.dap.description"));
        button.setIcon(LeosTheme.LEOS_DAP_ICON_16);
        button.addClickListener(new ClickListener() {
            private static final long serialVersionUID = -5633348109667050418L;

            @Override
            public void buttonClick(ClickEvent event) {
                Page.getCurrent().open(dapUrl, "_new");
            }
        });
        return button;
    }
}
