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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Annex;
import eu.europa.ec.leos.domain.document.LeosMetadata;
import eu.europa.ec.leos.domain.document.LeosMetadata.AnnexMetadata;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;

public interface AnnexService {

    Annex createAnnex(String templateId, String path, AnnexMetadata metadata, String actionMessage, byte[] content);

    void deleteAnnex(Annex annex);

    Annex updateAnnex(Annex annex, LeosMetadata.AnnexMetadata metadata, boolean major, String comment);

    Annex updateAnnex(Annex annex, byte[] updatedAnnexContent, boolean major, String comment);

    Annex findAnnex(String id);

    Annex findAnnexVersion(String id);

    List<Annex> findVersions(String id);

    Annex createVersion(String id, boolean major, String comment);
    
    List<TableOfContentItemVO> getTableOfContent(Annex document);
    
    Annex saveTableOfContent(Annex annex, List<TableOfContentItemVO> tocList, String actionMsg);
}
