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
package eu.europa.ec.leos.ui.component.versions;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.ui.extension.CollapsibleEllipsisExtension;
import eu.europa.ec.leos.ui.view.TriFunction;
import eu.europa.ec.leos.web.event.view.document.ComparisonEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SpringComponent
@ViewScope
@DesignRoot("VersionsTabDesign.html")
public class VersionsTab<D extends XmlDocument> extends VerticalLayout {
    
    private static final long serialVersionUID = -2540336182761979302L;

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private UserHelper userHelper;
    private VersionComparator versionComparator;
    
    private VerticalLayout versionsCardsHolder;
    private Button compareModeButton;
    private Button searchButton;

    private TriFunction<String, Integer, Integer, List<D>> minorVersionsFn;
    private Function<String, Integer> countMinorVersionsFn;
    private BiFunction<Integer, Integer, List<D>> recentChangesFn;
    private Supplier<Integer> countRecentChangesFn;
    private List<VersionVO> allVersions;
    private boolean comparisonMode;
    private boolean comparisonAvailable;
    
    private Set<CheckBox> allCheckBoxes;
    private Set<VersionVO> selectedCheckBoxes;
    private CollapsibleEllipsisExtension<VerticalLayout> ellipsisExtension;
    
    @Autowired
    public VersionsTab(MessageHelper messageHelper, EventBus eventBus, UserHelper userHelper, VersionComparator versionComparator) {
        this.messageHelper = messageHelper;
        this.eventBus = eventBus;
        this.userHelper = userHelper;
        this.versionComparator = versionComparator;
        
        Design.read(this);
        initView();
        initExtensions();
        allCheckBoxes = new HashSet<>();
        selectedCheckBoxes = new HashSet<>();
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
    
    private void initView() {
        compareModeButton.setIcon(VaadinIcons.COPY);
        compareModeButton.addClickListener(listener -> {
            eventBus.post(new ComparisonEvent(true));
            buildCards();
        });
        searchButton.setIcon(VaadinIcons.SEARCH);
        searchButton.setEnabled(false);
    }

    private void initExtensions() {
        ellipsisExtension = new CollapsibleEllipsisExtension<>(versionsCardsHolder, eventBus);
        ellipsisExtension.getState().showLess = "(" + messageHelper.getMessage("document.version.recentChanges.hide") + ")";
        ellipsisExtension.getState().showMore = "(" + messageHelper.getMessage("document.version.recentChanges.show") + ")";
    }

    public void setDataFunctions(
            List<VersionVO> allVersions,
            TriFunction<String, Integer, Integer, List<D>> minorVersionsFn, Function<String, Integer> countMinorVersionsFn,
            BiFunction<Integer, Integer, List<D>> recentChangesFn, Supplier<Integer> countRecentChangesFn,
            boolean comparisonAvailable) {

        this.minorVersionsFn = minorVersionsFn;
        this.countMinorVersionsFn = countMinorVersionsFn;
        this.recentChangesFn = recentChangesFn;
        this.countRecentChangesFn = countRecentChangesFn;
        this.allVersions = allVersions;
        this.comparisonAvailable = comparisonAvailable;
        
        enableDisableCompareButton();
        buildCards();
    }
    
    private void enableDisableCompareButton(){
        final String description;
        final boolean isEnable;
        if (comparisonAvailable) {
            isEnable = !comparisonMode;
            description = messageHelper.getMessage("document.accordion.versions.compare.button");
        } else {
            isEnable = false;
            description = messageHelper.getMessage("document.accordion.versions.compare.button.notAvailable");
        }
        compareModeButton.setDescription(description, ContentMode.HTML);
        compareModeButton.setEnabled(isEnable);
    }

    /**
     * On DocumentUpdatedEvent for now we recreate all the cards.
     * We can switch when coming a MinorChangeUpdatedEvent, we recreate only the recent card, otherwise the rest
     */
    private void buildCards() {
        versionsCardsHolder.removeAllComponents();
        
        VersionCard<D> recentCard = new VersionCard<>(null,
                minorVersionsFn, countMinorVersionsFn,
                recentChangesFn, countRecentChangesFn,
                messageHelper, eventBus, userHelper,
                comparisonMode, comparisonAvailable,
                allCheckBoxes, selectedCheckBoxes,
                versionComparator);
        versionsCardsHolder.addComponent(recentCard);

        for (VersionVO versionVO : allVersions) {
            VersionCard<D> minorCard = new VersionCard<>(versionVO,
                    minorVersionsFn, countMinorVersionsFn,
                    recentChangesFn, countRecentChangesFn,
                    messageHelper, eventBus, userHelper,
                    comparisonMode, comparisonAvailable,
                    allCheckBoxes, selectedCheckBoxes,
                    versionComparator);
            versionsCardsHolder.addComponent(minorCard);
        }
    }
    
    public void refreshVersions(List<VersionVO> allVersions, boolean comparisonMode) {
        this.allVersions = allVersions;
        this.comparisonMode = comparisonMode;
        compareModeButton.setEnabled(!comparisonMode);
        if(!comparisonMode) {
            allCheckBoxes = new HashSet<>();
            selectedCheckBoxes = new HashSet<>();
        }
        buildCards();
        ellipsisExtension.addCollapsibleListener();
    }
}
