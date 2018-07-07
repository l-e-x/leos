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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.vo.TableOfContentItemVO;

import java.util.List;

public interface BillService {

    Bill createBill(String templateId, String path, BillMetadata metadata);

    Bill findBill(String id);

    // FIXME temporary workaround
    Bill findBillByPackagePath(String path);

    Bill updateBill(Bill bill, BillMetadata metadata);

    Bill updateBill(Bill bill, byte[] updatedBillContent, String comments);

    Bill addAttachment(Bill bill, String href, String showAs);

    Bill removeAttachment(Bill bill, String href);

    Bill createVersion(String id, boolean major, String comment);

    List<Bill> findVersions(String id);

    List<TableOfContentItemVO> getTableOfContent(Bill bill);

    Bill saveTableOfContent(Bill bill, List<TableOfContentItemVO> tocList);

    byte[] searchAndReplaceText(byte[] xmlContent, String searchText, String replaceText);

    List<String> getAncestorsIdsForElementId(Bill bill, String elementId);
}
