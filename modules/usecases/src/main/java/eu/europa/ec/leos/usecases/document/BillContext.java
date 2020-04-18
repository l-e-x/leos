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
package eu.europa.ec.leos.usecases.document;

import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.Proposal;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.domain.vo.MetadataVO;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.document.ProposalService;
import eu.europa.ec.leos.services.store.PackageService;
import io.atlassian.fugue.Option;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static eu.europa.ec.leos.domain.cmis.LeosCategory.ANNEX;
import static eu.europa.ec.leos.domain.cmis.LeosCategory.MEDIA;

@Component
@Scope("prototype")
public class BillContext {

    private static final Logger LOG = LoggerFactory.getLogger(BillContext.class);

    private final BillService billService;
    private final ProposalService proposalService;
    private final PackageService packageService;
    private final AnnexService annexService;

    private final Provider<AnnexContext> annexContextProvider;

    private LeosPackage leosPackage = null;
    private Bill bill = null;
    private String versionComment;
    private String milestoneComment;
    private String purpose = null;
    private String moveDirection = null;
    private String annexId;

    private DocumentVO billDocument;
    private DocumentVO annexDocument;
    private String annexTemplate;

    private final Map<ContextAction, String> actionMsgMap;

    private static final String ANNEX_TITLE_PREFIX = "Annex";

    BillContext(BillService billService,
                PackageService packageService,
                ProposalService proposalService,
                AnnexService annexService,
                Provider<AnnexContext> annexContextProvider) {
        this.billService = billService;
        this.packageService = packageService;
        this.proposalService = proposalService;
        this.annexService = annexService;
        this.annexContextProvider = annexContextProvider;
        this.actionMsgMap = new HashMap<>();
    }

    public void usePackage(LeosPackage leosPackage) {
        Validate.notNull(leosPackage, "Bill package is required!");
        LOG.trace("Using Bill package... [id={}, path={}]", leosPackage.getId(), leosPackage.getPath());
        this.leosPackage = leosPackage;
    }

    public void useTemplate(Bill bill) {
        Validate.notNull(bill, "Bill template is required!");
        LOG.trace("Using Bill template... [id={}, name={}]", bill.getId(), bill.getName());
        this.bill = bill;
    }

    public void useActionMessage(ContextAction action, String actionMsg) {
        Validate.notNull(actionMsg, "Action message is required!");
        Validate.notNull(action, "Context Action not found! [name=%s]", action);

        LOG.trace("Using action message... [action={}, name={}]", action, actionMsg);
        actionMsgMap.put(action, actionMsg);
    }

    public void useActionMessageMap(Map<ContextAction, String> messages) {
        Validate.notNull(messages, "Action message map is required!");

        actionMsgMap.putAll(messages);
    }

    public void usePurpose(String purpose) {
        Validate.notNull(purpose, "Bill purpose is required!");
        LOG.trace("Using Bill purpose... [purpose={}]", purpose);
        this.purpose = purpose;
    }

    public void useMoveDirection(String moveDirection) {
        Validate.notNull(moveDirection, "Bill 'moveDirection' is required!");
        LOG.trace("Using Bill 'move direction'... [moveDirection={}]", moveDirection);
        this.moveDirection = moveDirection;
    }

    public void useAnnex(String annexId) {
        Validate.notNull(annexId, "Bill 'annexId' is required!");
        LOG.trace("Using Bill 'move direction'... [annexId={}]", annexId);
        this.annexId = annexId;
    }

    public void useDocument(DocumentVO document) {
        Validate.notNull(document, "Bill document is required!");
        billDocument = document;
    }

    public void useAnnexDocument(DocumentVO document) {
        Validate.notNull(document, "Annex document is required!");
        annexDocument = document;
    }

    public void useAnnexTemplate(String templateName) {
        Validate.notNull(templateName, "Annex template is required!");
        annexTemplate = templateName;
    }

    public void useVersionComment(String comment) {
        Validate.notNull(comment, "Version comment is required!");
        this.versionComment = comment;
    }

    public void useMilestoneComment(String milestoneComment) {
        Validate.notNull(milestoneComment, "milestoneComment is required!");
        this.milestoneComment = milestoneComment;
    }

