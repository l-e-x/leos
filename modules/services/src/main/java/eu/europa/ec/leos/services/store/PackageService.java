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
package eu.europa.ec.leos.services.store;

import eu.europa.ec.leos.domain.cmis.LeosPackage;
import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.domain.common.TocMode;
import eu.europa.ec.leos.vo.toc.TableOfContentItemVO;

import java.util.List;
import java.util.Map;

public interface PackageService {

    String NOT_AVAILABLE = "Not available";

    LeosPackage createPackage();

    void deletePackage(LeosPackage leosPackage);

    LeosPackage findPackageByDocumentId(String documentId);

    // TODO consider using package id instead of path
    <T extends LeosDocument> List<T> findDocumentsByPackagePath(String path, Class<T> filterType, Boolean fetchContent);
    
    <T extends LeosDocument> T findDocumentByPackagePathAndName(String path, String name, Class<T> filterType);

    <T extends LeosDocument> List<T> findDocumentsByPackageId(String id, Class<T> filterType, Boolean allVersions, Boolean fetchContent);
    
    <T extends LeosDocument> List<T> findDocumentsByUserId(String userId, Class<T> filterType, String leosAuthority);

    Map<String, List<TableOfContentItemVO>> getTableOfContent(String documentId, TocMode mode);

    String calculateDocType(XmlDocument targetDocument);
}
