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
package eu.europa.ec.leos.cmis.search;

import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import eu.europa.ec.leos.model.filter.QueryFilter;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.OperationContext;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface SearchStrategy {
    List<Document> findDocuments(Folder folder, String primaryType, Set<LeosCategory> categories, boolean descendants, boolean allVersion, OperationContext context);

    List<Document> findDocumentsForUser(String userId, String primaryType, String leosAuthority, OperationContext context);

    List<Document> findDocumentsByStatus(LeosLegStatus status, String primaryType, OperationContext context);

    Stream<Document> findDocumentPage(Folder folder, String primaryType, Set<LeosCategory> categories, boolean descendants, boolean allVersion, OperationContext context, int startIndex, QueryFilter workspaceFilter);

    int findDocumentCount(Folder folder, String primaryType, Set<LeosCategory> categories, boolean descendants, boolean allVersion, OperationContext context, QueryFilter workspaceFilter);

    List<Document> findDocumentsByRef(String ref, String primaryType, OperationContext context);
    
    Stream<Document> findAllMinorsForIntermediate(String primaryType, String docRef, String currIntVersion, String prevIntVersion, int startIndex, OperationContext context);
   
    int findAllMinorsCountForIntermediate(String primaryType, String docRef, String currIntVersion, String prevIntVersion, OperationContext context);

    Integer findAllMajorsCount(String primaryType, String docRef, OperationContext context);

    Stream<Document> findAllMajors(String primaryType, String docRef, int startIndex, OperationContext context);

    Stream<Document> findRecentMinorVersions(String primaryType, String documentRef, String versionLabel, int startIndex, OperationContext context);
    
    Integer findRecentMinorVersionsCount(String primaryType, String documentRef, String versionLabel, OperationContext context);

}
