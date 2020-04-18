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
package eu.europa.ec.leos.repository.store;

import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.domain.cmis.document.XmlDocument;
import eu.europa.ec.leos.model.filter.QueryFilter;
import org.springframework.security.access.prepost.PostAuthorize;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Workspace Repository interface.
 * <p>
 * Represents collections of *documents*, with specific methods to retrieve them.
 */
public interface WorkspaceRepository {

    /**
     * Finds documents with the specified characteristics.
     *
     * @param path the path where to find the documents.
     * @param type the type class of the documents.
     * @return the list of found documents or empty.
     */
    <T extends LeosDocument> List<T> findDocumentsByParentPath(String path, Class<? extends T> type, boolean fetchContent);

    <T extends LeosDocument> T findDocumentById(String id, Class<? extends T> type, boolean latest);

    <T extends LeosDocument> T findDocumentByRef(String ref, Class<? extends T> type);

    /**
     * Updates the collaborators of the specified document.
     *
     * @param id            the ID of the document to update.
     * @param collaborators the map of users to authorities.
     * @param type          the type class of the document.
     * @return the updated document.
     */
    <T extends XmlDocument> T updateDocumentCollaborators(String id, Map<String, String> collaborators, Class<? extends T> type);

    /**
     * Finds documents with the specified pagination .
     *
     * @param D          the document type
     * @param path       the path where to find the documents.
     * @param type       the type class of the documents.
     * @param startIndex the start index for result.
     * @param maxResults the maximum number of result to be displayed.
     * @return the list of found documents or empty.
     */
    <D extends LeosDocument> Stream<D> findPagedDocumentsByParentPath(String path, Class<? extends D> type, boolean fetchContent,
                                                                      int startIndex, int maxResults, QueryFilter workspaceFilter);
    /**
     * Finds documents count .
     *
     * @param D    the document type
     * @param path the path where to find the documents.
     * @param type the type class of the documents.
     * @return Number of documents.
     */
    <D extends LeosDocument> int findDocumentCountByParentPath(String path, Class<? extends D> type, QueryFilter workspaceFilter);
}
