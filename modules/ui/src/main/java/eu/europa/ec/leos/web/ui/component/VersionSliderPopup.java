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
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.Property;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.support.comparators.VersionComparator;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.event.view.document.MarkedContentRequestEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class VersionSliderPopup extends PopupView {
    private static final long serialVersionUID = -5435432434L;
    private static final Logger LOG = LoggerFactory.getLogger(VersionSliderPopup.class);
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private MessageHelper messageHelper;
    private EventBus eventBus;

    // components that contain display data
    private String minimizedLabel;
    private VerticalLayout popupMaximizedLayout = new VerticalLayout();
    private VerticalLayout oldSelectedDetails = new VerticalLayout();
    private VerticalLayout newSelectedDetails = new VerticalLayout();
    private CheckBox alwaysLatest;
    private RangeSliderComponent rangeSliderComponent;

    // data
    private List<LeosDocumentProperties> documentVersions;
    private Selection min = new Selection();
    private Selection max = new Selection();

    // init is a stateless function which should not be tied to item/card data
    protected VersionSliderPopup(MessageHelper messageHelper, final EventBus eventBus) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;

        init();
    }

    @Override public void attach() {
        super.attach();
        eventBus.register(this);
        max.setVersion(null);
        min.setVersion(null);
        eventBus.post(new VersionListRequestEvent());
    }

    @Override public void detach() {
        super.detach();
        eventBus.unregister(this);
    }

    private void init() {
        setHeight(100, Unit.PERCENTAGE);
        setHideOnMouseOut(false);
        setStyleName("leos-version-slider-popup");

        initMaximizedComponent();

        VersionSliderPopupContent content = new VersionSliderPopupContent();
        max.addObserver(content);//popup component needs to react to changes in selections
        min.addObserver(content);

        setContent(content);
    }

    @Subscribe
    public void setDocumentVersions(VersionListResponseEvent event) {
        // when data is received , update Slider
        documentVersions = event.getDocumentVersions();
        setEnabled(documentVersions.size() > 1 ? true : false);
        updateSliderSteps();
        setDefaultVersions();
        setMaxToLatest();
    }

    @Subscribe
    public void requestVersionList(DocumentUpdatedEvent event){
        eventBus.post(new VersionListRequestEvent());
    }

    private void updateSliderSteps() {
        rangeSliderComponent.setSteps(getVersionLabels(documentVersions));
    }

    private void setDefaultVersions() {
        if (min.getVersion() == null) {
            min.setVersion(documentVersions.size() < 2 ? documentVersions.get(0) : documentVersions.get(1));
        }

        if (max.getVersion() == null ) {
            max.setVersion(documentVersions.get(0));
        }
    }

    private void setMaxToLatest(){
        if(alwaysLatest.getValue()== true) {
            max.setVersion(documentVersions.get(0));
        }
    }

    private void initMaximizedComponent() {
        // initialize Maximized layout
        popupMaximizedLayout.setStyleName("leos-version-slider-popup-max");
        popupMaximizedLayout.removeAllComponents();
        popupMaximizedLayout.setSpacing(true);
        popupMaximizedLayout.setMargin(true);
        popupMaximizedLayout.setWidth(500.0f, Unit.PIXELS);

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setWidth(100, Unit.PERCENTAGE);

        Label popupCaption = new Label(messageHelper.getMessage("document.versions.table.caption"));
        hLayout.addComponent(popupCaption);

        alwaysLatest = createCheckBox();
        hLayout.addComponent(alwaysLatest);
        hLayout.setComponentAlignment(alwaysLatest, Alignment.TOP_RIGHT);

        popupMaximizedLayout.addComponent(hLayout);

        rangeSliderComponent = createSliderComponent();
        popupMaximizedLayout.addComponent(rangeSliderComponent);
        popupMaximizedLayout.setComponentAlignment(rangeSliderComponent, Alignment.MIDDLE_CENTER);
        popupMaximizedLayout.setExpandRatio(rangeSliderComponent, .5f);

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setWidth(100, Unit.PERCENTAGE);
        horizontalLayout.addComponent(oldSelectedDetails);
        horizontalLayout.addComponent(newSelectedDetails);
        popupMaximizedLayout.addComponent(horizontalLayout);
    }

    private CheckBox createCheckBox() {
        CheckBox cBox = new CheckBox(messageHelper.getMessage("popup.versions.checkbox.latest"), true);
        cBox.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(Property.ValueChangeEvent event) {
                    setMaxToLatest();
            }
        });
        return cBox;
    }

    private RangeSliderComponent createSliderComponent() {
        rangeSliderComponent = new RangeSliderComponent();
        rangeSliderComponent.setWidth(90, Unit.PERCENTAGE);
        rangeSliderComponent.setStyleName("leos-version-slider");
        rangeSliderComponent.addHandlesListener(new RangeSliderComponent.HandlesListener() {
            @Override
            public void valuesChanged(RangeSliderComponent.HandlesEvent event) {
                LOG.debug("Slider handles values changed!");
                String[] updatedHandles = event.getComponent().getHandles();

                if(!updatedHandles[1].equals(max.getVersion().getVersionLabel())) {
                    alwaysLatest.setValue(false);
                }

                min.setVersion(getVersionProperties(updatedHandles[0]));
                max.setVersion(getVersionProperties(updatedHandles[1]));
            }
        });
        return rangeSliderComponent;
    }

    private VerticalLayout populateItemDetails(VerticalLayout verticalLayout, LeosDocumentProperties componentItem) {
        // add the first line
        verticalLayout.removeAllComponents();
        verticalLayout.setWidth("100%");
        if (componentItem != null) {
            verticalLayout.addComponent(new Label(messageHelper.getMessage("popup.label.revision.details",
                    componentItem.getVersionLabel(),
                    componentItem.getUpdatedBy(),
                    dateFormatter.format(componentItem.getUpdatedOn())), ContentMode.HTML));
        }
        return verticalLayout;
    }

    private Set<String> getVersionLabels(List<LeosDocumentProperties> documentVersions) {
        Set<String> versions = new TreeSet<>(new VersionComparator());

        for (LeosDocumentProperties version : documentVersions) {
            versions.add(version.getVersionLabel());
        }
        return versions;
    }

    private LeosDocumentProperties getVersionProperties(String documentLabel) {
        for (LeosDocumentProperties version : documentVersions) {
            if (documentLabel != null && documentLabel.equals(version.getVersionLabel())) {
                return version;
            }
        }
        return null;
    }

    private void updateMinimizedRepresentation() {
        minimizedLabel = (documentVersions.size()>1)
                    ? messageHelper.getMessage("popup.compare.caption", min.getVersion().getVersionLabel(), max.getVersion().getVersionLabel())
                    : messageHelper.getMessage("popup.compare.caption.disabled");
        markAsDirty();// Marking Dirty to update popup label
    }

    private void selectionChanged(){
        updateMinimizedRepresentation();
        rangeSliderComponent.setHandles(new String[]{min.getVersion().getVersionLabel(), max.getVersion().getVersionLabel()});
        eventBus.post(new MarkedContentRequestEvent(min.getVersion(), max.getVersion(), 0));
    }

    private class VersionSliderPopupContent implements PopupView.Content, Observer {

        public String getMinimizedValueAsHTML() {
            return minimizedLabel;
        }

        public Component getPopupComponent() {
            return popupMaximizedLayout;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (arg != null && o.equals(max)) {
                populateItemDetails(newSelectedDetails, max.getVersion());
            } else if (arg != null && o.equals(min)) {
                populateItemDetails(oldSelectedDetails, min.getVersion());
            }

            if (min.getVersion() != null && max.getVersion() != null) {
                selectionChanged();
            }
        }
    }

    private class Selection extends Observable {
        private LeosDocumentProperties version;

        public LeosDocumentProperties getVersion() {
            return version;
        }

        public void setVersion(LeosDocumentProperties version) {
            if (this.version == null || !this.version.equals(version)) {
                this.version = version;
                setChanged();
                notifyObservers(version);
            }
        }
    }
}
