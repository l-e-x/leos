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
package eu.europa.ec.leos.annotate.repository;

import eu.europa.ec.leos.annotate.model.entity.Group;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * the repository for all {@link Group} objects denoting the groups in which users can be registered
 */
public interface GroupRepository extends CrudRepository<Group, Long> {

    /**
     * find a group based on its (internal) name
     * 
     * @param groupName the internal group name
     * @return found {@Group} object or {@literal null}
     */
    Group findByName(String groupName);

    /**
     * find a set of groups based on their IDs
     * 
     * @param groupIds group IDs to look for
     * @return found {@link Group} objects
     */
    List<Group> findByIdIn(List<Long> groupIds);
}
