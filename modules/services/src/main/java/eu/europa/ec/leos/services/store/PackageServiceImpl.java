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
package eu.europa.ec.leos.services.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.*;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
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
import eu.europa.ec.leos.services.export.ZipPackageUtil;
import eu.europa.ec.leos.services.rendition.FreemarkerRenditionProcessor;
import eu.europa.ec.leos.services.support.TableOfContentHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfig;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.vo.toc.TableOfContentItemHtmlVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import io.atlassian.fugue.Option;
import io.atlassian.fugue.Pair;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.*;
import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
class PackageServiceImpl implements PackageService {
    private static final Logger LOG = LoggerFactory.getLogger(PackageServiceImpl.class);

    private static final String PACKAGE_NAME_PREFIX = "package_";

    private final PackageRepository packageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AttachmentProcessor attachmentProcessor;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;
    private final AnnotateService  annotateService;
    private final FreemarkerRenditionProcessor freemarkerRenditionProcessor;
    private final ProposalConverterService proposalConverterService;
    private final LeosPermissionAuthorityMapHelper authorityMapHelper;
    private final BillService billService;
    private final MemorandumService memorandumService;
    private final AnnexService annexService;
    private final ContentComparatorService compareService;
    
    private final MessageHelper messageHelper;

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

    @Value("${leos.workspaces.path}")
    protected String storagePath;

    PackageServiceImpl(PackageRepository packageRepository,
            WorkspaceRepository workspaceRepository,
            AttachmentProcessor attachmentProcessor,
            XmlNodeProcessor xmlNodeProcessor,
            XmlNodeConfigHelper xmlNodeConfigHelper,
            AnnotateService  annotateService,
            FreemarkerRenditionProcessor freemarkerRenditionProcessor,
            ProposalConverterService proposalConverterService,
           LeosPermissionAuthorityMapHelper authorityMapHelper,
           BillService billService,
           MemorandumService memorandumService,
           AnnexService annexService,
           ContentComparatorService compareService,
           MessageHelper messageHelper) {
        this.packageRepository = packageRepository;
        this.workspaceRepository = workspaceRepository;
        this.attachmentProcessor = attachmentProcessor;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.annotateService = annotateService;
        this.freemarkerRenditionProcessor = freemarkerRenditionProcessor;
        this.proposalConverterService = proposalConverterService;
        this.authorityMapHelper = authorityMapHelper;
        this.billService = billService;
        this.memorandumService = memorandumService;
        this.annexService = annexService;
        this.messageHelper = messageHelper;
        this.compareService = compareService;
    }

    @Override
    public LeosPackage createPackage() {
        String name = generatePackageName();
        return packageRepository.createPackage(storagePath, name);
    }

    @Override
    public void deletePackage(LeosPackage leosPackage) {
        packageRepository.deletePackage(leosPackage.getPath());
    }

    @Override
    public LeosPackage findPackageByDocumentId(String documentId) {
        return packageRepository.findPackageByDocumentId(documentId);
    }

    @Override
    public <T extends LeosDocument> List<T> findDocumentsByPackagePath(String path, Class<T> filterType, Boolean fetchContent) {
        return packageRepository.findDocumentsByPackagePath(path, filterType, fetchContent);
    }

    @Override
    public <T extends LeosDocument> T findDocumentByPackagePathAndName(String path, String name, Class<T> filterType) {
        return packageRepository.findDocumentByPackagePathAndName(path, name, filterType);
    }
    
    @Override
    public <T extends LeosDocument> List<T> findDocumentsByPackageId(String id, Class<T> filterType, Boolean allVersions, Boolean fetchContent) {
        return packageRepository.findDocumentsByPackageId(id, filterType, allVersions, fetchContent);
    }

    @Override
    public <T extends Proposal> List<T> findDocumentsByUserId(String userId, Class<T> filterType, String leosAuthority) {
        return packageRepository.findDocumentsByUserId(userId, filterType, leosAuthority);
    }

