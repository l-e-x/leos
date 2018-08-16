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
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.*;
import com.vaadin.shared.ui.ContentMode;
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.ui.component.RangeSliderComponent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.event.component.MarkedContentRequestEvent;
import eu.europa.ec.leos.web.support.VersionComparator;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.converter.UserLoginDisplayConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.*;

public class VersionSliderPopup<T extends LeosDocument.XmlDocument> extends PopupView {
    private static final long serialVersionUID = -5435432434L;
    private static final Logger LOG = LoggerFactory.getLogger(VersionSliderPopup.class);
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private MessageHelper messageHelper;
    private EventBus eventBus;
    private UserHelper userHelper;

    // components that contain display data
    private String minimizedLabel;
    private VerticalLayout popupMaximizedLayout = new VerticalLayout();
    private HorizontalLayout oldSelectedDetails = new HorizontalLayout();
    private static final boolean HIDE_ALL_MINOR_BEFORE_LAST_MAJOR = true;
    private static final boolean PEG_MAX_TO_LATEST = true;
    private VerticalLayout sliderLayout;

    // data
    private List<T> filteredVersions;
    private Selection minSelection = new Selection();
    private Selection maxSelection = new Selection();

    private final VersionHistory versionHistory;
    private final VersionsObserver versionsObserver;

    // init is a stateless function which should not be tied to item/card data
    protected VersionSliderPopup(MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;

        versionHistory = new VersionHistory();
        versionsObserver = new VersionsObserver();
        versionHistory.addObserver(versionsObserver);

        init();

        // FIXME one of parent class constructors should be called for proper component hierarchy initialization
    }

    @Override public void attach() {
        super.attach();
        eventBus.register(this);
        maxSelection.setVersion(null);
        minSelection.setVersion(null);
        eventBus.post(new VersionListRequestEvent<T>());
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
        maxSelection.addObserver(content);//popup component needs to react to changes in selections
        minSelection.addObserver(content);

        setContent(content);
    }

    @Subscribe
    public void requestVersionList(DocumentUpdatedEvent event) {
        eventBus.post(new VersionListRequestEvent<T>());
    }

    private void initMaximizedComponent() {
        // initialize Maximized layout
        popupMaximizedLayout.setStyleName("leos-version-slider-popup-max");
        popupMaximizedLayout.removeAllComponents();
        popupMaximizedLayout.setWidth(500, Unit.PIXELS);

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setWidth(100, Unit.PERCENTAGE);
        popupMaximizedLayout.addComponent(hLayout);

        final Label popupCaption = new Label(messageHelper.getMessage("popup.header.caption"));
        hLayout.addComponent(popupCaption);

        final Label currentMarker = new Label(messageHelper.getMessage("popup.header.current.caption"), ContentMode.HTML);
        currentMarker.addStyleName("popup-current-caption");
        hLayout.addComponent(currentMarker);

        sliderLayout = new VerticalLayout();
        sliderLayout.setSizeFull();
        sliderLayout.setMargin(false);
        popupMaximizedLayout.addComponent(sliderLayout);
        popupMaximizedLayout.setComponentAlignment(sliderLayout, Alignment.MIDDLE_CENTER);
        popupMaximizedLayout.setExpandRatio(sliderLayout, .5f);

        addPopupVisibilityListener(event -> {
            if(event.isPopupVisible()) {
                LOG.trace("Popup is visible, adding range slider component...");
                RangeSliderComponent slider = createSliderComponent();
                sliderLayout.addComponent(slider);
                sliderLayout.setComponentAlignment(slider, Alignment.MIDDLE_CENTER);
            } else {
                LOG.trace("Popup is hidden, removing range slider component...");
                sliderLayout.removeAllComponents();
            }
        });

        popupMaximizedLayout.addComponent(oldSelectedDetails);
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

        if (HIDE_ALL_MINOR_BEFORE_LAST_MAJOR) {
            boolean versionAfterLastMajorVersion = true;
            /*INFO: All versions of the version series, sorted by {@code cmis:creationDate}
              descending and preceded by the PWC, if one exists, not {@code null}*/
            for(T version : allDocumentVersions) {
                if (version.isMajorVersion()) {
                    versionAfterLastMajorVersion = false;
                    filteredVersions.add(version);
                }

                if (versionAfterLastMajorVersion) {
                    filteredVersions.add(version);
                }
            }
        } else {
            filteredVersions.addAll(allDocumentVersions);
        }

        return filteredVersions;
    }

    private void setSelections(List<T> versions) {
        // if no version was already selected, only then modify
        if (minSelection.getVersion() == null ||
                !getVersionLabels(versions).contains(minSelection.getVersion().getVersionLabel())) {
                minSelection.setVersion(versions.get(0));//set to latest version
        }

        if (maxSelection.getVersion() == null) {
            maxSelection.setVersion(versions.get(0));
        }
        if (PEG_MAX_TO_LATEST) { // override if max is pegged to latest
            maxSelection.setVersion(versions.get(0));
        }
    }

