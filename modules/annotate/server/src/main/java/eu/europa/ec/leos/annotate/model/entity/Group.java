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

import eu.europa.ec.leos.annotate.Generated;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "GROUPS", uniqueConstraints = @UniqueConstraint(columnNames = {"NAME", "DISPLAYNAME"}))
public class Group {

    /**
     * Class representing a group in which an annotation is posted 
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "GROUP_ID", nullable = false)
    @GenericGenerator(name = "groupsSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "GROUPS_SEQ"),
            // @Parameter(name = "initial_value", value = "1000"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "groupsSequenceGenerator")
    @SuppressWarnings("PMD.ShortVariable")
    private long id;

    // internal ID of the group
    @Column(name = "NAME", nullable = false)
    private String name;

    // group name being shown to the user
    @Column(name = "DISPLAYNAME", nullable = false)
    private String displayName;

    // internal description of the group, intended to ease management
    @Column(name = "DESCRIPTION", nullable = false)
    private String description;

    // flag indicating whether group is public
    @Column(name = "ISPUBLIC", nullable = false)
    private boolean publicGroup;

    // -------------------------------------------------
    // Constructor
    // -------------------------------------------------

    public Group() {
        // default constructor required for JPA
    }

    // simple constructor, reuse name for other properties
    public Group(final String name, final boolean isPublic) {
        this.name = name;
        this.displayName = name;
        this.description = name;
        this.publicGroup = isPublic;
    }

    // simple constructor, separate display name
    public Group(final String name, final String displayName, final boolean isPublic) {
        this.name = name;
        this.displayName = displayName;
        this.description = displayName;
        this.publicGroup = isPublic;
    }

    public Group(final String name, final String displayName, final String description, final boolean isPublic) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.publicGroup = isPublic;
    }

    // -------------------------------------------------
    // Getters & setters
    // -------------------------------------------------

    @Generated
    public Long getId() {
        return id;
    }

    @Generated
    public void setId(final Long newId) {
        this.id = newId;
    }

    @Generated
    public String getName() {
        return name;
    }

    @Generated
    public void setName(final String name) {
        this.name = name;
    }

    @Generated
    public String getDisplayName() {
        return displayName;
    }

    @Generated
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    @Generated
    public String getDescription() {
        return description;
    }

    @Generated
    public void setDescription(final String description) {
        this.description = description;
    }

    @Generated
    public boolean isPublicGroup() {
        return publicGroup;
    }

    @Generated
    public void setPublicGroup(final boolean publicGroup) {
        this.publicGroup = publicGroup;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, name, displayName, description, publicGroup);
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
        final Group other = (Group) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.name, other.name) &&
                Objects.equals(this.displayName, other.displayName) &&
                Objects.equals(this.description, other.description) &&
                Objects.equals(this.publicGroup, other.publicGroup);
    }
}
