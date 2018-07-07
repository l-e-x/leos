/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
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
import com.vaadin.data.Container;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.web.ui.screen.ViewSettings;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/*------------------------------------------------------------------------

View

---------------------------------------------------------------------------------------
| MarkedTextComponent       ||   LegalTextComponent      ||  TableOfContentComponent  |
| Optional                  ||   mandatory               ||  Mandatory                |
|                           ||                           ||                           |
|                           ||                           ||                           |
|        contentSplitter--> ||             tocSplitter-->||                           |
---------------------------------------------------------------------------------------

ComponentArrangement in horizontal split panel
[ [MarkedTextComponent|LegalTextComponent] | TableOfContentComponent]
-------------------------------------------------------------------------*/
public class LegalTextPaneComponent extends CustomComponent {

    private static final long serialVersionUID = 2667950258861202550L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextPaneComponent.class);

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private ViewSettings viewSettings;

    private LegalTextComponent legalTextComponent;
    private TableOfContentComponent tableOfContentComponent;
    private MarkedTextComponent markedTextComponent;

    HashMap<Class,CustomComponent> componentMap= new HashMap<>();
    final HorizontalSplitPanel tocSplitter = new HorizontalSplitPanel();
    final HorizontalSplitPanel contentSplitter = new HorizontalSplitPanel();

    public LegalTextPaneComponent(EventBus eventBus, MessageHelper messageHelper, ViewSettings viewSettings) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.viewSettings = viewSettings;

        buildLegalTextPane();
    }

    @Override public void attach() {
        super.attach();
        eventBus.register(this);
    }

    @Override public void detach() {
        eventBus.unregister(this);
        super.detach();
    }

    private void buildLegalTextPane() {
        LOG.debug("Building legal text pane...");

        final VerticalLayout legalTextPane = new VerticalLayout();
        setCompositionRoot(legalTextPane);
        setSizeFull();
        legalTextPane.setSizeFull();

        createChildComponents();

        layoutComponents();

        legalTextPane.addComponent(tocSplitter);
        legalTextPane.setExpandRatio(tocSplitter, 1.0f);
    }

    @Subscribe
    public void changePosition(LayoutChangeRequestEvent event) {
        ColumnPosition position= event.getPosition();
        viewSettings.setComponentColumnPosition(position,event.getOriginatingComponent());
        layoutComponents();
        LOG.debug("position changed to {} ...", position);
    }

    @Subscribe
    public void moveSplitterPosition(SplitPositionEvent event) {
        LOG.debug("Split position event handled (move direction={} position={}{})...", event.getMoveDirection(),
                tocSplitter.getSplitPosition(), tocSplitter.getSplitPositionUnit());
        SplitPositionEvent.MoveDirection direction = event.getMoveDirection();
        Component originatingComponent = event.getOriginatingComponent();

        //FIXME this code is very very bad. Cleanup required.
        HorizontalSplitPanel parentSplitter = (HorizontalSplitPanel) originatingComponent.getParent();
        float splitterPosition =0f;
        if(originatingComponent instanceof MarkedTextComponent){
            splitterPosition=(parentSplitter.getSplitPosition() > 50f)
                    ? 50f
                    : 0.0f;
            parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
        }
        else if(originatingComponent instanceof LegalTextComponent){
            if (direction.equals(SplitPositionEvent.MoveDirection.RIGHT) && getLeftComponent(originatingComponent) != null) {
                splitterPosition =viewSettings.getDefaultSplitterPosition(0);
                parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
            } else if (direction.equals(SplitPositionEvent.MoveDirection.LEFT ) && getRightComponent(originatingComponent) != null) {
                splitterPosition = viewSettings.getDefaultSplitterPosition(1);
                tocSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
            }
        }
        else if(originatingComponent instanceof TableOfContentComponent){
            if (direction.equals(SplitPositionEvent.MoveDirection.RIGHT)) {
                splitterPosition =(parentSplitter.getSplitPosition() < viewSettings.getDefaultSplitterPosition(1))
                        ? viewSettings.getDefaultSplitterPosition(1)
                        : 100.0f;
            }
            parentSplitter.setSplitPosition(splitterPosition, Unit.PERCENTAGE);
        }
        LOG.debug("Update position to {}{})...", splitterPosition, tocSplitter.getSplitPositionUnit());

    }

    private Component getLeftComponent(Component originatingComponent){
        int index = viewSettings.getViewComponents().indexOf(originatingComponent.getClass());
        if (index -1 < 0){
            return null;
        }
        else{
            return componentMap.get(viewSettings.getViewComponents().get(index-1));
        }
    }

    private Component getRightComponent(Component originatingComponent){
        int index = viewSettings.getViewComponents().indexOf(originatingComponent.getClass());
        if (index < 0 ||  viewSettings.getViewComponents().size() <= index+1 ){
            return null;
        }
        else{
            return componentMap.get(viewSettings.getViewComponents().get(index+1));
        }
    }

    private void layoutComponents(){
        List<HorizontalSplitPanel> splitters= new ArrayList<>(Arrays.asList(contentSplitter, tocSplitter));
        for(HorizontalSplitPanel splitPanel:splitters){
            splitPanel.removeAllComponents();
        }
        int splitterIndex = splitters.size() - 1;
        int componentIndex = viewSettings.getViewComponents().size() - 1;
        List<Class> componentClasses=viewSettings.getViewComponents();

        do {
            HorizontalSplitPanel currentSplitPanel = splitters.get(splitterIndex);
            currentSplitPanel.setSecondComponent(componentMap.get(componentClasses.get(componentIndex--)));

            if (componentIndex >=1 ) {//remaining component 2 or more
                currentSplitPanel.setFirstComponent(splitters.get(splitterIndex - 1));
            }
            else if (componentIndex == 0) {//only one remaining component
                currentSplitPanel.setFirstComponent(componentMap.get(componentClasses.get(componentIndex--)));
            }

            currentSplitPanel.setSplitPosition(viewSettings.getDefaultSplitterPosition(splitterIndex), Unit.PERCENTAGE);
            splitterIndex--;
        } while(componentIndex > 0 );
    }


    public void populateContent(final String docContent) {
        legalTextComponent.populateContent(docContent);
    }

    public void populateMarkedContent(final String markedContent) {
        if (componentEnabled(MarkedTextComponent.class)) {
            markedTextComponent.populateMarkedContent(markedContent);
        }
    }

    public void setTableOfContent(final Container tocContainer) {
        if(componentEnabled(TableOfContentComponent.class)) {
            tableOfContentComponent.setTableOfContent(tocContainer);
        }
    }

    public void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL){
        legalTextComponent.setDocumentPreviewURLs(documentId, pdfURL, htmlURL);
    }

    public void updateLocks(LockActionInfo lockActionInfo){
        legalTextComponent.updateLocks(lockActionInfo);
        if(componentEnabled(TableOfContentComponent.class)) {
            tableOfContentComponent.updateLocks(lockActionInfo);
        }
    }

    private boolean componentEnabled(Class className){
        return viewSettings.getViewComponents().contains(className) && componentMap.containsKey(className);
    }

    private void createChildComponents(){
        // initialize the child components
        legalTextComponent = new LegalTextComponent(eventBus, messageHelper, viewSettings);
        componentMap.put(LegalTextComponent.class, legalTextComponent);

        tableOfContentComponent = new TableOfContentComponent(messageHelper, eventBus, viewSettings);
        componentMap.put(TableOfContentComponent.class, tableOfContentComponent);

        markedTextComponent = new MarkedTextComponent(eventBus, messageHelper, viewSettings);
        componentMap.put(MarkedTextComponent.class, markedTextComponent);
    }
}
