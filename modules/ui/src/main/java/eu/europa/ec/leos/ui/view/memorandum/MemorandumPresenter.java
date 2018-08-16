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
package eu.europa.ec.leos.ui.view.memorandum;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.GuidanceService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.MemorandumService;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.usecases.document.ProposalContext;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.ComparisonRequestEvent;
import eu.europa.ec.leos.web.event.component.MarkedContentRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Scope("prototype")
class MemorandumPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(MemorandumPresenter.class);

    private final MemorandumScreen memorandumScreen;
    private final MemorandumService memorandumService;
    private final ElementProcessor<Memorandum> elementProcessor;
    private final TransformationService transformationService;
    private final UrlBuilder urlBuilder;
    private final GuidanceService guidanceService;
    private final ComparisonDelegate<Memorandum> comparisonDelegate;
    private final UserHelper userHelper;
    private final MessageHelper messageHelper;
    private final Provider<ProposalContext> proposalContextProvider;
    
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Autowired
    MemorandumPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                        MemorandumScreen memorandumScreen,
                        MemorandumService memorandumService,
                        ElementProcessor<Memorandum> elementProcessor,
                        TransformationService transformationService,
                        UrlBuilder urlBuilder,
                        GuidanceService guidanceService,
                        ComparisonDelegate<Memorandum> comparisonDelegate,
                        UserHelper userHelper, MessageHelper messageHelper, Provider<ProposalContext> proposalContextProvider) {
        super(securityContext, httpSession, eventBus);
        LOG.trace("Initializing memorandum presenter...");
        this.memorandumScreen = memorandumScreen;
        this.memorandumService = memorandumService;
        this.elementProcessor = elementProcessor;
        this.transformationService = transformationService;
        this.urlBuilder = urlBuilder;
        this.guidanceService = guidanceService;
        this.comparisonDelegate = comparisonDelegate;
        this.userHelper = userHelper;
        this.messageHelper = messageHelper;
        this.proposalContextProvider = proposalContextProvider;
    }

    @Override
    public void enter() {
        super.enter();
        populateViewData();
    }

    private String getDocumentId() {
        return (String) httpSession.getAttribute(SessionAttribute.MEMORANDUM_ID.name());
    }

    private Memorandum getDocument() {
        String documentId = getDocumentId();
        return memorandumService.findMemorandum(documentId);
    }

    private void populateViewData() {
        try{
            Memorandum memorandum = getDocument();
            memorandumScreen.setTitle("Explanatory Memorandum"); //FIXME Temporary implementation waiting for Memorandum title feature development
            memorandumScreen.setDocumentVersionInfo(getVersionInfo(memorandum));
            String content = getEditableXml(memorandum);
            memorandumScreen.setContent(content);
            memorandumScreen.setToc(getTableOfContent(memorandum));
            DocumentVO memorandumVO = createMemorandumVO(memorandum);
            memorandumScreen.setPermissions(memorandumVO);
        }
        catch (Exception ex) {
            LOG.error("Error while processing document", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    private List<TableOfContentItemVO> getTableOfContent(Memorandum memorandum) {
        return memorandumService.getTableOfContent(memorandum);
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Memorandum> event) {
        List<Memorandum> memoVersions = memorandumService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent(new ArrayList<>(memoVersions)));
    }

    @Subscribe
    void getMarkedContent(MarkedContentRequestEvent<Memorandum> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        String markedContent = comparisonDelegate.getMarkedContent(memorandumService.findMemorandumVersion(oldVersionId), memorandumService.findMemorandumVersion(newVersionId));
        memorandumScreen.populateMarkedContent(markedContent);
    }

    private String getEditableXml(Memorandum memorandum) {
        securityContext.getPermissions(memorandum);
        return transformationService.toEditableXml(
                    new ByteArrayInputStream(getContent(memorandum)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), LeosCategory.MEMORANDUM, securityContext.getPermissions(memorandum));
    }

    @Subscribe
    void handleCloseScreenRequest(CloseScreenRequestEvent event) {
        LOG.trace("Handling close screen request...");
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void refreshDocument(RefreshDocumentEvent event) throws IOException {
        populateViewData();
    }

    @Subscribe
    void editElement(EditElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling edit element request... for {},id={}",elementTagName , elementId );

        try {
            Memorandum memorandum = getDocument();
            String element = elementProcessor.getElement(memorandum, elementTagName, elementId);
            memorandumScreen.showElementEditor(elementId, elementTagName, element);
        }
        catch (Exception ex){
            LOG.error("Exception while edit element operation for memorandum", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void saveElement(SaveElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling save element request... for {},id={}",elementTagName , elementId );

        try {
            Memorandum memorandum = getDocument();
            byte[] newXmlContent = elementProcessor.updateElement(memorandum, event.getElementContent(), elementTagName, elementId);

            memorandum = memorandumService.updateMemorandum(memorandum, newXmlContent, false, messageHelper.getMessage("operation." + elementTagName + ".updated"));

            if (memorandum != null) {
                String elementContent = elementProcessor.getElement(memorandum, elementTagName, elementId);
                memorandumScreen.refreshElementEditor(elementId, elementTagName, elementContent);
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
                memorandumScreen.scrollToMarkedChange(elementId);
            }
        } catch (Exception ex) {
            LOG.error("Exception while save  memorandum operation", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void closeElementEditor(CloseElementEditorEvent event) throws IOException {
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    public void getUserGuidance(FetchUserGuidanceRequest event) {
        // KLUGE temporary hack for compatibility with new domain model
        String documentId = getDocumentId();
        Memorandum memorandum = memorandumService.findMemorandum(documentId);
        String jsonGuidance = guidanceService.getGuidance(memorandum.getMetadata().get().getDocTemplate());
        memorandumScreen.setUserGuidance(jsonGuidance);
    }

    @Subscribe
    void mergeSuggestion(MergeSuggestionRequest event) {
        Memorandum document = getDocument();
        byte[] resultXmlContent = elementProcessor.replaceTextInElement(document, event.getOrigText(), event.getNewText(), event.getElementId(), event.getStartOffset(), event.getEndOffset());
        if (resultXmlContent == null) {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
            return;
        }
        document = memorandumService.updateMemorandum(document, resultXmlContent, false, messageHelper.getMessage("operation.merge.suggestion"));
        if (document != null) {
            eventBus.post(new RefreshDocumentEvent());
            eventBus.post(new DocumentUpdatedEvent());
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.success"), MergeSuggestionResponse.Result.SUCCESS));
        }
        else {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
        }
    }

    @Subscribe
    public void getUserPermissions(FetchUserPermissionsRequest event) {
        Memorandum memorandum = getDocument();
        List<LeosPermission> userPermissions = securityContext.getPermissions(memorandum);
        memorandumScreen.sendUserPermissions(userPermissions);
    }

    @Subscribe
    void showTimeLineWindow(ShowTimeLineWindowEvent event) {
        List documentVersions = memorandumService.findVersions(getDocumentId());
        memorandumScreen.showTimeLineWindow(documentVersions);
    }

    @Subscribe
    void versionCompare(ComparisonRequestEvent<Memorandum> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(memorandumService.findMemorandumVersion(oldVersionId), memorandumService.findMemorandumVersion(newVersionId), displayMode);
        memorandumScreen.displayComparison(result);        
    }

    @Subscribe
    public void showMajorVersionWindow(ShowMajorVersionWindowEvent event) {
        memorandumScreen.showMajorVersionWindow();
    }

    @Subscribe
    public void saveMajorVersion(SaveMajorVersionEvent event) {
        memorandumService.createVersion(getDocumentId(), event.isMajor(), event.getComments());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        populateViewData();
    }

    private byte[] getContent(Memorandum memorandum) {
        final Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        return content.getSource().getByteString().toByteArray();
    }

    private VersionInfoVO getVersionInfo(XmlDocument document){
        String userId = document.getLastModifiedBy();
        User user = userHelper.getUser(userId);

        return  new VersionInfoVO(
                document.getVersionLabel(),
                user!=null? user.getName(): userId,
                user!=null? user.getEntity(): "",
                dateFormatter.format(Date.from(document.getLastModificationInstant())),
                document.isMajorVersion());
    }

    private DocumentVO createMemorandumVO(Memorandum memorandum) {
        DocumentVO memorandumVO = new DocumentVO(memorandum.getId(),
                memorandum.getMetadata().exists(m -> m.getLanguage() != null) ? memorandum.getMetadata().get().getLanguage() : "EN",
                LeosCategory.MEMORANDUM,
                memorandum.getLastModifiedBy(),
                Date.from(memorandum.getLastModificationInstant()));

        if(!memorandum.getCollaborators().isEmpty()) {
            memorandumVO.addCollaborators(memorandum.getCollaborators());
        }
        
        return memorandumVO;
    }

    private Optional<CollaboratorVO> createCollaboratorVO(String login, LeosAuthority authority) {
        try {
            return Optional.of(new CollaboratorVO(new UserVO(userHelper.getUser(login)),authority));
        } catch (Exception e) {
            LOG.error(String.format("Exception while creating collaborator VO:%s, %s",login, authority), e);
            return Optional.empty();
        }
    }

    @Subscribe
    void updateProposalMetadata(DocumentUpdatedEvent event) {
        ProposalContext context = proposalContextProvider.get();
        context.useChildDocument(getDocumentId());
        context.executeUpdateProposalAsync();
    }
}