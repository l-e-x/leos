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
import com.vaadin.server.FileDownloader;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.xml.DownloadStreamResource;

import java.io.InputStream;

public class DownloadWindow extends AbstractWindow {
    private static final long serialVersionUID = 3378131768048329809L;

    private MessageHelper messageHelper;

    public DownloadWindow(MessageHelper messageHelper, EventBus eventBus, String docName, InputStream docContentStream, String msgKey) {
        super(messageHelper, eventBus);

        this.messageHelper = messageHelper;

        setWidth(300, Unit.PIXELS);
        setHeight(200, Unit.PIXELS);
        setCaption(messageHelper.getMessage("window.download.title"));

        Button downloadButton = buildDownloadButton(docName, docContentStream);
        addButton(downloadButton);

        Component windowBody = buildWindowBody(msgKey, docName);
        setBodyComponent(windowBody);
    }

    private Button buildDownloadButton(String docName, InputStream docContent) {
        final Button downloadButton = new Button(messageHelper.getMessage("window.download.button.caption"));
        downloadButton.addStyleName("primary");
        DownloadStreamResource downloadStreamResource = new DownloadStreamResource(docName, docContent);
        FileDownloader fileDownloader = new FileDownloader(downloadStreamResource);
        fileDownloader.extend(downloadButton);

        downloadButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                downloadButton.setEnabled(false);
            }
        });

        return downloadButton;
    }

    private Component buildWindowBody(String msgKey, String docName) {
        VerticalLayout layout = new VerticalLayout();

        Label downloadMsg = new Label(messageHelper.getMessage(msgKey, docName), ContentMode.HTML);
        layout.addComponent(downloadMsg);
        layout.setSizeFull();
        layout.setMargin(true);

        return layout;
    }

}
