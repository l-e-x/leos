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
package eu.europa.ec.leos.web.ui.screen.repository;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import ru.xpoft.vaadin.VaadinView;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.Page;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.view.repository.DocumentCreateWizardRequestEvent;
import eu.europa.ec.leos.web.event.view.repository.EnterRepositoryViewEvent;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.screen.LeosScreen;
import eu.europa.ec.leos.web.ui.themes.LeosTheme;
import eu.europa.ec.leos.web.ui.wizard.document.CreateDocumentWizard;
import eu.europa.ec.leos.web.view.RepositoryView;

@Scope("session")
@Component(RepositoryView.VIEW_ID)
@VaadinView(RepositoryView.VIEW_ID)
public class RepositoryViewImpl extends LeosScreen implements RepositoryView {

    private static final long serialVersionUID = -5293223956971397143L;
    private static final Logger LOG = LoggerFactory.getLogger(RepositoryViewImpl.class);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private VerticalLayout buttonContainerLayout;

    @Autowired
    private MessageHelper messageHelper;

    @PostConstruct
    private void init() {
        LOG.trace("Initializing {} view...", VIEW_ID);

        setSizeFull();
        setMargin(true);
        addStyleName("leos-repository");

        addComponent(createDocumentCreateButtton());
        addComponent(createListContainer());
        CustomComponent component = new CustomComponent();
        addComponent(component);
        setExpandRatio(component, 1f);
    }

    private Button createDocumentCreateButtton() {
        Button button = new Button(messageHelper.getMessage("repository.create.document"));
        button.setDescription(messageHelper.getMessage("repository.create.document.tooltip"));
        button.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent clickEvent) {
                eventBus.post(new DocumentCreateWizardRequestEvent());
            }
        });
        return button;
    }

    @Override
    public void showCreateDocumentWizard(List<CatalogItem> templates) {
        LOG.debug("Showing the create document wizard...");

        CreateDocumentWizard createDocumentWizard = new CreateDocumentWizard(templates, messageHelper, eventBus);

        UI.getCurrent().addWindow(createDocumentWizard);
        createDocumentWizard.center();
        createDocumentWizard.focus();
    }

    private Panel createListContainer() {
        Panel panel = new Panel();
        buttonContainerLayout = new VerticalLayout();
        buttonContainerLayout.setSizeFull();
        panel.setContent(buttonContainerLayout);
        return panel;
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
        LOG.debug("Entering {} view...", getViewId());
        eventBus.post(new EnterRepositoryViewEvent());
    }

    @Override
    public void setSampleDocuments(final List<DocumentVO> documents) {
        buttonContainerLayout.removeAllComponents();
        for (DocumentVO doc : documents) {
            buttonContainerLayout.addComponent(createDocumentLine(doc));
        }
    }

    private void applyLockInfo(DocumentVO documentVO, HorizontalLayout horizontalDocumentLine) {

        Iterator<com.vaadin.ui.Component> buttonLayout=	horizontalDocumentLine.iterator();
        Button buttonDoc = ((Button) buttonLayout.next());
        Button buttonInfo = ((Button) buttonLayout.next());

        String  lockMsg= documentVO.getMsgForUser();
        horizontalDocumentLine.setDescription(lockMsg);
        
        if(!StringUtils.isBlank(lockMsg)){
            buttonInfo.setVisible(true);
        }
        else{
            buttonInfo.setVisible(false);
        }

        if (documentVO.getLockState().equals(DocumentVO.LockState.LOCKED)) {
            buttonDoc.setIcon(LeosTheme.LEOS_DOCUMENT_LOCKED_ICON_32);
            buttonDoc.setEnabled(false);
        } else {
            buttonDoc.setIcon(LeosTheme.LEOS_DOCUMENT_ICON_32);
            buttonDoc.setEnabled(true);
        }
    }

    private com.vaadin.ui.Component createDocumentLine(DocumentVO documentVO) {

        LeosObjectProperties doc = documentVO.getLeosObjectProperties();
        HorizontalLayout horizontalDocumentLine = new HorizontalLayout();
        horizontalDocumentLine.setData(doc.getLeosId());
        horizontalDocumentLine.setSpacing(true);
        
        Button docButton=createDocumentButton(documentVO); 
        horizontalDocumentLine.addComponent(docButton);
        horizontalDocumentLine.setComponentAlignment(docButton,Alignment.MIDDLE_LEFT);
        
        Button infoButton=createInfoButton(documentVO); 
        horizontalDocumentLine.addComponent(infoButton);
        horizontalDocumentLine.setComponentAlignment(infoButton,Alignment.MIDDLE_RIGHT);
        
        applyLockInfo(documentVO, horizontalDocumentLine);

        return horizontalDocumentLine;
    }

    private Button createDocumentButton(DocumentVO documentVO) {
        LeosObjectProperties doc = documentVO.getLeosObjectProperties();
        Button docButton = new Button(doc.getName());
        docButton.setData(doc.getLeosId());
        docButton.setStyleName("link");
        docButton.setIcon(LeosTheme.LEOS_DOCUMENT_ICON_32);
        
        docButton.addClickListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                SelectDocumentEvent selectDocumentEvent = new SelectDocumentEvent((String) event.getButton().getData());
                eventBus.post(selectDocumentEvent);
            }
        });
        return docButton;
    }
    private Button createInfoButton(DocumentVO documentVO) {
        
        LeosObjectProperties doc = documentVO.getLeosObjectProperties();
        String  lockMsg= documentVO.getMsgForUser();
        
        Button infoButton = new Button();
        infoButton.setData(doc.getLeosId());
        infoButton.setIcon(LeosTheme.LEOS_INFO_WHITE_16);
        infoButton.setVisible(false);
        infoButton.setStyleName("link");
        infoButton.setDescription(lockMsg);//tooltip
        
        return infoButton;
    }
    
    @Override
    public @Nonnull
    String getViewId() {
        return VIEW_ID;
    }

    @Override
    public void updateLockInfo(final DocumentVO udpatedDocVO) {
        getUI().access(new Runnable() {
            @Override
            public void run() {
                Iterator<com.vaadin.ui.Component> iterate = buttonContainerLayout.iterator();
                while (iterate.hasNext()) {
                    HorizontalLayout c = (HorizontalLayout) iterate.next();
                    String buttonLineDocumentId = (String) c.getData();
                    if (buttonLineDocumentId.equals(udpatedDocVO.getDocumentId())) {
                        applyLockInfo(udpatedDocVO,(HorizontalLayout) c);
                    }
                }
            }
        });
    }

    public void showDisclaimer() {
        String caption = messageHelper.getMessage("prototype.disclaimer.caption");
        String description = messageHelper.getMessage("prototype.disclaimer.description");
        Notification disclaimer = new Notification(caption);
        disclaimer.setDelayMsec(Notification.DELAY_FOREVER);
        disclaimer.setHtmlContentAllowed(true);
        disclaimer.setDescription(description);
        disclaimer.setStyleName("leos-disclaimer");
        disclaimer.show(Page.getCurrent());
    }

    @Override
    public void attach() {
        super.attach();
        // enable polling
        setPollingStatus(true);
    }

    @Override
    public void detach() {
        super.detach();
        // disable polling
        setPollingStatus(false);
    }
}
