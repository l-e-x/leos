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
package eu.europa.ec.leos.ui.view.document;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.GuidanceService;
import eu.europa.ec.leos.services.content.processor.ArticleProcessor;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.ReferenceLabelService;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.content.toc.TocRulesService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.importoj.ImportService;
import eu.europa.ec.leos.ui.view.CommonDelegate;
import eu.europa.ec.leos.usecases.document.ProposalContext;
import eu.europa.ec.leos.web.model.*;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.toc.EditTocRequestEvent;
import eu.europa.ec.leos.ui.event.toc.SaveTocRequestEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.*;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@Scope("prototype")
class DocumentPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentPresenter.class);

    private final DocumentScreen documentScreen;
    private final BillService billService;
    private final ArticleProcessor articleProcessor;
    private final ElementProcessor<Bill> elementProcessor;
    private final ReferenceLabelService referenceLabelService;
    private final TransformationService transformationService;
    private final TocRulesService tocRulesService;
    private final UrlBuilder urlBuilder;
    private final ComparisonDelegate<Bill> comparisonDelegate;
    private final CommonDelegate<Bill> commonDelegate;
    private final GuidanceService guidanceService;
    private final ImportService importService;
    private final UserHelper userHelper;
    private final MessageHelper messageHelper;
    private final Provider<ProposalContext> proposalContextProvider;
    
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Autowired
    DocumentPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                      DocumentScreen documentScreen, BillService billService,
                      CommonDelegate<Bill> commonDelegate,
                      ComparisonDelegate<Bill> comparisonDelegate,
                      ArticleProcessor articleProcessor, ReferenceLabelService referenceLabelService,
                      ElementProcessor<Bill> elementProcessor, TransformationService transformationService,
                      TocRulesService tocRulesService, UrlBuilder urlBuilder, GuidanceService guidanceService,
                      ImportService importService, UserHelper userHelper, MessageHelper messageHelper, Provider<ProposalContext> proposalContextProvider) {
        super(securityContext, httpSession, eventBus);
        LOG.trace("Initializing document presenter...");
        this.documentScreen = documentScreen;
        this.billService = billService;
        this.comparisonDelegate = comparisonDelegate;
        this.commonDelegate = commonDelegate;
        this.articleProcessor = articleProcessor;
        this.elementProcessor = elementProcessor;
        this.referenceLabelService = referenceLabelService;
        this.transformationService = transformationService;
        this.tocRulesService = tocRulesService;
        this.urlBuilder = urlBuilder;
        this.guidanceService = guidanceService;
        this.importService = importService;
        this.userHelper = userHelper;
        this.messageHelper = messageHelper;
        this.proposalContextProvider = proposalContextProvider;
    }

    private byte[] getContent(Bill bill) {
        final Content content = bill.getContent().getOrError(() -> "Document content is required!");
        return content.getSource().getByteString().toByteArray();
    }

    @Override
    public void detach() {
        super.detach();
    }

    @Override
    public void enter() {
        super.enter();
        String billId = getDocumentId();
        if(billId == null ){
//            rejectView(RepositoryView.VIEW_ID, "document.id.missing");    // FIXME uncomment or replace
            return;
        }

        try {
            Bill bill = getDocument();
            populateViewWithDocumentDetails(bill);
            //TODO :dirty fix .below part needs to be rethought and enginnered again
        }catch (Exception exception){
            eventBus.post(new NotificationEvent(Type.INFO, "unknown.error.message"));
//            rejectView(RepositoryView.VIEW_ID, "unknown.error.message");      // FIXME uncomment or replace
        }
    }

    @Subscribe
    void closeDocument(CloseScreenRequestEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void refreshDocument(RefreshDocumentEvent event) throws IOException {
        Bill bill = getDocument();
        populateViewWithDocumentDetails(bill);
    }

    @Subscribe
    void editElement(EditElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();

        Bill bill = getDocument();
        String element = elementProcessor.getElement(bill, elementTagName, elementId);

        documentScreen.showElementEditor(event.getElementId(), elementTagName, element);
    }

    @Subscribe
    void saveElement(SaveElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();

        Bill bill = getDocument();
        byte[] newXmlContent = elementProcessor.updateElement(bill, event.getElementContent(), elementTagName, elementId);

        // save document into repository
        bill = billService.updateBill(bill, newXmlContent, messageHelper.getMessage("operation." + elementTagName + ".updated"));
        if (bill != null) {
            String elementContent = elementProcessor.getElement(bill, elementTagName, elementId);
            documentScreen.refreshElementEditor(elementId, elementTagName, elementContent);
            eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            eventBus.post(new DocumentUpdatedEvent());
            documentScreen.scrollToMarkedChange(elementId);
        }
    }

    @Subscribe
    void closeElementEditor(CloseElementEditorEvent event) throws IOException {
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    void deleteElement(DeleteElementRequestEvent event) throws IOException {
        String tagName = event.getElementTagName();
        if ("article".equals(tagName)) {
            Bill bill = getDocument();
            byte[] newXmlContent = articleProcessor.deleteArticle(bill, event.getElementId());
            updateBillContent(bill, newXmlContent, messageHelper.getMessage("operation.article.deleted"), "document.article.deleted");
        } else {
            throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }
    }

    @Subscribe
    void insertElement(InsertElementRequestEvent event) throws IOException {
        String tagName = event.getElementTagName();
        if ("article".equals(tagName)) {
                Bill bill = getDocument();
                byte[] newXmlContent = articleProcessor.insertNewArticle(bill, event.getElementId(), InsertElementRequestEvent.POSITION.BEFORE.equals(event.getPosition()));
                updateBillContent(bill, newXmlContent, messageHelper.getMessage("operation.article.inserted"), "document.article.inserted");
        } else {
            throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }
    }

    @Subscribe
    void editToc(EditTocRequestEvent event) {
        Bill bill = getDocument();
        documentScreen.showTocEditWindow(getTableOfContent(bill), tocRulesService.getDefaultTableOfContentRules());
    }

    @Subscribe
    void saveToc(SaveTocRequestEvent event) throws IOException {
        Bill bill = getDocument();
        bill = billService.saveTableOfContent(bill, event.getTableOfContentItemVOs(), messageHelper.getMessage("operation.toc.updated"));

        List<TableOfContentItemVO> tableOfContent = getTableOfContent(bill);
        documentScreen.setToc(tableOfContent);
        eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
        eventBus.post(new DocumentUpdatedEvent());
    }

    @Subscribe
    void fetchTocAndAncestors(FetchCrossRefTocRequestEvent event) {
        Bill bill = getDocument();
        List<String> elementAncestorsIds = null;
        if (event.getElementIds() != null && event.getElementIds().size() > 0) {
            try {
                elementAncestorsIds = billService.getAncestorsIdsForElementId(bill, event.getElementIds());
            } catch (Exception e) {
                LOG.warn("Could not get ancestors Ids", e);
            }
        }
        // we are combining two operations (get toc +  get selected element ancestors)
        documentScreen.setTocAndAncestors(getTableOfContent(bill), elementAncestorsIds);
    }

    @Subscribe
    void fetchElement(FetchElementRequestEvent event) {
        Bill bill = getDocument();
        String contentForType= elementProcessor.getElement(bill, event.getElementTagName(), event.getElementId());
        String wrappedContentXml = wrapXmlFragment(contentForType);
        InputStream contentStream = new ByteArrayInputStream(wrappedContentXml.getBytes(StandardCharsets.UTF_8));
        contentForType = transformationService.toXmlFragmentWrapper(contentStream, urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()),securityContext.getPermissions(bill));

        documentScreen.setElement(event.getElementId(), event.getElementTagName(), contentForType);
    }

    @Subscribe
    void fetchReferenceLabel(ReferenceLabelRequestEvent event) throws Exception{
        //Validate
        if(event.getReferences().size() < 1 ){
            eventBus.post(new NotificationEvent(Type.ERROR, "unknown.error.message"));
            LOG.error("No reference found in the request from client");
            return;
        }
        Bill bill = getDocument();

        //TODO : This interface is not good enough as it lose the id of ref. 
        Result<String> updatedLabel = referenceLabelService.generateLabel(event.getReferences(), event.getCurrentElementID(), getContent(bill), bill.getMetadata().get().getLanguage());

        documentScreen.setReferenceLabel(updatedLabel.get());
    }

    @Subscribe
    void mergeSuggestion(MergeSuggestionRequest event) {
        Bill bill = getDocument();
        commonDelegate.mergeSuggestion(bill, event, elementProcessor, billService::updateBill);
    }

    private String wrapXmlFragment(String xmlFragment) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aknFragment xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" + xmlFragment + "</aknFragment>";
    }
    
    private String getEditableXml(Bill document) {
        return transformationService.toEditableXml(
                    new ByteArrayInputStream(getContent(document)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), LeosCategory.BILL,  securityContext.getPermissions(document) );
    }

    private String getImportXml(String content) {
        return transformationService.toImportXml(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), securityContext.getPermissions(content));
    }

    private Bill getDocument() {
        String billId=getDocumentId();
        Bill bill = null;
        try{
            if (billId != null) {
                bill =  billService.findBill(billId);
            }
        } catch(IllegalArgumentException iae){
            LOG.debug("Document {} cannot be retrieved due to exception {}, Rejecting view", billId, iae.getMessage(), iae);
//            rejectView(RepositoryView.VIEW_ID, "document.not.found", documentId);     // FIXME uncomment or replace
        }

        return bill;
    }

    private void setDocumentId(String id) {
        LOG.trace("Setting document ID in HTTP session... [id={}]", id);
        httpSession.setAttribute(SessionAttribute.BILL_ID.name(), id);
    }

    private String getDocumentId() {
        String billId =(String) httpSession.getAttribute(SessionAttribute.BILL_ID.name());
        return billId;
    }

    private List<TableOfContentItemVO> getTableOfContent(Bill bill) {
        return billService.getTableOfContent(bill);
    }

    private void populateViewWithDocumentDetails(Bill bill) {
        if (bill != null) {
            documentScreen.setDocumentTitle(bill.getTitle());
            documentScreen.setDocumentVersionInfo(getVersionInfo(bill));
            documentScreen.refreshContent(getEditableXml(bill));
            documentScreen.setToc(getTableOfContent(bill));
            DocumentVO billVO = createLegalTextVO(bill);
            documentScreen.setPermissions(billVO);
        }
    }

    private String getUserLogin(){
        return securityContext.getUser().getLogin();
    }

    @Subscribe
    void showTimeLineWindow(ShowTimeLineWindowEvent event) {
        List documentVersions = billService.findVersions(getDocumentId());
        documentScreen.showTimeLineWindow(documentVersions);
    }
    
    @Subscribe
    void showMajorVersionWindow(ShowMajorVersionWindowEvent event) {
        documentScreen.showMajorVersionWindow();
    }

    @Subscribe
    void showImportWindow(ShowImportWindowEvent event) {
        documentScreen.showImportWindow();
    }

    @Subscribe
    void saveMajorVersion(SaveMajorVersionEvent event) {
        final Bill bill = billService.createVersion(getDocumentId(), event.isMajor(), event.getComments());
        setDocumentId(bill.getId());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        populateViewWithDocumentDetails(bill);
    }
    
    @Subscribe
    void importElements(ImportElementRequestEvent event) {
        try {
            SearchCriteriaVO serachCriteria = event.getSearchCriteria();
            List<String> elementIds = event.getElementIds();
            String aknDocument = importService.getAknDocument(serachCriteria.getType().getValue(), Integer.parseInt(serachCriteria.getYear()), Integer.parseInt(serachCriteria.getNumber()));
            if (aknDocument != null) {
                Bill bill = getDocument();
                LeosMetadata.BillMetadata metadata = bill.getMetadata().getOrError(() -> "Bill metadata is required");                
                byte[] newXmlContent = importService.insertSelectedElements(getContent(bill), aknDocument.getBytes(StandardCharsets.UTF_8), elementIds, metadata.getLanguage());
                updateBillContent(bill, newXmlContent, messageHelper.getMessage("operation.import.element.inserted"), "document.import.element.inserted");
                documentScreen.closeImportWindow();
            } else {
                eventBus.post(new NotificationEvent(Type.INFO, "document.import.no.result"));
            }
        } catch (Exception e) {
            eventBus.post(new NotificationEvent(Type.INFO, "document.import.failed"));
        }
    }
    
    private void updateBillContent(Bill bill, byte[] xmlContent, String operationMsg, String notificationMsg) {
        bill = billService.updateBill(bill, xmlContent, operationMsg);
        if (bill != null) {
            eventBus.post(new NotificationEvent(Type.INFO, notificationMsg));
            eventBus.post(new RefreshDocumentEvent());
            eventBus.post(new DocumentUpdatedEvent());
        }
    }
    
    @Subscribe
    void searchAct(SearchActRequestEvent event) {
        try {
            SearchCriteriaVO serachCriteria = event.getSearchCriteria();
            String aknDocument = importService.getAknDocument(serachCriteria.getType().getValue(), Integer.parseInt(serachCriteria.getYear()), Integer.parseInt(serachCriteria.getNumber()));
            if (aknDocument != null) {
                String transformedAknDocument = getImportXml(aknDocument);
                documentScreen.displaySearchedContent(transformedAknDocument);
            } else {
                documentScreen.displaySearchedContent(null);
                eventBus.post(new NotificationEvent(Type.INFO, "document.import.no.result"));
            }
        } catch (Exception e) {
            documentScreen.displaySearchedContent(null);
            eventBus.post(new NotificationEvent(Type.INFO, "document.import.failed"));
        }
    }

    @Subscribe
    void searchAndReplaceText(SearchAndReplaceTextEvent event) {
        Bill bill = getDocument();
        byte[] updatedContent = billService.searchAndReplaceText(getContent(bill), event.getSearchText(), event.getReplaceText());
        if(updatedContent != null) {
          // save document into repository
          bill = billService.updateBill(bill, updatedContent, messageHelper.getMessage("operation.search.replace.updated"));
          if (bill != null) {
              eventBus.post(new RefreshDocumentEvent());
              eventBus.post(new DocumentUpdatedEvent());
              eventBus.post(new NotificationEvent(Type.INFO, "document.popup.replace.success"));
          }
        } else {
            eventBus.post(new NotificationEvent(Type.INFO, "document.popup.replace.failed"));
        }
    }

    @Subscribe
    void getMarkedContent(MarkedContentRequestEvent<Bill> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        String markedContent = comparisonDelegate.getMarkedContent(billService.findBillVersion(oldVersionId),billService.findBillVersion(newVersionId));
        documentScreen.populateMarkedContent(markedContent);
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Bill> event) {
        List<Bill> billVersions = billService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent<>(new ArrayList<>(billVersions)));
    }

    @Subscribe
    void versionCompare(ComparisonRequestEvent<Bill> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(billService.findBillVersion(oldVersionId),billService.findBillVersion(newVersionId), displayMode);
        documentScreen.displayComparison(result);
    }

    @Subscribe
    public void getUserGuidance(FetchUserGuidanceRequest event) {
        // KLUGE temporary hack for compatibility with new domain model
        String documentId = getDocumentId();
        Bill bill = billService.findBill(documentId);
        String jsonGuidance = guidanceService.getGuidance(bill.getMetadata().get().getDocTemplate());
        documentScreen.setUserGuidance(jsonGuidance);
    }

    @Subscribe
    public void getUserPermissions(FetchUserPermissionsRequest event) {
        Bill bill = getDocument();
        List<LeosPermission> userPermissions = securityContext.getPermissions(bill);
        documentScreen.sendUserPermissions(userPermissions);
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

    private DocumentVO createLegalTextVO(Bill bill) {
        DocumentVO billVO = new DocumentVO(bill.getId(),
                bill.getMetadata().exists(m -> m.getLanguage() != null) ? bill.getMetadata().get().getLanguage() : "EN",
                LeosCategory.BILL,
                bill.getLastModifiedBy(),
                Date.from(bill.getLastModificationInstant()));
        
        if(!bill.getCollaborators().isEmpty()) {
            billVO.addCollaborators(bill.getCollaborators());
        }
        return billVO;
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
