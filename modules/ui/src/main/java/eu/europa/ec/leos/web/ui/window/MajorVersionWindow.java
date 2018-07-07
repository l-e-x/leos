/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.Button;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.web.event.view.document.SaveMajorVersionEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MajorVersionWindow extends AbstractWindow {

    private static final long serialVersionUID = -7318940290911926353L;
    private static final Logger LOG = LoggerFactory.getLogger(MajorVersionWindow.class);
    
    private TextArea commentArea;
    
    public MajorVersionWindow(MessageHelper messageHelper, EventBus eventBus) {
        super(messageHelper, eventBus);
        setCaption(messageHelper.getMessage("document.major.version.window.title"));
        prepareWindow();
    }

    public void prepareWindow() {
        setWidth("420px");
        setHeight("300px");
        
        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setSizeFull();
        windowLayout.setMargin(false);
        windowLayout.setSpacing(false);
        setBodyComponent(windowLayout);

        buildLayout(windowLayout);
        
        Button saveButon = buildSaveButton();
        addButton(saveButon);
    }

    private void buildLayout(VerticalLayout windowLayout) {
        commentArea =  new TextArea(messageHelper.getMessage("document.major.version.comment.caption"));
        commentArea.setRows(5);
        commentArea.setWidth("400px");
        windowLayout.addComponent(commentArea);
    }

    private Button buildSaveButton() {
        // create save Button
        Button saveButton = new Button(messageHelper.getMessage("document.major.version.button.save"));
        saveButton.addStyleName("primary");
        saveButton.addClickListener(event -> {
            LOG.debug("Saved as Major Version with Comments - " + commentArea.getValue());
            eventBus.post(new SaveMajorVersionEvent(commentArea.getValue(), true));
            close();
        });
        return saveButton;
    }
}
