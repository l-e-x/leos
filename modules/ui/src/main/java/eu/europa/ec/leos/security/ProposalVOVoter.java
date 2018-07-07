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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import eu.europa.ec.leos.domain.common.LeosAuthority;
import eu.europa.ec.leos.web.model.CollaboratorVO;
import eu.europa.ec.leos.web.model.ProposalVO;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
class ProposalVOVoter implements AccessDecisionVoter<ProposalVO> {

    @Override
    public boolean supports(ConfigAttribute attribute) {
        return true;// accept all as string
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ProposalVO.class.isAssignableFrom(clazz);
    }

    @Override
    public int vote(Authentication authentication, ProposalVO proposalVO, Collection<ConfigAttribute> attributes) {
        LeosAuthority authority = retrieveAuthority(authentication, proposalVO);
        LeosPermission permission = retrievePermission(attributes);

        if (authority == null || permission == null) {
            return ACCESS_DENIED;
        }

        Set<LeosPermission> authorityPermissions = LeosPermissionAuthorityMap.getPermissions(authority);
        if (authorityPermissions != null && authorityPermissions.contains(permission)) {
            return ACCESS_GRANTED;
        }
        else {
            return ACCESS_DENIED;
        }
    }

    private LeosAuthority retrieveAuthority(Authentication authentication, ProposalVO proposalVO) {
        String userLogin = ((AuthenticatedUser) authentication.getPrincipal()).getLogin();
        LeosAuthority authority = null;
        for (CollaboratorVO collaborator : proposalVO.getCollaborators()) {
            if (userLogin.equals(collaborator.getUser().getLogin())) {
                authority = collaborator.getLeosAuthority();
                break;//we are expecting only one role per user
            }
        }
        return authority;
    }

    private LeosPermission retrievePermission(Collection<ConfigAttribute> attributes) {
        LeosPermission permission = null;
        if (attributes != null) {
            Iterator<ConfigAttribute> iterator = attributes.iterator();
            if (iterator.hasNext()) {// we expect only one permission check
                permission = LeosPermission.valueOf(iterator.next().getAttribute());
            }
        }
        return permission;
    }
}
