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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

class SearchStrategyDiscoveryServices implements SearchStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SearchStrategyDiscoveryServices.class);

    private final Session cmisSession;

    SearchStrategyDiscoveryServices(Session cmisSession) {
        this.cmisSession = cmisSession;
    }

    @Override
    public List<Document> findDocuments(Folder folder, String primaryType, Set<LeosCategory> categories, boolean descendants, boolean allVersion, OperationContext context) {
        logger.trace("Finding documents...");
        String categoryStr = categories.stream()
                .map(leosCategory -> "'" + leosCategory.name() + "'")
                .collect(Collectors.joining(", "));

        String whereClause;
        if (descendants) {
            whereClause = CmisProperties.DOCUMENT_CATEGORY.getId() + " IN (" + categoryStr + ") AND IN_TREE('" + folder.getId() + "')";
        } else {
            whereClause = CmisProperties.DOCUMENT_CATEGORY.getId() + " IN (" + categoryStr + ") AND IN_FOLDER('" + folder.getId() + "')";
        }
        logger.trace("Querying CMIS objects... [primaryType=" + primaryType + ", where=" + whereClause + "]");

        // NOTE only the latest version (major or minor) of each document should be returned (searchAllVersions = false)
        ItemIterable<CmisObject> cmisObjects = cmisSession.queryObjects(primaryType, whereClause, allVersion, context);
        return StreamSupport.stream(cmisObjects.spliterator(), false)
                .map(cmisObject -> (Document) cmisObject)
                .collect(Collectors.toList());
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
        String whereClause = CmisProperties.STATUS.getId() + " IN ('" + status + "')";
        ItemIterable<CmisObject> cmisObjects = cmisSession.queryObjects(primaryType, whereClause, false, context);
        return StreamSupport.stream(cmisObjects.spliterator(), false)
                .map(cmisObject -> (Document) cmisObject)
                .collect(Collectors.toList());
    }
}
