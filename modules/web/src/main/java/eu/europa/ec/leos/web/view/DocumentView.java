/**
 * Copyright 2015 European Commission
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.europa.ec.leos.model.content.LeosDocument;
import eu.europa.ec.leos.model.content.LeosDocumentProperties;
import eu.europa.ec.leos.vo.MetaDataVO;
import eu.europa.ec.leos.vo.TableOfContentItemVO;
import eu.europa.ec.leos.vo.lock.LockActionInfo;

public interface DocumentView extends LeosView {

    public static final String VIEW_ID = "document";

    void setDocumentName(final String documentName);

    void refreshContent(final String documentContent);

    void setToc(List<TableOfContentItemVO> tableOfContentItemVoList);

    void showArticleEditor(String articleId, String article);

    void showCitationsEditor(String citationsId, String citations);
    
    void showRecitalsEditor(String recitalsId, String recitals);

    void showMetadataEditWindow(MetaDataVO metaDataVO);

    void showDownloadWindow(LeosDocument docContent, String msgKey);
    
    void showTocEditWindow(List<TableOfContentItemVO> tableOfContentItemVoList, Map<TableOfContentItemVO.Type, List<TableOfContentItemVO.Type>> tableOfContentRules);
    
    void setDocumentPreviewURLs(String documentId, String pdfURL, String htmlURL);
    
    void showVersionListWindow(List<LeosDocumentProperties> versions) ;
    
    void showVersionCompareWindow(LeosDocumentProperties oldVersion,  LeosDocumentProperties newVersion) ;

    void displayComparision(HashMap<Integer, Object> htmlCompareResult);

    void updateLocks(LockActionInfo lockActionInfo);

    void refreshArticleEditor(String articleContent);

    void refreshCitationsEditor(String citationsContent);
    
    void refreshRecitalsEditor(String recitalsContent);
    
}