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
package eu.europa.ec.leos.web.presenter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.PreambleService;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.view.document.EditCitationsRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EditRecitalsRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SaveCitationsRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SaveRecitalsRequestEvent;
import eu.europa.ec.leos.web.event.window.CloseCitationsEditorEvent;
import eu.europa.ec.leos.web.event.window.CloseRecitalsEditorEvent;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.view.DocumentView;

@Component
@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class PreamblePresenter extends AbstractPresenter<DocumentView> {

    @Autowired
    private DocumentView documentView;

    @Autowired
    private PreambleService preambleService;
    
    @Autowired
    private DocumentService documentService;
    
    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private LockHelper lockHelper;
    
    @Autowired
    private SecurityContext securityContext;

    @Override
    public DocumentView getView() {
        return documentView;
    }

    private static final Logger LOG = LoggerFactory.getLogger(PreamblePresenter.class);

    @Subscribe
    public void showCitationsEditor(EditCitationsRequestEvent event) {
        String citationsId = event.getCitationsId();
        
        if (lockHelper.lockElement( citationsId)) {
            LeosDocument document = getDocument();
            String citations = preambleService.getCitations(document, citationsId);
            documentView.showCitationsEditor(citationsId, citations);
        }
        //Do not reject view if lock is not available
    }
 
    @Subscribe
    public void saveCitations(SaveCitationsRequestEvent event) {
        String citationsId = event.getCitationsId();
        if (lockHelper.isElementLockedFor( event.getCitationsId())) {
            LeosDocument document = getDocument();
            document = preambleService.saveCitations(document, event.getCitationsContentt(), citationsId);
            if (document != null) {
                String citationsContent  = preambleService.getCitations(document, event.getCitationsId());
                documentView.refreshCitationsEditor(citationsContent);
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            }
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Subscribe
    public void closeCitationsEditor(CloseCitationsEditorEvent event) {
        String citationsId = event.getCitationsId();
        if (lockHelper.isElementLockedFor( citationsId)) {
            lockHelper.unlockElement( citationsId);
        }
    }
    
    @Subscribe
    public void showRecitalsEditor(EditRecitalsRequestEvent event) {
        String recitalsId = event.getRecitalsId();
        if (lockHelper.lockElement( recitalsId)) {
            LeosDocument document = getDocument();
            String recitals = preambleService.getRecitals(document, recitalsId);
            documentView.showRecitalsEditor(recitalsId, recitals);
        }
    }
 
    @Subscribe
    public void saveRecitals(SaveRecitalsRequestEvent event) {
        String recitalsId = event.getRecitalsId();

        if (lockHelper.isElementLockedFor( recitalsId)) {
            LeosDocument document = getDocument();
            document = preambleService.saveRecitals(document, event.getRecitalsContent(), recitalsId);
            if (document != null) {
                String recitalsContent  = preambleService.getRecitals(document, event.getRecitalsId());
                documentView.refreshRecitalsEditor(recitalsContent);
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            }
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Subscribe
    public void closeRecitalsEditor(CloseRecitalsEditorEvent event) {
        String recitalsId = event.getRecitalsId();
        if (lockHelper.isElementLockedFor( recitalsId)) {
            lockHelper.unlockElement( recitalsId);
        }
    }    
    
    private LeosDocument getDocument() {
        LeosDocument document = null;
        String documentId=getDocumentId();
        if (documentId != null) {
            document = documentService.getDocument(documentId);
        }
        return document;
    }
    
    private String getDocumentId() {
        return (String) session.getAttribute(SessionAttribute.DOCUMENT_ID.name());
    }
}

