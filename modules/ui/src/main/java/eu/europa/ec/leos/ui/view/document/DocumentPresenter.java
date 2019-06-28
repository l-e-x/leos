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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.event.DocumentUpdatedByCoEditorEvent;
import eu.europa.ec.leos.model.event.UpdateUserInfoEvent;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.model.xml.Element;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.ReferenceLabelService;
import eu.europa.ec.leos.services.content.TemplateConfigurationService;
import eu.europa.ec.leos.services.content.processor.BillProcessor;
import eu.europa.ec.leos.services.content.processor.DocumentContentService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.content.toc.TocRulesService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.importoj.ImportService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.ui.event.CloseBrowserRequestEvent;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.MergeElementRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.DocuWriteExportRequestEvent;
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
import eu.europa.ec.leos.ui.view.CommonDelegate;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.usecases.document.BillContext;
import eu.europa.ec.leos.usecases.document.CollectionContext;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.*;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent.Action;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.CloseTocEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.SearchCriteriaVO;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.support.xml.DownloadStreamResource;
import eu.europa.ec.leos.web.ui.navigation.Target;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.BLOCKCONTAINER;

@Component
@Scope("prototype")
class DocumentPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentPresenter.class);

    private final DocumentScreen documentScreen;
    private final BillService billService;
    private final BillProcessor billProcessor;
    private final ElementProcessor<Bill> elementProcessor;
    private final ReferenceLabelService referenceLabelService;
    private final TransformationService transformationService;
    private final TocRulesService tocRulesService;
    private final UrlBuilder urlBuilder;
    private final ComparisonDelegate<Bill> comparisonDelegate;
    private final DocumentContentService documentContentService;
    private final PackageService packageService;
    private final ExportService exportService;
    private final CommonDelegate<Bill> commonDelegate;
    private final TemplateConfigurationService templateConfigurationService;
    private final ImportService importService;
    private final UserHelper userHelper;
    private final MessageHelper messageHelper;
    private final Provider<CollectionContext> proposalContextProvider;
    private final Provider<BillContext> billContextProvider;
    private final CoEditionHelper coEditionHelper;

    private String strDocumentVersionSeriesId;
    private Element elementToEditAfterClose;

    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Autowired
    DocumentPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
            DocumentScreen documentScreen, BillService billService,
            CommonDelegate<Bill> commonDelegate,
            ComparisonDelegate<Bill> comparisonDelegate,
            DocumentContentService documentContentService, PackageService packageService, ExportService exportService,
            BillProcessor billProcessor, ReferenceLabelService referenceLabelService,
            ElementProcessor<Bill> elementProcessor, TransformationService transformationService,
            TocRulesService tocRulesService, UrlBuilder urlBuilder, TemplateConfigurationService templateConfigurationService,
            ImportService importService, UserHelper userHelper, MessageHelper messageHelper,
            Provider<CollectionContext> proposalContextProvider, Provider<BillContext> billContextProvider, CoEditionHelper coEditionHelper,
            EventBus leosApplicationEventBus, UuidHelper uuidHelper) {

        super(securityContext, httpSession, eventBus, leosApplicationEventBus, uuidHelper);
        LOG.trace("Initializing document presenter...");
        this.documentScreen = documentScreen;
        this.billService = billService;
        this.comparisonDelegate = comparisonDelegate;
        this.documentContentService = documentContentService;
        this.packageService = packageService;
        this.exportService = exportService;
        this.commonDelegate = commonDelegate;
        this.billProcessor = billProcessor;
        this.elementProcessor = elementProcessor;
        this.referenceLabelService = referenceLabelService;
        this.transformationService = transformationService;
        this.tocRulesService = tocRulesService;
        this.urlBuilder = urlBuilder;
        this.templateConfigurationService = templateConfigurationService;
        this.importService = importService;
        this.userHelper = userHelper;
        this.messageHelper = messageHelper;
        this.proposalContextProvider = proposalContextProvider;
        this.billContextProvider = billContextProvider;
        this.coEditionHelper = coEditionHelper;
    }

    private byte[] getContent(Bill bill) {
        final Content content = bill.getContent().getOrError(() -> "Document content is required!");
        return content.getSource().getBytes();
    }

    @Override
    public void enter() {
        super.enter();
        String billId = getDocumentId();
        if (billId == null) {
            // rejectView(RepositoryView.VIEW_ID, "document.id.missing"); // FIXME uncomment or replace
            return;
        }

        try {
            Bill bill = getDocument();
            populateViewWithDocumentDetails(bill);
            // TODO :dirty fix .below part needs to be rethought and enginnered again
        } catch (Exception exception) {
            eventBus.post(new NotificationEvent(Type.INFO, "unknown.error.message"));
            // rejectView(RepositoryView.VIEW_ID, "unknown.error.message"); // FIXME uncomment or replace
        }
    }

    @Override
    public void detach() {
        super.detach();
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
    }

    @Subscribe
    void closeDocument(CloseScreenRequestEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void closeDocument(CloseBrowserRequestEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
    }

    @Subscribe
    void refreshDocument(RefreshDocumentEvent event) {
        Bill bill = getDocument();
        populateViewWithDocumentDetails(bill);
    }

    @Subscribe
    void checkElementCoEdition(CheckElementCoEditionEvent event) {
        if (event.getAction().equals(Action.MERGE)) {
            Bill bill = getDocument();
            Element mergeOnElement = billProcessor.getMergeOnElement(bill, event.getElementContent(), event.getElementTagName(), event.getElementId());
            if (mergeOnElement != null) {
                documentScreen.checkElementCoEdition(coEditionHelper.getCurrentEditInfo(strDocumentVersionSeriesId), user,
                        mergeOnElement.getElementId(), mergeOnElement.getElementTagName(), event.getAction(), event.getActionEvent());
            } else {
                documentScreen.showAlertDialog("operation.element.not.performed");
            }
        } else {
            documentScreen.checkElementCoEdition(coEditionHelper.getCurrentEditInfo(strDocumentVersionSeriesId), user,
                    event.getElementId(), event.getElementTagName(), event.getAction(), event.getActionEvent());
        }
    }

    @Subscribe
    void editElement(EditElementRequestEvent event) {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        elementToEditAfterClose = null;

        Bill bill = getDocument();
        String jsonAlternatives = "";

        if("clause".equals(elementTagName)){
            jsonAlternatives = templateConfigurationService.getTempalteConfiguration(bill.getMetadata().get().getDocTemplate(),"alternatives");
        }
        String element = elementProcessor.getElement(bill, elementTagName, elementId);
        coEditionHelper.storeUserEditInfo(httpSession.getId(), id, user, strDocumentVersionSeriesId, elementId, InfoType.ELEMENT_INFO);
        documentScreen.showElementEditor(event.getElementId(), elementTagName, element, jsonAlternatives);
    }

    @Subscribe
    void saveElement(SaveElementRequestEvent event) {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        elementToEditAfterClose = null;

        Bill bill = getDocument();
        byte[] newXmlContent = elementProcessor.updateElement(bill, event.getElementContent(), elementTagName, elementId);
        if (newXmlContent == null) {
            documentScreen.showAlertDialog("operation.element.not.performed");
            return;
        }

        final String comment;
        if (elementTagName.equals(BLOCKCONTAINER)) {
            comment = messageHelper.getMessage("operation.blockContainer.updated");
        } else {
            comment = messageHelper.getMessage("operation.element.updated", StringUtils.capitalize(elementTagName));
        }
        // save document into repository
        bill = billService.updateBill(bill, newXmlContent, comment);
        if (bill != null) {
            if (!event.isSaveAndClose() && checkIfCloseElementEditor(elementTagName, event.getElementContent())) {
                elementToEditAfterClose = billProcessor.getSplittedElement(bill, elementTagName, elementId);
                eventBus.post(new CloseElementEvent());
            } else {
                String elementContent = elementProcessor.getElement(bill, elementTagName, elementId);
                documentScreen.refreshElementEditor(elementId, elementTagName, elementContent);
            }
            eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            eventBus.post(new DocumentUpdatedEvent());
            documentScreen.scrollToMarkedChange(elementId);
            leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
        }
    }

    boolean checkIfCloseElementEditor(String elementTagName, String elementContent) {
        switch (elementTagName) {
            case "subparagraph":
            case "alinea":
                return elementContent.contains("<" + elementTagName + ">");
            case "paragraph":
                return elementContent.contains("<subparagraph>");
            case "point":
                return elementContent.contains("<alinea>");
            default:
                return false;
        }
    }

    @Subscribe
    void closeElementEditor(CloseElementEditorEvent event) {
        String elementId = event.getElementId();
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, elementId, InfoType.ELEMENT_INFO);
        LOG.debug("User edit information removed");
        eventBus.post(new RefreshDocumentEvent());
        if (elementToEditAfterClose != null) {
            documentScreen.scrollTo(elementToEditAfterClose.getElementId());
            eventBus.post(new EditElementRequestEvent(elementToEditAfterClose.getElementId(), elementToEditAfterClose.getElementTagName()));
        }
    }

    @Subscribe
    void deleteElement(DeleteElementRequestEvent event) {
        String tagName = event.getElementTagName();
        Bill bill = getDocument();
        final byte[] newXmlContent;
        newXmlContent = billProcessor.deleteElement(bill, event.getElementId(), tagName, user);
        updateBillContent(bill, newXmlContent, messageHelper.getMessage("operation.element.deleted", StringUtils.capitalize(tagName)),
                "document." + tagName + ".deleted");
    }

    @Subscribe
    void insertElement(InsertElementRequestEvent event) {
        String tagName = event.getElementTagName();
        Bill bill = getDocument();
        final byte[] newXmlContent = billProcessor.insertNewElement(bill, event.getElementId(),
                InsertElementRequestEvent.POSITION.BEFORE.equals(event.getPosition()), tagName);
        updateBillContent(bill, newXmlContent, messageHelper.getMessage("operation.element.inserted", StringUtils.capitalize(tagName)),
                "document." + tagName + ".inserted");
    }

    @Subscribe
    void mergeElement(MergeElementRequestEvent event) {
        String elementId = event.getElementId();
        String tagName = event.getElementTagName();
        String elementContent = event.getElementContent();

        Bill bill = getDocument();
        Element mergeOnElement = billProcessor.getMergeOnElement(bill, elementContent, tagName, elementId);
        if (mergeOnElement != null) {
            byte[] newXmlContent = billProcessor.mergeElement(bill, elementContent, tagName, elementId);
            bill = billService.updateBill(bill, newXmlContent, messageHelper.getMessage("operation.element.updated", StringUtils.capitalize(tagName)));
            if (bill != null) {
                elementToEditAfterClose = mergeOnElement;
                eventBus.post(new CloseElementEvent());
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
                eventBus.post(new DocumentUpdatedEvent());
                leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
            }
        } else {
            documentScreen.showAlertDialog("operation.element.not.performed");
        }
    }

    @Subscribe
    void editToc(EditTocRequestEvent event) {
        Bill bill = getDocument();
        coEditionHelper.storeUserEditInfo(httpSession.getId(), id, user, strDocumentVersionSeriesId, null, InfoType.TOC_INFO);
        documentScreen.showTocEditWindow(getTableOfContent(bill, false), tocRulesService.getDefaultTableOfContentRules());
    }

    @Subscribe
    void closeToc(CloseTocEditorEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.TOC_INFO);
        LOG.debug("User edit information removed");
    }

    @Subscribe
    void saveToc(SaveTocRequestEvent event) {
        Bill bill = getDocument();
        bill = billService.saveTableOfContent(bill, event.getTableOfContentItemVOs(), messageHelper.getMessage("operation.toc.updated"), user);

        documentScreen.setTocEditWindow(getTableOfContent(bill, false));
        eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
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
        // we are combining two operations (get toc + get selected element ancestors)
        documentScreen.setTocAndAncestors(getTableOfContent(bill, true), elementAncestorsIds);
    }

    @Subscribe
    void fetchElement(FetchElementRequestEvent event) {
        Bill bill = getDocument();
        String contentForType = elementProcessor.getElement(bill, event.getElementTagName(), event.getElementId());
        String wrappedContentXml = wrapXmlFragment(contentForType != null ? contentForType : "");
        InputStream contentStream = new ByteArrayInputStream(wrappedContentXml.getBytes(StandardCharsets.UTF_8));
        contentForType = transformationService.toXmlFragmentWrapper(contentStream, urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()),
                securityContext.getPermissions(bill));

        documentScreen.setElement(event.getElementId(), event.getElementTagName(), contentForType);
    }

    @Subscribe
    void fetchReferenceLabel(ReferenceLabelRequestEvent event) throws Exception {
        // Validate
        if (event.getReferences().size() < 1) {
            eventBus.post(new NotificationEvent(Type.ERROR, "unknown.error.message"));
            LOG.error("No reference found in the request from client");
            return;
        }
        Bill bill = getDocument();

        // TODO : This interface is not good enough as it lose the id of ref.
        Result<String> updatedLabel = referenceLabelService.generateLabel(event.getReferences(), event.getCurrentElementID(), getContent(bill));

        documentScreen.setReferenceLabel(updatedLabel.get());
    }

    @Subscribe
    void mergeSuggestion(MergeSuggestionRequest event) {
        Bill bill = getDocument();
        commonDelegate.mergeSuggestion(bill, event, elementProcessor, billService::updateBill);
    }

    @Subscribe
    public void fetchMetadata(DocumentMetadataRequest event) {
        AnnotateMetadata metadata = new AnnotateMetadata();
        Bill bill = getDocument();
        metadata.setVersion(bill.getVersionLabel());
        metadata.setId(bill.getId());
        metadata.setTitle(bill.getTitle());
        eventBus.post(new DocumentMetadataResponse(metadata));
    }

    @Subscribe
    public void fetchSearchMetadata(SearchMetadataRequest event) {
        eventBus.post(new SearchMetadataResponse(Collections.emptyList()));
    }

    private String wrapXmlFragment(String xmlFragment) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aknFragment xmlns=\"http://docs.oasis-open.org/legaldocml/ns/akn/3.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" +
                xmlFragment + "</aknFragment>";
    }

    private String getEditableXml(Bill document) {
        return documentContentService.toEditableContent(document,
                urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), securityContext);
    }

    private String getImportXml(String content) {
        return transformationService.toImportXml(
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), securityContext.getPermissions(content));
    }

    private Bill getDocument() {
        String billId = getDocumentId();
        Bill bill = null;
        try {
            if (billId != null) {
                bill = billService.findBill(billId);
                strDocumentVersionSeriesId = bill.getVersionSeriesId();
            }
        } catch (IllegalArgumentException iae) {
            LOG.debug("Document {} cannot be retrieved due to exception {}, Rejecting view", billId, iae.getMessage(), iae);
            // rejectView(RepositoryView.VIEW_ID, "document.not.found", documentId); // FIXME uncomment or replace
        }

        return bill;
    }

    private void setBillDocumentId(String id) {
        LOG.trace("Setting bill ID in HTTP session... [id={}]", id);
        httpSession.setAttribute(this.id + "." + SessionAttribute.BILL_ID.name(), id);
    }

    private String getDocumentId() {
        return (String) httpSession.getAttribute(id + "." + SessionAttribute.BILL_ID.name());
    }
    
    private List<TableOfContentItemVO> getTableOfContent(Bill bill, boolean simplified) {
        return billService.getTableOfContent(bill, simplified);
    }

    private void populateViewWithDocumentDetails(Bill bill) {
        if (bill != null) {
            documentScreen.setDocumentTitle(bill.getTitle());
            documentScreen.setDocumentVersionInfo(getVersionInfo(bill));
            documentScreen.refreshContent(getEditableXml(bill));
            documentScreen.setToc(getTableOfContent(bill, true));
            DocumentVO billVO = createLegalTextVO(bill);
            documentScreen.setPermissions(billVO);
            documentScreen.updateUserCoEditionInfo(coEditionHelper.getCurrentEditInfo(bill.getVersionSeriesId()), id);
        }
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
        setBillDocumentId(bill.getId());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
        populateViewWithDocumentDetails(bill);
    }

    @Subscribe
    void importElements(ImportElementRequestEvent event) {
        try {
            SearchCriteriaVO serachCriteria = event.getSearchCriteria();
            List<String> elementIds = event.getElementIds();
            String aknDocument = importService.getAknDocument(serachCriteria.getType().getValue(), Integer.parseInt(serachCriteria.getYear()),
                    Integer.parseInt(serachCriteria.getNumber()));
            if (aknDocument != null) {
                Bill bill = getDocument();
                BillMetadata metadata = bill.getMetadata().getOrError(() -> "Bill metadata is required");
                byte[] newXmlContent = importService.insertSelectedElements(getContent(bill), aknDocument.getBytes(StandardCharsets.UTF_8), elementIds,
                        metadata.getLanguage());
                String notificationMsg = "document.import.element.inserted" + (elementIds.stream().anyMatch((s) -> s.startsWith("rec_")) ? ".recitals" : "") +
                        (elementIds.stream().anyMatch((s) -> s.startsWith("art_")) ? ".articles" : "");
                updateBillContent(bill, newXmlContent, messageHelper.getMessage("operation.import.element.inserted"), notificationMsg);
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
            leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
        }
    }

    @Subscribe
    void searchAct(SearchActRequestEvent event) {
        try {
            SearchCriteriaVO serachCriteria = event.getSearchCriteria();
            String aknDocument = importService.getAknDocument(serachCriteria.getType().getValue(), Integer.parseInt(serachCriteria.getYear()),
                    Integer.parseInt(serachCriteria.getNumber()));
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
        if (updatedContent != null) {
            // save document into repository
            bill = billService.updateBill(bill, updatedContent, messageHelper.getMessage("operation.search.replace.updated"));
            if (bill != null) {
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent());
                leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
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
        String markedContent = comparisonDelegate.getMarkedContent(billService.findBillVersion(oldVersionId), billService.findBillVersion(newVersionId));
        documentScreen.populateMarkedContent(markedContent);
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Bill> event) {
        List<Bill> billVersions = billService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent<>(new ArrayList<>(billVersions)));
    }

    @Subscribe
    void getDoubleCompareContent(DoubleCompareContentRequestEvent<Bill> event) {
        String originalProposalVersionId = event.getOriginalProposal().getId();
        String intermediateMajorVersionId = event.getIntermediateMajor().getId();
        String currentVersionId = event.getCurrent().getId();
        String resultContent = comparisonDelegate.doubleCompareHtmlContents(billService.findBillVersion(originalProposalVersionId),
                billService.findBillVersion(intermediateMajorVersionId), billService.findBillVersion(currentVersionId), event.isEnabled());
        documentScreen.populateDoubleComparisonContent(resultContent);
    }
    
    @Subscribe
    void exportToDocuWrite(DocuWriteExportRequestEvent<Bill> event) {
        ExportOptions exportOptions = event.getExportOptions();
        try {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(getDocumentId());
            BillContext context = billContextProvider.get();
            context.usePackage(leosPackage);
            String proposalId = context.getProposalIdFromBill();
            if (proposalId != null) {
                Optional<XmlDocument> versionToCompare = Optional.of(event.getVersion());
                File resultOutputFile = exportService.createDocuWritePackage("Proposal_" + proposalId + "_" + System.currentTimeMillis()+".zip", proposalId, exportOptions,
                        versionToCompare);
                DownloadStreamResource downloadStreamResource = new DownloadStreamResource(resultOutputFile.getName(), new FileInputStream(resultOutputFile));
                documentScreen.setDownloadStreamResource(downloadStreamResource);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occoured while using ExportService - {}", e.getMessage());
            eventBus.post(new NotificationEvent(Type.ERROR, "export.docuwrite.error.message", e.getMessage()));
        }
    }

    @Subscribe
    void versionRestore(RestoreVersionRequestEvent event) {
        String versionId = event.getVersionId();
        Bill version = billService.findBillVersion(versionId);
        byte[] resultXmlContent = getContent(version);
        billService.updateBill(getDocument(), resultXmlContent, messageHelper.getMessage("operation.restore.version", version.getVersionLabel()));

        List documentVersions = billService.findVersions(getDocumentId());
        documentScreen.updateTimeLineWindow(documentVersions);
        eventBus.post(new RefreshDocumentEvent());
        eventBus.post(new DocumentUpdatedEvent()); // Document might be updated.
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
    }

    @Subscribe
    void versionCompare(ComparisonRequestEvent<Bill> event) {
        String oldVersionId = event.getOldVersion().getId();
        String newVersionId = event.getNewVersion().getId();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(billService.findBillVersion(oldVersionId),
                billService.findBillVersion(newVersionId), displayMode);
        documentScreen.displayComparison(result);
    }

    @Subscribe
    public void getUserGuidance(FetchUserGuidanceRequest event) {
        // KLUGE temporary hack for compatibility with new domain model
        String documentId = getDocumentId();
        Bill bill = billService.findBill(documentId);
        String jsonGuidance = templateConfigurationService.getTempalteConfiguration(bill.getMetadata().get().getDocTemplate(),"guidance");
        documentScreen.setUserGuidance(jsonGuidance);
    }

    @Subscribe
    public void getUserPermissions(FetchUserPermissionsRequest event) {
        Bill bill = getDocument();
        List<LeosPermission> userPermissions = securityContext.getPermissions(bill);
        documentScreen.sendUserPermissions(userPermissions);
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

    private DocumentVO createLegalTextVO(Bill bill) {
        DocumentVO billVO = new DocumentVO(bill.getId(),
                bill.getMetadata().exists(m -> m.getLanguage() != null) ? bill.getMetadata().get().getLanguage() : "EN",
                LeosCategory.BILL,
                bill.getLastModifiedBy(),
                Date.from(bill.getLastModificationInstant()));

        if (!bill.getCollaborators().isEmpty()) {
            billVO.addCollaborators(bill.getCollaborators());
        }
        return billVO;
    }

    @Subscribe
    void updateProposalMetadata(DocumentUpdatedEvent event) {
        CollectionContext context = proposalContextProvider.get();
        context.useChildDocument(getDocumentId());
        context.useActionComment(messageHelper.getMessage("operation.metadata.updated"));
        context.executeUpdateProposalAsync();
    }

    @Subscribe
    public void onInfoUpdate(UpdateUserInfoEvent updateUserInfoEvent) {
        if (isCurrentInfoId(updateUserInfoEvent.getActionInfo().getInfo().getDocumentId())) {
            if (!id.equals(updateUserInfoEvent.getActionInfo().getInfo().getPresenterId())) {
                eventBus.post(new NotificationEvent(leosUI, "coedition.caption",
                        "coedition.operation." + updateUserInfoEvent.getActionInfo().getOperation().getValue(),
                        NotificationEvent.Type.TRAY, updateUserInfoEvent.getActionInfo().getInfo().getUserName()));
            }
            LOG.debug("Document Presenter updated the edit info -" + updateUserInfoEvent.getActionInfo().getOperation().name());
            documentScreen.updateUserCoEditionInfo(updateUserInfoEvent.getActionInfo().getCoEditionVos(), id);
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
            documentScreen.displayDocumentUpdatedByCoEditorWarning();
        }
    }
}