/**
 * Copyright 2015 European Commission
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
package eu.europa.ec.leos.web.ui.screen.feedback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.MenuBar;

import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.component.TocPositionEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.screen.document.TocPosition;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;

public class FeedbackMenuComponent extends MenuBar {

    private static final long serialVersionUID = -1111730147519706141L;

    private static final Logger LOG = LoggerFactory.getLogger(FeedbackMenuComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private FeedbackViewSettings viewSettings;

    private MenuBar.MenuItem tocOffItem;
    private MenuBar.MenuItem tocLeftItem;
    private MenuBar.MenuItem tocRightItem;
    
    public FeedbackMenuComponent(final MessageHelper messageHelper, final EventBus eventBus, final ConfigurationHelper cfgHelper,
            final FeedbackViewSettings viewSettings) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.viewSettings = viewSettings;
        buildViewMenubar();
    }

    private void buildViewMenubar() {
        LOG.debug("Building view menubar...");

        addStyleName("leos-document-layout");

        // create default not implemented action for menu items
        final Command notImplementedCmd = new Command() {
            private static final long serialVersionUID = 3775227587732968858L;

            @Override
            public void menuSelected(MenuBar.MenuItem menuItem) {
                LOG.debug("Menu item clicked ({})...", menuItem.getText());
                eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "leos.not.implemented", "Menu action: " + menuItem.getText()));
            }
        };

        // create menu items
        MenuItem closeDocumentItem = addItem(messageHelper.getMessage("menu.close.caption"), LeosTheme.LEOS_DOCVIEW_CLOSE, new CloseDocumentCommand());
        closeDocumentItem.setDescription(messageHelper.getMessage("menu.close.tooltip"));

        final MenuItem settingsItem = addItem("", LeosTheme.LEOS_MENU_SETTINGS_ICON_16, null);
        settingsItem.setDescription(messageHelper.getMessage("menu.settings.tooltip"));
        
        tocOffItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.tocoff.caption"), LeosTheme.LEOS_TOC_OFF_ICON_16, new LayoutMenuCommand(
                TocPosition.OFF));
        tocLeftItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.tocleft.caption"), LeosTheme.LEOS_TOC_LEFT_ICON_16, new LayoutMenuCommand(
                TocPosition.LEFT));
        tocRightItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.tocright.caption"), LeosTheme.LEOS_TOC_RIGHT_ICON_16, new LayoutMenuCommand(
                        TocPosition.RIGHT));
        
        configureSettingsMenu();

    }
    
    // Configure the settings menu item
    private void configureSettingsMenu() {
        final boolean tocOff = TocPosition.OFF.equals(viewSettings.getTocPosition());
        final boolean tocLeft = TocPosition.LEFT.equals(viewSettings.getTocPosition());
        final boolean tocRight = TocPosition.RIGHT.equals(viewSettings.getTocPosition());

        tocOffItem.setCheckable(true);
        tocOffItem.setChecked(tocOff);
        tocOffItem.setEnabled(!tocOff);

        tocLeftItem.setCheckable(true);
        tocLeftItem.setChecked(tocLeft);
        tocLeftItem.setEnabled(!tocLeft);

        tocRightItem.setCheckable(true);
        tocRightItem.setChecked(tocRight);
        tocRightItem.setEnabled(!tocRight);
    }

    // Create Layout for select menu commands for TOC
    private class LayoutMenuCommand implements MenuBar.Command {
        private static final long serialVersionUID = -4455740778411909392L;

        private TocPosition tocPosition;

        public LayoutMenuCommand(TocPosition tocPosition) {
            this.tocPosition = tocPosition;
        }

        @Override
        public void menuSelected(MenuBar.MenuItem menuItem) {
            LOG.debug("Layout menu item clicked ({}:{})...", tocPosition, menuItem.isChecked());
            if (menuItem.isEnabled() && menuItem.isCheckable() && menuItem.isChecked()) {
                LOG.debug("Changing layout settings (TOC={})...", tocPosition);
                viewSettings.setTocPosition(tocPosition);
                configureSettingsMenu();
                eventBus.post(new TocPositionEvent(tocPosition));
            }
        }
    }

    private class CloseDocumentCommand implements Command {

        private static final long serialVersionUID = -2486576157838091413L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            LOG.trace("Close document menu clicked...");
            eventBus.post(new CloseDocumentEvent());
        }
    }
}
