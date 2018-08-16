/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.ui.view.memorandum;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.data.TreeData;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.common.InstanceContext;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.component.toc.TableOfContentComponent;
import eu.europa.ec.leos.ui.component.toc.TableOfContentItemConverter;
import eu.europa.ec.leos.ui.event.StateChangeEvent;
import eu.europa.ec.leos.ui.event.security.SecurityTokenRequest;
import eu.europa.ec.leos.ui.event.security.SecurityTokenResponse;
import eu.europa.ec.leos.ui.extension.*;
import eu.europa.ec.leos.ui.model.VersionType;
import eu.europa.ec.leos.ui.view.ScreenLayoutHelper;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.component.ComparisonResponseEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.MarkedTextComponent;
import eu.europa.ec.leos.web.ui.component.MemorandumComponent;
import eu.europa.ec.leos.web.ui.window.MajorVersionWindow;
import eu.europa.ec.leos.web.ui.window.TimeLineWindow;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@DesignRoot("MemorandumScreenDesign.html")
@StyleSheet({"vaadin://../assets/css/memorandum.css" + LeosCacheToken.TOKEN})
abstract class MemorandumScreenImpl extends VerticalLayout implements MemorandumScreen {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(MemorandumScreenImpl.class);

    protected EventBus eventBus;
    protected UserHelper userHelper;
    protected SecurityContext securityContext;
    protected MessageHelper messageHelper;
    protected ConfigurationHelper cfgHelper;
    protected InstanceContext instanceContext;
    
    //dummy init to avoid design exception
    private ScreenLayoutHelper screenLayoutHelper = new ScreenLayoutHelper(null, null) ;

    protected HorizontalSplitPanel memorandumSplit;
    protected HorizontalSplitPanel contentSplit;
    protected Label memorandumTitle;

    protected MarkedTextComponent<LeosDocument.XmlDocument.Memorandum> markedTextComponent;
    protected TableOfContentComponent memorandumToc;
    protected MemorandumComponent memorandumDoc;
    protected LeosDisplayField memorandumContent;

    protected Button refreshButton;
    protected Button userGuidanceButton;
    protected Button timeLineButton;
    protected Button majorVersionButton;
    protected Label versionInfoLabel;

    MemorandumScreenImpl(SecurityContext securityContext, EventBus eventBus, MessageHelper messageHelper, ConfigurationHelper cfgHelper,
            UserHelper userHelper, InstanceContext instanceContext) {
        LOG.trace("Initializing memorandum screen...");
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(userHelper, "UserHelper must not be null!");
        this.userHelper = userHelper;
        Validate.notNull(cfgHelper, "Configuration helper must not be null!");
        this.cfgHelper = cfgHelper;
        Validate.notNull(instanceContext, "InstanceContext must not be null!");
        this.instanceContext = instanceContext;
        Design.read(this);
        init();
    }

    @Override
    public void setTitle(String title) {
        memorandumTitle.setValue(title);
    }

    @Override
    public void setContent(String content) {
        memorandumContent.setValue(addTimestamp(content));
    }

    private void init() {

        screenLayoutHelper = new ScreenLayoutHelper(eventBus, new ArrayList<>(Arrays.asList(contentSplit, memorandumSplit)));
        screenLayoutHelper.addPane(memorandumDoc, 1, true);
        screenLayoutHelper.addPane(memorandumToc, 0, true);
        screenLayoutHelper.addPane(markedTextComponent, 2, false);
        screenLayoutHelper.layoutComponents();

        new UserGuidanceExtension<>(memorandumContent, eventBus);
        new RefToLinkExtension<>(memorandumContent);
        new AnnotateExtension<>(memorandumContent, eventBus, cfgHelper);

        userGuidanceButton();
        refreshButton();
        buildTimeLineButton();
        buildMajorVersionButton();
    }

    @Override
    public void attach() {
        eventBus.register(this);
        eventBus.register(screenLayoutHelper);
        super.attach();
    }

    @Override
    public void detach() {
        super.detach();
        eventBus.unregister(screenLayoutHelper);
        eventBus.unregister(this);
    }

    @Override
    public void setUserGuidance(String userGuidance) {
        eventBus.post(new FetchUserGuidanceResponse(userGuidance));
    }

    private void userGuidanceButton() {
        userGuidanceButton.setData(false); // user Guidance is true in start
        userGuidanceButton.setDescription(messageHelper.getMessage("leos.button.tooltip.show.guidance"));

        userGuidanceButton.addClickListener(event -> {
            Button button = event.getButton();
            boolean targetState = !(boolean) button.getData();
            eventBus.post(new UserGuidanceRequest(targetState));
            button.setData(targetState);
            button.setIcon(targetState
                            ? VaadinIcons.QUESTION_CIRCLE       //enabled
                            : VaadinIcons.QUESTION_CIRCLE_O);   //disabled
            button.setDescription(targetState
                    ? messageHelper.getMessage("leos.button.tooltip.hide.guidance")       //enabled
                    : messageHelper.getMessage("leos.button.tooltip.show.guidance"));   //disabled
        });
    }
    
