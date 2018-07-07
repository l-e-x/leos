/**
 * Copyright 2015 European Commission
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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.Subscribe;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosFile;
import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.model.content.LeosTypeId;
import eu.europa.ec.leos.model.security.SecurityContext;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.DocumentService;
import eu.europa.ec.leos.services.content.TemplateService;
import eu.europa.ec.leos.services.content.WorkspaceService;
import eu.europa.ec.leos.services.locking.LockUpdateBroadcastListener;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.view.repository.DocumentCreateWizardRequestEvent;
import eu.europa.ec.leos.web.event.view.repository.EnterRepositoryViewEvent;
import eu.europa.ec.leos.web.event.view.repository.SelectDocumentEvent;
import eu.europa.ec.leos.web.event.wizard.CreateLeosDocumentRequestEvent;
import eu.europa.ec.leos.web.model.DocumentVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.ui.wizard.document.DocumentCreateWizardVO;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.RepositoryView;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class RepositoryPresenter extends AbstractPresenter<RepositoryView> implements LockUpdateBroadcastListener {
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

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
    
    private User user;
    
    //this is needed to reference the user object from different thread as session is not available from different thread.
    @PostConstruct
    private void setCurrentUser() {
    	user = leosSecurityContext.getUser();
    }
    
    @Subscribe
    public void enterRepositoryView(EnterRepositoryViewEvent event) {
        showDisclaimer();
        setDocumentsToView();
        lockingService.registerLockInfoBroadcastListener(this);
    }

    @Override
    public void onViewLeave() {
        lockingService.unregisterLockInfoBroadcastListener(this);
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
    
    private DocumentVO createDocumentVO(String leosId,LeosObjectProperties leosObjectProperties){
        DocumentVO documentVO = new DocumentVO();
        documentVO.setDocumentId(leosId);
        documentVO.setLeosObjectProperties(leosObjectProperties);//it will be null for the lockupdate request
        List<LockData> arrLocks=lockingService.getLockingInfo(leosId);
        documentVO.setLockInfo(arrLocks);
        documentVO.setMsgForUser(createMessage(leosId,arrLocks));
        documentVO.setLockState(DocumentVO.LockState.UNLOCKED);

        return documentVO;
    }
    
    private String createMessage(String leosId, List<LockData> arrLocks ){
        StringBuilder sb= new StringBuilder();
        for(LockData lockData: arrLocks){
            if(messageHelper!=null){
                switch (lockData.getLockLevel()){
                    case READ_LOCK:    			
                        sb.append(messageHelper.getMessage("document.locked.read", lockData.getUserName(), lockData.getUserLoginName(),
                                dateFormatter.format(new Date(lockData.getLockingAcquiredOn()))));
                        break;
                    case ELEMENT_LOCK:
                        sb.append(messageHelper.getMessage("document.locked.article", lockData.getUserName(), lockData.getUserLoginName(),lockData.getElementId(),
                                dateFormatter.format(new Date(lockData.getLockingAcquiredOn()))));
                        break;
                    case DOCUMENT_LOCK:
                        sb.append(messageHelper.getMessage("document.locked", lockData.getUserName(), lockData.getUserLoginName(),
                                dateFormatter.format(new Date(lockData.getLockingAcquiredOn()))));
                        break;
                }//end switch
                sb.append("<br> ");
            }
        }
        return sb.toString();
    }
    
    private List<LeosObjectProperties> getSampleDocuments() {
        List<LeosObjectProperties> documents = new ArrayList<>();
        List<LeosObjectProperties> samples = workspaceService.browseUserWorkspace();
        // grab documents only
        for (LeosObjectProperties obj : samples) {
            if (LeosTypeId.LEOS_DOCUMENT.equals(obj.getLeosTypeId())) {
                documents.add(obj);
            }
        }
        return documents;
    }

    @Subscribe
    public void navigateToDocumentView(SelectDocumentEvent event) {
        session.setAttribute(SessionAttribute.DOCUMENT_ID.name(), event.getDocumentId());
        eventBus.post(new NavigationRequestEvent(DocumentView.VIEW_ID));
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

    @Override
    public void onLockUpdate(LockActionInfo lockActionInfo) {
        String leosId= lockActionInfo.getLock().getLockId();
        repositoryView.updateLockInfo(createDocumentVO(leosId, null));
    }
    
    @Override
    public String getLockId() {
        return LockUpdateBroadcastListener.REPO_ID;
    }

    @Override
    public RepositoryView getView() {
        return repositoryView;
    }

    protected void showDisclaimer() {
        if (!Boolean.FALSE.equals(session.getAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name()))) {
            repositoryView.showDisclaimer();
            session.setAttribute(SessionAttribute.DISCLAIMER_ACTIVE.name(), Boolean.FALSE);
        }
    }
}