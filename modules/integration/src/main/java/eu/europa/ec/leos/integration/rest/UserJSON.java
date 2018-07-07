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
package eu.europa.ec.leos.integration.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.model.user.User;

import java.util.List;

public class UserJSON extends User {

    private String lastName;
    private String firstName;
    private List<LeosAuthority> authorities;

    @JsonCreator
    public UserJSON(@JsonProperty("login") String login, @JsonProperty("perId") Long perId, @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
            @JsonProperty("dg") String dg, @JsonProperty("email") String email, @JsonProperty("roles") List<LeosAuthority> authorities) {
        super(perId, login,  lastName + " " + firstName, dg, email);

        this.lastName = lastName;
        this.firstName = firstName;
        this.authorities = authorities;
    }
    
    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public List<LeosAuthority> getAuthorities() {
        return authorities;
    }
}
