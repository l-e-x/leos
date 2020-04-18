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
package eu.europa.ec.digit.userdata.controllers;

import eu.europa.ec.digit.userdata.entities.Entity;
import eu.europa.ec.digit.userdata.entities.User;
import eu.europa.ec.digit.userdata.repositories.EntityRepository;
import eu.europa.ec.digit.userdata.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private static int MAX_RECORDS = 100;

    @Autowired
    UserRepository userRepository;

    @Autowired
    EntityRepository entityRepository;

    @RequestMapping(method = RequestMethod.GET, path = "/users")
    @Transactional(readOnly = true)
    public Collection<User> searchUsers(
            @RequestParam(value = "searchKey", required = true) String searchKey) {
        return userRepository
                .findUsersByKey(searchKey.trim().replace(" ", "%").concat("%"))
                .limit(MAX_RECORDS).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/users/{userId}")
    @Transactional(readOnly = true)
    public User getUser(
            @PathVariable(value = "userId", required = true) String userId) {
        return userRepository.findByLogin(userId);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/entities")
    @Transactional(readOnly = true)
    public Collection<String> getAllOrganizations() {
        return entityRepository.findAllOrganizations()
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/entities/{org}/users")
    @Transactional(readOnly = true)
    public Collection<User> searchUsersByOrganizationAndKey(
            @PathVariable(value = "org", required = false) String organization,
            @RequestParam(value = "searchKey", required = true) String searchKey) {
        return userRepository
                .findUsersByKeyAndOrganization(
                        searchKey.trim().replace(" ", "%").concat("%"),
                        organization)
                .limit(MAX_RECORDS).collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, path = "/entities/{userId}")
    @Transactional(readOnly = true)
    public Collection<Entity> getAllFullPathEntitiesForUser(
            @PathVariable(value = "userId", required = true) String userId) {
        return entityRepository
                .findAllFullPathEntities(getUser(userId).getEntities().stream()
                        .map(e -> e.getId()).collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
