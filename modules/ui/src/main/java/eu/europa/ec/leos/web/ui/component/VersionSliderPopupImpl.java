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
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.ui.component.RangeSliderComponent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class VersionSliderPopupImpl<T extends XmlDocument> extends PopupView implements VersionSliderPopup<T> {
    private static final long serialVersionUID = -5435432434L;

    protected static final Logger LOG = LoggerFactory.getLogger(VersionSliderPopupImpl.class);
    protected final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    protected static final String FIRST_VERSION = "1.0";
    
    protected MessageHelper messageHelper;
    protected EventBus eventBus;
    protected UserHelper userHelper;

    // components that contain display data
    protected String minimizedLabel;
    protected VerticalLayout popupMaximizedLayout = new VerticalLayout();
    protected HorizontalLayout oldSelectedDetails = new HorizontalLayout();
    protected VerticalLayout sliderLayout;

    // data
    protected List<T> filteredVersions;
    protected Selection minSelection = new Selection();
    protected Selection maxSelection = new Selection();

    protected final VersionHistory versionHistory;
    protected final VersionsObserver versionsObserver;

    protected VersionSliderPopupImpl(MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;

        versionHistory = new VersionHistory();
        versionsObserver = new VersionsObserver();
        versionHistory.addObserver(versionsObserver);

        init();

        // FIXME one of parent class constructors should be called for proper component hierarchy initialization
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
        maxSelection.setVersion(null);
        minSelection.setVersion(null);
        eventBus.post(new VersionListRequestEvent<T>());
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
    }

    private void init() {
        setHeight(100, Unit.PERCENTAGE);
        setHideOnMouseOut(false);
        setStyleName("leos-version-slider-popup");

        initMaximizedComponent();
    }

    private void initMaximizedComponent() {
        popupMaximizedLayout.setStyleName("leos-version-slider-popup-max");
        popupMaximizedLayout.removeAllComponents();
        popupMaximizedLayout.setWidth(500, Unit.PIXELS);

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setWidth(100, Unit.PERCENTAGE);
        popupMaximizedLayout.addComponent(hLayout);

        final Label popupCaption = new Label(getPopupCaption());
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
            if (event.isPopupVisible()) {
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
        rangeSliderComponent.setSteps(getVersionSteps(versionHistory.getVersionSteps()));
        rangeSliderComponent.setHandles(versionHistory.getVersionHandles());
        rangeSliderComponent.setDisableInitialVersion(isDisableInitialVersion());
        
        return rangeSliderComponent;
    }


    public T getVersionProperties(String documentLabel) {
        for (T version : filteredVersions) {
            if (documentLabel != null && documentLabel.equals(version.getVersionLabel())) {
                return version;
            }
        }
        return null;
    }

    public List<T> getSelectedVersions() {
        List<T> selectedVersions = new ArrayList();
        selectedVersions.add(minSelection.getVersion());
        selectedVersions.add(maxSelection.getVersion());
        return selectedVersions;
    }
    
    public class Selection extends Observable {
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

    public class VersionHistory extends Observable {
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
            return Arrays.copyOf(versionHandles, 1);// only lower version is required for this class.
        }
    }

    public class VersionsObserver implements Observer {

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
                            rangeSlider.setSteps(getVersionSteps(versionHistory.getVersionSteps()));
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

    public class VersionSliderPopupContent implements PopupView.Content, Observer {

        private static final long serialVersionUID = -6423459105885332698L;

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
}
