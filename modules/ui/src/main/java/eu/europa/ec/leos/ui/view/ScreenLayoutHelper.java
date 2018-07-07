/*
 * Copyright 2017 European Commission
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
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.view.PaneAddEvent;
import eu.europa.ec.leos.web.event.view.PaneEnableEvent;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ScreenLayoutHelper {
    private static final Logger LOG = LoggerFactory.getLogger(ScreenLayoutHelper.class);

    private EventBus eventBus;
    private List<HorizontalSplitPanel> splitters;
    public ScreenLayoutHelper(EventBus eventBus, List<HorizontalSplitPanel> splitters) {
        this.eventBus = eventBus;
        this.splitters = splitters;
    }
    
    private List<PaneSettings> panesList = new ArrayList<>();    // List of panes on the screen, flag enabling them or not

    public void addPane(ContentPane pane, Integer position, Boolean isEnabled) {
        eventBus.post(new PaneAddEvent(pane.getClass()));
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

    private PaneSettings findPaneSettings(Class componentClass) {
        Optional<PaneSettings> paneSettings = panesList.stream().filter(f -> f.getPane().getClass() == componentClass).findFirst();
        if (paneSettings.isPresent()) {
            return paneSettings.get();
        }        
        return null;
    }

    private PaneSettings findPaneSettingsByPosition(int position) {
        Optional<PaneSettings> paneSettings = panesList.stream().filter(f -> f.getPosition() == position).findFirst();
        if (paneSettings.isPresent()) {
            return paneSettings.get();
        }        
        return null;
    }

    @Subscribe
    void changePosition(LayoutChangeRequestEvent event) {
        ColumnPosition position = event.getPosition();
        updatedComponentPosition(position, event.getOriginatingComponent());
        layoutComponents();
        LOG.debug("position changed to {} ...", position);
    }

    public void updatedComponentPosition(ColumnPosition position, Class componentClass){
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
    public List<PaneSettings> getSortedEnabledPanes() {
        List<PaneSettings> enabledPanes = panesList.stream().filter(pane -> pane.isEnabled()).collect(Collectors.toList());
        Collections.sort(enabledPanes, (f1, f2) -> f1.getPosition().compareTo(f2.getPosition()));
        return enabledPanes;
    }

    public void layoutComponents(){
        for(HorizontalSplitPanel splitPanel:splitters){
            splitPanel.removeAllComponents();
        }

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
    void moveSplitterPosition(SplitPositionEvent event) {
        LOG.debug("Split position event handled (move direction={})...", event.getMoveDirection());
        SplitPositionEvent.MoveDirection direction = event.getMoveDirection();
        Component originatingComponent = event.getOriginatingComponent();
        List<PaneSettings> panes = getSortedEnabledPanes();
        PaneSettings paneSettings = findPaneSettings(originatingComponent.getClass());

        //FIXME this code is very very bad. Cleanup required.
        HorizontalSplitPanel parentSplitter = (HorizontalSplitPanel) originatingComponent.getParent();
        float splitterPosition =0f;

        if (panes.size() >= 2) {
            if (paneSettings.getPosition() < 2) {  //Not in the top splitter or only one splitter
                if ((paneSettings.getPosition() == 0) && (direction.equals(SplitPositionEvent.MoveDirection.LEFT))) {
                    splitterPosition=(parentSplitter.getSplitPosition() > paneSettings.getPane().getDefaultPaneWidth(2))
                            ? paneSettings.getPane().getDefaultPaneWidth(panes.size())
                            : 0.0f;
                    parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
                }
                else if ((panes.size() == 3)  && (paneSettings.getPosition() == 1) && isPaneOnTheRight(paneSettings) && (direction.equals(SplitPositionEvent.MoveDirection.LEFT))) {
                    HorizontalSplitPanel topSplitter = (HorizontalSplitPanel) parentSplitter.getParent();
                    splitterPosition = getDefaultSplitterPosition(1);
                    topSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
                }
                else if ((panes.size() == 2)  && (paneSettings.getPosition() == 1) && isPaneOnTheRight(paneSettings) && (direction.equals(SplitPositionEvent.MoveDirection.LEFT))) {
                    splitterPosition = getDefaultSplitterPosition(0);
                    parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
                }
                else if ((paneSettings.getPosition() == 1) && isPaneOnTheLeft(paneSettings) && (direction.equals(SplitPositionEvent.MoveDirection.RIGHT))) {
                    splitterPosition = getDefaultSplitterPosition(0);
                    parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
                }
            }
            else if (paneSettings.getPosition() == 2) {
                if (direction.equals(SplitPositionEvent.MoveDirection.RIGHT)) {
                    splitterPosition =(parentSplitter.getSplitPosition() < getDefaultSplitterPosition(1))
                            ? getDefaultSplitterPosition(1)
                            : 100.0f;
                }
                parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
            }
        }
        LOG.debug("Update position to {})...", splitterPosition);
    }

    private boolean isPaneOnTheRight(PaneSettings paneSettings) {
        return (findPaneSettingsByPosition(paneSettings.getPosition() + 1)!= null && findPaneSettingsByPosition(paneSettings.getPosition() + 1).isEnabled());
    }

    private boolean isPaneOnTheLeft(PaneSettings paneSettings) {
        return (findPaneSettingsByPosition(paneSettings.getPosition() - 1)!= null && findPaneSettingsByPosition(paneSettings.getPosition() - 1).isEnabled());
    }

    // TODO calculation to be checked again -- bad implementation
    private float getDefaultSplitterPosition(int indexSplitter) {
        float splitterSize=0f;

        List<PaneSettings> panes = getSortedEnabledPanes();
        switch(panes.size()){
            case 1:
                splitterSize=0f;
                break;
            case 2:
                splitterSize = 100f - panes.get(1).getPane().getDefaultPaneWidth(2);
                break;
            case 3:
                splitterSize = (indexSplitter == 0) ? 100f - panes.get(1).getPane().getDefaultPaneWidth(3) : 100f - panes.get(2).getPane().getDefaultPaneWidth(3);
                break;
        }//end switch
        return splitterSize;
    }

    private class PaneSettings {
        private Boolean enabled = false;
        private Integer position = 0;
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
