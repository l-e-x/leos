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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.MarkedTextNavigationRequestEvent;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.extension.MathJaxExtension;
import eu.europa.ec.leos.web.ui.extension.SliderPinsExtension;
import eu.europa.ec.leos.web.ui.extension.MarkedTextNavigationHelper;
import eu.europa.ec.leos.web.ui.screen.ViewSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.teemu.VaadinIcons;

import java.util.HashMap;
import java.util.Map;

@StyleSheet({"theme://css/changes_xml.css" + LeosCacheToken.TOKEN})
public class MarkedTextComponent extends CustomComponent {
    private static final long serialVersionUID = -826802129383432798L;
    private static final Logger LOG = LoggerFactory.getLogger(MarkedTextComponent.class);
    private static final String LEOS_RELATIVE_FULL_WDT = "100%";

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private ViewSettings viewSettings;
    private Label markedContent;

    public MarkedTextComponent(final EventBus eventBus, final MessageHelper messageHelper, ViewSettings viewSettings) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.viewSettings = viewSettings;

        setSizeFull();
        VerticalLayout markedTextLayout = new VerticalLayout();
        markedTextLayout.setSizeFull();
        // create toolbar
        markedTextLayout.addComponent(buildMarkedTextToolbar());
        // create content
        final Component textContent = buildMarkedTextContent();

        markedTextLayout.addComponent(textContent);
        markedTextLayout.setExpandRatio(textContent, 1.0f);
        setCompositionRoot(markedTextLayout);
    }

    private Component buildMarkedTextToolbar() {
        LOG.debug("Building marked Text toolbar...");

        // create text toolbar layout
        final HorizontalLayout toolsLayout = new HorizontalLayout();
        toolsLayout.setId("markedTextToolbar");
        toolsLayout.setStyleName("leos-markedtext-bar");
        toolsLayout.setSpacing(true);

        // set toolbar style
        toolsLayout.setWidth(LEOS_RELATIVE_FULL_WDT);

        //create version selector
        Component versionSelector= versionSelector();
        toolsLayout.addComponent(versionSelector);
        toolsLayout.setComponentAlignment(versionSelector, Alignment.MIDDLE_CENTER);
        toolsLayout.setExpandRatio(versionSelector,1.0f);

        final Button markedTextPrevButton = markedTextPrevNavigationButton();
        markedTextPrevButton.setDescription(messageHelper.getMessage("version.changes.navigation.prev"));
        toolsLayout.addComponent(markedTextPrevButton);
        toolsLayout.setComponentAlignment(markedTextPrevButton, Alignment.MIDDLE_RIGHT);
        
        final Button markedTextNextButton = markedTextNextNavigationButton();
        markedTextNextButton.setDescription(messageHelper.getMessage("version.changes.navigation.next"));
        toolsLayout.addComponent(markedTextNextButton);
        toolsLayout.setComponentAlignment(markedTextNextButton, Alignment.MIDDLE_RIGHT);
        
        // create legal text slider button
        final Button markedTextSlideButton = markedTextSliderButton();
        toolsLayout.addComponent(markedTextSlideButton);
        toolsLayout.setComponentAlignment(markedTextSlideButton, Alignment.MIDDLE_RIGHT);

        return toolsLayout;
    }

    // create popup view
    private VersionSliderPopup versionSelector() {
        final VersionSliderPopup sliderPopup = new VersionSliderPopup(messageHelper, eventBus);

        sliderPopup.addPopupVisibilityListener(new PopupView.PopupVisibilityListener() {
            @Override
            public final void popupVisibilityChange(final PopupView.PopupVisibilityEvent event) {
                if(event.isPopupVisible()) {//popup opened
                    LOG.debug("popup opened");
                    eventBus.post(new VersionListRequestEvent());// get latest version information
                }
                //else{//popup closed Do Nothing
            }
        });
        return sliderPopup;
    }

    private Button markedTextPrevNavigationButton() {
        VaadinIcons markedTextPrevIcon = VaadinIcons.ANGLE_UP;
        final Button markedTextPrevButton = new Button();
        markedTextPrevButton.setIcon(markedTextPrevIcon);
        markedTextPrevButton.setStyleName("link");
        markedTextPrevButton.addStyleName("leos-toolbar-button");
        markedTextPrevButton.addClickListener(new ClickListener() {
            
            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new MarkedTextNavigationRequestEvent(MarkedTextNavigationRequestEvent.NAV_DIRECTION.PREV));
            }
        });
        return markedTextPrevButton;
    }
    
    private Button markedTextNextNavigationButton() {
        VaadinIcons markedTextNextIcon = VaadinIcons.ANGLE_DOWN;
        final Button markedTextNextButton = new Button();
        markedTextNextButton.setIcon(markedTextNextIcon);
        markedTextNextButton.setStyleName("link");
        markedTextNextButton.addStyleName("leos-toolbar-button");
        markedTextNextButton.addClickListener(new ClickListener() {
            
            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new MarkedTextNavigationRequestEvent(MarkedTextNavigationRequestEvent.NAV_DIRECTION.NEXT));
            }
        });
        return markedTextNextButton;
    }
    
    // create legal text slider button
    private Button markedTextSliderButton() {
        VaadinIcons markedTextSliderIcon = VaadinIcons.CARET_SQUARE_LEFT_O;

        final Button markedTextSliderButton = new Button();
        markedTextSliderButton.setIcon(markedTextSliderIcon);
        markedTextSliderButton.setData(SplitPositionEvent.MoveDirection.LEFT);
        markedTextSliderButton.setStyleName("link");
        markedTextSliderButton.addStyleName("leos-toolbar-button");
        markedTextSliderButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(), MarkedTextComponent.this));
            }
        });

        return markedTextSliderButton;
    }

    private Component buildMarkedTextContent() {
        LOG.debug("Building Marked Text content...");

        // create placeholder to display marked content
        markedContent = new Label();
        markedContent.setContentMode(ContentMode.HTML);
        markedContent.setSizeFull();
        markedContent.setId("leos-marked-content");
        markedContent.setStyleName("leos-marked-content");
        MathJaxExtension mathjax = new MathJaxExtension();
        mathjax.extend(markedContent);
        SliderPinsExtension sliderPins=new SliderPinsExtension(eventBus, getSelectorStyleMap());
        MarkedTextNavigationHelper navHelper = new MarkedTextNavigationHelper(sliderPins);
        this.eventBus.register(navHelper);//Registering helper object to eventBus. Presently this method is called only once if multiple invocation occurs in future will need to unregister the object on close of document.
        sliderPins.extend(markedContent);
        return markedContent;
    }

    private Map<String, String> getSelectorStyleMap(){
        Map<String, String> selectorStyleMap = new HashMap<>();
        selectorStyleMap.put(".leos-marker-content-removed","pin-leos-marker-content-removed");
        selectorStyleMap.put(".leos-marker-content-added","pin-leos-marker-content-added");
        selectorStyleMap.put(".leos-content-modified","pin-leos-content-modified");
        selectorStyleMap.put(".leos-content-removed","pin-leos-content-removed");
        selectorStyleMap.put(".leos-content-new", "pin-leos-content-new");
        return selectorStyleMap;
    }

    public void populateMarkedContent(String markedContentText) {
        markedContent.setValue(markedContentText);
    }
}
