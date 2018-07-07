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
import eu.europa.ec.leos.services.content.ArticleService;
import eu.europa.ec.leos.services.content.ElementService;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.RulesService;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.web.UrlBuilder;
import eu.europa.ec.leos.support.xml.TransformationManager;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockActionInfo.Operation;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.component.EditTocRequestEvent;
import eu.europa.ec.leos.web.event.view.LeaveViewEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.ui.screen.repository.RepositoryViewImpl;
import eu.europa.ec.leos.web.view.DocumentView;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

public class DocumentPresenterTest extends LeosPresenterTest {

    private static final String SESSION_ID = "sessionID";

    private static final String ARTICLE_TAG = "article";
    private static final String CITATIONS_TAG = "citations";
    private static final String RECITALS_TAG = "recitals";
    @Mock
    private DocumentView documentView;

    @Mock
    private DocumentService documentService;

    @Mock
    private ArticleService articleService;

    @Mock
    private ElementService elementService;

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
    
    @Mock
    private LockHelper lockHelper ;
    
    @InjectMocks
    private DocumentPresenter documentPresenter ;

    @Before
    public void init() throws Exception {
        when(httpSession.getId()).thenReturn(SESSION_ID);
    }
    
    @Test
    public void testEnterDocumentView() throws Exception {

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
        when(transformationManager.toEditableXml(argThat(sameInstance(document.getContentStream())), any(String.class))).thenReturn(displayableContent);
        when(documentService.getTableOfContent(document)).thenReturn(tableOfContentItemVoList);
        
        when(urlBuilder.getDocumentPdfUrl(any(HttpServletRequest.class), eq(docId))).thenReturn(url);
        when(urlBuilder.getDocumentHtmlUrl(any(HttpServletRequest.class), eq(docId))).thenReturn(url);

        // DO THE ACTUAL CALL
        documentPresenter.enterDocumentView(new EnterDocumentViewEvent());

        verify(documentService).getDocument(docId);
        verify(transformationManager).toEditableXml(argThat(sameInstance(document.getContentStream())), any(String.class));
        verify(documentView).setDocumentTitle(docName);
        verify(documentView).refreshContent(displayableContent);
        verify(documentService).getTableOfContent(document);
        verify(documentView).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(lockingService).lockDocument(docId, user, SESSION_ID, LockLevel.READ_LOCK);
        verify(documentView).setDocumentPreviewURLs(docId, "http://test.com", "http://test.com");
        verify(documentView).updateLocks(any(LockActionInfo.class));
        verify(documentView).setDocumentStage(document.getStage());

        verifyNoMoreInteractions(documentService, transformationManager, documentView);
    }

    @Test
    public void test_LoadCrossReferenceToc() throws Exception {

        String docId = "555";
        String windowName = "";
        String selectedNodeId = "xyz";
        LeosDocument document = mock(LeosDocument.class);

        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        List<String> ancestorsIds = Collections.emptyList();
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(documentService.getTableOfContent(document)).thenReturn(tableOfContentItemVoList);
        when(documentService.getAncestorsIdsForElementId(document, selectedNodeId)).thenReturn(ancestorsIds);

        // DO THE ACTUAL CALL
        documentPresenter.fetchTocAndAncestors(new FetchCrossRefTocRequestEvent(selectedNodeId));

        verify(documentService).getDocument(docId);
        verify(documentService).getTableOfContent(document);
        verify(documentService).getAncestorsIdsForElementId(document, selectedNodeId);
        verify(documentView).setTocAndAncestors(argThat(sameInstance(tableOfContentItemVoList)), eq(selectedNodeId), argThat(sameInstance(ancestorsIds)));
        verifyNoMoreInteractions(documentService, documentView);
    }

    
    @Test
    public void testEnterDocumentView_when_documentIsLocked() throws Exception {

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
        documentPresenter.enterDocumentView(new EnterDocumentViewEvent());

        verify(lockingService).lockDocument(docId, user, SESSION_ID, LockLevel.READ_LOCK);
        verify(eventBus).post(isA(NavigationRequestEvent.class));

        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void testLeaveDocumentView() throws Exception {

        String docId = "555";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);

        User user = ModelHelper.buildUser(45L, "login", "name");
        when(securityContext.getUser()).thenReturn(user);
        when(documentView.getViewId()).thenReturn(DocumentView.VIEW_ID);

        // DO THE ACTUAL CALL
        documentPresenter.leaveView(new LeaveViewEvent(DocumentView.VIEW_ID));

        verify(lockingService).releaseLocksForSession(docId, httpSession.getId());
        verify(httpSession).removeAttribute(SessionAttribute.DOCUMENT_ID.name());

    }

