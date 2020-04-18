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
package eu.europa.ec.leos.ui.view.annex;

import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.Content.Source;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.user.Entity;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.TemplateStructureService;
import eu.europa.ec.leos.services.content.processor.AnnexProcessor;
import eu.europa.ec.leos.services.content.processor.DocumentContentService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.messaging.UpdateInternalReferencesProducer;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.services.toc.StructureServiceImpl;
import eu.europa.ec.leos.test.support.model.ModelHelper;
import eu.europa.ec.leos.test.support.web.presenter.LeosPresenterTest;
import eu.europa.ec.leos.ui.support.CoEditionHelper;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo;
import eu.europa.ec.leos.vo.coedition.CoEditionActionInfo.Operation;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.toc.NumberingConfig;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.DeleteElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.EditElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.InsertElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.SaveElementRequestEvent;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import io.atlassian.fugue.Option;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Provider;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEVEL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class AnnexPresenterTest extends LeosPresenterTest {

    private static final String PRESENTER_ID = "ab51f419-c6b0-45cb-82ed-77a61099b58f";

    private SecurityContext securityContext;

    private User contextUser;

    private UuidHelper uuidHelper;

    @Mock
    private AnnexScreen annexScreen;

    @Mock
    private AnnexService annexService;

    @Mock
    private ElementProcessor<Annex> elementProcessor;

    @Mock
    private AnnexProcessor annexProcessor;

    @Mock
    private DocumentContentService documentContentService;

    @Mock
    private UrlBuilder urlBuilder;

    @Mock
    private UserHelper userHelper;

    @Mock
    private CoEditionHelper coEditionHelper;

    @Mock
    private MessageHelper messageHelper;

    @Mock
    private TemplateStructureService templateStructureService;

    @InjectMocks
    private StructureServiceImpl structureServiceImpl = Mockito.spy(new StructureServiceImpl());

    @InjectMocks
    private AnnexPresenter annexPresenter;

    @Mock
    private Provider<StructureContext> structureContextProvider;

    @Mock
    private StructureContext structureContext;

    @Mock
    private UpdateInternalReferencesProducer updateInternalReferencesProducer;

    private String docRef;
    private String docId;
    private String docTitle;
    private byte[] byteContent;

    private List<TocItem> tocItems;
    private List<NumberingConfig> numberingConfigs;
    String docTemplate = "SG-017";
    private Map<TocItem, List<TocItem>> tocRules;

    @Before
    public void setup(){
        contextUser = ModelHelper.buildUser(45L, "login", "name");
        securityContext = mock(SecurityContext.class);
        when(securityContext.getUser()).thenReturn(contextUser);
        uuidHelper = mock(UuidHelper.class);
        when(uuidHelper.getRandomUUID()).thenReturn(PRESENTER_ID);

        super.setup();

        byte[] bytesFile = getFileContent("/structure-test2.xml");
        when(templateStructureService.getStructure(docTemplate)).thenReturn(bytesFile);
        ReflectionTestUtils.setField(structureServiceImpl, "structureSchema", "toc/schema/structure_1.xsd");

        tocItems = structureServiceImpl.getTocItems(docTemplate);
        tocRules = structureServiceImpl.getTocRules(docTemplate);
        numberingConfigs = structureServiceImpl.getNumberingConfigs(docTemplate);

        when(structureContextProvider.get()).thenReturn(structureContext);
        when(structureContext.getTocItems()).thenReturn(tocItems);
        when(structureContext.getNumberingConfigs()).thenReturn(numberingConfigs);
        when(structureContext.getTocRules()).thenReturn(tocRules);
    }

    @Before
    public void init() {
        docRef = "annex_test.xml";
        docId = "555";
        docTitle = "document title";
        byteContent = new byte[]{1, 2, 3};
        when(httpSession.getAttribute(anyString() + "." + SessionAttribute.ANNEX_REF.name())).thenReturn(docRef);
    }

    @Test
    public void testEnterDocumentView() {
        Content content = mock(Content.class);
        Source source = mock(Source.class);
        String documentVersion="0.0.1";

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", docTemplate, "annex-id", 1, "Annex 1", docTitle, "", "0.0.1");
        Instant now = Instant.now();
        Annex annex = getMockedAnnex(content, documentVersion, annexMetadata);
        DocumentVO annexVO = new DocumentVO(docId,"EN", LeosCategory.ANNEX, "login",  Date.from(now));
        annexVO.setDocNumber(1);
        annexVO.setTitle(docTitle);
        annexVO.addCollaborators(Collections.emptyMap());
        String displayableContent = "document displayable content";
        List<Entity> entities = new ArrayList<Entity>();
        entities.add(new Entity("1", "DIGIT.B2", "DIGIT"));
        User user = ModelHelper.buildUser(45L, "login", "name", entities);
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        List<LeosPermission> permissions = Collections.emptyList();
        List<CoEditionVO> coEditionVos = Collections.emptyList();

        when(annexService.findAnnexByRef(docRef)).thenReturn(annex);
        when(userHelper.getUser("login")).thenReturn(user);
        when(securityContext.getPermissions(annex)).thenReturn(permissions);
        when(documentContentService.toEditableContent(isA(XmlDocument.class), any(), any())).thenReturn(displayableContent);
        when(annexService.getTableOfContent(annex, TocMode.SIMPLIFIED)).thenReturn(tableOfContentItemVoList);

        // DO THE ACTUAL CALL
        annexPresenter.enter();

        verify(annexService).findAnnexByRef(docRef);
        verify(annexService).getTableOfContent(annex, TocMode.SIMPLIFIED);
        verify(userHelper).getUser("login");

        verify(annexScreen).setDocumentVersionInfo(any());
        verify(documentContentService).toEditableContent(any(XmlDocument.class), any(), any());

        verify(annexScreen).setContent(displayableContent);
        verify(annexScreen).setTitle(docTitle);
        verify(annexScreen).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(annexScreen).setPermissions(argThat(org.hamcrest.Matchers.hasProperty("id",equalTo(annexVO.getId()))));
        verify(annexScreen).updateUserCoEditionInfo(coEditionVos, PRESENTER_ID);
        verify(annexScreen).setStructureChangeMenuItem();
        verify(annexService).getAllVersions(docId, docRef);
        verify(annexScreen).setDataFunctions(any(), any(), any(), any(), any(), any(), any());
        verifyNoMoreInteractions(userHelper, annexService, documentContentService, annexScreen);
    }
    
    private Annex getMockedAnnex(Content content, String documentVersion, AnnexMetadata annexMetadata) {
        return new Annex(docId, "Annex", "login", Instant.now(), "login", Instant.now(),
                    documentVersion, "", documentVersion, "", VersionType.MINOR, true,
                    docTitle, Collections.emptyMap(), Arrays.asList(""),
                    Option.some(content), Option.some(annexMetadata));
    }
    
    @Test
    public void testCloseDocument() {
        // DO THE ACTUAL CALL
        annexPresenter.handleCloseDocument(new CloseDocumentEvent());
        verify(eventBus).post(argThat(Matchers.<NavigationRequestEvent>hasProperty("target", equalTo(Target.PREVIOUS))));
    }

    @Test
    public void testRefreshDocument() {
        Content content = mock(Content.class);
        Source source = mock(Source.class);
        String documentVersion="0.0.1";

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);
        Instant now = Instant.now();
        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", docTemplate, "annex-id", 1, "Annex 1", docTitle, "", "0.0.1");
        Annex annex = getMockedAnnex(content, documentVersion, annexMetadata);
        DocumentVO annexVO = new DocumentVO(docId,"EN", LeosCategory.ANNEX, "login",  Date.from(now));
        annexVO.setDocNumber(1);
        annexVO.setTitle(docTitle);
        String displayableContent = "document displayable content";
        List<Entity> entities = new ArrayList<Entity>();
        entities.add(new Entity("1", "DIGIT.B2", "DIGIT"));
        User user = ModelHelper.buildUser(45L, "login", "name", entities);
        List<TableOfContentItemVO> tableOfContentItemVoList = Collections.emptyList();
        List<LeosPermission> permissions = Collections.emptyList();
        List<CoEditionVO> coEditionVos = Collections.emptyList();

        when(annexService.findAnnexByRef(docRef)).thenReturn(annex);
        when(urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest())).thenReturn("");
        when(userHelper.getUser("login")).thenReturn(user);
        when(securityContext.getPermissions(annex)).thenReturn(permissions);
        when(documentContentService.toEditableContent(isA(XmlDocument.class), any(), any())).thenReturn(displayableContent);
        when(annexService.getTableOfContent(annex, TocMode.SIMPLIFIED)).thenReturn(tableOfContentItemVoList);

        // DO THE ACTUAL CALL
        annexPresenter.refreshDocument(new RefreshDocumentEvent());

        verify(annexService).findAnnexByRef(docRef);
        verify(annexService).getTableOfContent(annex, TocMode.SIMPLIFIED);
        verify(userHelper).getUser("login");

        verify(documentContentService).toEditableContent(any(XmlDocument.class), any(), any());
        verify(annexScreen).setDocumentVersionInfo(any());
        verify(annexScreen).setContent(displayableContent);
        verify(annexScreen).setTitle(docTitle);
        verify(annexScreen).setToc(argThat(sameInstance(tableOfContentItemVoList)));
        verify(annexScreen).setPermissions(argThat(org.hamcrest.Matchers.hasProperty("id",equalTo(annexVO.getId()))));
        verify(annexScreen).updateUserCoEditionInfo(coEditionVos, PRESENTER_ID);
        verify(annexScreen).setStructureChangeMenuItem();
        verifyNoMoreInteractions(userHelper, annexService, documentContentService, annexScreen);
    }

    @Test
    public void testDeleteAnnexBlock() {
        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", docTemplate, "annex-id", 1, "Annex 1", docTitle, "", "0.0.1");
    
        Annex originalDocument = getMockedAnnex(content, "", annexMetadata);
    
        byte[] updatedBytes = new byte[] {'1','2'};
        Annex savedDocument = getMockedAnnex(content, "", annexMetadata);
    
        String elementTag = LEVEL;
        String elementId = "486";

        when(annexService.findAnnexByRef(docRef)).thenReturn(originalDocument);
        when(annexProcessor.deleteAnnexBlock(originalDocument, elementId, elementTag)).thenReturn(updatedBytes);
        when(annexService.updateAnnex(originalDocument, updatedBytes, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.deleted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        annexPresenter.deleteAnnexBlock(new DeleteElementRequestEvent(elementId, elementTag ));

        verify(annexProcessor).deleteAnnexBlock(originalDocument, elementId, elementTag);
        verify(annexService).findAnnexByRef(docRef);
        verify(annexService).updateAnnex(originalDocument, updatedBytes, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.deleted"));
        verifyNoMoreInteractions(annexService, elementProcessor);
    }

    @Test
    public void testInsertAnnexBlock_Before() {

        boolean before = true;

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", docTemplate, "annex-id", 1, "Annex 1", docTitle, "", "0.0.1");
    
        Annex originalDocument = getMockedAnnex(content, "", annexMetadata);
    
        byte[] updatedBytes = new byte[] {'1','2'};
        Annex savedDocument = getMockedAnnex(content, "", annexMetadata);
    
        String elementId = "486";
        String elementTag = LEVEL;

        when(annexService.findAnnexByRef(docRef)).thenReturn(originalDocument);
        when(annexProcessor.insertAnnexBlock(originalDocument, elementId, elementTag, before)).thenReturn(updatedBytes);
        when(annexService.updateAnnex(originalDocument, updatedBytes, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.inserted"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        annexPresenter.insertAnnexBlock(new InsertElementRequestEvent(elementId, elementTag, InsertElementRequestEvent.POSITION.BEFORE));

        verify(annexProcessor).insertAnnexBlock(originalDocument, elementId, elementTag, before);
        verify(annexService).findAnnexByRef(docRef);
        verify(annexService).updateAnnex(originalDocument, updatedBytes, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.inserted"));
        verifyNoMoreInteractions(annexService, elementProcessor);
    }

    @Test
    public void testEditAnnexBlock() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", docTemplate, "annex-id", 1, "Annex 1", docTitle, "", "0.0.1");
    
        Annex document = getMockedAnnex(content, "", annexMetadata);
        String elementTag = LEVEL;
        String elementId = "486";
        CoEditionActionInfo actionInfo = new CoEditionActionInfo(true, Operation.STORE, null, new ArrayList<CoEditionVO>());
        String contentString = "Content";

        when(annexService.findAnnexByRef(docRef)).thenReturn(document);
        when(elementProcessor.getElement(document, elementTag, elementId)).thenReturn(contentString);
        when(coEditionHelper.getCurrentEditInfo("test")).thenReturn(actionInfo.getCoEditionVos());

        // DO THE ACTUAL CALL
        annexPresenter.editAnnexBlock(new EditElementRequestEvent(elementId, elementTag));

        verify(annexService).findAnnexByRef(docRef);
        verify(annexScreen).showElementEditor(elementId, elementTag, contentString);
        verifyNoMoreInteractions(annexService, annexScreen);
    }
    
    @Test
    public void testSaveAnnexBlock() {

        Content content = mock(Content.class);
        Source source = mock(Source.class);

        when(source.getBytes()).thenReturn(byteContent);
        when(content.getSource()).thenReturn(source);

        AnnexMetadata annexMetadata = new AnnexMetadata("", "REGULATION", "", "AN-000.xml", "EN", docTemplate, "annex-id", 1, "Annex 1", docTitle, "", "0.0.1");
    
        Annex originalDocument = getMockedAnnex(content, "", annexMetadata);
    
        byte[] updatedBytes = new byte[] {'1','2'};
    
        Annex savedDocument = getMockedAnnex(content, "", annexMetadata);
    
        String elementTag = LEVEL;
        String elementId = "486";
        String updatedContent = "Updated Content";

        when(annexService.findAnnexByRef(docRef)).thenReturn(originalDocument);
        when(elementProcessor.updateElement(originalDocument, updatedContent, elementTag, elementId)).thenReturn(updatedBytes);
        when(annexService.updateAnnex(originalDocument, updatedBytes, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.updated"))).thenReturn(savedDocument);

        // DO THE ACTUAL CALL
        annexPresenter.saveElement(new SaveElementRequestEvent(elementId, elementTag, updatedContent, false));

        verify(elementProcessor).updateElement(originalDocument, updatedContent, elementTag, elementId);
        verify(annexService).updateAnnex(originalDocument, updatedBytes, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.updated"));
        verify(annexService).findAnnexByRef(docRef);
        verifyNoMoreInteractions(annexService);
    }

    public byte[] getFileContent(String fileName) {
        try {
            InputStream inputStream = this.getClass().getResource(fileName).openStream();
            byte[] content = new byte[inputStream.available()];
            inputStream.read(content);
            inputStream.close();
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read bytes from file: " + fileName);
        }
    }
}