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
package eu.europa.ec.leos.web.ui.component;

import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

import com.google.common.eventbus.EventBus;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.MenuBar;

import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.component.ReleaseAllLocksEvent;
import eu.europa.ec.leos.web.event.component.TocPositionEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.DownloadXmlRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EditMetadataRequestEvent;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.ui.screen.document.DocumentViewSettings;
import eu.europa.ec.leos.web.ui.screen.document.TocPosition;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuBarComponent extends MenuBar {

    private static final long serialVersionUID = -1111730147519706141L;

    private static final Logger LOG = LoggerFactory.getLogger(MenuBarComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private ConfigurationHelper cfgHelper;
    private DocumentViewSettings docViewSettings;

    private MenuBar.MenuItem tocOffItem;
    private MenuBar.MenuItem tocLeftItem;
    private MenuBar.MenuItem tocRightItem;
    
    private static final int CLOSE_INDEX=0;
    private static final int METADATA_INDEX=1;
    private static final int DOWNLOAD_INDEX=2;
    private static final int TOC_INDEX=3;
    private static final int LOCKS_INDEX=4;
    private static final int PROD_MODE_INDEX=5;

    
    public MenuBarComponent(final MessageHelper messageHelper, final EventBus eventBus, final ConfigurationHelper cfgHelper,
            final DocumentViewSettings docViewSettings) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.cfgHelper = cfgHelper;
        this.docViewSettings = docViewSettings;
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

        final MenuItem metadataItem = addItem(messageHelper.getMessage("menu.metadata.caption"), null);
        metadataItem.addItem(messageHelper.getMessage("menu.metadata.edit.caption"), LeosTheme.LEOS_MENU_ITEM_LOCKED_ICON_16, new MetaDataEditCommand());
        
        final MenuItem downloadItem = addItem(messageHelper.getMessage("menu.download.caption"), null);
        downloadItem.addItem(messageHelper.getMessage("menu.download.legfile.caption"), LeosTheme.LEOS_DOCUMENT_XML_ICON_16, notImplementedCmd);

        final MenuItem settingsItem = addItem("", LeosTheme.LEOS_MENU_SETTINGS_ICON_16, null);
        settingsItem.setDescription(messageHelper.getMessage("menu.settings.tooltip"));
        
        tocOffItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.tocoff.caption"), LeosTheme.LEOS_TOC_OFF_ICON_16, new LayoutMenuCommand(
                TocPosition.OFF));
        tocLeftItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.tocleft.caption"), LeosTheme.LEOS_TOC_LEFT_ICON_16, new LayoutMenuCommand(
                TocPosition.LEFT));
        tocRightItem = settingsItem.addItem(messageHelper.getMessage("menu.settings.tocright.caption"), LeosTheme.LEOS_TOC_RIGHT_ICON_16, new LayoutMenuCommand(
                        TocPosition.RIGHT));
        
        configureSettingsMenu();

        createLockMenu();
        
        String mode = cfgHelper.getProperty("leos.vaadin.productionMode");
        if (!Boolean.parseBoolean(mode)) {
            final MenuItem devMenuItem = addItem("", LeosTheme.LEOS_MENU_DEV_ICON_16, null);
            devMenuItem.setDescription(messageHelper.getMessage("menu.development.tooltip"));
            devMenuItem.addItem(messageHelper.getMessage("menu.development.xmlfile.caption"), new DownloadXmlCommand());
        }
    }
    
    private void createLockMenu(){
        final MenuItem lockItems = addItem("", LeosTheme.LEOS_MENU_LOCK_ICON_16, null);
        lockItems.setDescription(messageHelper.getMessage("menu.lock.tooltip"));
        lockItems.addItem(messageHelper.getMessage("menu.lock.releaseall.caption"), null, new ReleaseAllLocksCommand());
        MenuItem releaseAllMyLocks =lockItems.getChildren().get(0);//only child as of now 
        releaseAllMyLocks.setDescription(messageHelper.getMessage("menu.lock.releaseall.tooltip"));
    }
    
    // Configure the settings menu item
    private void configureSettingsMenu() {
        final boolean tocOff = TocPosition.OFF.equals(docViewSettings.getTocPosition());
        final boolean tocLeft = TocPosition.LEFT.equals(docViewSettings.getTocPosition());
        final boolean tocRight = TocPosition.RIGHT.equals(docViewSettings.getTocPosition());

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
                docViewSettings.setTocPosition(tocPosition);
                configureSettingsMenu();
                eventBus.post(new TocPositionEvent(tocPosition));
            }
        }
    }

    private class MetaDataEditCommand implements MenuBar.Command {
        private static final long serialVersionUID = -985893222028456485L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            LOG.debug("Metadata Edit menu item clicked...");
            eventBus.post(new EditMetadataRequestEvent());
        }
    }

    private class ReleaseAllLocksCommand implements MenuBar.Command {
        private static final long serialVersionUID = -985893222028454485L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            LOG.debug("Release All menu item clicked...");
            eventBus.post(new ReleaseAllLocksEvent());
            eventBus.post(new CloseDocumentEvent());
        }
    }

    
    private class DownloadXmlCommand implements MenuBar.Command {
        private static final long serialVersionUID = 5595453150823239001L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            LOG.debug("Download XML File menu item clicked...");
            eventBus.post(new DownloadXmlRequestEvent());
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
    
    public void updateLocks(LockActionInfo lockActionInfo) {
        LockData lock = lockActionInfo.getLock();
        MenuItem metadataMenu = getItems().get(METADATA_INDEX);
        MenuItem metadataEdit = metadataMenu.getChildren().get(0);

        String currentSessionId= VaadinSession.getCurrent().getSession().getId();
        boolean metadataEditEnabaled=true;
        for (LockData lockData : lockActionInfo.getCurrentLocks()) {
            if( !currentSessionId.equals(lockData.getSessionId())
                    && (LockLevel.DOCUMENT_LOCK.equals(lockData.getLockLevel())
                        || LockLevel.ELEMENT_LOCK.equals(lockData.getLockLevel()))){
                //if some other session has acquired the element or doc lock...disable the metadata edit              
                metadataEditEnabaled=false;
                break;
            }
        }
        if(metadataEditEnabaled){
            metadataEdit.setEnabled(true);
            metadataEdit.setIcon(null);
            metadataEdit.setDescription("");
        }
        else{
            metadataEdit.setEnabled(false);
            metadataEdit.setIcon(LeosTheme.LEOS_MENU_ITEM_LOCKED_ICON_16);
            metadataEdit.setDescription(messageHelper.getMessage("menu.metadata.edit.locked.tooltop", lock.getUserName()));

        }
    }
}
