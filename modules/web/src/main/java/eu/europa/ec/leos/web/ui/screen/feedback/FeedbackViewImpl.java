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
package eu.europa.ec.leos.web.ui.screen.feedback;

import com.vaadin.annotations.JavaScript;
import com.vaadin.data.Container;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.event.view.feedback.EnterFeedbackViewEvent;
import eu.europa.ec.leos.web.support.LockNotificationManager;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.component.CommentsComponent;
import eu.europa.ec.leos.web.ui.component.LegalTextPaneComponent;
import eu.europa.ec.leos.web.ui.component.SharedLockComponent;
import eu.europa.ec.leos.web.ui.converter.StageIconConverter;
import eu.europa.ec.leos.web.ui.converter.TableOfContentItemConverter;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.view.FeedbackView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import ru.xpoft.vaadin.VaadinView;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Scope("session")
@org.springframework.stereotype.Component(FeedbackView.VIEW_ID)
@VaadinView(FeedbackView.VIEW_ID)
@JavaScript({"http://cdn.mathjax.org/mathjax/2.3-latest/MathJax.js?config=default"})
public class FeedbackViewImpl extends LeosScreen implements FeedbackView {

    private static final long serialVersionUID = 4697888482004181236L;
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackViewImpl.class);

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private ConfigurationHelper cfgHelper;

    private FeedbackViewSettings viewSettings;
    private final Label docTitle = new Label();
    private Label docIcon = new Label("", ContentMode.HTML);
    private SharedLockComponent lockCompoment;
    private LegalTextPaneComponent legalTextPaneComponent;
    private FeedbackMenuComponent menuBarComponent;
    private CommentsComponent commentsComponent;
    
    @PostConstruct
    private void init() {
        LOG.trace("Initializing {} view...", VIEW_ID);
        viewSettings = new FeedbackViewSettings();
        eventBus.register(this);
        // initialize view layout
        initLayout();
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        LOG.debug("Entering {} view...Params:{}", getViewId(), event.getParameters());
        eventBus.post(new EnterFeedbackViewEvent());
    }

    private void initLayout() {
        addStyleName("leos-feedback-layout");

        setSizeFull();
        // create layout for document title
        buildTitleAndMenubar();
        buildDocumentPane();

        //non visual component
        lockCompoment= new SharedLockComponent();
        lockCompoment.setLocks(new ArrayList<LockData>(0));
        addComponent(lockCompoment);
    }

    @Override
    public void attach() {
        super.attach();
        setPollingStatus(true);        // enable polling
    }

    @Override
    public void detach() {
        super.detach();
        setPollingStatus(false);        // disable polling
    }

    private HorizontalLayout buildTitleAndMenubar() {

        HorizontalLayout docLayout = new HorizontalLayout();
        docLayout.addStyleName("leos-docview-header-layout");
        docLayout.setWidth(100, Unit.PERCENTAGE);
        docLayout.setSpacing(true);
        addComponent(docLayout);

        docIcon.setWidth("32px");
        docIcon.addStyleName("leos-docview-icon");
        docLayout.addComponent(docIcon);
        docLayout.setComponentAlignment(docIcon, Alignment.MIDDLE_LEFT);

        docTitle.addStyleName("leos-docview-doctitle");
        docLayout.addComponent(docTitle);
        docLayout.setExpandRatio(docTitle, 1.0f);
        docLayout.setComponentAlignment(docTitle, Alignment.MIDDLE_LEFT);

        // add menu bar component
        menuBarComponent = new FeedbackMenuComponent(messageHelper, eventBus, cfgHelper, viewSettings);
        docLayout.addComponent(menuBarComponent);
        docLayout.setComponentAlignment(menuBarComponent, Alignment.TOP_RIGHT);

        return docLayout;
    }

    private void buildDocumentPane() {
        LOG.debug("Building document pane...");
        
        legalTextPaneComponent = new LegalTextPaneComponent(eventBus, messageHelper, viewSettings);
        addComponent(legalTextPaneComponent);
        setExpandRatio(legalTextPaneComponent, 1.0f);

        commentsComponent = new CommentsComponent(eventBus,messageHelper);
        addComponent(commentsComponent);
    }

    @Override
    public void setDocumentTitle(final String documentTitle) {
        docTitle.setValue(documentTitle);
    }

    @Override
    public void setDocumentStage(final LeosDocumentProperties.Stage value) {
        LeosDocumentProperties.Stage stage = (value == null) ? LeosDocumentProperties.Stage.DRAFT : value;
        docIcon.setValue(new StageIconConverter().convertToPresentation(stage, null, null));
        docIcon.addStyleName(stage.toString().toLowerCase());
    }

    @Override
    public void refreshContent(final String documentContent) {
        legalTextPaneComponent.populateContent(documentContent);
        forceMathJaxRendering();
    }

    @Override
    public void setToc(final List<TableOfContentItemVO> tableOfContentItemVoList) {
        Container tocContainer = TableOfContentItemConverter.buildTocContainer(tableOfContentItemVoList, messageHelper);
        legalTextPaneComponent.setTableOfContent(tocContainer);
    }
    
    @Override
    public @Nonnull
    String getViewId() {
        return VIEW_ID;
    }

    @Override
    public void setData(List<CommentVO> comments) {
        commentsComponent.setData(comments, securityContext.getUser());
    }
    
    private void forceMathJaxRendering() {
        // KLUGE hack to force MathJax rendering of formulas when document content is refreshed
        LOG.trace("Forcing MathJax rendering of formulas...");
        Page.getCurrent().getJavaScript().execute("MathJax.Hub.Queue([\"Typeset\",MathJax.Hub]);");
        Page.getCurrent().getJavaScript().execute("MathJax.Hub.Config({\"HTML-CSS\": {imageFont: null}});");
    }

    public void updateLocks(final LockActionInfo lockActionInfo){
        getUI().access(new Runnable() {
            @Override
            public void run() {
                final List<LockData> lstLocks=lockActionInfo.getCurrentLocks();
                
                lockCompoment.setLocks(lstLocks);                  //1. set updated locks in Javascript world at client end 
                legalTextPaneComponent.updateLocks(lockActionInfo);//2. do update on GUI for buttoms etc 
                showTrayNotfnForLockUpdate(lockActionInfo);        //3. show Notification
            }

        });
    }

    private void showTrayNotfnForLockUpdate(LockActionInfo lockActionInfo){

        if(! VaadinSession.getCurrent().getSession().getId().equals(lockActionInfo.getLock().getSessionId())){
            LockNotificationManager.notifyUser(messageHelper, lockActionInfo);
        }
    }
}

