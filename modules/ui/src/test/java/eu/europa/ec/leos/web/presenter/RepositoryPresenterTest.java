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
package eu.europa.ec.leos.web.presenter;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties.Stage;
import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.model.content.LeosTypeId;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.WorkspaceService;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.services.user.PermissionService;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockActionInfo.Operation;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.view.repository.EnterRepositoryViewEvent;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.FeedbackView;
import eu.europa.ec.leos.web.view.RepositoryView;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class RepositoryPresenterTest extends LeosPresenterTest {

    @Mock
    private RepositoryView repositoryView;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private DocumentService documentService;
    
    @Mock
    private LockingService lockingService;

    @Mock
    private LockHelper lockHelper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private User user;

    @InjectMocks
    private RepositoryPresenter repositoryPresenter;

    @Test
    public void testEnterRepositoryView() throws Exception {

        List<LeosObjectProperties> documents = new ArrayList<>();
        LeosDocument leosObjectMock = mock(LeosDocument.class);
        documents.add(leosObjectMock);
        when(workspaceService.browseUserWorkspace()).thenReturn(documents);

        when(leosObjectMock.getLeosId()).thenReturn("878");
        when(leosObjectMock.getLeosTypeId()).thenReturn(LeosTypeId.LEOS_DOCUMENT);
        when(leosObjectMock.getStage()).thenReturn(Stage.DRAFT);
        
        HashMap<String, List<LockData>> map = new HashMap<String, List<LockData>>();
        List<LockData> lstLocksInfo = new ArrayList<LockData>();
        lstLocksInfo.add(new LockData("878", 1418046932495L, "login", "user", "sessionID", LockLevel.READ_LOCK));
        map.put("878", lstLocksInfo);
        when(lockingService.getLockingInfo("878")).thenReturn(lstLocksInfo);
        when(user.getLogin()).thenReturn("login");
        
        // DO THE ACTUAL CALL
        repositoryPresenter.enterRepositoryView(new EnterRepositoryViewEvent());

        verify(repositoryView).showDisclaimer();
        verify(workspaceService).browseUserWorkspace();

        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        verify(repositoryView).setSampleDocuments(argument.capture());

        List documentList = argument.getValue();
        assertThat(documentList.size(), is(1));
        assertThat(((DocumentVO) documentList.get(0)).getLeosId(), equalTo(leosObjectMock.getLeosId()));
        assertThat(((DocumentVO) documentList.get(0)).getLockInfo().get(0), is(sameInstance(map.get("878").get(0))));

        verifyNoMoreInteractions(workspaceService, repositoryView);
    }

    @Test
    public void testNavigateToDocumentView() {

        String docId = "123";

        // DO THE ACTUAL CALL
        repositoryPresenter.navigateToDocumentView(new SelectDocumentEvent(docId, Stage.DRAFT));

        verify(httpSession).setAttribute(SessionAttribute.DOCUMENT_ID.name(), docId);
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("viewId", equalTo(DocumentView.VIEW_ID))));
    }

    @Test
    public void testNavigateToFeedbackView() {

        String docId = "123";

        // DO THE ACTUAL CALL
        repositoryPresenter.navigateToDocumentView(new SelectDocumentEvent(docId, Stage.FEEDBACK));

        verify(httpSession).setAttribute(SessionAttribute.DOCUMENT_ID.name(), docId);
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("viewId", equalTo(FeedbackView.VIEW_ID))));
    }
    @Test
    public void test_onDocumentLockUpdate_readLock() {

        LockData lockData = new LockData("45", 45L, "test", "test", "sessionID", LockLevel.READ_LOCK);
        List lstLocks= new ArrayList<LockData>();
        lstLocks.add(lockData);
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,lockData, lstLocks);

        // DO THE ACTUAL CALL
        repositoryPresenter.onLockUpdate(lockActionInfo);

        verify(repositoryView).updateLockInfo( argThat(Matchers.<DocumentVO>hasProperty("lockState", equalTo(DocumentVO.LockState.UNLOCKED))));
    }

    @Test
    public void test_onDocumentLockUpdate_documentLock() {

        LockData lockData = new LockData("45", 45L, "test", "test", "sessionID", LockLevel.DOCUMENT_LOCK);
        List lstLocks= new ArrayList<LockData>();
        lstLocks.add(lockData);
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,lockData, lstLocks);

        when(lockingService.getLockingInfo("45")).thenReturn(lstLocks);
        when(user.getLogin()).thenReturn("login");

        // DO THE ACTUAL CALL
        repositoryPresenter.onLockUpdate(lockActionInfo);

        verify(repositoryView).updateLockInfo(argThat(Matchers.<DocumentVO>hasProperty("leosId", equalTo("45"))));
        verify(repositoryView).updateLockInfo(argThat(Matchers.<DocumentVO>hasProperty("lockState", equalTo(DocumentVO.LockState.UNLOCKED))));
    }
    
    @Test
    public void test_onDocumentLockUpdate_elementLock() {

        LockData lockData = new LockData("45", 45L, "test", "test", "sessionID", LockLevel.ELEMENT_LOCK, "E1");
        List lstLocks= new ArrayList<LockData>();
        lstLocks.add(lockData);
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,lockData, lstLocks);

        when(lockingService.getLockingInfo("45")).thenReturn(lstLocks);
        when(user.getLogin()).thenReturn("login");

        // DO THE ACTUAL CALL
        repositoryPresenter.onLockUpdate(lockActionInfo);

        verify(repositoryView).updateLockInfo(argThat(Matchers.<DocumentVO>hasProperty("leosId", equalTo("45"))));
        verify(repositoryView).updateLockInfo( argThat(Matchers.<DocumentVO>hasProperty("lockState", equalTo(DocumentVO.LockState.UNLOCKED))));
    }
    
    @Test
    public void test_onViewLeave() {

        // DO THE ACTUAL CALL
        repositoryPresenter.onViewLeave();
        
        verify(lockingService).unregisterLockInfoBroadcastListener(repositoryPresenter);
    }

    @Test
    public void test_showDisclaimer_should_showDisclaimer_when_sessionParamNotFound() {
        when(httpSession.getAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name())).thenReturn(null);

        // DO THE ACTUAL CALL
        repositoryPresenter.showDisclaimer();

        verify(repositoryView).showDisclaimer();
        verify(httpSession).setAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name(), Boolean.FALSE);
    }

    @Test
     public void test_showDisclaimer_should_showDisclaimer_when_sessionParamIsTrue() {
        when(httpSession.getAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name())).thenReturn(Boolean.TRUE);

        // DO THE ACTUAL CALL
        repositoryPresenter.showDisclaimer();

        verify(repositoryView).showDisclaimer();
        verify(httpSession).setAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name(), Boolean.FALSE);
    }

    @Test
    public void test_showDisclaimer_should_ignoreDisclaimer_when_sessionParamIsFalse() {
        when(httpSession.getAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name())).thenReturn(Boolean.FALSE);

        // DO THE ACTUAL CALL
        repositoryPresenter.showDisclaimer();

        verify(repositoryView, never()).showDisclaimer();
    }
    
}
