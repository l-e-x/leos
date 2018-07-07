/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.web.presenter;

import com.google.common.eventbus.Subscribe;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.model.content.LeosFile;
import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.TemplateService;
import eu.europa.ec.leos.services.content.WorkspaceService;
import eu.europa.ec.leos.services.locking.LockUpdateBroadcastListener;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.services.user.PermissionService;
import eu.europa.ec.leos.services.user.UserService;
import eu.europa.ec.leos.vo.UserVO;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.view.repository.*;
import eu.europa.ec.leos.web.event.wizard.CreateLeosDocumentRequestEvent;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.wizard.document.DocumentCreateWizardVO;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.FeedbackView;
import eu.europa.ec.leos.web.view.RepositoryView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class RepositoryPresenter extends AbstractPresenter<RepositoryView> implements LockUpdateBroadcastListener {

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private LockingService lockingService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private RepositoryView repositoryView;

    @Autowired
    private SecurityContext leosSecurityContext;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private LockHelper lockHelper;

    @Autowired
    private UserService userService;

    private User user;

    //this is needed to reference the user object from different thread as session is not available from different thread.
    @PostConstruct
    private void setCurrentUser() {
    	user = leosSecurityContext.getUser();
    }

    @Override
    public void onViewLeave() {
        lockingService.unregisterLockInfoBroadcastListener(this);
    }

    @Override
    public String getLockId() {
        return LockUpdateBroadcastListener.REPO_ID;
    }

    @Override
    public RepositoryView getView() {
        return repositoryView;
    }

    @Subscribe
    public void enterRepositoryView(EnterRepositoryViewEvent event) {
        showDisclaimer();
        setDocumentsToView();
        lockingService.registerLockInfoBroadcastListener(this);
    }

    private void setDocumentsToView() {
        // FIXME just showing some sample documents for development
        List<LeosObjectProperties> sampleDocuments = getSampleDocuments();

        List<DocumentVO> documentVOs = new ArrayList<DocumentVO>();
        for (LeosObjectProperties leosObjectProperties : sampleDocuments) {
        	DocumentVO documentVO = createDocumentVO(leosObjectProperties.getLeosId(), leosObjectProperties);
            documentVOs.add(documentVO);
        }
        repositoryView.setSampleDocuments(documentVOs);
    }

    @Subscribe
    public void refreshDocumentList(RefreshDocumentListEvent event){
        setDocumentsToView();
    }

    private DocumentVO createDocumentVO(String leosId,LeosObjectProperties leosObjectProperties){
        DocumentVO documentVO = new DocumentVO((LeosDocumentProperties)leosObjectProperties);//can be null
        documentVO.setLeosId(leosId);
        List<LockData> arrLocks=lockingService.getLockingInfo(leosId);
        documentVO.setLockInfo(arrLocks);
        documentVO.setMsgForUser(lockHelper.constructUserNote(leosId, arrLocks));
        documentVO.setLockState(DocumentVO.LockState.UNLOCKED);
        documentVO.setPermissions(permissionService.getPermissions(user, leosObjectProperties));
        return documentVO;
    }

    private List<LeosObjectProperties> getSampleDocuments() {
        return workspaceService.browseUserWorkspace();
    }

    @Subscribe
    public void navigateToDocumentView(SelectDocumentEvent event) {
        session.setAttribute(SessionAttribute.DOCUMENT_ID.name(), event.getDocumentId());
        switch (event.getDocumentStage()) {
            case DRAFT:
            case EDIT:
            case ARCHIVED://TODO remove later when archive functionality is finalized
                eventBus.post(new NavigationRequestEvent(DocumentView.VIEW_ID));
                break;
            case FEEDBACK:
                eventBus.post(new NavigationRequestEvent(FeedbackView.VIEW_ID));
                break;
        }
    }

    @Subscribe
    public void showDocumentCreateWizard(DocumentCreateWizardRequestEvent event) throws IOException {
        LeosFile templateCatalog = templateService.getTemplatesCatalog();
        List<CatalogItem> catalogItems = templateService.getAllTemplates(templateCatalog.getContentStream());
        repositoryView.showCreateDocumentWizard(catalogItems);
    }

    @Subscribe
    public void createLeosDocument(CreateLeosDocumentRequestEvent event) {
        DocumentCreateWizardVO vo = event.getDocumentCreateWizardVO();
        documentService.createDocumentFromTemplate(vo.getTemplateId(), vo.getMetaDataVO());
        setDocumentsToView();
    }

    @Subscribe
    public void updateStage(StageChangeRequest event) {
        String leosId = event.getDocumentId();
        try {
            if (lockHelper.lockDocument(leosId)) {
                documentService.updateStage(leosId, event.getStage());
                eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "operation.stage.updated"));
            }
        } finally {
            lockHelper.unlockDocument(leosId);
        }
        setDocumentsToView();
    }

    @Override
    public void onLockUpdate(LockActionInfo lockActionInfo) {
        String leosId= lockActionInfo.getLock().getLockId();
        repositoryView.updateLockInfo(createDocumentVO(leosId, null));
    }

    protected void showDisclaimer() {
        if (!Boolean.FALSE.equals(session.getAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name()))) {
            repositoryView.showDisclaimer();
            session.setAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name(), Boolean.FALSE);
        }
    }


    @Subscribe
    public void selectContributors(EditContributorRequest event) {
        DocumentVO docDetails=event.getDocumentVO();
        ArrayList<UserVO> allContributors= new ArrayList<>();
        List<User> allUsers = userService.findAll();
        for(User user:allUsers){
            allContributors.add(new UserVO(user.getLogin(), user.getName(),null));
        }
        repositoryView.openContributorsWindow(allContributors, docDetails);
    }

    @Subscribe
    public void updateContributors(EditContributorEvent event) {
        String leosId = event.getDocumentId();
        if(lockHelper.lockDocument(leosId)) {
            documentService.setContributors(leosId, event.getContributors());
            lockHelper.unlockDocument(leosId);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "operation.contributor.updated"));
        }
        setDocumentsToView();//update the list
    }

    @Subscribe
    public void removeContributor(RemoveContributorRequest event) {
        String leosId = event.getDocumentId();

        if(lockHelper.lockDocument(leosId)) {
            List<UserVO> contributors = documentService.getDocument(leosId).getContributors();
            contributors.remove(event.getContributor());
            documentService.setContributors(leosId, contributors);
            lockHelper.unlockDocument(leosId);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "operation.contributor.removed"));
        }
        setDocumentsToView();//update the list
    }

    @Subscribe
    public void deleteDocument(DeleteDocumentEvent event) {
        final String leosId = event.getDocumentId();
        if(lockHelper.lockDocument(leosId)) {
            documentService.deleteDocument(leosId);
            eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.deleted"));
            lockHelper.unlockDocument(leosId);
        }
        setDocumentsToView();//update the list
    }
}