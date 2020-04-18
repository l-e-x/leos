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
package eu.europa.ec.leos.ui.view.document;


import com.vaadin.server.Resource;
import eu.europa.ec.leos.domain.cmis.document.Bill;
import eu.europa.ec.leos.domain.cmis.document.LegDocument;
import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.ui.view.ComparisonDisplayMode;
import eu.europa.ec.leos.ui.view.TriFunction;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent.Action;
import eu.europa.ec.leos.web.model.VersionInfoVO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

interface DocumentScreen {

    void setDocumentTitle(final String documentTitle);

    void setDocumentVersionInfo(VersionInfoVO versionInfoVO);

    void refreshContent(final String documentContent);

    void populateMarkedContent(String comparedContent, String comparedInfo);

    void populateDoubleComparisonContent(String comparedContent,  String comparedInfo);

    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);
    
    void showElementEditor(String elementId, String elementTagName, String elementContent, String alternatives);

    void refreshElementEditor(String elementId, String elementTagName, String elementContent);
    
    void enableTocEdition(List<TableOfContentItemVO> tableOfContentItemVoList);

    void showTimeLineWindow(List documentVersions);

    void updateTimeLineWindow(List documentVersions);

    void showIntermediateVersionWindow();

    void showImportWindow();

    void displayComparison(HashMap<ComparisonDisplayMode, Object> htmlCompareResult);

    void setTocAndAncestors(Map<String, List<TableOfContentItemVO>> tocItemList, List<String> elementAncestorsIds);

    void setElement(String elementId, String elementTagName, String elementContent, String documentRef);

    void setUserGuidance(String jsonGuidance);

    void sendUserPermissions(List<LeosPermission> userPermissions);

    void displaySearchedContent(String content);

    void closeImportWindow();

    void setPermissions(DocumentVO bill);

    void scrollToMarkedChange(final String elementId);

    void scrollTo(final String elementId);

    void setReferenceLabel(String referenceLabels, String documentRef);

    void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId);

    void displayDocumentUpdatedByCoEditorWarning();

    void checkElementCoEdition(List<CoEditionVO> coEditionVos, User user, final String elementId, final String elementTagName, final Action action, final Object actionEvent);

    void showAlertDialog(String messageKey);
    
    void setDownloadStreamResource(Resource downloadstreamResource);

    boolean isTocEnabled();
    
    void setDataFunctions(List<VersionVO> allVersions,
                          BiFunction<Integer, Integer, List<Bill>> majorVersionsFn,
                          Supplier<Integer> countMajorVersionsFn,
                          TriFunction<String, Integer, Integer, List<Bill>> minorVersionsFn,
                          Function<String, Integer> countMinorVersionsFn,
                          BiFunction<Integer, Integer, List<Bill>> recentChangesFn,
                          Supplier<Integer> countRecentChangesFn);
    
    void refreshVersions(List<VersionVO> allVersions, boolean isComparisonMode);
    
    void showVersion(String content, String versionInfo);

    void showMilestoneExplorer(LegDocument legDocument, String milestoneTitle);
    
    void cleanComparedContent();

    boolean isComparisonComponentVisible();
}
