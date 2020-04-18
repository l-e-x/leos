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
package eu.europa.ec.digit.userdata.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;

@javax.persistence.Entity
@Table(name = "LEOS_ENTITY")
public class Entity {

    @Id
    @Column(name = "ENTITY_ID", nullable = false, insertable = false, updatable = false)
    private String id;

    @Column(name = "ENTITY_NAME", nullable = false, insertable = false, updatable = false)
    private String name;

    @JsonIgnore
    @Column(name = "ENTITY_PARENT_ID", nullable = true, insertable = false, updatable = false)
    private String parentId;

    @Column(name = "ENTITY_ORG_NAME", nullable = false, insertable = false, updatable = false)
    private String organizationName;

    public Entity() {
    }

    public Entity(String id, String name, String parentId,
            String organizationName) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.organizationName = organizationName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getParentId() {
        return parentId;
    }

    public String getOrganizationName() {
        return organizationName;
    }
}
