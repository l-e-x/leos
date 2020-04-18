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
package eu.europa.ec.leos.ui.view.collection;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.Memorandum;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.event.MilestoneCreatedEvent;
import eu.europa.ec.leos.model.event.MilestoneUpdatedEvent;
import eu.europa.ec.leos.model.event.UpdateUserInfoEvent;
import eu.europa.ec.leos.model.messaging.UpdateInternalReferencesMessage;
import eu.europa.ec.leos.model.notification.collaborators.AddCollaborator;
import eu.europa.ec.leos.model.notification.collaborators.EditCollaborator;
import eu.europa.ec.leos.model.notification.collaborators.RemoveCollaborator;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.permissions.Role;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.document.ProposalService;
import eu.europa.ec.leos.services.document.SecurityService;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.milestone.MilestoneService;
import eu.europa.ec.leos.services.notification.NotificationService;
import eu.europa.ec.leos.services.messaging.UpdateInternalReferencesProducer;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.CreateMilestoneEvent;
import eu.europa.ec.leos.ui.event.CreateMilestoneRequestEvent;
import eu.europa.ec.leos.ui.event.FetchMilestoneEvent;
import eu.europa.ec.leos.ui.event.view.collection.AddCollaboratorRequest;
import eu.europa.ec.leos.ui.event.view.collection.CreateAnnexRequest;
import eu.europa.ec.leos.ui.event.view.collection.DeleteAnnexEvent;
import eu.europa.ec.leos.ui.event.view.collection.DeleteAnnexRequest;
import eu.europa.ec.leos.ui.event.view.collection.DeleteCollectionEvent;
import eu.europa.ec.leos.ui.event.view.collection.DeleteCollectionRequest;
import eu.europa.ec.leos.ui.event.view.collection.DownloadMandateEvent;
import eu.europa.ec.leos.ui.event.view.collection.DownloadProposalEvent;
import eu.europa.ec.leos.ui.event.view.collection.EditCollaboratorRequest;
import eu.europa.ec.leos.ui.event.view.collection.ExportMandateEvent;
import eu.europa.ec.leos.ui.event.view.collection.ExportProposalEvent;
import eu.europa.ec.leos.ui.event.view.collection.RemoveCollaboratorRequest;
import eu.europa.ec.leos.ui.event.view.collection.SaveAnnexMetaDataRequest;
import eu.europa.ec.leos.ui.event.view.collection.SearchUserRequest;
import eu.europa.ec.leos.ui.model.MilestonesVO;
import eu.europa.ec.leos.ui.support.CoEditionHelper;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.window.milestone.MilestoneExplorer;
import eu.europa.ec.leos.usecases.document.BillContext;
import eu.europa.ec.leos.usecases.document.CollectionContext;
import eu.europa.ec.leos.usecases.document.ContextAction;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.component.WindowClosedEvent;
import eu.europa.ec.leos.web.event.view.annex.OpenAnnexEvent;
import eu.europa.ec.leos.web.event.view.document.CollaboratorsUpdatedEvent;
import eu.europa.ec.leos.web.event.view.document.CollaboratorsUpdatedEvent.Operation;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.event.view.document.OpenLegalTextEvent;
import eu.europa.ec.leos.web.event.view.memorandum.OpenMemorandumEvent;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.model.UserVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.support.xml.DownloadStreamResource;
import eu.europa.ec.leos.web.ui.component.MoveAnnexEvent;
import eu.europa.ec.leos.web.ui.navigation.Target;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import javax.xml.ws.WebServiceException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import static eu.europa.ec.leos.security.LeosPermission.CAN_ADD_REMOVE_COLLABORATOR;

