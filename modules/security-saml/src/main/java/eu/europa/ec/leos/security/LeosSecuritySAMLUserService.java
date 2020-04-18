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
package eu.europa.ec.leos.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.saml.SAMLCredential;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.stereotype.Service;

import eu.europa.ec.leos.model.user.User;

@Service
public class LeosSecuritySAMLUserService implements SAMLUserDetailsService {

    final private SecurityUserProvider userProvider;

    @Autowired
    public LeosSecuritySAMLUserService(SecurityUserProvider userProvider) {
        this.userProvider = userProvider;
    }

    @Override
    public UserDetails loadUserBySAML(SAMLCredential credential) throws UsernameNotFoundException {
        try {
            User user = userProvider.getUserByLogin(credential.getNameID().getValue());
            AuthenticatedUser authenticatedUser = new AuthenticatedUser(user);
            List<GrantedAuthority> allRoles = new ArrayList<GrantedAuthority>();
            if (user instanceof SecurityUser) {
                List<String> leosRoles = ((SecurityUser) user).getRoles();
                leosRoles.forEach(auth -> {
                 allRoles.add(new SimpleGrantedAuthority(auth));
                });
            }
            authenticatedUser.setAuthorities(allRoles);
            return authenticatedUser;
        } catch (Exception ex) {
            throw new UsernameNotFoundException("Cannot retrieve the userDetails", ex);
        }
    }
}
