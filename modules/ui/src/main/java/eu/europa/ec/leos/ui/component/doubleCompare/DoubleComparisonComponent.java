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
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FileResource;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.component.markedText.MarkedTextNavigationHelper;
import eu.europa.ec.leos.ui.event.EnableSyncScrollRequestEvent;
import eu.europa.ec.leos.ui.extension.MathJaxExtension;
import eu.europa.ec.leos.ui.extension.ScrollPaneExtension;
import eu.europa.ec.leos.ui.extension.SliderPinsExtension;
import eu.europa.ec.leos.ui.extension.SoftActionsExtension;
import eu.europa.ec.leos.web.event.component.MarkedTextNavigationRequestEvent;
import eu.europa.ec.leos.web.event.view.document.ComparisonEvent;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@ViewScope
@SpringComponent
public class DoubleComparisonComponent extends CustomComponent implements ContentPane {
    
    private static final long serialVersionUID = -826802129383432798L;
    private static final Logger LOG = LoggerFactory.getLogger(DoubleComparisonComponent.class);
    private static final String LEOS_RELATIVE_FULL_WDT = "100%";

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private LeosDisplayField doubleComparisonContent;
    private Button exportToDocuwriteButton;
    private Button syncScrollSwitch;
    private Button markedTextNextButton;
    private Button markedTextPrevButton;

    private FileDownloader fileDownloader;
    private Label versionLabel;

    public DoubleComparisonComponent(final EventBus eventBus, final MessageHelper messageHelper) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;

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
        hideCompareButtons();
        hideExportToDocuwriteButton();
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
        
        markedTextNextButton = markedTextNextNavigationButton();
        markedTextNextButton.setDescription(messageHelper.getMessage("version.changes.navigation.next"));
        toolsLayout.addComponent(markedTextNextButton);
        toolsLayout.setComponentAlignment(markedTextNextButton, Alignment.MIDDLE_LEFT);
        
        markedTextPrevButton = markedTextPrevNavigationButton();
        markedTextPrevButton.setDescription(messageHelper.getMessage("version.changes.navigation.prev"));
        toolsLayout.addComponent(markedTextPrevButton);
        toolsLayout.setComponentAlignment(markedTextPrevButton, Alignment.MIDDLE_LEFT);

        //create version selector
        versionLabel = new Label();
        versionLabel.setSizeUndefined();
        versionLabel.setContentMode(ContentMode.HTML);
        toolsLayout.addComponent(versionLabel);
        toolsLayout.setComponentAlignment(versionLabel, Alignment.MIDDLE_CENTER);
        toolsLayout.setExpandRatio(versionLabel,1.0f);

        // create print button
        exportToDocuwriteButton();
        toolsLayout.addComponent(exportToDocuwriteButton);
        toolsLayout.setComponentAlignment(exportToDocuwriteButton, Alignment.MIDDLE_RIGHT);

        Button closeButton = closeMarkedTextComponent();
        toolsLayout.addComponent(closeButton);
        toolsLayout.setComponentAlignment(closeButton, Alignment.MIDDLE_RIGHT);
        
