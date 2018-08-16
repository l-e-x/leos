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
package eu.europa.ec.leos.ui.view.annex;

import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.vo.toctype.TocItemType;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.web.model.VersionInfoVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface AnnexScreen {
    void setTitle(String title);
    void setContent(String content);
    void refreshElementEditor(String elementId, String elementTagName, String elementContent);
    void showElementEditor(String elementId, String elementTagName, String element);
    void populateMarkedContent(String markedContent);
    void showTimeLineWindow(List documentVersions);
    void displayComparison(HashMap<Integer, Object> htmlCompareResult);
    void showMajorVersionWindow();
    void setDocumentVersionInfo(VersionInfoVO versionInfoVO);
    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);
    void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList,
            Map<TocItemType, List<TocItemType>> tableOfContentRules);
    void setPermissions(DocumentVO annex);
    void scrollToMarkedChange(final String elementId);
    void sendUserPermissions(List<LeosPermission> userPermissions);
}
