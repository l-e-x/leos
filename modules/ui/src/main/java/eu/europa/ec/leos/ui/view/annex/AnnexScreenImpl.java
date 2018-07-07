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
package eu.europa.ec.leos.ui.view.annex;

import com.google.common.eventbus.EventBus;
import com.vaadin.annotations.DesignRoot;
import com.vaadin.annotations.StyleSheet;
import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.ViewScope;
import com.vaadin.ui.*;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.declarative.Design;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.ui.component.LeosDisplayField;
import eu.europa.ec.leos.ui.extension.ActionManagerExtension;
import eu.europa.ec.leos.ui.extension.LeosEditorExtension;
import eu.europa.ec.leos.ui.extension.RefToLinkExtension;
import eu.europa.ec.leos.ui.model.VersionType;
import eu.europa.ec.leos.ui.view.ScreenLayoutHelper;
import eu.europa.ec.leos.web.event.component.ComparisonResponseEvent;
import eu.europa.ec.leos.web.event.component.SplitPositionEvent;
import eu.europa.ec.leos.web.event.view.document.EditElementResponseEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshElementEvent;
import eu.europa.ec.leos.web.event.view.document.ShowMajorVersionWindowEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.LeosCacheToken;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.component.AnnexComponent;
import eu.europa.ec.leos.web.ui.component.MarkedTextComponent;
import eu.europa.ec.leos.web.ui.window.MajorVersionWindow;
import eu.europa.ec.leos.web.ui.window.TimeLineWindow;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@SpringComponent
@ViewScope
@DesignRoot("AnnexScreenDesign.html")
@StyleSheet({"vaadin://../assets/css/annex.css" + LeosCacheToken.TOKEN})
class AnnexScreenImpl extends VerticalLayout implements AnnexScreen {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(AnnexScreenImpl.class);

    private final EventBus eventBus;
    private final UserHelper userHelper;
    private final SecurityContext securityContext;
    private MessageHelper messageHelper;
    // dummy init to avoid design exception
    private ScreenLayoutHelper screenLayoutHelper = new ScreenLayoutHelper(null, null);
    protected HorizontalSplitPanel annexSplit;
    protected HorizontalSplitPanel contentSplit;
    protected Label annexTitle;

    protected MarkedTextComponent<LeosDocument.XmlDocument.Annex> markedTextComponent;
    protected AnnexComponent annexDoc;
    protected LeosDisplayField annexContent;

    protected Button rightSliderButton;
    protected Button timeLineButton;
    protected Button majorVersionButton;
    protected Label versionInfoLabel;

    @Autowired
    AnnexScreenImpl(MessageHelper messageHelper, EventBus eventBus, SecurityContext securityContext, UserHelper userHelper) {
        LOG.trace("Initializing annex screen...");
        Validate.notNull(messageHelper, "MessageHelper must not be null!");
        this.messageHelper = messageHelper;
        Validate.notNull(eventBus, "EventBus must not be null!");
        this.eventBus = eventBus;
        Validate.notNull(securityContext, "SecurityContext must not be null!");
        this.securityContext = securityContext;
        Validate.notNull(userHelper, "UserHelper must not be null!");
        this.userHelper = userHelper;
        Design.read(this);
        init();
    }
    
    @Override
    public void setTitle(String title) {
        annexTitle.setValue((title == null || title.isEmpty())
                ? messageHelper.getMessage("document.annex.title.default")
                : title);
        annexTitle.setWidth("100%");
    }
    
    @Override
    public void setContent(String content) {
        annexContent.setValue(addTimestamp(content));
    }
    
    private void init() {
        annexContent.addStyleName(LeosCategory.ANNEX.name().toLowerCase()); // Adding the document type as style to scope annex styles

        screenLayoutHelper = new ScreenLayoutHelper(eventBus, new ArrayList<>(Arrays.asList(contentSplit, annexSplit)));
        screenLayoutHelper.addPane(annexDoc, 1, true);
        screenLayoutHelper.addPane(markedTextComponent, 0, false);
        screenLayoutHelper.layoutComponents();

        new LeosEditorExtension<>(annexContent, eventBus);
        new ActionManagerExtension<>(annexContent);
        new RefToLinkExtension<>(annexContent);

        rightSliderButton();
        buildTimeLineButton();
        buildMajorVersionButton();
    }
    
    @Override
    public void attach() {
        super.attach();
        eventBus.register(screenLayoutHelper);
    }
    
    @Override
    public void detach() {
        eventBus.unregister(screenLayoutHelper);
        super.detach();
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

    private void rightSliderButton() {
        rightSliderButton.setData(SplitPositionEvent.MoveDirection.RIGHT);
        rightSliderButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                eventBus.post(new SplitPositionEvent((SplitPositionEvent.MoveDirection) event.getButton().getData(), annexDoc));
            }
        });
    }
    
    @Override
    public void populateMarkedContent(String markedContentText) {
        markedTextComponent.addStyleName(LeosCategory.ANNEX.name().toLowerCase());
        markedTextComponent.populateMarkedContent(markedContentText, LeosCategory.ANNEX);
    }
    
    @Override
    public void showElementEditor(final String elementId, final String elementTagName, final String elementFragment) {
        eventBus.post(new EditElementResponseEvent(elementId, elementTagName, elementFragment, LeosCategory.ANNEX.name(), securityContext.getUser(),
                securityContext.hasRole(LeosAuthority.SUPPORT) ? new LeosAuthority[]{LeosAuthority.SUPPORT} : null));
    }
    
    @Override
    public void refreshElementEditor(final String elementId, final String elementTagName, final String elementFragment) {
        eventBus.post(new RefreshElementEvent(elementId, elementTagName, elementFragment));
    }

    @Override
    public void showTimeLineWindow(List documentVersions) {
        TimeLineWindow<LeosDocument.XmlDocument.Annex> timeLineWindow = new TimeLineWindow<LeosDocument.XmlDocument.Annex>(messageHelper, eventBus, userHelper, documentVersions);       
        UI.getCurrent().addWindow(timeLineWindow);
        timeLineWindow.center();
        timeLineWindow.focus();
    }

    @Override
    public void displayComparison(HashMap<Integer, Object> htmlResult){      
        eventBus.post(new ComparisonResponseEvent(htmlResult,LeosCategory.ANNEX.name().toLowerCase()));
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
        this.versionInfoLabel.setValue(messageHelper.getMessage("document.version.caption", versionInfoVO.getDocumentVersion(), versionType, versionInfoVO.getLastModifiedBy(), versionInfoVO.getDg(), versionInfoVO.getLastModificationInstant()));
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
}
