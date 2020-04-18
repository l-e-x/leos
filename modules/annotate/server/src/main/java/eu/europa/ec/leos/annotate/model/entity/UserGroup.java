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
package eu.europa.ec.leos.annotate.model.entity;

import javax.persistence.*;

import eu.europa.ec.leos.annotate.Generated;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "USERS_GROUPS", uniqueConstraints = @UniqueConstraint(columnNames = {"USER_ID",
        "GROUP_ID"}), indexes = @Index(columnList = "GROUP_ID", name = "USERS_GROUPS_IX_GROUPS"))
public class UserGroup implements Serializable {

    private static final long serialVersionUID = -3611867106577022011L;

    /**
     * Class representing a membership of a user in a group 
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "ID", nullable = false)
    @GenericGenerator(name = "usersGroupsSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "USERS_GROUPS_SEQ"),
            // @Parameter(name = "initial_value", value = "1000"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "usersGroupsSequenceGenerator")
    @SuppressWarnings("PMD.ShortVariable")
    private long id;

    @Column(name = "USER_ID", nullable = false)
    private long userId;

    @Column(name = "GROUP_ID", nullable = false)
    private long groupId;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public UserGroup() {
        // default constructor required by JPA
    }

    public UserGroup(final long userId, final long groupId) {
        this.userId = userId;
        this.groupId = groupId;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public Long getId() {
        return id;
    }

    @Generated
    public void setId(final Long newId) {
        this.id = newId;
    }

    @Generated
    public long getUserId() {
        return userId;
    }

    @Generated
    public void setUserId(final long userId) {
        this.userId = userId;
    }

    @Generated
    public long getGroupId() {
        return groupId;
    }

    @Generated
    public void setGroupId(final long groupId) {
        this.groupId = groupId;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, groupId, userId);
    }

    @Generated
    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final UserGroup other = (UserGroup) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.groupId, other.groupId) &&
                Objects.equals(this.userId, other.userId);
    }
}
