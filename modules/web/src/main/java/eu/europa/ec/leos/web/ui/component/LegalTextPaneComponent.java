/**
 * Copyright 2015 European Commission
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.data.Container;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;

import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.component.TocPositionEvent;
import eu.europa.ec.leos.web.model.ViewSettings;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.toc.TableOfContentComponent;

public class LegalTextPaneComponent extends CustomComponent {

    private static final long serialVersionUID = 2667950258861202550L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextPaneComponent.class);

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private ViewSettings viewSettings;

    private LegalTextComponent legalTextComponent;
    private TableOfContentComponent tableOfContentComponent;

    public LegalTextPaneComponent(EventBus eventBus, MessageHelper messageHelper, ViewSettings viewSettings) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.viewSettings = viewSettings;

        buildLegalTextPane();
    }

    private void buildLegalTextPane() {
        LOG.debug("Building legal text pane...");

        final VerticalLayout legalTextPane = new VerticalLayout();
        setCompositionRoot(legalTextPane);
        setSizeFull();
        legalTextPane.setSizeFull();

        final HorizontalSplitPanel legalTextSplitter = new HorizontalSplitPanel();
        legalTextSplitter.setSizeFull();

        // set splitter position
        float defaultPosition = viewSettings.getDefaultSplitterPosition();
        legalTextSplitter.setSplitPosition(defaultPosition, Unit.PERCENTAGE);

        // initialize the child components
        legalTextComponent = new LegalTextComponent(eventBus,messageHelper, viewSettings );
        legalTextSplitter.setFirstComponent(legalTextComponent);

        tableOfContentComponent = new TableOfContentComponent(messageHelper, eventBus, viewSettings);
        legalTextSplitter.setSecondComponent(tableOfContentComponent);

        legalTextPane.addComponent(legalTextSplitter);
        legalTextPane.setExpandRatio(legalTextSplitter, 1.0f);

        // Toc position change event handled from the menu item.
        Object tocPositionChanger = new Object() {
            @Subscribe
            public void changeTocPosition(TocPositionEvent event) {

                legalTextSplitter.removeAllComponents();

                legalTextSplitter.setLocked(false);
                legalTextSplitter.setSplitPosition(viewSettings.getDefaultSplitterPosition(), Unit.PERCENTAGE);

                switch (event.getTocPosition()) {
                    case OFF:
                        legalTextSplitter.addComponent(legalTextComponent);

                        // since we have only one component, lock the splitter and expand it to maximum
                        legalTextSplitter.setLocked(true);
                        legalTextSplitter.setSplitPosition(viewSettings.getMaxSplitterPosition(), Unit.PERCENTAGE);
                        break;
                    case LEFT:
                        legalTextSplitter.setFirstComponent(tableOfContentComponent);
                        legalTextSplitter.setSecondComponent(legalTextComponent);
                        break;
                    case RIGHT:
                        legalTextSplitter.setFirstComponent(legalTextComponent);
                        legalTextSplitter.setSecondComponent(tableOfContentComponent);
                        break;
                }
            }

        };
        eventBus.register(tocPositionChanger);

        // Split position change event handler
        Object splitPositionChanger = new Object() {
            @Subscribe
            public void moveSplitterPosition(SplitPositionEvent event) {
                LOG.debug("Split position event handled (move direction={} position={}{})...", event.getMoveDirection(),
                        legalTextSplitter.getSplitPosition(), legalTextSplitter.getSplitPositionUnit());
                SplitPositionEvent.MoveDirection direction = event.getMoveDirection();
                float newPosition;
                if (direction.equals(SplitPositionEvent.MoveDirection.RIGHT)) {
                    newPosition = (legalTextSplitter.getSplitPosition() < viewSettings.getDefaultSplitterPosition()) ? viewSettings
                            .getDefaultSplitterPosition() : viewSettings.getMaxSplitterPosition();
                } else {
                    newPosition = (legalTextSplitter.getSplitPosition() > viewSettings.getDefaultSplitterPosition()) ? viewSettings
                            .getDefaultSplitterPosition() : 0.0f;

                }
                LOG.debug("Update position to {}{})...", newPosition, legalTextSplitter.getSplitPositionUnit());

                legalTextSplitter.setSplitPosition(newPosition, Unit.PERCENTAGE);
            }
        };
        eventBus.register(splitPositionChanger);
    }
    
    public void populateContent(final String docContent) {
        legalTextComponent.populateContent(docContent);
    }

    public void setTableOfContent(final Container tocContainer) {
        tableOfContentComponent.setTableOfContent(tocContainer);
    }
    
    public void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL){
        legalTextComponent.setDocumentPreviewURLs(documentId, pdfURL, htmlURL);
    }
    
    public void updateLocks(LockActionInfo lockActionInfo){
        legalTextComponent.updateLocks(lockActionInfo);
        tableOfContentComponent.updateLocks(lockActionInfo);
    }
}
