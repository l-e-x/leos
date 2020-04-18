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
package eu.europa.ec.leos.ui.view.annex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;

import eu.europa.ec.leos.model.messaging.UpdateInternalReferencesMessage;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.messaging.UpdateInternalReferencesProducer;
import eu.europa.ec.leos.services.store.WorkspaceService;
import eu.europa.ec.leos.web.event.view.document.FetchCrossRefTocResponseEvent;
import eu.europa.ec.leos.web.event.view.document.FetchElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.FetchElementResponseEvent;
import eu.europa.ec.leos.web.event.view.document.ReferenceLabelResponseEvent;
import eu.europa.ec.leos.web.model.TocAndAncestorsVO;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.server.VaadinServletService;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.common.Result;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.model.annex.AnnexStructureType;
import eu.europa.ec.leos.model.event.DocumentUpdatedByCoEditorEvent;
import eu.europa.ec.leos.model.event.UpdateUserInfoEvent;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.ReferenceLabelService;
import eu.europa.ec.leos.services.content.processor.AnnexProcessor;
import eu.europa.ec.leos.services.content.processor.DocumentContentService;
import eu.europa.ec.leos.services.content.processor.ElementProcessor;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportService;
import eu.europa.ec.leos.services.store.PackageService;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.ui.component.ComparisonComponent;
import eu.europa.ec.leos.ui.event.CloseBrowserRequestEvent;
import eu.europa.ec.leos.ui.event.CloseScreenRequestEvent;
import eu.europa.ec.leos.ui.event.EnableSyncScrollRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.DocuWriteExportRequestEvent;
import eu.europa.ec.leos.ui.event.doubleCompare.DoubleCompareRequestEvent;
import eu.europa.ec.leos.ui.event.metadata.DocumentMetadataRequest;
import eu.europa.ec.leos.ui.event.metadata.DocumentMetadataResponse;
import eu.europa.ec.leos.ui.event.metadata.SearchMetadataRequest;
import eu.europa.ec.leos.ui.event.metadata.SearchMetadataResponse;
import eu.europa.ec.leos.ui.event.toc.CloseEditTocEvent;
import eu.europa.ec.leos.ui.event.toc.InlineTocCloseRequestEvent;
import eu.europa.ec.leos.ui.event.toc.InlineTocEditRequestEvent;
import eu.europa.ec.leos.ui.event.toc.RefreshTocEvent;
import eu.europa.ec.leos.ui.event.toc.SaveTocRequestEvent;
import eu.europa.ec.leos.ui.event.view.AnnexStructureChangeEvent;
import eu.europa.ec.leos.ui.model.AnnotateMetadata;
import eu.europa.ec.leos.ui.support.CoEditionHelper;
import eu.europa.ec.leos.ui.view.AbstractLeosPresenter;
import eu.europa.ec.leos.ui.view.ComparisonDelegate;
import eu.europa.ec.leos.ui.view.ComparisonDisplayMode;
import eu.europa.ec.leos.usecases.document.AnnexContext;
import eu.europa.ec.leos.usecases.document.BillContext;
import eu.europa.ec.leos.usecases.document.CollectionContext;
import eu.europa.ec.leos.usecases.document.ContextAction;
import eu.europa.ec.leos.vo.coedition.InfoType;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toc.TocItem;
import eu.europa.ec.leos.web.event.NavigationRequestEvent;
import eu.europa.ec.leos.web.event.NotificationEvent;
import eu.europa.ec.leos.web.event.NotificationEvent.Type;
import eu.europa.ec.leos.web.event.component.CleanComparedContentEvent;
import eu.europa.ec.leos.web.event.component.CompareRequestEvent;
import eu.europa.ec.leos.web.event.component.CompareTimeLineRequestEvent;
import eu.europa.ec.leos.web.event.component.LayoutChangeRequestEvent;
import eu.europa.ec.leos.web.event.component.RestoreVersionRequestEvent;
import eu.europa.ec.leos.web.event.component.ShowVersionRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListRequestEvent;
import eu.europa.ec.leos.web.event.component.VersionListResponseEvent;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent;
import eu.europa.ec.leos.web.event.view.document.CloseDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.ComparisonEvent;
import eu.europa.ec.leos.web.event.view.document.DeleteElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.DocumentUpdatedEvent;
import eu.europa.ec.leos.web.event.view.document.EditElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.FetchCrossRefTocRequestEvent;
import eu.europa.ec.leos.web.event.view.document.FetchUserPermissionsRequest;
import eu.europa.ec.leos.web.event.view.document.InsertElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.MergeSuggestionRequest;
import eu.europa.ec.leos.web.event.view.document.MergeSuggestionResponse;
import eu.europa.ec.leos.web.event.view.document.ReferenceLabelRequestEvent;
import eu.europa.ec.leos.web.event.view.document.RefreshDocumentEvent;
import eu.europa.ec.leos.web.event.view.document.SaveElementRequestEvent;
import eu.europa.ec.leos.web.event.view.document.SaveIntermediateVersionEvent;
import eu.europa.ec.leos.web.event.view.document.ShowIntermediateVersionWindowEvent;
import eu.europa.ec.leos.web.event.window.CloseElementEditorEvent;
import eu.europa.ec.leos.web.event.window.ShowTimeLineWindowEvent;
import eu.europa.ec.leos.web.model.VersionInfoVO;
import eu.europa.ec.leos.web.support.SessionAttribute;
import eu.europa.ec.leos.web.support.UrlBuilder;
import eu.europa.ec.leos.web.support.UuidHelper;
import eu.europa.ec.leos.web.support.cfg.ConfigurationHelper;
import eu.europa.ec.leos.web.support.user.UserHelper;
import eu.europa.ec.leos.web.support.xml.DownloadStreamResource;
import eu.europa.ec.leos.web.ui.navigation.Target;
import eu.europa.ec.leos.web.ui.screen.document.ColumnPosition;
import io.atlassian.fugue.Option;

