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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.*;
import eu.europa.ec.leos.web.event.LeosUIComponentsStateUpdateEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;

public class AbstractWindow extends Window {
    private final Panel bodyComponentWrapper;
    private final HorizontalLayout buttonsArea;
    private final HorizontalLayout buttonsLeftArea;
    protected final MessageHelper messageHelper;
    protected final EventBus eventBus;

    protected AbstractWindow(MessageHelper messageHelper, final EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.bodyComponentWrapper = new Panel();
        this.buttonsArea = new HorizontalLayout();
        this.buttonsLeftArea = new HorizontalLayout();
        this.eventBus = eventBus;
        this.setClosable(false);

        this.addAttachListener(new AttachListener() {
            @Override
            public void attach(AttachEvent event) {
                eventBus.post(new LeosUIComponentsStateUpdateEvent(false));
            }
        });
        this.addDetachListener(new DetachListener() {
            @Override
            public void detach(DetachEvent event) {
                eventBus.post(new LeosUIComponentsStateUpdateEvent(true));
            }
        });
        setResizable(false);

        initLayout();
    }

    private void initLayout() {
        VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setSizeFull();

        bodyComponentWrapper.setSizeFull();
        verticalLayout.addComponent(bodyComponentWrapper);
        verticalLayout.setExpandRatio(bodyComponentWrapper, 1.0f);

        HorizontalLayout buttonsLayout = new HorizontalLayout();
        buttonsLayout.setWidth(100, Unit.PERCENTAGE);

        HorizontalLayout buildButtonsLeftArea = buildButtonsLeftArea();
        buttonsLayout.addComponent(buildButtonsLeftArea);
        HorizontalLayout buildButtonsArea = buildButtonsArea();
        buttonsLayout.addComponent(buildButtonsArea);
        buttonsLayout.setExpandRatio(buildButtonsArea, 1);
        verticalLayout.addComponent(buttonsLayout);

        setContent(verticalLayout);
    }

    protected void setBodyComponent(Component component) {
        bodyComponentWrapper.setContent(component);
    }

    protected void addButton(Button button) {
        buttonsArea.addComponent(button, 1);
    }

    protected void addButtonOnLeft(Button button) {
        buttonsLeftArea.addComponent(button);
    }

    protected boolean hasCloseButton() {
        return true;
    }

    private HorizontalLayout buildButtonsArea() {
        buttonsArea.setSpacing(true);
        buttonsArea.setStyleName("leos-window-buttons-area");

        buttonsArea.setWidth(100, Unit.PERCENTAGE);
        Label spacer = new Label("");
        buttonsArea.addComponent(spacer, 0);
        buttonsArea.setExpandRatio(spacer, 1f);

        if (hasCloseButton()) {
            Button closeButton = new Button(messageHelper.getMessage("leos.button.close"));
            closeButton.addClickListener(new Button.ClickListener() {
                @Override
                public void buttonClick(Button.ClickEvent event) {
                    handleCloseButton();
                }
            });
            buttonsArea.addComponent(closeButton);
        }
        return buttonsArea;
    }

    private HorizontalLayout buildButtonsLeftArea() {
        buttonsLeftArea.setHeight(100, Unit.PERCENTAGE);
        buttonsLeftArea.setSpacing(true);
        buttonsLeftArea.setStyleName("leos-window-buttons-area");
        return buttonsLeftArea;
    }

    protected void handleCloseButton() {
        close();
    }
}
