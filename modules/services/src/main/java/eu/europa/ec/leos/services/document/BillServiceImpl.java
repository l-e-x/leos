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
package eu.europa.ec.leos.services.document;

import com.google.common.base.Stopwatch;
import cool.graph.cuid.Cuid;
import eu.europa.ec.leos.domain.document.Content;
import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.repository.document.BillRepository;
import eu.europa.ec.leos.repository.store.PackageRepository;
import eu.europa.ec.leos.services.content.processor.AttachmentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextTocItemType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper.createValueMap;

@Service
public class BillServiceImpl implements BillService {

    private static final Logger LOG = LoggerFactory.getLogger(BillServiceImpl.class);
    private static final String BILL_NAME_PREFIX = "bill_";

    private final BillRepository billRepository;
    private final PackageRepository packageRepository;
    private final XmlNodeProcessor xmlNodeProcessor;
    private final XmlContentProcessor xmlContentProcessor;
    private final XmlNodeConfigHelper xmlNodeConfigHelper;
    private final AttachmentProcessor attachmentProcessor;

    BillServiceImpl(BillRepository billRepository,
                    PackageRepository packageRepository,
                    XmlNodeProcessor xmlNodeProcessor,
                    XmlContentProcessor xmlContentProcessor,
                    XmlNodeConfigHelper xmlNodeConfigHelper,
                    AttachmentProcessor attachmentProcessor) {
        this.billRepository = billRepository;
        this.packageRepository = packageRepository;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlContentProcessor = xmlContentProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.attachmentProcessor = attachmentProcessor;
    }

    @Override
    public Bill createBill(String templateId, String path, BillMetadata metadata) {
        LOG.trace("Creating Bill... [templateId={}, path={}, metadata={}]", templateId, path, metadata);
        String name = generateBillName();
        Bill bill = billRepository.createBill(templateId, path, name, metadata);
        byte[] updatedBytes = updateDataInXml(bill, metadata);
        return billRepository.updateBill(bill.getId(), metadata, updatedBytes, false, "Metadata updated.");
    }

    @Override
    public Bill findBill(String id) {
        LOG.trace("Finding Bill... [it={}]", id);
        return billRepository.findBillById(id, true);
    }

    @Override
    public Bill findBillByPackagePath(String path) {
        LOG.trace("Finding Bill by package path... [path={}]", path);
        // FIXME temporary workaround
        List<Bill> docs = packageRepository.findDocumentsByPackagePath(path, Bill.class);
        Bill bill = findBill(docs.get(0).getId());
        return bill;
    }

    @Override
    public Bill updateBill(Bill bill, byte[] updatedBillContent, String comments) {
        LOG.trace("Updating Bill Xml Content... [id={}]", bill.getId());
        final BillMetadata metadata = bill.getMetadata().getOrError(() -> "Bill metadata is required!");
        return billRepository.updateBill(bill.getId(), metadata, updatedBillContent, false, comments);
    }

    @Override
    public Bill updateBill(Bill bill, BillMetadata updatedMetadata) {
        LOG.trace("Updating Bill... [id={}, updatedMetadata={}]", bill.getId(), updatedMetadata);
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] updatedBytes = updateDataInXml(bill, updatedMetadata); //FIXME: Do we need latest data again??
        bill = billRepository.updateBill(bill.getId(), updatedMetadata, updatedBytes, false, "Metadata updated.");
        LOG.trace("Updated Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bill;
    }

    @Override
    public Bill addAttachment(Bill bill, String href, String showAs) {
        LOG.trace("Add attachment in bill ... [id={}, href={}]", bill.getId(), href);
        Stopwatch stopwatch = Stopwatch.createStarted();

        //Do the xml update
        byte[] xmlBytes = getContent(bill);
        byte[] updatedBytes = attachmentProcessor.addAttachmentInBill(xmlBytes, href, showAs);

        //save updated xml
        bill = billRepository.updateBill(bill.getId(), bill.getMetadata().get(), updatedBytes, false, "Attachment added.");

        LOG.trace("Added attachment in Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bill;
    }

    @Override
    public Bill removeAttachment(Bill bill, String href) {
        LOG.trace("Remove attachment from bill ... [id={}, href={}]", bill.getId(), href);
        Stopwatch stopwatch = Stopwatch.createStarted();

        //Do the xml update
        byte[] xmlBytes = getContent(bill);
        byte[] updatedBytes = attachmentProcessor.removeAttachmentFromBill(xmlBytes, href);

        //save updated xml
        bill = billRepository.updateBill(bill.getId(), bill.getMetadata().get(), updatedBytes, false, "Attachment removed.");

        LOG.trace("Removed attachment from Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bill;
    }

    @Override
    public Bill createVersion(String id, boolean major, String comment) {
        LOG.trace("Creating Bill version... [id={}, major={}, comment={}]", id, major, comment);
        final Bill bill = findBill(id);
        final BillMetadata metadata = bill.getMetadata().getOrError(() -> "Bill metadata is required!");
        final Content content = bill.getContent().getOrError(() -> "Bill content is required!");
        final byte[] contentBytes = content.getSource().getByteString().toByteArray();
        return billRepository.updateBill(id, metadata, contentBytes, major, comment);
    }

    @Override
    public List<Bill> findVersions(String id) {
        LOG.trace("Finding Bill versions... [id={}]", id);
        return billRepository.findBillVersions(id);
    }

    @Override
    public List<TableOfContentItemVO> getTableOfContent(Bill bill) {
        Validate.notNull(bill, "Bill is required");
        return xmlContentProcessor.buildTableOfContent("bill", LegalTextTocItemType::getTocItemTypeFromName, getContent(bill));
    }

    @Override
    public Bill saveTableOfContent(Bill bill, List<TableOfContentItemVO> tocList) {
        Validate.notNull(bill, "Bill is required");
        Validate.notNull(tocList, "Table of content list is required");

        byte[] newXmlContent;
        newXmlContent = xmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, tocList, getContent(bill));
        // TODO validate bytearray for being valid xml/AKN content

        newXmlContent = xmlContentProcessor.renumberArticles(newXmlContent, bill.getLanguage());

        return updateBill(bill, newXmlContent, "operation.toc.updated");
    }

    @Override
    public byte[] searchAndReplaceText(byte[] xmlContent, String searchText, String replaceText) {
        return xmlContentProcessor.searchAndReplaceText(xmlContent, searchText, replaceText);
    }

    @Override
    public List<String> getAncestorsIdsForElementId(Bill bill, String elementId) {
        Validate.notNull(bill, "Bill is required");
        Validate.notNull(elementId, "Element id is required");

        return xmlContentProcessor.getAncestorsIdsForElementId(
                getContent(bill),
                elementId);
    }

    private byte[] updateDataInXml(Bill bill, BillMetadata dataObject) {
        byte[] xmlBytes = getContent(bill);
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(xmlBytes, createValueMap(dataObject), xmlNodeConfigHelper.getConfig(bill.getCategory()));
        return xmlContentProcessor.doXMLPostProcessing(updatedBytes);
    }

    private String generateBillName() {
        return BILL_NAME_PREFIX + Cuid.createCuid();
    }

    private byte[] getContent(Bill bill) {
        final Content content = bill.getContent().getOrError(() -> "Bill content is required!");
        return content.getSource().getByteString().toByteArray();
    }
}
