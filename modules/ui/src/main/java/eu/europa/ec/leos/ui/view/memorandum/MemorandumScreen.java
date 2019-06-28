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
package eu.europa.ec.leos.ui.view.memorandum;

import eu.europa.ec.leos.domain.vo.DocumentVO;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.LeosPermission;
import eu.europa.ec.leos.vo.coedition.CoEditionVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;
import eu.europa.ec.leos.web.event.view.document.CheckElementCoEditionEvent.Action;
import eu.europa.ec.leos.web.model.VersionInfoVO;

import java.util.HashMap;
import java.util.List;

interface MemorandumScreen {
    void setTitle(String title);

    void setContent(String content);

    void refreshElementEditor(String elementId, String elementTagName, String elementContent);

    void showElementEditor(String elementId, String elementTagName, String element);

    void setUserGuidance(String guidance);

    void sendUserPermissions(List<LeosPermission> userPermissions);

    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);

    void populateMarkedContent(String markedContent);
    
    void showTimeLineWindow(List documentVersions);
    
    void updateTimeLineWindow(List documentVersions);

    void displayComparison(HashMap<Integer, Object> htmlCompareResult);

    void showMajorVersionWindow();
    
    void setDocumentVersionInfo(VersionInfoVO versionInfoVO);
    
    void setPermissions(DocumentVO memorandum);
    
    void scrollToMarkedChange(final String elementId);

    void updateUserCoEditionInfo(List<CoEditionVO> coEditionVos, String presenterId);
    
    void displayDocumentUpdatedByCoEditorWarning();

    void checkElementCoEdition(List<CoEditionVO> coEditionVos, User user, final String elementId, final String elementTagName, final Action action, final Object actionEvent);

    void showAlertDialog(String messageKey);
}
