/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.web.view;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface DocumentView extends LeosView {

    String VIEW_ID = "document";

    void setDocumentTitle(final String documentTitle);

    void setDocumentStage(final LeosDocumentProperties.Stage stage);

    void refreshContent(final String documentContent);

    void populateMarkedContent(final String diffContent);

    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);

    void showElementEditor(String elementId, String elementTagName, String elementContent);

    void refreshElementEditor(String elementId, String elementTagName, String elementContent);

    void showMetadataEditWindow(MetaDataVO metaDataVO);

    void showDownloadWindow(LeosDocument docContent, String msgKey);

    void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList,
            Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules);

    void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL);

    void showVersionListWindow(List<LeosDocumentProperties> versions);

    void showVersionCompareWindow(LeosDocumentProperties oldVersion, LeosDocumentProperties newVersion);

    void displayComparison(HashMap<Integer, Object> htmlCompareResult);

    void updateLocks(LockActionInfo lockActionInfo);

    void setTocAndAncestors(List<TableOfContentItemVO> tocItemList, String elementId, List<String> elementAncestorsIds);

    void setElement(String elementId, String elementTagName, String elementContent);
}