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
import com.vaadin.server.Resource;
import com.vaadin.ui.MenuBar;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class ActionsMenuBarComponent extends MenuBar {

    private static final long serialVersionUID = -1111730147519706141L;
    private static final Logger LOG = LoggerFactory.getLogger(ActionsMenuBarComponent.class);

    protected MessageHelper messageHelper;
    protected EventBus eventBus;
    protected MenuItem mainMenuItem;

    @Autowired
    public ActionsMenuBarComponent(final MessageHelper messageHelper, final EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        addStyleName("leos-actions-menu");
        eventBus.register(this);
        buildActionsMenuBar();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }
    
    @Override
    public void detach() {
        eventBus.unregister(this);
        super.detach();
    }
    
    protected void buildActionsMenuBar() {
        LOG.debug("Building actions menu bar...");
        buildDropDownMenuItem();
        initDropDownMenu();
    }

    protected void buildDropDownMenuItem() {
        mainMenuItem = addItem("", LeosTheme.LEOS_HAMBURGUER_16, null);
        mainMenuItem.setStyleName("leos-actions-menu-selector");
        mainMenuItem.setDescription(messageHelper.getMessage("menu.actions.tooltip"));
    }

    abstract void initDropDownMenu();

    protected MenuItem addCustomSeparator(String label) {
        MenuItem customSeparator = mainMenuItem.addItem(label);
        customSeparator.setStyleName("leos-actions-custom-separator");
        return customSeparator;
    }

    protected MenuItem createMenuItem(String caption, Resource icon, MenuBar.Command command) {
        MenuItem menuItem = mainMenuItem.addItem(caption, icon, command);
        menuItem.setStyleName("leos-actions-sub-menu-item");
        return menuItem;
    }

    protected MenuItem createMenuItem(String caption, Command command) {
        return createMenuItem(caption, null, command);
    }

    protected MenuItem createCheckMenuItem(String caption, Command command) {
        MenuItem menuItem = createMenuItem(caption, LeosTheme.LEOS_CHECKBOX_16, command);
        menuItem.setCheckable(true);
        return menuItem;
    }
}
