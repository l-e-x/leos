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
package eu.europa.ec.digit.userdata.controllers;

import java.util.Collection;
import java.util.stream.Collectors;

import eu.europa.ec.digit.userdata.entities.User;
import eu.europa.ec.digit.userdata.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserController {

    private static int MAX_RECORDS = 100;

    @Autowired UserRepository repository;

    @RequestMapping(method = RequestMethod.GET, path="/users")
    @Transactional(readOnly = true)
    public Collection<User> searchUsers(
            @RequestParam(value = "searchKey", required = true) String searchKey) {
        return repository.findUsersByKey(searchKey.trim().replace(" ", "%").concat("%"))
                .limit(MAX_RECORDS)
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, path="/users/{userId}")
    @Transactional(readOnly = true)
    public User getUser(
            @PathVariable(value = "userId", required = true) String userId) {
        return repository.findByLogin(userId);
    }

    @RequestMapping(method = RequestMethod.GET, path="entities")
    @Transactional(readOnly = true)
    public Collection<String> getAllEntities() {
        return repository.findAllEntities()
                .collect(Collectors.toList());
    }

    @RequestMapping(method = RequestMethod.GET, path="entities/{entity}/users")
    @Transactional(readOnly = true)
    public Collection<User> searchUsersByEntityIdAndKey(
            @PathVariable(value = "entity", required = false) String entity,
            @RequestParam(value = "searchKey", required = true) String searchKey) {
        return repository.findUsersByKeyAndEntity(searchKey.trim().replace(" ", "%").concat("%"), entity)
                .limit(MAX_RECORDS)
                .collect(Collectors.toList());
    }
}
