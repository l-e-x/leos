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
package eu.europa.ec.leos.ui.view.repository;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.ActType;
import eu.europa.ec.leos.domain.common.ProcedureType;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ValidationVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.converter.ProposalConverterService;
import eu.europa.ec.leos.services.mandate.PostProcessingMandateService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.services.validation.ValidationService;
import eu.europa.ec.leos.ui.event.CreateDocumentRequestEvent;
import eu.europa.ec.leos.ui.event.view.collection.DisplayCollectionEvent;
import eu.europa.ec.leos.ui.model.RepositoryType;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.usecases.document.CollectionContext;
import eu.europa.ec.leos.usecases.document.ContextAction;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.repository.*;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.ui.navigation.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@Scope("prototype")
class RepositoryPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryPresenter.class);

    private final RepositoryScreen repositoryScreen;
    private final WorkspaceService workspaceService;
    private final TemplateService templateService;
    private final PackageService packageService;
    private final Provider<CollectionContext> proposalContextProvider;
    private final MessageHelper messageHelper;
    private final ValidationService validationService;
    private final ProposalConverterService proposalConverterService;
    private final PostProcessingMandateService postProcessingMandateService;
    

    @Autowired
    RepositoryPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
            RepositoryScreen repositoryScreen,
            WorkspaceService workspaceService,
            TemplateService templateService,
            Provider<CollectionContext> proposalContextProvider,
            PackageService packageService, MessageHelper messageHelper, ValidationService validationService, 
            ProposalConverterService proposalConverterService,
            PostProcessingMandateService postProcessingMandateService, EventBus leosApplicationEventBus, UuidHelper uuidHelper) {
        super(securityContext, httpSession, eventBus, leosApplicationEventBus, uuidHelper);
        LOG.trace("Initializing repository presenter...");
        this.repositoryScreen = repositoryScreen;
        this.workspaceService = workspaceService;
        this.templateService = templateService;
        this.proposalContextProvider = proposalContextProvider;
        this.packageService = packageService;
        this.messageHelper = messageHelper;
        this.validationService = validationService;
        this.proposalConverterService = proposalConverterService;
        this.postProcessingMandateService = postProcessingMandateService;
    }

    @Override
    public void enter() {
        super.enter();
        setRepositoryType(getRepositoryType());
        populateData();
    }

    private void populateData() {
        // FIXME just showing some sample documents for development
        List<DocumentVO> documentVOs;

        if (RepositoryType.PROPOSALS.equals(getRepositoryType())) {
            List<Proposal> proposals = workspaceService.browseWorkspace(Proposal.class, false);
            documentVOs = proposals
                    .stream()
                    .map(this::mapProposalToViewObject)
                    .filter(obj -> (obj != null))   // FIXME do we really need this filter?!
                    .collect(Collectors.toList());
        } else {
            List<XmlDocument> xmlDocuments = workspaceService.browseWorkspace(XmlDocument.class, false);
            documentVOs = xmlDocuments
                    .stream()
                    .map(DocumentVO::new)
                    .filter(obj -> (obj != null))   // FIXME do we really need this filter?!
                    .collect(Collectors.toList());
        }

        repositoryScreen.populateData(documentVOs);
    }

    private DocumentVO mapProposalToViewObject(Proposal proposal) {
        DocumentVO documentVO = new DocumentVO(proposal);
        documentVO.setProcedureType(ProcedureType.ORDINARY_LEGISLATIVE_PROC);   // FIXME get from template
        String docType = proposal.getMetadata().get().getType();
        try {
            documentVO.setActType(ActType.valueOf(docType.substring(0, docType.indexOf(" "))));// FIXME need a better implementation.
        } catch(Exception e) {
            documentVO.setActType(ActType.valueOf(docType.substring(docType.indexOf(" ")+1)));// FIXME need a better implementation. LEOS-3453
        }
        documentVO.setCreatedBy(proposal.getInitialCreatedBy());
        documentVO.setCreatedOn(Date.from(proposal.getInitialCreationInstant()));
        return documentVO;
    }

    @Subscribe
    void refreshDisplayedList(RefreshDisplayedListEvent event) {
        populateData();
    }

    private RepositoryType getRepositoryType() {
        RepositoryType repositoryType = (RepositoryType) httpSession.getAttribute(id + "." + SessionAttribute.REPOSITORY_TYPE.name());
        if (repositoryType == null) {
            repositoryType = RepositoryType.PROPOSALS;// setDefault
        }
        return repositoryType;
    }

    private void setRepositoryType(RepositoryType repositoryType) {
        httpSession.setAttribute(id + "." + SessionAttribute.REPOSITORY_TYPE.name(), repositoryType);
        repositoryScreen.setRepositoryType(repositoryType);
    }

    @Subscribe
    void changeDisplayType(ChangeDisplayTypeEvent event) {
        setRepositoryType(event.getRepositoryType());
        populateData();
    }

    @Subscribe
    void navigateToView(SelectDocumentEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.getTarget(event.getDocType()), event.getDocumentId()));
    }

    @Subscribe
    void showDocumentCreateWizard(DocumentCreateWizardRequestEvent event) throws IOException {
        List<CatalogItem> catalogItems = templateService.getTemplatesCatalog();
        repositoryScreen.showCreateDocumentWizard(catalogItems);
    }

    @Subscribe
    void showMandateCreateWizard(MandateCreateWizardRequestEvent event) throws IOException {
        repositoryScreen.showCreateMandateWizard();
    }

    @Subscribe
    void handleCreateDocumentRequest(CreateDocumentRequestEvent event) {
        LOG.debug("Handling create document request event... [category={}]", event.getDocument().getCategory());
        if (event.getDocument().isUploaded()) {
            //if it has id means that it is an uploaded document.
            CollectionContext context = proposalContextProvider.get();
            context.useDocument(event.getDocument());
            addTemplateInContext(context,event.getDocument());
            context.useActionMessage(ContextAction.METADATA_UPDATED, messageHelper.getMessage("operation.document.imported"));
            context.useActionMessage(ContextAction.ANNEX_BLOCK_UPDATED, messageHelper.getMessage("operation.document.imported"));
            context.useActionMessage(ContextAction.ANNEX_ADDED, messageHelper.getMessage("collection.block.annex.added"));
            context.executeImportProposal();
        }else if (LeosCategory.PROPOSAL.equals(event.getDocument().getCategory())) {
            CollectionContext context = proposalContextProvider.get();
            String template = event.getDocument().getMetadata().getDocTemplate();
            String[] templates = (template != null) ? template.split(";") : new String[0];
            for (String name : templates) {
                context.useTemplate(name);
            }
            context.usePurpose(event.getDocument().getMetadata().getDocPurpose());
            context.useActionMessage(ContextAction.METADATA_UPDATED, messageHelper.getMessage("operation.metadata.updated"));
            context.executeCreateProposal();
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "leos.not.implemented", event.getDocument().getCategory()));
        }

        populateData();
    }

    @Subscribe
    void displayCollectionEvent(DisplayCollectionEvent event){
        if (event.getDocumentType().equals(LeosCategory.PROPOSAL)){
            eventBus.post(new NavigationRequestEvent(Target.getTarget(LeosCategory.PROPOSAL), event.getDocumentId()));
        }
        else {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(event.getDocumentId());
            List<Proposal> proposals = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Proposal.class,false);
            Optional<Proposal> proposal = proposals.stream().filter(prop -> prop.isLatestVersion()).findFirst();
            if (proposal.isPresent()) {
                eventBus.post(new NavigationRequestEvent(Target.getTarget(LeosCategory.PROPOSAL), proposal.get().getId()));
            }
        }
    }

    @Subscribe
    void validateUploadedDocument(ValidateProposalEvent event) {
        LOG.trace("Validating uploaded document");
        ValidationVO result = new ValidationVO();
        result.addErrors(validationService.validateDocument(event.getDocumentVO()));
        repositoryScreen.showValidationResult(result);
    }

    @Subscribe
    void postProcessMandate(PostProcessingMandateEvent event) {
        LOG.trace("Post Processing uploaded mandate");
        Result result = postProcessingMandateService.processMandate(event.getDocumentVO());
        repositoryScreen.showPostProcessingResult(result);
    }

    @Subscribe
    void fetchProposalFromFile(FetchProposalFromFileEvent event) {
        proposalConverterService.createProposalFromLegFile(event.getFile(), event.getDocument(), true);
    }

    private void addTemplateInContext(CollectionContext context, eu.europa.ec.leos.domain.vo.DocumentVO documentVO){
        context.useTemplate(documentVO.getMetadata().getDocTemplate());
        if(documentVO.getChildDocuments()!=null) {
            for (eu.europa.ec.leos.domain.vo.DocumentVO docChild : documentVO.getChildDocuments()) {
                addTemplateInContext(context, docChild);
            }
        }
    }
}