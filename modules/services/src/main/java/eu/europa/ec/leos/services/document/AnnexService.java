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
import eu.europa.ec.leos.domain.cmis.document.Annex;
import eu.europa.ec.leos.domain.cmis.metadata.AnnexMetadata;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.model.action.VersionVO;
import eu.europa.ec.leos.model.annex.AnnexStructureType;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;

public interface AnnexService {

    Annex createAnnex(String templateId, String path, AnnexMetadata metadata, String actionMessage, byte[] content);

    Annex createAnnexFromContent(String path, AnnexMetadata metadata, String actionMessage, byte[] content);

    void deleteAnnex(Annex annex);

    Annex updateAnnex(Annex annex, AnnexMetadata metadata, VersionType versionType, String comment);
    
    Annex updateAnnex(Annex annex, byte[] updatedAnnexContent, VersionType versionType, String comment);

    Annex updateAnnex(String annexId, AnnexMetadata metadata);

    Annex updateAnnexWithMilestoneComments(Annex annex, List<String> milestoneComments, VersionType versionType, String comment);

    Annex updateAnnexWithMilestoneComments(String annexId, List<String> milestoneComments);
    
    Annex updateAnnexWithMetadata(Annex annex, byte[] updatedAnnexContent, AnnexMetadata metadata, VersionType versionType, String comment);

    Annex findAnnex(String id);

    Annex findAnnexVersion(String id);

    List<Annex> findVersions(String id);

    Annex createVersion(String id, VersionType versionType, String comment);
    
    List<TableOfContentItemVO> getTableOfContent(Annex document, TocMode mode);
    
    Annex saveTableOfContent(Annex annex, List<TableOfContentItemVO> tocList, AnnexStructureType structureType, String actionMsg, User user);

    Annex findAnnexByRef(String ref);
    
    List<VersionVO> getAllVersions(String id, String documentId);
    
    List<Annex> findAllMinorsForIntermediate(String docRef, String currIntVersion, int startIndex, int maxResults);
    
    int findAllMinorsCountForIntermediate(String docRef, String currIntVersion);
    
    Integer findAllMajorsCount(String docRef);
    
    List<Annex> findAllMajors(String docRef, int startIndex, int maxResults);
    
    List<Annex> findRecentMinorVersions(String documentId, String documentRef, int startIndex, int maxResults);
    
    Integer findRecentMinorVersionsCount(String documentId, String documentRef);

    List<String> getAncestorsIdsForElementId(Annex annex, List<String> elementIds);
}
