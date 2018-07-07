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
package eu.europa.ec.leos.usecases.document;

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.domain.document.LeosPackage;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import eu.europa.ec.leos.services.store.PackageService;
import io.atlassian.fugue.Option;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Provider;
import java.util.List;

@Component
@Scope("prototype")
public class BillContext {

    private static final Logger LOG = LoggerFactory.getLogger(BillContext.class);

    private final BillService billService;
    private final PackageService packageService;
    private final AnnexService annexService;

    private final Provider<AnnexContext> annexContextProvider;

    private LeosPackage leosPackage = null;
    private Bill bill = null;
    private String purpose = null;
    private String moveDirection = null;
    private String annexId;

    BillContext(BillService billService,
            PackageService packageService,
            AnnexService annexService,
            Provider<AnnexContext> annexContextProvider) {
        this.billService = billService;
        this.packageService = packageService;
        this.annexService = annexService;
        this.annexContextProvider = annexContextProvider;
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

    public Bill executeCreateBill() {
        LOG.trace("Executing 'Create Bill' use case...");
        Validate.notNull(leosPackage, "Bill package is required!");
        Validate.notNull(bill, "Bill template is required!");

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");

        Validate.notNull(purpose, "Bill purpose is required!");
        BillMetadata metadata = metadataOption.get().withPurpose(purpose);

        return billService.createBill(bill.getId(), leosPackage.getPath(), metadata);
    }

    public void executeUpdateBill() {
        LOG.trace("Executing 'Update Bill' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");
        Validate.notNull(purpose, "Bill purpose is required!");
        BillMetadata metadata = metadataOption.get().withPurpose(purpose);
        billService.updateBill(bill, metadata);

        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class);
        annexes.forEach(annex -> {
            AnnexContext annexContext = annexContextProvider.get();
            annexContext.usePurpose(purpose);
            annexContext.useAnnexId(annex.getId());
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
        billService.removeAttachment(bill, href);

        // Renumber remaining annexes
        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class);
        annexes.forEach(annex -> {
            if (annex.getMetadata().get().getIndex() > currentIndex) {
                AnnexContext affectedAnnexContext = annexContextProvider.get();
                affectedAnnexContext.useAnnexId(annex.getId());
                affectedAnnexContext.useIndex(annex.getMetadata().get().getIndex()-1);
                affectedAnnexContext.executeUpdateAnnexIndex();
            }
        });
    }

    public void executeCreateBillAnnex() {
        LOG.trace("Executing 'Create Bill Annex' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        AnnexContext annexContext = annexContextProvider.get();
        annexContext.usePackage(leosPackage);

        Bill bill = billService.findBillByPackagePath(leosPackage.getPath());
        annexContext.useTemplate(bill.getTemplate());

        Option<BillMetadata> metadataOption = bill.getMetadata();
        Validate.isTrue(metadataOption.isDefined(), "Bill metadata is required!");
        BillMetadata metadata = metadataOption.get();
        annexContext.usePurpose(metadata.getPurpose());

        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class);
        int annexIndex = annexes.size() + 1;
        annexContext.useIndex(annexIndex);
        annexContext.useCollaborators(bill.getCollaborators());

        Annex annex = annexContext.executeCreateAnnex();

        String href = annex.getName();
        String showAs = ""; //createdAnnex.getMetadata().get().getNumber(); //ShowAs attribute is not used so it is kept as blank as of now.
        billService.addAttachment(bill, href, showAs);
    }

    public void executeMoveAnnex() {
        LOG.trace("Executing 'Update Bill Move Annex' use case...");

        Validate.notNull(leosPackage, "Bill package is required!");
        Validate.notNull(moveDirection, "Bill moveDirection is required");

        Annex operatedAnnex = annexService.findAnnex(annexId);
        int currentIndex = operatedAnnex.getMetadata().get().getIndex();
        Annex affectedAnnex = findAffectedAnnex(moveDirection.equalsIgnoreCase("UP"), currentIndex);

        AnnexContext operatedAnnexContext = annexContextProvider.get();
        operatedAnnexContext.useAnnexId(operatedAnnex.getId());
        operatedAnnexContext.useIndex(affectedAnnex.getMetadata().get().getIndex());
        operatedAnnexContext.executeUpdateAnnexIndex();

        AnnexContext affectedAnnexContext = annexContextProvider.get();
        affectedAnnexContext.useAnnexId(affectedAnnex.getId());
        affectedAnnexContext.useIndex(currentIndex);
        affectedAnnexContext.executeUpdateAnnexIndex();

        //TODO: Update bill xml ??
    }

    private Annex findAffectedAnnex(boolean before, int index) {
        Validate.notNull(leosPackage, "Bill package is required!");
        List<Annex> annexes = packageService.findDocumentsByPackagePath(leosPackage.getPath(), Annex.class);
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
}
