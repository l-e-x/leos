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
package eu.europa.ec.leos.annotate.model;

import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;

import java.util.Objects;

/**
 * Class for bundling user information, which was retrieved from various input
 */
public class UserInformation {

    private String login;             // the user's login
    private String authority;         // authority of the user
    private Token currentToken;       // token found for the user
    private User user;                // user information from our database
    private UserDetails userDetails;  // user details retrieved from UD-repo
    private String clientId;          // browser generated id

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public UserInformation(final String login, final String authority) {
        this.login = login;
        this.authority = authority;
    }

    public UserInformation(final Token token) {
        this.currentToken = token;
        if (token != null) {
            this.user = token.getUser();
            this.authority = token.getAuthority();
            this.login = this.user.getLogin();
        }
    }

    public UserInformation(final User user, final String authority) {
        this.user = user;
        if (user != null) {
            this.login = this.user.getLogin();
        }
        this.authority = authority;
    }

    @SuppressWarnings("PMD.ConfusingTernary")
    public UserInformation(final User user, final UserDetails userDetails) {
        this.userDetails = userDetails;
        this.user = user;
        if (user != null) {
            this.login = user.getLogin();
        } else if(userDetails != null) {
            this.login = userDetails.getLogin();
        }
    }

    // assemble the hypothesis user account based on the given user information
    public String getAsHypothesisAccount() {
        
        return "acct:" + login + "@" + authority;
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

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(final String authority) {
        this.authority = authority;
    }

    public Token getCurrentToken() {
        return currentToken;
    }

    public void setCurrentToken(final Token currentToken) {
        this.currentToken = currentToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public UserDetails getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(final UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(login, authority, currentToken, user, userDetails);
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
        final UserInformation other = (UserInformation) obj;
        return Objects.equals(this.authority, other.authority) &&
                Objects.equals(this.login, other.login) &&
                Objects.equals(this.currentToken, other.currentToken) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.userDetails, other.userDetails);
    }
}
