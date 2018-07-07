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

import java.io.Serializable;
import java.util.Date;

public interface AuditableEntity<ID extends Serializable> extends BaseEntity<ID> {

    public User getCreatedBy();

    public void setCreatedBy(final User createdBy);

    public Date getCreatedOn();

    public void setCreatedOn(final Date createdOn);

    public User getUpdatedBy();

    public void setUpdatedBy(final User updatedBy);

    public Date getUpdatedOn();

    public void setUpdatedOn(final Date updatedOn);
}
