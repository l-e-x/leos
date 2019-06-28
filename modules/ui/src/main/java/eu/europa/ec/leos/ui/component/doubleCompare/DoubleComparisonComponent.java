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
package eu.europa.ec.leos.ui.component.doubleCompare;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.*;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.event.EnableSyncScrollRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.DocuWriteExportRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.VersionSelectorUpdateEvent;
import eu.europa.ec.leos.ui.extension.MathJaxExtension;
import eu.europa.ec.leos.ui.extension.ScrollPaneExtension;
import eu.europa.ec.leos.ui.extension.SliderPinsExtension;
import eu.europa.ec.leos.ui.extension.SoftActionsExtension;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DoubleComparisonComponent<T extends XmlDocument> extends CustomComponent implements ContentPane {
    
    private static final long serialVersionUID = -826802129383432798L;
    private static final Logger LOG = LoggerFactory.getLogger(DoubleComparisonComponent.class);
    private static final String LEOS_RELATIVE_FULL_WDT = "100%";

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private UserHelper userHelper;
    private LeosDisplayField doubleComparisonContent;
    private Button exportToDocuwriteButton;
    private DoubleCompareVersionSelector<T> sliderPopup;
    private Button syncScrollSwitch;
    private ScrollPaneExtension scrollPaneExtension;
    
    private FileDownloader fileDownloader;

    public DoubleComparisonComponent(final EventBus eventBus, final MessageHelper messageHelper, final UserHelper userHelper) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.userHelper = userHelper;

        setSizeFull();
        VerticalLayout doubleComparisonLayout = new VerticalLayout();
        doubleComparisonLayout.setSizeFull();
        doubleComparisonLayout.setSpacing(false);
        doubleComparisonLayout.setMargin(false);
        
        // create toolbar
        doubleComparisonLayout.addComponent(buildDoubleComparisonToolbar());
        // create content
        final Component comparisonContent = buildDoubleComparisonContent();

        doubleComparisonLayout.addComponent(comparisonContent);
        doubleComparisonLayout.setExpandRatio(comparisonContent, 1.0f);
        setCompositionRoot(doubleComparisonLayout);
        
        initDownloader();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }
    
    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
    }
    
    private Component buildDoubleComparisonToolbar() {
        LOG.debug("Building double comparison toolbar...");

        // create text toolbar layout
        final HorizontalLayout toolsLayout = new HorizontalLayout();
        toolsLayout.setId("doubleComparisonToolbar");
        toolsLayout.setStyleName("leos-doubleComparison-bar");

        // set toolbar style
        toolsLayout.setWidth(LEOS_RELATIVE_FULL_WDT);

        //create sync scroll
        syncScrollSwitch();
        toolsLayout.addComponent(syncScrollSwitch);
        toolsLayout.setComponentAlignment(syncScrollSwitch, Alignment.MIDDLE_LEFT);

        //create version selector
        versionSelector();
        toolsLayout.addComponent(sliderPopup);
        toolsLayout.setComponentAlignment(sliderPopup, Alignment.MIDDLE_CENTER);
        toolsLayout.setExpandRatio(sliderPopup,1.0f);

        // create print button
        exportToDocuwriteButton();
        toolsLayout.addComponent(exportToDocuwriteButton);
        toolsLayout.setComponentAlignment(exportToDocuwriteButton, Alignment.MIDDLE_RIGHT);

        return toolsLayout;
    }

    private void syncScrollSwitch() {
        VaadinIcons syncIcon = VaadinIcons.EXCHANGE;
        syncScrollSwitch = new Button();
        syncScrollSwitch.setIcon(syncIcon);
        syncScrollSwitch.setStyleName("link");
        syncScrollSwitch.addStyleName("leos-toolbar-button enable-sync");
        syncScrollSwitch.setData(true);
        syncScrollSwitch.setDescription(messageHelper.getMessage("leos.button.tooltip.disable.sync"), ContentMode.HTML);
        
        syncScrollSwitch.addClickListener(event -> {
            Button button = event.getButton();
            boolean syncState = !(boolean) button.getData();
            button = updateStyle(button, syncState);
            eventBus.post(new EnableSyncScrollRequestEvent(syncState));
            button.setDescription(syncState
                    ? messageHelper.getMessage("leos.button.tooltip.disable.sync")
                    : messageHelper.getMessage("leos.button.tooltip.enable.sync"));

            button.setData(syncState);
        });
    }

    private Button updateStyle(Button button, boolean syncState) {
        if(syncState) {
            button.removeStyleName("disable-sync");
            button.addStyleName("enable-sync");
        } else {
            button.removeStyleName("enable-sync");
            button.addStyleName("disable-sync");
        }
        return button;
    }

    private DoubleCompareVersionSelector<T> versionSelector() {
        sliderPopup = new DoubleCompareVersionSelector<>(messageHelper, eventBus, userHelper);
        sliderPopup.addPopupVisibilityListener(event -> {
            if(event.isPopupVisible()) {
                eventBus.post(new VersionListRequestEvent<T>()); // get latest version information
            }
        });
        
        return sliderPopup;
    }
    
    private Component buildDoubleComparisonContent() {
        doubleComparisonContent = new LeosDisplayField();
        doubleComparisonContent.setSizeFull();
        doubleComparisonContent.setId("leos-double-comparison-content");
        doubleComparisonContent.setStyleName("leos-double-comparison-content");
        
        new MathJaxExtension<>(doubleComparisonContent);
        new SliderPinsExtension<>(doubleComparisonContent, getSelectorStyleMap());
        new SoftActionsExtension<>(doubleComparisonContent);
        scrollPaneExtension = new ScrollPaneExtension(doubleComparisonContent, eventBus);
        scrollPaneExtension.getState().idPrefix = "doubleCompare-";
        scrollPaneExtension.getState().containerSelector = ".leos-double-comparison-content";

        return doubleComparisonContent;
    }
    
    // create export button
    private Button exportToDocuwriteButton() {
        exportToDocuwriteButton = new Button();
        exportToDocuwriteButton.setDescription(messageHelper.getMessage("leos.button.tooltip.export.docuwrite"));
        exportToDocuwriteButton.addStyleName("link leos-toolbar-button");
        exportToDocuwriteButton.setIcon(VaadinIcons.SHARE_SQUARE);
        return exportToDocuwriteButton;
    }
    
    private void initDownloader() {
        // Resource cannot be null at instantiation time of the FileDownloader, creating a dummy one
        FileResource downloadStreamResource = new FileResource(new File(""));
        fileDownloader = new FileDownloader(downloadStreamResource) {
            private static final long serialVersionUID = -4584979099145066535L;
            @Override
            public boolean handleConnectorRequest(VaadinRequest request, VaadinResponse response, String path) throws IOException {
                boolean result = false;
                try {
                    eventBus.post(new DocuWriteExportRequestEvent<T>(ExportOptions.TO_WORD_DW_LT, sliderPopup.getSelectedVersions().get(0)));
                    result = super.handleConnectorRequest(request, response, path);
                } catch (Exception exception) {
                    LOG.error("Error occured in download", exception.getMessage());
                }

                return result;
            }
        };
        fileDownloader.extend(exportToDocuwriteButton);
    }
    
    public void setDownloadStreamResource(Resource downloadstreamResource) {
        fileDownloader.setFileDownloadResource(downloadstreamResource);
    }

    private Map<String, String> getSelectorStyleMap(){
        Map<String, String> selectorStyleMap = new HashMap<>();
        selectorStyleMap.put(".leos-double-compare-removed-intermediate","pin-leos-double-compare-removed-intermediate");
        selectorStyleMap.put(".leos-double-compare-added-intermediate","pin-leos-double-compare-added-intermediate");
        selectorStyleMap.put(".leos-double-compare-removed-original","pin-leos-double-compare-removed-original");
        selectorStyleMap.put(".leos-double-compare-added-original", "pin-leos-double-compare-added-original");
        selectorStyleMap.put(".leos-double-compare-removed","pin-leos-double-compare-removed");
        selectorStyleMap.put(".leos-double-compare-added", "pin-leos-double-compare-added");
        return selectorStyleMap;
    }
    
    @Subscribe
    public void refreshPane(VersionSelectorUpdateEvent event) {
        exportToDocuwriteButton.setEnabled(event.isVersionSelectorState());
        syncScrollSwitch.setEnabled(event.isVersionSelectorState());
        scrollPaneExtension.getState().enableSync = event.isVersionSelectorState();
        setEnabled(event.isVersionSelectorState());
    }
    
    public void populateDoubleComparisonContent(String comparisonContent, LeosCategory leosCategory) {
        doubleComparisonContent.addStyleName(leosCategory.name().toLowerCase());
        doubleComparisonContent.setValue(comparisonContent);
    }
    
    @Override
    public float getDefaultPaneWidth(int numberOfFeatures, boolean tocPresent) {
        final float featureWidth;
        switch(numberOfFeatures){
            case 1:
                featureWidth=100f;
                break;
            default:
                if(tocPresent) {
                    featureWidth = 42.5f;
                } else {
                    featureWidth = 50f;
                }
                break;
        }//end switch
        return featureWidth;
    }

    @Override
    public Class getChildClass() {
        return null;
    }
}
