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
package eu.europa.ec.leos.model.user;

import eu.europa.ec.leos.model.AbstractAuditableEntity;
import eu.europa.ec.leos.model.AbstractBaseEntity;

import javax.persistence.*;

@Entity
@Table(name = "LEOS_USER")
@AttributeOverrides({
        @AttributeOverride(name = AbstractBaseEntity.STATE_FIELD_NAME, column = @Column(name = "USR_STATE", nullable = false, insertable = false, updatable = false)),
        @AttributeOverride(name = AbstractAuditableEntity.CREATED_ON_FIELD_NAME, column = @Column(name = "USR_CREATED_ON", nullable = false, insertable = false, updatable = false)),
        @AttributeOverride(name = AbstractAuditableEntity.UPDATED_ON_FIELD_NAME, column = @Column(name = "USR_UPDATED_ON", insertable = false, updatable = false))})
@AssociationOverrides({
        @AssociationOverride(name = AbstractAuditableEntity.CREATED_BY_FIELD_NAME, joinColumns = @JoinColumn(name = "USR_CREATED_BY", nullable = false, insertable = false, updatable = false)),
        @AssociationOverride(name = AbstractAuditableEntity.UPDATED_BY_FIELD_NAME, joinColumns = @JoinColumn(name = "USR_UPDATED_BY", insertable = false, updatable = false))})
@SequenceGenerator(name = "Generator.LeosUser", sequenceName = "SEQ_LEOS_USER", allocationSize = 1)
public class User extends AbstractAuditableEntity<Long> {
    // Because the application should not be managing users,
    // fields should be insertable=false and updatable=false.

    private static final long serialVersionUID = 2524826537916551014L;

    @Id
    @Column(name = "USR_ID", nullable = false, insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "Generator.LeosUser")
    private Long id;

    @Column(name = "USR_LOGIN", nullable = false, insertable = false, updatable = false, unique = true)
    private String login;

    @Column(name = "USR_NAME", nullable = false, insertable = false, updatable = false)
    private String name;

    @Override
    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }
    
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("id=").append(id);
        sb.append(", login='").append(login).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append('}');
        return sb.toString();
    }
    
    @Transient
    private Department dg;
    
    public Department getDepartment() {
    	return dg;    
    }   
    
    public void setDepartment(Department department) {
    	dg=department;
    }
}
