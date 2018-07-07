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
package eu.europa.ec.leos.web.ui.component;

import com.google.common.eventbus.EventBus;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.v7.data.Container;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.ui.view.ScreenLayoutHelper;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.toc.TableOfContentComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

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
    private ScreenLayoutHelper screenLayoutHelper;
    private UserHelper userHelper;

    private LegalTextComponent legalTextComponent;
    private TableOfContentComponent tableOfContentComponent;
    private MarkedTextComponent<LeosDocument.XmlDocument.Bill> markedTextComponent;

    private HorizontalSplitPanel tocSplitter = new HorizontalSplitPanel();
    private HorizontalSplitPanel contentSplitter = new HorizontalSplitPanel();

    public LegalTextPaneComponent(EventBus eventBus, MessageHelper messageHelper, UserHelper userHelper) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.userHelper = userHelper;
        this.screenLayoutHelper = new ScreenLayoutHelper(eventBus, new ArrayList<>(Arrays.asList(contentSplitter, tocSplitter)));

        buildLegalTextPane();
    }

    @Override public void attach() {
        super.attach();
        eventBus.register(this);
        eventBus.register(screenLayoutHelper);
    }

    @Override public void detach() {
        eventBus.unregister(this);
        eventBus.unregister(screenLayoutHelper);
        super.detach();
    }

    private void buildLegalTextPane() {
        LOG.debug("Building legal text pane...");

        final VerticalLayout legalTextPane = new VerticalLayout();
        legalTextPane.setSpacing(false);
        legalTextPane.setMargin(false);
        setCompositionRoot(legalTextPane);
        setSizeFull();
        legalTextPane.setSizeFull();

        createChildComponents();

        legalTextPane.addComponent(tocSplitter);
        legalTextPane.setExpandRatio(tocSplitter, 1.0f);
    }

    public void populateContent(final String docContent) {
        legalTextComponent.populateContent(docContent);
    }

    public void populateMarkedContent(final String markedContent) {
        if (componentEnabled(MarkedTextComponent.class)) {
            markedTextComponent.populateMarkedContent(markedContent, LeosCategory.BILL);
        }
    }

    public void setTableOfContent(final Container tocContainer) {
        if(componentEnabled(TableOfContentComponent.class)) {
            tableOfContentComponent.setTableOfContent(tocContainer);
        }
    }

    public void setDocumentVersionInfo(VersionInfoVO versionInfoVO) {
        legalTextComponent.setDocumentVersionInfo(versionInfoVO);
    }
    
    private boolean componentEnabled(Class className){
        return screenLayoutHelper.isPaneEnabled(className);
    }

    private void createChildComponents(){
        // initialize the child components
        legalTextComponent = new LegalTextComponent(eventBus, messageHelper);
        screenLayoutHelper.addPane(legalTextComponent, 1, true);

        tableOfContentComponent = new TableOfContentComponent(messageHelper, eventBus, true);
        screenLayoutHelper.addPane(tableOfContentComponent, 2, true);

        markedTextComponent = new MarkedTextComponent<>(eventBus, messageHelper, userHelper);
        screenLayoutHelper.addPane(markedTextComponent, 0, false);
        screenLayoutHelper.layoutComponents();
    }
}