    @Override
    public List<LegDocumentVO> getLegDocumentDetailsByUserId(String userId) {
        List<Proposal> proposals = findDocumentsByUserId(userId, Proposal.class, authorityMapHelper.getRoleForDocCreation());
        List<LegDocumentVO> legDocumentVOs = new ArrayList<>();
        for (Proposal proposal : proposals) {
            LeosPackage leosPackage = findPackageByDocumentId(proposal.getId());
            List<LegDocument> legDocuments = findDocumentsByPackagePath(leosPackage.getPath(), LegDocument.class, false);
            if (!legDocuments.isEmpty()) {
                LegDocument leg = legDocuments.get(0);
                LegDocumentVO legDocumentVO = new LegDocumentVO();
                legDocumentVO.setProposalId(proposal.getVersionSeriesId());
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

    private String generatePackageName() {
        return PACKAGE_NAME_PREFIX + Cuid.createCuid();
    }

    private String generateLegName(){
        return LEG_FILE_PREFIX + Cuid.createCuid() + LEG_FILE_EXTENSION;
    }
	
	private byte[] addObjectIdToProposal(byte[] xmlContent, Proposal proposal){
        Option<ProposalMetadata> metadataOption = proposal.getMetadata();
        ProposalMetadata metadata = metadataOption.get().withObjectId(proposal.getId());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }

    private byte[] addObjectIdToMemorandum(byte[] xmlContent, Memorandum memorandum){
        Option<MemorandumMetadata> metadataOption = memorandum.getMetadata();
        MemorandumMetadata metadata = metadataOption.get().withObjectId(memorandum.getId());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }

    private byte[] addObjectIdToBill(byte[] xmlContent, Bill bill){
        Option<BillMetadata> metadataOption = bill.getMetadata();
        BillMetadata metadata = metadataOption.get().withObjectId(bill.getId());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;

    }

    private byte[] addObjectIdToAnnex(byte[] xmlContent, Annex annex){
        Option<AnnexMetadata> metadataOption = annex.getMetadata();
        AnnexMetadata metadata = metadataOption.get().withObjectId(annex.getId());
        xmlContent = xmlNodeProcessor.setValuesInXml(xmlContent, createValueMap(metadata), xmlNodeConfigHelper.getConfig(metadata.getCategory()));
        return xmlContent;
    }

    /**
     * Creates the Pair<File, ExportResource> for the given leg file.
     * @param legFile legFile for which we need to create the Pair<File, ExportResource>
     * @return Pair<File, ExportResource> used to be sent to Toolbox for PDF/LegisWrite generation.
     */
    @Override
    public Pair<File, ExportResource> createLegPackage(File legFile, ExportOptions exportOptions) throws IOException {
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

        return new Pair<>(legFileTemp, proposalExportResource);
    }
    
    @Override
    public Pair<File, ExportResource> createLegPackage(String proposalId, ExportOptions exportOptions) throws IOException {
        LOG.trace("Creating Leg Package using [documentId={}]", proposalId);
        return createLegPackage(proposalId, exportOptions, null);
    }
    
    /**
     * Creates the Pair<File, ExportResource>, which is the logical representation of the leg file, for the given proposalId.
     * @param proposalId proposalId for which we need to create the Pair<File, ExportResource>
     * @param exportOptions exportOptions to select what needs to be exported
     * @param versionToCompare Required in case ExportOptions.ComparisonType.DOUBLE is selected for export of double comparison result
     * @return Pair<File, ExportResource> used to be sent to Toolbox for PDF/LegisWrite generation.
     */
    @Override
    public Pair<File, ExportResource> createLegPackage(String proposalId, ExportOptions exportOptions, XmlDocument versionToCompare) throws IOException {
        LOG.trace("Creating Leg Package... [documentId={}]", proposalId);

        final LeosPackage leosPackage = packageRepository.findPackageByDocumentId(proposalId);
        final Map<String, Object> contentToZip = new HashMap<>();
        final ExportResource exportProposalResource = new ExportResource(LeosCategory.PROPOSAL);
        exportProposalResource.setExportOptions(exportOptions);
        
        //1. Add Proposal to package
        final Proposal proposal = workspaceRepository.findDocumentById(proposalId, Proposal.class, true);
        final Map<String, String> proposalRefsMap = enrichZipWithProposal(contentToZip, exportProposalResource, proposal);
        
        //2. Depending on ExportOptions FileType add documents to package 
        Bill bill; 
        switch (exportOptions.getFileType()) {
            case "LEGALTEXT": // TODO: Get from LeosCategory once Kotlin code is removed
                bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(),
                        proposalRefsMap.get(LeosCategory.BILL.name() + "_href"), Bill.class);
                addBillToPackage(contentToZip, exportProposalResource, proposalRefsMap, bill, versionToCompare);
                break;
            case "MEMORANDUM":
                if (proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_href") != null) {
                    addMemorandumToPackage(leosPackage, contentToZip, exportProposalResource, proposalRefsMap);
                }
                break;
            case "ANNEX":
                bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(),
                        proposalRefsMap.get(LeosCategory.BILL.name() + "_href"), Bill.class);
                addAnnexToPackage(leosPackage, bill, contentToZip, exportProposalResource);
                break;
            default:
                bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(),
                        proposalRefsMap.get(LeosCategory.BILL.name() + "_href"), Bill.class);
                ExportResource exportBillResource = addBillToPackage(contentToZip, exportProposalResource, proposalRefsMap, bill, versionToCompare);
                if (proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_href") != null) {
                    addMemorandumToPackage(leosPackage, contentToZip, exportProposalResource, proposalRefsMap);
                }
                addAnnexToPackage(leosPackage, bill, contentToZip, exportBillResource);
        }
        
        //3. Add TOC
        enrichZipWithToc(contentToZip);

        //4. Add media
        final List<MediaDocument> mediaDocs = packageRepository.findDocumentsByPackagePath(leosPackage.getPath(), MediaDocument.class, true);
        enrichZipWithMedia(contentToZip, mediaDocs);
        
        return new Pair<>(ZipPackageUtil.zipFiles(proposalRefsMap.get(XmlNodeConfigHelper.PROPOSAL_DOC_COLLECTION) + ".leg", contentToZip), exportProposalResource);
    }