    public Bill executeCreateBill() {
        LOG.trace("Executing 'Create Bill' use case...");
        Validate.notNull(leosPackage, "Bill package is required!");
        Validate.notNull(bill, "Bill template is required!");

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");

        Validate.notNull(purpose, "Bill purpose is required!");
        BillMetadata metadata = metadataOption.get().withPurpose(purpose);

        Bill billCreated = billService.createBill(bill.getId(), leosPackage.getPath(), metadata, actionMsgMap.get(ContextAction.METADATA_UPDATED), null);
        return billService.createVersion(billCreated.getId(), VersionType.INTERMEDIATE, actionMsgMap.get(ContextAction.DOCUMENT_CREATED));
    }

    public Bill executeImportBill() {
        LOG.trace("Executing 'Create Bill' use case...");
        Validate.notNull(leosPackage, "Bill package is required!");
        Validate.notNull(bill, "Bill template is required!");

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");

        Validate.notNull(purpose, "Bill purpose is required!");
        // TODO right now it's only needed the docPurpose and ref, we will have to add more in the future.
        BillMetadata metadata = metadataOption.get().withPurpose(purpose);

        Validate.notNull(billDocument.getSource(), "Bill xml is required!");

        Bill billCreated = billService.createBillFromContent(leosPackage.getPath(), metadata, actionMsgMap.get(ContextAction.METADATA_UPDATED), billDocument.getSource());

        // once having bill we can create media and anexes
        if (billDocument.getChildDocuments() != null) {
            for (DocumentVO docChild : billDocument.getChildDocuments()) {
                if (docChild.getCategory() == ANNEX) {
                    useAnnexDocument(docChild);
                    executeImportBillAnnex();
                } else if (docChild.getCategory() == MEDIA) {
                    // TODO not yet implemented
                }
            }
        }

        return billService.createVersion(billCreated.getId(), VersionType.INTERMEDIATE, actionMsgMap.get(ContextAction.DOCUMENT_CREATED));
    }

