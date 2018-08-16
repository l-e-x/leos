/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.addon.onoffswitch.OnOffSwitch;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.*;
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.ui.component.RangeSliderComponent;
import eu.europa.ec.leos.web.event.component.ComparisonRequestEvent;
import eu.europa.ec.leos.web.support.VersionComparator;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

public class TimeLineHeaderComponent<T extends LeosDocument.XmlDocument> extends VerticalLayout {

    private static final long serialVersionUID = -7965253132523132302L;
    private static final Logger LOG = LoggerFactory.getLogger(TimeLineHeaderComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private UserHelper userHelper;
    
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    final static int SINGLE_COLUMN_MODE = 1;
    final static int TWO_COLUMN_MODE = 2;
    private int diffMode;
    
    private GridLayout oldVersionInfo;
    private GridLayout newVersionInfo;
    private RangeSliderComponent rangeSliderComponent;
    
    // data
    private List<T> allDocumentVersions;
    private List<T> filteredVersions;
    private Selection minSelection;
    private Selection maxSelection;
    private boolean showAllIntermediateBeforeLastMajor = false;

    public TimeLineHeaderComponent(final MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper, List<T> docVersions) {
        super();
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;
        this.allDocumentVersions = docVersions;
        init();
    }

    private void init() {
        setStyleName("leos-timeline-header");
        
        minSelection = new Selection();
        maxSelection = new Selection();
        
        setSpacing(true);
        setMargin(false);

        TimeLineSliderContent content = new TimeLineSliderContent();
        maxSelection.addObserver(content);
        minSelection.addObserver(content);
        
        this.filteredVersions = filterVersions(allDocumentVersions);
        initTimeLineHeaderComponent();
    }

    private void initTimeLineHeaderComponent() {
        // initialize TimeLine Header
        addComponent(buildDiffModeSelectionArea());
        
        RangeSliderComponent slider = createSliderComponent();
        addComponent(slider);
        addComponent(buildVersionInfoLayout());
        setComponentAlignment(slider, Alignment.MIDDLE_CENTER);
        setSelections(filteredVersions); //set default version selections
    }
    
    private void setSelections(List<T> versions) {
        // if no version was already selected, set min to last major version.
        if (minSelection.getVersion() == null ||
                !getVersionLabels(versions).contains(minSelection.getVersion().getVersionLabel())) {
            T lastMajorVersion = null;
            for (T version : versions) {
                if (version.isMajorVersion()) {//list is sorted. last to first.
                    lastMajorVersion = version;
                    break;
                }
            }
            //Set Default comparison between last major version and current version. 
            //If the current version is a major version then default comparison will be
            //between current version and previous major version.
            if(lastMajorVersion != null && !versions.get(0).isMajorVersion()) {
                minSelection.setVersion(lastMajorVersion);
            }
            else{
                minSelection.setVersion(versions.size() < 2 ? versions.get(0) : versions.get(1));
            }
        }

        if (maxSelection.getVersion() == null) {
            maxSelection.setVersion(versions.get(0));
        }
    }

    private Set<String> getVersionLabels(List<T> displayedVersions) {
        Set<String> versions = new TreeSet<>(new VersionComparator());
        displayedVersions.forEach(version -> versions.add(version.getVersionLabel()));
        return versions;
    }
    
    private Component buildDiffModeSelectionArea() {
        diffMode = TWO_COLUMN_MODE;//default selection mode
        
        HorizontalLayout diffModeSelectionArea = new HorizontalLayout();
        diffModeSelectionArea.setMargin(new MarginInfo(false, true, false, true));
        diffModeSelectionArea.setSizeFull();
        
        Component captionLabel = buildCaptionLabel();
        diffModeSelectionArea.addComponent(captionLabel);
        diffModeSelectionArea.setExpandRatio(captionLabel, 0.8f);
        diffModeSelectionArea.setComponentAlignment(captionLabel, Alignment.MIDDLE_LEFT);
        
        final CheckBox intermediateVersionCheckBox = buildIntermediateVersionCheckBox();
        intermediateVersionCheckBox.addStyleName("leos-intermediate-version-compare");
        diffModeSelectionArea.addComponent(intermediateVersionCheckBox);
        diffModeSelectionArea.setComponentAlignment(intermediateVersionCheckBox, Alignment.MIDDLE_RIGHT);
        
        final OnOffSwitch onoffSwitch = new OnOffSwitch();
        onoffSwitch.setCaption(messageHelper.getMessage("leos.version.singlecolumn"));
        onoffSwitch.setCaptionAsHtml(true);
        diffModeSelectionArea.addComponent(onoffSwitch);
        diffModeSelectionArea.setComponentAlignment(onoffSwitch, Alignment.MIDDLE_RIGHT);
        
        final Label twoColumnLbl = new Label(messageHelper.getMessage("leos.version.twocolumn"), ContentMode.HTML);
        diffModeSelectionArea.addComponent(twoColumnLbl);
        diffModeSelectionArea.setExpandRatio(twoColumnLbl, 0.2f);
        diffModeSelectionArea.setComponentAlignment(twoColumnLbl, Alignment.MIDDLE_LEFT);
        
        if (diffMode == SINGLE_COLUMN_MODE) {
            onoffSwitch.setValue(false);
        } else {
            onoffSwitch.setValue(true);
        }

        onoffSwitch.addValueChangeListener(event -> {
             if (event.getValue()) { //Two Column
                 diffMode = TWO_COLUMN_MODE;
                 eventBus.post(new ComparisonRequestEvent<>(minSelection.getVersion(), maxSelection.getVersion(), diffMode));
             } else { //Single column
                 diffMode = SINGLE_COLUMN_MODE;
                 eventBus.post(new ComparisonRequestEvent<>(minSelection.getVersion(), maxSelection.getVersion(), diffMode));
             }
        });

        return diffModeSelectionArea;
    }
    
    private CheckBox buildIntermediateVersionCheckBox() {
        String intermediateVersion = messageHelper.getMessage("document.intermediate.versions.checkbox");
        final CheckBox intermediateVersionCheckBox = new CheckBox(intermediateVersion);
        intermediateVersionCheckBox.setCaptionAsHtml(true);
        intermediateVersionCheckBox.addValueChangeListener(event -> {
            showAllIntermediateBeforeLastMajor = event.getValue() ? true : false;
            filteredVersions = filterVersions(allDocumentVersions);
            rangeSliderComponent.setSteps(getVersionLabels(filteredVersions));
            setSelections(filteredVersions);
        });
        return intermediateVersionCheckBox;
    }
    
    private Component buildCaptionLabel() {
        return new Label(messageHelper.getMessage("document.compare.version.caption"), ContentMode.HTML);
    }

    private RangeSliderComponent createSliderComponent() {
        rangeSliderComponent = new RangeSliderComponent();
        rangeSliderComponent.setWidth(90, Unit.PERCENTAGE);
        rangeSliderComponent.setColouredArea(RangeSliderComponent.ColouredArea.ENCLOSED);
        rangeSliderComponent.addHandlesListener(event -> {
                LOG.debug("Slider handles values changed!");
                String[] updatedHandles = event.getComponent().getHandles();
                
                minSelection.setVersion(getDocumentVersion(updatedHandles[0]));
                maxSelection.setVersion(getDocumentVersion(updatedHandles[1]));
            });
        rangeSliderComponent.setSteps(getVersionLabels(filteredVersions));
        return rangeSliderComponent;
    }
    
    private Component buildVersionInfoLayout() {
        HorizontalLayout versionInfoLayout = new HorizontalLayout();
        versionInfoLayout.setSizeFull();
        versionInfoLayout.setSpacing(false);
        versionInfoLayout.setMargin(new MarginInfo(true, false, false, false)); //top margin
        
        oldVersionInfo = createVersionInfo();
        versionInfoLayout.addComponent(oldVersionInfo);
        versionInfoLayout.setComponentAlignment(oldVersionInfo, Alignment.MIDDLE_CENTER);

        newVersionInfo = createVersionInfo();
        versionInfoLayout.addComponent(newVersionInfo);
        versionInfoLayout.setComponentAlignment(newVersionInfo, Alignment.MIDDLE_CENTER);
        return versionInfoLayout;
    }
    
    private GridLayout createVersionInfo() {
        GridLayout gridLayout = new GridLayout(2,2);
        
        gridLayout.setSpacing(false);
        gridLayout.setMargin(new MarginInfo(false, true, false, true));
        gridLayout.setStyleName("leos-version-info-layout");
        
        // add version
        Label versionLabel = new Label("", ContentMode.HTML);
        versionLabel.setStyleName("leos-version-info-label");
        gridLayout.addComponent(versionLabel, 0, 0);
        
        // add date
        Label dateLabel = new Label("", ContentMode.HTML);
        dateLabel.setStyleName("leos-version-info-label");
        gridLayout.addComponent(dateLabel, 0, 1);
        
        // add action
        Label actionLabel = new Label("", ContentMode.HTML);
        gridLayout.addComponent(actionLabel, 1, 0);
        
        // add user
        Label userLabel = new Label("", ContentMode.HTML);
        gridLayout.addComponent(userLabel, 1, 1);
        
        return gridLayout;
    }
    
    private void updateVersionInfo(GridLayout gridLayout, T componentCompareItem) {
        Label versionLabel = (Label)gridLayout.getComponent(0, 0);
        versionLabel.setValue(messageHelper.getMessage("document.versions.caption.version", componentCompareItem.getVersionLabel(), getVersionType(componentCompareItem)));
        
        Label dateLabel = (Label)gridLayout.getComponent(0, 1);
        dateLabel.setValue(messageHelper.getMessage("document.versions.caption.date", dateFormatter.format(Date.from(componentCompareItem.getLastModificationInstant()))));

        Label actionLabel = (Label)gridLayout.getComponent(1, 0);
        String action = componentCompareItem.getVersionComment() != null
                ? messageHelper.getMessage((String) componentCompareItem.getVersionComment()) : messageHelper.getMessage("popup.label.revision.nocomment");
        actionLabel.setValue(messageHelper.getMessage("document.versions.caption.action", action));
        
        Label userLabel = (Label)gridLayout.getComponent(1, 1);
        userLabel.setValue(messageHelper.getMessage("document.versions.caption.user",
                new UserLoginDisplayConverter(userHelper).convertToPresentation(componentCompareItem.getLastModifiedBy(), null, null)));
    }

    private String getVersionType(T document) {
        return document.isMajorVersion() ? messageHelper.getMessage("document.versions.caption.versionMajor")
                : messageHelper.getMessage("document.versions.caption.versionMinor");
    }

    private List<T> filterVersions(List<T> documentVersions) {
        List<T> filteredVersions = new ArrayList<>();

        if (!showAllIntermediateBeforeLastMajor) {
            boolean versionAfterLastMajorVersion = true;
            /*INFO: All versions of the version series, sorted by {@code cmis:creationDate}
              descending and preceded by the PWC, if one exists, not {@code null}*/
            for(T version : documentVersions) {
                if (version.isMajorVersion()) {
                    versionAfterLastMajorVersion = false;
                    filteredVersions.add(version);
                }

                if (versionAfterLastMajorVersion) {
                    filteredVersions.add(version);
                }
            }
        } else {
            filteredVersions.addAll(documentVersions);
        }
        return filteredVersions;
    }

    private T getDocumentVersion(String documentLabel) {
        for (T version : filteredVersions) {
            if (documentLabel != null && documentLabel.equals(version.getVersionLabel())) {
                return version;
            }
        }
        return null;
    }
    
    private void selectionChanged() {
        rangeSliderComponent.setHandles(new String[]{minSelection.getVersion().getVersionLabel(), maxSelection.getVersion().getVersionLabel()});
        eventBus.post(new ComparisonRequestEvent<>(minSelection.getVersion(), maxSelection.getVersion(), diffMode));
    }

    private class TimeLineSliderContent implements Observer {

        @Override
        public void update(Observable o, Object arg) {
            if (arg != null && o.equals(maxSelection)) {
                updateVersionInfo(newVersionInfo, maxSelection.getVersion());
            } else if (arg != null && o.equals(minSelection)) {
                updateVersionInfo(oldVersionInfo, minSelection.getVersion());
            }
            
            if (minSelection.getVersion() != null && maxSelection.getVersion() != null) {
                selectionChanged();
            }
        }
    }

    private class Selection extends Observable {
        private T version;

        public T getVersion() {
            return version;
        }

        public void setVersion(T version) {
            if (this.version == null || !this.version.equals(version)) {
                this.version = version;
                setChanged();
                notifyObservers(version);
            }
        }
    }
}
