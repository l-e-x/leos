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

import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

/**
 * the repository for all {@link UserGroup} objects denoting the user memberships in groups
 */
public interface UserGroupRepository extends CrudRepository<UserGroup, Long> {
    
    /**
     * find a particular group membership of a user
     * 
     * @param userId the user ID
     * @param groupId the group ID
     * @return the found membership entry of the user in the group, or {@literal null}
     */
    UserGroup findByUserIdAndGroupId(long userId, long groupId);
    
    /**
     * retrieve all group memberships of a given user
     * 
     * @param userId the user ID
     * @return all {@link UserGroup} objects denoting the user's group memberships
     */
    List<UserGroup> findByUserId(long userId);

    /**
     * retrieve all group memberships of a given group
     * 
     * @param groupId the group ID
     * @return all {@link UserGroup} objects denoting the group's user memberships
     */
    List<UserGroup> findByGroupId(long groupId);
}
