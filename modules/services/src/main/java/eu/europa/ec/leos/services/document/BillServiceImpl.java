/*
 * Copyright 2018 European Commission
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
import eu.europa.ec.leos.services.document.util.DocumentVOProvider;
import eu.europa.ec.leos.services.support.xml.NumberProcessor;
import eu.europa.ec.leos.services.support.xml.XmlContentProcessor;
import eu.europa.ec.leos.services.support.xml.XmlNodeConfigHelper;
import eu.europa.ec.leos.services.support.xml.XmlNodeProcessor;
import eu.europa.ec.leos.services.validation.ValidationService;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.LegalTextTocItemType;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final ValidationService validationService;
    private final DocumentVOProvider documentVOProvider;
    private final NumberProcessor numberingProcessor;

    BillServiceImpl(BillRepository billRepository, PackageRepository packageRepository,
                    XmlNodeProcessor xmlNodeProcessor, XmlContentProcessor xmlContentProcessor,
                    XmlNodeConfigHelper xmlNodeConfigHelper, AttachmentProcessor attachmentProcessor,
                    ValidationService validationService, DocumentVOProvider documentVOProvider, NumberProcessor numberingProcessor) {
        this.billRepository = billRepository;
        this.packageRepository = packageRepository;
        this.xmlNodeProcessor = xmlNodeProcessor;
        this.xmlContentProcessor = xmlContentProcessor;
        this.xmlNodeConfigHelper = xmlNodeConfigHelper;
        this.attachmentProcessor = attachmentProcessor;
        this.validationService = validationService;
        this.documentVOProvider = documentVOProvider;
        this.numberingProcessor = numberingProcessor;
    }

    @Override
    public Bill createBill(String templateId, String path, BillMetadata metadata, String actionMsg, byte[] content) {
        LOG.trace("Creating Bill... [templateId={}, path={}, metadata={}]", templateId, path, metadata);
        String name = generateBillName();
        metadata = metadata.withRef(name);//FIXME: a better scheme needs to be devised
        Bill bill = billRepository.createBill(templateId, path, name, metadata);
        byte[] updatedBytes = updateDataInXml((content==null)? getContent(bill) : content, metadata);
        return billRepository.updateBill(bill.getId(), metadata, updatedBytes, false, actionMsg);
    }

    @Override
    public Bill findBill(String id) {
        LOG.trace("Finding Bill... [it={}]", id);
        return billRepository.findBillById(id, true);
    }

    @Override
    @Cacheable(value="docVersions", cacheManager = "cacheManager")
    public Bill findBillVersion(String id) {
        LOG.trace("Finding Bill version... [it={}]", id);
        return billRepository.findBillById(id, false);
    }

    @Override
    public Bill findBillByPackagePath(String path) {
        LOG.trace("Finding Bill by package path... [path={}]", path);
        // FIXME can be improved, now we dont fetch ALL docs because it's loaded later the one needed, 
        // this can be improved adding a page of 1 item or changing the method/query.
        List<Bill> docs = packageRepository.findDocumentsByPackagePath(path, Bill.class,false);
        Bill bill = findBill(docs.get(0).getId());
        return bill;
    }

    @Override
    public Bill updateBill(Bill bill, byte[] updatedBillContent, String comments) {
        LOG.trace("Updating Bill Xml Content... [id={}]", bill.getId());
        final BillMetadata metadata = bill.getMetadata().getOrError(() -> "Bill metadata is required!");
        bill = billRepository.updateBill(bill.getId(), metadata, updatedBillContent, false, comments); 
        
        //call validation on document with updated content
        validationService.validateDocumentAsync(documentVOProvider.createDocumentVO(bill, updatedBillContent));
        
        return bill;
    }

    @Override
    public Bill updateBill(Bill bill, BillMetadata updatedMetadata, String actionMsg) {
        LOG.trace("Updating Bill... [id={}, updatedMetadata={}]", bill.getId(), updatedMetadata);
        Stopwatch stopwatch = Stopwatch.createStarted();
        byte[] updatedBytes = updateDataInXml(getContent(bill), updatedMetadata); //FIXME: Do we need latest data again??
        
        bill = billRepository.updateBill(bill.getId(), updatedMetadata, updatedBytes, false, actionMsg);
        
        //call validation on document with updated content
        validationService.validateDocumentAsync(documentVOProvider.createDocumentVO(bill, updatedBytes));
        
        LOG.trace("Updated Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bill;
    }

    @Override
    public Bill addAttachment(Bill bill, String href, String showAs, String actionMsg) {
        LOG.trace("Add attachment in bill ... [id={}, href={}]", bill.getId(), href);
        Stopwatch stopwatch = Stopwatch.createStarted();

        //Do the xml update
        byte[] xmlBytes = getContent(bill);
        byte[] updatedBytes = attachmentProcessor.addAttachmentInBill(xmlBytes, href, showAs);

        //save updated xml
        bill = billRepository.updateBill(bill.getId(), bill.getMetadata().get(), updatedBytes, false, actionMsg);

        LOG.trace("Added attachment in Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bill;
    }

    @Override
    public Bill removeAttachment(Bill bill, String href, String actionMsg) {
        LOG.trace("Remove attachment from bill ... [id={}, href={}]", bill.getId(), href);
        Stopwatch stopwatch = Stopwatch.createStarted();

        //Do the xml update
        byte[] xmlBytes = getContent(bill);
        byte[] updatedBytes = attachmentProcessor.removeAttachmentFromBill(xmlBytes, href);

        //save updated xml
        bill = billRepository.updateBill(bill.getId(), bill.getMetadata().get(), updatedBytes, false, actionMsg);

        LOG.trace("Removed attachment from Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        return bill;
    }

    @Override
    public Bill updateAttachments(Bill bill, HashMap<String, String> attachmentsElements, String actionMsg) {
        LOG.trace("Update attachments in bill ... [id={}]", bill.getId());
        Stopwatch stopwatch = Stopwatch.createStarted();

        //Do the xml update
        byte[] xmlBytes = getContent(bill);
        byte[] updatedBytes = attachmentProcessor.updateAttachmentsInBill(xmlBytes, attachmentsElements);

        //save updated xml
        bill = billRepository.updateBill(bill.getId(), bill.getMetadata().get(), updatedBytes, false, actionMsg);

        LOG.trace("Update attachments in Bill ...({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));
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
        //LEOS-2813 We have memory issues is we fetch the content of all versions.
        return billRepository.findBillVersions(id, false);
    }

    @Override
    public List<TableOfContentItemVO> getTableOfContent(Bill bill) {
        Validate.notNull(bill, "Bill is required");
        return xmlContentProcessor.buildTableOfContent("bill", LegalTextTocItemType::getTocItemTypeFromName, getContent(bill));
    }

    @Override
    public Bill saveTableOfContent(Bill bill, List<TableOfContentItemVO> tocList, String actionMsg) {
        Validate.notNull(bill, "Bill is required");
        Validate.notNull(tocList, "Table of content list is required");
        final BillMetadata metadata = bill.getMetadata().getOrError(() -> "Document metadata is required!");

        byte[] newXmlContent;

        newXmlContent = xmlContentProcessor.createDocumentContentWithNewTocList(LegalTextTocItemType::getTocItemTypeFromName, LegalTextTocItemType.BODY, tocList, getContent(bill));
        // TODO validate bytearray for being valid xml/AKN content

        newXmlContent = numberingProcessor.renumberArticles(newXmlContent, metadata.getLanguage());
        newXmlContent = xmlContentProcessor.doXMLPostProcessing(newXmlContent);

        return updateBill(bill, newXmlContent, actionMsg);
    }

    @Override
    public byte[] searchAndReplaceText(byte[] xmlContent, String searchText, String replaceText) {
        return xmlContentProcessor.searchAndReplaceText(xmlContent, searchText, replaceText);
    }

    @Override
    public List<String> getAncestorsIdsForElementId(Bill bill, List<String> elementIds) {
        Validate.notNull(bill, "Bill is required");
        Validate.notNull(elementIds, "Element id is required");
        List<String> ancestorIds = new ArrayList<String>();
        for(String elementId : elementIds) {
            ancestorIds.addAll(xmlContentProcessor.getAncestorsIdsForElementId(
                getContent(bill),
                elementId));
        }
        return ancestorIds;
    }

    private byte[] updateDataInXml(final byte[] content, BillMetadata dataObject) {
        byte[] updatedBytes = xmlNodeProcessor.setValuesInXml(content, createValueMap(dataObject), xmlNodeConfigHelper.getConfig(dataObject.getCategory()));
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
