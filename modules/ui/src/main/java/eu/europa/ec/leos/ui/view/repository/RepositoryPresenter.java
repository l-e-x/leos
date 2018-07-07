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
package eu.europa.ec.leos.ui.view.repository;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosPackage;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Proposal;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.ui.event.CreateDocumentRequestEvent;
import eu.europa.ec.leos.ui.model.ActType;
import eu.europa.ec.leos.ui.model.ProcedureType;
import eu.europa.ec.leos.ui.model.RepositoryType;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.usecases.document.ProposalContext;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.proposal.DisplayProposalEvent;
import eu.europa.ec.leos.web.event.view.repository.ChangeDisplayTypeEvent;
import eu.europa.ec.leos.web.event.view.repository.DocumentCreateWizardRequestEvent;
import eu.europa.ec.leos.web.event.view.repository.RefreshDisplayedListEvent;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
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
import java.util.stream.Collectors;

@Component
@Scope("prototype")
class RepositoryPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoryPresenter.class);

    private final RepositoryScreen repositoryScreen;
    private final WorkspaceService workspaceService;
    private final TemplateService templateService;
    private final PackageService packageService;
    private final Provider<ProposalContext> proposalContextProvider;

    @Autowired
    RepositoryPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                        RepositoryScreen repositoryScreen,
                        WorkspaceService workspaceService,
                        TemplateService templateService,
                        Provider<ProposalContext> proposalContextProvider,
                        PackageService packageService) {
        super(securityContext, httpSession, eventBus);
        LOG.trace("Initializing repository presenter...");
        this.repositoryScreen = repositoryScreen;
        this.workspaceService = workspaceService;
        this.templateService = templateService;
        this.proposalContextProvider = proposalContextProvider;
        this.packageService = packageService;
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
            List<XmlDocument.Proposal> proposals = workspaceService.browseWorkspace(XmlDocument.Proposal.class);
            documentVOs = proposals
                    .stream()
                    .map(this::mapProposalToViewObject)
                    .filter(obj -> (obj != null))   // FIXME do we really need this filter?!
                    .collect(Collectors.toList());
        } else {
            List<XmlDocument> xmlDocuments = workspaceService.browseWorkspace(XmlDocument.class);
            documentVOs = xmlDocuments
                    .stream()
                    .map(DocumentVO::new)
                    .filter(obj -> (obj != null))   // FIXME do we really need this filter?!
                    .collect(Collectors.toList());
        }

        repositoryScreen.populateData(documentVOs);
    }

    private DocumentVO mapProposalToViewObject(XmlDocument.Proposal proposal) {
        DocumentVO documentVO = new DocumentVO(proposal);
        documentVO.setProcedureType(ProcedureType.ORDINARY_LEGISLATIVE_PROC);   // FIXME get from template
        String docType = proposal.getMetadata().get().getType();
        documentVO.setActType(ActType.valueOf(docType.substring(0, docType.indexOf(" "))));// FIXME need a better implementation.
        return documentVO;
    }

    @Subscribe
    void refreshDisplayedList(RefreshDisplayedListEvent event) {
        populateData();
    }

    private RepositoryType getRepositoryType() {
        RepositoryType repositoryType = (RepositoryType) httpSession.getAttribute(SessionAttribute.REPOSITORY_TYPE.name());
        if (repositoryType == null) {
            repositoryType = RepositoryType.PROPOSALS;// setDefault
        }
        return repositoryType;
    }

    private void setRepositoryType(RepositoryType repositoryType) {
        httpSession.setAttribute(SessionAttribute.REPOSITORY_TYPE.name(), repositoryType);
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
    void handleCreateDocumentRequest(CreateDocumentRequestEvent event) {
        LOG.debug("Handling create document request event... [category={}]", event.getCategory());

        if (LeosCategory.PROPOSAL.equals(event.getCategory())) {
            ProposalContext context = proposalContextProvider.get();
            for (String name : event.getTemplates()) {
                context.useTemplate(name);
            }
            context.usePurpose(event.getPurpose());
            context.executeCreateProposal();
        } else {
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "leos.not.implemented", event.getCategory()));
        }

        populateData();
    }

    @Subscribe
    void displayProposalEvent(DisplayProposalEvent event){
        if (event.getDocumentType().equals(LeosCategory.PROPOSAL)){
            eventBus.post(new NavigationRequestEvent(Target.getTarget(LeosCategory.PROPOSAL), event.getDocumentId()));
        }
        else {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(event.getDocumentId());
            List<Proposal> proposals = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Proposal.class);
            Optional<Proposal> proposal = proposals.stream().filter(prop -> prop.isLatestVersion()).findFirst();
            if (proposal.isPresent()) {
                eventBus.post(new NavigationRequestEvent(Target.getTarget(LeosCategory.PROPOSAL), proposal.get().getId()));
            }
        }
    }
}
