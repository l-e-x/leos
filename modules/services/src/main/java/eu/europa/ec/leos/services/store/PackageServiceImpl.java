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
package eu.europa.ec.leos.services.store;

import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.document.*;
import eu.europa.ec.leos.domain.document.LeosDocument.MediaDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Proposal;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.integration.toolbox.ExportResource;
import eu.europa.ec.leos.integration.utils.zip.ZipPackageUtil;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.repository.store.WorkspaceRepository;
import eu.europa.ec.leos.services.content.processor.AttachmentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfig;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import io.atlassian.fugue.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
class PackageServiceImpl implements PackageService {
    private static final Logger LOG = LoggerFactory.getLogger(PackageServiceImpl.class);

    private static final String PACKAGE_NAME_PREFIX = "package_";

    private final PackageRepository packageRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AttachmentProcessor attachmentProcessor;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;

    @Value("${leos.workspaces.path}")
    protected String storagePath;

    PackageServiceImpl(PackageRepository packageRepository,
            WorkspaceRepository workspaceRepository,
            AttachmentProcessor attachmentProcessor,
            XmlNodeProcessor xmlNodeProcessor,
            XmlNodeConfigHelper xmlNodeConfigHelper) {
        this.packageRepository = packageRepository;
        this.workspaceRepository = workspaceRepository;
        this.attachmentProcessor = attachmentProcessor;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
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
    public <T extends LeosDocument> List<T> findDocumentsByPackagePath(String path, Class<T> filterType) {
        return packageRepository.findDocumentsByPackagePath(path, filterType);
    }

    private String generatePackageName() {
        return PACKAGE_NAME_PREFIX + Cuid.createCuid();
    }

    @Override
    public Pair<File, ExportResource> createLegPackage(String proposalId) throws IOException {
        LOG.trace("Creating Leg Package... [documentId={}]", proposalId);

        LeosPackage leosPackage = packageRepository.findPackageByDocumentId(proposalId);
        List<MediaDocument> mediaDocs = packageRepository.findDocumentsByPackagePath(leosPackage.getPath(), MediaDocument.class);
        Map<String, Object> contentToZip = new HashMap<String, Object>();

        //Get Proposal
        ExportResource exportProposalResource = new ExportResource(LeosCategory.PROPOSAL);

        Proposal proposal = workspaceRepository.findDocumentById(proposalId, Proposal.class, true);
        Content content = proposal.getContent().getOrError(() -> "Proposal content is required!");
        byte[] xmlContent = content.getSource().getByteString().toByteArray();

        Map<String, XmlNodeConfig> config = new HashMap();
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.MEMORANDUM, "GUID"));
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.MEMORANDUM, "href"));
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.BILL, "GUID"));
        config.putAll(xmlNodeConfigHelper.getProposalComponentsConfig(LeosCategory.BILL, "href"));
        config.putAll(xmlNodeConfigHelper.getConfig(LeosCategory.PROPOSAL));
        
        String memoComponentHrefKey = LeosCategory.MEMORANDUM.name() + "_href";
        String memoComponentGUIDKey = LeosCategory.MEMORANDUM.name() + "_GUID";
        String billComponentHrefKey = LeosCategory.BILL.name() + "_href";
        String billComponentGUIDKey = LeosCategory.BILL.name() + "_GUID";
        Map<String, String> proposalRefsMap = xmlNodeProcessor.getValuesFromXml(xmlContent,
                new String[]{XmlNodeConfigHelper.PROPOSAL_DOC_COLLECTION, XmlNodeConfigHelper.DOC_REF_COVER, 
                        memoComponentGUIDKey,
                        memoComponentHrefKey, 
                        billComponentGUIDKey,
                        billComponentHrefKey
                        },
                config);

        String legPackageName = proposalRefsMap.get(XmlNodeConfigHelper.PROPOSAL_DOC_COLLECTION);
        String memorandumResourceRef = proposalRefsMap.get(memoComponentGUIDKey);
        String memorandumResourceName = proposalRefsMap.get(memoComponentHrefKey);
        String billResourceRef = proposalRefsMap.get(billComponentGUIDKey);
        String billResourceName = proposalRefsMap.get(billComponentHrefKey);
        Map<String, String> proposalComponentRefs = new HashMap();
        proposalComponentRefs.put(XmlNodeConfigHelper.DOC_REF_COVER, proposalRefsMap.get(XmlNodeConfigHelper.DOC_REF_COVER));

        //Adding Proposal to zip package
        contentToZip.put("main.xml", xmlContent);
        LOG.trace("Add Proposal to Package");

        exportProposalResource.setResourceId(legPackageName);
        exportProposalResource.setComponentsIdsMap(proposalComponentRefs);

        ExportResource exportMemorandumResource = new ExportResource(LeosCategory.MEMORANDUM);
        exportMemorandumResource.setResourceId(memorandumResourceRef);

        Memorandum memorandum = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), memorandumResourceName, Memorandum.class);
        content = memorandum.getContent().getOrError(() -> "Memorandum content is required!");
        xmlContent = content.getSource().getByteString().toByteArray();

        setComponentsRefs(LeosCategory.MEMORANDUM, exportMemorandumResource, xmlContent);

        //Adding Memorandum to zip package
        contentToZip.put(memorandum.getName(), xmlContent);
        LOG.trace("Add Memorandum to Package");
        exportProposalResource.addChildResource(exportMemorandumResource);

        ExportResource exportBillResource = new ExportResource(LeosCategory.BILL);
        exportBillResource.setResourceId(billResourceRef);

        Bill bill = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), billResourceName, Bill.class);
        content = bill.getContent().getOrError(() -> "Bill content is required!");
        xmlContent = content.getSource().getByteString().toByteArray();

        setComponentsRefs(LeosCategory.BILL, exportBillResource, xmlContent);

        //Adding Bill to zip package
        contentToZip.put(bill.getName(), xmlContent);
        LOG.trace("Add Bill to Package");
        exportProposalResource.addChildResource(exportBillResource);

        //Getting annexes from bill
        Map<String, String> attachmentIds = attachmentProcessor.getAttachmentsIdFromBill(xmlContent);
        attachmentIds.forEach((href, id) -> {
            Annex annex = packageRepository.findDocumentByPackagePathAndName(leosPackage.getPath(), href, Annex.class);
            final Content annexContent = annex.getContent().getOrError(() -> "Annex content is required!");
            final byte[] xmlAnnexContent = annexContent.getSource().getByteString().toByteArray();

            ExportResource exportAnnexResource = new ExportResource(LeosCategory.ANNEX);
            exportAnnexResource.setResourceId(id);
            AnnexMetadata annexMetadata = annex.getMetadata().getOrError(() -> "Annex metadata is required!");
            exportAnnexResource.setDocNumber(annexMetadata.getIndex());
            setComponentsRefs(LeosCategory.ANNEX, exportAnnexResource, xmlAnnexContent);

            //Adding Annex to zip package
            contentToZip.put(annex.getName(), xmlAnnexContent);
            LOG.trace("Add Annex to Package");
            exportBillResource.addChildResource(exportAnnexResource);
        });

        for (MediaDocument mediaDoc : mediaDocs) {
            content = mediaDoc.getContent().getOrError(() -> "Document content is required!");
            byte[] byteContent = content.getSource().getByteString().toByteArray();
            contentToZip.put("media/" + mediaDoc.getName(), byteContent);
        }

        return new Pair(ZipPackageUtil.zipFiles(legPackageName + ".leg", contentToZip), exportProposalResource);
    }

    private void setComponentsRefs(LeosCategory leosCategory, final ExportResource exportResource, byte[] xmlContent) {
        Map<String, String> componentMap = xmlNodeProcessor.getValuesFromXml(xmlContent,
                new String[]{XmlNodeConfigHelper.DOC_REF_COVER},
                xmlNodeConfigHelper.getConfig(leosCategory));
        exportResource.setComponentsIdsMap(componentMap);
    }
}
