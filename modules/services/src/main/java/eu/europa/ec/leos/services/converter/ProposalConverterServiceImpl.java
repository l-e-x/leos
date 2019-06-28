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
package eu.europa.ec.leos.services.converter;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.services.export.ZipPackageUtil;
import eu.europa.ec.leos.services.store.TemplateService;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.vo.catalog.CatalogItem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ProposalConverterServiceImpl implements ProposalConverterService {

    private static final Logger LOG = LoggerFactory.getLogger(ProposalConverterServiceImpl.class);

    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;
    protected final XmlContentProcessor xmlContentProcessor;
    private final TemplateService templateService;

    private static final String ANNEX_FILE_PREFIX = "annex_";
    private static final String BILL_FILE_PREFIX = "bill_";
    private static final String MEMORANDUM_FILE_PREFIX = "memorandum_";
    private static final String MEDIA_FILE_PREFIX = "media_";
    private static final String PROPOSAL_FILE = "main.xml";
    private static final String XML_DOC_EXT = ".xml";

    private List<CatalogItem> templatesCatalog;

    @Autowired
    ProposalConverterServiceImpl(
            XmlNodeProcessor xmlNodeProcessor,
            XmlNodeConfigHelper xmlNodeConfigHelper,
            XmlContentProcessor xmlContentProcessor,
            TemplateService templateService) {
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.xmlContentProcessor = xmlContentProcessor;
        this.templateService = templateService;
    }

    /**
     * Creates a DocumentVO for the leg file passed as parameter.
     * After the computation the file will be deleted from the filesystem.
     *
     * The xml files inside the leg/zip file are mapped into array source[] of DocumentVO.
     * When canModifySource is true, some tags are not included in the source[] field, otherwise when is false the
     * array source contains the xml as it is in the zip/leg file.
     *
     * @param file leg file from where to create the DocumentVO.
     * @param proposal DocumentVO with the data of the proposal. The same object will be enriched and returned by the method
     * @param canModifySource true to exclude some xml tags into byte array source, false if you need to keep the original integrity of the document
     * @return the enriched DocumentVO representing the proposal inside the leg file.
     */
    public DocumentVO createProposalFromLegFile(File file, final DocumentVO proposal, boolean canModifySource) {
        proposal.clean();
        proposal.setId(PROPOSAL_FILE);
        proposal.setCategory(LeosCategory.PROPOSAL);
        // unzip file
        Map<String, Object> unzippedFiles = ZipPackageUtil.unzipFiles(file);
        try {
            templatesCatalog = templateService.getTemplatesCatalog();
            if (unzippedFiles.containsKey(PROPOSAL_FILE)) {
                List<DocumentVO> propChildDocs = new ArrayList<>();
                File proposalFile = (File) unzippedFiles.get(PROPOSAL_FILE);
                updateSource(proposal, proposalFile, canModifySource);
                updateMetadataVO(proposal);
                List<DocumentVO> billChildDocs = new ArrayList<>();
                DocumentVO billDoc = null;
                for (String docName : unzippedFiles.keySet()) {
                    File docFile = (File) unzippedFiles.get(docName);
                    DocumentVO doc = createDocument(docName, docFile, canModifySource);
                    if (doc != null) {
                        if (doc.getCategory() == LeosCategory.ANNEX || doc.getCategory() == LeosCategory.MEDIA) {
                            billChildDocs.add(doc);
                        } else if (doc.getCategory() == LeosCategory.BILL) {
                            billDoc = doc;
                        } else {
                            propChildDocs.add(doc);
                        }
                    }
                }
                if (billDoc != null) {
                    billDoc.setChildDocuments(billChildDocs);
                    propChildDocs.add(billDoc);
                }
                proposal.setChildDocuments(propChildDocs);
            }
        } catch (Exception e) {
            LOG.error("Error generating the map of the document: {}", e);
        } finally {
            deleteFiles(file, unzippedFiles);
        }
        return proposal;
    }

    private DocumentVO createDocument(String docName, File docFile, boolean canModifySource) {
        DocumentVO doc = null;
        LeosCategory category = identifyCategory(docName);
        if (category != null) {
            doc = new DocumentVO(category);
            doc.setId(docName);
            updateSource(doc, docFile, canModifySource);
            updateMetadataVO(doc);
        }
        return doc;
    }

    private LeosCategory identifyCategory(String docName) {
        LeosCategory category = null;
        if (docName.endsWith(XML_DOC_EXT)) {
            if (docName.startsWith(ANNEX_FILE_PREFIX)) {
                category = LeosCategory.ANNEX;
            } else if (docName.startsWith(BILL_FILE_PREFIX)) {
                category = LeosCategory.BILL;
            } else if (docName.startsWith(MEMORANDUM_FILE_PREFIX)) {
                category = LeosCategory.MEMORANDUM;
            } else if (docName.startsWith(MEDIA_FILE_PREFIX)) {
                category = LeosCategory.MEDIA;
            }
        }
        return category;
    }

    protected abstract void updateSource(final DocumentVO document, File documentFile, boolean canModifySource);

    private void updateMetadataVO(final DocumentVO document) {
        if (document.getSource() != null) {
            try {
                MetadataVO metadata = document.getMetadata();
                Map<String, String> metadataVOMap = xmlNodeProcessor.getValuesFromXml(document.getSource(), new String[]{
                        XmlNodeConfigHelper.DOC_PURPOSE_META,
                        XmlNodeConfigHelper.DOC_STAGE_META,
                        XmlNodeConfigHelper.DOC_TYPE_META,
                        XmlNodeConfigHelper.DOC_LANGUAGE,
                        XmlNodeConfigHelper.DOC_SPECIFIC_TEMPLATE,
                        XmlNodeConfigHelper.DOC_TEMPLATE,
                        XmlNodeConfigHelper.ANNEX_TITLE_META,
                        XmlNodeConfigHelper.ANNEX_INDEX_META,
                        XmlNodeConfigHelper.ANNEX_NUMBER_META,
                }, xmlNodeConfigHelper.getConfig(document.getCategory()));

                metadata.setDocPurpose(metadataVOMap.get(XmlNodeConfigHelper.DOC_PURPOSE_META));
                metadata.setDocStage(metadataVOMap.get(XmlNodeConfigHelper.DOC_STAGE_META));
                metadata.setDocType(metadataVOMap.get(XmlNodeConfigHelper.DOC_TYPE_META));
                metadata.setLanguage(metadataVOMap.get(XmlNodeConfigHelper.DOC_LANGUAGE));
                metadata.setDocTemplate(metadataVOMap.get(XmlNodeConfigHelper.DOC_SPECIFIC_TEMPLATE));
                metadata.setTemplate(metadataVOMap.get(XmlNodeConfigHelper.DOC_TEMPLATE));
                metadata.setTitle(metadataVOMap.get(XmlNodeConfigHelper.ANNEX_TITLE_META));
                metadata.setIndex(metadataVOMap.get(XmlNodeConfigHelper.ANNEX_INDEX_META));
                metadata.setNumber(metadataVOMap.get(XmlNodeConfigHelper.ANNEX_NUMBER_META));

                // if the template doesnt exist in the system we don't continue, we won't import it.
                metadata.setTemplateName(templateService.getTemplateName(templatesCatalog, metadata.getDocTemplate(), metadata.getLanguage()));
            } catch (Exception e) {
                LOG.error("Error parsing metadata {}", e);
            }
        }
    }

    /**
     * Will delete form the temporary folder the files uploaded and the unzipped files + parent folder.
     * @param mainFile
     * @param unzippedFiles
     */
    private void deleteFiles(File mainFile, Map<String, Object> unzippedFiles) {
        mainFile.delete();
        List<String> parentFolders = new ArrayList<>();
        for (String docName : unzippedFiles.keySet()) {
            File unzippedFile = (File) unzippedFiles.get(docName);
            String parent = unzippedFile.getParent();
            if (!parentFolders.contains(parent)) {
                parentFolders.add(parent);
            }
            if (!unzippedFile.delete()) {
                LOG.info("File not deleted {}", unzippedFile.getPath());
            }
        }
        try {
            // we must clean also the folder.
            for (String parent : parentFolders) {
                FileUtils.deleteDirectory(new File(parent));
            }
        } catch (IOException e) {
            LOG.error("Error deleting the folder {}", e);
        }
    }
}
