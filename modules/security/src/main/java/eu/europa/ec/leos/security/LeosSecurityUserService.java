/*
 * Copyright 2018 European Commission
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
package eu.europa.ec.leos.security;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.integration.UsersProvider;
import eu.europa.ec.leos.integration.rest.UserJSON;
import eu.europa.ec.leos.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesUserDetailsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
public class LeosSecurityUserService extends PreAuthenticatedGrantedAuthoritiesUserDetailsService {

    final private UsersProvider userProvider;

    @Autowired
    public LeosSecurityUserService(UsersProvider userProvider) {
        this.userProvider = userProvider;
    }

    /**
     * Overridden method to allow population of user details in Security context
     * @param token the authentication request token
     * @param authorities the pre-authenticated authorities.
     */
    @Override
    protected UserDetails createUserDetails(Authentication token, Collection<? extends GrantedAuthority> authorities) {
        try {
            User user = userProvider.getUserByLogin(token.getName());
            AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
            List<GrantedAuthority> allAuthorities = new ArrayList<GrantedAuthority>();
            /*Add all the authorities {application specific + ecas specific} to the AuthenticatedUser object
             *as a collection of {GrantedAuthority}
            */
            if(user instanceof UserJSON) {
                List<LeosAuthority> leosAuthorities = ((UserJSON) user).getAuthorities();
                leosAuthorities.forEach(auth -> {
                    allAuthorities.add(new SimpleGrantedAuthority(auth.name()));   
                });
            }
            allAuthorities.addAll(authorities);
            authenticatedUser.setAuthorities(allAuthorities);
            return authenticatedUser;
        } catch (Exception ex) {
            throw new UsernameNotFoundException("Cannot retrieve the userDetails", ex);
        }
    }
}