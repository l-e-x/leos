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

import eu.europa.ec.digit.userdata.entities.Entity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.stream.Stream;

public interface EntityRepository extends Repository<Entity, String> {

    @Query(value = "SELECT DISTINCT(ENTITY_ORG_NAME) FROM LEOS_ENTITY ORDER BY ENTITY_ORG_NAME", nativeQuery = true)
    Stream<String> findAllOrganizations();

    @Query(value = "WITH ANCESTORS(ENTITY_ID, ENTITY_NAME, ENTITY_PARENT_ID, ENTITY_ORG_NAME) AS "
            + " ( "
            + " SELECT ENTITY_ID, ENTITY_NAME, ENTITY_PARENT_ID, ENTITY_ORG_NAME FROM LEOS_ENTITY WHERE ENTITY_ID IN (?1) "
            + " UNION ALL "
            + " SELECT T2.ENTITY_ID, T2.ENTITY_NAME, T2.ENTITY_PARENT_ID, T2.ENTITY_ORG_NAME FROM ANCESTORS T1 INNER JOIN LEOS_ENTITY T2 ON T1.ENTITY_PARENT_ID = T2.ENTITY_ID"
            + " ) "
            + " SELECT DISTINCT ENTITY_ID, ENTITY_NAME, ENTITY_PARENT_ID, ENTITY_ORG_NAME FROM ANCESTORS ORDER BY ENTITY_ORG_NAME, ENTITY_NAME ", nativeQuery = true)
    Stream<Entity> findAllFullPathEntities(List<String> entitiesIds);
}
