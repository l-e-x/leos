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
package eu.europa.ec.leos.services.export;

import com.itextpdf.forms.PdfPageFormCopier;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import eu.europa.ec.leos.domain.cmis.Content;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.domain.cmis.document.Memorandum;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.cmis.metadata.ProposalMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.integration.ToolBoxService;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.content.processor.TransformationService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.store.LegService;
import eu.europa.ec.leos.services.store.PackageService;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

abstract class ExportServiceImpl implements ExportService {

    protected final LegService legService;
    protected final PackageService packageService;
    protected ToolBoxService toolBoxService;
    protected final SecurityContext securityContext;
    protected final ExportHelper exportHelper;
    protected final BillService billService;
    protected final AnnexService annexService;
    protected final TransformationService transformationService;

    private Set<String> docVersionSeriesIds;

    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceImpl.class);

    private static final String TEMP_FILE_NAME = "tmp";
    private static final String TEMP_FINAL_FILE_NAME = "tmp_final";
    private static final String PDF_EXTENSION = "pdf";
    private static final String CONTENT = "{content}";
    private static final String BASIC_CSS = "{basicCSS}";

    private static final String EXPORT_TEMPLATE_FILE = "eu/europa/ec/leos/html/templates/export/pdf.html";
    private static final String CSS_EXTENSION = "css";

    @Autowired
    ExportServiceImpl(LegService legService, PackageService packageService, SecurityContext securityContext, ExportHelper exportHelper,
            BillService billService, AnnexService annexService, TransformationService transformationService) {
        this.legService = legService;
        this.packageService = packageService;
        this.securityContext = securityContext;
        this.exportHelper = exportHelper;
        this.billService = billService;
        this.annexService = annexService;
        this.transformationService = transformationService;
    }

    @Override
    public File createDocuWritePackage(String jobFileName, String documentId, ExportOptions exportOptions, Optional<XmlDocument> versionToCompare)
            throws Exception {
        return null;
    }

    @Override
    public String exportToToolboxCoDe(String documentId, ExportOptions exportOptions) throws Exception {
        return null;
    }

    @Override
    public byte[] exportToToolboxCoDe(File legFile, ExportOptions exportOptions) throws Exception {
        return null;
    }

    @Override
    public String exportLegPackage(String proposalId, LegPackage legPackage) throws Exception {
        return null;
    }

    @Override
    public File createCollectionPackage(String jobFileName, String documentId, ExportOptions exportOptions) throws Exception {
        Validate.notNull(jobFileName);
        Validate.notNull(exportOptions);
        Validate.notNull(documentId);
        File legFile = null;
        try {
            LegPackage legPackage = legService.createLegPackage(documentId, exportOptions);
            legFile = legPackage.getFile();
            return createZipFile(legPackage, jobFileName, exportOptions);
        } finally {
            if (legFile != null && legFile.exists()) {
                legFile.delete();
            }
        }
    }

    protected File createZipFile(LegPackage legPackage, String jobFileName, ExportOptions exportOptions) throws Exception{
        Validate.notNull(legPackage);
        Validate.notNull(jobFileName);
        Validate.notNull(exportOptions);
        try (ByteArrayOutputStream contentFileContent = exportHelper.createContentFile(exportOptions, legPackage.getExportResource())) {
            Map<String, Object> contentToZip = new HashMap<>();
            contentToZip.put("content.xml", contentFileContent);
            contentToZip.put(legPackage.getFile().getName(), legPackage.getFile());
            return ZipPackageUtil.zipFiles(jobFileName, contentToZip);
        }
    }

    @Override
    public File exportToPdf(String proposalId) {
        List<File> fileList = new ArrayList<>();

        ConverterProperties converterProperties = new ConverterProperties();
        converterProperties.setTagWorkerFactory(new CustomTagWorkerFactory());
        converterProperties.setCssApplierFactory(new CustomCssApplierFactory());
        converterProperties.setFontProvider(new DefaultFontProvider(true, true, true));

        List<String> htmlSourceList = getHTMLContent(proposalId);
        htmlSourceList.forEach(htmlSource -> {
            String fileName = String.format("%s_%s.%s", TEMP_FINAL_FILE_NAME, (new Date()).getTime(), PDF_EXTENSION);
            File pdfDest = new File(fileName);

            try {
                HtmlConverter.convertToPdf(htmlSource, new FileOutputStream(pdfDest), converterProperties);
                fileList.add(pdfDest);
            } catch (Exception e) {
                LOG.error("An error occurred when export to Pdf! {}", e.getMessage(), e);
            }
        });
        File file = mergeDocuments(fileList);
        removeTemporalFile(fileList);
        return file;
    }

    private void removeTemporalFile(List<File> files) {
        for (File f : files) {
            if (f.exists()) {
                f.delete();
            }
        }
    }

    private List<String> getHTMLContent(String proposalId) {
        LeosPackage leosPackage = packageService.findPackageByDocumentId(proposalId);
        List<XmlDocument> documents = packageService.findDocumentsByPackagePath(leosPackage.getPath(),
                XmlDocument.class, false);

        DocumentVO proposalVO = createViewObject(documents);
        List<Entry<LeosCategory, String>> documentHtmlList = getHTMLContent(proposalVO, new ArrayList<>());

        return fillTemplate(documentHtmlList);
    }

    private List<Entry<LeosCategory, String>> getHTMLContent(DocumentVO documentVO, List<Entry<LeosCategory, String>> htmlList) {
        for (DocumentVO d : documentVO.getChildDocuments()) {
            String documentId = d.getId();
            String html = getHtmlContent(documentId, d.getDocumentType());
            Entry<LeosCategory, String> htmlEntry = new SimpleEntry<>(d.getDocumentType(), html);
            htmlList.add(htmlEntry);
            if (!d.getChildDocuments().isEmpty()) {
                htmlList = getHTMLContent(d, htmlList);
            }
        };

        return htmlList;
    }

    private String getHtmlContent(String documentId, LeosCategory category) {
        String html = "";
        try {
            if (documentId != null) {
                if (category == LeosCategory.BILL) {
                    Bill bill = billService.findBill(documentId);
                    html = getHtmlContent(bill, category);
                } else if (category == LeosCategory.ANNEX) {
                    Annex annex = annexService.findAnnex(documentId);
                    html = getHtmlContent(annex, category);
                }
                // else if (category == LeosCategory.REPORT) {
                // Report report= reportService.findReport(documentId);
                // html = getHtmlContent(report, category);
                // }
                else if (category == LeosCategory.MEMORANDUM) {
                    // TODO
                }
            }
        } catch (IllegalArgumentException iae) {
            LOG.debug("Document {} cannot be retrieved due to exception {}, Rejecting view", documentId,
                    iae.getMessage(), iae);
        }
        return html;
    }

    private String getHtmlContent(LeosDocument document, LeosCategory category) {
        return transformationService.toEditableXml(new ByteArrayInputStream(getContent(document)), null, category,
                securityContext.getPermissions(document));
    }

    private byte[] getContent(LeosDocument doc) {
        final Content content = doc.getContent().getOrError(() -> "Document content is required!");
        return content.getSource().getBytes();
    }

    private String getFileContent(String fileName) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        if (is != null) {
            try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
                return buffer.lines().collect(Collectors.joining(""));
            } catch (IOException e) {
                LOG.error("An error occurred when getting file content! {}", e.getMessage(), e);
            }
        }
        return "";
    }

    private DocumentVO createViewObject(List<XmlDocument> documents) {
        DocumentVO proposalVO = new DocumentVO(LeosCategory.PROPOSAL);
        List<DocumentVO> annexVOList = new ArrayList<>();
        docVersionSeriesIds = new HashSet<>();
        // We have the latest version of the document, no need to search for them again
        for (XmlDocument document : documents) {
            switch (document.getCategory()) {
                case PROPOSAL: {
                    Proposal proposal = (Proposal) document;
                    MetadataVO metadataVO = createMetadataVO(proposal);
                    proposalVO.setMetaData(metadataVO);
                    proposalVO.addCollaborators(proposal.getCollaborators());
                    break;
                }
                case MEMORANDUM: {
                    Memorandum memorandum = (Memorandum) document;
                    DocumentVO memorandumVO = getMemorandumVO(memorandum);
                    proposalVO.addChildDocument(memorandumVO);
                    memorandumVO.addCollaborators(memorandum.getCollaborators());
                    memorandumVO.getMetadata().setInternalRef(memorandum.getMetadata().getOrError(() -> "Memorandum metadata is not available!").getRef());
                    memorandumVO.setVersionSeriesId(memorandum.getVersionSeriesId());
                    docVersionSeriesIds.add(memorandum.getVersionSeriesId());
                    break;
                }
                case BILL: {
                    Bill bill = (Bill) document;
                    DocumentVO billVO = getLegalTextVO(bill);
                    proposalVO.addChildDocument(billVO);
                    billVO.addCollaborators(bill.getCollaborators());
                    billVO.getMetadata().setInternalRef(bill.getMetadata().getOrError(() -> "Legal text metadata is not available!").getRef());
                    billVO.setVersionSeriesId(bill.getVersionSeriesId());
                    docVersionSeriesIds.add(bill.getVersionSeriesId());
                    break;
                }
                case ANNEX: {
                    Annex annex = (Annex) document;
                    DocumentVO annexVO = createAnnexVO(annex);
                    annexVO.addCollaborators(annex.getCollaborators());
                    annexVO.getMetadata().setInternalRef(annex.getMetadata().getOrError(() -> "Annex metadata is not available!").getRef());
                    annexVOList.add(annexVO);
                    annexVO.setVersionSeriesId(annex.getVersionSeriesId());
                    docVersionSeriesIds.add(annex.getVersionSeriesId());
                    break;
                }
                default:
                    LOG.debug("Do nothing for rest of the categories like MEDIA, CONFIG & LEG");
                    break;
            }
        }

        annexVOList.sort(Comparator.comparingInt(DocumentVO::getDocNumber));
        DocumentVO legalText = proposalVO.getChildDocument(LeosCategory.BILL);
        if (legalText != null) {
            for (DocumentVO annexVO : annexVOList) {
                legalText.addChildDocument(annexVO);
            }
        }

        return proposalVO;
    }

    // FIXME refine
    private DocumentVO getMemorandumVO(Memorandum memorandum) {
        return new DocumentVO(memorandum.getId(),
                memorandum.getMetadata().exists(m -> m.getLanguage() != null) ? memorandum.getMetadata().get().getLanguage() : "EN",
                LeosCategory.MEMORANDUM,
                memorandum.getLastModifiedBy(),
                Date.from(memorandum.getLastModificationInstant()));
    }

    // FIXME refine
    private DocumentVO getLegalTextVO(Bill bill) {
        return new DocumentVO(bill.getId(),
                bill.getMetadata().exists(m -> m.getLanguage() != null) ? bill.getMetadata().get().getLanguage() : "EN",
                LeosCategory.BILL,
                bill.getLastModifiedBy(),
                Date.from(bill.getLastModificationInstant()));
    }

    // FIXME refine
    private DocumentVO createAnnexVO(Annex annex) {
        DocumentVO annexVO = new DocumentVO(annex.getId(),
                annex.getMetadata().exists(m -> m.getLanguage() != null) ? annex.getMetadata().get().getLanguage() : "EN",
                LeosCategory.ANNEX,
                annex.getLastModifiedBy(),
                Date.from(annex.getLastModificationInstant()));

        if (annex.getMetadata().isDefined()) {
            AnnexMetadata metadata = annex.getMetadata().get();
            annexVO.setDocNumber(metadata.getIndex());
            annexVO.setTitle(metadata.getTitle());
        }

        return annexVO;
    }

    private MetadataVO createMetadataVO(Proposal proposal) {
        ProposalMetadata metadata = proposal.getMetadata().getOrError(() -> "Proposal metadata is not available!");
        return new MetadataVO(metadata.getStage(), metadata.getType(), metadata.getPurpose(), metadata.getTemplate(), metadata.getLanguage());
    }

    private List<String> fillTemplate(List<Entry<LeosCategory, String>> htmlContentList) {
        List<String> templateFilledList = new ArrayList<>();

        htmlContentList.forEach(entry -> {
            String templateContent = getFileContent(EXPORT_TEMPLATE_FILE);
            String cssFileName = String.format("%s.%s", entry.getKey().name().toLowerCase(), CSS_EXTENSION);
            // change for include annex, memorandum, report
            if (cssFileName.contains("bill")) {
                String basicCss = getFileContent("META-INF/resources/assets/css/" + cssFileName);
                if (!"".equals(entry.getValue())) {
                    templateContent = templateContent.replace(BASIC_CSS, basicCss);
                    templateContent = templateContent.replace(CONTENT, entry.getValue());
                    templateFilledList.add(templateContent);
                }
            }
        });
        return templateFilledList;
    }

    private File mergeDocuments(List<File> files) {
        String fileName = String.format("%s_%s.%s", TEMP_FILE_NAME, (new Date()).getTime(), PDF_EXTENSION);
        File pdfDest = new File(fileName);

        PdfDocument pdfDoc;
        try {
            pdfDoc = new PdfDocument(new PdfWriter(pdfDest));
            pdfDoc.initializeOutlines();
            files.forEach(file -> {
                try {
                    PdfReader reader = new PdfReader(file);
                    PdfDocument readerDoc = new PdfDocument(reader);
                    readerDoc.copyPagesTo(1, readerDoc.getNumberOfPages(), pdfDoc, new PdfPageFormCopier());
                    readerDoc.close();
                    reader.close();
                } catch (IOException e) {
                    LOG.error("An error occurred when merging documents! {}", e.getMessage(), e);
                }
            });
            pdfDoc.close();
        } catch (FileNotFoundException e) {
            LOG.error("An error occurred when merging documents! {}", e.getMessage(), e);
        }
        return pdfDest;
    }
}
