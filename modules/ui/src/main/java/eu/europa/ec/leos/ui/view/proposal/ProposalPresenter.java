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
package eu.europa.ec.leos.ui.view.proposal;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Proposal;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.domain.document.LeosMetadata.ProposalMetadata;
import eu.europa.ec.leos.domain.document.LeosPackage;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.model.notification.collaborators.AddCollaborator;
import eu.europa.ec.leos.model.notification.collaborators.EditCollaborator;
import eu.europa.ec.leos.model.notification.collaborators.RemoveCollaborator;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.document.*;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.notification.NotificationService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.usecases.document.ContextAction;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.usecases.document.BillContext;
import eu.europa.ec.leos.usecases.document.ProposalContext;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.annex.OpenAnnexEvent;
import eu.europa.ec.leos.web.event.view.document.OpenLegalTextEvent;
import eu.europa.ec.leos.web.event.view.memorandum.OpenMemorandumEvent;
import eu.europa.ec.leos.web.event.view.proposal.*;
import eu.europa.ec.leos.web.event.window.SaveMetaDataRequestEvent;
import eu.europa.ec.leos.web.model.*;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.support.xml.DownloadStreamResource;
import eu.europa.ec.leos.web.ui.component.MoveAnnexEvent;
import eu.europa.ec.leos.web.ui.navigation.Target;
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
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
class ProposalPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalPresenter.class);

    private final ProposalScreen proposalScreen;
    private final Provider<ProposalContext> proposalContextProvider;
    private final Provider<BillContext> billContextProvider;
    private final AnnexService annexService;
    private final PackageService packageService;
    private final UserHelper userHelper;
    private final ExportService exportService;
    private final SecurityService securityService;
    private final NotificationService notificationService;
    private final MessageHelper messageHelper;

    @Autowired
    ProposalPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                      ProposalScreen proposalScreen,
                      Provider<ProposalContext> proposalContextProvider,
                      Provider<BillContext> billContextProvider,
                      AnnexService annexService,
                      PackageService packageService,
                      UserHelper userHelper,
                      ExportService exportService,
                      SecurityService securityService,
                      NotificationService notificationService, MessageHelper messageHelper) {
        super(securityContext, httpSession, eventBus);
        this.proposalScreen = proposalScreen;
        this.proposalContextProvider = proposalContextProvider;
        this.billContextProvider = billContextProvider;
        this.annexService = annexService;
        this.packageService = packageService;
        this.userHelper = userHelper;
        this.exportService = exportService;
        this.securityService = securityService;
        this.notificationService = notificationService;
        this.messageHelper = messageHelper;
    }

    @Override
    public void enter() {
        super.enter();
        populateData();
    }

    private void populateData() {
        String proposalId = getProposalId();
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        List<XmlDocument> documents = packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class, false);

        DocumentVO proposalVO = createViewObject(documents);
        proposalScreen.populateData(proposalVO);
    }

    private DocumentVO createViewObject(List<XmlDocument> documents) {
        DocumentVO proposalVO = new DocumentVO(LeosCategory.PROPOSAL);
        List<DocumentVO> annexVOS = new ArrayList<>();
        //We have the latest version of the document, no need to search for them again
        for (XmlDocument document : documents) {
            switch (document.getCategory()) {
                case PROPOSAL: {
                    Proposal proposal = (Proposal) document;
                    MetadataVO metadataVO = createMetadataVO(proposal);
                    proposalVO.setMetaData(metadataVO);
                    proposalVO.addCollaborators(proposal.getCollaborators());
                    break;
                }
                case MEMORANDUM: {
                    Memorandum memorandum = (Memorandum) document;
                    DocumentVO memorandumVO = getMemorandumVO(memorandum);
                    proposalVO.addChildDocument(memorandumVO);
                    memorandumVO.addCollaborators(memorandum.getCollaborators());
                    break;
                }
                case BILL: {
                    Bill bill = (Bill) document;
                    DocumentVO billVO = getLegalTextVO(bill);
                    proposalVO.addChildDocument(billVO);
                    billVO.addCollaborators(bill.getCollaborators());
                    break;
                }
                case ANNEX: {
                    Annex annex = (Annex) document;
                    DocumentVO annexVO = createAnnexVO(annex);
                    annexVO.addCollaborators(annex.getCollaborators());
                    annexVOS.add(annexVO);
                    break;
                }
            }
        }

        Collections.sort(annexVOS, Comparator.comparingInt(DocumentVO::getDocNumber));
        DocumentVO legalText = proposalVO.getChildDocument(LeosCategory.BILL);
        if (legalText != null) {
            for (DocumentVO annexVO : annexVOS) {
                legalText.addChildDocument(annexVO);
            }
        }

        return proposalVO;
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

    // TODO dummy impl
    private MetadataVO createMetadataVO(Proposal proposal) {
        ProposalMetadata metadata = proposal.getMetadata().getOrError(() -> "Proposal metadata is not available!");
        return new MetadataVO(metadata.getStage(), metadata.getType(), metadata.getPurpose(), metadata.getTemplate(), metadata.getLanguage());
    }

    @Subscribe
    void saveMetaData(SaveMetaDataRequestEvent event) {
        LOG.trace("Saving proposal metadata...");
        ProposalContext context = proposalContextProvider.get();
        context.useProposal(getProposalId());
        context.usePurpose(event.getMetaDataVO().getDocPurpose());
        context.useActionMessage(ContextAction.METADATA_UPDATED, messageHelper.getMessage("operation.metadata.updated"));
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
        annexService.updateAnnex(annex, updatedMetadata,false, messageHelper.getMessage("proposal.block.annex.metadata.updated"));
        eventBus.post(new DocumentUpdatedEvent());
        // 3.update ui
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "proposal.block.annex.metadata.updated"));
        populateData();
    }

    @Subscribe
    void createAnnex(CreateAnnexRequest event) {
        // KLUGE temporary workaround
        LeosPackage leosPackage = packageService.findPackageByDocumentId(getProposalId());

        BillContext billContext = billContextProvider.get();
        billContext.usePackage(leosPackage);
        billContext.useActionMessage(ContextAction.ANNEX_METADATA_UPDATED, messageHelper.getMessage("proposal.block.annex.metadata.updated"));
        billContext.useActionMessage(ContextAction.ANNEX_ADDED, messageHelper.getMessage("proposal.block.annex.added"));
        billContext.executeCreateBillAnnex();
        eventBus.post(new DocumentUpdatedEvent());
        populateData();
    }

    @Subscribe
    void deleteAnnexRequest(DeleteAnnexRequest event) {
        proposalScreen.confirmAnnexDeletion(event.getAnnex());
    }

    @Subscribe
    void deleteAnnex(DeleteAnnexEvent event) {
        // FIXME To be implemented, this part is commented: add a confirmation dialog?
        // 1. delete Annex
        LeosPackage leosPackage = packageService.findPackageByDocumentId(getProposalId());

        BillContext billContext = billContextProvider.get();
        billContext.useAnnex(event.getAnnex().getId());
        billContext.usePackage(leosPackage);
        billContext.useActionMessage(ContextAction.ANNEX_METADATA_UPDATED, messageHelper.getMessage("proposal.block.annex.metadata.updated"));
        billContext.useActionMessage(ContextAction.ANNEX_DELETED, messageHelper.getMessage("proposal.block.annex.removed"));
        billContext.executeRemoveBillAnnex();
        eventBus.post(new DocumentUpdatedEvent());
        // 2. update ui
        populateData();
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "annex.deleted"));
    }

    @Subscribe
    void moveAnnex(MoveAnnexEvent event) {
        //1.call Service to reorder and update annexes
        LeosPackage leosPackage = packageService.findPackageByDocumentId(getProposalId());

        BillContext billContext = billContextProvider.get();
        billContext.usePackage(leosPackage);
        billContext.useMoveDirection(event.getDirection().toString());
        billContext.useAnnex(event.getAnnexVo().getId());
        billContext.useActionMessage(ContextAction.ANNEX_METADATA_UPDATED, messageHelper.getMessage("proposal.block.annex.metadata.updated"));
        billContext.executeMoveAnnex();
        eventBus.post(new DocumentUpdatedEvent());
        //2. update screen to show update
        populateData();
    }

    private String getProposalId() {
        return (String) httpSession.getAttribute(SessionAttribute.PROPOSAL_ID.name());
    }

    private String getUserLogin() {
        return securityContext.getUser().getLogin();
    }

    @Subscribe
    void openMemorandum(OpenMemorandumEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.MEMORANDUM, event.getMemorandum().getId()));
    }

    @Subscribe
    void openLegalText(OpenLegalTextEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.LEGALTEXT, event.getDocumentId()));
    }

    @Subscribe
    void openAnnex(OpenAnnexEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.ANNEX, event.getAnnex().getId()));
    }

    @Subscribe
    void deleteProposalRequest(DeleteProposalRequest event) {
        proposalScreen.confirmProposalDeletion();
    }

    @Subscribe
    void deleteProposalEvent(DeleteProposalEvent event) {
        ProposalContext context = proposalContextProvider.get();
        context.useProposal(getProposalId());
        context.executeDeleteProposal();
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "proposal.deleted"));
        eventBus.post(new NavigationRequestEvent(Target.REPOSITORY));
    }

    @Subscribe
    void exportProposal(ExportProposalEvent event) {
        try {
            
            String jobId = exportService.exportToToolboxCoDe(getProposalId(), event.getExportOptions());
            String formatExport;
            switch (event.getExportOptions()) {
                case TO_PDF:
                    formatExport = "Pdf";
                    break;
                case TO_LEGISWRITE:
                    formatExport = "Legiswrite";
                    break;
                default:
                    throw new Exception("Bad export format option");
            }
            eventBus.post(new NotificationEvent("proposal.caption.menuitem.export", "proposal.exported", NotificationEvent.Type.TRAY, formatExport, securityContext.getUser().getEmail(), jobId));
        } catch(WebServiceException wse) {
            LOG.error("External system not available due to WebServiceException: {}", wse.getMessage());
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "proposal.export.url.error", wse.getMessage()));
        }
        catch (Exception e) {
            LOG.error("Unexpected error occured while sending job to ToolBox: {}", e.getMessage());
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "proposal.export.error", e.getMessage()));
        }
    }

    @Subscribe
    void prepareProposalDownloadPackage(DownloadProposalEvent event) {
        File packageFile = null;
        try {
            packageFile = exportService.createProposalLegisWritePackage("Proposal_" + getProposalId() + ".zip", getProposalId(), ExportOptions.TO_LEGISWRITE);
            DownloadStreamResource downloadStreamResource = new DownloadStreamResource(packageFile.getName(), new FileInputStream(packageFile));
            proposalScreen.setDownloadStreamResource(downloadStreamResource);
            eventBus.post(new NotificationEvent("menu.download.caption", "proposal.downloaded", NotificationEvent.Type.TRAY));
            LOG.trace("Successfully prepared proposal for download");
        } 
        catch (Exception e) {
            LOG.error("Error while creating download proposal package {}", e.getMessage());
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "proposal.downloaded.error", e.getMessage()));
        }
        finally {
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
        proposalScreen.proposeUsers(users.stream().map(user -> new UserVO(user)).collect(Collectors.toList()));
    }

    @Subscribe
    void addCollaborator(AddCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        LeosAuthority authority = event.getCollaborator().getLeosAuthority();
        LOG.trace("Adding collaborator...{}, with authority {}", user.getLogin(), authority);
        getXmlDocuments()
                .forEach(doc -> {
                    updateCollaborators(user, authority, doc, false);
                });

        notificationService.sendNotification(new AddCollaborator(user, authority, getProposalId(), event.getProposalURL()));
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.added", user.getName(),
                messageHelper.getMessage("collaborator.column.leosAuthority." + authority.name())));
    }

    @Subscribe
    void removeCollaborator(RemoveCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        LeosAuthority authority = event.getCollaborator().getLeosAuthority();
        List<XmlDocument> documents = getXmlDocuments();
        LOG.trace("Removing collaborator...{}, with authority {}", user.getLogin(), authority);
        if (!checkCollaboratorIsLastOwner(documents, authority)) {
            documents
                    .forEach(doc -> {
                        updateCollaborators(user, authority, doc, true);
                    });
            notificationService.sendNotification(new RemoveCollaborator(user, authority, getProposalId(), event.getProposalURL()));
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.removed", user.getName()));
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collaborator.message.last.owner.removed",
                    messageHelper.getMessage("collaborator.column.leosAuthority." + authority.name())));
        }
        populateData();
    }

    @Subscribe
    void editCollaborator(EditCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        LeosAuthority authority = event.getCollaborator().getLeosAuthority();
        List<XmlDocument> documents = getXmlDocuments();
        LeosAuthority oldAuthority = documents.get(0).getCollaborators().get(user.getLogin());
        LOG.trace("Updating collaborator...{}, old authority {}, with new authority {}", user.getLogin(), oldAuthority, authority);
        if (!checkCollaboratorIsLastOwner(documents, oldAuthority)) {
            documents
                    .forEach(doc -> {
                        updateCollaborators(user, authority, doc, false);
                    });
            
            notificationService.sendNotification(new EditCollaborator(user, authority, getProposalId(), event.getProposalURL()));
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.edited", user.getName(),
                    messageHelper.getMessage("collaborator.column.leosAuthority." + oldAuthority.name()),
                    messageHelper.getMessage("collaborator.column.leosAuthority." + authority.name())));
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.ERROR, "collaborator.message.last.owner.edited",
                    messageHelper.getMessage("collaborator.column.leosAuthority." + oldAuthority.name())));
        }
        populateData();
        
    }

    //Update document based on action(add/edit/remove)
    private void updateCollaborators(User user, LeosAuthority authority, XmlDocument doc, boolean isRemoveAction) {
        Map<String, LeosAuthority> collaborators = doc.getCollaborators();
        if (isRemoveAction) {
            collaborators.remove(user.getLogin());
        } else {
            collaborators.put(user.getLogin(), authority);
        }
        securityService.updateCollaborators(doc.getId(), collaborators, doc.getClass());
    }

    //Check if in collaborators there is only one author 
    private boolean checkCollaboratorIsLastOwner(List<XmlDocument> documents, LeosAuthority authority) {
        boolean isLastOwner = false;
        if (authority == LeosAuthority.OWNER) {
            Map<String, LeosAuthority> collaborators = documents.get(0).getCollaborators();
            if (Collections.frequency(collaborators.values(), LeosAuthority.OWNER) == 1) {
                isLastOwner = true;
            }
        }
        return isLastOwner;
    }

    private  List<XmlDocument> getXmlDocuments(){
        String proposalId = getProposalId();
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        return packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class, false);
    }

    @Subscribe
    void updateProposalMetadata(DocumentUpdatedEvent event) {
        ProposalContext context = proposalContextProvider.get();
        context.useChildDocument(getProposalId());
        context.executeUpdateProposalAsync();
    }
}