    private RangeSliderComponent createSliderComponent() {
        final RangeSliderComponent rangeSliderComponent = new RangeSliderComponent();
        rangeSliderComponent.setWidth(90, Unit.PERCENTAGE);
        rangeSliderComponent.setStyleName("leos-version-slider");
        rangeSliderComponent.addHandlesListener(new RangeSliderComponent.HandlesListener() {
            private static final long serialVersionUID = 7505199713966444736L;

            @Override
            public void valuesChanged(RangeSliderComponent.HandlesEvent event) {
                LOG.debug("Slider handles values changed!");
                String[] updatedHandles = event.getComponent().getHandles();

                minSelection.setVersion(getVersionProperties(updatedHandles[0]));
            }
        });

        rangeSliderComponent.addAttachListener(event -> {
            LOG.trace("Attaching range slider component...");
            versionsObserver.attach((RangeSliderComponent) event.getSource());
        });

        rangeSliderComponent.addDetachListener(event -> {
            LOG.trace("Detaching range slider component...");
            versionsObserver.detach();
        });

        rangeSliderComponent.setColouredArea(RangeSliderComponent.ColouredArea.UPPER);
        rangeSliderComponent.setSteps(versionHistory.getVersionSteps());
        rangeSliderComponent.setHandles(versionHistory.getVersionHandles());

        return rangeSliderComponent;
    }

    private HorizontalLayout populateItemDetails(HorizontalLayout horizontalLayout, T documentVersion) {
        // add the first line
        horizontalLayout.removeAllComponents();
        horizontalLayout.setWidth("100%");
        if (documentVersion != null) {
            Label details = new Label(messageHelper.getMessage("popup.label.revision.details",
                    documentVersion.getVersionLabel(),
                    (documentVersion.getVersionComment() != null) ? documentVersion.getVersionComment() : messageHelper.getMessage("popup.label.revision.nocomment"),
                    dateFormatter.format(Date.from(documentVersion.getLastModificationInstant())),
                    new UserLoginDisplayConverter(userHelper).convertToPresentation(documentVersion.getLastModifiedBy(), null, null)),
                    ContentMode.HTML);
            horizontalLayout.addComponent(details);
            horizontalLayout.setComponentAlignment(details, Alignment.MIDDLE_CENTER);
        }
        return horizontalLayout;
    }

    private Set<String> getVersionLabels(List<T> displayedVersions) {
        Set<String> versions = new TreeSet<>(new VersionComparator());
        displayedVersions.forEach(version -> versions.add(version.getVersionLabel()));
        return versions;
    }

    private T getVersionProperties(String documentLabel) {
        for (T version : filteredVersions) {
            if (documentLabel != null && documentLabel.equals(version.getVersionLabel())) {
                return version;
            }
        }
        return null;
    }

    private void updateMinimizedRepresentation() {
        minimizedLabel = (filteredVersions.size() > 1)
                    ? messageHelper.getMessage("popup.compare.caption", minSelection.getVersion().getVersionLabel(), maxSelection.getVersion().getVersionLabel())
                    : messageHelper.getMessage("popup.compare.caption.disabled");
        markAsDirty();// Marking Dirty to update popup label
    }

    private void selectionChanged(){
        updateMinimizedRepresentation();
        versionHistory.setSelection(minSelection.getVersion().getVersionLabel(), maxSelection.getVersion().getVersionLabel());
        eventBus.post(new MarkedContentRequestEvent(minSelection.getVersion(), maxSelection.getVersion(), 0));
    }

    private class VersionSliderPopupContent implements PopupView.Content, Observer {

        private static final long serialVersionUID = 6659583160446283505L;

        public String getMinimizedValueAsHTML() {
            return minimizedLabel;
        }

        public Component getPopupComponent() {
            return popupMaximizedLayout;
        }

        @Override
        public void update(Observable o, Object arg) {
            if (arg != null && o.equals(minSelection)) {
                populateItemDetails(oldSelectedDetails, minSelection.getVersion());
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

    private class VersionHistory extends Observable {
        public static final String HISTORY = "HISTORY";
        public static final String SELECTION = "SELECTION";

        private Set<String> versionSteps = Collections.emptySet();
        private String[] versionHandles = {};

        public void setHistory(List<T> versions) {
            versionSteps = getVersionLabels(versions);
            setChanged();
            notifyObservers(HISTORY);
        }

        public void setSelection(String lowerVersion, String higherVersion) {
            versionHandles = new String[]{lowerVersion, higherVersion};
            setChanged();
            notifyObservers(SELECTION);
        }

        public Set<String> getVersionSteps() {
            return Collections.unmodifiableSet(versionSteps);
        }

        public String[] getVersionHandles() {
            return Arrays.copyOf(versionHandles, 1);//only lower version is required for this class.
        }
    }

    private class VersionsObserver implements Observer {

        private WeakReference<RangeSliderComponent> sliderRef;

        public void attach(RangeSliderComponent slider) {
            sliderRef = new WeakReference<>(slider);
        }

        public void detach() {
            sliderRef.clear();
        }

        @Override
        public void update(Observable obs, Object arg) {
            if (sliderRef != null) {
                final RangeSliderComponent rangeSlider = sliderRef.get();
                if ((rangeSlider != null) && (VersionHistory.class.isInstance(obs)) && (String.class.isInstance(arg))) {
                    VersionHistory versionHistory = (VersionHistory) obs;
                    switch ((String) arg) {
                        case VersionHistory.HISTORY: {
                            LOG.trace("Versions observer: updating range slider steps...");
                            rangeSlider.setSteps(versionHistory.getVersionSteps());
                            break;
                        }
                        case VersionHistory.SELECTION: {
                            LOG.trace("Versions observer: updating range slider handles...");
                            rangeSlider.setHandles(versionHistory.getVersionHandles());
                            break;
                        }
                        default: {
                            LOG.trace("Versions observer: ignoring history update...");
                        }
                    }
                }
            }
        }
    }
}