import static eu.europa.ec.leos.services.support.xml.XmlHelper.LEVEL;
import static eu.europa.ec.leos.services.support.xml.XmlHelper.POINT;

@Component
@Scope("prototype")
class AnnexPresenter extends AbstractLeosPresenter {

    private static final Logger LOG = LoggerFactory.getLogger(AnnexPresenter.class);

    private final AnnexScreen annexScreen;
    private final AnnexService annexService;
    private final ElementProcessor<Annex> elementProcessor;
    private final AnnexProcessor annexProcessor;
    private final DocumentContentService documentContentService;
    private final UrlBuilder urlBuilder;
    private final ComparisonDelegate<Annex> comparisonDelegate;
    private final UserHelper userHelper;
    private final MessageHelper messageHelper;
    private final ConfigurationHelper cfgHelper;
    private final Provider<CollectionContext> proposalContextProvider;
    private final CoEditionHelper coEditionHelper;
    private final ExportService exportService;
    private final Provider<BillContext> billContextProvider;
    private final Provider<StructureContext> structureContextProvider;
    private final ReferenceLabelService referenceLabelService;
    private final Provider<AnnexContext> annexContextProvider;
    private final UpdateInternalReferencesProducer updateInternalReferencesProducer;
    private final TransformationService transformationService;;

    private String strDocumentVersionSeriesId;
    private String documentId;
    private String documentRef;
    private boolean comparisonMode;
    private final static SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Autowired
    AnnexPresenter(SecurityContext securityContext, HttpSession httpSession, EventBus eventBus,
                   AnnexScreen annexScreen,
                   AnnexService annexService, PackageService packageService, ExportService exportService,
                   Provider<BillContext> billContextProvider, Provider<AnnexContext> annexContextProvider, ElementProcessor<Annex> elementProcessor,
                   AnnexProcessor annexProcessor, DocumentContentService documentContentService, UrlBuilder urlBuilder,
                   ComparisonDelegate<Annex> comparisonDelegate, UserHelper userHelper,
                   MessageHelper messageHelper, ConfigurationHelper cfgHelper, Provider<CollectionContext> proposalContextProvider,
                   CoEditionHelper coEditionHelper, EventBus leosApplicationEventBus, UuidHelper uuidHelper,
                   Provider<StructureContext> structureContextProvider, ReferenceLabelService referenceLabelService, WorkspaceService workspaceService,
                   UpdateInternalReferencesProducer updateInternalReferencesProducer, TransformationService transformationService) {
        super(securityContext, httpSession, eventBus, leosApplicationEventBus, uuidHelper, packageService, workspaceService);
        LOG.trace("Initializing annex presenter...");
        this.annexScreen = annexScreen;
        this.annexService = annexService;
        this.elementProcessor = elementProcessor;
        this.annexProcessor = annexProcessor;
        this.documentContentService = documentContentService;
        this.urlBuilder = urlBuilder;
        this.comparisonDelegate = comparisonDelegate;
        this.userHelper = userHelper;
        this.messageHelper = messageHelper;
        this.cfgHelper = cfgHelper;
        this.proposalContextProvider = proposalContextProvider;
        this.coEditionHelper = coEditionHelper;
        this.exportService = exportService;
        this.billContextProvider = billContextProvider;
        this.annexContextProvider = annexContextProvider;
        this.structureContextProvider = structureContextProvider;
        this.referenceLabelService = referenceLabelService;
        this.updateInternalReferencesProducer = updateInternalReferencesProducer;
        this.transformationService = transformationService;
    }
    
