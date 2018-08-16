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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Bill;
import eu.europa.ec.leos.domain.document.LeosMetadata.BillMetadata;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.HashMap;
import java.util.List;

public interface BillService {

    Bill createBill(String templateId, String path, BillMetadata metadata, String actionMsg, byte[] content);

    Bill findBill(String id);

    Bill findBillVersion(String id);

    // FIXME temporary workaround
    Bill findBillByPackagePath(String path);

    Bill updateBill(Bill bill, BillMetadata metadata, String actionMsg);

    Bill updateBill(Bill bill, byte[] updatedBillContent, String comments);

    Bill addAttachment(Bill bill, String href, String showAs, String actionMsg);

    Bill removeAttachment(Bill bill, String href, String actionMsg);

    Bill updateAttachments(Bill bill, HashMap<String, String> attachmentsElements, String actionMsg);

    Bill createVersion(String id, boolean major, String comment);

    List<Bill> findVersions(String id);

    List<TableOfContentItemVO> getTableOfContent(Bill bill);

    Bill saveTableOfContent(Bill bill, List<TableOfContentItemVO> tocList, String actionMsg);

    byte[] searchAndReplaceText(byte[] xmlContent, String searchText, String replaceText);

    List<String> getAncestorsIdsForElementId(Bill bill, List<String> elementIds);
}
