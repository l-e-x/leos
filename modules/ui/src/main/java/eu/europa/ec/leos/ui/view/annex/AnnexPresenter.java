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
package eu.europa.ec.leos.ui.view.annex;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosCategory;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.processor.AnnexProcessor;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.user.UserService;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.ui.navigation.Target;
import io.atlassian.fugue.Option;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.ComparisonRequestEvent;
import eu.europa.ec.leos.web.event.component.MarkedContentRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.*;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
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
class AnnexPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexPresenter.class);

    private final AnnexScreen annexScreen;
    private final AnnexService annexService;
    private final ElementProcessor<Annex> elementProcessor;
    private final AnnexProcessor annexProcessor;
    private final TransformationService transformationService;
    private final UrlBuilder urlBuilder;
    private final ComparisonDelegate<Annex> comparisonDelegate;
    private final UserService userService;
    
    private static final String ANNEX_BLOCK_TAG = "blockContainer";
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    
    @Autowired
    AnnexPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
            AnnexScreen annexScreen,
            AnnexService annexService,
            ElementProcessor<Annex> elementProcessor, AnnexProcessor annexProcessor, TransformationService transformationService,
            UrlBuilder urlBuilder, ComparisonDelegate<Annex> comparisonDelegate, UserService userService) {
        super(securityContext, httpSession, eventBus);
        LOG.trace("Initializing annex presenter...");
        this.annexScreen = annexScreen;
        this.annexService = annexService;
        this.elementProcessor = elementProcessor;
        this.annexProcessor = annexProcessor;
        this.transformationService = transformationService;
        this.urlBuilder = urlBuilder;
        this.comparisonDelegate = comparisonDelegate;
        this.userService = userService;
    }

    @Override
    public void enter() {
        super.enter();
        populateViewData();
    }

    private String getDocumentId() {
        return (String) httpSession.getAttribute(SessionAttribute.ANNEX_ID.name());
    }

    private Annex getDocument() {
        String documentId = getDocumentId();
        return annexService.findAnnex(documentId);
    }

    private void populateViewData() {
        try{
            Annex annex = getDocument();
            Option<AnnexMetadata> annexMetadata = annex.getMetadata();
            if (annexMetadata.isDefined()) {
                annexScreen.setTitle(annexMetadata.get().getTitle());
            }
            annexScreen.setDocumentVersionInfo(getVersionInfo(annex));
            annexScreen.setContent(getEditableXml(annex));
        }
        catch (Exception ex) {
            LOG.error("Error while processing document", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }
 
    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Annex> event) {
        List<Annex> annexVersions = annexService.findVersions(getDocumentId());
        eventBus.post(new VersionListResponseEvent(new ArrayList<>(annexVersions)));
    }

    @Subscribe
    void getMarkedContent(MarkedContentRequestEvent<Annex> event) {
        Annex oldVersion = event.getOldVersion();
        Annex newVersion = event.getNewVersion();
        String markedContent = comparisonDelegate.getMarkedContent(oldVersion, newVersion);
        annexScreen.populateMarkedContent(markedContent);
    }
    
    private String getEditableXml(Annex document) {        
        return transformationService.toEditableXml(
                    new ByteArrayInputStream(getContent(document)),
                    urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()),  LeosCategory.ANNEX);
    }

    private byte[] getContent(Annex annex) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        return content.getSource().getByteString().toByteArray();
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
    
    private String getUserLogin() {
        return securityContext.getUser().getLogin();
    }

    @Subscribe
    void deleteAnnexBlock(DeleteElementRequestEvent event) throws IOException {
       String tagName = event.getElementTagName();
       if (ANNEX_BLOCK_TAG.equals(tagName)) { 
         Annex annex = getDocument();
         byte[] updatedXmlContent = elementProcessor.deleteElement(annex, event.getElementId(), tagName);

         // save document into repository
         annex = annexService.updateAnnex(annex.getId(), updatedXmlContent, false, "operation.annex.block.deleted");
         if (annex != null) {
             eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.deleted"));
             eventBus.post(new RefreshDocumentEvent());
             eventBus.post(new DocumentUpdatedEvent());
         }
       }
    }

    @Subscribe
    void insertAnnexBlock(InsertElementRequestEvent event) throws IOException {
        String tagName = event.getElementTagName();
        if (ANNEX_BLOCK_TAG.equals(tagName)) {
          Annex annex = getDocument();
          byte[] updatedXmlContent = annexProcessor.insertAnnexBlock(getContent(annex), event.getElementId(), InsertElementRequestEvent.POSITION.BEFORE.equals(event.getPosition()));

          annex = annexService.updateAnnex(annex.getId(), updatedXmlContent, false, "operation.annex.block.inserted");
          if (annex != null) {
              eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.inserted"));
              eventBus.post(new RefreshDocumentEvent());
              eventBus.post(new DocumentUpdatedEvent());
          }
       }
    }

    @Subscribe
    void editAnnexBlock(EditElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling edit element request... for {},id={}",elementTagName , elementId );

        try {
            Annex annex = getDocument();
            String element = elementProcessor.getElement(annex, elementTagName, elementId);
            annexScreen.showElementEditor(elementId, elementTagName, element);
        }
        catch (Exception ex){
            LOG.error("Exception while edit element operation for ", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void saveAnnexBlock(SaveElementRequestEvent event) throws IOException {
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling save element request... for {},id={}",elementTagName , elementId );

        try {
            Annex annex = getDocument();
            byte[] updatedXmlContent = elementProcessor.updateElement(annex, event.getElementContent(), elementTagName, elementId);

            annex = annexService.updateAnnex(annex.getId(), updatedXmlContent, false, "operation.annex.block.updated");

            if (annex != null) {
                String elementContent = elementProcessor.getElement(annex, elementTagName, elementId);
                annexScreen.refreshElementEditor(elementId, elementTagName, elementContent);
                eventBus.post(new DocumentUpdatedEvent());
                eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.updated"));
            }
        } catch (Exception ex) {
            LOG.error("Exception while save annex operation", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void closeAnnexBlock(CloseElementEditorEvent event) throws IOException {
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    void showTimeLineWindow(ShowTimeLineWindowEvent event) {
        List documentVersions = annexService.findVersions(getDocumentId());
        annexScreen.showTimeLineWindow(documentVersions);
    }

    @Subscribe
    void versionCompare(ComparisonRequestEvent<Annex> event) {
        Annex oldVersion = event.getOldVersion();
        Annex newVersion = event.getNewVersion();
        int displayMode = event.getDisplayMode();
        HashMap<Integer, Object> result = comparisonDelegate.versionCompare(oldVersion, newVersion, displayMode);
        annexScreen.displayComparison(result);        
    }

    @Subscribe
    public void showMajorVersionWindow(ShowMajorVersionWindowEvent event) {
        annexScreen.showMajorVersionWindow();
    }

    @Subscribe
    public void saveMajorVersion(SaveMajorVersionEvent event) {
        annexService.createVersion(getDocumentId(), event.isMajor(), event.getComments());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        populateViewData();
    }

    private VersionInfoVO getVersionInfo(XmlDocument document) {
        String userId = document.getLastModifiedBy();
        User user = userService.getUser(userId);

        return new VersionInfoVO(
                document.getVersionLabel(),
                user != null ? user.getName() : userId,
                user != null ? user.getDg() : "",
                dateFormatter.format(Date.from(document.getLastModificationInstant())),
                document.isMajorVersion());
    }
}