    @Override
    public void enter() {
        super.enter();
        init();
    }

    @Override
    public void detach() {
        super.detach();
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
    }
    
    private void init() {
        try {
            populateViewData(TocMode.SIMPLIFIED);
            populateVersionsData();
        } catch (Exception exception) {
            LOG.error("Exception occurred in init(): ", exception);
            eventBus.post(new NotificationEvent(Type.INFO, "unknown.error.message"));
        }
    }
    
    private String getDocumentRef() {
        return (String) httpSession.getAttribute(id + "." + SessionAttribute.ANNEX_REF.name());
    }

    private Annex getDocument() {
        documentRef = getDocumentRef();
        Annex annex = annexService.findAnnexByRef(documentRef);
        strDocumentVersionSeriesId = annex.getVersionSeriesId();
        documentId = annex.getId();
        structureContextProvider.get().useDocumentTemplate(annex.getMetadata().getOrError(() -> "Annex metadata is required!").getDocTemplate());
        return annex;
    }

    private void populateViewData(TocMode mode) {
        try{
            Annex annex = getDocument();
            Option<AnnexMetadata> annexMetadata = annex.getMetadata();
            if (annexMetadata.isDefined()) {
                annexScreen.setTitle(annexMetadata.get().getTitle());
            }
            annexScreen.setDocumentVersionInfo(getVersionInfo(annex));
            annexScreen.setContent(getEditableXml(annex));
            annexScreen.setToc(getTableOfContent(annex, mode));
            annexScreen.setStructureChangeMenuItem();
            DocumentVO annexVO = createAnnexVO(annex);
            annexScreen.updateUserCoEditionInfo(coEditionHelper.getCurrentEditInfo(annex.getVersionSeriesId()), id);
            annexScreen.setPermissions(annexVO);
        }
        catch (Exception ex) {
            LOG.error("Error while processing document", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }
    
    private void populateVersionsData() {
        final List<VersionVO> allVersions = annexService.getAllVersions(documentId, documentRef);
        annexScreen.setDataFunctions(
                allVersions,
                this::majorVersionsFn, this::countMajorVersionsFn,
                this::minorVersionsFn, this::countMinorVersionsFn,
                this::recentChangesFn, this::countRecentChangesFn);
    }
    
    @Subscribe
    public void updateVersionsTab(DocumentUpdatedEvent event) {
        final List<VersionVO> allVersions = annexService.getAllVersions(documentId, documentRef);
        annexScreen.refreshVersions(allVersions, comparisonMode);
    }
    
    private Integer countMinorVersionsFn(String currIntVersion) {
        return annexService.findAllMinorsCountForIntermediate(documentRef, currIntVersion);
    }
    
    private List<Annex> minorVersionsFn(String currIntVersion, int startIndex, int maxResults) {
        return annexService.findAllMinorsForIntermediate(documentRef, currIntVersion, startIndex, maxResults);
    }
    
    private Integer countMajorVersionsFn() {
        return annexService.findAllMajorsCount(documentRef);
    }
    
    private List<Annex> majorVersionsFn(int startIndex, int maxResults) {
        return annexService.findAllMajors(documentRef, startIndex, maxResults);
    }
    
    private Integer countRecentChangesFn() {
        return annexService.findRecentMinorVersionsCount(documentId, documentRef);
    }
    
    private List<Annex> recentChangesFn(int startIndex, int maxResults) {
        return annexService.findRecentMinorVersions(documentId, documentRef, startIndex, maxResults);
    }
 
    private List<TableOfContentItemVO> getTableOfContent(Annex annex, TocMode mode) {
        return annexService.getTableOfContent(annex, mode);
    }

    @Subscribe
    void exportToDocuWrite(DocuWriteExportRequestEvent<Annex> event) {
        ExportOptions exportOptions = event.getExportOptions();
        try {
            LeosPackage leosPackage = packageService.findPackageByDocumentId(documentId);
            BillContext context = billContextProvider.get();
            context.usePackage(leosPackage);
            String proposalId = context.getProposalIdFromBill();
            if (proposalId != null) {
                Optional<XmlDocument> versionToCompare = Optional.of(event.getVersion());
                File resultOutputFile = exportService.createDocuWritePackage("Proposal_" + proposalId + "_" + System.currentTimeMillis()+".zip", proposalId, exportOptions,
                        versionToCompare);
                DownloadStreamResource downloadStreamResource = new DownloadStreamResource(resultOutputFile.getName(), new FileInputStream(resultOutputFile));
                annexScreen.setDownloadStreamResource(downloadStreamResource);
            }
        } catch (Exception e) {
            LOG.error("Unexpected error occurred while using ExportService - {}", e.getMessage());
            eventBus.post(new NotificationEvent(Type.ERROR, "export.docuwrite.error.message"));
        }
    }

    @Subscribe
    void getDocumentVersionsList(VersionListRequestEvent<Annex> event) {
        List<Annex> annexVersions = annexService.findVersions(documentId);
        eventBus.post(new VersionListResponseEvent<Annex>(new ArrayList<>(annexVersions)));
    }
    
    private String getEditableXml(Annex document) {
        return documentContentService.toEditableContent(document,
                urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()), securityContext);
    }

    private byte[] getContent(Annex annex) {
        final Content content = annex.getContent().getOrError(() -> "Annex content is required!");
        return content.getSource().getBytes();
    }

    @Subscribe
    void handleCloseDocument(CloseDocumentEvent event) {
        LOG.trace("Handling close document request...");
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
        eventBus.post(new NavigationRequestEvent(Target.PREVIOUS));
    }

    @Subscribe
    void handleCloseBrowserRequest(CloseBrowserRequestEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.DOCUMENT_INFO);
    }

