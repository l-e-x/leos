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
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.document.*;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.user.UserService;
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
import javax.xml.ws.soap.SOAPFaultException;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
class ProposalPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalPresenter.class);

    private final ProposalScreen proposalScreen;
    private final ProposalService proposalService;
    private final Provider<ProposalContext> proposalContextProvider;
    private final MemorandumService memorandumService;
    private final BillService billService;
    private final Provider<BillContext> billContextProvider;
    private final AnnexService annexService;
    private final PackageService packageService;
    private final UserService userService;
    private final ExportService exportService;
    private final SecurityService securityService;

    @Autowired
    ProposalPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                      ProposalScreen proposalScreen,
                      ProposalService proposalService,
                      Provider<ProposalContext> proposalContextProvider,
                      MemorandumService memorandumService,
                      BillService billService,
                      Provider<BillContext> billContextProvider,
                      AnnexService annexService,
                      PackageService packageService,
                      UserService userService,
                      ExportService exportService,
                      SecurityService securityService) {
        super(securityContext, httpSession, eventBus);
        this.proposalScreen = proposalScreen;
        this.proposalService = proposalService;
        this.proposalContextProvider = proposalContextProvider;
        this.memorandumService = memorandumService;
        this.billService = billService;
        this.billContextProvider = billContextProvider;
        this.annexService = annexService;
        this.packageService = packageService;
        this.userService = userService;
        this.exportService = exportService;
        this.securityService = securityService;
    }

    @Override
    public void enter() {
        super.enter();
        populateData();
    }

    private void populateData() {
        String proposalId = getProposalId();
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        List<XmlDocument> documents = packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class);

        ProposalVO proposalVO = createViewObject(documents);
        proposalScreen.populateData(proposalVO);
    }

    private ProposalVO createViewObject(List<XmlDocument> documents) {
        ProposalVO proposalVO = new ProposalVO();
        List<DocumentVO> annexVOS = new ArrayList<>();

        for (XmlDocument document : documents) {
            switch (document.getCategory()) {
                case PROPOSAL: {
                    Proposal proposal = proposalService.findProposal(document.getId());
                    MetadataVO metadataVO = createMetadataVO(proposal);
                    proposalVO.setMetaData(metadataVO);
                    proposalVO.addCollaborators(createCollaboratorVOs(proposal.getCollaborators()));
                    break;
                }
                case MEMORANDUM: {
                    Memorandum memorandum = memorandumService.findMemorandum(document.getId());
                    DocumentVO memorandumVO = getMemorandumVO(memorandum);
                    proposalVO.setExplanatoryMemorandum(memorandumVO);
                    break;
                }
                case BILL: {
                    Bill bill = billService.findBill(document.getId());
                    DocumentVO billVO = getLegalTextVO(bill);
                    proposalVO.setLegalText(billVO);
                    break;
                }
                case ANNEX: {
                    Annex annex = annexService.findAnnex(document.getId());
                    DocumentVO annexVO = createAnnexVO(annex);
                    annexVOS.add(annexVO);
                    break;
                }
            }
        }

        Collections.sort(annexVOS, Comparator.comparingInt(DocumentVO::getDocNumber));
        if (proposalVO.getLegalText() != null) {
            for (DocumentVO annexVO : annexVOS) {
                proposalVO.getLegalText().addChildDocument(annexVO);
            }
        }

        return proposalVO;
    }

    // FIXME refine
    private DocumentVO getMemorandumVO(Memorandum memorandum) {
        return new DocumentVO(memorandum.getId(),
                         memorandum.getLanguage(),
                         LeosCategory.MEMORANDUM,
                         memorandum.getLastModifiedBy(),
                         Date.from(memorandum.getLastModificationInstant()));
    }

    // FIXME refine
    private DocumentVO getLegalTextVO(Bill bill) {
        return new DocumentVO(bill.getId(),
                         bill.getLanguage(),
                         LeosCategory.BILL,
                         bill.getLastModifiedBy(),
                         Date.from(bill.getLastModificationInstant()));
    }

    // FIXME refine
    private DocumentVO createAnnexVO(Annex annex) {
        DocumentVO annexVO =
                new DocumentVO(annex.getId(),
                          annex.getLanguage(),
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

    private List<CollaboratorVO> createCollaboratorVOs(Map<String, LeosAuthority> allowedUsers) {
        return allowedUsers.entrySet().parallelStream()
                .map(entry -> createCollaboratorVO(entry.getKey(), entry.getValue()))
                .filter(option -> option.isPresent())
                .map(option -> option.get())
                .collect(Collectors.toList());
    }

    private Optional<CollaboratorVO> createCollaboratorVO(String login, LeosAuthority authority) {
        try {
            return Optional.of(new CollaboratorVO(new UserVO(userService.getUser(login)),authority));
        } catch (Exception e) {
            LOG.error(String.format("Exception while creating collaborator VO:%s, %s",login, authority), e);
            return Optional.empty();
        }
    }

    // TODO dummy impl
    private MetadataVO createMetadataVO(Proposal proposal) {
        ProposalMetadata metadata = proposal.getMetadata().getOrError(() -> "Proposal metadata is not available!");
        return new MetadataVO(metadata, proposal.getTemplate(), proposal.getLanguage());
    }

    @Subscribe
    void saveMetaData(SaveMetaDataRequestEvent event) {
        LOG.trace("Saving proposal metadata...");
        ProposalContext context = proposalContextProvider.get();
        context.useProposal(getProposalId());
        context.usePurpose(event.getMetaDataVO().getDocPurpose());
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
        annexService.updateAnnex(annex, updatedMetadata,false, "Metadata updated.");

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
        billContext.executeCreateBillAnnex();

        populateData();
    }

    @Subscribe
    void deleteAnnexRequest(DeleteAnnexRequest event) {
        proposalScreen.confirmAnnexDeletion(event.getAnnex());
    }

    @Subscribe
    void deleteAnnexEvent(DeleteAnnexEvent event) {
        // FIXME To be implemented, this part is commented: add a confirmation dialog?
        // 1. delete Annex
        LeosPackage leosPackage = packageService.findPackageByDocumentId(getProposalId());

        BillContext billContext = billContextProvider.get();
        billContext.useAnnex(event.getAnnex().getId());
        billContext.usePackage(leosPackage);
        billContext.executeRemoveBillAnnex();
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

        billContext.executeMoveAnnex();

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
        List<User> users = userService.searchUsersByKey(event.getSearchKey());
        proposalScreen.proposeUsers(users.stream().map(user -> new UserVO(user)).collect(Collectors.toList()));
    }

    @Subscribe
    void addCollaborator(AddCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        LeosAuthority authority = event.getCollaborator().getLeosAuthority();
        LOG.trace("Adding collaborator...{}, with authority {}", user.getLogin(), authority);

        getXmlDocuments()
                .forEach(doc ->
                        securityService.addOrUpdateCollaborator(doc.getId(), user.getLogin(), authority, doc.getClass()));

        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.added", user.getName(), authority));
    }

    @Subscribe
    void removeCollaborator(RemoveCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        LeosAuthority authority = event.getCollaborator().getLeosAuthority();
        LOG.trace("Removing collaborator...{}, with authority {}", user.getLogin(), authority);

        getXmlDocuments()
                .forEach(doc ->
                        securityService.removeCollaborator(doc.getId(), user.getLogin(), doc.getClass()));

        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.removed", user.getName()));
    }

    @Subscribe
    void editCollaborator(EditCollaboratorRequest event) {
        User user = event.getCollaborator().getUser();
        LeosAuthority authority = event.getCollaborator().getLeosAuthority();
        List<XmlDocument> documents = getXmlDocuments();
        LeosAuthority oldAuthority = documents.get(0).getCollaborators().get(user.getLogin());
        LOG.trace("Updating collaborator...{}, old authority {}, with new authority {}", user.getLogin(), oldAuthority, authority);
        documents
                .forEach(doc ->
                        securityService.addOrUpdateCollaborator(doc.getId(), user.getLogin(), authority, doc.getClass()));

        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "collaborator.message.edited", user.getName(), oldAuthority, authority));
    }

    private  List<XmlDocument> getXmlDocuments(){
        String proposalId = getProposalId();
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        return packageService.findDocumentsByPackagePath(leosPackage.getPath(), XmlDocument.class);
    }
}