    public void executeUpdateBill() {
        LOG.trace("Executing 'Update Bill' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");
        Validate.notNull(purpose, "Bill purpose is required!");
        BillMetadata metadata = metadataOption.get().withPurpose(purpose);
        billService.updateBill(bill, metadata, VersionType.MINOR, actionMsgMap.get(ContextAction.METADATA_UPDATED));
        // We dont need to fetch the content here, the executeUpdateAnnexMetadata gets the latest version of the annex by id
        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class, false);
        annexes.forEach(annex -> {
            AnnexContext annexContext = annexContextProvider.get();
            annexContext.usePurpose(purpose);
            annexContext.useAnnexId(annex.getId());
            annexContext.useActionMessageMap(actionMsgMap);
            annexContext.executeUpdateAnnexMetadata();
        });
    }

    public void executeRemoveBillAnnex() {
        LOG.trace("Executing 'Remove Bill Annex' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");

        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());

        Annex deletedAnnex = annexService.findAnnex(annexId);
        int currentIndex = deletedAnnex.getMetadata().get().getIndex();

        annexService.deleteAnnex(deletedAnnex);

        String href = deletedAnnex.getName();
        bill = billService.removeAttachment(bill, href, actionMsgMap.get(ContextAction.ANNEX_DELETED));

        // Renumber remaining annexes
        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class, false);
        HashMap<String, String> attachments = new HashMap<>();
        annexes.forEach(annex -> {
            int index = annex.getMetadata().get().getIndex();
            if (index > currentIndex || annexes.size() == 1) {
                AnnexContext affectedAnnexContext = annexContextProvider.get();
                affectedAnnexContext.useAnnexId(annex.getId());
                affectedAnnexContext.useIndex(index == 1 ? index : index - 1);
                affectedAnnexContext.useActionMessageMap(actionMsgMap);
                String affectedAnnexNumber = AnnexNumberGenerator.getAnnexNumber(annexes.size() == 1 ? 0 : index - 1);
                affectedAnnexContext.useAnnexNumber(affectedAnnexNumber);
                affectedAnnexContext.executeUpdateAnnexIndex();
                attachments.put(annex.getName(), affectedAnnexNumber);
            }
        });

        billService.updateAttachments(bill, attachments, actionMsgMap.get(ContextAction.ANNEX_BLOCK_UPDATED));
    }

    public void executeCreateBillAnnex() {
        LOG.trace("Executing 'Create Bill Annex' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        Validate.notNull(bill, "Bill is required!");
        Validate.notNull(purpose, "Purpose is required!");
        AnnexContext annexContext = annexContextProvider.get();
        annexContext.usePackage(leosPackage);
        annexContext.usePurpose(purpose);
        annexContext.useTemplate(annexTemplate);
        // we are using the same template for the annexes for sj-23 and sj19, the only change is this type. that's why we get it form the bill.
        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");
        BillMetadata metadata = metadataOption.get();
        annexContext.useType(metadata.getType());
        annexContext.usePackageTemplate(metadata.getTemplate());
        // We dont need to fetch the content here, the executeUpdateAnnexMetadata gets the latest version of the annex by id
        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class, false);
        int annexIndex = annexes.size() + 1;
        String annexNumber = AnnexNumberGenerator.getAnnexNumber(annexes.size() == 0 ? annexes.size() : annexIndex);
        annexContext.useIndex(annexIndex);
        annexContext.useCollaborators(bill.getCollaborators());
        annexContext.useActionMessageMap(actionMsgMap);
        annexContext.useAnnexNumber(annexNumber);
        Annex annex = annexContext.executeCreateAnnex();

        String href = annex.getName();
        String showAs = annexNumber; //createdAnnex.getMetadata().get().getNumber(); //ShowAs attribute is not used so it is kept as blank as of now.
        bill = billService.addAttachment(bill, href, showAs, actionMsgMap.get(ContextAction.ANNEX_ADDED));

        //updating the first annex number if not done already
        Annex firstAnnex = getFirstIndexAnnex(annexes);
        if (firstAnnex != null && ANNEX_TITLE_PREFIX.equals(firstAnnex.getMetadata().get().getNumber())) {
            int firstIndex = firstAnnex.getMetadata().get().getIndex();
            annexContext.useAnnexId(firstAnnex.getId());
            annexContext.useIndex(firstIndex);
            annexContext.useCollaborators(bill.getCollaborators());
            annexContext.useActionMessageMap(actionMsgMap);
            String firstAnnexNumber = AnnexNumberGenerator.getAnnexNumber(firstIndex);
            annexContext.useAnnexNumber(firstAnnexNumber);
            annexContext.executeUpdateAnnexIndex();
            HashMap<String, String> attachmentsElements = new HashMap<>();
            attachmentsElements.put(firstAnnex.getName(), firstAnnexNumber);
            billService.updateAttachments(bill, attachmentsElements, actionMsgMap.get(ContextAction.ANNEX_BLOCK_UPDATED));
        }

    }

    public void executeImportBillAnnex() {
        LOG.trace("Executing 'Import Bill Annex' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        AnnexContext annexContext = annexContextProvider.get();
        annexContext.usePackage(leosPackage);

        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");
        BillMetadata metadata = metadataOption.get();
        annexContext.usePurpose(metadata.getPurpose());
        annexContext.useType(metadata.getType());
        annexContext.usePackageTemplate(metadata.getTemplate());

        MetadataVO annexMeta = annexDocument.getMetadata();
        annexContext.useTemplate(annexMeta.getDocTemplate());
        int annexIndex;
        if (annexMeta.getIndex() != null) {
            annexIndex = Integer.parseInt(annexMeta.getIndex());
        } else {
            // We dont need to fetch the content here, the executeUpdateAnnexMetadata gets the latest version of the annex by id
            List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class, false);
            annexIndex = annexes.size() + 1;
        }
        annexContext.useIndex(annexIndex);
        annexContext.useCollaborators(bill.getCollaborators());
        annexContext.useDocument(annexDocument);
        annexContext.useActionMessageMap(actionMsgMap);
        annexContext.useAnnexNumber(annexMeta.getNumber());
        Annex annex = annexContext.executeImportAnnex();

        String href = annex.getName();
        String showAs = annexMeta.getNumber(); //createdAnnex.getMetadata().get().getNumber(); //ShowAs attribute is not used so it is kept as blank as of now.
        billService.addAttachment(bill, href, showAs, actionMsgMap.get(ContextAction.ANNEX_ADDED));
    }

    public void executeMoveAnnex() {
        LOG.trace("Executing 'Update Bill Move Annex' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        Validate.notNull(moveDirection, "Bill moveDirection is required");
        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());
        Annex operatedAnnex = annexService.findAnnex(annexId);
        int currentIndex = operatedAnnex.getMetadata().get().getIndex();
        Annex affectedAnnex = findAffectedAnnex(moveDirection.equalsIgnoreCase("UP"), currentIndex);

        AnnexContext operatedAnnexContext = annexContextProvider.get();
        operatedAnnexContext.useAnnexId(operatedAnnex.getId());
        operatedAnnexContext.useIndex(affectedAnnex.getMetadata().get().getIndex());
        operatedAnnexContext.useActionMessageMap(actionMsgMap);
        String operatedAnnexNumber = AnnexNumberGenerator.getAnnexNumber(affectedAnnex.getMetadata().get().getIndex());
        operatedAnnexContext.useAnnexNumber(operatedAnnexNumber);
        operatedAnnexContext.executeUpdateAnnexIndex();

        AnnexContext affectedAnnexContext = annexContextProvider.get();
        affectedAnnexContext.useAnnexId(affectedAnnex.getId());
        affectedAnnexContext.useIndex(currentIndex);
        affectedAnnexContext.useActionMessageMap(actionMsgMap);
        String affectedAnnexNumber = AnnexNumberGenerator.getAnnexNumber(currentIndex);
        affectedAnnexContext.useAnnexNumber(affectedAnnexNumber);
        affectedAnnexContext.executeUpdateAnnexIndex();

        //Update bill xml
        HashMap<String, String> attachments = new HashMap<>();
        attachments.put(operatedAnnex.getName(), operatedAnnexNumber);
        attachments.put(affectedAnnex.getName(), affectedAnnexNumber);

        billService.updateAttachments(bill, attachments, actionMsgMap.get(ContextAction.ANNEX_BLOCK_UPDATED));
    }

    private Annex findAffectedAnnex(boolean before, int index) {
        Validate.notNull(leosPackage, "Bill package is required!");
        // We dont need to fetch the content here, the executeUpdateAnnexMetadata gets the latest version of the annex by id
        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class, false);
        int targetIndex = index + (before ? -1 : 1); //index start with 0
        if (targetIndex < 0 || targetIndex > annexes.size()) {
            throw new UnsupportedOperationException("Invalid index requested");
        }

        for (Annex annex : annexes) {//assuming unsorted annex list
            annex = annexService.findAnnex(annex.getId());
            if (annex.getMetadata().get().getIndex() == targetIndex) {
                return annex;
            }
        }
        throw new UnsupportedOperationException("Invalid index for annex");
    }


    /**
     * @param list of Annexes currently added
     * @return result if first Annex is numbered or not
     */
    private Annex getFirstIndexAnnex(List<Annex> annexes) {
        Annex firstAnnex = null;
        for (Annex annex : annexes) {
            if (annex.getMetadata().get().getIndex() == 1) {
                firstAnnex = annex;
            }
        }
        return firstAnnex;
    }

    public void executeCreateMilestone() {
        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());
        List<String> milestoneComments = bill.getMilestoneComments();
        milestoneComments.add(milestoneComment);
        if (bill.getVersionType().equals(VersionType.MAJOR)) {
            bill = billService.updateBillWithMilestoneComments(bill.getId(), milestoneComments);
            LOG.info("Major version {} already present. Updated only milestoneComment for [bill={}]", bill.getVersionLabel(), bill.getId());
        } else {
            bill = billService.updateBillWithMilestoneComments(bill, milestoneComments, VersionType.MAJOR, versionComment);
            LOG.info("Created major version {} for [bill={}]", bill.getVersionLabel(), bill.getId());
        }

        final List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class, false);
        annexes.forEach(annex -> {
            AnnexContext annexContext = annexContextProvider.get();
            annexContext.useAnnexId(annex.getId());
            annexContext.useVersionComment(versionComment);
            annexContext.useMilestoneComment(milestoneComment);
            annexContext.executeCreateMilestone();
        });
    }
    
    public String getProposalIdFromBill() {
        Proposal proposal = proposalService.findProposalByPackagePath(leosPackage.getPath());
        return proposal != null ? proposal.getId() : null;
    }
    
    public String getUpdatedBillId() {
        return bill.getId();
    }
}
