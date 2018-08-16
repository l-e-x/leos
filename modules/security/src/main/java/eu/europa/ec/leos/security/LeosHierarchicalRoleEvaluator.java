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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Component
class LeosHierarchicalRoleEvaluator {
    private static final Logger LOG = LoggerFactory.getLogger(LeosHierarchicalRoleEvaluator.class);

    private RoleHierarchy roleHierarchy;

    @Autowired
    public LeosHierarchicalRoleEvaluator(RoleHierarchy roleHierarchy) {
        this.roleHierarchy = roleHierarchy;
    }

    public boolean hasRole(Authentication authentication, String authority) {
        boolean result = false;
        List<ConfigAttribute> configAttributes = SecurityConfig.createListFromCommaDelimitedString(authority);
        LeosAuthority leosAuthority = retrieveAuthority(configAttributes);
        if (leosAuthority != null) {
            // Attempt to find a matching granted authority
            for (GrantedAuthority grantedAuthority : retrieveUserAuthorities(authentication)) {
                if (grantedAuthority.getAuthority().equals(leosAuthority.name())) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    private Collection<? extends GrantedAuthority> retrieveUserAuthorities(Authentication authentication) {
        return this.roleHierarchy.getReachableGrantedAuthorities(authentication.getAuthorities());
    }

    private LeosAuthority retrieveAuthority(Collection<ConfigAttribute> attributes) {
        LeosAuthority leosAuthority = null;
        if (attributes != null) {
            Iterator<ConfigAttribute> iterator = attributes.iterator();
            if (iterator.hasNext()) {// we expect only one authority check
                leosAuthority = getLeosAuthority(iterator.next().getAttribute());
            }
        }
        return leosAuthority;
    }

    private LeosAuthority getLeosAuthority(String authority) {
        try {
            return LeosAuthority.valueOf(authority);
        } catch (IllegalArgumentException e) {
            LOG.debug("Illegal Argument in LeosAuthority...ignoring & returning null: " + e);
            return null;
        }
    }
}
