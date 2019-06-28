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
import com.vaadin.data.TreeData;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.VerticalLayout;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.ui.extension.UserCoEditionExtension;
import eu.europa.ec.leos.ui.view.ScreenLayoutHelper;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.i18n.MessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*------------------------------------------------------------------------

View

---------------------------------------------------------------------------------------
| TableOfContentComponent   ||   LegalTextComponent      ||  MarkedTextComponent      ||
| Mandatory                 ||   mandatory               ||  Optional                 ||
|                           ||                           ||                           ||
|                           ||                           ||                           ||
|            tocSplitter--> ||         contentSplitter-->||                           ||
---------------------------------------------------------------------------------------

ComponentArrangement in horizontal split panel
[ [TableOfContentComponent|LegalTextComponent] | MarkedTextComponent]
-------------------------------------------------------------------------*/
public class LegalTextPaneComponent extends CustomComponent {

    private static final long serialVersionUID = 2667950258861202550L;
    private static final Logger LOG = LoggerFactory.getLogger(LegalTextPaneComponent.class);

    private EventBus eventBus;
    private MessageHelper messageHelper;
    private ConfigurationHelper cfgHelper;
    private SecurityContext securityContext;

    private LegalTextComponent legalTextComponent;
    private TableOfContentComponent tableOfContentComponent;
    
    
    public ScreenLayoutHelper screenLayoutHelper;

    private HorizontalSplitPanel tocSplitter = new HorizontalSplitPanel();
    private HorizontalSplitPanel contentSplitter = new HorizontalSplitPanel();

    public LegalTextPaneComponent(EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper, SecurityContext securityContext) {
        this.eventBus = eventBus;
        this.messageHelper = messageHelper;
        this.cfgHelper = cfgHelper;
        this.screenLayoutHelper = new ScreenLayoutHelper(eventBus, new ArrayList<>(Arrays.asList(contentSplitter, tocSplitter)));
        this.securityContext = securityContext;
        
        buildLegalTextPane();
    }

    @Override
    public void attach() {
        super.attach();
        eventBus.register(this);
        eventBus.register(screenLayoutHelper);
    }

    @Override
    public void detach() {
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

    public LeosDisplayField getContent() {
        return legalTextComponent.getContent();
    }

    public void setTableOfContent(final TreeData<TableOfContentItemVO> treeTocData) {
        if(componentEnabled(TableOfContentComponent.class)) {
            tableOfContentComponent.setTableOfContent(treeTocData);
        }
    }

    public void setDocumentVersionInfo(VersionInfoVO versionInfoVO) {
        legalTextComponent.setDocumentVersionInfo(versionInfoVO);
    }
    
    private boolean componentEnabled(Class className){
        return screenLayoutHelper.isPaneEnabled(className);
    }

    private void createChildComponents() {
        // initialize the child components
        legalTextComponent = new LegalTextComponent(eventBus, messageHelper, cfgHelper, securityContext);
        screenLayoutHelper.addPane(legalTextComponent, 1, true);

        tableOfContentComponent = new TableOfContentComponent(messageHelper, eventBus, securityContext, cfgHelper);
        screenLayoutHelper.addPane(tableOfContentComponent, 0, true);
        screenLayoutHelper.layoutComponents();
    }

    public UserCoEditionExtension getUserCoEditionExtension() {
        return legalTextComponent.getUserCoEditionExtension();
    }

    public void setPermissions(DocumentVO bill, String instanceType){
        boolean enableUpdate = securityContext.hasPermission(bill, LeosPermission.CAN_UPDATE);
        tableOfContentComponent.setPermissions(enableUpdate);
        legalTextComponent.setPermissions(enableUpdate, instanceType);
    }
    
    public void updateTocUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId) {
        if (componentEnabled(TableOfContentComponent.class)) {
            tableOfContentComponent.updateUserCoEditionInfo(coEditionVos, presenterId);
        }
    }
    
    public void displayDocumentUpdatedByCoEditorWarning() {
        legalTextComponent.displayDocumentUpdatedByCoEditorWarning();
    }
}