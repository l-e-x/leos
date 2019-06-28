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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.event.DocumentUpdatedByCoEditorEvent;
import eu.europa.ec.leos.model.event.UpdateUserInfoEvent;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.processor.AnnexProcessor;
import eu.europa.ec.leos.services.content.processor.DocumentContentService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.toc.TocRulesService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.ui.event.CloseBrowserRequestEvent;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.DoubleCompareContentRequestEvent;
import eu.europa.ec.leos.ui.event.metadata.DocumentMetadataRequest;
import eu.europa.ec.leos.ui.event.metadata.DocumentMetadataResponse;
import eu.europa.ec.leos.ui.event.metadata.SearchMetadataRequest;
import eu.europa.ec.leos.ui.event.metadata.SearchMetadataResponse;
import eu.europa.ec.leos.ui.event.toc.EditTocRequestEvent;
import eu.europa.ec.leos.ui.event.toc.SaveTocRequestEvent;
import eu.europa.ec.leos.ui.model.AnnotateMetadata;
import eu.europa.ec.leos.ui.support.CoEditionHelper;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.usecases.document.CollectionContext;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.*;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.CloseTocEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import io.atlassian.fugue.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Scope("prototype")
class AnnexPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexPresenter.class);

    private final AnnexScreen annexScreen;
    private final AnnexService annexService;
    private final ElementProcessor<Annex> elementProcessor;
    private final AnnexProcessor annexProcessor;
    private final DocumentContentService documentContentService;
    private final UrlBuilder urlBuilder;
    private final ComparisonDelegate<Annex> comparisonDelegate;
    private final UserHelper userHelper;
    private final MessageHelper messageHelper;
    private final TocRulesService tocRulesService;
    private final Provider<CollectionContext> proposalContextProvider;
    private final CoEditionHelper coEditionHelper;

    private static final String ANNEX_BLOCK_TAG = "division";
    private String strDocumentVersionSeriesId;
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    @Autowired
    AnnexPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
            AnnexScreen annexScreen,
            AnnexService annexService,
            ElementProcessor<Annex> elementProcessor, AnnexProcessor annexProcessor,
            DocumentContentService documentContentService, UrlBuilder urlBuilder,
            ComparisonDelegate<Annex> comparisonDelegate, UserHelper userHelper, 
            MessageHelper messageHelper, TocRulesService tocRulesService, 
            Provider<CollectionContext> proposalContextProvider,
            CoEditionHelper coEditionHelper, EventBus leosApplicationEventBus, UuidHelper uuidHelper) {
        super(securityContext, httpSession, eventBus, leosApplicationEventBus, uuidHelper);
        LOG.trace("Initializing annex presenter...");
        this.annexScreen = annexScreen;
        this.annexService = annexService;
        this.elementProcessor = elementProcessor;
        this.annexProcessor = annexProcessor;
        this.documentContentService = documentContentService;
        this.urlBuilder = urlBuilder;
        this.comparisonDelegate = comparisonDelegate;
        this.userHelper = userHelper;
        this.messageHelper = messageHelper;
        this.tocRulesService = tocRulesService;
        this.proposalContextProvider = proposalContextProvider;
        this.coEditionHelper = coEditionHelper;
    }
    
    @Override
    public void enter() {
        super.enter();
        populateViewData();
    }

    @Override
    public void detach() {
        super.detach();
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
    }
    
    private String getDocumentId() {
        return (String) httpSession.getAttribute(id + "." + SessionAttribute.ANNEX_ID.name());
    }

    private Annex getDocument() {
        String documentId = getDocumentId();
        Annex annex = annexService.findAnnex(documentId);
        strDocumentVersionSeriesId = annex.getVersionSeriesId();
        return annex;
    }

    private void populateViewData() {
        try{
            Annex annex = getDocument();
            Option<AnnexMetadata> annexMetadata = annex.getMetadata();
            if (annexMetadata.isDefined()) {
                annexScreen.setTitle(annexMetadata.get().getTitle());
            }
            annexScreen.setDocumentVersionInfo(getVersionInfo(annex));
            annexScreen.setContent(getEditableXml(annex));
            annexScreen.setToc(getTableOfContent(annex));
            DocumentVO annexVO = createAnnexVO(annex);
            annexScreen.setPermissions(annexVO);
            annexScreen.updateUserCoEditionInfo(coEditionHelper.getCurrentEditInfo(annex.getVersionSeriesId()), id);
        }
        catch (Exception ex) {
            LOG.error("Error while processing document", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }
 
    private List<TableOfContentItemVO> getTableOfContent(Annex annex) {
        return annexService.getTableOfContent(annex);
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Annex> event) {
        List<Annex> annexVersions = annexService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent(new ArrayList<>(annexVersions)));
    }

    @Subscribe
    void getMarkedContent(MarkedContentRequestEvent<Annex> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        String markedContent = comparisonDelegate.getMarkedContent(annexService.findAnnexVersion(oldVersionId),annexService.findAnnexVersion(newVersionId));
        annexScreen.populateMarkedContent(markedContent);
    }

    @Subscribe
    void getDoubleCompareContent(DoubleCompareContentRequestEvent<Annex> event) {
        String originalProposalVersionId = event.getOriginalProposal().getId();
        String intermediateMajorVersionId = event.getIntermediateMajor().getId();
        String currentVersionId = event.getCurrent().getId();
        String resultContent = comparisonDelegate.doubleCompareHtmlContents(annexService.findAnnexVersion(originalProposalVersionId), 
                annexService.findAnnexVersion(intermediateMajorVersionId), annexService.findAnnexVersion(currentVersionId), event.isEnabled());
        annexScreen.populateDoubleComparisonContent(resultContent);
    }
    
    private String getEditableXml(Annex document) {
        return documentContentService.toEditableContent(document,
                urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), securityContext);
    }

    private byte[] getContent(Annex annex) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        return content.getSource().getBytes();
    }

    @Subscribe
    void handleCloseScreenRequest(CloseScreenRequestEvent event) {
        LOG.trace("Handling close screen request...");
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void handleCloseBrowserRequest(CloseBrowserRequestEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
    }

    @Subscribe
    void refreshDocument(RefreshDocumentEvent event) {
        populateViewData();
    }
    
    @Subscribe
    void deleteAnnexBlock(DeleteElementRequestEvent event){
       String tagName = event.getElementTagName();
       if (ANNEX_BLOCK_TAG.equals(tagName)) { 
         Annex annex = getDocument();
         byte[] updatedXmlContent = elementProcessor.deleteElement(annex, event.getElementId(), tagName);

         // save document into repository
         annex = annexService.updateAnnex(annex, updatedXmlContent, false, messageHelper.getMessage("operation.annex.block.deleted"));
         if (annex != null) {
             eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.deleted"));
             eventBus.post(new RefreshDocumentEvent());
             eventBus.post(new DocumentUpdatedEvent());
             leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
         }
       }
    }

    @Subscribe
    void insertAnnexBlock(InsertElementRequestEvent event){
        String tagName = event.getElementTagName();
        if (ANNEX_BLOCK_TAG.equals(tagName)) {
          Annex annex = getDocument();
          byte[] updatedXmlContent = annexProcessor.insertAnnexBlock(getContent(annex), event.getElementId(), tagName, InsertElementRequestEvent.POSITION.BEFORE.equals(event.getPosition()));

          annex = annexService.updateAnnex(annex, updatedXmlContent, false, messageHelper.getMessage("operation.annex.block.inserted"));
          if (annex != null) {
              eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.inserted"));
              eventBus.post(new RefreshDocumentEvent());
              eventBus.post(new DocumentUpdatedEvent());
              leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
          }
       }
    }

    @Subscribe
    void CheckAnnexBlockCoEdition(CheckElementCoEditionEvent event) {
        annexScreen.checkElementCoEdition(coEditionHelper.getCurrentEditInfo(strDocumentVersionSeriesId), user,
                event.getElementId(), event.getElementTagName(), event.getAction(), event.getActionEvent());
    }

    @Subscribe
    void editAnnexBlock(EditElementRequestEvent event){
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        
        LOG.trace("Handling edit element request... for {},id={}",elementTagName , elementId );

        try {
            Annex annex = getDocument();
            String element = elementProcessor.getElement(annex, elementTagName, elementId);
            coEditionHelper.storeUserEditInfo(httpSession.getId(), id, user, strDocumentVersionSeriesId, elementId, InfoType.ELEMENT_INFO);
            annexScreen.showElementEditor(elementId, elementTagName, element);
        }
        catch (Exception ex){
            LOG.error("Exception while edit element operation for ", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void saveElement(SaveElementRequestEvent event){
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling save element request... for {},id={}",elementTagName , elementId );

        try {
            Annex annex = getDocument();
            byte[] updatedXmlContent = elementProcessor.updateElement(annex, event.getElementContent(), elementTagName, elementId);
            if (updatedXmlContent == null) {
                annexScreen.showAlertDialog("operation.element.not.performed");
                return;
            }

            annex = annexService.updateAnnex(annex, updatedXmlContent, false, messageHelper.getMessage("operation.annex.block.updated"));

            if (annex != null) {
                String elementContent = elementProcessor.getElement(annex, elementTagName, elementId);
                annexScreen.refreshElementEditor(elementId, elementTagName, elementContent);
                eventBus.post(new DocumentUpdatedEvent());
                eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.updated"));
                annexScreen.scrollToMarkedChange(elementId);
                leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
            }
        } catch (Exception ex) {
            LOG.error("Exception while save annex operation", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void closeAnnexBlock(CloseElementEditorEvent event){
        String elementId = event.getElementId();
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, elementId, InfoType.ELEMENT_INFO);
        LOG.debug("User edit information removed");
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    void showTimeLineWindow(ShowTimeLineWindowEvent event) {
        List documentVersions = annexService.findVersions(getDocumentId());
        annexScreen.showTimeLineWindow(documentVersions);
    }

    @Subscribe
    void versionRestore(RestoreVersionRequestEvent event) {
        String versionId = event.getVersionId();
        Annex version = annexService.findAnnexVersion(versionId);
        byte[] resultXmlContent = getContent(version);
        annexService.updateAnnex(getDocument(), resultXmlContent, false, messageHelper.getMessage("operation.restore.version", version.getVersionLabel()));

        List documentVersions = annexService.findVersions(getDocumentId());
        annexScreen.updateTimeLineWindow(documentVersions);
        eventBus.post(new RefreshDocumentEvent());
        eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
    }

    @Subscribe
    void versionCompare(ComparisonRequestEvent<Annex> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(annexService.findAnnexVersion(oldVersionId),annexService.findAnnexVersion(newVersionId), displayMode);
        annexScreen.displayComparison(result);        
    }

    @Subscribe
    public void showMajorVersionWindow(ShowMajorVersionWindowEvent event) {
        annexScreen.showMajorVersionWindow();
    }

    @Subscribe
    public void saveMajorVersion(SaveMajorVersionEvent event) {
        final Annex annex = annexService.createVersion(getDocumentId(), event.isMajor(), event.getComments());
        setAnnexDocumentId(annex.getId());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
        populateViewData();
    }
    
    private void setAnnexDocumentId(String id) {
        LOG.trace("Setting annex id in HTTP session... [id={}]", id);
        httpSession.setAttribute(this.id + "." + SessionAttribute.ANNEX_ID.name(), id);
    }

    @Subscribe
    void editToc(EditTocRequestEvent event) {
        coEditionHelper.storeUserEditInfo(httpSession.getId(), id, user, strDocumentVersionSeriesId, null, InfoType.TOC_INFO);
        annexScreen.showTocEditWindow(getTableOfContent(getDocument()), tocRulesService.getDefaultTableOfContentRules());
    }
    
    @Subscribe
    void closeToc(CloseTocEditorEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.TOC_INFO);
        LOG.debug("User edit information removed");
    }
    
    @Subscribe
    void saveToc(SaveTocRequestEvent event) {
        Annex annex = getDocument();
        annex = annexService.saveTableOfContent(annex, event.getTableOfContentItemVOs(), messageHelper.getMessage("operation.toc.updated"), user);

        List<TableOfContentItemVO> tableOfContent = getTableOfContent(annex);
        annexScreen.setToc(tableOfContent);
        eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
    }

    @Subscribe
    public void getUserPermissions(FetchUserPermissionsRequest event) {
        Annex annex = getDocument();
        List<LeosPermission> userPermissions = securityContext.getPermissions(annex);
        annexScreen.sendUserPermissions(userPermissions);
    }

    @Subscribe
    public void fetchSearchMetadata(SearchMetadataRequest event){
        eventBus.post(new SearchMetadataResponse(Collections.emptyList()));
    }

    @Subscribe
    public void fetchMetadata(DocumentMetadataRequest event){
        AnnotateMetadata metadata = new AnnotateMetadata();
        Annex annex = getDocument();
        metadata.setVersion(annex.getVersionLabel());
        metadata.setId(annex.getId());
        metadata.setTitle(annex.getTitle());
        eventBus.post(new DocumentMetadataResponse(metadata));
    }

    @Subscribe
    void mergeSuggestion(MergeSuggestionRequest event) {
        Annex document = getDocument();
        byte[] resultXmlContent = elementProcessor.replaceTextInElement(document, event.getOrigText(), event.getNewText(), event.getElementId(), event.getStartOffset(), event.getEndOffset());
        if (resultXmlContent == null) {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
            return;
        }
        document = annexService.updateAnnex(document, resultXmlContent, false, messageHelper.getMessage("operation.merge.suggestion"));
        if (document != null) {
            eventBus.post(new RefreshDocumentEvent());
            eventBus.post(new DocumentUpdatedEvent());
            leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.success"), MergeSuggestionResponse.Result.SUCCESS));
        }
        else {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
        }
    }

    private VersionInfoVO getVersionInfo(XmlDocument document) {
        String userId = document.getLastModifiedBy();
        User user = userHelper.getUser(userId);

        return new VersionInfoVO(
                document.getVersionLabel(),
                user != null ? user.getName() : userId,
                user != null ? user.getEntity() : "",
                dateFormatter.format(Date.from(document.getLastModificationInstant())),
                document.isMajorVersion());
    }

    private DocumentVO createAnnexVO(Annex annex) {
        DocumentVO annexVO =
                new DocumentVO(annex.getId(),
                        annex.getMetadata().exists(m -> m.getLanguage() != null) ? annex.getMetadata().get().getLanguage() : "EN",
                        LeosCategory.ANNEX,
                        annex.getLastModifiedBy(),
                        Date.from(annex.getLastModificationInstant()));

        if (annex.getMetadata().isDefined()) {
            AnnexMetadata metadata = annex.getMetadata().get();
            annexVO.setDocNumber(metadata.getIndex());
            annexVO.setTitle(metadata.getTitle());
        }
        if(!annex.getCollaborators().isEmpty()) {
            annexVO.addCollaborators(annex.getCollaborators());
        }
        return annexVO;
    }

    /*private Optional<CollaboratorVO> createCollaboratorVO(String login, Role role) {
        try {
            return Optional.of(new CollaboratorVO(new UserVO(userHelper.getUser(login)),role));
        } catch (Exception e) {
            LOG.error(String.format("Exception while creating collaborator VO:%s, %s",login, role), e);
            return Optional.empty();
        }
    }*/

    @Subscribe
    void updateProposalMetadata(DocumentUpdatedEvent event) {
        CollectionContext context = proposalContextProvider.get();
        context.useChildDocument(getDocumentId());
        context.useActionComment(messageHelper.getMessage("operation.metadata.updated"));
        context.executeUpdateProposalAsync();
    }

    @Subscribe
    public void onInfoUpdate(UpdateUserInfoEvent updateUserInfoEvent) {
        if(isCurrentInfoId(updateUserInfoEvent.getActionInfo().getInfo().getDocumentId())) {
            if (!id.equals(updateUserInfoEvent.getActionInfo().getInfo().getPresenterId())) {
                eventBus.post(new NotificationEvent(leosUI, "coedition.caption", "coedition.operation." + updateUserInfoEvent.getActionInfo().getOperation().getValue(),
                        NotificationEvent.Type.TRAY, updateUserInfoEvent.getActionInfo().getInfo().getUserName()));
            }
            LOG.debug("Annex Presenter updated the edit info -" + updateUserInfoEvent.getActionInfo().getOperation().name());
            annexScreen.updateUserCoEditionInfo(updateUserInfoEvent.getActionInfo().getCoEditionVos(), id);
        }
    }
    
    private boolean isCurrentInfoId(String versionSeriesId) {
        return versionSeriesId.equals(strDocumentVersionSeriesId);
    }
    
    @Subscribe
    private void documentUpdatedByCoEditor(DocumentUpdatedByCoEditorEvent documentUpdatedByCoEditorEvent) {
        if (isCurrentInfoId(documentUpdatedByCoEditorEvent.getDocumentId()) &&
                !id.equals(documentUpdatedByCoEditorEvent.getPresenterId())) {
            eventBus.post(new NotificationEvent(leosUI, "coedition.caption", "coedition.operation.update", NotificationEvent.Type.TRAY,
                    documentUpdatedByCoEditorEvent.getUser().getName()));
            annexScreen.displayDocumentUpdatedByCoEditorWarning();
        }
    }
}
