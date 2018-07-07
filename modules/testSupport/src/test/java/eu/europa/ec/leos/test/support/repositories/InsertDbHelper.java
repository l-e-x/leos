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
package eu.europa.ec.leos.test.support.repositories;

import eu.europa.ec.leos.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

public class InsertDbHelper {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void insertUser(User user) {
        final String insertUserQuery = "INSERT INTO LEOS_USER (USR_ID, USR_LOGIN, USR_NAME, USR_CREATED_BY, USR_CREATED_ON, USR_UPDATED_BY, USR_UPDATED_ON, USR_STATE) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Long createdById = user.getCreatedBy() != null ? user.getCreatedBy().getId() : null;
        Long updatedById = user.getUpdatedBy() != null ? user.getUpdatedBy().getId() : null;
        String state = user.getState() != null ? user.getState().name() : null;

        jdbcTemplate.update(insertUserQuery, user.getId(), user.getLogin(), user.getName(), createdById,
                user.getCreatedOn(), updatedById, user.getUpdatedOn(), state);
    }
}
