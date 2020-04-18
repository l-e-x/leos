package eu.europa.ec.leos.services.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.cmis.document.MediaDocument;
import eu.europa.ec.leos.domain.cmis.document.Memorandum;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.LegDocumentVO;
import eu.europa.ec.leos.i18n.MessageHelper;
import eu.europa.ec.leos.model.rendition.RenderedDocument;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.repository.store.WorkspaceRepository;
import eu.europa.ec.leos.security.LeosPermissionAuthorityMapHelper;
import eu.europa.ec.leos.services.Annotate.AnnotateService;
import eu.europa.ec.leos.services.compare.ContentComparatorContext;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.content.processor.AttachmentProcessor;
import eu.europa.ec.leos.services.converter.ProposalConverterService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.document.MemorandumService;
import eu.europa.ec.leos.services.export.ExportOptions;
import eu.europa.ec.leos.services.export.ExportResource;
import eu.europa.ec.leos.services.export.LegPackage;
import eu.europa.ec.leos.services.export.ZipPackageUtil;
import eu.europa.ec.leos.services.rendition.HtmlRenditionProcessor;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfig;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.services.toc.StructureContext;
import eu.europa.ec.leos.vo.toc.TableOfContentItemHtmlVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import io.atlassian.fugue.Option;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.inject.Provider;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.ATTR_NAME;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_ADDED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_REMOVED_CLASS;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_INTERMEDIATE_STYLE;
import static eu.europa.ec.leos.services.compare.ContentComparatorService.DOUBLE_COMPARE_ORIGINAL_STYLE;
import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class LegServiceImpl implements LegService {
    private static final Logger LOG = LoggerFactory.getLogger(LegServiceImpl.class);
    
    private final PackageRepository packageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AttachmentProcessor attachmentProcessor;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;
    private final AnnotateService annotateService;
    private final HtmlRenditionProcessor htmlRenditionProcessor;
    private final ProposalConverterService proposalConverterService;
    private final LeosPermissionAuthorityMapHelper authorityMapHelper;
    private final ContentComparatorService compareService;
    private final MessageHelper messageHelper;
    private final Provider<StructureContext> structureContextProvider;
    private final BillService billService;
    private final AnnexService annexService;
    private final MemorandumService memorandumService;
    
    private static final String MEDIA_DIR = "media/";
    private static final String ANNOT_FILE_EXT = ".json";
    private static final String ANNOT_FILE_PREFIX = "annot_";
    private static final String LEG_FILE_PREFIX = "leg_";
    private static final String LEG_FILE_EXTENSION = ".leg";
    private static final String STYLE_SHEET_EXT = ".css";
    private static final String JS_EXT = ".js";
    private static final String STYLE_DEST_DIR = "renditions/html/css/";
    private static final String JS_DEST_DIR = "renditions/html/js/";
    private static final String STYLES_SOURCE_PATH = "META-INF/resources/assets/css/";
    private static final String JS_SOURCE_PATH = "META-INF/resources/js/";
    private static final String JQUERY_SOURCE_PATH = "META-INF/resources/lib/jquery_3.2.1/";
    private static final String JQTREE_SOURCE_PATH = "META-INF/resources/lib/jqTree_1.4.9/";
    private static final String HTML_RENDITION = "renditions/html/";
    private static final String PDF_RENDITION = "renditions/pdf/";
    private static final String WORD_RENDITION = "renditions/word/";
    
    @Autowired
    LegServiceImpl(PackageRepository packageRepository,
                   WorkspaceRepository workspaceRepository,
                   AttachmentProcessor attachmentProcessor,
                   XmlNodeProcessor xmlNodeProcessor,
                   XmlNodeConfigHelper xmlNodeConfigHelper,
                   AnnotateService annotateService,
                   HtmlRenditionProcessor htmlRenditionProcessor,
                   ProposalConverterService proposalConverterService,
                   LeosPermissionAuthorityMapHelper authorityMapHelper,
                   ContentComparatorService compareService,
                   MessageHelper messageHelper,
                   Provider<StructureContext> structureContextProvider,
                   BillService billService,
                   MemorandumService memorandumService,
                   AnnexService annexService) {
        this.packageRepository = packageRepository;
        this.workspaceRepository = workspaceRepository;
        this.attachmentProcessor = attachmentProcessor;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.annotateService = annotateService;
        this.htmlRenditionProcessor = htmlRenditionProcessor;
        this.proposalConverterService = proposalConverterService;
        this.authorityMapHelper = authorityMapHelper;
        this.messageHelper = messageHelper;
        this.compareService = compareService;
        this.structureContextProvider = structureContextProvider;
        this.billService = billService;
        this.memorandumService = memorandumService;
        this.annexService = annexService;
    }
    
    @Override
    public LegDocument findLastLegByVersionedReference(String path, String versionedReference) {
        return packageRepository.findLastLegByVersionedReference(path, versionedReference);
    }
    
    @Override
    public List<LegDocumentVO> getLegDocumentDetailsByUserId(String userId) {
        List<Proposal> proposals = packageRepository.findDocumentsByUserId(userId, Proposal.class, authorityMapHelper.getRoleForDocCreation());
        List<LegDocumentVO> legDocumentVOs = new ArrayList<>();
        for (Proposal proposal : proposals) {
            LeosPackage leosPackage = packageRepository.findPackageByDocumentId(proposal.getId());
            List<LegDocument> legDocuments = packageRepository.findDocumentsByPackagePath(leosPackage.getPath(), LegDocument.class, false);
            if (!legDocuments.isEmpty()) {
                LegDocument leg = legDocuments.get(0);
                LegDocumentVO legDocumentVO = new LegDocumentVO();
                legDocumentVO.setProposalId(proposal.getMetadata().getOrError(() -> "Proposal metadata is not available!").getRef());
                legDocumentVO.setDocumentTitle(proposal.getTitle());
                legDocumentVO.setLegFileId(leg.getId());
                legDocumentVO.setLegFileName(leg.getName());
                legDocumentVO.setLegFileStatus(leg.getStatus().name());
                legDocumentVO.setMilestoneComments(leg.getMilestoneComments());
                legDocumentVOs.add(legDocumentVO);
            }
        }
        return legDocumentVOs;
    }
    
    private String generateLegName() {
        return LEG_FILE_PREFIX + Cuid.createCuid() + LEG_FILE_EXTENSION;
    }
    
    private byte[] addMetadataToProposal(byte[] xmlContent, Proposal proposal) {
        Option<ProposalMetadata> metadataOption = proposal.getMetadata();
        ProposalMetadata metadata = metadataOption.get().withObjectId(proposal.getId()).withDocVersion(proposal.getVersionLabel());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }
    
    private byte[] addMetadataToMemorandum(byte[] xmlContent, Memorandum memorandum) {
        Option<MemorandumMetadata> metadataOption = memorandum.getMetadata();
        MemorandumMetadata metadata = metadataOption.get().withObjectId(memorandum.getId()).withDocVersion(memorandum.getVersionLabel());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }
    
    private byte[] addMetadataToBill(byte[] xmlContent, Bill bill) {
        Option<BillMetadata> metadataOption = bill.getMetadata();
        BillMetadata metadata = metadataOption.get().withObjectId(bill.getId()).withDocVersion(bill.getVersionLabel());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }
    
    private byte[] addMetadataToAnnex(byte[] xmlContent, Annex annex) {
        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        AnnexMetadata metadata = metadataOption.get().withObjectId(annex.getId()).withDocVersion(annex.getVersionLabel());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }
    
    /**
     * Creates the LegPackage for the given leg file.
     *
     * @param legFile legFile for which we need to create the LegPackage
     * @return LegPackage used to be sent to Toolbox for PDF/LegisWrite generation.
     */
    @Override
    public LegPackage createLegPackage(File legFile, ExportOptions exportOptions) throws IOException {
        // legFile will be deleted after createProposalFromLegFile(), so we save the bytes in a temporary file
        File legFileTemp = File.createTempFile("RENDITION_", ".leg");
        FileUtils.copyFile(legFile, legFileTemp);
        
        final DocumentVO proposalVO = proposalConverterService.createProposalFromLegFile(legFile, new DocumentVO(LeosCategory.PROPOSAL), false);
        final byte[] proposalXmlContent = proposalVO.getSource();
        ExportResource proposalExportResource = new ExportResource(LeosCategory.PROPOSAL);
        final Map<String, String> proposalRefsMap = buildProposalExportResource(proposalExportResource, proposalXmlContent);
        proposalExportResource.setExportOptions(exportOptions);
        final DocumentVO memorandumVO = proposalVO.getChildDocument(LeosCategory.MEMORANDUM);
        final byte[] memorandumXmlContent = memorandumVO.getSource();
        final ExportResource memorandumExportResource = buildExportResourceMemorandum(proposalRefsMap, memorandumXmlContent);
        proposalExportResource.addChildResource(memorandumExportResource);
        
        final DocumentVO billVO = proposalVO.getChildDocument(LeosCategory.BILL);
        final byte[] billXmlContent = billVO.getSource();
        final ExportResource billExportResource = buildExportResourceBill(proposalRefsMap, billXmlContent);
        
        // add annexes to billExportResource
        final Map<String, String> attachmentIds = attachmentProcessor.getAttachmentsIdFromBill(billXmlContent);
        final List<DocumentVO> annexesVO = billVO.getChildDocuments(LeosCategory.ANNEX);
        annexesVO.forEach((annexVO) -> {
            final byte[] annexXmlContent = annexVO.getSource();
            final int docNumber = Integer.parseInt(annexVO.getMetadata().getIndex());
            final String resourceId = attachmentIds.entrySet()
                    .stream()
                    .filter(e -> e.getKey().equals(annexVO.getId()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .get();
            final ExportResource annexExportResource = buildExportResourceAnnex(docNumber, resourceId, annexXmlContent);
            billExportResource.addChildResource(annexExportResource);
        });
        proposalExportResource.addChildResource(billExportResource);
        LegPackage legPackage = new LegPackage();
        legPackage.setFile(legFileTemp);
        legPackage.setExportResource(proposalExportResource);
        return legPackage;
    }
    
    @Override
    public LegPackage createLegPackage(String proposalId, ExportOptions exportOptions) throws IOException {
        LOG.trace("Creating Leg Package using [documentId={}]", proposalId);
        return createLegPackage(proposalId, exportOptions, null);
    }
    
    /**
     * Creates the LegPackage, which is the logical representation of the leg file, for the given proposalId.
     *
     * @param proposalId       proposalId for which we need to create the LegPackage
     * @param exportOptions    exportOptions to select what needs to be exported
     * @param versionToCompare Required in case ExportOptions.ComparisonType.DOUBLE is selected for export of double comparison result
     * @return LegPackage used to be sent to Toolbox for PDF/LegisWrite generation.
     */
    @Override
    public LegPackage createLegPackage(String proposalId, ExportOptions exportOptions, XmlDocument versionToCompare) throws IOException {
        LOG.trace("Creating Leg Package... [documentId={}]", proposalId);
        
        final LegPackage legPackage = new LegPackage();
        final LeosPackage leosPackage = packageRepository.findPackageByDocumentId(proposalId);
        final Map<String, Object> contentToZip = new HashMap<>();
        final ExportResource exportProposalResource = new ExportResource(LeosCategory.PROPOSAL);
        exportProposalResource.setExportOptions(exportOptions);
        
        //1. Add Proposal to package
        final Proposal proposal = workspaceRepository.findDocumentById(proposalId, Proposal.class, true);
        final Map<String, String> proposalRefsMap = enrichZipWithProposal(contentToZip, exportProposalResource, proposal);
        
        //2. Depending on ExportOptions FileType add documents to package
        Bill bill;
        ExportResource exportBillResource;
        switch (exportOptions.getFileType()) {
            case "LEGALTEXT": // TODO: Get from LeosCategory once Kotlin code is removed
                bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(),
                        proposalRefsMap.get(LeosCategory.BILL.name() + "_href"), Bill.class);
                addBillToPackage(contentToZip, exportProposalResource, proposalRefsMap, bill, versionToCompare);
                legPackage.addContainedFile(bill.getVersionedReference());
                break;
            case "MEMORANDUM":
                if (proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_href") != null) {
                    addMemorandumToPackage(leosPackage, contentToZip, exportProposalResource, proposalRefsMap, legPackage);
                }
                break;
            case "ANNEX":
                bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(),
                        proposalRefsMap.get(LeosCategory.BILL.name() + "_href"), Bill.class);
                
                Content content = bill.getContent().getOrNull();
                exportBillResource = buildExportResourceBill(proposalRefsMap, content.getSource().getBytes());
                exportBillResource.setExportOptions(exportOptions);
                exportProposalResource.addChildResource(exportBillResource);
                legPackage.addContainedFile(bill.getVersionedReference());
                addAnnexToPackage(leosPackage, bill, contentToZip, ((Annex) versionToCompare), exportBillResource, legPackage);
                break;
            default:
                bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(),
                        proposalRefsMap.get(LeosCategory.BILL.name() + "_href"), Bill.class);
                exportBillResource = addBillToPackage(contentToZip, exportProposalResource, proposalRefsMap, bill, versionToCompare);
                legPackage.addContainedFile(bill.getVersionedReference());
                if (proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_href") != null) {
                    addMemorandumToPackage(leosPackage, contentToZip, exportProposalResource, proposalRefsMap, legPackage);
                }
                addAnnexToPackage(leosPackage, bill, contentToZip, null, exportBillResource, legPackage);
        }
        
        //3. Add TOC
        enrichZipWithToc(contentToZip);
        
        //4. Add media
        final List<MediaDocument> mediaDocs = packageRepository.findDocumentsByPackagePath(leosPackage.getPath(), MediaDocument.class, true);
        enrichZipWithMedia(contentToZip, mediaDocs);
        legPackage.setFile(ZipPackageUtil.zipFiles(proposalRefsMap.get(XmlNodeConfigHelper.PROPOSAL_DOC_COLLECTION) + ".leg", contentToZip));
        legPackage.setExportResource(exportProposalResource);
        return legPackage;
    }
    
    private ExportResource addBillToPackage(final Map<String, Object> contentToZip, ExportResource exportProposalResource,
                                            final Map<String, String> proposalRefsMap, final Bill bill, XmlDocument versionToCompare) {
        Content content = bill.getContent().getOrError(() -> "Bill content is required!");
        byte[] xmlContent = content.getSource().getBytes();
        return enrichZipWithBill(contentToZip, exportProposalResource, proposalRefsMap, bill, xmlContent, versionToCompare);
    }
    
    private void addMemorandumToPackage(final LeosPackage leosPackage, final Map<String, Object> contentToZip, ExportResource exportProposalResource,
                                        final Map<String, String> proposalRefsMap, LegPackage legPackage) {
        final Memorandum memorandum = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_href"), Memorandum.class);
        enrichZipWithMemorandum(contentToZip, exportProposalResource, proposalRefsMap, memorandum);
        legPackage.addContainedFile(memorandum.getVersionedReference());
    }
    
    /**
     * Used to add a Single annex to package, or all annexes if @param annexId is null
     */
    private void addAnnexToPackage(final LeosPackage leosPackage, Bill bill, final Map<String, Object> contentToZip,
                                   Annex versionToCompare, ExportResource exportProposalResource, LegPackage legPackage) {
        Content content = bill.getContent().getOrError(() -> "Bill content is required!");
        String annexId = versionToCompare != null ? versionToCompare.getMetadata().get().getRef() : null;
        byte[] xmlContent = content.getSource().getBytes();
        final Map<String, String> attachmentIds = attachmentProcessor.getAttachmentsIdFromBill(xmlContent);
        final String annexStyleSheet = LeosCategory.ANNEX.name().toLowerCase() + STYLE_SHEET_EXT;
        attachmentIds.forEach((href, id) -> {
            if (annexId == null || href.equals(annexId)) {
                final Annex annex = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), href, Annex.class);
                enrichZipWithAnnex(contentToZip, exportProposalResource, annexStyleSheet, annex, versionToCompare, id, href);
                legPackage.addContainedFile(annex.getVersionedReference());
            }
        });
        if (!attachmentIds.isEmpty()) {
            //Add annex style only if at least one is present
            addResourceToZipContent(contentToZip, annexStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        }
    }
    
    private Map<String, String> enrichZipWithProposal(final Map<String, Object> contentToZip, ExportResource exportProposalResource, Proposal proposal) {
        Content content = proposal.getContent().getOrError(() -> "Proposal content is required!");
        byte[] xmlContent = content.getSource().getBytes();
        xmlContent = addMetadataToProposal(xmlContent, proposal);
        contentToZip.put("main.xml", xmlContent);
        
        return buildProposalExportResource(exportProposalResource, xmlContent);
    }
    
    private void enrichZipWithToc(final Map<String, Object> contentToZip) {
        addResourceToZipContent(contentToZip, "jquery" + JS_EXT, JQUERY_SOURCE_PATH, JS_DEST_DIR);
        addResourceToZipContent(contentToZip, "jqtree" + JS_EXT, JQTREE_SOURCE_PATH, JS_DEST_DIR);
        addResourceToZipContent(contentToZip, "jqtree" + STYLE_SHEET_EXT, JQTREE_SOURCE_PATH + "css/", STYLE_DEST_DIR);
        addResourceToZipContent(contentToZip, "leos-toc-rendition" + JS_EXT, JS_SOURCE_PATH + "rendition/", JS_DEST_DIR);
        addResourceToZipContent(contentToZip, "leos-toc-rendition" + STYLE_SHEET_EXT, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
    }
    
    private void enrichZipWithMemorandum(final Map<String, Object> contentToZip, ExportResource exportProposalResource, Map<String, String> proposalRefsMap, Memorandum memorandum) {
        Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        ExportOptions exportOptions = exportProposalResource.getExportOptions();
        
        byte[] xmlContent = content.getSource().getBytes();
        xmlContent = addMetadataToMemorandum(xmlContent, memorandum);
        contentToZip.put(memorandum.getName(), xmlContent);
        if (exportOptions.isConvertAnnotations()) {
            String annotations = annotateService.getAnnotations(memorandum.getMetadata().getOrError(() -> "Memorandum metadata is not available!").getRef());
            addAnnotateToZipContent(contentToZip, memorandum.getName(), annotations);
        }
        
        String memoStyleSheet = LeosCategory.MEMORANDUM.name().toLowerCase() + STYLE_SHEET_EXT;
        addResourceToZipContent(contentToZip, memoStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        structureContextProvider.get().useDocumentTemplate(memorandum.getMetadata().get().getDocTemplate());
        final String memoTocJson = getTocAsJson(memorandumService.getTableOfContent(memorandum, TocMode.SIMPLIFIED_CLEAN));
        addHtmlRendition(contentToZip, memorandum, memoStyleSheet, memoTocJson);
        
        final ExportResource memorandumExportResource = buildExportResourceMemorandum(proposalRefsMap, xmlContent);
        exportProposalResource.addChildResource(memorandumExportResource);
    }
    
    private ExportResource enrichZipWithBill(final Map<String, Object> contentToZip, ExportResource exportProposalResource, Map<String, String> proposalRefsMap,
                                             Bill bill, byte[] xmlContent, XmlDocument versionToCompare) {
        ExportOptions exportOptions = exportProposalResource.getExportOptions();
        String resultContent = "";
        
        switch (exportOptions.getComparisonType()) {
            case DOUBLE:
                List<Bill> billVersions = billService.findVersions(bill.getId());
                Bill originalVersion = billService.findBillVersion(billVersions.get(billVersions.size() - 1).getId());
                versionToCompare = versionToCompare != null ? billService.findBillVersion(versionToCompare.getId()) : originalVersion;
                resultContent = doubleCompareXmlContents(originalVersion, versionToCompare, xmlContent);
                xmlContent = resultContent != null ? resultContent.getBytes(UTF_8) : xmlContent;
                break;
            case SIMPLE:
                resultContent = simpleCompareXmlContents(versionToCompare, xmlContent);
                xmlContent = resultContent != null ? resultContent.getBytes(UTF_8) : xmlContent;
                break;
            default:
                xmlContent = addMetadataToBill(xmlContent, bill);
        }
        contentToZip.put(bill.getName(), xmlContent);
        if (exportOptions.isConvertAnnotations()) {
            String billAnnotations = annotateService.getAnnotations(bill.getMetadata().getOrError(() -> "Legal text metadata is not available!").getRef());
            addAnnotateToZipContent(contentToZip, bill.getName(), billAnnotations);
        }
        
        String billStyleSheet = LeosCategory.BILL.name().toLowerCase() + STYLE_SHEET_EXT;
        addResourceToZipContent(contentToZip, billStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        structureContextProvider.get().useDocumentTemplate(bill.getMetadata().get().getDocTemplate());
        final String billTocJson = getTocAsJson(billService.getTableOfContent(bill, TocMode.SIMPLIFIED_CLEAN));
        addHtmlRendition(contentToZip, bill, billStyleSheet, billTocJson);
        
        final ExportResource exportBillResource = buildExportResourceBill(proposalRefsMap, xmlContent);
        exportBillResource.setExportOptions(exportOptions);
        exportProposalResource.addChildResource(exportBillResource);
        return exportBillResource;
    }
    
    private String doubleCompareXmlContents(XmlDocument originalVersion, XmlDocument intermediateMajor, byte[] currentXmlContent) {
        String originalXml = originalVersion.getContent().getOrError(() -> "Original document content is required!")
                .getSource().toString();
        String intermediateMajorXml = intermediateMajor.getContent().getOrError(() -> "Intermadiate Major Version document content is required!")
                .getSource().toString();
        String currentXml = new String(currentXmlContent, UTF_8);
        
        return compareService.compareContents(new ContentComparatorContext.Builder(originalXml, currentXml, intermediateMajorXml)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(DOUBLE_COMPARE_REMOVED_CLASS)
                .withAddedValue(DOUBLE_COMPARE_ADDED_CLASS)
                .withRemovedIntermediateValue(DOUBLE_COMPARE_REMOVED_CLASS + DOUBLE_COMPARE_INTERMEDIATE_STYLE)
                .withAddedIntermediateValue(DOUBLE_COMPARE_ADDED_CLASS + DOUBLE_COMPARE_INTERMEDIATE_STYLE)
                .withRemovedOriginalValue(DOUBLE_COMPARE_REMOVED_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                .withAddedOriginalValue(DOUBLE_COMPARE_ADDED_CLASS + DOUBLE_COMPARE_ORIGINAL_STYLE)
                .withDisplayRemovedContentAsReadOnly(Boolean.TRUE)
                .withThreeWayDiff(true)
                .build());
    }
    
    private String simpleCompareXmlContents(XmlDocument versionToCompare, byte[] currentXmlContent) {
        String versionToCompareXml = versionToCompare.getContent().getOrError(() -> "Document to compare is required!")
                .getSource().toString();
        String currentXml = new String(currentXmlContent, UTF_8);
        
        return compareService.compareContents(new ContentComparatorContext.Builder(versionToCompareXml, currentXml)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(DOUBLE_COMPARE_REMOVED_CLASS)
                .withAddedValue(DOUBLE_COMPARE_ADDED_CLASS)
                .withDisplayRemovedContentAsReadOnly(Boolean.TRUE)
                .withThreeWayDiff(false)
                .build());
    }
    
    private void enrichZipWithAnnex(final Map<String, Object> contentToZip, ExportResource exportBillResource, String annexStyleSheet, Annex annex, Annex versionToCompare, String resourceId, String href) {
        ExportOptions exportOptions = exportBillResource.getExportOptions();
        String resultContent;
        final Content annexContent = annex.getContent().getOrError(() -> "Annex content is required!");
        byte[] xmlAnnexContent = annexContent.getSource().getBytes();
        switch (exportOptions.getComparisonType()) {
            case DOUBLE:
                List<Annex> annexVersions = annexService.findVersions(annex.getId());
                Annex originalVersion = annexService.findAnnexVersion(annexVersions.get(annexVersions.size() - 1).getId());
                versionToCompare = versionToCompare != null ? annexService.findAnnexVersion(versionToCompare.getId()) : originalVersion;
                resultContent = doubleCompareXmlContents(originalVersion, versionToCompare, xmlAnnexContent);
                xmlAnnexContent = resultContent != null ? resultContent.getBytes(UTF_8) : xmlAnnexContent;
                break;
            case SIMPLE:
                resultContent = simpleCompareXmlContents(versionToCompare, xmlAnnexContent);
                xmlAnnexContent = resultContent != null ? resultContent.getBytes(UTF_8) : xmlAnnexContent;
                break;
            default:
                xmlAnnexContent = addMetadataToAnnex(xmlAnnexContent, annex);
        }
        contentToZip.put(annex.getName(), xmlAnnexContent);
        if (exportOptions.isConvertAnnotations()) {
            String annexAnnotations = annotateService.getAnnotations(annex.getMetadata().getOrError(() -> "Annex metadata is not available!").getRef());
            addAnnotateToZipContent(contentToZip, annex.getName(), annexAnnotations);
        }
        
        addResourceToZipContent(contentToZip, annexStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        structureContextProvider.get().useDocumentTemplate(annex.getMetadata().get().getDocTemplate());
        final String annexTocJson = getTocAsJson(annexService.getTableOfContent(annex, TocMode.SIMPLIFIED_CLEAN));
        addHtmlRendition(contentToZip, annex, annexStyleSheet, annexTocJson);
        
        int docNumber = annex.getMetadata().get().getIndex();
        final ExportResource annexExportResource = buildExportResourceAnnex(docNumber, resourceId, href, xmlAnnexContent);
        exportBillResource.addChildResource(annexExportResource);
    }
    
    private List<TableOfContentItemHtmlVO> buildTocHtml(List<TableOfContentItemVO> tableOfContents) {
        List<TableOfContentItemHtmlVO> tocHtml = new ArrayList<>();
        for (TableOfContentItemVO item : tableOfContents) {
            String name = TableOfContentHelper.buildItemCaption(item, TableOfContentHelper.DEFAULT_CAPTION_MAX_SIZE, messageHelper);
            TableOfContentItemHtmlVO itemHtml = new TableOfContentItemHtmlVO(name, "#" + item.getId());
            if (item.getChildItems().size() > 0) {
                itemHtml.setChildren(buildTocHtml(item.getChildItems()));
            }
            tocHtml.add(itemHtml);
        }
        return tocHtml;
    }
    
    private String getTocAsJson(List<TableOfContentItemVO> tableOfContent) {
        final String json;
        try {
            List<TableOfContentItemHtmlVO> tocHtml = buildTocHtml(tableOfContent);
            json = new ObjectMapper().writeValueAsString(tocHtml);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Exception while converting 'tableOfContent' in json format.", e);
        }
        return json;
    }
    
    private Map<String, String> buildProposalExportResource(ExportResource exportResource, byte[] xmlContent) {
        Map<String, XmlNodeConfig> config = new HashMap<>();
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.MEMORANDUM, "xml:id"));
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.MEMORANDUM, "href"));
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.BILL, "xml:id"));
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.BILL, "href"));
        config.putAll(xmlNodeConfigHelper.getConfig(LeosCategory.PROPOSAL));
        Map<String, String> proposalRefsMap = xmlNodeProcessor.getValuesFromXml(xmlContent,
                new String[]{XmlNodeConfigHelper.PROPOSAL_DOC_COLLECTION, XmlNodeConfigHelper.DOC_REF_COVER,
                        LeosCategory.MEMORANDUM.name() + "_xml:id",
                        LeosCategory.MEMORANDUM.name() + "_href",
                        LeosCategory.BILL.name() + "_xml:id",
                        LeosCategory.BILL.name() + "_href"
                },
                config);
        
        Map<String, String> proposalComponentRefs = new HashMap<>();
        proposalComponentRefs.put(XmlNodeConfigHelper.DOC_REF_COVER, proposalRefsMap.get(XmlNodeConfigHelper.DOC_REF_COVER));
        
        exportResource.setResourceId(proposalRefsMap.get(XmlNodeConfigHelper.PROPOSAL_DOC_COLLECTION));
        exportResource.setComponentsIdsMap(proposalComponentRefs);
        return proposalRefsMap;
    }
    
    private ExportResource buildExportResourceMemorandum(Map<String, String> proposalRefsMap, byte[] xmlContent) {
        ExportResource memorandumExportResource = new ExportResource(LeosCategory.MEMORANDUM);
        memorandumExportResource.setResourceId(proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_xml:id"));
        setComponentsRefs(LeosCategory.MEMORANDUM, memorandumExportResource, xmlContent);
        return memorandumExportResource;
    }
    
    private ExportResource buildExportResourceBill(Map<String, String> proposalRefsMap, byte[] xmlContent) {
        ExportResource billExportResource = new ExportResource(LeosCategory.BILL);
        billExportResource.setResourceId(proposalRefsMap.get(LeosCategory.BILL.name() + "_xml:id"));
        setComponentsRefs(LeosCategory.BILL, billExportResource, xmlContent);
        return billExportResource;
    }
    
    private ExportResource buildExportResourceAnnex(int docNumber, String resourceId, byte[] xmlContent) {
        //TODO : FIXME : populate href for Proposal export
        return buildExportResourceAnnex(docNumber, resourceId, null, xmlContent);
    }
    
    private ExportResource buildExportResourceAnnex(int docNumber, String resourceId, String href, byte[] xmlContent) {
        ExportResource annexExportResource = new ExportResource(LeosCategory.ANNEX);
        annexExportResource.setResourceId(resourceId);
        annexExportResource.setHref(href);
        annexExportResource.setDocNumber(docNumber);
        setComponentsRefs(LeosCategory.ANNEX, annexExportResource, xmlContent);
        return annexExportResource;
    }
    
    private void enrichZipWithMedia(final Map<String, Object> contentToZip, List<MediaDocument> mediaDocs) {
        Content content;
        for (MediaDocument mediaDoc : mediaDocs) {
            content = mediaDoc.getContent().getOrError(() -> "Document content is required!");
            byte[] byteContent = content.getSource().getBytes();
            contentToZip.put(MEDIA_DIR + mediaDoc.getName(), byteContent);
        }
    }
    
    @Override
    public LegDocument createLegDocument(String proposalId, String jobId, LegPackage legPackage, LeosLegStatus status) throws IOException {
        LOG.trace("Creating Leg Document for Package... [documentId={}]", proposalId);
        return packageRepository.createLegDocumentFromContent(packageRepository.findPackageByDocumentId(proposalId).getPath(), generateLegName(),
                jobId, legPackage.getMilestoneComments(), getFileContent(legPackage.getFile()), status, legPackage.getContainedFiles());
    }
    
    @Override
    public LegDocument updateLegDocument(String id, LeosLegStatus status) {
        LOG.trace("Updating Leg document status... [id={}, status={}]", id, status.name());
        return packageRepository.updateLegDocument(id, status);
    }
    
    @Override
    public LegDocument updateLegDocument(String id, byte[] pdfJobZip, byte[] wordJobZip) {
        LOG.trace("Updating Leg document with id={} status to {} and content with pdf and word renditions", id, LeosLegStatus.FILE_READY.name());
        LegDocument document = findLegDocumentById(id);
        try {
            byte[] content = updateContentWithPdfAndWordRenditions(pdfJobZip, wordJobZip, document.getContent().getOrNull());
            return packageRepository.updateLegDocument(document.getId(), LeosLegStatus.FILE_READY, content, VersionType.INTERMEDIATE, "Milestone is now validated");
        } catch (Exception e) {
            LOG.error("Error while updating the content of the Leg Document with id=" + id, e);
            return packageRepository.updateLegDocument(document.getId(), LeosLegStatus.FILE_ERROR);
        }
    }
    
    @Override
    public LegDocument findLegDocumentById(String id) {
        LOG.trace("Finding Leg Document by id... [documentId={}]", id);
        return packageRepository.findLegDocumentById(id, true);
    }
    
    @Override
    public LegDocument findLegDocumentByAnyDocumentIdAndJobId(String documentId, String jobId) {
        LOG.trace("Finding Leg Document by proposal id and job id... [proposalId={}, jobId={}]", documentId, jobId);
        LeosPackage leosPackage = packageRepository.findPackageByDocumentId(documentId);
        List<LegDocument> legDocuments = packageRepository.findDocumentsByPackageId(leosPackage.getId(), LegDocument.class, false, false);
        return legDocuments.stream()
                .filter(legDocument -> jobId.equals(legDocument.getJobId()))
                .findAny()
                .orElse(null);
    }
    
    @Override
    public List<LegDocument> findLegDocumentByStatus(LeosLegStatus leosLegStatus) {
        return packageRepository.findDocumentsByStatus(leosLegStatus, LegDocument.class);
    }
    
    @Override
    public List<LegDocument> findLegDocumentByProposal(String proposalId) {
        LeosPackage leosPackage = packageRepository.findPackageByDocumentId(proposalId);
        return packageRepository.findDocumentsByPackageId(leosPackage.getId(), LegDocument.class, false, false);
    }
    
    private byte[] updateContentWithPdfAndWordRenditions(byte[] pdfJobZip, byte[] wordJobZip, Content content) throws IOException {
        Map<String, Object> legContent = ZipPackageUtil.unzipByteArray(content.getSource().getBytes());
        addPdfRendition(pdfJobZip, legContent);
        addWordRenditions(wordJobZip, legContent);
        return ZipPackageUtil.zipByteArray(legContent);
    }
    
    private void addPdfRendition(byte[] pdfJobZip, Map<String, Object> legContent) throws IOException {
        Map.Entry<String, Object> neededEntry = unzipJobResult(pdfJobZip).entrySet().stream()
                .filter(pdfEntry -> !pdfEntry.getKey().endsWith("_pdfa.pdf"))
                .findAny()
                .orElseThrow(() -> new FileNotFoundException("Pdf rendition not found in the pdf document job file"));
        legContent.put(PDF_RENDITION + neededEntry.getKey(), neededEntry.getValue());
    }
    
    private void addWordRenditions(byte[] wordJobZip, Map<String, Object> legContent) throws IOException {
        List<String> wordEntries = new ArrayList<>();
        unzipJobResult(wordJobZip).entrySet().stream()
                .filter(wordEntity -> !wordEntity.getKey().isEmpty())
                .forEach(wordEntry -> {
                    legContent.put(WORD_RENDITION + wordEntry.getKey(), wordEntry.getValue());
                    wordEntries.add(wordEntry.getKey());
                });
        if (wordEntries.isEmpty()) {
            throw new FileNotFoundException("No word rendition found in the word document job file");
        }
    }
    
    private Map<String, Object> unzipJobResult(byte[] jobZip) throws IOException {
        Map<String, Object> jobContent = ZipPackageUtil.unzipByteArray(jobZip);
        for (Map.Entry<String, Object> entry : jobContent.entrySet()) {
            if (entry.getKey().endsWith("_out.zip")) {
                return ZipPackageUtil.unzipByteArray((byte[]) entry.getValue());
            }
        }
        throw new FileNotFoundException("The job result zip file is not present in the job file");
    }
    
    private byte[] getFileContent(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            byte[] content = new byte[(int) file.length()];
            is.read(content);
            return content;
        }
    }
    
    private void addHtmlRendition(Map<String, Object> contentToZip, XmlDocument xmlDocument, String styleSheetName, String tocJson) {
        try {
            RenderedDocument htmlDocument = new RenderedDocument();
            htmlDocument.setContent(new ByteArrayInputStream(getContent(xmlDocument)));
            htmlDocument.setStyleSheetName(styleSheetName);
            String htmlName = HTML_RENDITION + xmlDocument.getName().replaceAll(".xml", ".html");
            contentToZip.put(htmlName, htmlRenditionProcessor.processTemplate(htmlDocument).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UnsupportedEncodingException while processing document " + xmlDocument.getName(), exception);
        }
        
        try {
            // Build toc_docName.js file
            RenderedDocument tocHtmlDocument = new RenderedDocument();
            tocHtmlDocument.setContent(new ByteArrayInputStream(getContent(xmlDocument)));
            tocHtmlDocument.setStyleSheetName(styleSheetName);
            final String tocJsName = xmlDocument.getName().substring(0, xmlDocument.getName().indexOf(".xml")) + "_toc" + ".js";
            final String tocJsFile = JS_DEST_DIR + tocJsName;
            contentToZip.put(tocJsFile, htmlRenditionProcessor.processJsTemplate(tocJson).getBytes("UTF-8"));
            
            //build html_docName_toc.html
            tocHtmlDocument = new RenderedDocument();
            tocHtmlDocument.setContent(new ByteArrayInputStream(getContent(xmlDocument)));
            tocHtmlDocument.setStyleSheetName(styleSheetName);
            String tocHtmlFile = HTML_RENDITION + xmlDocument.getName();
            tocHtmlFile = tocHtmlFile.substring(0, tocHtmlFile.indexOf(".xml")) + "_toc" + ".html";
            contentToZip.put(tocHtmlFile, htmlRenditionProcessor.processTocTemplate(tocHtmlDocument, tocJsName).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UnsupportedEncodingException while processing document " + xmlDocument.getName(), exception);
        }
    }
    
    /**
     * Add resource to exported .leg file in renditions/html/css or js folder
     */
    private void addResourceToZipContent(Map<String, Object> contentToZip, String resourceName, String sourcePath, String destPath) {
        try {
            Resource resource = new ClassPathResource(sourcePath + resourceName);
            contentToZip.put(destPath + resourceName, IOUtils.toByteArray(resource.getInputStream()));
        } catch (IOException io) {
            LOG.error("Error occurred while getting styles ", io);
        }
    }
    
    /**
     * Calls service to get Annotations per document
     */
    private void addAnnotateToZipContent(Map<String, Object> contentToZip, String docName, String annotations) {
        final byte[] xmlAnnotationContent = annotations.getBytes(UTF_8);
        contentToZip.put(creatAnnotationFileName(docName), xmlAnnotationContent);
    }
    
    private String creatAnnotationFileName(String docName) {
        return MEDIA_DIR + ANNOT_FILE_PREFIX + docName + ANNOT_FILE_EXT;
    }
    
    private void setComponentsRefs(LeosCategory leosCategory, final ExportResource exportResource, byte[] xmlContent) {
        Map<String, String> componentMap = xmlNodeProcessor.getValuesFromXml(xmlContent,
                new String[]{XmlNodeConfigHelper.DOC_REF_COVER},
                xmlNodeConfigHelper.getConfig(leosCategory));
        exportResource.setComponentsIdsMap(componentMap);
    }
    
    private byte[] getContent(XmlDocument xmlDocument) {
        final Content content = xmlDocument.getContent().getOrError(() -> "xml content is required!");
        return content.getSource().getBytes();
    }
    
}
