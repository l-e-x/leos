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
package eu.europa.ec.leos.ui.view.workspace;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.ValidationVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.converter.ProposalConverterService;
import eu.europa.ec.leos.services.mandate.PostProcessingMandateService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.services.validation.ValidationService;
import eu.europa.ec.leos.ui.event.CreateDocumentRequestEvent;
import eu.europa.ec.leos.ui.event.view.collection.DisplayCollectionEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.usecases.document.CollectionContext;
import eu.europa.ec.leos.usecases.document.ContextAction;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.repository.*;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Component
@Scope("prototype")
class WorkspacePresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspacePresenter.class);

    private final WorkspaceScreen workspaceScreen;
    private final WorkspaceService workspaceService;
    private final TemplateService templateService;
    private final Provider<CollectionContext> proposalContextProvider;
    private final MessageHelper messageHelper;
    private final ValidationService validationService;
    private final ProposalConverterService proposalConverterService;
    private final PostProcessingMandateService postProcessingMandateService;

    @Autowired
    WorkspacePresenter(SecurityContext securityContext,
                       HttpSession httpSession,
                       EventBus eventBus,
                       WorkspaceScreen workspaceScreen,
                       WorkspaceService workspaceService,
                       TemplateService templateService,
                       Provider<CollectionContext> proposalContextProvider,
                       PackageService packageService,
                       MessageHelper messageHelper,
                       ValidationService validationService,
                       ProposalConverterService proposalConverterService,
                       PostProcessingMandateService postProcessingMandateService,
                       EventBus leosApplicationEventBus,
                       UuidHelper uuidHelper) {
        super(securityContext, httpSession, eventBus, leosApplicationEventBus, uuidHelper, packageService, workspaceService);
        LOG.trace("Initializing workspace presenter...");
        this.workspaceScreen = workspaceScreen;
        this.workspaceService = workspaceService;
        this.templateService = templateService;
        this.proposalContextProvider = proposalContextProvider;
        this.messageHelper = messageHelper;
        this.validationService = validationService;
        this.proposalConverterService = proposalConverterService;
        this.postProcessingMandateService = postProcessingMandateService;
    }

    @Override
    public void enter() {
        super.enter();
        initialize();
    }

    private void initialize() {
        workspaceScreen.setDataFunctions(this::dataFn, this::countFn);
        try {
            workspaceScreen.intializeFiltersWithData(templateService.getTemplatesCatalog());
        } catch (Exception ioe) {
            throw new RuntimeException("Catalog has some issues", ioe);
        }
    }

    private Stream<Proposal> dataFn(int startIndex, int maxResults, QueryFilter workspaceFilter) {
        return workspaceService.findDocuments(Proposal.class, false, startIndex, maxResults, workspaceFilter);
    }

    private Integer countFn(QueryFilter workspaceFilter) {
        return workspaceService.findDocumentCount(Proposal.class, workspaceFilter);
    }

    @Subscribe
    void navigateToView(SelectDocumentEvent event) {
        eventBus.post(new NavigationRequestEvent(Target.getTarget(event.getDocType()), event.getDocumentId()));
    }

    @Subscribe
    void showDocumentCreateWizard(DocumentCreateWizardRequestEvent event) throws IOException {
        List<CatalogItem> catalogItems = templateService.getTemplatesCatalog();
        workspaceScreen.showCreateDocumentWizard(catalogItems);
    }

    @Subscribe
    void showMandateCreateWizard(MandateCreateWizardRequestEvent event) throws IOException {
        workspaceScreen.showCreateMandateWizard();
    }

    @Subscribe
    void handleCreateDocumentRequest(CreateDocumentRequestEvent event) {
        LOG.debug("Handling create document request event... [category={}]", event.getDocument().getCategory());
        if (event.getDocument().isUploaded()) {
            //if it has id means that it is an uploaded document.
            CollectionContext context = proposalContextProvider.get();
            context.useDocument(event.getDocument());
            addTemplateInContext(context, event.getDocument());
            context.useActionMessage(ContextAction.METADATA_UPDATED, messageHelper.getMessage("operation.document.imported"));
            context.useActionMessage(ContextAction.ANNEX_BLOCK_UPDATED, messageHelper.getMessage("operation.document.imported"));
            context.useActionMessage(ContextAction.ANNEX_ADDED, messageHelper.getMessage("collection.block.annex.added"));
            context.useActionMessage(ContextAction.DOCUMENT_CREATED, messageHelper.getMessage("operation.document.created"));
            context.executeImportProposal();
        } else if (LeosCategory.PROPOSAL.equals(event.getDocument().getCategory())) {
            CollectionContext context = proposalContextProvider.get();
            String template = event.getDocument().getMetadata().getDocTemplate();
            String[] templates = (template != null) ? template.split(";") : new String[0];
            for (String name : templates) {
                context.useTemplate(name);
            }
            context.usePurpose(event.getDocument().getMetadata().getDocPurpose());
            context.useActionMessage(ContextAction.METADATA_UPDATED, messageHelper.getMessage("operation.metadata.updated"));
            context.useActionMessage(ContextAction.DOCUMENT_CREATED, messageHelper.getMessage("operation.document.created"));
            context.executeCreateProposal();
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "leos.not.implemented", event.getDocument().getCategory()));
        }
        workspaceScreen.refreshData();
    }

    @Subscribe
    void displayProposalEvent(DisplayCollectionEvent event) {
        if (event.getDocumentType().equals(LeosCategory.PROPOSAL)) {
            eventBus.post(new NavigationRequestEvent(Target.getTarget(LeosCategory.PROPOSAL), event.getDocumentId()));
        } else {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(event.getDocumentId());
            List<Proposal> proposals = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Proposal.class, false);
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
        workspaceScreen.showValidationResult(result);
    }

    @Subscribe
    void postProcessMandate(PostProcessingMandateEvent event) {
        LOG.trace("Post Processing uploaded mandate");
        Result result = postProcessingMandateService.processMandate(event.getDocumentVO());
        workspaceScreen.showPostProcessingResult(result);
    }

    @Subscribe
    void fetchProposalFromFile(FetchProposalFromFileEvent event) {
        proposalConverterService.createProposalFromLegFile(event.getFile(), event.getDocument(), true);
    }

    private void addTemplateInContext(CollectionContext context, DocumentVO documentVO) {
        context.useTemplate(documentVO.getMetadata().getDocTemplate());
        if (documentVO.getChildDocuments() != null) {
            for (DocumentVO docChild : documentVO.getChildDocuments()) {
                addTemplateInContext(context, docChild);
            }
        }
    }
}