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

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.PreambleService;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.document.EditCitationsRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EditRecitalsRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SaveCitationsRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SaveRecitalsRequestEvent;
import eu.europa.ec.leos.web.event.window.CloseCitationsEditorEvent;
import eu.europa.ec.leos.web.event.window.CloseRecitalsEditorEvent;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.view.DocumentView;

public class PreamblePresenterTest extends LeosPresenterTest {

    private static final String SESSION_ID = "sessionID";

    @Mock
    private DocumentView documentView;

    @Mock
    private DocumentService documentService;

    @Mock
    private PreambleService  preambleService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private LockHelper lockHelper ;

    @InjectMocks
    private PreamblePresenter preamblePresenter ;

    @Before
    public void init() throws Exception {
        when(httpSession.getId()).thenReturn(SESSION_ID);
    }

    @Test
    public void testEditCitations_should_showCitationsEditor() throws Exception {

        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.lockElement( citationsId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.getCitations(document, citationsId)).thenReturn(citationsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.showCitationsEditor((new EditCitationsRequestEvent(citationsId)));

        verify(documentService).getDocument(docId);
        verify(preambleService).getCitations(document, citationsId);
        verify(documentView).showCitationsEditor(citationsId, citationsContent);

        verifyNoMoreInteractions(documentService, documentView);
    }
    @Test
    public void testEditCitations_should_NotshowCitationsEditor_whenCitationsAreLocked() throws Exception {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.lockElement( citationsId)).thenReturn(false);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.getCitations(document, citationsId)).thenReturn(citationsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.showCitationsEditor((new EditCitationsRequestEvent(citationsId)));

        verifyZeroInteractions(documentView);
        verifyNoMoreInteractions(documentService, eventBus);
    }


    @Test
    public void testCloseCitationsEditor() {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";
        when(lockHelper.isElementLockedFor( citationsId)).thenReturn(true);
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);

        // DO THE ACTUAL CALL
        preamblePresenter.closeCitationsEditor(new CloseCitationsEditorEvent("7474"));

        verify(lockHelper).isElementLockedFor( citationsId);
        verify(lockHelper).unlockElement( citationsId);

    }

    @Test
    public void test_saveCitations_should_saveUpdatedDocument() throws Exception {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";
        String newCitationsContent = "<citations id=\"7474\"><citation>testCit</citation><citation>newtestCit</citation></citations>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( citationsId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.saveCitations(document,newCitationsContent,citationsId)).thenReturn(updatedDocument);
        when(preambleService.getCitations(updatedDocument, citationsId)).thenReturn(newCitationsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.saveCitations(new SaveCitationsRequestEvent(citationsId,newCitationsContent));

        verify(preambleService).saveCitations(document, newCitationsContent, citationsId);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
        verify(documentView).refreshCitationsEditor(newCitationsContent);
        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void test_saveCitations_should_displayNotification_whenNoLockWasAvailable() throws Exception {

        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";
        String newCitationsContent = "<citations id=\"7474\"><citation>testCit</citation><citation>newtestCit</citation></citations>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( citationsId)).thenReturn(false);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.getCitations(document, citationsId)).thenReturn(citationsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.saveCitations(new SaveCitationsRequestEvent(citationsId,newCitationsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, preambleService);
    }

    @Test
    public void testSaveCitations_when_NoDocumentId_should_displayNotification() throws Exception {

        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";
        String newCitationsContent = "<citations id=\"7474\"><citation>testCit</citation><citation>newtestCit</citation></citations>";
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(null);

        // DO THE ACTUAL CALL
        preamblePresenter.saveCitations(new SaveCitationsRequestEvent(citationsId, newCitationsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, preambleService);
    }

    @Test
    public void testEditRecitals_should_showRecitalsEditor() throws Exception {

        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);

        String recitalsId = "7474";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.lockElement( recitalsId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.getRecitals(document, recitalsId)).thenReturn(recitalsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.showRecitalsEditor((new EditRecitalsRequestEvent(recitalsId)));

        verify(documentService).getDocument(docId);
        verify(preambleService).getRecitals(document, recitalsId);
        verify(documentView).showRecitalsEditor(recitalsId, recitalsContent);

        verifyNoMoreInteractions(documentService, documentView);
    }
    @Test
    public void testEditRecitals_should_NotshowRecitalsEditor_whenRecitalsAreLocked() throws Exception {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);

        String recitalsId = "7474";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.lockElement( recitalsId)).thenReturn(false);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.getRecitals(document, recitalsId)).thenReturn(recitalsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.showRecitalsEditor((new EditRecitalsRequestEvent(recitalsId)));

        verifyZeroInteractions(documentView);
        verifyNoMoreInteractions(documentService, eventBus);
    }


    @Test
    public void testCloseRecitalsEditor() {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);

        String recitalsId = "7474";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";
        when(lockHelper.isElementLockedFor( recitalsId)).thenReturn(true);
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);

        // DO THE ACTUAL CALL
        preamblePresenter.closeRecitalsEditor(new CloseRecitalsEditorEvent("7474"));

        verify(lockHelper).isElementLockedFor( recitalsId);
        verify(lockHelper).unlockElement( recitalsId);

    }

    @Test
    public void test_saveRecitals_should_saveUpdatedDocument() throws Exception {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String recitalsId = "7474";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";
        String newRecitalsContent = "<recitals id=\"7474\"><recital>testCit</recital><recital>newtestCit</recital></recitals>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( recitalsId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.saveRecitals(document,newRecitalsContent,recitalsId)).thenReturn(updatedDocument);
        when(preambleService.getRecitals(updatedDocument, recitalsId)).thenReturn(newRecitalsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.saveRecitals(new SaveRecitalsRequestEvent(recitalsId,newRecitalsContent));

        verify(preambleService).saveRecitals(document, newRecitalsContent, recitalsId);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
        verify(documentView).refreshRecitalsEditor(newRecitalsContent);
        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void test_saveRecitals_should_displayNotification_whenNoLockWasAvailable() throws Exception {

        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String recitalsId = "7474";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";
        String newRecitalsContent = "<recitals id=\"7474\"><recital>testCit</recital><recital>newtestCit</recital></recitals>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( recitalsId)).thenReturn(false);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(preambleService.getRecitals(document, recitalsId)).thenReturn(recitalsContent);

        // DO THE ACTUAL CALL
        preamblePresenter.saveRecitals(new SaveRecitalsRequestEvent(recitalsId,newRecitalsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, preambleService);
    }

    @Test
    public void testSaveRecitals_when_NoDocumentId_should_displayNotification() throws Exception {

        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String recitalsId = "7474";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";
        String newRecitalsContent = "<recitals id=\"7474\"><recital>testCit</recital><recital>newtestCit</recital></recitals>";
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(null);

        // DO THE ACTUAL CALL
        preamblePresenter.saveRecitals(new SaveRecitalsRequestEvent(recitalsId, newRecitalsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, preambleService);
    }    
}
