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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.addon.onoffswitch.OnOffSwitch;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.CheckinCommentVO;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.document.util.CheckinCommentUtil;
import eu.europa.ec.leos.services.support.VersionsUtil;
import eu.europa.ec.leos.ui.component.RangeSliderComponent;
import eu.europa.ec.leos.ui.component.RangeSliderStepVO;
import eu.europa.ec.leos.ui.view.ComparisonDisplayMode;
import eu.europa.ec.leos.web.event.component.CompareTimeLineRequestEvent;
import eu.europa.ec.leos.web.event.component.RestoreVersionRequestEvent;
import eu.europa.ec.leos.web.support.StepValueComparator;
import eu.europa.ec.leos.web.support.VersionComparator;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vaadin.dialogs.ConfirmDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

public class TimeLineHeaderComponent<T extends XmlDocument> extends VerticalLayout {

    private static final long serialVersionUID = -7965253132523132302L;
    private static final Logger LOG = LoggerFactory.getLogger(TimeLineHeaderComponent.class);

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private UserHelper userHelper;
    private SecurityContext securityContext;

    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private ComparisonDisplayMode diffMode;

    private GridLayout oldVersionInfo;
    private GridLayout newVersionInfo;
    private RangeSliderComponent rangeSliderComponent;
    private CheckBox intermediateVersionCheckBox;

    // data
    private List<T> allDocumentVersions;
    private List<T> filteredVersions;
    private Selection minSelection;
    private Selection maxSelection;
    private boolean showAllIntermediateBeforeLastMajor = false;
    private HashMap<Button, Registration> restoreVersionRegisteredListeners = new HashMap<Button, Registration>();

