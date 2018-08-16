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

import eu.europa.ec.leos.domain.document.LeosDocument.XmlDocument.Memorandum;
import eu.europa.ec.leos.domain.document.LeosMetadata.MemorandumMetadata;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;

public interface MemorandumService {

    Memorandum createMemorandum(String templateId, String path, MemorandumMetadata metadata, String actionMsg, byte[] content);

    Memorandum findMemorandum(String id);
    
    Memorandum findMemorandumVersion(String id);

    // FIXME temporary workaround
    Memorandum findMemorandumByPackagePath(String path);

    Memorandum updateMemorandum(Memorandum memorandum, byte[] updatedMemorandumContent, boolean major, String comment);

    Memorandum updateMemorandum(Memorandum memorandum, MemorandumMetadata metadata, boolean major, String comment);

    List<TableOfContentItemVO> getTableOfContent(Memorandum document);

    List<Memorandum> findVersions(String id);

    Memorandum createVersion(String id, boolean major, String comment);
}
