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
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.CommentService;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.RulesService;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.web.UrlBuilder;
import eu.europa.ec.leos.support.xml.TransformationManager;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.vo.CommentVO;
import eu.europa.ec.leos.vo.CommentVO.RefersTo;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockActionInfo.Operation;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.LeaveViewEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.view.feedback.DeleteCommentEvent;
import eu.europa.ec.leos.web.event.view.feedback.EnterFeedbackViewEvent;
import eu.europa.ec.leos.web.event.view.feedback.InsertCommentEvent;
import eu.europa.ec.leos.web.event.view.feedback.UpdateCommentEvent;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.ui.screen.repository.RepositoryViewImpl;
import eu.europa.ec.leos.web.view.FeedbackView;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class FeedbackPresenterTest extends LeosPresenterTest {

    private static final String SESSION_ID = "sessionID";

    @Mock
    private FeedbackView feedbackView;

    @Mock
    private DocumentService documentService;

    @Mock
    private CommentService commentService;

    @Mock
    private LockingService lockingService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private TransformationManager transformationManager;

    @Mock
    private RulesService rulesService;

    @Mock
    private UrlBuilder urlBuilder;
    
    @InjectMocks
    private FeedbackPresenter feedbackPresenter ;

    @Before
    public void init() throws Exception {

        when(httpSession.getId()).thenReturn(SESSION_ID);
    }
    
    @Test
    public void testEnterFeedbackView() throws Exception {

        String docId = "555";
        String docName = "document name";
        String url="http://test.com";
        byte[] byteContent = new byte[]{1, 2, 3};

        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(docId);
        when(document.getTitle()).thenReturn(docName);
        ByteArrayInputStream bStreeam=new ByteArrayInputStream(byteContent);
        when(document.getContentStream()).thenReturn(bStreeam);

        String displayableContent = "document displayable content";
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);

        User user = ModelHelper.buildUser(45L, "login", "name");
        when(securityContext.getUser()).thenReturn(user);

        LockData lockData = new LockData(docId, new Date().getTime(), "loginB", "name", SESSION_ID, LockLevel.READ_LOCK);
        List lstLocks= new ArrayList<LockData>();
        lstLocks.add(lockData);
        
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,lockData, lstLocks);
        when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.READ_LOCK)).thenReturn(lockActionInfo);

        when(documentService.getDocument(docId)).thenReturn(document);
        when(transformationManager.toNonEditableXml(argThat(sameInstance(document.getContentStream())), any(String.class))).thenReturn(displayableContent);
        when(documentService.getTableOfContent(document)).thenReturn(tableOfContentItemVoList);
        
        // DO THE ACTUAL CALL
        feedbackPresenter.enterFeedbackView(new EnterFeedbackViewEvent());

        verify(documentService).getDocument(docId);
        verify(transformationManager).toNonEditableXml(argThat(sameInstance(document.getContentStream())), any(String.class));
        verify(feedbackView).setDocumentTitle(docName);
        verify(feedbackView).setDocumentStage(document.getStage());
        verify(feedbackView).refreshContent(displayableContent);
        verify(documentService).getTableOfContent(document);
        verify(feedbackView).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(lockingService).lockDocument(docId, user, SESSION_ID,LockLevel.READ_LOCK);
        verify(feedbackView).updateLocks(any(LockActionInfo.class));
        verify(feedbackView).setUser();

        verifyNoMoreInteractions(documentService, transformationManager, feedbackView);
    }

    @Test
    public void testEnterFeedbackView_when_documentIsLocked() throws Exception {

        String docId = "555";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);

        User user = ModelHelper.buildUser(45L, "login", "name");
        when(securityContext.getUser()).thenReturn(user);

        LockData lockData = new LockData(docId, new Date().getTime(), "loginB", "name", SESSION_ID, LockLevel.READ_LOCK);
        List lstLocks= new ArrayList<LockData>();
        lstLocks.add(lockData);
        
        LockActionInfo lockActionInfo = new LockActionInfo(false,Operation.ACQUIRE,lockData, lstLocks);
        when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.READ_LOCK)).thenReturn(lockActionInfo);

        // DO THE ACTUAL CALL
        feedbackPresenter.enterFeedbackView(new EnterFeedbackViewEvent());

        verify(lockingService).lockDocument(docId, user, SESSION_ID, LockLevel.READ_LOCK);
        verify(eventBus).post(isA(NavigationRequestEvent.class));

        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void testLeaveFeedbackView() throws Exception {

        String docId = "555";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);

        User user = ModelHelper.buildUser(45L, "login", "name");
        when(securityContext.getUser()).thenReturn(user);
        when(feedbackView.getViewId()).thenReturn(FeedbackView.VIEW_ID);

        // DO THE ACTUAL CALL
        feedbackPresenter.leaveView(new LeaveViewEvent(FeedbackView.VIEW_ID));

        verify(lockingService).releaseLocksForSession(docId, httpSession.getId());
        verify(httpSession).removeAttribute(SessionAttribute.DOCUMENT_ID.name());

    }

    @Test
    public void testEnterFeedbackView_should_showWarningMessage_when_noIdOnSession() throws IOException {

        // DO THE ACTUAL CALL
        feedbackPresenter.enterFeedbackView(new EnterFeedbackViewEvent());

        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("viewId", equalTo(RepositoryViewImpl.VIEW_ID))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.id.missing"))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("type", equalTo(NotificationEvent.Type.WARNING))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("args", equalTo(new Object[0]))));
    }

    @Test
    public void testCloseDocument() {

        // DO THE ACTUAL CALL
        feedbackPresenter.closeDocument(new CloseDocumentEvent());
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("viewId", equalTo(RepositoryViewImpl.VIEW_ID))));
    }

    @Test
    public void testRefreshDocument() throws IOException, ParseException {

        String docId = "555";
        String docName = "document name";
        byte[] byteContent = new byte[]{1, 2, 3};
        String url="AnyURL";

        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(docId);
        when(document.getTitle()).thenReturn(docName);
        ByteArrayInputStream bStreeam=new ByteArrayInputStream(byteContent);
        when(document.getContentStream()).thenReturn(bStreeam);

        String displayableContent = "document displayable content";
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
       
        CommentVO commentVOExpected1 = new CommentVO("xyz", "ElementId", "This is a comment...", "User One", "user1","testDG.G",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-29T11:30:00Z"),RefersTo.LEOS_COMMENT);
        CommentVO commentVOExpected2 = new CommentVO("xyz2", "ElementId", "This is a comment...2", "User One2", "user2","testDG.G1",
                new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse("2015-05-30T11:30:00Z"),RefersTo.LEOS_COMMENT);
        
        List<CommentVO> comments= new ArrayList<CommentVO>(Arrays.asList(commentVOExpected1, commentVOExpected2));
        when(commentService.getAllComments(document)).thenReturn(comments);
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(transformationManager.toNonEditableXml(argThat(sameInstance(document.getContentStream())),any(String.class))).thenReturn(displayableContent);
        when(documentService.getTableOfContent(document)).thenReturn(tableOfContentItemVoList);
        
        // DO THE ACTUAL CALL
        feedbackPresenter.refreshDocument(new RefreshDocumentEvent());

        verify(documentService).getDocument(docId);
        verify(documentService).getTableOfContent(document);
        verify(transformationManager).toNonEditableXml(argThat(sameInstance(document.getContentStream())), any(String.class));
        verify(feedbackView).refreshContent(displayableContent);
        verify(feedbackView).setDocumentTitle(docName);
        verify(feedbackView).setUser();
        verify(feedbackView).setDocumentStage(document.getStage());
        verify(feedbackView).setToc(argThat(sameInstance(tableOfContentItemVoList)));

        verifyNoMoreInteractions(documentService, transformationManager, feedbackView, eventBus);
    }
    
    @Test
    public void test_insertComment_After() throws Exception {
        //setup
        String docId = "555";
        String elementId="EID";
        String commentContent="new Comment";
        String commentId="CID";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(commentService.insertNewComment(originalDocument, elementId, commentId, commentContent, false)).thenReturn(updatedDocument);

        // DO THE ACTUAL CALL
        feedbackPresenter.insertComment(new InsertCommentEvent(elementId,commentId,commentContent));

        verify(commentService).insertNewComment(originalDocument, elementId, commentId, commentContent, false);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("operation.comment.inserted"))));
        verifyNoMoreInteractions(commentService,documentService,eventBus);
    }
    
    @Test
    public void test_editComment() throws Exception {
        //setup
        String docId = "555";
        String elementId="EID";
        String commentContent="new Comment";
        String commentId="CID";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(commentService.updateComment(originalDocument, commentId, commentContent)).thenReturn(updatedDocument);

        // DO THE ACTUAL CALL
        feedbackPresenter.updateComment(new UpdateCommentEvent(elementId,commentId,commentContent));

        verify(commentService).updateComment(originalDocument, commentId, commentContent);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("operation.comment.updated"))));
        verifyNoMoreInteractions(commentService,documentService,eventBus);
    }
    @Test
    public void test_deleteComment() throws Exception {
        //setup
        String docId = "555";
        String elementId="EID";
        String commentId="CID";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(commentService.deleteComment(originalDocument, commentId)).thenReturn(updatedDocument);

        // DO THE ACTUAL CALL
        feedbackPresenter.deleteComment(new DeleteCommentEvent(elementId, commentId));

        verify(commentService).deleteComment(originalDocument, commentId);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("operation.comment.deleted"))));
        verifyNoMoreInteractions(commentService,documentService,eventBus);
    }
}