    public TimeLineHeaderComponent(SecurityContext securityContext, final MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper, List<T> docVersions) {
        super();
        Validate.notNull(docVersions);
        Validate.notEmpty(docVersions);
        this.securityContext = securityContext;
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
        
        intermediateVersionCheckBox.setValue(true);
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

    /**
     * Set Default comparison between last major version and current version, OR the last minor version created by the same user and current version.
     * If the current version is a major version then default comparison will be between current version and previous major version.
     * @param versions List with all versions in descending order (last to first). First element of the List should be the actual version
     */
    private void setSelections(List<T> versions) {
        final T maxVersion = versions.get(0); //actual version
        if (maxSelection.getVersion() == null) {
            maxSelection.setVersion(maxVersion);
        }

        if (minSelection.getVersion() == null || !getVersionLabels(versions).contains(minSelection.getVersion().getVersionLabel())) {
            minSelection.setVersion(getMinVersion(versions, maxVersion));
        }
    }

    private T getMinVersion(List<T> versions, T maxVersion) {
        final T minVersion;
        T lastMajorOrIntermediateVersion = getLastMajorOrIntermediateVersion(versions);
        T lastMinorVersionCreatedBySameUser = getLastMinorVersionCreatedBySameUser(versions);

        if(lastMajorOrIntermediateVersion == null || maxVersion.getVersionType().equals(VersionType.MAJOR) || maxVersion.getVersionType().equals(VersionType.INTERMEDIATE)) {
            // If no major version present, or the actual version is already a major version, take the previous one.
            minVersion = getPreviousVersion(versions);
        } else {
            if (lastMinorVersionCreatedBySameUser == null) {
                // compare with last major
                if(lastMajorOrIntermediateVersion == null || maxVersion.getVersionType().equals(VersionType.MAJOR) || maxVersion.getVersionType().equals(VersionType.INTERMEDIATE)) {
                    // if the actual version is a major, compare with the previous one
                    minVersion = getPreviousVersion(versions);
                } else {
                    minVersion = lastMajorOrIntermediateVersion;
                }
            } else {
                // compare with last minor
                minVersion = compareVersions(lastMinorVersionCreatedBySameUser.getVersionLabel(), lastMajorOrIntermediateVersion.getVersionLabel()) > 0
                        ? lastMinorVersionCreatedBySameUser : lastMajorOrIntermediateVersion;
            }
        }
        return minVersion;
    }

    private int compareVersions(String firstVersion, String secondVersion) {
        String[] firstVersionArray = firstVersion.split("\\.");
        String[] secondVersionArray = secondVersion.split("\\.");

        for (int i = 0; i < firstVersionArray.length; i++) {
            int firstVersionInt = Integer.parseInt(firstVersionArray[i]);
            int secondVersionInt = Integer.parseInt(secondVersionArray[i]);
            if (firstVersionInt < secondVersionInt) {
                return -1;
            } else if (firstVersionInt > secondVersionInt) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * @param versions List with all versions in descending order. First element of the List should be the actual version     *
     * @return First last minor version created by the logged user after the last major.
     */
    private T getLastMinorVersionCreatedBySameUser(List<T> versions) {
        T lastMinor = null;
        for (T version : versions) {
            if (version.getLastModifiedBy().contains(securityContext.getUser().getLogin()) && version.getVersionType().equals(VersionType.MINOR)) {
                lastMinor = version;
                break;
            }
        }
        return lastMinor;
    }
    
    private T getLastMajorOrIntermediateVersion(List<T> versions) {
        T lastMajorOrIntermediateVersion = null;
        for (T version : versions) {
            if (version.getVersionType().equals(VersionType.MAJOR) || version.getVersionType().equals(VersionType.INTERMEDIATE)) {
                lastMajorOrIntermediateVersion = version;
                break;
            }
        }
        return lastMajorOrIntermediateVersion;
    }

    /**
     * @param versions List with all versions in descending order. First element of the List should be the actual version
     * @return Previous version (second element of the list) or itself (first element) if is the only present version.
     */
    private T getPreviousVersion(List<T> versions) {
        return (versions.size() == 1) ? versions.get(0) : versions.get(1);
    }

    private Set<RangeSliderStepVO> getVersionSteps(List<T> displayedVersions) {
        Set<RangeSliderStepVO> versions = new TreeSet<>(new StepValueComparator());
        displayedVersions.forEach(version ->  {
            if(version.getMilestoneComments().size() > 0) {
                final StringBuffer commentsBuffer = new StringBuffer(" - ");
                final String mileStoneComments;
                if (version.getMilestoneComments().size() == 1){
                    commentsBuffer.append(version.getMilestoneComments().get(0));
                    mileStoneComments = commentsBuffer.toString();
                } else {
                    for (String comment : version.getMilestoneComments()) {
                        commentsBuffer.append(comment).append("\n - ");
                    }
                    mileStoneComments = commentsBuffer.delete(commentsBuffer.lastIndexOf("\n - "), commentsBuffer.length()).toString();
                }

                versions.add(new RangeSliderStepVO(version.getVersionLabel(), mileStoneComments, true));
            } else {
                versions.add(new RangeSliderStepVO(version.getVersionLabel(), "", false));
            }
        });

        return versions;
    }

    private Set<String> getVersionLabels(List<T> displayedVersions) {
        Set<String> versionLabels = new TreeSet<>(new VersionComparator());
        displayedVersions.forEach(version ->  {
            versionLabels.add(version.getVersionLabel());
        });
        return versionLabels;
    }

    private Component buildDiffModeSelectionArea() {
        diffMode = ComparisonDisplayMode.TWO_COLUMN_MODE;//default selection mode

        HorizontalLayout diffModeSelectionArea = new HorizontalLayout();
        diffModeSelectionArea.setMargin(new MarginInfo(false, true, false, true));
        diffModeSelectionArea.setSizeFull();

        Component captionLabel = buildCaptionLabel();
        diffModeSelectionArea.addComponent(captionLabel);
        diffModeSelectionArea.setExpandRatio(captionLabel, 0.8f);
        diffModeSelectionArea.setComponentAlignment(captionLabel, Alignment.MIDDLE_LEFT);

        intermediateVersionCheckBox = buildIntermediateVersionCheckBox();
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

        if (diffMode == ComparisonDisplayMode.SINGLE_COLUMN_MODE) {
            onoffSwitch.setValue(false);
        } else {
            onoffSwitch.setValue(true);
        }

        onoffSwitch.addValueChangeListener(event -> {
             if (event.getValue()) { //Two Column
                 diffMode = ComparisonDisplayMode.TWO_COLUMN_MODE;
                 eventBus.post(new CompareTimeLineRequestEvent(minSelection.getVersion().getId(), maxSelection.getVersion().getId(), diffMode));
             } else { //Single column
                 diffMode = ComparisonDisplayMode.SINGLE_COLUMN_MODE;
                 eventBus.post(new CompareTimeLineRequestEvent(minSelection.getVersion().getId(), maxSelection.getVersion().getId(), diffMode));
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
            rangeSliderComponent.setSteps(getVersionSteps(filteredVersions));
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
        rangeSliderComponent.setSteps(getVersionSteps(filteredVersions));
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

        HorizontalLayout versionInfoLayout = new HorizontalLayout();
        versionInfoLayout.setSizeFull();
        versionInfoLayout.setSpacing(false);
        versionInfoLayout.setMargin(new MarginInfo(false, false, false, false)); //top margin

        // add version
        Label versionLabel = new Label("", ContentMode.HTML);
        versionInfoLayout.addStyleName("leos-version-info-column");
        versionInfoLayout.addComponent(versionLabel);
        versionInfoLayout.setComponentAlignment(versionLabel, Alignment.TOP_LEFT);

        if (hasRestoreVersionPermission()) {
            // add restore version button
            Button versionRestoreButton = new Button();
            versionRestoreButton.setIcon(VaadinIcons.TIME_BACKWARD);
            versionRestoreButton.addStyleName("link");
            versionRestoreButton.addStyleName("leos-version-restore-button");
            versionRestoreButton.setDescription(messageHelper.getMessage("document.restore.version.button"));
            versionInfoLayout.addComponent(versionRestoreButton);
            versionInfoLayout.setComponentAlignment(versionRestoreButton, Alignment.TOP_RIGHT);
            versionLabel.addStyleName("leos-version-info-label");
            versionLabel.setWidthUndefined();
            versionInfoLayout.setWidthUndefined();
        }

        gridLayout.addComponent(versionInfoLayout, 0, 0);
        // add date
        Label dateLabel = new Label("", ContentMode.HTML);
        dateLabel.addStyleName("leos-version-info-column");
        gridLayout.addComponent(dateLabel, 0, 1);

        // add action
        Label actionLabel = new Label("", ContentMode.HTML);
        gridLayout.addComponent(actionLabel, 1, 0);

        // add user
        Label userLabel = new Label("", ContentMode.HTML);
        gridLayout.addComponent(userLabel, 1, 1);

        return gridLayout;
    }

    public void updateVersions(List<T> docVersions) {
        this.allDocumentVersions = docVersions;
        this.filteredVersions = filterVersions(allDocumentVersions);
        rangeSliderComponent.setSteps(getVersionSteps(filteredVersions));
        maxSelection.setVersion(allDocumentVersions.get(0));
        selectionChanged();
    }

    private void restoreVersion(T version) {
        // ask confirmation before restore
        ConfirmDialog confirmDialog = ConfirmDialog.getFactory().create(
                messageHelper.getMessage("document.restore.version.window.title"),
                messageHelper.getMessage("document.restore.version.comment.caption", version.getVersionLabel()),
                messageHelper.getMessage("document.restore.version.button.revert"),
                messageHelper.getMessage("document.restore.version.button.cancel"), null);
        confirmDialog.setContentMode(ConfirmDialog.ContentMode.HTML);

        confirmDialog.show(getUI(),
                new ConfirmDialog.Listener() {
                    private static final long serialVersionUID = 144198814274639L;

                    public void onClose(ConfirmDialog dialog) {
                        if (dialog.isConfirmed()) {
                            eventBus.post(new RestoreVersionRequestEvent(version.getId()));
                        }
                    }
                }, true);
    }

    private void updateVersionInfo(GridLayout gridLayout, T componentCompareItem) {
        HorizontalLayout versionInfoLayout = (HorizontalLayout)gridLayout.getComponent(0, 0);
        Label versionLabel = (Label)versionInfoLayout.getComponent(0);
        versionLabel.setValue(messageHelper.getMessage("document.versions.caption.version", componentCompareItem.getVersionLabel(), getVersionType(componentCompareItem)));

        Label dateLabel = (Label)gridLayout.getComponent(0, 1);
        dateLabel.setValue(messageHelper.getMessage("document.versions.caption.date", dateFormatter.format(Date.from(componentCompareItem.getLastModificationInstant()))));

        if (hasRestoreVersionPermission()) {
            Button versionRestoreButton = (Button)versionInfoLayout.getComponent(1);
            if (restoreVersionRegisteredListeners.containsKey(versionRestoreButton)) {
                Registration oldClickRegistration = restoreVersionRegisteredListeners.get(versionRestoreButton);
                oldClickRegistration.remove();
            }
            restoreVersionRegisteredListeners.remove(versionRestoreButton);
            Registration restoreVersionClickListener = versionRestoreButton.addClickListener(event -> restoreVersion(componentCompareItem));
            restoreVersionRegisteredListeners.put(versionRestoreButton, restoreVersionClickListener);

            versionRestoreButton.setVisible(!componentCompareItem.isLatestVersion());
        }

        Label actionLabel = (Label)gridLayout.getComponent(1, 0);
        final String checkinCommentJson = componentCompareItem.getVersionComment() != null
                ? messageHelper.getMessage((String) componentCompareItem.getVersionComment()) : messageHelper.getMessage("popup.label.revision.nocomment");
        final CheckinCommentVO checkinComment = CheckinCommentUtil.getJavaObjectFromJson(checkinCommentJson);
        final String labelComment = VersionsUtil.buildLabel(checkinComment, messageHelper);
        final String label = messageHelper.getMessage("document.versions.caption.action", StringEscapeUtils.escapeHtml4(labelComment));
        actionLabel.setValue(label);

        Label userLabel = (Label)gridLayout.getComponent(1, 1);
        userLabel.setValue(messageHelper.getMessage("document.versions.caption.user",
                userHelper.convertToPresentation(componentCompareItem.getLastModifiedBy())));
    }
    
    private boolean hasRestoreVersionPermission() {
        if (allDocumentVersions != null && allDocumentVersions.size() > 0) {
            return securityContext.hasPermission(allDocumentVersions.get(0), LeosPermission.CAN_RESTORE_PREVIOUS_VERSION);
        }

        return false;
    }


    private String getVersionType(T document) {
        return messageHelper.getMessage("document.versions.caption.version." + document.getVersionType().value());
    }

    private List<T> filterVersions(List<T> documentVersions) {
        List<T> filteredVersions = new ArrayList<>();

        if (!showAllIntermediateBeforeLastMajor) {
            boolean versionAfterLastMajorVersion = true;
            /*INFO: All versions of the version series, sorted by {@code cmis:creationDate}
              descending and preceded by the PWC, if one exists, not {@code null}*/
            for(T version : documentVersions) {
                if (version.getVersionType().equals(VersionType.MAJOR) || version.getVersionType().equals(VersionType.INTERMEDIATE)) {
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
        eventBus.post(new CompareTimeLineRequestEvent(minSelection.getVersion().getId(), maxSelection.getVersion().getId(), diffMode));
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
