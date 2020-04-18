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
import eu.europa.ec.leos.model.filter.QueryFilter;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class SearchStrategyNavigationServices extends SearchStrategyImpl {

    private static final Logger logger = LoggerFactory.getLogger(SearchStrategyNavigationServices.class);
    
    SearchStrategyNavigationServices(Session cmisSession) {
        super(cmisSession);
    }

    @Override
    public List<Document> findDocuments(Folder folder, String primaryType, Set<LeosCategory> categories, boolean descendants, boolean allVersion,
            OperationContext context) {
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
        });
    }
}
