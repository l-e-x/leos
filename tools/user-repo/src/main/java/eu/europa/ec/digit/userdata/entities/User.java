/*
 * Copyright 2017 European Commission
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

import javax.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "LEOS_USER")
public class User implements Serializable {

    private static final long serialVersionUID = -242509624358432413L;
    
    @Id
    @Column(name = "USER_LOGIN", nullable = false, insertable = false, updatable = false)
    private String login;
    
    @Column(name = "USER_PER_ID", nullable = false, insertable = false, updatable = false)
    private Long perId;

    @Column(name = "USER_LASTNAME", nullable = false, insertable = false, updatable = false)
    private String lastName;

    @Column(name = "USER_FIRSTNAME", nullable = false, insertable = false, updatable = false)
    private String firstName;

    @Column(name = "USER_DG", nullable = false, insertable = false, updatable = false)
    private String dg;

    @Column(name = "USER_EMAIL", nullable = false, insertable = false, updatable = false)
    private String email;
    
    @JsonIgnore
    @OneToMany(fetch=FetchType.EAGER)
    @JoinTable(name = "LEOS_USER_ROLE", joinColumns = @JoinColumn(name = "USER_LOGIN"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_NAME"))
    private List<Role> roleEntities;

    public User() {
    }

    public User(String login, Long perId, String lastName, String firstName, String dg, String email, List<Role> roleEntities) {
        this.login = login;
        this.perId = perId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.dg = dg;
        this.email = email;
        this.roleEntities = roleEntities;
    }

    public String getLogin() {
        return login;
    }

    public Long getPerId() {
        return perId;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getDg() {
        return dg;
    }
    public String getEmail() {
        return email;
    }

    public List<Role> getRoleEntities() {
        return roleEntities;
    }

    public List<String> getRoles() {
        List<String> roles = new ArrayList<>();
        this.roleEntities.forEach(r -> roles.add(r.getRole()));
        return roles;
    }
}
