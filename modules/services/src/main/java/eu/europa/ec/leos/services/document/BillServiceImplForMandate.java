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
package eu.europa.ec.leos.services.document;

import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.metadata.BillMetadata;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.model.user.User;
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
import eu.europa.ec.leos.vo.toctype.LegalTextMandateTocItemType;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Instance(InstanceType.COUNCIL)
public class BillServiceImplForMandate extends BillServiceImpl {

    private static final Logger LOG = LoggerFactory.getLogger(BillServiceImplForMandate.class);

    BillServiceImplForMandate(BillRepository billRepository, PackageRepository packageRepository,
                    XmlNodeProcessor xmlNodeProcessor, XmlContentProcessor xmlContentProcessor,
                    XmlNodeConfigHelper xmlNodeConfigHelper, AttachmentProcessor attachmentProcessor,
                    ValidationService validationService, DocumentVOProvider documentVOProvider, NumberProcessor numberingProcessor) {
        
        super(billRepository, packageRepository, xmlNodeProcessor, xmlContentProcessor, xmlNodeConfigHelper, attachmentProcessor,
                validationService, documentVOProvider, numberingProcessor);
    }

    @Override
    public List<TableOfContentItemVO> getTableOfContent(Bill bill, boolean simplified) {
        Validate.notNull(bill, "Bill is required");
        return xmlContentProcessor.buildTableOfContent("bill", LegalTextMandateTocItemType::getTocItemTypeFromName, getContent(bill), simplified);
    }

    @Override
    public Bill saveTableOfContent(Bill bill, List<TableOfContentItemVO> tocList, String actionMsg, User user) {
        Validate.notNull(bill, "Bill is required");
        Validate.notNull(tocList, "Table of content list is required");
        final BillMetadata metadata = bill.getMetadata().getOrError(() -> "Document metadata is required!");

        byte[] newXmlContent;
        newXmlContent = xmlContentProcessor.createDocumentContentWithNewTocList(LegalTextMandateTocItemType::getTocItemTypeFromName, tocList, getContent(bill), user);

        newXmlContent = numberingProcessor.renumberArticles(newXmlContent, metadata.getLanguage());
        newXmlContent = numberingProcessor.renumberRecitals(newXmlContent);
        newXmlContent = xmlContentProcessor.doXMLPostProcessing(newXmlContent);

        return updateBill(bill, newXmlContent, actionMsg);
    }

}
