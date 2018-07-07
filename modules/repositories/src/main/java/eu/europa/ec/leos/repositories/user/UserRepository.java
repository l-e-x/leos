/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.repositories.user;

import eu.europa.ec.leos.model.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface UserRepository extends Repository<User, Long> {

    /**
     * Find a user by login and state.
     *
     * @param login the user login.
     * @param state the user state.
     * @return an user object or <code>null</code> if the user is not found.
     */
    User findByLoginAndState(String login, User.State state);

    /**
     * find the list of all users in the repository.
     *
     * @return list of user.
     */
    @Query("select user from User user")
    List<User> findAll();
}
