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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import com.google.common.eventbus.EventBus;
import com.vaadin.data.Property;
import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;


public class VersionListComponent extends CustomComponent {

    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private LeosDocumentProperties selectedVersion;
    private LeosDocumentProperties otherVersion;
    List<LeosDocumentProperties> documentAllVersions;
    private MessageHelper messageHelper;
    private EventBus eventBus;
    //private Button compareButton;

    public VersionListComponent(MessageHelper messageHelper, EventBus eventBus, 
            List<LeosDocumentProperties> documentVersions) {
        this.messageHelper= messageHelper;
        this.eventBus=eventBus;
        this.documentAllVersions=documentVersions;

        initLayout();
    }

    private void initLayout(){
        VerticalLayout layout = new VerticalLayout();
        setCompositionRoot(layout);
        setSizeFull();
        layout.setSizeFull();

        Table table = buildDocumentTable();
        layout.addComponent(table);
        layout.setExpandRatio(table, 1f);

        table.removeAllItems();

        for (LeosDocumentProperties version : documentAllVersions) {
            table.addItem(new Object[]{version.getVersionLabel(),
                    version.getCreatedBy(),
                    dateFormatter.format(version.getCreatedOn()),
                    version.getVersionComment()!=null? messageHelper.getMessage(version.getVersionComment()):""
            }, version.getVersionId());
        }
        layout.setMargin(true);
    }

    private Table buildDocumentTable() {

        final Table versionTable = new Table(messageHelper.getMessage("document.versions.table.caption"));

        versionTable.addContainerProperty("versionLabel", String.class, null, messageHelper.getMessage("document.versions.table.versionLabel"), null, null);
        versionTable.addContainerProperty("modifiedBy"  , String.class, null, messageHelper.getMessage("document.versions.table.modifiedBy"), null, null);
        versionTable.addContainerProperty("modifiedOn"  , String.class, null, messageHelper.getMessage("document.versions.table.modifiedOn"), null, null);
        versionTable.addContainerProperty("comments"    , String.class, null, messageHelper.getMessage("document.versions.table.comments"), null, null);

        versionTable.setColumnExpandRatio("versionLabel", 1.0f);
        versionTable.setColumnExpandRatio("modifiedBy", 1.0f);
        versionTable.setColumnExpandRatio("modifiedOn", 1.0f);
        versionTable.setColumnExpandRatio("comments", 2.0f);

        versionTable.setMultiSelect(true);
        versionTable.setMultiSelectMode(MultiSelectMode.SIMPLE);

        versionTable.setSelectable(true);
        versionTable.setImmediate(true);
        versionTable.setNullSelectionAllowed(false);

        versionTable.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                Set<String> selectedValue = (Set) event.getProperty().getValue();
                List<String> orderedItems = new ArrayList<>(selectedValue);

                //unselect older selections
                while (orderedItems.size() > 2) {
                    orderedItems.remove(0);
                }
                versionTable.setValue(orderedItems);
                if (orderedItems.size() == 2) {
                    selectedVersion = getDocumentVersion(orderedItems.get(0));
                    otherVersion = getDocumentVersion(orderedItems.get(1));
                } else {
                    selectedVersion = null;
                    otherVersion = null;
                }
            }
        });
        // use all available space
        versionTable.setSizeFull();
        return versionTable;
    }

    public LeosDocumentProperties getSelectedVersion() {
        return selectedVersion;
    }

    public LeosDocumentProperties getOtherVersion() {
        return otherVersion;
    }
    
    // here revisionId is the obj id from repository and Not LEOS id
    private LeosDocumentProperties getDocumentVersion(String revisionId){

        Validate.notNull(revisionId);
        for (LeosDocumentProperties version : documentAllVersions) {
            if(version.getVersionId().equalsIgnoreCase(revisionId)){
                return version;
            }
        }
        return null;
    }


}
