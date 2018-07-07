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
package eu.europa.ec.leos.ui.view.memorandum;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.GuidanceService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.MemorandumService;
import eu.europa.ec.leos.services.user.UserService;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.*;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.ui.navigation.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
@Scope("prototype")
class MemorandumPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(MemorandumPresenter.class);

    private final MemorandumScreen memorandumScreen;
    private final MemorandumService memorandumService;
    private final ElementProcessor<Memorandum> elementProcessor;
    private final TransformationService transformationService;
    private final UrlBuilder urlBuilder;
    private final GuidanceService guidanceService;
    private final ComparisonDelegate<Memorandum> comparisonDelegate;
    private final UserService userService;
    
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Autowired
    MemorandumPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                        MemorandumScreen memorandumScreen,
                        MemorandumService memorandumService,
                        ElementProcessor<Memorandum> elementProcessor,
                        TransformationService transformationService,
                        UrlBuilder urlBuilder,
                        GuidanceService guidanceService,
                        ComparisonDelegate<Memorandum> comparisonDelegate, 
                        UserService userService) {
        super(securityContext, httpSession, eventBus);
        LOG.trace("Initializing memorandum presenter...");
        this.memorandumScreen = memorandumScreen;
        this.memorandumService = memorandumService;
        this.elementProcessor = elementProcessor;
        this.transformationService = transformationService;
        this.urlBuilder = urlBuilder;
        this.guidanceService = guidanceService;
        this.comparisonDelegate = comparisonDelegate;
        this.userService = userService;
    }

    @Override
    public void enter() {
        super.enter();
        populateViewData();
    }

    private String getDocumentId() {
        return (String) httpSession.getAttribute(SessionAttribute.MEMORANDUM_ID.name());
    }

    private Memorandum getDocument() {
        String documentId = getDocumentId();
        return memorandumService.findMemorandum(documentId);
    }

    private void populateViewData() {
        try{
            Memorandum memorandum = getDocument();
            memorandumScreen.setTitle("Explanatory Memorandum"); //FIXME Temporary implementation waiting for Memorandum title feature development
            memorandumScreen.setDocumentVersionInfo(getVersionInfo(memorandum));
            String content = getEditableXml(memorandum);
            memorandumScreen.setContent(content);
            memorandumScreen.setToc(getTableOfContent(memorandum));
        }
        catch (Exception ex) {
            LOG.error("Error while processing document", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }
    
    private List<TableOfContentItemVO> getTableOfContent(Memorandum memorandum) {
        return memorandumService.getTableOfContent(memorandum);
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Memorandum> event) {
        List<Memorandum> memoVersions = memorandumService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent(new ArrayList<>(memoVersions)));
    }

    @Subscribe
    void getMarkedContent(MarkedContentRequestEvent<Memorandum> event) {
        Memorandum oldVersion = event.getOldVersion();
        Memorandum newVersion = event.getNewVersion();
        String markedContent = comparisonDelegate.getMarkedContent(oldVersion, newVersion);
        memorandumScreen.populateMarkedContent(markedContent);
    }

    private String getEditableXml(Memorandum memorandum) {
        return transformationService.toEditableXml(
                    new ByteArrayInputStream(getContent(memorandum)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), LeosCategory.MEMORANDUM);
    }

    @Subscribe
    void handleCloseScreenRequest(CloseScreenRequestEvent event) {
        LOG.trace("Handling close screen request...");
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void refreshDocument(RefreshDocumentEvent event) throws IOException {
        populateViewData();
    }

    @Subscribe
    void editElement(EditElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling edit element request... for {},id={}",elementTagName , elementId );

        try {
            Memorandum memorandum = getDocument();
            String element = elementProcessor.getElement(memorandum, elementTagName, elementId);
            memorandumScreen.showElementEditor(elementId, elementTagName, element);
        }
        catch (Exception ex){
            LOG.error("Exception while edit element operation for memorandum", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void saveElement(SaveElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling save element request... for {},id={}",elementTagName , elementId );

        try {
            Memorandum memorandum = getDocument();
            byte[] newXmlContent = elementProcessor.updateElement(memorandum, event.getElementContent(), elementTagName, elementId);
            memorandum = memorandumService.updateMemorandum(memorandum.getId(),newXmlContent, false, "operation." + elementTagName + ".updated");

            if (memorandum != null) {
                String elementContent = elementProcessor.getElement(memorandum, elementTagName, elementId);
                memorandumScreen.refreshElementEditor(elementId, elementTagName, elementContent);
                eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
                eventBus.post(new NotificationEvent(Type.INFO, "document.content.updated"));
            }
        } catch (Exception ex) {
            LOG.error("Exception while save  memorandum operation", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void closeElementEditor(CloseElementEditorEvent event) throws IOException {
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    public void getUserGuidance(FetchUserGuidanceRequest event) {
        // KLUGE temporary hack for compatibility with new domain model
        String documentId = getDocumentId();
        Memorandum memorandum = memorandumService.findMemorandum(documentId);
        String jsonGuidance = guidanceService.getGuidance(memorandum.getMetadata().get().getDocTemplate());
        memorandumScreen.setUserGuidance(jsonGuidance);
    }

    @Subscribe
    void showTimeLineWindow(ShowTimeLineWindowEvent event) {
        List documentVersions = memorandumService.findVersions(getDocumentId());
        memorandumScreen.showTimeLineWindow(documentVersions);
    }
    
    @Subscribe
    void versionCompare(ComparisonRequestEvent<Memorandum> event) {
        Memorandum oldVersion = event.getOldVersion();
        Memorandum newVersion = event.getNewVersion();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(oldVersion, newVersion, displayMode);
        memorandumScreen.displayComparison(result);        
    }

    @Subscribe
    public void showMajorVersionWindow(ShowMajorVersionWindowEvent event) {
        memorandumScreen.showMajorVersionWindow();
    }

    @Subscribe
    public void saveMajorVersion(SaveMajorVersionEvent event) {
        memorandumService.createVersion(getDocumentId(), event.isMajor(), event.getComments());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        populateViewData();
    }

    private byte[] getContent(Memorandum memorandum) {
        final Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        return content.getSource().getByteString().toByteArray();
    }
    
    private VersionInfoVO getVersionInfo(XmlDocument document){
        String userId = document.getLastModifiedBy();
        User user = userService.getUser(userId);

        return  new VersionInfoVO(
                document.getVersionLabel(),
                user!=null? user.getName(): userId,
                user!=null? user.getDg(): "",
                dateFormatter.format(Date.from(document.getLastModificationInstant())),
                document.isMajorVersion());
    }
}