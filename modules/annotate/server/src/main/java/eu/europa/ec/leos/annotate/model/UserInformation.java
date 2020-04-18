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

import eu.europa.ec.leos.annotate.Authorities;
import eu.europa.ec.leos.annotate.Generated;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.search.Consts;
import eu.europa.ec.leos.annotate.model.search.Consts.SearchUserType;

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
    private String connectedEntity;   // entity from which the user connects (used in ISC)
    private Consts.SearchUserType searchUser = Consts.SearchUserType.Unknown; // type of user executing the search (relevant for ISC)

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------
    public UserInformation(final String login, final String authority) {
        this.login = login;
        setAuthorityIntern(authority);
    }

    public UserInformation(final Token token) {
        this.currentToken = token;
        if (token != null) {
            this.user = token.getUser();
            setAuthorityIntern(token.getAuthority());
            this.login = this.user.getLogin();
        }
    }

    public UserInformation(final User user, final String authority) {
        this.user = user;
        if (user != null) {
            this.login = this.user.getLogin();
        }
        setAuthorityIntern(authority);
    }

    @SuppressWarnings("PMD.ConfusingTernary")
    public UserInformation(final User user, final UserDetails userDetails) {
        this.userDetails = userDetails;
        this.user = user;
        if (user != null) {
            this.login = user.getLogin();
        } else if (userDetails != null) {
            this.login = userDetails.getLogin();
        }
    }

    // assemble the hypothesis user account based on the given user information
    public String getAsHypothesisAccount() {

        return "acct:" + login + "@" + authority;
    }

    private void updateSearchUser() {

        if (Authorities.isLeos(this.authority)) {
            this.searchUser = SearchUserType.EdiT;
        } else if (Authorities.isIsc(this.authority)) {
            this.searchUser = SearchUserType.ISC;
        }
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------

    @Generated
    public String getLogin() {
        return login;
    }

    @Generated
    public void setLogin(final String login) {
        this.login = login;
    }

    @Generated
    public String getAuthority() {
        return authority;
    }

    @Generated
    public void setAuthority(final String authority) {
        setAuthorityIntern(authority);
    }

    // PMD conform in order to avoid calling the overridable setAuthority method by other methods
    private void setAuthorityIntern(final String authority) {
        this.authority = authority;

        updateSearchUser();
    }

    @Generated
    public Token getCurrentToken() {
        return currentToken;
    }

    @Generated
    public void setCurrentToken(final Token currentToken) {
        this.currentToken = currentToken;
    }

    @Generated
    public User getUser() {
        return user;
    }

    @Generated
    public void setUser(final User user) {
        this.user = user;
    }

    @Generated
    public UserDetails getUserDetails() {
        return userDetails;
    }

    @Generated
    public void setUserDetails(final UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    @Generated
    public String getClientId() {
        return clientId;
    }

    @Generated
    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    @Generated
    public String getConnectedEntity() {
        return connectedEntity;
    }

    @Generated
    public void setConnectedEntity(final String entity) {
        this.connectedEntity = entity;
    }

    @Generated
    public void setSearchUser(final Consts.SearchUserType searchUser) {
        this.searchUser = searchUser;
    }

    @Generated
    public Consts.SearchUserType getSearchUser() {
        return searchUser;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Generated
    @Override
    public int hashCode() {
        return Objects.hash(login, authority, currentToken, user, userDetails, connectedEntity, searchUser);
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
                Objects.equals(this.userDetails, other.userDetails) &&
                Objects.equals(this.connectedEntity, other.connectedEntity) &&
                Objects.equals(this.searchUser, other.searchUser);
    }

}
