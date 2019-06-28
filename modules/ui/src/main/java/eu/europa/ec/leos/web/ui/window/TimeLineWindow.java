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
package eu.europa.ec.leos.web.ui.window;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.extension.MathJaxExtension;
import eu.europa.ec.leos.ui.extension.SliderPinsExtension;
import eu.europa.ec.leos.web.event.component.ComparisonResponseEvent;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.TimeLineHeaderComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@com.vaadin.annotations.JavaScript({"vaadin://../js/lib/legacy/versionCompare.js" + LeosCacheToken.TOKEN})
public class TimeLineWindow<T extends XmlDocument> extends AbstractWindow {

    private static final long serialVersionUID = 638705322911001557L;

    private UserHelper userHelper;
    private HorizontalLayout contentCompareResult;
    private SecurityContext securityContext;

    private TimeLineHeaderComponent<T> header;

    final static int SINGLE_COLUMN_MODE = 1;
    final static int TWO_COLUMN_MODE = 2;

    final static String SINGLE_COLUMN_WIDTH = "27.0cm";
    final static String TWO_COLUMN_WIDTH = "37.5cm";

    public TimeLineWindow(final MessageHelper messageHelper, final EventBus eventBus) {
        super(messageHelper, eventBus);
    }

    public TimeLineWindow(final SecurityContext securityContext, final MessageHelper messageHelper, final EventBus eventBus, final UserHelper userHelper,
            List<T> documentVersions) {

        super(messageHelper, eventBus);
        this.userHelper = userHelper;
        this.securityContext= securityContext;

        setCaption(messageHelper.getMessage("document.versions.caption.window"));
        prepareWindow(documentVersions);
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(this);
    }

    public void prepareWindow(List<T> documentVersions) {
        setWidth(TWO_COLUMN_WIDTH); // default setting for two column mode
        setHeight(95, Unit.PERCENTAGE);

        VerticalLayout windowLayout = new VerticalLayout();
        windowLayout.setSizeFull();
        windowLayout.setSpacing(true);
        windowLayout.setMargin(false);
        setBodyComponent(windowLayout);

        fillLayout(windowLayout, documentVersions);
    }

    public void updateVersions(List<T> documentVersions) {
        header.updateVersions(documentVersions);
    }

    private void fillLayout(VerticalLayout windowLayout, List<T> documentVersions) {
        header = new TimeLineHeaderComponent<T>(securityContext, messageHelper, eventBus, userHelper, documentVersions);
        windowLayout.addComponent(header);

        contentCompareResult = new HorizontalLayout();
        contentCompareResult.setSpacing(false);
        contentCompareResult.setSizeFull();
        windowLayout.addComponent(contentCompareResult);
        windowLayout.setExpandRatio(contentCompareResult, 1f);

        SliderPinsExtension sliderPins = new SliderPinsExtension(contentCompareResult, getSelectorStyleMap());
    }

    private HorizontalLayout buildComparisonResultArea(HashMap<Integer, Object> htmlCompareResult) {
        contentCompareResult.removeAllComponents();
        contentCompareResult.setPrimaryStyleName("leos-compare-layout");

        if (htmlCompareResult.get(SINGLE_COLUMN_MODE) != null) {
            LeosDisplayField docDiff = new LeosDisplayField();
            new MathJaxExtension<>(docDiff);

            setWidth(SINGLE_COLUMN_WIDTH);// setting the window for single column layout
            docDiff.setSizeFull();
            contentCompareResult.addComponent(docDiff);
            contentCompareResult.setExpandRatio(docDiff, 1.0f);

            docDiff.setValue((String) htmlCompareResult.get(SINGLE_COLUMN_MODE));
        } else if (htmlCompareResult.get(TWO_COLUMN_MODE) != null) {
            LeosDisplayField leftSide = new LeosDisplayField();
            leftSide.setValue(((String[]) htmlCompareResult.get(TWO_COLUMN_MODE))[0]);
            new MathJaxExtension<>(leftSide);
            leftSide.setPrimaryStyleName("leos-two-column-compare");

            LeosDisplayField rightSide = new LeosDisplayField();
            new MathJaxExtension<>(rightSide);
            rightSide.setPrimaryStyleName("leos-two-column-compare");

            setWidth(TWO_COLUMN_WIDTH); // expanding the window for two column layout
            leftSide.setSizeUndefined();
            contentCompareResult.addComponent(leftSide);
            contentCompareResult.setExpandRatio(leftSide, 0.5f);
            
            rightSide.setSizeUndefined();
            contentCompareResult.addComponent(rightSide);
            contentCompareResult.setExpandRatio(rightSide, 0.5f);

            leftSide.setValue(((String[]) htmlCompareResult.get(TWO_COLUMN_MODE))[0]);
            rightSide.setValue(((String[]) htmlCompareResult.get(TWO_COLUMN_MODE))[1]);
            // this implementation binds as well a JS call to align the modified elements
            JavaScript.getCurrent().execute("versionCompare.alignModifiedElements();");
        }
        return contentCompareResult;
    }

    private Map<String, String> getSelectorStyleMap(){
        Map<String, String> selectorStyleMap = new HashMap<>();
        selectorStyleMap.put(".leos-marker-content-removed","pin-leos-marker-content-removed");
        selectorStyleMap.put(".leos-marker-content-added","pin-leos-marker-content-added");
        selectorStyleMap.put(".leos-content-removed","pin-leos-content-removed");
        selectorStyleMap.put(".leos-content-new", "pin-leos-content-new");
        return selectorStyleMap;
    }

    @Subscribe
    public void setComparisonContent(ComparisonResponseEvent event) {
        buildComparisonResultArea(event.getResult());
        contentCompareResult.addStyleName(event.getLeosCategory());
        this.center();
        this.focus();
    }
}
