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
package eu.europa.ec.digit.userdata.repositories;

import eu.europa.ec.digit.userdata.entities.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.stream.Stream;

public interface UserRepository extends Repository<User, Long> {

    User findByLogin(String login);

    // FIXME: shift functions to DB later
    @Query(value = "SELECT * FROM LEOS_USER " + " WHERE "
            + " deAccent(USER_LASTNAME || ' ' || USER_FIRSTNAME) LIKE deAccent(?1) "
            + " OR "
            + " deAccent(USER_FIRSTNAME || ' ' || USER_LASTNAME) LIKE deAccent(?1) "
            + " ORDER BY USER_LASTNAME, USER_FIRSTNAME ", nativeQuery = true)
    Stream<User> findUsersByKey(String key);

    // FIXME: shift functions to DB later
    @Query(value = "SELECT DISTINCT LEOS_USER.* FROM LEOS_USER INNER JOIN LEOS_USER_ENTITY ON LEOS_USER.USER_LOGIN = LEOS_USER_ENTITY.USER_LOGIN "
            + " INNER JOIN LEOS_ENTITY ON LEOS_USER_ENTITY.ENTITY_ID = LEOS_ENTITY.ENTITY_ID "
            + " WHERE "
            + " (deAccent(USER_LASTNAME || ' ' || USER_FIRSTNAME) LIKE deAccent(?1) "
            + " OR "
            + " deAccent(USER_FIRSTNAME || ' ' || USER_LASTNAME) LIKE deAccent(?1)) "
            + " AND "
            + " deAccent(LEOS_ENTITY.ENTITY_ORG_NAME) LIKE deAccent(?2) "
            + " ORDER BY USER_LASTNAME, USER_FIRSTNAME ", nativeQuery = true)
    Stream<User> findUsersByKeyAndOrganization(String key, String organization);
}