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
package eu.europa.ec.leos.ui.view.document;

import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.web.model.VersionInfoVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface DocumentScreen {

    void setDocumentTitle(final String documentTitle);
    
    void setDocumentVersionInfo(VersionInfoVO versionInfoVO);

    void refreshContent(final String documentContent);

    void populateMarkedContent(final String diffContent);

    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);

    void showElementEditor(String elementId, String elementTagName, String elementContent);

    void refreshElementEditor(String elementId, String elementTagName, String elementContent);

    void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList,
                           Map<TocItemType, List<TocItemType>> tableOfContentRules);

    void showTimeLineWindow(List documentVersions);
    
    void showMajorVersionWindow();
    
    void showImportWindow();
    
    void displayComparison(HashMap<Integer, Object> htmlCompareResult);

    void setTocAndAncestors(List<TableOfContentItemVO> tocItemList, String elementId, List<String> elementAncestorsIds);

    void setElement(String elementId, String elementTagName, String elementContent);

    void setUserGuidance(String jsonGuidance);
    
    void displaySearchedContent(String content);
    
    void closeImportWindow();
}
