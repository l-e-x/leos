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
package eu.europa.ec.leos.ui.component.markedText;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.ui.component.RangeSliderStepVO;
import eu.europa.ec.leos.web.event.component.MarkedContentRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.support.StepValueComparator;
import eu.europa.ec.leos.web.support.VersionComparator;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.VersionSliderPopupImpl;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;

import java.util.*;

public class MarkedTextVersionSelector<T extends XmlDocument> extends VersionSliderPopupImpl<T> {
    private static final long serialVersionUID = -5435432434L;

    protected MarkedTextVersionSelector(MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper) {
        super(messageHelper, eventBus, userHelper);

        VersionSliderPopupContent content = new VersionSliderPopupContent();
        maxSelection.addObserver(content);// popup component needs to react to changes in selections
        minSelection.addObserver(content);
        setContent(content);
    }

    @Subscribe
    public void requestVersionList(DocumentUpdatedEvent event) {
        eventBus.post(new VersionListRequestEvent<T>());
    }

    @Subscribe
    public void setDocumentVersions(VersionListResponseEvent<T> event) {
        // when data is received , update Slider
        List<T> allDocumentVersions = event.getDocumentVersions();
        filteredVersions = applyFilter(allDocumentVersions);
        setEnabled(filteredVersions.size() > 1 ? true : false);
        setSelections(filteredVersions);
        versionHistory.setHistory(filteredVersions);
    }

    private List<T> applyFilter(List<T> allDocumentVersions) {
        List<T> filteredVersions = new ArrayList<>();
        boolean versionAfterLastMajorVersion = true;
        /*INFO: All versions of the version series, sorted by {@code cmis:creationDate}
          descending and preceded by the PWC, if one exists, not {@code null}*/
        for (T version : allDocumentVersions) {
            if (version.isMajorVersion()) {
                versionAfterLastMajorVersion = false;
                filteredVersions.add(version);
            } 
            if (versionAfterLastMajorVersion) {
                filteredVersions.add(version);
            }
        }

        return filteredVersions;
    }

    private void setSelections(List<T> versions) {
        // if no version was already selected, only then modify
        if (minSelection.getVersion() == null ||
                !getVersionLabels(versions).contains(minSelection.getVersion().getVersionLabel())) {
            minSelection.setVersion(versions.get(0));// set to latest version
        }

        maxSelection.setVersion(versions.get(0));
    }

    @Override
    public boolean isDisableInitialVersion() {
        return false;
    }
    
    @Override
    public HorizontalLayout populateItemDetails(HorizontalLayout horizontalLayout, T documentVersion) {
        // add the first line
        horizontalLayout.removeAllComponents();
        horizontalLayout.setWidth("100%");
        if (documentVersion != null) {
            Label details = new Label(messageHelper.getMessage("popup.label.revision.details",
                    documentVersion.getVersionLabel(),
                    (documentVersion.getVersionComment() != null) ? documentVersion.getVersionComment()
                            : messageHelper.getMessage("popup.label.revision.nocomment"),
                    dateFormatter.format(Date.from(documentVersion.getLastModificationInstant())),
                    new UserLoginDisplayConverter(userHelper).convertToPresentation(documentVersion.getLastModifiedBy(), null, null)),
                    ContentMode.HTML);
            horizontalLayout.addComponent(details);
            horizontalLayout.setComponentAlignment(details, Alignment.MIDDLE_CENTER);
        }
        return horizontalLayout;
    }

    @Override
    public Set<String> getVersionLabels(List<T> displayedVersions) {
        Set<String> versions = new TreeSet<>(new VersionComparator());
        displayedVersions.forEach(version -> versions.add(version.getVersionLabel()));
        return versions;
    }

    @Override
    public void updateMinimizedRepresentation() {
        minimizedLabel = (filteredVersions.size() > 1)
                ? messageHelper.getMessage("popup.compare.caption", minSelection.getVersion().getVersionLabel(), maxSelection.getVersion().getVersionLabel())
                : messageHelper.getMessage("popup.compare.caption.disabled");
        markAsDirty();// Marking Dirty to update popup label
    }

    @Override
    public void selectionChanged() {
        updateMinimizedRepresentation();
        versionHistory.setSelection(minSelection.getVersion().getVersionLabel(), maxSelection.getVersion().getVersionLabel());
        eventBus.post(new MarkedContentRequestEvent<T>(minSelection.getVersion(), maxSelection.getVersion(), 0));
    }
    
    @Override
    public Set<RangeSliderStepVO> getVersionSteps(Set<String> displayedVersions) {
        Set<RangeSliderStepVO> versions = new TreeSet<>(new StepValueComparator());
        displayedVersions.forEach(version -> {
            versions.add(new RangeSliderStepVO(version, "", false));
        });
        return versions;
    }

    @Override
    public String getPopupCaption() {
        return messageHelper.getMessage("popup.header.caption");
    }
}
