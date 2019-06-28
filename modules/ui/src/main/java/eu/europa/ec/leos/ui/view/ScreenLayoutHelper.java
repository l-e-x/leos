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
package eu.europa.ec.leos.ui.view;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractSplitPanel;
import com.vaadin.ui.HorizontalSplitPanel;
import eu.europa.ec.leos.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.view.PaneAddEvent;
import eu.europa.ec.leos.web.event.view.PaneEnableEvent;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ScreenLayoutHelper implements AbstractSplitPanel.SplitterClickListener{
    private static final Logger LOG = LoggerFactory.getLogger(ScreenLayoutHelper.class);

    private EventBus eventBus;
    private List<HorizontalSplitPanel> splitters;
    private List<PaneSettings> panesList;

    public ScreenLayoutHelper(EventBus eventBus, List<HorizontalSplitPanel> splitters) {
        this.eventBus = eventBus;
        this.splitters = splitters;
        this.panesList = new ArrayList<>();    // List of panes on the screen, flag enabling them or not
    }

    public void addPane(ContentPane pane, Integer position, Boolean isEnabled) {
        eventBus.post(new PaneAddEvent(pane.getClass(), pane.getChildClass()));
        PaneSettings paneSettings = new PaneSettings(pane, position, isEnabled);
        panesList.add(paneSettings);
    }

    public boolean isPaneEnabled(Class componentClass) {
        Optional<PaneSettings> paneSettings = panesList.stream().filter(f -> f.getPane().getClass() == componentClass).findFirst();
        if (paneSettings.isPresent()) {
            return paneSettings.get().isEnabled();
        }        
        return false;
    }

    @Subscribe
    void changePosition(LayoutChangeRequestEvent event) {
        ColumnPosition position = event.getPosition();
        updatedComponentPosition(position, event.getOriginatingComponent());
        layoutComponents();
        LOG.debug("position changed to {} ...", position);
    }

    private void updatedComponentPosition(ColumnPosition position, Class componentClass){
        Optional<PaneSettings> paneSettings = panesList.stream().filter(f -> f.getPane().getClass() == componentClass).findFirst();
        if (paneSettings.isPresent()) {
            switch (position) {
                case OFF:
                    paneSettings.get().setEnabled(false);
                    break;
                default:
                    paneSettings.get().setEnabled(true);
                    break;
            }
        }
}
    private List<PaneSettings> getSortedEnabledPanes() {
        List<PaneSettings> enabledPanes = panesList.stream().filter(pane -> pane.isEnabled()).collect(Collectors.toList());
        Collections.sort(enabledPanes, (f1, f2) -> f1.getPosition().compareTo(f2.getPosition()));
        return enabledPanes;
    }

    public void layoutComponents(){
        for(HorizontalSplitPanel splitPanel:splitters){
            splitPanel.removeAllComponents();
            splitPanel.addSplitterClickListener(this);
        }

        placeComponents();
    }

    private void placeComponents() {
        List<PaneSettings> panes = getSortedEnabledPanes();

        int splitterIndex = splitters.size() - 1;
        int componentIndex =  panes.size() - 1;

        do {
            HorizontalSplitPanel currentSplitPanel = splitters.get(splitterIndex);
            currentSplitPanel.setSecondComponent(panes.get(componentIndex--).getPane());

            if (componentIndex >=1 ) {//remaining component 2 or more
                currentSplitPanel.setFirstComponent(splitters.get(splitterIndex - 1));
            }
            else if (componentIndex == 0) {//only one remaining component
                currentSplitPanel.setFirstComponent(panes.get(componentIndex--).getPane());
            }

            currentSplitPanel.setSplitPosition(getDefaultSplitterPosition(splitterIndex), Unit.PERCENTAGE);
            splitterIndex--;
        } while(componentIndex > 0 );
    }

    @Subscribe
    public void splitterClick(AbstractSplitPanel.SplitterClickEvent event) {
        if (event.isDoubleClick()) {
            //Reset layout pane components position.
            placeComponents();
        }
    }

    // TODO calculation to be checked again -- bad implementation
    private float getDefaultSplitterPosition(int indexSplitter) {
        final float splitterSize;

        List<PaneSettings> panes = getSortedEnabledPanes();
        boolean tocPaneEnabled = isTocPaneEnabled();
        //we give priority to the first element - left element
        switch(panes.size()){
            case 2:
                splitterSize = panes.get(0).getPane().getDefaultPaneWidth(2, tocPaneEnabled);
                break;
            case 3:
                splitterSize = (indexSplitter == 0) ?
                                panes.get(0).getPane().getDefaultPaneWidth(3, tocPaneEnabled) :
                                100f - panes.get(2).getPane().getDefaultPaneWidth(3, tocPaneEnabled);
                break;
            default:
                splitterSize=0f;
                break;
        }//end switch
        return splitterSize;
    }

    private boolean isTocPaneEnabled() {
        return panesList.stream().anyMatch(paneSettings -> paneSettings.getPane().getClass().equals(TableOfContentComponent.class) && paneSettings.isEnabled());
    }

    private class PaneSettings {
        private Boolean enabled = false;
        private Integer position;
        private ContentPane pane;

        PaneSettings(ContentPane pane, Integer position, Boolean enabled) {
            this.pane = pane;
            this.position = position;
            this.setEnabled(enabled);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            eventBus.post(new PaneEnableEvent(pane.getClass(), enabled));
        }

        public Integer getPosition() {
            return position;
        }

        public ContentPane getPane() {
            return pane;
        }
    }
}
