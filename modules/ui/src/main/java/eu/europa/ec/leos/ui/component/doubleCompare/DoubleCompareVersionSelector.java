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
package eu.europa.ec.leos.ui.component.doubleCompare;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.ui.component.RangeSliderStepVO;
import eu.europa.ec.leos.ui.event.doubleCompare.DoubleCompareContentRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.VersionSelectorUpdateEvent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.support.VersionComparator;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.VersionSliderPopupImpl;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DoubleCompareVersionSelector<T extends XmlDocument> extends VersionSliderPopupImpl<T> {

    private static final long serialVersionUID = 3009163855541962623L;

    protected DoubleCompareVersionSelector(MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper) {
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
        setSelections(filteredVersions);
        versionHistory.setHistory(filteredVersions);
        boolean versionSelectorState = (filteredVersions != null && filteredVersions.size() > 2);
        setEnabled(versionSelectorState);
        eventBus.post(new VersionSelectorUpdateEvent(versionSelectorState));
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
                    versionAfterLastMajorVersion = false;
                    filteredVersions.add(version);
                }
            }

        return filteredVersions;
    }

    private void setSelections(List<T> versions) {
        // if no version was already selected, only then modify
        if (minSelection.getVersion() == null ||
                !getVersionLabels(versions).contains(minSelection.getVersion().getVersionLabel())) {
            if(filteredVersions.size() > 2) {
                minSelection.setVersion(versions.get(1));
            } else {
                minSelection.setVersion(versions.get(0));
            }
        } 
        maxSelection.setVersion(versions.get(0));
    }
    
    @Override
    public boolean isDisableInitialVersion() {
        return true;
    }

    @Override
    public HorizontalLayout populateItemDetails(HorizontalLayout horizontalLayout, T documentVersion) {
        // add the first line
        horizontalLayout.removeAllComponents();
        horizontalLayout.setWidth("100%");
        StringBuilder versionCommentsBuilder = new StringBuilder();
        if (documentVersion != null) {
            if((documentVersion.getVersionComment() != null)) {
                versionCommentsBuilder.append(documentVersion.getVersionComment());
                if(documentVersion.getMilestoneComments() != null && documentVersion.getMilestoneComments().size() > 0) {
                    versionCommentsBuilder.append(" | ").append(getMilestoneComments(documentVersion));
                }
            } else {
                versionCommentsBuilder.append(messageHelper.getMessage("popup.label.revision.nocomment"));
            }
            
            Label details = new Label(messageHelper.getMessage("popup.label.revision.details",
                    documentVersion.getVersionLabel(), StringUtils.abbreviate(versionCommentsBuilder.toString(), 50),
                    dateFormatter.format(Date.from(documentVersion.getLastModificationInstant())),
                    new UserLoginDisplayConverter(userHelper).convertToPresentation(documentVersion.getLastModifiedBy(), null, null)),
                    ContentMode.HTML);
            horizontalLayout.addComponent(details);
            horizontalLayout.setComponentAlignment(details, Alignment.MIDDLE_CENTER);
        }
        return horizontalLayout;
    }

    private String getMilestoneComments(T version) {
        String mileStoneComments = null;
        final StringBuffer commentsBuffer = new StringBuffer(" - ");
        if (version.getMilestoneComments().size() == 1){
            commentsBuffer.append(version.getMilestoneComments().get(0));
            mileStoneComments = commentsBuffer.toString();
        } else {
            for (String comment : version.getMilestoneComments()) {
                commentsBuffer.append(comment).append("\n - ");
            }
            mileStoneComments = commentsBuffer.delete(commentsBuffer.lastIndexOf("\n - "), commentsBuffer.length()).toString();
        }
        return mileStoneComments;
    }
    
    @Override
    public Set<String> getVersionLabels(List<T> displayedVersions) {
        Set<String> versions = new TreeSet<>(new VersionComparator());
        displayedVersions.forEach(version -> versions.add(version.getVersionLabel()));
        return versions;
    }

    @Override
    public void updateMinimizedRepresentation() {
        String entity = getUserEntity(maxSelection.getVersion().getLastModifiedBy());
        
        minimizedLabel = (filteredVersions.size() > 2)
                ? messageHelper.getMessage("popup.double.compare.minimized.caption", minSelection.getVersion().getVersionLabel(),
                        new UserLoginDisplayConverter(userHelper).convertToPresentation(maxSelection.getVersion().getLastModifiedBy(), null, null),
                        entity ,dateFormatter.format(Date.from(maxSelection.getVersion().getLastModificationInstant())))
                : messageHelper.getMessage("popup.double.compare.minimized.disabled");
        markAsDirty();// Marking Dirty to update popup label
    }

    @Override
    public void selectionChanged() {
        updateMinimizedRepresentation();
        versionHistory.setSelection(minSelection.getVersion().getVersionLabel(), null);
        boolean enabled = filteredVersions.size() > 2 ? true : false;
        eventBus.post(new DoubleCompareContentRequestEvent<T>(filteredVersions.get(filteredVersions.size() - 1), minSelection.getVersion(), maxSelection.getVersion(), 0, enabled));
    }
    
    @Override
    public Set<RangeSliderStepVO> getVersionSteps(Set<String> displayedVersions) {
        Set<RangeSliderStepVO> versions = new LinkedHashSet<>();
        displayedVersions.stream().sorted(new VersionComparator()).forEach(version -> {
            if(version.equals(FIRST_VERSION)) {
                versions.add(new RangeSliderStepVO(messageHelper.getMessage("popup.double.compare.ec.proposal"), "", false));
            } else {
                versions.add(new RangeSliderStepVO(version, "", false));
            }
        });
        return versions;
    }
    
    private String getUserEntity(String userId) {
        User user = userHelper.getUser(userId);
        return user.getEntity();
    }

    @Override
    public String getPopupCaption() {
        return messageHelper.getMessage("popup.double.compare.header.caption");
    }
}
