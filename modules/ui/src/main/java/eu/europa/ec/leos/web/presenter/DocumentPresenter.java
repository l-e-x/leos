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
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.services.content.*;
import eu.europa.ec.leos.services.exception.LeosPermissionDeniedException;
import eu.europa.ec.leos.services.locking.LockUpdateBroadcastListener;
import eu.europa.ec.leos.services.locking.LockingService;
import eu.europa.ec.leos.support.web.UrlBuilder;
import eu.europa.ec.leos.support.xml.TransformationManager;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;
import eu.europa.ec.leos.vo.lock.LockData;
import eu.europa.ec.leos.vo.lock.LockLevel;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.EditTocRequestEvent;
import eu.europa.ec.leos.web.event.component.ReleaseAllLocksEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.*;
import eu.europa.ec.leos.web.support.LockHelper;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.i18n.MessageHelper;
import eu.europa.ec.leos.web.view.DocumentView;
import eu.europa.ec.leos.web.view.RepositoryView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class DocumentPresenter extends AbstractPresenter<DocumentView> implements LockUpdateBroadcastListener {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentPresenter.class);

    @Autowired
    private DocumentService documentService;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private LockingService lockingService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ElementService elementService;

    @Autowired
    private DocumentView documentView;

    @Autowired
    private TransformationManager transformationManager;

    @Autowired
    private RulesService rulesService;

    @Autowired
    private MessageHelper messageHelper;

    @Autowired
    private UrlBuilder urlBuilder;

    @Autowired
    private LockHelper lockHelper;

    private String strLockId;

    private final static String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";

    //this is needed to reference the  object from different threads as springContext is not available from different thread.
    @PostConstruct
    private void setLocalVariables() {
        strLockId=getDocumentId();
    }

    @Subscribe
    public void enterDocumentView(EnterDocumentViewEvent event) throws IOException {
        String documentId = getDocumentId();
        if(documentId == null ){
            rejectView(RepositoryView.VIEW_ID, "document.id.missing");
            return;
        }

        User user = leosSecurityContext.getUser();
        LockActionInfo lockActionInfo = lockingService.lockDocument(documentId, user, session.getId(), LockLevel.READ_LOCK);

        if (lockActionInfo.sucesss()) {
            try {
                LeosDocument document = getDocument();
                populateViewWithDocumentDetails(document);
                lockingService.registerLockInfoBroadcastListener(this);
                documentView.updateLocks(lockActionInfo);
                //TODO :dirty fix .below part needs to be rethought and enginnered again
            }catch (LeosPermissionDeniedException permissionException){
                eventBus.post(new NotificationEvent(Type.INFO, "permission.denied.message"));
                rejectView(RepositoryView.VIEW_ID, "permission.denied.message");
            }
        } else {
            LockData lockingInfo = lockActionInfo.getCurrentLocks().get(0); 
            rejectView(RepositoryView.VIEW_ID, "document.locked", lockingInfo.getUserName(), lockingInfo.getUserLoginName(),
                    (new SimpleDateFormat(DATE_FORMAT)).format(new Date(lockingInfo.getLockingAcquiredOn())));
        }
    }

    @Override
    public void onViewLeave() {
        // cleanup
        lockingService.unregisterLockInfoBroadcastListener(this);
        String documentId = getDocumentId();
        if(documentId!=null){
            lockingService.releaseLocksForSession(documentId, session.getId());
            session.removeAttribute(SessionAttribute.DOCUMENT_ID.name());
        }
        strLockId=null;
    }

    @Subscribe
    public void closeDocument(CloseDocumentEvent event) {
        eventBus.post(new NavigationRequestEvent(RepositoryView.VIEW_ID));
    }

    @Subscribe
    public void refreshDocument(RefreshDocumentEvent event) throws IOException {
        LeosDocument document = getDocument();
        populateViewWithDocumentDetails(document);
    }

    @Subscribe
    public void editElement(EditElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();

        if (lockHelper.lockElement(elementId)) {
            LeosDocument document = getDocument();
            String element = elementService.getElement(document, elementTagName, elementId);

            documentView.showElementEditor(event.getElementId(), elementTagName, element);
        }
        //Do not reject view if lock is not available
    }

    @Subscribe
    public void saveElement(SaveElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();

        if (lockHelper.isElementLockedFor(elementId)) {
            LeosDocument document = getDocument();
            document = elementService.saveElement(document, getUserLogin(), event.getElementContent(), elementTagName, elementId);
            if (document != null) {
                String elementContent  = elementService.getElement(document, elementTagName, elementId);
                documentView.refreshElementEditor(elementId, elementTagName, elementContent);
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
                eventBus.post(new DocumentUpdatedEvent());
            }
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Subscribe
    public void closeElementEditor(CloseElementEditorEvent event) throws IOException {
        String elementId = event.getElementId();
        if (lockHelper.isElementLockedFor(elementId)) {
            lockHelper.unlockElement(elementId);
        }
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    public void closeTocEditor(CloseTocEditorEvent event) throws IOException {
        if (lockHelper.isDocumentLockedFor()) {
            lockHelper.unlockDocument();
        }
    }

    @Subscribe
    public void closeMetadataEditor(CloseMetadataEditorEvent event) throws IOException {
        if (lockHelper.isDocumentLockedFor()) {
            lockHelper.unlockDocument();
        }
    }

    @Subscribe
    public void deleteElement(DeleteElementRequestEvent event) throws IOException {
        String tagName = event.getElementTagName();
        if ("article".equals(tagName)) {
            if (lockHelper.lockDocument()) {
                LeosDocument document = getDocument();
                document = elementService.deleteElement(document, getUserLogin(), event.getElementId(), tagName);

                if (document != null) {
                    eventBus.post(new NotificationEvent(Type.INFO, "document."+ tagName + ".deleted"));
                    eventBus.post(new RefreshDocumentEvent());
                    eventBus.post(new DocumentUpdatedEvent());
                }
                lockHelper.unlockDocument();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }
    }

    @Subscribe
    public void insertElement(InsertElementRequestEvent event) throws IOException {
        String tagName = event.getElementTagName();
        if ("article".equals(tagName)) {
            if (lockHelper.lockDocument()) {
                LeosDocument document = getDocument();
                document = articleService.insertNewArticle(document, getUserLogin(), event.getElementId(),
                        InsertElementRequestEvent.POSITION.BEFORE.equals(event.getPosition()));
                if (document != null) {
                    eventBus.post(new NotificationEvent(Type.INFO, "document.article.inserted"));
                    eventBus.post(new RefreshDocumentEvent());
                    eventBus.post(new DocumentUpdatedEvent());
                }
                lockHelper.unlockDocument();
            }
        } else {
            throw new UnsupportedOperationException("Unsupported operation for tag: " + tagName);
        }
    }

    @Subscribe
    public void editToc(EditTocRequestEvent event) {
        if (lockHelper.lockDocument()) {
            LeosDocument document = getDocument();
            documentView.showTocEditWindow(getTableOfContent(document), rulesService.getDefaultTableOfContentRules());
        }
    }

    @Subscribe
    public void releaseAllLocks(ReleaseAllLocksEvent event){
        //this method is invoked when force release of locks is done.
        //1. all locks for current doc are fetched from the lock service
        //2. one by one all locks are released.
        User user = leosSecurityContext.getUser();
        String documentId = getDocumentId();

        List<LockData> locks=lockingService.getLockingInfo(documentId);
        for(LockData lockData : locks){
            LockActionInfo lockActionInfo = null;

            if(lockData.getUserLoginName().equalsIgnoreCase(user.getLogin())){
                switch (lockData.getLockLevel()){
                    case READ_LOCK:
                    case DOCUMENT_LOCK:
                        lockActionInfo= lockingService.unlockDocument(documentId, user.getLogin(),lockData.getSessionId() , lockData.getLockLevel());
                        break;
                    case ELEMENT_LOCK:
                        lockActionInfo=lockingService.unlockDocument(documentId, user.getLogin(),lockData.getSessionId() , lockData.getLockLevel(), lockData.getElementId());
                        break;
                }//end switch
                if(!lockActionInfo.sucesss()){
                    lockHelper.handleLockFailure(lockActionInfo);
                }//end handle failure
            }//end   
        }//end for
    }

    @Subscribe
    public void editMetadata(EditMetadataRequestEvent event) throws IOException {
        if (lockHelper.lockDocument()) {
            LeosDocument document = getDocument();
            documentView.showMetadataEditWindow(documentService.getMetaData(document));
        }
    }

    @Subscribe
    public void downloadXml(DownloadXmlRequestEvent event) {
        LeosDocument document = getDocument();
        if (document != null){
            documentView.showDownloadWindow(document, "xml.window.download.message");
        }
    }

    @Subscribe
    public void saveToc(SaveTocRequestEvent event) throws IOException {
        if (lockHelper.isDocumentLockedFor()) {
            LeosDocument document = getDocument();
            document = documentService.saveTableOfContent(document, getUserLogin(), event.getTableOfContentItemVOs());

            List<TableOfContentItemVO> tableOfContent = getTableOfContent(document);
            documentView.setToc(tableOfContent);
            eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
            eventBus.post(new DocumentUpdatedEvent());
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Subscribe
    public void fetchTocAndAncestors(FetchCrossRefTocRequestEvent event) {
        LeosDocument document = getDocument();
        List<String> elementAncestorsIds = null;
        if (event.getElementId() != null) {
            elementAncestorsIds = documentService.getAncestorsIdsForElementId(document, event.getElementId());
        }
        // we are combining two operations (get toc +  get selected element ancestors)
        documentView.setTocAndAncestors(getTableOfContent(document), event.getElementId(), elementAncestorsIds);
    }
    
    
    @Subscribe
    public void fetchElement(FetchElementRequestEvent event) {
        LeosDocument document = getDocument();
        String contentForType= elementService.getElement(document, event.getElementTagName(), event.getElementId());
        String wrappedContentXml = wrapXmlFragment(contentForType);
        InputStream contentStream = new ByteArrayInputStream(wrappedContentXml.getBytes(StandardCharsets.UTF_8));
        contentForType = transformationManager.toXmlFragmentWrapper(contentStream, urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()));

        documentView.setElement(event.getElementId(), event.getElementTagName(), contentForType);
    }
    
    private String wrapXmlFragment(String xmlFragment) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><aknFragment xmlns=\"http://www.akomantoso.org/2.0\" xmlns:leos=\"urn:eu:europa:ec:leos\">" + xmlFragment + "</aknFragment>";
    }
    
    @Subscribe
    public void saveMetaData(SaveMetaDataRequestEvent event) {
        if (lockHelper.isDocumentLockedFor()) {
            LeosDocument document = getDocument();
            document = documentService.updateMetaData(document, getUserLogin(), event.getMetaDataVO());

            if (document != null) {
                eventBus.post(new NotificationEvent(Type.INFO, "metadata.edit.saved"));
                eventBus.post(new RefreshDocumentEvent());
                eventBus.post(new DocumentUpdatedEvent());
            }
        }
        else{
            eventBus.post(new NotificationEvent(Type.WARNING, "document.lock.lost"));
        }
    }

    @Override
    public DocumentView getView() {
        return documentView;
    }

    private String getDocumentContent(LeosDocument document) {
        return transformationManager.toEditableXml(document.getContentStream(), urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()));
    }

    private LeosDocument getDocument() {
        String documentId=getDocumentId();
        LeosDocument document = null;
        try{
            if (documentId != null) {
                document =  documentService.getDocument(documentId);
            }
        } catch(IllegalArgumentException iae){
            LOG.debug("Document {} can not be retrieved due to exception {}, Rejecting view", documentId, iae);
            rejectView(RepositoryView.VIEW_ID, "document.not.found", documentId);
        }

        return document;
    }

    private String getDocumentId() {
        String documentId =(String) session.getAttribute(SessionAttribute.DOCUMENT_ID.name());
        strLockId=documentId;//to set the id to receive lock updates
        return documentId;
    }

    private List<TableOfContentItemVO> getTableOfContent(LeosDocument leosDocument) {
        return documentService.getTableOfContent(leosDocument);
    }

    private void populateViewWithDocumentDetails(LeosDocument document) throws IOException {
        if (document != null) {
            documentView.setDocumentTitle(document.getTitle());
            documentView.setDocumentStage(document.getStage());
            documentView.refreshContent(getDocumentContent(document));
            documentView.setToc(getTableOfContent(document));
            String documentId = getDocumentId();
            documentView.setDocumentPreviewURLs(documentId,
                    urlBuilder.getDocumentPdfUrl(VaadinServletService.getCurrentServletRequest(), documentId),
                    urlBuilder.getDocumentHtmlUrl(VaadinServletService.getCurrentServletRequest(), documentId));
        }
    }

    private String getUserLogin(){
        return leosSecurityContext.getUser().getLogin();
    }

    @Override
    public void onLockUpdate(LockActionInfo lockActionInfo) {
        documentView.updateLocks(lockActionInfo);
    }

    /** to be used only for the lock update mechanism in the GUI 
     * as session is not available for async threads
     */
    @Override
    public String getLockId(){
        return strLockId;
    }
}