    @Subscribe
    void handleCloseScreenRequest(CloseScreenRequestEvent event) {
        if (annexScreen.isTocEnabled()) {
            eventBus.post(new CloseEditTocEvent());
        } else {
            eventBus.post(new CloseDocumentEvent());
        }   
    }

    @Subscribe
    void refreshDocument(RefreshDocumentEvent event) {
        populateViewData(event.getTocMode());
    }
    
    @Subscribe
    void refreshToc(RefreshTocEvent event) {
        try {
            Annex annex = getDocument();
            annexScreen.setToc(getTableOfContent(annex, event.getTocMode()));
        } catch (Exception ex) {
            LOG.error("Error while refreshing TOC", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }
    
    @Subscribe
    void deleteAnnexBlock(DeleteElementRequestEvent event){
       String tagName = event.getElementTagName();
       Annex annex = getDocument();
       byte[] updatedXmlContent = annexProcessor.deleteAnnexBlock(annex, event.getElementId(), tagName);

       // save document into repository
       annex = annexService.updateAnnex(annex, updatedXmlContent, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.deleted"));
       if (annex != null) {
           eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.deleted", tagName.equalsIgnoreCase(LEVEL) ? StringUtils.capitalize(POINT) : StringUtils.capitalize(tagName)));
           eventBus.post(new RefreshDocumentEvent());
           eventBus.post(new DocumentUpdatedEvent());
           leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
           updateInternalReferencesProducer.send(new UpdateInternalReferencesMessage(annex.getId(), annex.getMetadata().get().getRef(), id));
       }
    }

    @Subscribe
    void insertAnnexBlock(InsertElementRequestEvent event){
        String tagName = event.getElementTagName();
        Annex annex = getDocument();
        byte[] updatedXmlContent = annexProcessor.insertAnnexBlock(annex, event.getElementId(), tagName, InsertElementRequestEvent.POSITION.BEFORE.equals(event.getPosition()));

        annex = annexService.updateAnnex(annex, updatedXmlContent, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.inserted"));
        if (annex != null) {
            eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.inserted",  tagName.equalsIgnoreCase(LEVEL) ? StringUtils.capitalize(POINT) : StringUtils.capitalize(tagName)));
            eventBus.post(new RefreshDocumentEvent());
            eventBus.post(new DocumentUpdatedEvent());
            leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
            updateInternalReferencesProducer.send(new UpdateInternalReferencesMessage(annex.getId(), annex.getMetadata().get().getRef(), id));
        }
    }

    @Subscribe
    void CheckAnnexBlockCoEdition(CheckElementCoEditionEvent event) {
        annexScreen.checkElementCoEdition(coEditionHelper.getCurrentEditInfo(strDocumentVersionSeriesId), user,
                event.getElementId(), event.getElementTagName(), event.getAction(), event.getActionEvent());
    }

    @Subscribe
    void editAnnexBlock(EditElementRequestEvent event){
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        
        LOG.trace("Handling edit element request... for {},id={}",elementTagName , elementId );

        try {
            Annex annex = getDocument();
            String element = elementProcessor.getElement(annex, elementTagName, elementId);
            coEditionHelper.storeUserEditInfo(httpSession.getId(), id, user, strDocumentVersionSeriesId, elementId, InfoType.ELEMENT_INFO);
            annexScreen.showElementEditor(elementId, elementTagName, element);
        }
        catch (Exception ex){
            LOG.error("Exception while edit element operation for ", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void saveElement(SaveElementRequestEvent event){
        String elementId = event.getElementId();
        String elementTagName = event.getElementTagName();
        LOG.trace("Handling save element request... for {},id={}",elementTagName , elementId );

        try {
            Annex annex = getDocument();
            byte[] updatedXmlContent = elementProcessor.updateElement(annex, event.getElementContent(), elementTagName, elementId);
            if (updatedXmlContent == null) {
                annexScreen.showAlertDialog("operation.element.not.performed");
                return;
            }

            annex = annexService.updateAnnex(annex, updatedXmlContent, VersionType.MINOR, messageHelper.getMessage("operation.annex.block.updated"));

            if (annex != null) {
                String elementContent = elementProcessor.getElement(annex, elementTagName, elementId);
                annexScreen.refreshElementEditor(elementId, elementTagName, elementContent);
                eventBus.post(new DocumentUpdatedEvent());
                eventBus.post(new NotificationEvent(Type.INFO, "document.annex.block.updated", elementTagName.equalsIgnoreCase(LEVEL) ?  StringUtils.capitalize(POINT) : StringUtils.capitalize(elementTagName)));
                annexScreen.scrollToMarkedChange(elementId);
                leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
            }
        } catch (Exception ex) {
            LOG.error("Exception while save annex operation", ex);
            eventBus.post(new NotificationEvent(Type.INFO, "error.message", ex.getMessage()));
        }
    }

    @Subscribe
    void closeAnnexBlock(CloseElementEditorEvent event){
        String elementId = event.getElementId();
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, elementId, InfoType.ELEMENT_INFO);
        LOG.debug("User edit information removed");
        eventBus.post(new RefreshDocumentEvent());
    }

    @Subscribe
    void showTimeLineWindow(ShowTimeLineWindowEvent event) {
        List<Annex> documentVersions = annexService.findVersions(documentId);
        annexScreen.showTimeLineWindow(documentVersions);
    }
    
    @Subscribe
    void cleanComparedContent(CleanComparedContentEvent event) {
        annexScreen.cleanComparedContent();
    }
    
    @Subscribe
    void showVersion(ShowVersionRequestEvent event) {
        final Annex version = annexService.findAnnexVersion(event.getVersionId());
        final String versionContent = comparisonDelegate.getDocumentAsHtml(version);
        final String versionInfo = getVersionInfoAsString(version);
        annexScreen.showVersion(versionContent, versionInfo);
        eventBus.post(new EnableSyncScrollRequestEvent(true));
    }
    
    @Subscribe
    void getCompareContentForTimeLine(CompareTimeLineRequestEvent event) {
        final Annex oldVersion = annexService.findAnnexVersion(event.getOldVersion());
        final Annex newVersion = annexService.findAnnexVersion(event.getNewVersion());
        final ComparisonDisplayMode displayMode = event.getDisplayMode();
        HashMap<ComparisonDisplayMode, Object> result = comparisonDelegate.versionCompare(oldVersion, newVersion, displayMode);
        annexScreen.displayComparison(result);
    }
    
    @Subscribe
    void compare(CompareRequestEvent event) {
        final Annex oldVersion = annexService.findAnnexVersion(event.getOldVersionId());
        final Annex newVersion = annexService.findAnnexVersion(event.getNewVersionId());
        String comparedContent = comparisonDelegate.getMarkedContent(oldVersion, newVersion);
        final String comparedInfo = messageHelper.getMessage("version.compare.simple", oldVersion.getVersionLabel(), newVersion.getVersionLabel());
        annexScreen.populateComparisonContent(comparedContent, comparedInfo);
        eventBus.post(new EnableSyncScrollRequestEvent(true));
    }
    
    @Subscribe
    void doubleCompare(DoubleCompareRequestEvent event) {
        final Annex original = annexService.findAnnexVersion(event.getOriginalProposalId());
        final Annex intermediate = annexService.findAnnexVersion(event.getIntermediateMajorId());
        final Annex current = annexService.findAnnexVersion(event.getCurrentId());
        String resultContent = comparisonDelegate.doubleCompareHtmlContents(original, intermediate, current, event.isEnabled());
        final String comparedInfo = messageHelper.getMessage("version.compare.double", original.getVersionLabel(), intermediate.getVersionLabel(), current.getVersionLabel());
        annexScreen.populateDoubleComparisonContent(resultContent, comparedInfo);
    }
    
    @Subscribe
    void versionRestore(RestoreVersionRequestEvent event) {
        final Annex version = annexService.findAnnexVersion(event.getVersionId());
        final byte[] resultXmlContent = getContent(version);
        annexService.updateAnnex(getDocument(), resultXmlContent, VersionType.MINOR, messageHelper.getMessage("operation.restore.version", version.getVersionLabel()));
        
        List<Annex> documentVersions = annexService.findVersions(documentId);
        annexScreen.updateTimeLineWindow(documentVersions);
        eventBus.post(new RefreshDocumentEvent());
        eventBus.post(new DocumentUpdatedEvent()); //Document might be updated.
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
    }
    
    private String getVersionInfoAsString(XmlDocument document) {
        final VersionInfoVO versionInfo = getVersionInfo(document);
        final String versionInfoString = messageHelper.getMessage(
                "document.version.caption",
                versionInfo.getDocumentVersion(),
                versionInfo.getLastModifiedBy(),
                versionInfo.getEntity(),
                versionInfo.getLastModificationInstant()
        );
        return versionInfoString;
    }
    
    @Subscribe
    public void changeComparisionMode(ComparisonEvent event) {
        comparisonMode = event.isComparsionMode();
        if (comparisonMode) {
            annexScreen.cleanComparedContent();
            if (!annexScreen.isComparisonComponentVisible()) {
                LayoutChangeRequestEvent layoutEvent = new LayoutChangeRequestEvent(ColumnPosition.DEFAULT, ComparisonComponent.class);
                eventBus.post(layoutEvent);
            }
        } else {
            LayoutChangeRequestEvent layoutEvent = new LayoutChangeRequestEvent(ColumnPosition.OFF, ComparisonComponent.class);
            eventBus.post(layoutEvent);
        }
        updateVersionsTab(new DocumentUpdatedEvent());
    }

    @Subscribe
    public void showIntermediateVersionWindow(ShowIntermediateVersionWindowEvent event) {
        annexScreen.showIntermediateVersionWindow();
    }

    @Subscribe
    public void saveIntermediateVersion(SaveIntermediateVersionEvent event) {
        final Annex annex = annexService.createVersion(documentId, event.getVersionType(), event.getCheckinComment());
        eventBus.post(new NotificationEvent(NotificationEvent.Type.INFO, "document.major.version.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, annex.getVersionSeriesId(), id));
        populateViewData(TocMode.SIMPLIFIED);
    }

    @Subscribe
    void saveToc(SaveTocRequestEvent event) {
        Annex annex = getDocument();
        AnnexStructureType sturcutureType = getStructureType();
        annex = annexService.saveTableOfContent(annex, event.getTableOfContentItemVOs(), sturcutureType, messageHelper.getMessage("operation.toc.updated"), user);

        eventBus.post(new NotificationEvent(Type.INFO, "toc.edit.saved"));
        eventBus.post(new DocumentUpdatedEvent());
        leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
        updateInternalReferencesProducer.send(new UpdateInternalReferencesMessage(annex.getId(), annex.getMetadata().get().getRef(), id));
    }

    private AnnexStructureType getStructureType() {
        List<TocItem> tocItems = structureContextProvider.get().getTocItems().stream().
        filter(tocItem -> (tocItem.getAknTag().value().equalsIgnoreCase(AnnexStructureType.LEVEL.getType()) ||
                tocItem.getAknTag().value().equalsIgnoreCase(AnnexStructureType.ARTICLE.getType()))).collect(Collectors.toList());
        return AnnexStructureType.valueOf(tocItems.get(0).getAknTag().value().toUpperCase());
    }

    @Subscribe
    public void getUserPermissions(FetchUserPermissionsRequest event) {
        Annex annex = getDocument();
        List<LeosPermission> userPermissions = securityContext.getPermissions(annex);
        annexScreen.sendUserPermissions(userPermissions);
    }

    @Subscribe
    public void fetchSearchMetadata(SearchMetadataRequest event){
        eventBus.post(new SearchMetadataResponse(Collections.emptyList()));
    }

    @Subscribe
    public void fetchMetadata(DocumentMetadataRequest event){
        AnnotateMetadata metadata = new AnnotateMetadata();
        Annex annex = getDocument();
        metadata.setVersion(annex.getVersionLabel());
        metadata.setId(annex.getId());
        metadata.setTitle(annex.getTitle());
        eventBus.post(new DocumentMetadataResponse(metadata));
    }

    @Subscribe
    void mergeSuggestion(MergeSuggestionRequest event) {
        Annex document = getDocument();
        byte[] resultXmlContent = elementProcessor.replaceTextInElement(document, event.getOrigText(), event.getNewText(), event.getElementId(), event.getStartOffset(), event.getEndOffset());
        if (resultXmlContent == null) {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
            return;
        }
        document = annexService.updateAnnex(document, resultXmlContent, VersionType.MINOR, messageHelper.getMessage("operation.merge.suggestion"));
        if (document != null) {
            eventBus.post(new RefreshDocumentEvent());
            eventBus.post(new DocumentUpdatedEvent());
            leosApplicationEventBus.post(new DocumentUpdatedByCoEditorEvent(user, strDocumentVersionSeriesId, id));
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.success"), MergeSuggestionResponse.Result.SUCCESS));
        }
        else {
            eventBus.post(new MergeSuggestionResponse(messageHelper.getMessage("document.merge.suggestion.failed"), MergeSuggestionResponse.Result.ERROR));
        }
    }

    private VersionInfoVO getVersionInfo(XmlDocument document) {
        String userId = document.getLastModifiedBy();
        User user = userHelper.getUser(userId);

        return new VersionInfoVO(
                document.getVersionLabel(),
                user.getName(), user.getDefaultEntity() != null ? user.getDefaultEntity().getOrganizationName() : "",
                dateFormatter.format(Date.from(document.getLastModificationInstant())),
                document.getVersionType());
    }

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
            annexVO.getMetadata().setInternalRef(metadata.getRef());
        }
        if(!annex.getCollaborators().isEmpty()) {
            annexVO.addCollaborators(annex.getCollaborators());
        }
        return annexVO;
    }

    @Subscribe
    void updateProposalMetadata(DocumentUpdatedEvent event) {
        if (event.isModified()) {
            CollectionContext context = proposalContextProvider.get();
            context.useChildDocument(documentId);
            context.executeUpdateProposalAsync();
        }
    }

    @Subscribe
    public void onInfoUpdate(UpdateUserInfoEvent updateUserInfoEvent) {
        if(isCurrentInfoId(updateUserInfoEvent.getActionInfo().getInfo().getDocumentId())) {
            if (!id.equals(updateUserInfoEvent.getActionInfo().getInfo().getPresenterId())) {
                eventBus.post(new NotificationEvent(leosUI, "coedition.caption", "coedition.operation." + updateUserInfoEvent.getActionInfo().getOperation().getValue(),
                        NotificationEvent.Type.TRAY, updateUserInfoEvent.getActionInfo().getInfo().getUserName()));
            }
            LOG.debug("Annex Presenter updated the edit info -" + updateUserInfoEvent.getActionInfo().getOperation().name());
            annexScreen.updateUserCoEditionInfo(updateUserInfoEvent.getActionInfo().getCoEditionVos(), id);
        }
    }
    
    private boolean isCurrentInfoId(String versionSeriesId) {
        return versionSeriesId.equals(strDocumentVersionSeriesId);
    }
    
    @Subscribe
    public void documentUpdatedByCoEditor(DocumentUpdatedByCoEditorEvent documentUpdatedByCoEditorEvent) {
        if (isCurrentInfoId(documentUpdatedByCoEditorEvent.getDocumentId()) &&
                !id.equals(documentUpdatedByCoEditorEvent.getPresenterId())) {
            eventBus.post(new NotificationEvent(leosUI, "coedition.caption", "coedition.operation.update", NotificationEvent.Type.TRAY,
                    documentUpdatedByCoEditorEvent.getUser().getName()));
            annexScreen.displayDocumentUpdatedByCoEditorWarning();
        }
    }
    
    @Subscribe
    void editInlineToc(InlineTocEditRequestEvent event) {
        Annex annex = getDocument();
        coEditionHelper.storeUserEditInfo(httpSession.getId(), id, user, strDocumentVersionSeriesId, null, InfoType.TOC_INFO);
        annexScreen.enableTocEdition(getTableOfContent(annex, TocMode.NOT_SIMPLIFIED));
    }

    @Subscribe
    void closeInlineToc(InlineTocCloseRequestEvent event) {
        coEditionHelper.removeUserEditInfo(id, strDocumentVersionSeriesId, null, InfoType.TOC_INFO);
        LOG.debug("User edit information removed");
    }

    @Subscribe
    void fetchTocAndAncestors(FetchCrossRefTocRequestEvent event) {
        Annex annex = getDocument();
        List<String> elementAncestorsIds = null;
        if (event.getElementIds() != null && event.getElementIds().size() > 0) {
            try {
                elementAncestorsIds = annexService.getAncestorsIdsForElementId(annex, event.getElementIds());
            } catch (Exception e) {
                LOG.warn("Could not get ancestors Ids", e);
            }
        }
        // we are combining two operations (get toc + get selected element ancestors)
        final Map<String, List<TableOfContentItemVO>> tocItemList = packageService.getTableOfContent(annex.getId(), TocMode.SIMPLIFIED_CLEAN);
        eventBus.post(new FetchCrossRefTocResponseEvent(new TocAndAncestorsVO(tocItemList, elementAncestorsIds, messageHelper)));
    }

    @Subscribe
    void fetchElement(FetchElementRequestEvent event) {
        XmlDocument document = workspaceService.findDocumentByRef(event.getDocumentRef(), XmlDocument.class);
        String contentForType = elementProcessor.getElement(document, event.getElementTagName(), event.getElementId());
        String wrappedContentXml = wrapXmlFragment(contentForType != null ? contentForType : "");
        InputStream contentStream = new ByteArrayInputStream(wrappedContentXml.getBytes(StandardCharsets.UTF_8));
        contentForType = transformationService.toXmlFragmentWrapper(contentStream, urlBuilder.getWebAppPath(VaadinServletService.getCurrentServletRequest()),
                securityContext.getPermissions(document));

        eventBus.post(new FetchElementResponseEvent(event.getElementId(), event.getElementTagName(), contentForType, event.getDocumentRef()));
    }

    @Subscribe
    void fetchReferenceLabel(ReferenceLabelRequestEvent event) {
        // Validate
        if (event.getReferences().size() < 1) {
            eventBus.post(new NotificationEvent(Type.ERROR, "unknown.error.message"));
            LOG.error("No reference found in the request from client");
            return;
        }

        final byte[] sourceXmlContent = getDocument().getContent().get().getSource().getBytes();
        Result<String> updatedLabel = referenceLabelService.generateLabelStringRef(event.getReferences(), getDocumentRef(), event.getCurrentElementID(), sourceXmlContent, event.getDocumentRef(), true);
        eventBus.post(new ReferenceLabelResponseEvent(updatedLabel.get(), event.getDocumentRef()));
    }


    @Subscribe
    void structureChangeHandler(AnnexStructureChangeEvent event) {
        AnnexContext annexContext = annexContextProvider.get();
        String elementType = event.getStructureType().getType();
        String template = cfgHelper.getProperty("leos.annex." + elementType + ".template");
        structureContextProvider.get().useDocumentTemplate(template);
        annexContext.useTemplate(template);
        annexContext.useAnnexId(documentId);
        annexContext.useActionMessage(ContextAction.ANNEX_STRUCTURE_UPDATED, messageHelper.getMessage("operation.annex.switch."+ elementType +".structure"));
        annexContext.executeUpdateAnnexStructure();
        refreshView(elementType);
    }

    private void refreshView(String elementType) {
        eventBus.post(new NavigationRequestEvent(Target.ANNEX, getDocumentRef()));
        eventBus.post(new NotificationEvent(Type.INFO, "annex.structure.changed.message." + elementType));
    }
}