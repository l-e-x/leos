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
import eu.europa.ec.leos.annotate.model.UserDetails;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "USERS", uniqueConstraints = @UniqueConstraint(columnNames = {"LOGIN"}))
@SuppressWarnings("PMD.ShortClassName")
public class User {

    /**
     * Class representing a user creating an annotation 
     * note: we only store minimum information, more user details can be fetched from external repository on demand (cfr. {@link UserDetails})
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "USER_ID", nullable = false)
    @GenericGenerator(name = "usersSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "USERS_SEQ"),
            // @Parameter(name = "initial_value", value = "1000"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "usersSequenceGenerator")
    @SuppressWarnings("PMD.ShortVariable")
    private long id;

    @Column(name = "LOGIN", nullable = false)
    private String login;

    @Column(name = "SIDEBAR_TUTORIAL_DISMISSED", nullable = false)
    private boolean sidebarTutorialDismissed;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public User() {
        // default constructor required by JPA
    }

    public User(final String login) {
        this.login = login;
    }

    public User(final String login, final boolean tutorialDismissed) {
        this.login = login;
        this.sidebarTutorialDismissed = tutorialDismissed;
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    public String getLogin() {
        return login;
    }

    public void setLogin(final String login) {
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public void setId(final Long newId) {
        this.id = newId;
    }

    public boolean isSidebarTutorialDismissed() {
        return sidebarTutorialDismissed;
    }

    public void setSidebarTutorialDismissed(final boolean sidebarTutorialDismissed) {
        this.sidebarTutorialDismissed = sidebarTutorialDismissed;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(id, login, sidebarTutorialDismissed);
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
        final User other = (User) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.login, other.login) &&
                Objects.equals(this.sidebarTutorialDismissed, other.sidebarTutorialDismissed);
    }
}
