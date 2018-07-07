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
package eu.europa.ec.leos.model;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;

@MappedSuperclass
public abstract class AbstractBaseEntity<ID extends Serializable> implements BaseEntity<ID> {

    public static final String STATE_FIELD_NAME = "state";

    @Enumerated(EnumType.STRING)
    private State state;

    public AbstractBaseEntity() {
        setState(State.A);  // Entity is ACTIVE by default
    }

    @Override
    public boolean isNew() {
        return (getId() == null);
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(final State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AbstractBaseEntity{");
        sb.append("id=").append(getId());
        sb.append(", state=").append(state);
        sb.append('}');
        return sb.toString();
    }
}
