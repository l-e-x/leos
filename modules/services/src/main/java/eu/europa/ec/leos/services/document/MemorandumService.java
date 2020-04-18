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
package eu.europa.ec.leos.services.document;


import eu.europa.ec.leos.domain.cmis.common.VersionType;
import eu.europa.ec.leos.domain.cmis.document.Memorandum;
import eu.europa.ec.leos.domain.cmis.metadata.MemorandumMetadata;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;

public interface MemorandumService {

    Memorandum createMemorandum(String templateId, String path, MemorandumMetadata metadata, String actionMsg, byte[] content);

    Memorandum createMemorandumFromContent(String path, MemorandumMetadata metadata, String actionMsg, byte[] content);

    Memorandum findMemorandum(String id);
    
    Memorandum findMemorandumVersion(String id);

    // FIXME temporary workaround
    Memorandum findMemorandumByPackagePath(String path);

    Memorandum updateMemorandum(Memorandum memorandum, byte[] updatedMemorandumContent, VersionType versionType, String comment);

    Memorandum updateMemorandum(Memorandum memorandum, MemorandumMetadata metadata, VersionType versionType, String comment);

    Memorandum updateMemorandum(String memorandumId, MemorandumMetadata metadata);

    Memorandum updateMemorandumWithMilestoneComments(Memorandum memorandum, List<String> milestoneComments, VersionType versionType, String comment);

    Memorandum updateMemorandumWithMilestoneComments(String memorandumId, List<String> milestoneComments);

    List<TableOfContentItemVO> getTableOfContent(Memorandum document, TocMode mode);

    List<Memorandum> findVersions(String id);

    Memorandum createVersion(String id, VersionType versionType, String comment);

    Memorandum findMemorandumByRef(String ref);
    
    List<VersionVO> getAllVersions(String id, String documentId);
    
    List<Memorandum> findAllMinorsForIntermediate(String docRef, String currIntVersion, int startIndex, int maxResults);
    
    int findAllMinorsCountForIntermediate(String docRef, String currIntVersion);
    
    Integer findAllMajorsCount(String docRef);
    
    List<Memorandum> findAllMajors(String docRef, int startIndex, int maxResults);
    
    List<Memorandum> findRecentMinorVersions(String documentId, String documentRef, int startIndex, int maxResults);
    
    Integer findRecentMinorVersionsCount(String documentId, String documentRef);
}
