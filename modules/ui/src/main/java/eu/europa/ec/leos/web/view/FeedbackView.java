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

import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;

import java.util.List;

public interface FeedbackView extends LeosView {

    String VIEW_ID = "feedback";

    void setDocumentTitle(final String documentTitle);

    void setDocumentStage(final LeosDocumentProperties.Stage setStage);

    void refreshContent(final String documentContent);

    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);

    void updateLocks(LockActionInfo lockActionInfo);
    
    void showSuggestionEditor(String elementId, String suggestionFragment);

    void setUser();
}
