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
import com.vaadin.ui.Button;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.support.comparators.VersionComparator;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.document.CompareVersionRequestEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.VersionListComponent;

import java.util.List;


public class VersionsListWindow extends AbstractWindow {

    private VersionListComponent versionListComponent ;
    List<LeosDocumentProperties> documentVersions;

    public VersionsListWindow(MessageHelper messageHelper, EventBus eventBus, List<LeosDocumentProperties> documentVersions) {
        super(messageHelper, eventBus);
        this.documentVersions = documentVersions;
        setCaption(messageHelper.getMessage("document.versions.window.title"));
        prepareWindow(documentVersions);
    }

    public void prepareWindow(List<LeosDocumentProperties> documentVersions) {
        setWidth("650px");
        setHeight("400px");

        Button compareButton = buildCompareButton();
        addButton(compareButton);

        versionListComponent = new VersionListComponent(messageHelper, eventBus,documentVersions);
        setBodyComponent(versionListComponent);
    }

    private void postCompareVersionsEvent() {
        LeosDocumentProperties selectedVersion=versionListComponent.getSelectedVersion();
        LeosDocumentProperties otherVersion=versionListComponent.getOtherVersion();

        if (selectedVersion != null && otherVersion != null) {
            //do the comparison

            LeosDocumentProperties newerRevision;
            LeosDocumentProperties olderRevision;

            if (new VersionComparator().compare(selectedVersion.getVersionLabel(), otherVersion.getVersionLabel())>0)  {
                newerRevision = selectedVersion;
                olderRevision = otherVersion;
            } else {
                newerRevision = otherVersion;
                olderRevision = selectedVersion;
            }

            //if this goes wrong things will go wrong in comparator
            LeosDocumentProperties olderItem = (olderRevision);
            LeosDocumentProperties newerItem = (newerRevision);

            eventBus.post(new CompareVersionRequestEvent(olderItem, newerItem));

        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.WARNING, "document.versions.selectError"));
        }
    }


    private Button buildCompareButton() {
        // create compare Button
        Button compareButton = new Button(messageHelper.getMessage("document.versions.button.compare"));
        compareButton.addStyleName("primary");
        compareButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                if (isEnabled()) {
                    postCompareVersionsEvent();
                }
            }
        });
        return compareButton;
    }
}
