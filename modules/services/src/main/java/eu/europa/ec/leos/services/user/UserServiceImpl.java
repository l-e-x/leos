/*
 * Copyright 2017 European Commission
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
package eu.europa.ec.leos.services.user;

import eu.europa.ec.leos.integration.UsersProvider;
import eu.europa.ec.leos.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersProvider usersClient;

    @Override
    @Cacheable(value="users", cacheManager = "cacheManager")
    public User getUser(String login) {
        User result = usersClient.getUserByLogin(login);

        return result;
    }

    @Override
    public List<User> searchUsersByKey(String key) {
        List<User> result = usersClient.searchUsers(key);

        return result;
    }
}
