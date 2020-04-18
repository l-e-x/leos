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
import com.vaadin.server.Page;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.AbstractSplitPanel;
import com.vaadin.ui.HorizontalSplitPanel;
import eu.europa.ec.leos.ui.component.AccordionPane;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.event.toc.TocResizedEvent;
import eu.europa.ec.leos.web.event.view.PaneAddEvent;
import eu.europa.ec.leos.web.event.view.PaneEnableEvent;
import eu.europa.ec.leos.web.ui.component.ContentPane;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ScreenLayoutHelper implements AbstractSplitPanel.SplitterClickListener, AbstractSplitPanel.SplitPositionChangeListener, Page.BrowserWindowResizeListener {
    private static final long serialVersionUID = -407128199469215051L;

    private static final Logger LOG = LoggerFactory.getLogger(ScreenLayoutHelper.class);

    private EventBus eventBus;
    private List<HorizontalSplitPanel> splitters;
    private List<PaneSettings> panesList;
    private static final float UNDEFINED = -1F;
    private static final float DEFAULT_TOC_WIDTH = 20.0f;
    private float tocSplitterWidth = UNDEFINED;
    private float contentSplitterWidth = UNDEFINED;
    private float tocWidth = UNDEFINED;

    private static final Class TOC = AccordionPane.class;
    private static final Class CHANGES = ComparisonComponent.class;
    public static final String TOC_SPLITTER = "TOC_SPLITTER";
    public static final String CONTENT_SPLITTER = "CONTENT_SPLITTER";

    public ScreenLayoutHelper(EventBus eventBus, List<HorizontalSplitPanel> splitters) {
        this.eventBus = eventBus;
        this.splitters = splitters == null ? new ArrayList<>() : new ArrayList<>(splitters);
        this.panesList = new ArrayList<>();    // List of panes on the screen, flag enabling them or not
        Page.getCurrent().addBrowserWindowResizeListener(this);
    }

    public void addPane(ContentPane pane, Integer position, Boolean isEnabled) {
        eventBus.post(new PaneAddEvent(pane.getClass()));
        PaneSettings paneSettings = new PaneSettings(pane, position, isEnabled);
        panesList.add(paneSettings);
    }

    public boolean isPaneEnabled(Class componentClass) {
        return panesList
                .stream()
                .filter(PaneSettings::isEnabled)
                .map(PaneSettings::getPane)
                .anyMatch(componentClass::isInstance);
    }

    public boolean isTocPaneEnabled() {
        return isPaneEnabled(TOC);
    }

    public void changePosition(ColumnPosition position, Class componentClass) {
        panesList
                .stream()
                .filter(f -> componentClass.isInstance(f.getPane()))
                .forEach(f -> f.setEnabled(position != ColumnPosition.OFF));
        layoutComponents();
        LOG.debug("position changed to {} ...", position);
    }

    private List<PaneSettings> getSortedEnabledPanes() {
        return panesList
                .stream()
                .filter(PaneSettings::isEnabled)
                .sorted(Comparator.comparing(PaneSettings::getPosition))
                .collect(Collectors.toList());
    }

    public void layoutComponents(){
        for(HorizontalSplitPanel splitPanel:splitters){
            splitPanel.removeAllComponents();
            splitPanel.addSplitterClickListener(this);
            splitPanel.addSplitPositionChangeListener(this);
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
        boolean tocPaneEnabled = isPaneEnabled(TOC);
        //we give priority to the first element - left element
        switch(panes.size()){
            case 2:
                splitterSize = panes.get(0).getPane().getDefaultPaneWidth(2, tocPaneEnabled);
                break;
            case 3:
                if(indexSplitter == 0) { //for AccordionPane
                    splitterSize = panes.get(0).getPane().getDefaultPaneWidth(3, tocPaneEnabled);
                }
                else 
                    splitterSize = 100f - panes.get(2).getPane().getDefaultPaneWidth(3, tocPaneEnabled);
                break;
            default:
                splitterSize=0f;
                break;
        }//end switch
        return splitterSize;
    }

    private float getBrowserWidth() {
        return Page.getCurrent().getBrowserWindowWidth();
    }

    private float getSplitWidth (AbstractSplitPanel.SplitPositionChangeEvent event) {
        if (event.getSplitPositionUnit() == Unit.PERCENTAGE) {
            return event.getSplitPosition();
        } else
            return event.getSplitPosition() / getBrowserWidth() * 100F;
    }

    public void onSplitPositionChanged(AbstractSplitPanel.SplitPositionChangeEvent event) {
        if (event.getSource() instanceof HorizontalSplitPanel) {
            HorizontalSplitPanel panel = (HorizontalSplitPanel) event.getSource();
            String id = panel.getId();
            if (CONTENT_SPLITTER.equals(id)) {
                contentSplitterWidth = getSplitWidth(event);
            } else if (TOC_SPLITTER.equals(id)) {
                tocSplitterWidth = getSplitWidth(event);
            }
        }
        if (tocSplitterWidth != UNDEFINED) {
            if (contentSplitterWidth != UNDEFINED && isPaneEnabled(CHANGES)) {
                tocWidth = tocSplitterWidth * contentSplitterWidth / 100F;
            } else {
                tocWidth = tocSplitterWidth;
            }
            sendPositionChangeEvent();
        }
    }

    public void browserWindowResized(Page.BrowserWindowResizeEvent browserWindowResizeEvent) {
        sendPositionChangeEvent();
    }

    private void sendPositionChangeEvent() {
        if (tocWidth != UNDEFINED && isPaneEnabled(TOC)) {
            eventBus.post(new TocResizedEvent(getBrowserWidth() * tocWidth / 100F));
        }
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
