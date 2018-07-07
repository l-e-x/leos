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

import eu.europa.ec.leos.model.user.User;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@MappedSuperclass
public abstract class AbstractAuditableEntity<ID extends Serializable> extends AbstractBaseEntity<ID> implements AuditableEntity<ID> {

    public static final String CREATED_BY_FIELD_NAME = "createdBy";
    public static final String CREATED_ON_FIELD_NAME = "createdOn";
    public static final String UPDATED_BY_FIELD_NAME = "updatedBy";
    public static final String UPDATED_ON_FIELD_NAME = "updatedOn";

    @CreatedBy
    @ManyToOne(fetch = FetchType.LAZY)
    private User createdBy;

    @CreatedDate
    @Temporal(TemporalType.DATE)
    private Date createdOn;

    @LastModifiedBy
    @ManyToOne(fetch = FetchType.LAZY)
    private User updatedBy;

    @LastModifiedDate
    @Temporal(TemporalType.DATE)
    private Date updatedOn;

    @Override
    public User getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(final User createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Date getCreatedOn() {
        return createdOn;
    }

    @Override
    public void setCreatedOn(final Date createdOn) {
        this.createdOn = createdOn;
    }

    @Override
    public User getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public void setUpdatedBy(final User updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public Date getUpdatedOn() {
        return updatedOn;
    }

    @Override
    public void setUpdatedOn(final Date updatedOn) {
        this.updatedOn = updatedOn;
    }
}