    private void buildTimeLineButton() {        
        timeLineButton.setDescription(messageHelper.getMessage("document.tab.legal.button.timeline.tooltip"));
        timeLineButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent clickEvent) {
                LOG.debug("Time line Button clicked...");
                eventBus.post(new ShowTimeLineWindowEvent());
            }
        });
    }

    private void buildMajorVersionButton() {
        majorVersionButton.setDescription(messageHelper.getMessage("document.major.version.button.tooltip"));
        majorVersionButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent clickEvent) {
                LOG.debug("Major version Button clicked...");
                eventBus.post(new ShowMajorVersionWindowEvent());
            }
        });
    }

    // create text refresh button
    private void refreshButton() {
        refreshButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 3714441703159576377L;

            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
            }
        });
    }

    @Override
    public void setToc(final List<TableOfContentItemVO> tableOfContentItemVoList) {
    	TreeData<TableOfContentItemVO> treeTocData= TableOfContentItemConverter.buildTocData(tableOfContentItemVoList);
        memorandumToc.setTableOfContent(treeTocData);
    }

    @Override
    public void populateMarkedContent(String markedContentText) {
        markedTextComponent.populateMarkedContent(markedContentText, LeosCategory.MEMORANDUM);
    }

    @Override
    public void showElementEditor(final String elementId, final String  elementTagName, final String elementFragment) {
        eventBus.post(new EditElementResponseEvent(elementId, elementTagName, elementFragment, LeosCategory.MEMORANDUM.name(), securityContext.getUser(),
                securityContext.hasRole(LeosAuthority.SUPPORT) ? new LeosAuthority[]{LeosAuthority.SUPPORT} : null, instanceContext.getType().getValue()));
    }

    @Override
    public void refreshElementEditor(final String elementId, final String  elementTagName, final String elementFragment) {
        eventBus.post(new RefreshElementEvent(elementId, elementTagName, elementFragment));
    }

    @Override
    public void showTimeLineWindow(List documentVersions) {
        TimeLineWindow<LeosDocument.XmlDocument.Memorandum> timeLineWindow = new TimeLineWindow<LeosDocument.XmlDocument.Memorandum>(messageHelper, eventBus, userHelper, documentVersions);       
        UI.getCurrent().addWindow(timeLineWindow);
        timeLineWindow.center();
        timeLineWindow.focus();
    }

    @Override
    public void displayComparison(HashMap<Integer, Object> htmlResult){      
        eventBus.post(new ComparisonResponseEvent(htmlResult,LeosCategory.MEMORANDUM.name().toLowerCase()));
    }

    @Override
    public void showMajorVersionWindow() {
        MajorVersionWindow majorVersionWindow = new MajorVersionWindow(messageHelper, eventBus);
        UI.getCurrent().addWindow(majorVersionWindow);
        majorVersionWindow.center();
        majorVersionWindow.focus();
    }
    
    @Override
    public void setDocumentVersionInfo(VersionInfoVO versionInfoVO) {
        String versionType = versionInfoVO.isMajor() ? VersionType.MAJOR.getVersionType() : 
                                                       VersionType.MINOR.getVersionType();
        this.versionInfoLabel.setValue(messageHelper.getMessage("document.version.caption", versionInfoVO.getDocumentVersion(), versionType, versionInfoVO.getLastModifiedBy(), versionInfoVO.getEntity(), versionInfoVO.getLastModificationInstant()));
    }
    
    @Subscribe
    public void handleElementState(StateChangeEvent event) {
        if(event.getState() != null) {
            majorVersionButton.setEnabled(event.getState().isState());
            refreshButton.setEnabled(event.getState().isState());
        }
    }

    private String addTimestamp(String docContentText) {
        /* KLUGE: In order to force the update of the docContent on the client side
         * the unique seed is added on every docContent update, please note markDirty
         * method did not work, this was the only solution worked.*/
        String seed = "<div style='display:none' >" +
                new Date().getTime() +
                "</div>";
        return docContentText + seed;
    }

    @Subscribe
    public void fetchToken(SecurityTokenRequest event){
        eventBus.post(new SecurityTokenResponse(securityContext.getToken(event.getUrl())));
    }

    @Override
    public void scrollToMarkedChange(String elementId) {
        markedTextComponent.scrollToMarkedChange(elementId);
    }
}
