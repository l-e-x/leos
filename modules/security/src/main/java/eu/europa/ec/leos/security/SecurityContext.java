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
package eu.europa.ec.leos.security;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/* This class is just a indirection to not to replicate code to get user at different places
   SecurityContextHolder manages context for different threads. So it would return respective user for the thread*/
@Component
public class SecurityContext {
    private LeosPermissionEvaluator leosPermissionEvaluator;

    private LeosHierarchicalRoleEvaluator leosHierarchicalRoleEvaluator;

    @Autowired
    public SecurityContext(LeosPermissionEvaluator leosPermissionEvaluator, LeosHierarchicalRoleEvaluator leosHierarchicalRoleVoter){
        this.leosPermissionEvaluator = leosPermissionEvaluator;
        this.leosHierarchicalRoleEvaluator = leosHierarchicalRoleVoter;
    }

    public User getUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public boolean isUserAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
    }

    public boolean hasPermission(Object domainObject, LeosPermission permission){
        return leosPermissionEvaluator.hasPermission(SecurityContextHolder.getContext().getAuthentication(),
                                                        domainObject,
                                                        permission.name());
    }
    
    public boolean hasRole(LeosAuthority authority) {
        //FIXME: Call Spring's SecurityExpressionRoot method directly??
        return leosHierarchicalRoleEvaluator.hasRole(SecurityContextHolder.getContext().getAuthentication(), authority.name());
    }

}