    @Test
    public void testEnterDocumentView_should_showWarningMessage_when_noIdOnSession() throws IOException {

        // DO THE ACTUAL CALL
        documentPresenter.enterDocumentView(new EnterDocumentViewEvent());

        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("viewId", equalTo(RepositoryViewImpl.VIEW_ID))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.id.missing"))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("type", equalTo(NotificationEvent.Type.WARNING))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("args", equalTo(new Object[0]))));
    }

    @Test
    public void testCloseDocument() {

        // DO THE ACTUAL CALL
        documentPresenter.closeDocument(new CloseDocumentEvent());
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("viewId", equalTo(RepositoryViewImpl.VIEW_ID))));
    }

    @Test
    public void testRefreshDocument() throws IOException {

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

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(transformationManager.toEditableXml(argThat(sameInstance(document.getContentStream())),any(String.class))).thenReturn(displayableContent);
        when(documentService.getTableOfContent(document)).thenReturn(tableOfContentItemVoList);
        
        when(urlBuilder.getDocumentPdfUrl(any(HttpServletRequest.class), eq(docId))).thenReturn(url);
        when(urlBuilder.getDocumentHtmlUrl(any(HttpServletRequest.class), eq(docId))).thenReturn(url);

        // DO THE ACTUAL CALL
        documentPresenter.refreshDocument(new RefreshDocumentEvent());

        verify(documentService).getDocument(docId);
        verify(documentService).getTableOfContent(document);
        verify(transformationManager).toEditableXml(argThat(sameInstance(document.getContentStream())), any(String.class));
        verify(documentView).refreshContent(displayableContent);
        verify(documentView).setDocumentTitle(docName);
        verify(documentView).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(documentView).setDocumentPreviewURLs(docId, url, url);
        verify(documentView).setDocumentStage(document.getStage());

        verifyNoMoreInteractions(documentService, transformationManager, documentView);
    }

    @Test
    public void test_downloadXml() {

        String docId = "555";
        String docName = "document name";
        byte[] byteContent = new byte[]{1, 2, 3};
        ByteArrayInputStream bios = new ByteArrayInputStream(byteContent);

        LeosDocument document = mock(LeosDocument.class);
        when(document.getLeosId()).thenReturn(docId);
        when(document.getTitle()).thenReturn(docName);
        when(document.getContentStream()).thenReturn(bios);

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(document);

        // DO THE ACTUAL CALL
        documentPresenter.downloadXml(new DownloadXmlRequestEvent());

        verify(documentService).getDocument(docId);
        verify(documentView).showDownloadWindow(document, "xml.window.download.message");

        verifyNoMoreInteractions(documentService, documentView);
    }

    @Test
    public void testEditArticle_should_showArticleEditor() throws Exception {

        String docId = "555";

        LeosDocument document = mock(LeosDocument.class);

        String articleId = "7474";
        String articleContent = "article content";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        ArrayList<LockData> lockInfolst=new ArrayList<LockData>();
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,null, new ArrayList<LockData>());
        
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockingService.isElementLockedFor(docId, userLogin ,SESSION_ID,articleId)).thenReturn(true);
        when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.ELEMENT_LOCK, articleId )).thenReturn(lockActionInfo);
        when(lockHelper.lockElement(articleId)).thenReturn(true);
        
        when(documentService.getDocument(docId)).thenReturn(document);
        when(elementService.getElement(document, ARTICLE_TAG, articleId)).thenReturn(articleContent);
        
        // DO THE ACTUAL CALL
        documentPresenter.editElement(new EditElementRequestEvent(articleId, ARTICLE_TAG));

        verify(documentService).getDocument(docId);
        verify(documentView).showElementEditor(articleId, ARTICLE_TAG, articleContent);

        verifyNoMoreInteractions(documentService, documentView);
    }

    @Test
    public void testEditArticle_should_NotshowArticleEditor_whenArticleLocked() throws Exception {
            String docId = "555";

            LeosDocument document = mock(LeosDocument.class);

            String articleId = "7474";
            String articleContent = "article content";
            
            String userLogin="login";
            User user = ModelHelper.buildUser(45L, userLogin, "name");
            when(securityContext.getUser()).thenReturn(user);
            ArrayList<LockData> lockInfolst=new ArrayList<LockData>();
            lockInfolst.add(new LockData(docId, new Date().getTime(), userLogin, "name", SESSION_ID, LockLevel.ELEMENT_LOCK, articleId));
            //lock not allocated 
            LockActionInfo lockActionInfo = new LockActionInfo(false,Operation.ACQUIRE,lockInfolst.get(0), lockInfolst);

            
            when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
            when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.ELEMENT_LOCK, articleId )).thenReturn(lockActionInfo);

            when(documentService.getDocument(docId)).thenReturn(document);
            when(elementService.getElement(document, ARTICLE_TAG, articleId)).thenReturn(articleContent);
            when(lockHelper.lockElement( articleId)).thenReturn(false);

            // DO THE ACTUAL CALL
            documentPresenter.editElement(new EditElementRequestEvent(articleId, ARTICLE_TAG));

            verifyZeroInteractions(documentView);

            verifyNoMoreInteractions(documentService, documentView);
        }
    
    @Test
    public void testEditMetadata_should_NotshowMetaDataEditor_whenDocumentLocked() throws Exception {
            String docId = "555";

            LeosDocument document = mock(LeosDocument.class);

            String userLogin="login";
            User user = ModelHelper.buildUser(45L, userLogin, "name");

            when(securityContext.getUser()).thenReturn(user);

            ArrayList<LockData> lockInfolst=new ArrayList<LockData>();
            lockInfolst.add(new LockData(docId, new Date().getTime(), userLogin, "name", SESSION_ID, LockLevel.DOCUMENT_LOCK));
             //lock not allocated 
            LockActionInfo lockActionInfo = new LockActionInfo(false,Operation.ACQUIRE,lockInfolst.get(0), lockInfolst);

            when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
            when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
            when(lockHelper.lockDocument()).thenReturn(false);
            when(documentService.getDocument(docId)).thenReturn(document);

            // DO THE ACTUAL CALL
            documentPresenter.editMetadata(new EditMetadataRequestEvent());

            verifyZeroInteractions(documentView);

            verifyNoMoreInteractions(documentService, documentView);
        }

    @Test
    public void testEditToc_should_NotshowTocEditor_whenDocumentLocked() throws Exception {
            String docId = "555";

            LeosDocument document = mock(LeosDocument.class);

            String userLogin="login";
            User user = ModelHelper.buildUser(45L, userLogin, "name");

            when(securityContext.getUser()).thenReturn(user);
            ArrayList<LockData> lockInfolst=new ArrayList<LockData>();
            lockInfolst.add(new LockData(docId, new Date().getTime(), userLogin, "name", SESSION_ID, LockLevel.DOCUMENT_LOCK));
             //lock not allocated 
            LockActionInfo lockActionInfo = new LockActionInfo(false,Operation.ACQUIRE,lockInfolst.get(0), lockInfolst);

            when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
            when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);

            when(documentService.getDocument(docId)).thenReturn(document);
            when(lockHelper.lockDocument()).thenReturn(false);

            // DO THE ACTUAL CALL
            documentPresenter.editToc(new EditTocRequestEvent());

            verifyZeroInteractions(documentView);
            //verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.locked"))));

            verifyNoMoreInteractions(documentService, documentView);
        }
    
    @Test
    public void test_saveArticle_should_returnUpdatedDocument() throws Exception {

        String docId = "555";

        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);
        String articleId = "486";
        String newArticleText = "new article text";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( articleId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(elementService.saveElement(originalDocument, userLogin, newArticleText, ARTICLE_TAG, articleId)).thenReturn(updatedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(articleId, ARTICLE_TAG, newArticleText));

        verify(elementService).saveElement(originalDocument, userLogin, newArticleText, ARTICLE_TAG, articleId);
        verify(documentService).getDocument(docId);

        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void test_saveArticle_should_redirectToRepositoryView_whenNoLockWasAvailable() throws Exception {

        String docId = "555";

        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String articleId = "486";
        String newArticleText = "new article text";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockingService.isDocumentLockedFor(docId, userLogin ,SESSION_ID)).thenReturn(false);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(elementService.saveElement(originalDocument, userLogin, newArticleText, ARTICLE_TAG, articleId)).thenReturn(updatedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(articleId, ARTICLE_TAG, newArticleText));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));

        verifyNoMoreInteractions(documentService, elementService, eventBus);
    }

    @Test
    public void test_deleteArticle_should_returnUpdatedDocument() throws Exception {

        String docId = "555";

        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String articleTag = "article";
        String articleId = "486";
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);

        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,null, new ArrayList<LockData>());
        
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(lockingService.unlockDocument(docId, user.getLogin(), SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(elementService.deleteElement(originalDocument, userLogin, articleId, articleTag)).thenReturn(updatedDocument);
        when(lockHelper.lockDocument()).thenReturn(true);
        when(lockHelper.unlockDocument()).thenReturn(true);

        // DO THE ACTUAL CALL
        documentPresenter.deleteElement(new DeleteElementRequestEvent(articleId, articleTag));

        verify(elementService).deleteElement(originalDocument, userLogin, articleId, articleTag);
        verify(documentService).getDocument(docId);

        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void test_deleteArticle_should_NotDeleteArticle() throws Exception {

        String docId = "555";

        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String articleTag = "article";
        String articleId = "486";
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,null, new ArrayList<LockData>());

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(lockingService.unlockDocument(docId, user.getLogin(), SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(elementService.deleteElement(originalDocument, userLogin, articleId, articleTag)).thenReturn(updatedDocument);
        when(lockHelper.lockDocument()).thenReturn(false);

        // DO THE ACTUAL CALL
        documentPresenter.deleteElement(new DeleteElementRequestEvent(articleId, articleTag));

        verifyNoMoreInteractions(documentService,elementService);
    }

    @Test
    public void test_insertArticle_Before() throws Exception {

        String docId = "555";
        boolean before = true;

        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        LeosDocument originalDocument = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);
        
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,null, new ArrayList<LockData>());

        String articleTag = "article";
        String articleId = "486";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(documentService.getDocument(docId)).thenReturn(originalDocument);
        when(lockingService.lockDocument(docId, user, SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(lockingService.unlockDocument(docId, user.getLogin(), SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(articleService.insertNewArticle(originalDocument, userLogin, articleId, before)).thenReturn(updatedDocument);
        when(lockHelper.lockDocument()).thenReturn(true);
        // DO THE ACTUAL CALL
        documentPresenter.insertElement(new InsertElementRequestEvent(articleId, articleTag, InsertElementRequestEvent.POSITION.BEFORE));

        verify(articleService).insertNewArticle(originalDocument, userLogin, articleId, before);
        verify(documentService).getDocument(docId);

        verifyNoMoreInteractions(documentService);
    }

    @Test
    public void testEditMetadata_should_showMetadataEditor() throws Exception {

        LeosDocument originalDocument = mock(LeosDocument.class);
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn("test");
        when(documentService.getDocument("test")).thenReturn(originalDocument);
        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,null, new ArrayList<LockData>());
        when(lockingService.lockDocument("test", user, SESSION_ID, LockLevel.DOCUMENT_LOCK )).thenReturn(lockActionInfo);
        when(lockHelper.lockDocument()).thenReturn(true);
        MetaDataVO metaDataVO = new MetaDataVO();
        when(documentService.getMetaData(originalDocument)).thenReturn(metaDataVO);

        // DO THE ACTUAL CALL
        documentPresenter.editMetadata(new EditMetadataRequestEvent());

        verify(documentView).showMetadataEditWindow(metaDataVO);
    }

    @Test
    public void testSaveMetadata() throws Exception {
        MetaDataVO metaDataVO = new MetaDataVO("template", "en", "docStage", "docType", "docTitle", "");
        LeosDocument originalDocument = mock(LeosDocument.class);
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        when(originalDocument.getLeosId()).thenReturn("test");
        byte[] originalByteContent = new byte[]{1, 2, 3};
        when(lockHelper.isDocumentLockedFor()).thenReturn(true);
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn("test");
        when(documentService.getDocument("test")).thenReturn(originalDocument);
        when(documentService.updateMetaData(originalDocument, userLogin, metaDataVO)).thenReturn(originalDocument);

        // DO THE ACTUAL CALL
        documentPresenter.saveMetaData(new SaveMetaDataRequestEvent(metaDataVO));

        verify(documentService).getDocument("test");
        verify(documentService).updateMetaData(originalDocument, userLogin, metaDataVO);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("metadata.edit.saved"))));
    }

    @Test
    public void testEditToc() throws Exception {
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);

        LockActionInfo lockActionInfo = new LockActionInfo(true,Operation.ACQUIRE,null, new ArrayList<LockData>());
        
        LeosDocument originalDocument = mock(LeosDocument.class);
        when(originalDocument.getLeosId()).thenReturn("test");
        byte[] originalByteContent = new byte[]{1, 2, 3};
        when(originalDocument.getContentStream()).thenReturn(new ByteArrayInputStream(originalByteContent));

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn("test");
        when(documentService.getDocument("test")).thenReturn(originalDocument);
        when(lockingService.lockDocument("test", user, SESSION_ID, LockLevel.DOCUMENT_LOCK)).thenReturn(lockActionInfo);
        when(lockHelper.lockDocument()).thenReturn(true);
        List<TableOfContentItemVO> tocList = new ArrayList<TableOfContentItemVO>();
        when(documentService.getTableOfContent(originalDocument)).thenReturn(tocList);

        Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tocRules = Collections.emptyMap();
        when(rulesService.getDefaultTableOfContentRules()).thenReturn(tocRules);

        // DO THE ACTUAL CALL
        documentPresenter.editToc(new EditTocRequestEvent());

        verify(documentService).getDocument("test");
        verify(documentService).getTableOfContent(originalDocument);
        verify(documentView).showTocEditWindow(tocList, tocRules);
        verify(rulesService).getDefaultTableOfContentRules();
    }

    @Test
    public void testSaveArticle_when_NoDocumentId_should_DisplayNotification() throws Exception {

        String articleId = "486";
        String newArticleText = "new article text";
        
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);
        
        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(null);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(articleId, ARTICLE_TAG, newArticleText));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));

        verifyNoMoreInteractions(documentService, elementService, eventBus, lockingService);
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
        when(elementService.getElement(document, CITATIONS_TAG, citationsId)).thenReturn(citationsContent);

        // DO THE ACTUAL CALL
        documentPresenter.editElement((new EditElementRequestEvent(citationsId, CITATIONS_TAG)));

        verify(documentService).getDocument(docId);
        verify(elementService).getElement(document, CITATIONS_TAG, citationsId);
        verify(documentView).showElementEditor(citationsId, CITATIONS_TAG, citationsContent);

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
        when(elementService.getElement(document, CITATIONS_TAG, citationsId)).thenReturn(citationsContent);

        // DO THE ACTUAL CALL
        documentPresenter.editElement((new EditElementRequestEvent(citationsId, CITATIONS_TAG)));

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
        try {
            documentPresenter.closeElementEditor(new CloseElementEditorEvent("7474"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        verify(lockHelper).isElementLockedFor( citationsId);
        verify(lockHelper).unlockElement( citationsId);

    }

    @Test
    public void test_saveCitations_should_saveUpdatedDocument() throws Exception {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String citationsId = "7474";
        String citationsTag = "citations";
        String citationsContent = "<citations id=\"7474\"><citation>testCit</citation></citations>";
        String newCitationsContent = "<citations id=\"7474\"><citation>testCit</citation><citation>newtestCit</citation></citations>";

        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( citationsId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(elementService.saveElement(document, userLogin, newCitationsContent, CITATIONS_TAG, citationsId)).thenReturn(updatedDocument);
        when(elementService.getElement(updatedDocument, CITATIONS_TAG, citationsId)).thenReturn(newCitationsContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(citationsId, CITATIONS_TAG, newCitationsContent));

        verify(elementService).saveElement(document, userLogin, newCitationsContent, CITATIONS_TAG, citationsId);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
        verify(documentView).refreshElementEditor(citationsId, citationsTag, newCitationsContent);
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
        when(elementService.getElement(document, CITATIONS_TAG, citationsId)).thenReturn(citationsContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(citationsId, CITATIONS_TAG, newCitationsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, elementService);
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
        documentPresenter.saveElement(new SaveElementRequestEvent(citationsId, CITATIONS_TAG, newCitationsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, elementService);
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
        when(elementService.getElement(document, RECITALS_TAG, recitalsId)).thenReturn(recitalsContent);

        // DO THE ACTUAL CALL
        documentPresenter.editElement((new EditElementRequestEvent(recitalsId, RECITALS_TAG)));

        verify(documentService).getDocument(docId);
        verify(elementService).getElement(document, RECITALS_TAG, recitalsId);
        verify(documentView).showElementEditor(recitalsId, RECITALS_TAG, recitalsContent);

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
        when(elementService.getElement(document, RECITALS_TAG, recitalsId)).thenReturn(recitalsContent);

        // DO THE ACTUAL CALL
        documentPresenter.editElement((new EditElementRequestEvent(recitalsId, RECITALS_TAG)));

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
        try {
            documentPresenter.closeElementEditor(new CloseElementEditorEvent("7474"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        verify(lockHelper).isElementLockedFor( recitalsId);
        verify(lockHelper).unlockElement( recitalsId);

    }

    @Test
    public void test_saveRecitals_should_saveUpdatedDocument() throws Exception {
        String docId = "555";
        LeosDocument document = mock(LeosDocument.class);
        LeosDocument updatedDocument = mock(LeosDocument.class);

        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);

        String recitalsId = "7474";
        String recitalsTag = "recitals";
        String recitalsContent = "<recitals id=\"7474\"><recital>testCit</recital></recitals>";
        String newRecitalsContent = "<recitals id=\"7474\"><recital>testCit</recital><recital>newtestCit</recital></recitals>";

        when(httpSession.getAttribute(SessionAttribute.DOCUMENT_ID.name())).thenReturn(docId);
        when(lockHelper.isElementLockedFor( recitalsId)).thenReturn(true);
        when(documentService.getDocument(docId)).thenReturn(document);
        when(elementService.saveElement(document, userLogin, newRecitalsContent, RECITALS_TAG, recitalsId)).thenReturn(updatedDocument);
        when(elementService.getElement(updatedDocument, RECITALS_TAG, recitalsId)).thenReturn(newRecitalsContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(recitalsId, RECITALS_TAG, newRecitalsContent));

        verify(elementService).saveElement(document, userLogin, newRecitalsContent, RECITALS_TAG, recitalsId);
        verify(documentService).getDocument(docId);
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
        verify(documentView).refreshElementEditor(recitalsId, recitalsTag, newRecitalsContent);
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
        when(elementService.getElement(document, RECITALS_TAG, recitalsId)).thenReturn(recitalsContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(recitalsId, RECITALS_TAG, newRecitalsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, elementService);
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
        documentPresenter.saveElement(new SaveElementRequestEvent(recitalsId, RECITALS_TAG, newRecitalsContent));

        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.lock.lost"))));
        verifyNoMoreInteractions(documentService, elementService);
    }

}
