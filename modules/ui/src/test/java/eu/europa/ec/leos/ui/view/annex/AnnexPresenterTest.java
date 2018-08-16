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
package eu.europa.ec.leos.ui.view.annex;

import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.Content.Source;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.processor.AnnexProcessor;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import io.atlassian.fugue.Option;
import okio.ByteString;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class AnnexPresenterTest extends LeosPresenterTest {

    @Mock
    private SecurityContext securityContext;

    @Mock
    private AnnexScreen annexScreen;

    @Mock
    private AnnexService annexService;

    @Mock
    private ElementProcessor<Annex> elementProcessor;

    @Mock
    private AnnexProcessor annexProcessor;

    @Mock
    private TransformationService transformationManager;

    @Mock
    private UrlBuilder urlBuilder;

    @Mock
    private UserHelper userHelper;

    @Mock
    private MessageHelper messageHelper;

    @InjectMocks
    private AnnexPresenter annexPresenter;

    private String docId;
    private String docTitle;
    private byte[] byteContent;

    @Before
    public void init() throws Exception {
        docId = "555";
        docTitle = "document title";
        byteContent = new byte[]{1, 2, 3};
        when(httpSession.getAttribute(SessionAttribute.ANNEX_ID.name())).thenReturn(docId);
        User user = ModelHelper.buildUser(45L, "login", "name");
        when(securityContext.getUser()).thenReturn(user);
    }

    @Test
    public void testEnterDocumentView() throws Exception {
        Content content = mock(Content.class);
        Source source = mock(Source.class);
        String documentVersion="1.1";

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN","","annex-id", 1, "Annex 1", docTitle);
        Instant now = Instant.now();
        Annex annex = new Annex(docId, "Annex", "login", now, "login", now,
                documentVersion, documentVersion, "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));
        DocumentVO annexVO = new DocumentVO(docId,"EN", LeosCategory.ANNEX, "login",  Date.from(now));
        annexVO.setDocNumber(1);
        annexVO.setTitle(docTitle);
        annexVO.addCollaborators(Collections.emptyMap());
        String displayableContent = "document displayable content";
        User user = ModelHelper.buildUser(45L, "login", "name", "DIGIT");
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        List<LeosPermission> permissions = Collections.emptyList();

        when(annexService.findAnnex(docId)).thenReturn(annex);
        when(userHelper.getUser("login")).thenReturn(user);
        when(securityContext.getPermissions(annex)).thenReturn(permissions);
        when(transformationManager.toEditableXml(isA(ByteArrayInputStream.class), any(), eq(LeosCategory.ANNEX),eq(permissions))).thenReturn(displayableContent);
        when(annexService.getTableOfContent(annex)).thenReturn(tableOfContentItemVoList);
        
        // DO THE ACTUAL CALL
        annexPresenter.enter();

        verify(annexService).findAnnex(docId);
        verify(annexService).getTableOfContent(annex);
        verify(userHelper).getUser("login");
        
        verify(annexScreen).setDocumentVersionInfo(any());
        verify(transformationManager).toEditableXml(any(ByteArrayInputStream.class), any(), eq(LeosCategory.ANNEX),eq(permissions));
        
        verify(annexScreen).setContent(displayableContent);
        verify(annexScreen).setTitle(docTitle);
        verify(annexScreen).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(annexScreen).setPermissions(argThat(org.hamcrest.Matchers.hasProperty("id",equalTo(annexVO.getId()))));

        verifyNoMoreInteractions(userHelper, annexService, transformationManager, annexScreen);
    }

    @Test
    public void testCloseDocument() {
        // DO THE ACTUAL CALL
        annexPresenter.handleCloseScreenRequest(new CloseScreenRequestEvent());
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("target", equalTo(Target.PREVIOUS))));
    }

    @Test
    public void testRefreshDocument() throws Exception {
        Content content = mock(Content.class);
        Source source = mock(Source.class);
        String documentVersion="1.1";

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);
        Instant now = Instant.now();
        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN","","annex-id", 1, "Annex 1", docTitle);
        Annex annex = new Annex(docId, "Annex", "login", now, "login", now,
                documentVersion, documentVersion, "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));
        DocumentVO annexVO = new DocumentVO(docId,"EN", LeosCategory.ANNEX, "login",  Date.from(now));
        annexVO.setDocNumber(1);
        annexVO.setTitle(docTitle);
        String displayableContent = "document displayable content";
        User user = ModelHelper.buildUser(45L, "login", "name", "DIGIT");
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        List<LeosPermission> permissions = Collections.emptyList();

        when(annexService.findAnnex(docId)).thenReturn(annex);
        when(urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest())).thenReturn("");
        when(userHelper.getUser("login")).thenReturn(user);
        when(securityContext.getPermissions(annex)).thenReturn(permissions);
        when(transformationManager.toEditableXml(any(ByteArrayInputStream.class),any(), eq(LeosCategory.ANNEX), eq(permissions))).thenReturn(displayableContent);
        when(annexService.getTableOfContent(annex)).thenReturn(tableOfContentItemVoList);
        
        // DO THE ACTUAL CALL
        annexPresenter.refreshDocument(new RefreshDocumentEvent());

        verify(annexService).findAnnex(docId);
        verify(annexService).getTableOfContent(annex);
        verify(userHelper).getUser("login");
        
        verify(transformationManager).toEditableXml(any(ByteArrayInputStream.class), any(), eq(LeosCategory.ANNEX), eq(permissions));
        verify(annexScreen).setDocumentVersionInfo(any());
        verify(annexScreen).setContent(displayableContent);
        verify(annexScreen).setTitle(docTitle);
        verify(annexScreen).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(annexScreen).setPermissions(argThat(org.hamcrest.Matchers.hasProperty("id",equalTo(annexVO.getId()))));

        verifyNoMoreInteractions(userHelper, annexService, transformationManager, annexScreen);
    }

    @Test
    public void testDeleteAnnexBlock() throws Exception {
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", "","annex-id", 1, "Annex 1", docTitle);

        Annex originalDocument = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));

        byte[] updatedBytes = new byte[] {'1','2'};
        Annex savedDocument = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));

        String elementTag = "division";
        String elementId = "486";
        String userLogin="login";
        
        when(annexService.findAnnex(docId)).thenReturn(originalDocument);
        when(elementProcessor.deleteElement(originalDocument, elementId, elementTag)).thenReturn(updatedBytes);
        when(annexService.updateAnnex(originalDocument, updatedBytes, false, messageHelper.getMessage("operation.annex.block.deleted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        annexPresenter.deleteAnnexBlock(new DeleteElementRequestEvent(elementId, elementTag ));

        verify(elementProcessor).deleteElement(originalDocument, elementId, elementTag);
        verify(annexService).findAnnex(docId);
        verify(annexService).updateAnnex(originalDocument, updatedBytes, false, messageHelper.getMessage("operation.annex.block.deleted"));
        verifyNoMoreInteractions(annexService, elementProcessor);
    }

    @Test
    public void testInsertAnnexBlock_Before() throws Exception {

        boolean before = true;
        String userLogin="login";
        User user = ModelHelper.buildUser(45L, userLogin, "name");
        when(securityContext.getUser()).thenReturn(user);

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN","","annex-id", 1, "Annex 1", docTitle);

        Annex originalDocument = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));

        byte[] updatedBytes = new byte[] {'1','2'};
        Annex savedDocument = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));

        String elementId = "486";
        String elementTag = "division";

        when(annexService.findAnnex(docId)).thenReturn(originalDocument);
        when(annexProcessor.insertAnnexBlock(byteContent, elementId, elementTag, before)).thenReturn(updatedBytes);
        when(annexService.updateAnnex(originalDocument, updatedBytes, false, messageHelper.getMessage("operation.annex.block.inserted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        annexPresenter.insertAnnexBlock(new InsertElementRequestEvent(elementId, elementTag, InsertElementRequestEvent.POSITION.BEFORE));

        verify(annexProcessor).insertAnnexBlock(byteContent, elementId, elementTag, before);
        verify(annexService).findAnnex(docId);
        verify(annexService).updateAnnex(originalDocument, updatedBytes, false, messageHelper.getMessage("operation.annex.block.inserted"));
        verifyNoMoreInteractions(annexService, elementProcessor);
    }

    @Test
    public void testEditAnnexBlock() throws Exception {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", "","annex-id", 1, "Annex 1", docTitle);

        Annex document = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));
        String elementTag = "division";
        String elementId = "486";

        String contentString = "Content";

        when(annexService.findAnnex(docId)).thenReturn(document);
        when(elementProcessor.getElement(document, elementTag, elementId)).thenReturn(contentString);

        // DO THE ACTUAL CALL
        annexPresenter.editAnnexBlock(new EditElementRequestEvent(elementId, elementTag));

        verify(annexService).findAnnex(docId);
        verify(annexScreen).showElementEditor(elementId, elementTag, contentString);
        verifyNoMoreInteractions(annexService, annexScreen);
    }

    @Test
    public void testSaveAnnexBlock() throws Exception {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getByteString()).thenReturn(ByteString.of(byteContent));
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN","","annex-id", 1, "Annex 1", docTitle);

        Annex originalDocument = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));

        byte[] updatedBytes = new byte[] {'1','2'};
        
        Annex savedDocument = new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                "", "", "", true, true,
                docTitle, Collections.emptyMap(),
                Option.some(content), Option.some(annexMetadata));

        String elementTag = "division";
        String elementId = "486";
        String userLogin="login";
        
        String updatedContent = "Updated Content";

        when(annexService.findAnnex(docId)).thenReturn(originalDocument);
        when(elementProcessor.updateElement(originalDocument, updatedContent, elementTag, elementId)).thenReturn(updatedBytes);
        when(annexService.updateAnnex(originalDocument, updatedBytes, false, messageHelper.getMessage("operation.annex.block.updated"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        annexPresenter.saveElement(new SaveElementRequestEvent(elementId, elementTag, updatedContent));

        verify(elementProcessor).updateElement(originalDocument, updatedContent, elementTag, elementId);
        verify(annexService).updateAnnex(originalDocument, updatedBytes, false, messageHelper.getMessage("operation.annex.block.updated"));
        verify(annexService).findAnnex(docId);
        verifyNoMoreInteractions(annexService);
    }
}
