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
package eu.europa.ec.leos.ui.view.document;


import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.Content.Source;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.processor.BillProcessor;
import eu.europa.ec.leos.services.content.processor.DocumentContentService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.toc.TocRulesService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.importoj.ImportService;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.toc.EditTocRequestEvent;
import eu.europa.ec.leos.ui.support.CoEditionHelper;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo.Operation;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.model.DocType;
import eu.europa.ec.leos.web.model.SearchCriteriaVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import io.atlassian.fugue.Option;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class DocumentPresenterTest extends LeosPresenterTest {

    private static final String SESSION_ID = "sessionID";

    private static final String ARTICLE_TAG = "article";
    private static final String CITATIONS_TAG = "citations";
    private static final String RECITALS_TAG = "recitals";

    private static final String PRESENTER_ID = "ab51f419-c6b0-45cb-82ed-77a61099b58f";

    private SecurityContext securityContext;

    private User contextUser;

    private UuidHelper uuidHelper;

    @Mock
    private DocumentScreen documentScreen;

    @Mock
    private BillService billService;

    @Mock
    private ImportService importService;

    @Mock
    private UserHelper userHelper;

    @Mock
    private CoEditionHelper coEditionHelper;

    @Mock
    private BillProcessor billProcessor;

    @Mock
    private ElementProcessor<Bill> elementProcessor;

    @Mock
    private DocumentContentService documentContentService;

    @Mock
    private TocRulesService tocRulesService;

    @InjectMocks
    private DocumentPresenter documentPresenter ;

    @Mock
    private UrlBuilder urlBuilder;

    @Mock
    private MessageHelper messageHelper;

    @Before
    public void setup(){
        contextUser = ModelHelper.buildUser(45L, "login", "name");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUser()).thenReturn(contextUser);
        uuidHelper = mock(UuidHelper.class);
        when(uuidHelper.getRandomUUID()).thenReturn(PRESENTER_ID);
        super.setup();
    }

    @Before
    public void init() {
        when(httpSession.getId()).thenReturn(SESSION_ID);
    }

    @Test
    public void testEnterDocumentView() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        String docName = "document name";
        String url="http://test.com";
        String documentVersion="1.1";
        byte[] byteContent = new byte[]{1, 2, 3};

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Instant now = Instant.now();
        Bill document = new Bill(docId, "Proposal", "login", now, "login", now,
                documentVersion, documentVersion, "", true, true,
                docName, collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String displayableContent = "document displayable content";
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        User user = ModelHelper.buildUser(45L, "login", "name", "DIGIT");

        DocumentVO billVO = new DocumentVO(docId,"EN", LeosCategory.BILL, "login",  Date.from(now));;
        billVO.addCollaborators(collaborators);
        List<LeosPermission> permissions = Collections.emptyList();
        List<CoEditionVO> coEditionVos = Collections.emptyList();

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);

        when(billService.findBill(docId)).thenReturn(document);
        when(userHelper.getUser("login")).thenReturn(user);
        when(securityContext.getPermissions(document)).thenReturn(permissions);
        when(documentContentService.toEditableContent(isA(XmlDocument.class), any(), any())).thenReturn(displayableContent);
        when(billService.getTableOfContent(document, true)).thenReturn(tableOfContentItemVoList);

        // DO THE ACTUAL CALL
        documentPresenter.enter();

        verify(billService).findBill(docId);
        verify(documentContentService).toEditableContent(any(XmlDocument.class), any(), any());
        verify(billService).getTableOfContent(document, true);

        verify(documentScreen).refreshContent(displayableContent);
        verify(documentScreen).setDocumentTitle(docName);
        verify(documentScreen).setDocumentVersionInfo(any());
        verify(documentScreen).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(documentScreen).setPermissions(argThat(org.hamcrest.Matchers.hasProperty("id")));
        verify(documentScreen).updateUserCoEditionInfo(coEditionVos, PRESENTER_ID);

        verifyNoMoreInteractions(billService, documentContentService, documentScreen);
    }

    @Test
    public void test_LoadCrossReferenceToc() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        String windowName = "";
        List<String> selectedNodeId = new ArrayList();
        selectedNodeId.add("xyz");

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        List<String> ancestorsIds = Collections.emptyList();
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(document);
        when(billService.getTableOfContent(document, true)).thenReturn(tableOfContentItemVoList);
        when(billService.getAncestorsIdsForElementId(document, selectedNodeId)).thenReturn(ancestorsIds);

        // DO THE ACTUAL CALL
        documentPresenter.fetchTocAndAncestors(new FetchCrossRefTocRequestEvent(selectedNodeId));

        verify(billService).findBill(docId);
        verify(billService).getTableOfContent(document, true);
        verify(billService).getAncestorsIdsForElementId(document, selectedNodeId);
        verify(documentScreen).setTocAndAncestors(argThat(sameInstance(tableOfContentItemVoList)), argThat(sameInstance(ancestorsIds)));
        verifyNoMoreInteractions(billService, documentScreen);
    }

    @Test
    @Ignore
    public void testLeaveDocumentView() {

        String docId = "555";

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);

        // DO THE ACTUAL CALL
        //        documentPresenter.detachView();   // FIXME replace detach view with something else

        verify(httpSession).removeAttribute(anyString() + "." + SessionAttribute.BILL_ID.name());
    }

    @Test
    @Ignore
    public void testEnterDocumentView_should_showWarningMessage_when_noIdOnSession() {

        // DO THE ACTUAL CALL
        documentPresenter.enter();

        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("target", equalTo(Target.HOME))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.id.missing"))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("type", equalTo(NotificationEvent.Type.WARNING))));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("args", equalTo(new Object[0]))));
    }

    @Test
    public void testCloseDocument() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        String docName = "document name";
        String documentVersion="1.1";
        byte[] byteContent = new byte[]{1, 2, 3};
        String url="AnyURL";
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");

        Bill document = new Bill(docId, "Bill", "login", Instant.now(), "login", Instant.now(),
                documentVersion, documentVersion, "", true, true,
                docName, collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);

        // DO THE ACTUAL CALL
        documentPresenter.closeDocument(new CloseScreenRequestEvent());
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("target", equalTo(Target.PREVIOUS))));
    }

    @Test
    public void testRefreshDocument() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        String docName = "document name";
        String documentVersion="1.1";
        byte[] byteContent = new byte[]{1, 2, 3};
        String url="AnyURL";

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Instant now = Instant.now();
        Bill document = new Bill(docId, "Bill", "login", now, "login", now,
                documentVersion, documentVersion, "", true, true,
                docName, collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String displayableContent = "document displayable content";
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        User user = ModelHelper.buildUser(45L, "login", "name", "DIGIT");
        DocumentVO billVO = new DocumentVO(docId,"EN", LeosCategory.BILL, "login",  Date.from(now));
        billVO.addCollaborators(collaborators);
        List<LeosPermission> permissions = Collections.emptyList();
        List<CoEditionVO> coEditionVos = Collections.emptyList();

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(document);
        when(userHelper.getUser("login")).thenReturn(user);
        when(securityContext.getPermissions(document)).thenReturn(permissions);
        when(documentContentService.toEditableContent(isA(XmlDocument.class), any(), any())).thenReturn(displayableContent);
        when(billService.getTableOfContent(document, true)).thenReturn(tableOfContentItemVoList);

        // DO THE ACTUAL CALL
        documentPresenter.refreshDocument(new RefreshDocumentEvent());

        verify(billService).findBill(docId);
        verify(billService).getTableOfContent(document, true);
        verify(documentContentService).toEditableContent(any(XmlDocument.class), any(), any());

        verify(documentScreen).refreshContent(displayableContent);
        verify(documentScreen).setDocumentTitle(docName);
        verify(documentScreen).setDocumentVersionInfo(any());
        verify(documentScreen).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(documentScreen).setPermissions(argThat(org.hamcrest.Matchers.hasProperty("id")));
        verify(documentScreen).updateUserCoEditionInfo(coEditionVos, PRESENTER_ID);

        verifyNoMoreInteractions(billService, documentContentService, documentScreen);
    }

    @Test
    public void testEditArticle_should_showArticleEditor() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        String versionSeriesId = "1234";
        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                versionSeriesId, "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String articleId = "7474";
        String articleContent = "article content";
        CoEditionActionInfo actionInfo = new CoEditionActionInfo(true, Operation.STORE, null, new ArrayList<CoEditionVO>());

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(document);
        when(elementProcessor.getElement(document, ARTICLE_TAG, articleId)).thenReturn(articleContent);
        when(coEditionHelper.getCurrentEditInfo(versionSeriesId)).thenReturn(actionInfo.getCoEditionVos());
        // DO THE ACTUAL CALL
        documentPresenter.editElement(new EditElementRequestEvent(articleId, ARTICLE_TAG));

        verify(billService).findBill(docId);
        verify(documentScreen).showElementEditor(articleId, ARTICLE_TAG, articleContent, "");
        verifyNoMoreInteractions(billService, documentScreen);
    }

    @Test
    public void test_saveArticle_should_returnUpdatedDocument() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String articleId = "486";
        String newArticleText = "new article text";

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(originalDocument);
        when(elementProcessor.updateElement(originalDocument, newArticleText, ARTICLE_TAG, articleId)).thenReturn(updatedDocumentContent);
        when(billService.updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation." + ARTICLE_TAG + ".updated"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(articleId, ARTICLE_TAG, newArticleText, false));

        verify(elementProcessor).updateElement(originalDocument, newArticleText, ARTICLE_TAG, articleId);
        verify(billService).updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation." + ARTICLE_TAG + ".updated"));
        verify(billService).findBill(docId);
        verifyNoMoreInteractions(billService);
    }

    @Test
    public void test_deleteArticle_should_returnUpdatedDocument() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        User user = ModelHelper.buildUser(45L, "login", "name", "DIGIT");

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(originalDocument);
        when(billProcessor.deleteElement(originalDocument, articleId, articleTag, user)).thenReturn(updatedDocumentContent);
        when(billService.updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("document." + ARTICLE_TAG + ".deleted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.deleteElement(new DeleteElementRequestEvent(articleId, articleTag));

        verify(billProcessor).deleteElement(originalDocument, articleId, articleTag, user);
        verify(billService).findBill(docId);
        verify(billService).updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("document." + ARTICLE_TAG + ".deleted"));
        verifyNoMoreInteractions(billService, elementProcessor);
    }

    @Test
    public void test_deleteArticle_should_NotDeleteArticle() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        User user = ModelHelper.buildUser(45L, "login", "name", "DIGIT");

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String articleTag = "article";
        String articleId = "486";
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(originalDocument);
        when(billProcessor.deleteElement(originalDocument, articleId, articleTag, user)).thenReturn(updatedDocumentContent);
        when(billService.updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("document." + ARTICLE_TAG + ".deleted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.deleteElement(new DeleteElementRequestEvent(articleId, articleTag));

        verify(billProcessor).deleteElement(originalDocument, articleId, articleTag, user);
        verify(billService).findBill(docId);
        verify(billService).updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("document." + ARTICLE_TAG + ".deleted"));
        verifyNoMoreInteractions(billService, elementProcessor);
    }

    @Test
    public void test_insertArticle_Before() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        boolean before = true;

        String articleTag = "article";
        String articleId = "486";

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(originalDocument);
        when(billProcessor.insertNewElement(originalDocument, articleId, before, articleTag)).thenReturn(updatedDocumentContent);
        when(billService.updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation.element.inserted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.insertElement(new InsertElementRequestEvent(articleId, articleTag, InsertElementRequestEvent.POSITION.BEFORE));

        verify(billProcessor).insertNewElement(originalDocument, articleId, before, articleTag);
        verify(billService).findBill(docId);
        verify(billService).updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation.element.inserted"));

        verifyNoMoreInteractions(billService);
    }

    @Test
    public void testEditToc() {
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        byte[] originalByteContent = new byte[]{1, 2, 3};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill("test", "Proposal", "login", Instant.now(), "login", Instant.now(),
                "test", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};
        CoEditionActionInfo actionInfo = new CoEditionActionInfo(true, Operation.STORE, null, new ArrayList<CoEditionVO>());

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn("test");
        when(billService.findBill("test")).thenReturn(originalDocument);
        List<TableOfContentItemVO> tocList = new ArrayList<TableOfContentItemVO>();
        when(billService.getTableOfContent(originalDocument, false)).thenReturn(tocList);

        Map<TocItemType, List<TocItemType>> tocRules = Collections.emptyMap();
        when(tocRulesService.getDefaultTableOfContentRules()).thenReturn(tocRules);
        when(coEditionHelper.getCurrentEditInfo("test")).thenReturn(actionInfo.getCoEditionVos());

        // DO THE ACTUAL CALL
        documentPresenter.editToc(new EditTocRequestEvent());

        verify(billService).findBill("test");
        verify(billService).getTableOfContent(originalDocument, false);
        verify(documentScreen).showTocEditWindow(tocList, tocRules);
        verify(tocRulesService).getDefaultTableOfContentRules();
    }

    @Test
    public void testEditCitations_should_showCitationsEditor() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "test", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String citationsId = "7474";
        String citationsContent = "<citations xml:id=\"7474\"><citation>testCit</citation></citations>";
        CoEditionActionInfo actionInfo = new CoEditionActionInfo(true, Operation.STORE, null, new ArrayList<CoEditionVO>());

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(document);
        when(elementProcessor.getElement(document, CITATIONS_TAG, citationsId)).thenReturn(citationsContent);
        when(coEditionHelper.getCurrentEditInfo("test")).thenReturn(actionInfo.getCoEditionVos());

        // DO THE ACTUAL CALL
        documentPresenter.editElement((new EditElementRequestEvent(citationsId, CITATIONS_TAG)));

        verify(billService).findBill(docId);
        verify(elementProcessor).getElement(document, CITATIONS_TAG, citationsId);
        verify(documentScreen).showElementEditor(citationsId, CITATIONS_TAG, citationsContent, "");

        verifyNoMoreInteractions(billService, documentScreen);
    }

    @Test
    public void testCloseCitationsEditor() {
        String docId = "555";
        String citationsId = "7474";
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docName = "document name";
        String documentVersion="1.1";
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");

        Bill document = new Bill(docId, "Bill", "login", Instant.now(), "login", Instant.now(),
                documentVersion, documentVersion, "", true, true,
                docName, collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        when(billService.findBill(docId)).thenReturn(document);
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);

        // DO THE ACTUAL CALL
        documentPresenter.closeElementEditor(new CloseElementEditorEvent(citationsId, "citation"));
        //verify(eventBus).post(argThat(Matchers.<RefreshDocumentEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
    }

    @Test
    public void test_saveCitations_should_saveUpdatedDocument() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String citationsId = "7474";
        String citationsTag = "citations";
        String newCitationsContent = "<citations xml:id=\"7474\"><citation>testCit</citation><citation>newtestCit</citation></citations>";

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(originalDocument);
        when(elementProcessor.updateElement(originalDocument, newCitationsContent, CITATIONS_TAG, citationsId)).thenReturn(updatedDocumentContent);
        when(billService.updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation." + CITATIONS_TAG + ".updated"))).thenReturn(savedDocument);
        when(elementProcessor.getElement(savedDocument, CITATIONS_TAG, citationsId)).thenReturn(newCitationsContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(citationsId, CITATIONS_TAG, newCitationsContent, false));

        verify(elementProcessor).updateElement(originalDocument, newCitationsContent, CITATIONS_TAG, citationsId);
        verify(billService).findBill(docId);
        verify(billService).updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation." + CITATIONS_TAG + ".updated"));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
        verify(documentScreen).refreshElementEditor(citationsId, citationsTag, newCitationsContent);

        verifyNoMoreInteractions(billService);
    }

    @Ignore // FIXME: this test is not working as this behaviour is removed from presenters
    @Test
    public void testSaveCitations_when_NoDocumentId_should_displayNotification() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));
        byte[] updatedDocumentContent = new byte[]{1, 2};

        String citationsId = "7474";
        String citationsContent = "<citations xml:id=\"7474\"><citation>testCit</citation></citations>";
        String newCitationsContent = "<citations xml:id=\"7474\"><citation>testCit</citation><citation>newtestCit</citation></citations>";
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(null);
        when(billService.findBill(docId)).thenReturn(null);
        when(elementProcessor.updateElement(document, newCitationsContent, CITATIONS_TAG, citationsId)).thenReturn(updatedDocumentContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(citationsId, CITATIONS_TAG, newCitationsContent, false));

        verify(elementProcessor).updateElement(null, newCitationsContent, CITATIONS_TAG, citationsId);
        verifyNoMoreInteractions(billService, elementProcessor);
    }

    @Test
    public void testEditRecitals_should_showRecitalsEditor() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        byte[] originalByteContent = new byte[]{1, 2, 3};

        when(source.getBytes()).thenReturn(originalByteContent);
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "test", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String recitalsId = "7474";
        String recitalsContent = "<recitals xml:id=\"7474\"><recital>testCit</recital></recitals>";
        CoEditionActionInfo actionInfo = new CoEditionActionInfo(true, Operation.STORE, null, new ArrayList<CoEditionVO>());

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(document);
        when(elementProcessor.getElement(document, RECITALS_TAG, recitalsId)).thenReturn(recitalsContent);
        when(coEditionHelper.getCurrentEditInfo("test")).thenReturn(actionInfo.getCoEditionVos());

        // DO THE ACTUAL CALL
        documentPresenter.editElement((new EditElementRequestEvent(recitalsId, RECITALS_TAG)));

        verify(billService).findBill(docId);
        verify(elementProcessor).getElement(document, RECITALS_TAG, recitalsId);
        verify(documentScreen).showElementEditor(recitalsId, RECITALS_TAG, recitalsContent, "");

        verifyNoMoreInteractions(billService, documentScreen);
    }

    @Test
    public void testCloseRecitalsEditor() {
        String docId = "555";
        String recitalsId = "7474";
        String recitalsContent = "<recitals xml:id=\"7474\"><recital>testCit</recital></recitals>";
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docName = "document name";
        String documentVersion="1.1";
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");

        Bill document = new Bill(docId, "Bill", "login", Instant.now(), "login", Instant.now(),
                documentVersion, documentVersion, "", true, true,
                docName, collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        when(billService.findBill(docId)).thenReturn(document);
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);

        // DO THE ACTUAL CALL
        documentPresenter.closeElementEditor(new CloseElementEditorEvent("7474", "recital"));

        //verify(eventBus).post(argThat(Matchers.<RefreshDocumentEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
    }

    @Test
    public void test_saveRecitals_should_saveUpdatedDocument() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        String recitalsId = "7474";
        String recitalsTag = "recitals";
        String newRecitalsContent = "<recitals xml:id=\"7474\"><recital>testCit</recital><recital>newtestCit</recital></recitals>";

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(document);
        when(elementProcessor.updateElement(document, newRecitalsContent, RECITALS_TAG, recitalsId)).thenReturn(updatedDocumentContent);
        when(billService.updateBill(document, updatedDocumentContent, messageHelper.getMessage("operation." + RECITALS_TAG + ".updated"))).thenReturn(savedDocument);
        when(elementProcessor.getElement(savedDocument, RECITALS_TAG, recitalsId)).thenReturn(newRecitalsContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(recitalsId, RECITALS_TAG, newRecitalsContent, false));

        verify(elementProcessor).updateElement(document, newRecitalsContent, RECITALS_TAG, recitalsId);
        verify(billService).findBill(docId);
        verify(billService).updateBill(document, updatedDocumentContent, messageHelper.getMessage("operation." + RECITALS_TAG + ".updated"));
        verify(eventBus).post(argThat(Matchers.<NotificationEvent>hasProperty("messageKey", equalTo("document.content.updated"))));
        verify(documentScreen).refreshElementEditor(recitalsId, recitalsTag, newRecitalsContent);

        verifyNoMoreInteractions(billService);
    }

    @Ignore // FIXME: this test is not working as this behaviour is removed from presenters
    @Test
    public void testSaveRecitals_when_NoDocumentId_should_displayNotification() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill document = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] updatedDocumentContent = new byte[]{1, 2, 3};

        String recitalsId = "7474";
        String recitalsContent = "<recitals xml:id=\"7474\"><recital>testCit</recital></recitals>";
        String newRecitalsContent = "<recitals xml:id=\"7474\"><recital>testCit</recital><recital>newtestCit</recital></recitals>";
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(null);
        when(elementProcessor.updateElement(document, newRecitalsContent, CITATIONS_TAG, recitalsId)).thenReturn(updatedDocumentContent);

        // DO THE ACTUAL CALL
        documentPresenter.saveElement(new SaveElementRequestEvent(recitalsId, RECITALS_TAG, newRecitalsContent, false));

        verify(elementProcessor).updateElement(null, newRecitalsContent, RECITALS_TAG, recitalsId);
        verifyNoMoreInteractions(billService, elementProcessor);
    }

    @Test
    public void test_importElement() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        String docId = "555";
        String aknDocument = "<!--This AkomaNtoso document was created via a LegisWrite export.-->"
                + "<akomaNtoso xml:id=\"akn\">" +
                "<article xml:id=\"art486\">" +
                "<num xml:id=\"aknum\">Article 486</num>" +
                "<heading xml:id=\"aknhead\">1st article</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<num xml:id=\"aknnum2\"></num>" +
                "<heading xml:id=\"aknhead2\">2th articl<authorialNote marker=\"101\" xml:id=\"a1\"><p xml:id=\"p1\">TestNote1</p></authorialNote>e</heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "<heading xml:id=\"aknhead3\">3th article<authorialNote marker=\"90\" xml:id=\"a2\"><p xml:id=\"p2\">TestNote2</p></authorialNote></heading>" +
                "</article>" +
                "<article xml:id=\"art486\">" +
                "</article>" +
                "</akomaNtoso>";

        String articleId = "001";
        String articleTag = "article";
        String article = "<article>Text...</article>";

        SearchCriteriaVO searchCriteria = new SearchCriteriaVO(DocType.REGULATION, "2015", "25");
        List<String> elementIdList = new ArrayList<String>();
        elementIdList.add(articleId);

        when(source.getBytes()).thenReturn(new byte[]{1, 2, 3});
        when(content.getSource()).thenReturn(source);

        BillMetadata billMetadata =new BillMetadata("", "REGULATION", "", "BL-000.xml", "EN", "", "bill-id", "");
        Map<String, String> collaborators = new HashMap<String, String>();
        collaborators.put("login", "OWNER");
        Bill originalDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        byte[] originalDocumentContent = new byte[]{1, 2, 3};
        byte[] updatedDocumentContent = new byte[]{4, 5, 6};

        Bill savedDocument = new Bill(docId, "Proposal", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                "title", collaborators, Arrays.asList(""),
                Option.some(content), Option.some(billMetadata));

        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.BILL_ID.name())).thenReturn(docId);
        when(billService.findBill(docId)).thenReturn(originalDocument);
        when(importService.getAknDocument("reg", 2015, 25)).thenReturn(aknDocument);
        when(importService.insertSelectedElements(originalDocumentContent, aknDocument.getBytes(), elementIdList, "EN")).thenReturn(updatedDocumentContent);
        when(billService.updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation.import.element.inserted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        documentPresenter.importElements(new ImportElementRequestEvent(searchCriteria, elementIdList));

        verify(importService).getAknDocument("reg", 2015, 25);
        verify(importService).insertSelectedElements(originalDocumentContent, aknDocument.getBytes(), elementIdList, "EN");
        verify(billService).findBill(docId);
        verify(billService).updateBill(originalDocument, updatedDocumentContent, messageHelper.getMessage("operation.import.element.inserted"));

        verifyNoMoreInteractions(importService);
        verifyNoMoreInteractions(billService);
    }
}
