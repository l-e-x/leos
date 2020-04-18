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
package eu.europa.ec.leos.services.content.processor;

import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.InstanceType;
import eu.europa.ec.leos.instance.Instance;
import eu.europa.ec.leos.security.SecurityContext;
import eu.europa.ec.leos.services.compare.ContentComparatorContext;
import eu.europa.ec.leos.services.compare.ContentComparatorService;
import eu.europa.ec.leos.services.document.AnnexService;
import eu.europa.ec.leos.services.document.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

import static eu.europa.ec.leos.services.compare.ContentComparatorService.*;

@Service
@Instance(InstanceType.COUNCIL)
public class MandateDocumentContentServiceImpl implements DocumentContentService {

    private TransformationService transformationService;
    private ContentComparatorService compareService;
    private AnnexService annexService;
    private BillService billService;

    @Autowired
    public MandateDocumentContentServiceImpl(TransformationService transformationService,
            ContentComparatorService compareService, AnnexService annexService, BillService billService) {

        this.transformationService = transformationService;
        this.compareService = compareService;
        this.annexService = annexService;
        this.billService = billService;
    }

    @Override
    public String toEditableContent(XmlDocument xmlDocument, String contextPath, SecurityContext securityContext) {
        String currentDocumentEditableXml = transformationService.toEditableXml(getContentInputStream(xmlDocument), contextPath, xmlDocument.getCategory(), securityContext.getPermissions(xmlDocument));
        XmlDocument originalDocument;

        switch (xmlDocument.getCategory()){
            case MEMORANDUM:
                return currentDocumentEditableXml;
            case ANNEX:
                originalDocument = getOriginalAnnex(xmlDocument);
                break;
            case BILL:
                originalDocument = getOriginalBill(xmlDocument);
                break;
            default:
                throw new UnsupportedOperationException("No transformation supported for this category");
        }

        String originalDocumentEditableXml = transformationService.toEditableXml(getContentInputStream(originalDocument), contextPath, originalDocument.getCategory(), securityContext.getPermissions(originalDocument));
        return compareService.compareContents(new ContentComparatorContext.Builder(originalDocumentEditableXml, currentDocumentEditableXml)
                .withAttrName(ATTR_NAME)
                .withRemovedValue(CONTENT_SOFT_REMOVED_CLASS)
                .withAddedValue(CONTENT_SOFT_ADDED_CLASS)
                .withDisplayRemovedContentAsReadOnly(Boolean.TRUE)
                .build());
    }

    private XmlDocument getOriginalAnnex(XmlDocument xmlDocument) {
        List<Annex> annexVersions = annexService.findVersions(xmlDocument.getId());
        annexVersions.sort(Comparator.comparing(Annex::getCreationInstant));
        return annexVersions.isEmpty() ? xmlDocument : annexService.findAnnexVersion(annexVersions.get(0).getId());
    }

    private XmlDocument getOriginalBill(XmlDocument xmlDocument) {
        List<Bill> billVersions = billService.findVersions(xmlDocument.getId());
        billVersions.sort(Comparator.comparing(Bill::getCreationInstant));
        return billVersions.isEmpty() ? xmlDocument : billService.findBillVersion(billVersions.get(0).getId());
    }
}