@Component
@Scope("prototype")
class CollectionPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(CollectionPresenter.class);

    private final CollectionScreen collectionScreen;
    private final Provider<CollectionContext> proposalContextProvider;
    private final Provider<BillContext> billContextProvider;
    private final AnnexService annexService;
    private final BillService billService;
    private final PackageService packageService;
    private final UserHelper userHelper;
    private final ExportService exportService;
    private final MilestoneService milestoneService;
    private final SecurityService securityService;
    private final NotificationService notificationService;
    private final TemplateService templateService;
    private final MessageHelper messageHelper;
    private final CoEditionHelper coEditionHelper;
    private final LeosPermissionAuthorityMapHelper authorityMapHelper;
    private final ProposalService proposalService;
    private final UpdateInternalReferencesProducer updateInternalReferencesProducer;

    private String proposalId;
    private String proposalVersionSeriesId;
    private Set<MilestonesVO> milestonesVOs = new TreeSet<>(Comparator.comparing(MilestonesVO::getUpdatedDate).reversed());
    private final StampedLock milestonesVOsLock = new StampedLock();
    private Set<String> docVersionSeriesIds;

    private static final String DOWNLOAD_PREFIX = "download";

    @Autowired CollectionPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                                   EventBus leosApplicationEventBus,
                                   CollectionScreen collectionScreen,
                                   Provider<CollectionContext> proposalContextProvider,
                                   Provider<BillContext> billContextProvider,
                                   AnnexService annexService,
                                   BillService billService,
                                   PackageService packageService,
                                   MilestoneService milestoneService,
                                   UserHelper userHelper,
                                   ExportService exportService,
                                   SecurityService securityService,
                                   NotificationService notificationService, MessageHelper messageHelper,
                                   TemplateService templateService, CoEditionHelper coEditionHelper,
                                   LeosPermissionAuthorityMapHelper authorityMapHelper, UuidHelper uuidHelper, ProposalService proposalService, WorkspaceService workspaceService,
                                   UpdateInternalReferencesProducer updateInternalReferencesProducer) {
        
        super(securityContext, httpSession, eventBus, leosApplicationEventBus, uuidHelper, packageService, workspaceService);
        
        this.collectionScreen = collectionScreen;
        this.proposalContextProvider = proposalContextProvider;
        this.billContextProvider = billContextProvider;
        this.annexService = annexService;
        this.billService = billService;
        this.packageService = packageService;
        this.milestoneService = milestoneService;
        this.userHelper = userHelper;
        this.exportService = exportService;
        this.securityService = securityService;
        this.notificationService = notificationService;
        this.messageHelper = messageHelper;
        this.templateService = templateService;
        this.coEditionHelper = coEditionHelper;
        this.authorityMapHelper = authorityMapHelper;
        this.proposalService = proposalService;
        this.updateInternalReferencesProducer = updateInternalReferencesProducer;
    }
    
    @Override
    public void enter() {
        super.enter();
        populateData();
    }
    
    private void populateData() {
        proposalId = getProposalRef() != null ? proposalService.findProposalByRef(getProposalRef()).getId() : proposalId;
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        List<XmlDocument> documents = packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class, false);
        List<LegDocument> legDocuments = packageService.findDocumentsByPackageId(leosPackage.getId(), LegDocument.class, false, false);
        legDocuments.sort(Comparator.comparing(LegDocument::getLastModificationInstant).reversed());
        
        DocumentVO proposalVO = createViewObject(documents);
        collectionScreen.populateData(proposalVO);

        long stamp = milestonesVOsLock.writeLock();
        try {
            milestonesVOs.clear();
            legDocuments.forEach(document -> milestonesVOs.add(getMilestonesVO(document)));
            collectionScreen.populateMilestones(milestonesVOs);
        } finally {
            milestonesVOsLock.unlockWrite(stamp);
        }

        collectionScreen.updateUserCoEditionInfo(coEditionHelper.getAllEditInfo(), user);
    }

    private DocumentVO createViewObject(List<XmlDocument> documents) {
        DocumentVO proposalVO = new DocumentVO(LeosCategory.PROPOSAL);
        List<DocumentVO> annexVOList = new ArrayList<>();
        docVersionSeriesIds = new HashSet<>();
        //We have the latest version of the document, no need to search for them again
        for (XmlDocument document : documents) {
            switch (document.getCategory()) {
                case PROPOSAL: {
                    Proposal proposal = (Proposal) document;
                    MetadataVO metadataVO = createMetadataVO(proposal);
                    proposalVO.setMetaData(metadataVO);
                    proposalVO.addCollaborators(proposal.getCollaborators());
                    proposalVersionSeriesId = proposal.getVersionSeriesId();
                    break;
                }
                case MEMORANDUM: {
                    Memorandum memorandum = (Memorandum) document;
                    DocumentVO memorandumVO = getMemorandumVO(memorandum);
                    proposalVO.addChildDocument(memorandumVO);
                    memorandumVO.addCollaborators(memorandum.getCollaborators());
                    memorandumVO.getMetadata().setInternalRef(memorandum.getMetadata().getOrError(() -> "Memorandum metadata is not available!").getRef());
                    memorandumVO.setVersionSeriesId(memorandum.getVersionSeriesId());
                    docVersionSeriesIds.add(memorandum.getVersionSeriesId());
                    break;
                }
                case BILL: {
                    Bill bill = (Bill) document;
                    DocumentVO billVO = getLegalTextVO(bill);
                    proposalVO.addChildDocument(billVO);
                    billVO.addCollaborators(bill.getCollaborators());
                    billVO.getMetadata().setInternalRef(bill.getMetadata().getOrError(() -> "Legal text metadata is not available!").getRef());
                    billVO.setVersionSeriesId(bill.getVersionSeriesId());
                    docVersionSeriesIds.add(bill.getVersionSeriesId());
                    break;
                }
                case ANNEX: {
                    Annex annex = (Annex) document;
                    DocumentVO annexVO = createAnnexVO(annex);
                    annexVO.addCollaborators(annex.getCollaborators());
                    annexVO.getMetadata().setInternalRef(annex.getMetadata().getOrError(() -> "Annex metadata is not available!").getRef());
                    annexVOList.add(annexVO);
                    annexVO.setVersionSeriesId(annex.getVersionSeriesId());
                    docVersionSeriesIds.add(annex.getVersionSeriesId());
                    break;
                }
                default:
                    LOG.debug("Do nothing for rest of the categories like MEDIA, CONFIG & LEG");
                    break;
            }
        }

        annexVOList.sort(Comparator.comparingInt(DocumentVO::getDocNumber));
        DocumentVO legalText = proposalVO.getChildDocument(LeosCategory.BILL);
        if (legalText != null) {
            for (DocumentVO annexVO : annexVOList) {
                legalText.addChildDocument(annexVO);
            }
        }

        return proposalVO;
    }
    
    private MilestonesVO getMilestonesVO(LegDocument legDocument) {
        return new MilestonesVO(legDocument.getMilestoneComments(),
                         Date.from(legDocument.getCreationInstant()),
                         Date.from(legDocument.getLastModificationInstant()),
                         messageHelper.getMessage( "milestones.column.status.value." + legDocument.getStatus().name()),
                         legDocument.getName());
    }

    // FIXME refine
    private DocumentVO getMemorandumVO(Memorandum memorandum) {
        return new DocumentVO(memorandum.getId(),
                         memorandum.getMetadata().exists(m -> m.getLanguage() != null) ? memorandum.getMetadata().get().getLanguage() : "EN",
                         LeosCategory.MEMORANDUM,
                         memorandum.getLastModifiedBy(),
                         Date.from(memorandum.getLastModificationInstant()));
    }

    // FIXME refine
    private DocumentVO getLegalTextVO(Bill bill) {
        return new DocumentVO(bill.getId(),
                         bill.getMetadata().exists(m -> m.getLanguage() != null) ? bill.getMetadata().get().getLanguage() : "EN",
                         LeosCategory.BILL,
                         bill.getLastModifiedBy(),
                         Date.from(bill.getLastModificationInstant()));
    }

    // FIXME refine
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

        return annexVO;
    }

    private MetadataVO createMetadataVO(Proposal proposal) {
        ProposalMetadata metadata = proposal.getMetadata().getOrError(() -> "Proposal metadata is not available!");
        return new MetadataVO(metadata.getStage(), metadata.getType(), metadata.getPurpose(), metadata.getTemplate(), metadata.getLanguage());
    }

    @Subscribe
    void saveMetaData(SaveMetaDataRequestEvent event) {
        LOG.trace("Saving proposal metadata...");
        CollectionContext context = proposalContextProvider.get();
        context.useProposal(proposalId);
        context.usePurpose(event.getMetaDataVO().getDocPurpose());
        String comment = messageHelper.getMessage("operation.metadata.updated");
        context.useActionMessage(ContextAction.METADATA_UPDATED, comment);
        context.useActionComment(comment);
        context.executeUpdateProposal();
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "metadata.edit.saved"));
        // TODO optimize refresh of data on the screen
        // populateData();
    }

    @Subscribe
    void saveAnnexMetaData(SaveAnnexMetaDataRequest event) {
        // 1. get Annex
        Annex annex = annexService.findAnnex(event.getAnnex().getId());
        AnnexMetadata metadata = annex.getMetadata().getOrError(()-> "Annex metadata not found!");
        AnnexMetadata updatedMetadata = metadata.withTitle(event.getAnnex().getTitle());

        // 2. save metadata
        annexService.updateAnnex(annex, updatedMetadata, VersionType.MINOR, messageHelper.getMessage("collection.block.annex.metadata.updated"));
        eventBus.post(new DocumentUpdatedEvent());
        // 3.update ui
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collection.block.annex.metadata.updated"));
        populateData();
    }

    @Subscribe
    void createAnnex(CreateAnnexRequest event) {
        // KLUGE temporary workaround
        try {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
            Bill bill = billService.findBillByPackagePath(leosPackage.getPath());
            BillMetadata metadata = bill.getMetadata().getOrError(() -> "Bill metadata is required!");
            BillContext billContext = billContextProvider.get();
            billContext.usePackage(leosPackage);
            billContext.useTemplate(bill);
            billContext.usePurpose(metadata.getPurpose());
            billContext.useActionMessage(ContextAction.ANNEX_METADATA_UPDATED, messageHelper.getMessage("collection.block.annex.metadata.updated"));
            billContext.useActionMessage(ContextAction.ANNEX_ADDED, messageHelper.getMessage("collection.block.annex.added"));
            billContext.useActionMessage(ContextAction.DOCUMENT_CREATED, messageHelper.getMessage("operation.document.created"));
            // KLUGE temporary workaround
            // We will have to change the ui to allow the user to select the annex child template that s/he wants.
            CatalogItem templateItem = templateService.getTemplateItem(metadata.getDocTemplate());
            String annexTemplate = templateItem.getItems().get(0).getId();
            billContext.useAnnexTemplate(annexTemplate);
            billContext.executeCreateBillAnnex();
            eventBus.post(new DocumentUpdatedEvent());
            populateData();
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while creating the annex", e);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collection.block.annex.add.error", e.getMessage()));
        }
    }

    @Subscribe
    void deleteAnnexRequest(DeleteAnnexRequest event) {
        collectionScreen.confirmAnnexDeletion(event.getAnnex());
    }

    @Subscribe
    void deleteAnnex(DeleteAnnexEvent event) {
        // FIXME To be implemented, this part is commented: add a confirmation dialog?
        // 1. delete Annex
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);

        BillContext billContext = billContextProvider.get();
        billContext.useAnnex(event.getAnnex().getId());
        billContext.usePackage(leosPackage);
        billContext.useActionMessage(ContextAction.ANNEX_METADATA_UPDATED, messageHelper.getMessage("collection.block.annex.metadata.updated"));
        billContext.useActionMessage(ContextAction.ANNEX_DELETED, messageHelper.getMessage("collection.block.annex.removed"));
        billContext.executeRemoveBillAnnex();
        updateInternalReferencesProducer.send(new UpdateInternalReferencesMessage(proposalId, event.getAnnex().getMetadata().getInternalRef(), id));
        eventBus.post(new DocumentUpdatedEvent());
        // 2. update ui
        populateData();
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "annex.deleted"));
    }

    @Subscribe
    void moveAnnex(MoveAnnexEvent event) {
        //1.call Service to reorder and update annexes
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);

        BillContext billContext = billContextProvider.get();
        billContext.usePackage(leosPackage);
        billContext.useMoveDirection(event.getDirection().toString());
        billContext.useAnnex(event.getAnnexVo().getId());
        billContext.useActionMessage(ContextAction.ANNEX_METADATA_UPDATED, messageHelper.getMessage("collection.block.annex.metadata.updated"));
        billContext.executeMoveAnnex();
        eventBus.post(new DocumentUpdatedEvent());
        //2. update screen to show update
        populateData();
    }

    private String getProposalRef() {
        return (String) httpSession.getAttribute(id + "." + SessionAttribute.PROPOSAL_REF.name());
    }

    @Subscribe
    void openMemorandum(OpenMemorandumEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.MEMORANDUM, event.getMemorandum().getMetadata().getInternalRef()));
    }

    @Subscribe
    void openLegalText(OpenLegalTextEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.LEGALTEXT, event.getLegalText().getMetadata().getInternalRef()));
    }

    @Subscribe
    void openAnnex(OpenAnnexEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.ANNEX, event.getAnnex().getMetadata().getInternalRef()));
    }

    @Subscribe
    void deleteCollectionRequest(DeleteCollectionRequest event) {
        collectionScreen.confirmCollectionDeletion();
    }

    @Subscribe
    void createMilestoneRequest(CreateMilestoneRequestEvent event){
        collectionScreen.openCreateMilestoneWindow();
    }

    @Subscribe
    void createMilestone(CreateMilestoneEvent event) {
        final String milestoneComment = event.getMilestoneComment();
        final String versionComment = messageHelper.getMessage("milestone.versionComment");
        final CollectionContext context = proposalContextProvider.get();
        createMajorVersions(proposalId, milestoneComment, versionComment, context);
        String milestoneProposalId = context.getUpdatedProposalId();
        saveLegFileInCMIS(milestoneProposalId, milestoneComment);
        populateData();
        eventBus.post(new NavigationRequestEvent(Target.PROPOSAL, getProposalRef()));
    }

    @Subscribe
    public void addNewMilestone(MilestoneCreatedEvent milestoneCreatedEvent){
        if(milestoneCreatedEvent.getProposalVersionSeriesId().equals(proposalVersionSeriesId)){
            MilestonesVO newMilestonesVO = getMilestonesVO(milestoneCreatedEvent.getLegDocument());
            long stamp = milestonesVOsLock.writeLock();
            try {
                if(milestonesVOs.add(newMilestonesVO)){
                    collectionScreen.populateMilestones(milestonesVOs);
                    eventBus.post(new NotificationEvent(leosUI, "milestone.caption", "milestone.creation", NotificationEvent.Type.TRAY, StringEscapeUtils.escapeHtml4(newMilestonesVO.getTitle())));
                }
            } finally {
                milestonesVOsLock.unlockWrite(stamp);
            }
        }
    }

    @Subscribe
    public void updateMilestones(MilestoneUpdatedEvent milestoneUpdatedEvent) {
        MilestonesVO eventMilestone = getMilestonesVO(milestoneUpdatedEvent.getLegDocument());
        long stamp = milestonesVOsLock.writeLock();
        try{
            MilestonesVO milestoneToBeUpdated = milestonesVOs.stream().filter(milestonesVO -> milestonesVO.getLegDocumentName().equals(eventMilestone.getLegDocumentName()))
                                                             .findAny().orElse(null);
            if(milestoneToBeUpdated != null && !milestoneToBeUpdated.equals(eventMilestone)){
                milestoneToBeUpdated.setStatus(eventMilestone.getStatus());
                milestoneToBeUpdated.setUpdatedDate(eventMilestone.getUpdatedDate());
                collectionScreen.populateMilestones(milestonesVOs);
                eventBus.post(new NotificationEvent(leosUI, "milestone.caption", "milestone.updated", NotificationEvent.Type.TRAY, StringEscapeUtils.escapeHtml4(milestoneToBeUpdated.getTitle())));
            }
        } finally {
             milestonesVOsLock.unlockWrite(stamp);
        }
    }

    /**
     * Create a major version in CMIS for all documents of this proposal: bill, memorandum, annexes, etc.
     * If already a major version, do not create it.
     * @param proposalId the proposal Id
     * @param milestoneComment the milestone title
     * @param versionComment the version comment
     * @param context the proposal context
     */
    private void createMajorVersions(String proposalId, String milestoneComment, String versionComment, CollectionContext context) {
        context.useProposal(proposalId);
        context.useMilestoneComment(milestoneComment);
        context.useVersionComment(versionComment);
        context.executeCreateMilestone();
    }

    private void saveLegFileInCMIS(String proposalId, String milestoneComment) {
        try{
            LegDocument newLegDocument = milestoneService.createMilestone(proposalId, milestoneComment);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.create.milestone.created"));
            leosApplicationEventBus.post(new MilestoneCreatedEvent(proposalVersionSeriesId, newLegDocument));
            LOG.trace("Milestone creation successfully requested leg");
        } catch(WebServiceException wse) {
            LOG.error("External system not available due to WebServiceException", wse);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "document.create.milestone.url.error", wse.getMessage()));
        } catch (Exception e){
            LOG.error("Milestone creation request failed.", e);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "document.create.milestone.error", e.getMessage()));
        }
    }

    @Subscribe
    void deleteCollectionEvent(DeleteCollectionEvent event) {
        CollectionContext context = proposalContextProvider.get();
        context.useProposal(proposalId);
        context.executeDeleteProposal();
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collection.deleted"));
        eventBus.post(new NavigationRequestEvent(Target.WORKSPACE));
    }

    @Subscribe
    void exportProposal(ExportProposalEvent event) {
        try {
            String formatExport;
            switch (event.getExportOptions()) {
                case TO_PDF:
                    File downloadFile = exportService.exportToPdf(proposalId);
                    formatExport = "Pdf";
                    DownloadStreamResource downloadStreamResource = new DownloadStreamResource(
                            getDownloadFileName(formatExport), new FileInputStream(downloadFile));
                    collectionScreen.setExportPdfStreamResource(downloadStreamResource);
                    eventBus.post(new NotificationEvent("menu.download.caption", "collection.downloaded", NotificationEvent.Type.TRAY));
                    break;
                case TO_PDF_LW:
                case TO_WORD_LW:
                    String jobId = exportService.exportToToolboxCoDe(proposalId, event.getExportOptions());
                    formatExport = event.getExportOptions().equals(ExportOptions.TO_PDF_LW) ? "Pdf" : "Legiswrite";    
                    eventBus.post(new NotificationEvent("collection.caption.menuitem.export", "collection.exported", NotificationEvent.Type.TRAY, formatExport, user.getEmail(), jobId));
                    break;
                default:
                    throw new Exception("Bad export format option");
            }
        } catch(WebServiceException wse) {
            LOG.error("External system not available due to WebServiceException: {}", wse.getMessage());
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collection.export.url.error", wse.getMessage()));
        } catch (Exception e) {
            LOG.error("Unexpected error occured while sending job to ToolBox: {}", e.getMessage());
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collection.export.error", e.getMessage()));
        }
    }

    private String getDownloadFileName(String extension) throws Exception {
        return String.format("%s_%s_%s.%s", DOWNLOAD_PREFIX, proposalId, String.valueOf((new Date()).getTime()), extension.toLowerCase());
    }

    @Subscribe
    void exportMandate(ExportMandateEvent event) {
        try {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
            BillContext context = billContextProvider.get();
            context.usePackage(leosPackage);
            if (proposalId != null) {
                File resultOutputFile = exportService.createDocuWritePackage("Proposal_" + proposalId + "_" + System.currentTimeMillis()+".zip", proposalId, event.getExportOptions(), Optional.empty());
                DownloadStreamResource downloadStreamResource = new DownloadStreamResource(resultOutputFile.getName(), new FileInputStream(resultOutputFile));
                collectionScreen.setDownloadStreamResource(downloadStreamResource);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while using ExportService", e);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "export.docuwrite.error.message", e.getMessage()));
        }
    }

    private String getJobFileName() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("Proposal_");
        strBuilder.append(proposalId);
        strBuilder.append(".zip");
        return strBuilder.toString();
    }

    private void prepareDownloadPackage(File packageFile) throws FileNotFoundException {
        if (packageFile != null) {
            DownloadStreamResource downloadStreamResource = new DownloadStreamResource(packageFile.getName(), new FileInputStream(packageFile));
            collectionScreen.setDownloadStreamResource(downloadStreamResource);
            eventBus.post(new NotificationEvent("menu.download.caption", "collection.downloaded", NotificationEvent.Type.TRAY));
            LOG.trace("Successfully prepared proposal for download");
        }
    }

    @Subscribe
    void prepareProposalDownloadPackage(DownloadProposalEvent event) {
        String jobFileName = getJobFileName();
        File packageFile = null;
        try {
            packageFile = exportService.createCollectionPackage(jobFileName, proposalId, ExportOptions.TO_WORD_LW);
            prepareDownloadPackage(packageFile);
        } catch (Exception e) {
            LOG.error("Error while creating download proposal package", e);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collection.downloaded.error", e.getMessage()));
        } finally {
            if (packageFile != null && packageFile.exists()) {
                packageFile.delete();
            }
        }
    }

    @Subscribe
    void prepareMandateDownloadPackage(DownloadMandateEvent event) {
        String jobFileName = getJobFileName();
        File packageFile = null;
        try {
            packageFile = exportService.createCollectionPackage(jobFileName, proposalId, ExportOptions.TO_WORD_DW);
            prepareDownloadPackage(packageFile);
        } catch (Exception e) {
            LOG.error("Error while creating download proposal package", e);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collection.downloaded.error", e.getMessage()));
        } finally {
            if (packageFile != null && packageFile.exists()) {
                packageFile.delete();
            }
        }
    }

    @Subscribe
    void closeProposalView(CloseScreenRequestEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void searchUser(SearchUserRequest event) {
        List<User> users = userHelper.searchUsersByKey(event.getSearchKey());
        collectionScreen.proposeUsers(users.stream().map(UserVO::new).collect(Collectors.toList()));
    }

    @Subscribe
    void addCollaborator(AddCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        Role role = event.getCollaborator().getRole();
        LOG.trace("Adding collaborator...{}, with authority {}", user.getLogin(), role);
        getXmlDocuments().forEach(doc -> updateCollaborators(user, role, doc, false));

        notificationService.sendNotification(new AddCollaborator(user, role.getName(), proposalId, event.getProposalURL()));
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.added", user.getName(),
                messageHelper.getMessage(role.getMessageKey())));
        leosApplicationEventBus.post(new CollaboratorsUpdatedEvent(id, proposalVersionSeriesId, Operation.ADDED));
    }

    @Subscribe
    void removeCollaborator(RemoveCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        Role role = event.getCollaborator().getRole();
        List<XmlDocument> documents = getXmlDocuments();
        LOG.trace("Removing collaborator...{}, with authority {}", user.getLogin(), role);
        if (checkCollaboratorIsNotLastOwner(documents, role)) {
            documents.forEach(doc -> updateCollaborators(user, role, doc, true));
            notificationService.sendNotification(new RemoveCollaborator(user, role.getName(), proposalId, event.getProposalURL()));
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.removed", user.getName()));
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collaborator.message.last.owner.removed",
                    messageHelper.getMessage(role.getMessageKey())));
        }
        leosApplicationEventBus.post(new CollaboratorsUpdatedEvent(id, proposalVersionSeriesId, Operation.REMOVED));
    }

    @Subscribe
    void editCollaborator(EditCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        Role role = event.getCollaborator().getRole();
        List<XmlDocument> documents = getXmlDocuments();
        Role oldRole = authorityMapHelper.getRoleFromListOfRoles(documents.get(0).getCollaborators().get(user.getLogin()));
        LOG.trace("Updating collaborator...{}, old authority {}, with new authority {}", user.getLogin(), oldRole, role);
        if (checkCollaboratorIsNotLastOwner(documents, oldRole)) {
            documents.forEach(doc -> updateCollaborators(user, role, doc, false));

            notificationService.sendNotification(new EditCollaborator(user, role.getName(), proposalId, event.getProposalURL()));
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.edited", user.getName(),
                    messageHelper.getMessage(oldRole.getMessageKey()),
                    messageHelper.getMessage(role.getMessageKey())));
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collaborator.message.last.owner.edited",
                    messageHelper.getMessage(oldRole.getMessageKey())));
        }
        leosApplicationEventBus.post(new CollaboratorsUpdatedEvent(id, proposalVersionSeriesId, Operation.EDITED));
    }

    //Update document based on action(add/edit/remove)
    private void updateCollaborators(User user, Role role, XmlDocument doc, boolean isRemoveAction) {
        Map<String, String> collaborators = doc.getCollaborators();
        if (isRemoveAction) {
            collaborators.remove(user.getLogin());
        } else {
            collaborators.put(user.getLogin(), role.getName());
        }
        securityService.updateCollaborators(doc.getId(), collaborators, doc.getClass());
    }

    //Check if in collaborators there is only one author
    private boolean checkCollaboratorIsNotLastOwner(List<XmlDocument> documents, Role role) {
        boolean isLastOwner = false;
        boolean canAddRemoveCollaborators = false;
        for(String permission : role.getPermissions().getPermissions()){
            if(CAN_ADD_REMOVE_COLLABORATOR.name().equals(permission)){
                canAddRemoveCollaborators = true;
            }
        }
        if (role.isCollaborator() && canAddRemoveCollaborators) {
            Map<String, String> collaborators = documents.get(0).getCollaborators();
            if (Collections.frequency(collaborators.values(), role.getName()) == 1) {
                isLastOwner = true;
            }
        }
        return !isLastOwner;
    }

    private List<XmlDocument> getXmlDocuments() {
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        return packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class, false);
    }

    @Subscribe
    private void onCollaboratorsUpdate(CollaboratorsUpdatedEvent event) {
        if (event.getDocumentId().equals(proposalVersionSeriesId) &&
                (event.getOperation().equals(Operation.EDITED) || event.getOperation().equals(Operation.REMOVED) || 
                        (event.getOperation().equals(Operation.ADDED) && !event.getPresenterId().equals(id)))) {
            populateData();
        }
    }

    @Subscribe
    void updateProposalMetadata(DocumentUpdatedEvent event) {
        if (event.isModified()) {
            CollectionContext context = proposalContextProvider.get();
            context.useChildDocument(proposalId);
            context.useActionComment(messageHelper.getMessage("operation.metadata.updated"));
            context.executeUpdateProposalAsync();
        }
    }

    @Subscribe
    public void onInfoUpdate(UpdateUserInfoEvent updateUserInfoEvent) {
        if(isCurrentInfoId(updateUserInfoEvent.getActionInfo().getInfo().getDocumentId())) {
            if (!user.getLogin().equals(updateUserInfoEvent.getActionInfo().getInfo().getUserLoginName())) {
                eventBus.post(new NotificationEvent(leosUI, "coedition.caption", "coedition.operation." + updateUserInfoEvent.getActionInfo().getOperation().getValue(),
                        NotificationEvent.Type.TRAY, updateUserInfoEvent.getActionInfo().getInfo().getUserName()));
            }
            LOG.debug("Proposal Presenter updated the edit info -" + updateUserInfoEvent.getActionInfo().getOperation().name());
            collectionScreen.updateUserCoEditionInfo(coEditionHelper.getAllEditInfo(), user);
        }
    }
    
    @Subscribe
    public void fetchMilestone(FetchMilestoneEvent event) {
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        LegDocument legDocument = packageService.findDocumentByPackagePathAndName(leosPackage.getPath(), event.getLegFileName(), LegDocument.class);
        collectionScreen.showMilestoneExplorer(legDocument, event.getMilestoneTitle());
    }
    
    private boolean isCurrentInfoId(String versionSeriesId) {
        return docVersionSeriesIds.contains(versionSeriesId);
    }

    @Subscribe
    public void afterClosedWindow(WindowClosedEvent<MilestoneExplorer> windowClosedEvent) {
        LOG.debug("Caught windowClosedEvent for MilestoneExplorer");
    }
}
