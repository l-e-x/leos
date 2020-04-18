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
package eu.europa.ec.leos.model.user;

import java.io.Serializable;

public class Entity implements Serializable {

    private static final long serialVersionUID = 7727461372817817872L;

    private String id;
    private String name;
    private String organizationName;

    public Entity() {
    }

    public Entity(String id, String name, String organizationName) {
        this.id = id;
        this.name = name;
        this.organizationName = organizationName;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Entity other = (Entity) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Entity [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", organizationName=");
        builder.append(organizationName);
        builder.append("]");
        return builder.toString();
    }
}
