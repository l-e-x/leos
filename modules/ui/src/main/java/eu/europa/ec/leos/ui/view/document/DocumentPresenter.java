/*
 * Copyright 2017 European Commission
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
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.GuidanceService;
import eu.europa.ec.leos.services.content.RulesService;
import eu.europa.ec.leos.services.content.processor.ArticleProcessor;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.importoj.ImportService;
import eu.europa.ec.leos.services.user.UserService;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.*;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.SaveTocRequestEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.SearchCriteriaVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.ui.navigation.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
@Scope("prototype")
class DocumentPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentPresenter.class);

    private final DocumentScreen documentScreen;
    private final BillService billService;
    private final ArticleProcessor articleProcessor;
    private final ElementProcessor<Bill> elementProcessor;
    private final TransformationService transformationService;
    private final RulesService rulesService;
    private final UrlBuilder urlBuilder;
    private final ComparisonDelegate<Bill> comparisonDelegate;
    private final GuidanceService guidanceService;
    private final ImportService importService;
    private final UserService userService;
    
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Autowired
    DocumentPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                      DocumentScreen documentScreen, BillService billService, ComparisonDelegate<Bill> comparisonDelegate,
                      ArticleProcessor articleProcessor,
                      ElementProcessor<Bill> elementProcessor, TransformationService transformationService,
                      RulesService rulesService, UrlBuilder urlBuilder, GuidanceService guidanceService,
                      ImportService importService, UserService userService) {
        super(securityContext, httpSession, eventBus);
        LOG.trace("Initializing document presenter...");
        this.documentScreen = documentScreen;
        this.billService = billService;
        this.comparisonDelegate = comparisonDelegate;
        this.articleProcessor = articleProcessor;
        this.elementProcessor = elementProcessor;
        this.transformationService = transformationService;
        this.rulesService = rulesService;
        this.urlBuilder = urlBuilder;
        this.guidanceService = guidanceService;
        this.importService = importService;
        this.userService = userService;
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
        bill = billService.updateBill(bill, newXmlContent, "operation." + elementTagName + ".updated");
        if (bill != null) {
            String elementContent = elementProcessor.getElement(bill, elementTagName, elementId);
            documentScreen.refreshElementEditor(elementId, elementTagName, elementContent);
            eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            eventBus.post(new DocumentUpdatedEvent());
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
            updateBillContent(bill, newXmlContent, "document."+ tagName + ".deleted", "document."+ tagName + ".deleted");
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
                updateBillContent(bill, newXmlContent, "operation.article.inserted", "document.article.inserted");
        } else {
            throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }
    }

    @Subscribe
    void editToc(EditTocRequestEvent event) {
        Bill bill = getDocument();
        documentScreen.showTocEditWindow(getTableOfContent(bill), rulesService.getDefaultTableOfContentRules());
    }

    @Subscribe
    void saveToc(SaveTocRequestEvent event) throws IOException {
        Bill bill = getDocument();
        bill = billService.saveTableOfContent(bill, event.getTableOfContentItemVOs());

        List<TableOfContentItemVO> tableOfContent = getTableOfContent(bill);
        documentScreen.setToc(tableOfContent);
        eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
        eventBus.post(new DocumentUpdatedEvent());
    }

    @Subscribe
    void fetchTocAndAncestors(FetchCrossRefTocRequestEvent event) {
        Bill bill = getDocument();
        List<String> elementAncestorsIds = null;
        if (event.getElementId() != null) {
            elementAncestorsIds = billService.getAncestorsIdsForElementId(bill, event.getElementId());
        }
        // we are combining two operations (get toc +  get selected element ancestors)
        documentScreen.setTocAndAncestors(getTableOfContent(bill), event.getElementId(), elementAncestorsIds);
    }

    @Subscribe
    void fetchElement(FetchElementRequestEvent event) {
        Bill bill = getDocument();
        String contentForType= elementProcessor.getElement(bill, event.getElementTagName(), event.getElementId());
        String wrappedContentXml = wrapXmlFragment(contentForType);
        InputStream contentStream = new ByteArrayInputStream(wrappedContentXml.getBytes(StandardCharsets.UTF_8));
        contentForType = transformationService.toXmlFragmentWrapper(contentStream, urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()));

        documentScreen.setElement(event.getElementId(), event.getElementTagName(), contentForType);
    }
    
    private String wrapXmlFragment(String xmlFragment) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aknFragment xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" + xmlFragment + "</aknFragment>";
    }
    
    private String getEditableXml(Bill document) {
        return transformationService.toEditableXml(
                    new ByteArrayInputStream(getContent(document)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), LeosCategory.BILL);
    }

    private String getImportXml(String content) {
        return transformationService.toImportXml(
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()));
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
                byte[] newXmlContent = importService.insertSelectedElements(getContent(bill), aknDocument.getBytes(StandardCharsets.UTF_8), elementIds, (String) bill.getLanguage());
                updateBillContent(bill, newXmlContent, "operation.import.element.inserted", "document.import.element.inserted");
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
          bill = billService.updateBill(bill, updatedContent, "operation.search.replace.updated");
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
        Bill oldVersion = event.getOldVersion();
        Bill newVersion = event.getNewVersion();
        String markedContent = comparisonDelegate.getMarkedContent(oldVersion, newVersion);
        documentScreen.populateMarkedContent(markedContent);
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Bill> event) {
        List<Bill> billVersions = billService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent<>(new ArrayList<>(billVersions)));
    }

    @Subscribe
    void versionCompare(ComparisonRequestEvent<Bill> event) {
        Bill oldVersion = event.getOldVersion();
        Bill newVersion = event.getNewVersion();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(oldVersion, newVersion, displayMode);
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
    
    private VersionInfoVO getVersionInfo(XmlDocument document){
        String userId = document.getLastModifiedBy();
        User user = userService.getUser(userId);

        return  new VersionInfoVO(
                document.getVersionLabel(),
                user!=null? user.getName(): userId,
                user!=null? user.getDg(): "",
                dateFormatter.format(Date.from(document.getLastModificationInstant())),
                document.isMajorVersion());
    }
}