        return toolsLayout;
    }
    
    private Button closeMarkedTextComponent() {
        Button closeButton = new Button();
        closeButton.setDescription(messageHelper.getMessage("version.compare.close.button.description"));
        closeButton.addStyleName("link leos-toolbar-button");
        closeButton.setIcon(VaadinIcons.CLOSE_CIRCLE);
        closeButton.addClickListener(event -> eventBus.post(new ComparisonEvent(false)));
        return closeButton;
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
            updateStyle(button, syncState);
            eventBus.post(new EnableSyncScrollRequestEvent(syncState));
            button.setDescription(syncState
                    ? messageHelper.getMessage("leos.button.tooltip.disable.sync")
                    : messageHelper.getMessage("leos.button.tooltip.enable.sync"));

            button.setData(syncState);
        });
    }

    private Button markedTextPrevNavigationButton() {
        VaadinIcons markedTextPrevIcon = VaadinIcons.CARET_UP;
        final Button markedTextPrevButton = new Button();
        markedTextPrevButton.setIcon(markedTextPrevIcon);
        markedTextPrevButton.addStyleName("link leos-toolbar-button navigation-btn");
        markedTextPrevButton.addClickListener(event -> eventBus.post(new MarkedTextNavigationRequestEvent(MarkedTextNavigationRequestEvent.NAV_DIRECTION.PREV)));
        return markedTextPrevButton;
    }
    
    private Button markedTextNextNavigationButton() {
        VaadinIcons markedTextNextIcon = VaadinIcons.CARET_DOWN;
        final Button markedTextNextButton = new Button();
        markedTextNextButton.setIcon(markedTextNextIcon);
        markedTextNextButton.addStyleName("link leos-toolbar-button navigation-btn");
        markedTextNextButton.addClickListener(event -> eventBus.post(new MarkedTextNavigationRequestEvent(MarkedTextNavigationRequestEvent.NAV_DIRECTION.NEXT)));
        return markedTextNextButton;
    }
    
    private void updateStyle(Button button, boolean syncState) {
        if(syncState) {
            button.removeStyleName("disable-sync");
            button.addStyleName("enable-sync");
        } else {
            button.removeStyleName("enable-sync");
            button.addStyleName("disable-sync");
        }
    }

    private Component buildDoubleComparisonContent() {
        doubleComparisonContent = new LeosDisplayField();
        doubleComparisonContent.setSizeFull();
        doubleComparisonContent.setId("leos-double-comparison-content");
        doubleComparisonContent.setStyleName("leos-double-comparison-content");
        
        new MathJaxExtension<>(doubleComparisonContent);
        new SoftActionsExtension<>(doubleComparisonContent);
        ScrollPaneExtension scrollPaneExtension = new ScrollPaneExtension(doubleComparisonContent, eventBus);
        scrollPaneExtension.getState().idPrefix = "doubleCompare-";
        scrollPaneExtension.getState().containerSelector = ".leos-double-comparison-content";

        SliderPinsExtension<LeosDisplayField> sliderPins = new SliderPinsExtension<>(doubleComparisonContent, getSelectorStyleMap());
        MarkedTextNavigationHelper navHelper = new MarkedTextNavigationHelper(sliderPins);
        this.eventBus.register(navHelper);

        return doubleComparisonContent;
    }
    
    // create export button
    private void exportToDocuwriteButton() {
        exportToDocuwriteButton = new Button();
        exportToDocuwriteButton.setDescription(messageHelper.getMessage("leos.button.tooltip.export.docuwrite"));
        exportToDocuwriteButton.addStyleName("link leos-toolbar-button");
        exportToDocuwriteButton.setIcon(VaadinIcons.DOWNLOAD);
    }
    
    private void initDownloader() {
        // Resource cannot be null at instantiation time of the FileDownloader, creating a dummy one
        FileResource downloadStreamResource = new FileResource(new File(""));
        fileDownloader = new FileDownloader(downloadStreamResource) {
            private static final long serialVersionUID = -4584979099145066535L;
            @Override
            public boolean handleConnectorRequest(VaadinRequest request, VaadinResponse response, String path) {
                boolean result = false;
                try {
//                    final String downloadVersion = ""; //TODO understand what version will be able for download. populateDoubleComparisonContent() accepts already compared result
//                    eventBus.post(new DocuWriteExportRequestEvent(ExportOptions.TO_WORD_DW_LT, T);
                    result = super.handleConnectorRequest(request, response, path);
                } catch (Exception exception) {
                    LOG.error("Error occurred in download: " + exception.getMessage(), exception);
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
        selectorStyleMap.put(".leos-double-compare-removed-intermediate","pin-leos-double-compare-removed-intermediate-hidden");
        selectorStyleMap.put(".leos-double-compare-added-intermediate","pin-leos-double-compare-added-intermediate-hidden");
        selectorStyleMap.put(".leos-double-compare-removed-original","pin-leos-double-compare-removed-original-hidden");
        selectorStyleMap.put(".leos-double-compare-added-original", "pin-leos-double-compare-added-original-hidden");
        selectorStyleMap.put(".leos-double-compare-removed","pin-leos-double-compare-removed-hidden");
        selectorStyleMap.put(".leos-double-compare-added", "pin-leos-double-compare-added-hidden");
        selectorStyleMap.put(".leos-content-removed", "pin-leos-content-removed-hidden");
        selectorStyleMap.put(".leos-content-new", "pin-leos-content-new-hidden");
        return selectorStyleMap;
    }

    public void populateDoubleComparisonContent(String comparisonContent, LeosCategory leosCategory, String comparedInfo) {
        doubleComparisonContent.addStyleName(leosCategory.name().toLowerCase());
        doubleComparisonContent.setValue(comparisonContent);
        versionLabel.setValue(comparedInfo);
    }

    public void hideCompareButtons() {
        markedTextNextButton.setVisible(false);
        markedTextPrevButton.setVisible(false);
    }

    public void showCompareButtons() {
        markedTextNextButton.setVisible(true);
        markedTextPrevButton.setVisible(true);
    }
    
    public void hideExportToDocuwriteButton() {
        exportToDocuwriteButton.setVisible(false);
    }
    
    public void showExportToDocuwriteButton() {
        exportToDocuwriteButton.setVisible(true);
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
}
