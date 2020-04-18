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
import eu.europa.ec.leos.repository.LeosRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Workspace Repository implementation.
 *
 * @constructor Creates a specific Workspace Repository, injected with a generic LEOS Repository.
 */
@Repository
public class WorkspaceRepositoryImpl implements WorkspaceRepository {

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceRepositoryImpl.class);

    private final LeosRepository leosRepository;

    @Autowired
    public WorkspaceRepositoryImpl(LeosRepository leosRepository) {
        this.leosRepository = leosRepository;
    }

    @Override
    public <T extends LeosDocument> List<T> findDocumentsByParentPath(String path, Class<? extends T> type, boolean fetchContent) {
        logger.debug("Finding document by parent path... [path=" + path + ", type=" + type.getSimpleName() + "]");
        return leosRepository.findDocumentsByParentPath(path, type, true, fetchContent);
    }

    @Override
    public <T extends LeosDocument> T findDocumentById(String id, Class<? extends T> type, boolean latest) {
        logger.debug("Finding document by ID... [id=" + id + ", type=" + type.getSimpleName() + ", latest=" + latest + "]");
        return leosRepository.findDocumentById(id, type, latest);
    }

    @Override
    public <T extends LeosDocument> T findDocumentByRef(String ref, Class<? extends T> type) {
        logger.debug("Finding document by ref... [ref=" + ref + ", type=" + type.getSimpleName() + "]");
        return leosRepository.findDocumentByRef(ref, type);
    }

    @Override
    public <T extends XmlDocument> T updateDocumentCollaborators(String id, Map<String, String> collaborators, Class<? extends T> type) {
        logger.debug("Updating document collaborators... [id=" + id + ", collaborators=" + collaborators + "]");
        return leosRepository.updateDocument(id, collaborators, type);
    }

    @Override
    public <D extends LeosDocument> Stream<D> findPagedDocumentsByParentPath(String path, Class<? extends D> type, boolean fetchContent,
                                                                             int startIndex, int maxResults, QueryFilter workspaceFilter) {
        logger.debug("Finding document by parent path... [path=$path, type=${type.simpleName}]");
        return leosRepository.findPagedDocumentsByParentPath(path, type, true, fetchContent, startIndex, maxResults, workspaceFilter);
    }

    @Override
    public <D extends LeosDocument> int findDocumentCountByParentPath(String path, Class<? extends D> type, QueryFilter workspaceFilter) {
        logger.debug("Finding document by parent path... [path=$path, type=${type.simpleName}]");
        return leosRepository.findDocumentCountByParentPath(path, type, true, workspaceFilter);
    }
}
