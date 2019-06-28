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
package eu.europa.ec.leos.integration.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.security.SecurityUser;

import java.util.List;

public class UserJSON extends User implements SecurityUser {

    private String lastName;
    private String firstName;
    private List<String> roles;

    @JsonCreator
    public UserJSON(@JsonProperty("login") String login, @JsonProperty("perId") Long perId, @JsonProperty("firstName") String firstName, @JsonProperty("lastName") String lastName,
            @JsonProperty("entity") String entity, @JsonProperty("email") String email, @JsonProperty("roles") List<String> roles) {
        super(perId, login,  lastName + " " + firstName, entity, email,roles);

        this.lastName = lastName;
        this.firstName = firstName;
        this.roles = roles;
    }
    
    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public List<String> getRoles() {
        return roles;
    }
}