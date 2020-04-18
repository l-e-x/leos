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

import eu.europa.ec.leos.domain.cmis.document.LeosDocument;
import eu.europa.ec.leos.model.filter.QueryFilter;
import eu.europa.ec.leos.repository.store.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;

@Service
class WorkspaceServiceImpl implements WorkspaceService {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

    private final WorkspaceRepository workspaceRepository;

    @Value("${leos.workspaces.path}")
    protected String workspacesPath;

    WorkspaceServiceImpl(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }

    @Override
    @PostFilter("hasPermission(filterObject, 'CAN_READ')")
    public <T extends LeosDocument> List<T> browseWorkspace(Class<T> filterType, Boolean fetchContent) {
        LOG.debug("Browsing workspace... [path={}, filter={}]", workspacesPath, filterType.getSimpleName());
        return workspaceRepository.findDocumentsByParentPath(workspacesPath, filterType, fetchContent);
    }

    public <T extends LeosDocument> Stream<T> findDocuments(Class<T> filterType, Boolean fetchContent,
                                                            int startIndex, int maxResults, QueryFilter workspaceFilter) {
        LOG.debug("Browsing workspace... [path={}, filter={}]", workspacesPath, filterType.getSimpleName());
        return workspaceRepository.findPagedDocumentsByParentPath(workspacesPath, filterType, fetchContent,
                startIndex, maxResults, workspaceFilter);
    }

    public <T extends LeosDocument> int findDocumentCount(Class<T> filterType, QueryFilter workspaceFilter) {
        LOG.debug("Browsing workspace... [path={}, filter={}]", workspacesPath, filterType.getSimpleName());
        return workspaceRepository.findDocumentCountByParentPath(workspacesPath, filterType, workspaceFilter);
    }

    @Override
    public <T extends LeosDocument> T findDocumentById(String id, Class<T> filterType) {
        return workspaceRepository.findDocumentById(id, filterType, true);
    }

    @Override
    public <T extends LeosDocument> T findDocumentByRef(String ref, Class<T> filterType) {
        return workspaceRepository.findDocumentByRef(ref, filterType);
    }
}