    private ExportResource addBillToPackage(final Map<String, Object> contentToZip, ExportResource exportProposalResource,
            final Map<String, String> proposalRefsMap, final Bill bill, XmlDocument versionToCompare) {
        Content content = bill.getContent().getOrError(() -> "Bill content is required!");
        byte[] xmlContent = content.getSource().getBytes();
        return enrichZipWithBill(contentToZip, exportProposalResource, proposalRefsMap, bill, xmlContent, versionToCompare);
    }

    private void addMemorandumToPackage(final LeosPackage leosPackage, final Map<String, Object> contentToZip, ExportResource exportProposalResource,
            final Map<String, String> proposalRefsMap) {
        final Memorandum memorandum = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), proposalRefsMap.get(LeosCategory.MEMORANDUM.name() + "_href"), Memorandum.class);
        enrichZipWithMemorandum(contentToZip, exportProposalResource, proposalRefsMap, memorandum);
    }

    private void addAnnexToPackage(final LeosPackage leosPackage, Bill bill, final Map<String, Object> contentToZip, ExportResource exportProposalResource) {
        Content content = bill.getContent().getOrError(() -> "Bill content is required!");
        byte[] xmlContent = content.getSource().getBytes();
        final Map<String, String> attachmentIds = attachmentProcessor.getAttachmentsIdFromBill(xmlContent);
        final String annexStyleSheet = LeosCategory.ANNEX.name().toLowerCase()+STYLE_SHEET_EXT;
        attachmentIds.forEach((href, id) -> {
            final Annex annex = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), href, Annex.class);
            enrichZipWithAnnex(contentToZip, exportProposalResource, annexStyleSheet, annex, id);
        });
        if(!attachmentIds.isEmpty()) {
            //Add annex style only if at least one is present
            addResourceToZipContent(contentToZip, annexStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        }
    }
    
    private Map<String, String> enrichZipWithProposal(final Map<String, Object> contentToZip, ExportResource exportProposalResource, Proposal proposal) {
        Content content = proposal.getContent().getOrError(() -> "Proposal content is required!");
        byte[] xmlContent = content.getSource().getBytes();
        xmlContent = addObjectIdToProposal(xmlContent, proposal);
        contentToZip.put("main.xml", xmlContent);

        return buildProposalExportResource(exportProposalResource, xmlContent);
    }
    
    private void enrichZipWithToc(final Map<String, Object> contentToZip) {
        addResourceToZipContent(contentToZip, "jquery" + JS_EXT, JQUERY_SOURCE_PATH, JS_DEST_DIR);
        addResourceToZipContent(contentToZip, "jqtree" + JS_EXT, JQTREE_SOURCE_PATH, JS_DEST_DIR);
        addResourceToZipContent(contentToZip, "jqtree" + STYLE_SHEET_EXT, JQTREE_SOURCE_PATH + "css/", STYLE_DEST_DIR);
        addResourceToZipContent(contentToZip, "leos-toc-rendition" + JS_EXT, JS_SOURCE_PATH + "rendition/", JS_DEST_DIR);
        addResourceToZipContent(contentToZip, "leos-toc-rendition"    + STYLE_SHEET_EXT, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
    }

    private void enrichZipWithMemorandum(final Map<String, Object> contentToZip, ExportResource exportProposalResource, Map<String, String> proposalRefsMap, Memorandum memorandum) {
        Content content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        ExportOptions exportOptions = exportProposalResource.getExportOptions();

        byte[] xmlContent = content.getSource().getBytes();
        xmlContent = addObjectIdToMemorandum(xmlContent, memorandum);
        contentToZip.put(memorandum.getName(), xmlContent);
        if(exportOptions.isConvertAnnotations()) {
            String annotations = annotateService.getAnnotations(memorandum.getName());
            addAnnotateToZipContent(contentToZip, memorandum.getName(), annotations);
        }
        
        String memoStyleSheet = LeosCategory.MEMORANDUM.name().toLowerCase()+STYLE_SHEET_EXT;
        addResourceToZipContent(contentToZip, memoStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        String memoTocJson = getTocAsJson(memorandumService.getTableOfContent(memorandum));
        addHtmlRendition(contentToZip, memorandum,  memoStyleSheet, memoTocJson);
        
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
                xmlContent = addObjectIdToBill(xmlContent, bill);
        }
        contentToZip.put(bill.getName(), xmlContent);
        if(exportOptions.isConvertAnnotations()) {
            String billAnnotations = annotateService.getAnnotations(bill.getName());
            addAnnotateToZipContent(contentToZip,bill.getName(), billAnnotations);
        }
        
        String billStyleSheet = LeosCategory.BILL.name().toLowerCase()+STYLE_SHEET_EXT;
        addResourceToZipContent(contentToZip, billStyleSheet, STYLES_SOURCE_PATH, STYLE_DEST_DIR);
        String billTocJson = getTocAsJson(billService.getTableOfContent(bill, true));
        addHtmlRendition(contentToZip, bill, billStyleSheet, billTocJson);
       
        final  ExportResource exportBillResource = buildExportResourceBill(proposalRefsMap, xmlContent);
        exportBillResource.setExportOptions(exportOptions);
        exportProposalResource.addChildResource(exportBillResource);
        return exportBillResource;
    }

    private String doubleCompareXmlContents(Bill originalVersion, XmlDocument intermediateMajor, byte[] currentXmlContent) {
        String originalXml = originalVersion.getContent().getOrError(() -> "Original document content is required!")
                .getSource().toString();
        String intermediateMajorXml = intermediateMajor.getContent().getOrError(() -> "Intermadiate Major Version document content is required!")
                .getSource().toString();
        String currentXml = new String(currentXmlContent, UTF_8);

        return compareService.compareContents(new ContentComparatorContext.Builder(originalXml, currentXml, intermediateMajorXml)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(DOUBLE_COMPARE_REMOVED_CLASS)
                .withAddedValue(DOUBLE_COMPARE_ADDED_CLASS)
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
    
    private void enrichZipWithAnnex(final Map<String, Object> contentToZip, ExportResource exportBillResource, String annexStyleSheet, Annex annex, String resourceId) {
        ExportOptions exportOptions = exportBillResource.getExportOptions();
        
        final Content annexContent = annex.getContent().getOrError(() -> "Annex content is required!");
        byte[] xmlAnnexContent = annexContent.getSource().getBytes();
        xmlAnnexContent = addObjectIdToAnnex(xmlAnnexContent, annex);
        contentToZip.put(annex.getName(), xmlAnnexContent);
        
        if(exportOptions.isConvertAnnotations()) {
            String annexAnnotations = annotateService.getAnnotations(annex.getName());
            addAnnotateToZipContent(contentToZip, annex.getName(), annexAnnotations);
        }
        
        String annexTocJson = getTocAsJson(annexService.getTableOfContent(annex));
        addHtmlRendition(contentToZip, annex, annexStyleSheet, annexTocJson);
   
        int docNumber = annex.getMetadata().get().getIndex();
        final ExportResource annexExportResource = buildExportResourceAnnex(docNumber, resourceId, xmlAnnexContent);
        exportBillResource.addChildResource(annexExportResource);
    }
    
    private List<TableOfContentItemHtmlVO> buildTocHtml(List<TableOfContentItemVO> tableOfContents) {
        List<TableOfContentItemHtmlVO> tocHtml = new ArrayList<>();
        for (TableOfContentItemVO item : tableOfContents) {
            String name = TableOfContentHelper.buildItemCaption(item, TableOfContentHelper.DEFAULT_CAPTION_MAX_SIZE, messageHelper);
            TableOfContentItemHtmlVO itemHtml = new TableOfContentItemHtmlVO(name, "#" + item.getId());
            if(item.getChildItems().size() > 0) {
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
        ExportResource annexExportResource = new ExportResource(LeosCategory.ANNEX);
        annexExportResource.setResourceId(resourceId);
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
    public LegDocument createLegDocument(String proposalId, String jobId, String milestoneComment, File legFile, LeosLegStatus status) throws IOException {
        LOG.trace("Creating Leg Document for Package... [documentId={}]", proposalId);
        return packageRepository.createLegDocumentFromContent(packageRepository.findPackageByDocumentId(proposalId).getPath(), generateLegName(),
                jobId, Collections.singletonList(milestoneComment), getFileContent(legFile), status);
    }

    @Override
    public LegDocument updateLegDocument(String id, LeosLegStatus status) {
        LOG.trace("Updating Leg document status... [id={}, status={}]", id, status.name());
        return packageRepository.updateLegDocument(id, status);
    }

    @Override
    public LegDocument updateLegDocument(String id, byte[] pdfJobZip, byte[] wordJobZip){
        LOG.trace("Updating Leg document with id={} status to {} and content with pdf and word renditions", id, LeosLegStatus.FILE_READY.name());
        LegDocument document = findLegDocumentById(id);
        try {
            byte[] content = updateContentWithPdfAndWordRenditions(pdfJobZip, wordJobZip, document.getContent().getOrNull());
            return packageRepository.updateLegDocument(document.getId(), LeosLegStatus.FILE_READY, content, true, "Milestone is now validated");
        } catch (Exception e){
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
    public LegDocument findLegDocumentByAnyDocumentIdAndJobId(String documentId, String jobId){
        LOG.trace("Finding Leg Document by proposal id and job id... [proposalId={}, jobId={}]", documentId, jobId);
        LeosPackage leosPackage = findPackageByDocumentId(documentId);
        List<LegDocument> legDocuments = findDocumentsByPackageId(leosPackage.getId(), LegDocument.class, false, false);
        return legDocuments.stream()
                .filter(legDocument -> jobId.equals(legDocument.getJobId()))
                .findAny()
                .orElse(null);
    }

    @Override
    public List<LegDocument> findLegDocumentByStatus(LeosLegStatus leosLegStatus){
        return packageRepository.findDocumentsByStatus(leosLegStatus, LegDocument.class);
    }

    private byte[] updateContentWithPdfAndWordRenditions(byte[] pdfJobZip, byte[] wordJobZip, Content content) throws IOException{
        Map<String, Object> legContent = ZipPackageUtil.unzipByteArray(content.getSource().getBytes());
        addPdfRendition(pdfJobZip, legContent);
        addWordRenditions(wordJobZip, legContent);
        return ZipPackageUtil.zipByteArray(legContent);
    }

    private void addPdfRendition(byte[] pdfJobZip, Map<String, Object> legContent) throws IOException {
        Map.Entry<String, Object> neededEntry =  unzipJobResult(pdfJobZip).entrySet().stream()
                .filter(pdfEntry -> !pdfEntry.getKey().endsWith("_pdfa.pdf"))
                .findAny()
                .orElseThrow(() -> new FileNotFoundException("Pdf rendition not found in the pdf document job file"));
        legContent.put(PDF_RENDITION + neededEntry.getKey(), neededEntry.getValue());
    }

    private void addWordRenditions(byte[] wordJobZip, Map<String, Object> legContent) throws IOException {
        List<String> wordEntries = new ArrayList<>();
        unzipJobResult(wordJobZip).entrySet().stream()
                .filter(wordEntity -> !wordEntity.getKey().isEmpty())
                .forEach(wordEntry -> { legContent.put(WORD_RENDITION + wordEntry.getKey(), wordEntry.getValue());
                                        wordEntries.add(wordEntry.getKey());});
        if(wordEntries.isEmpty()){
            throw new FileNotFoundException("No word rendition found in the word document job file");
        }
    }

    private Map<String, Object> unzipJobResult(byte[] jobZip) throws IOException {
        Map<String, Object> jobContent = ZipPackageUtil.unzipByteArray(jobZip);
        for(Map.Entry<String, Object> entry : jobContent.entrySet()){
            if(entry.getKey().endsWith("_out.zip")){
                return ZipPackageUtil.unzipByteArray((byte[]) entry.getValue());
            }
        }
        throw new FileNotFoundException("The job result zip file is not present in the job file");
    }

    private byte[] getFileContent(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)){
            byte[] content = new byte[(int) file.length()];
            is.read(content);
            return content;
        }
    }

    private void addHtmlRendition(Map<String, Object> contentToZip, XmlDocument xmlDocument, String styleSheetName, String tocList) {
        try {
            RenderedDocument htmlDocument = new RenderedDocument();
            htmlDocument.setContent(new ByteArrayInputStream(getContent(xmlDocument)));
            htmlDocument.setStyleSheetName(styleSheetName);
            String htmlName = HTML_RENDITION + xmlDocument.getName().replaceAll(".xml", ".html");
            contentToZip.put(htmlName, freemarkerRenditionProcessor.processTemplate(htmlDocument).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UnsupportedEncodingException while processing document "+ xmlDocument.getName(), exception);
        }
        
        try {
            RenderedDocument tocHtmlDocument = new RenderedDocument();
            tocHtmlDocument.setContent(new ByteArrayInputStream(getContent(xmlDocument)));
            tocHtmlDocument.setStyleSheetName(styleSheetName);
            String tocHtmlName = HTML_RENDITION + xmlDocument.getName();
            tocHtmlName = tocHtmlName.substring(0, tocHtmlName.indexOf(".xml")) + "_toc" + ".html";
            contentToZip.put(tocHtmlName, freemarkerRenditionProcessor.processTocTemplate(tocHtmlDocument, tocList).getBytes("UTF-8"));
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UnsupportedEncodingException while processing document "+ xmlDocument.getName(), exception);
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
