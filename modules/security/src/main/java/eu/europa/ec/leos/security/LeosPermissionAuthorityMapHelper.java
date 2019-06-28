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

import eu.europa.ec.leos.permissions.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class LeosPermissionAuthorityMapHelper {

    @Autowired
    LeosPermissionAuthorityMap leosPermissionAuthorityMap;


    public List<Role> getCollaboratorRoles() {
        List<Role> collaboratorRoles = new ArrayList<>();
        for (Role role : leosPermissionAuthorityMap.getAllRoles()) {
            if (role.isCollaborator()) {
                collaboratorRoles.add(role);
            }
        }
        return collaboratorRoles;
    }

    public String getRoleForDocCreation() {
        for (Role role : leosPermissionAuthorityMap.getAllRoles()) {
            if (role.isCollaborator() && role.isDefaultDocCreationRole()) {
                return role.getName();
            }
        }
        return null;
    }

    public Role getRoleFromListOfRoles(String roleName) {
        for (Role role : leosPermissionAuthorityMap.getAllRoles()) {
            if (role.getName().equals(roleName)) {
                return role;
            }
        }
        return null;
    }
    
    public String[] getPermissionsForRoles(List<String> authorities) {

        Set<LeosPermission> leosPermission = leosPermissionAuthorityMap.getPermissions(authorities);
        Set<String> leosPermissionValues = new HashSet<>();
        if (leosPermission != null) {
            for (LeosPermission permission : leosPermission) {
                leosPermissionValues.add(permission.name());
            }
        }
        return leosPermissionValues.toArray(new String[0]);

	}

}