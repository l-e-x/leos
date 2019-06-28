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

import eu.europa.ec.leos.cmis.mapping.CmisProperties;
import eu.europa.ec.leos.domain.cmis.LeosCategory;
import eu.europa.ec.leos.domain.cmis.LeosLegStatus;
import org.apache.chemistry.opencmis.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class SearchStrategyNavigationServices implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SearchStrategyNavigationServices.class);

    private final Session cmisSession;

    SearchStrategyNavigationServices(Session cmisSession) {
        this.cmisSession = cmisSession;
    }

    @Override
    public List<Document> findDocuments(Folder folder, String primaryType, Set<LeosCategory> categories, boolean descendants, boolean allVersion, OperationContext context) {
        logger.trace("Finding documents...");
        List<String> categoryList = categories.stream().map(LeosCategory::name).collect(Collectors.toList());
        List<Document> documents;
        if (descendants) {
            documents = findDescendants(folder, primaryType, context);
        } else {
            documents = findChildren(folder, primaryType, context);
        }
        return documents.stream()
                .filter(document -> categoryList.contains(document.getPropertyValue(CmisProperties.DOCUMENT_CATEGORY.getId())))
                .collect(Collectors.toList());
    }

    private List<Document> findChildren(Folder folder, String primaryType, OperationContext context) {
        ItemIterable<CmisObject> children = folder.getChildren(context);
        return StreamSupport.stream(children.spliterator(), false)
                .filter(cmisObject -> cmisObject.getType().getId().equals(primaryType))
                .map(cmisObject -> (Document) cmisObject)
                .collect(Collectors.toList());
    }

    private List<Document> findDescendants(Folder folder, String primaryType, OperationContext context) {
        List<Document> documents = new ArrayList<>();
        List<Tree<FileableCmisObject>> descendants = folder.getDescendants(-1, context);
        flattenAndFilter(descendants, primaryType, documents);
        return documents;
    }

    private void flattenAndFilter(List<Tree<FileableCmisObject>> nodes, String primaryType, List<Document> documents) {
        nodes.forEach(cmisObject -> {
            if (cmisObject.getItem().getType().getId().equals(primaryType)) {
                documents.add((Document) cmisObject.getItem());
                    }
            flattenAndFilter(cmisObject.getChildren(), primaryType, documents);
                }
        );
    }

    @Override
    public List<Document> findDocumentsForUser(String userId, String primaryType, String leosAuthority, OperationContext context) {
        logger.trace("Finding documents...");
        String whereClause = CmisProperties.DOCUMENT_CATEGORY.getId() + " IN ('PROPOSAL') AND ANY " + CmisProperties.COLLABORATORS.getId() + " IN ('" + userId + "::" + leosAuthority + "')";
        logger.trace("Ordering by ....." + context.getOrderBy());
        ItemIterable<CmisObject> cmisObjects = cmisSession.queryObjects(primaryType, whereClause, false, context);

        return StreamSupport.stream(cmisObjects.spliterator(), false)
                .map(cmisObject -> (Document) cmisObject)
                .collect(Collectors.toList());
    }

    @Override
    public List<Document> findDocumentsByStatus(LeosLegStatus status, String primaryType, OperationContext context) {
        logger.trace("Finding documents by status...");
        String whereClause = CmisProperties.STATUS.getId() + " IN ('" + status + "')";
        ItemIterable<CmisObject> cmisObjects = cmisSession.queryObjects(primaryType, whereClause, false, context);
        return StreamSupport.stream(cmisObjects.spliterator(), false)
                .map(cmisObject -> (Document) cmisObject)
                .collect(Collectors.toList());
    }
}
