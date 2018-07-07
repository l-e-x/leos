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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.europa.ec.leos.domain.common.LeosAuthority;

class LeosPermissionAuthorityMap {

    private final static Map<LeosAuthority, Set<LeosPermission>> permissionMap = new HashMap<>();

    static {
        final Set<LeosPermission> ownerPermissions = new HashSet<>();
        ownerPermissions.add(LeosPermission.CAN_READ);
        ownerPermissions.add(LeosPermission.CAN_UPDATE);
        ownerPermissions.add(LeosPermission.CAN_DELETE);
        ownerPermissions.add(LeosPermission.CAN_COMMENT);
        ownerPermissions.add(LeosPermission.CAN_SHARE);
        ownerPermissions.add(LeosPermission.CAN_PRINT_LW);
        permissionMap.put(LeosAuthority.OWNER, ownerPermissions);

        final Set<LeosPermission> collaboratorPermissions = new HashSet<>();
        collaboratorPermissions.add(LeosPermission.CAN_READ);
        collaboratorPermissions.add(LeosPermission.CAN_UPDATE);
        collaboratorPermissions.add(LeosPermission.CAN_COMMENT);
        permissionMap.put(LeosAuthority.CONTRIBUTOR, collaboratorPermissions);

        final Set<LeosPermission> reviewerPermissions = new HashSet<>();
        reviewerPermissions.add(LeosPermission.CAN_READ);
        reviewerPermissions.add(LeosPermission.CAN_COMMENT);
        permissionMap.put(LeosAuthority.REVIEWER, reviewerPermissions);
        
        final Set<LeosPermission> supportPermission = new HashSet<>();
        supportPermission.add(LeosPermission.CAN_READ);
        supportPermission.add(LeosPermission.CAN_UPDATE);
        supportPermission.add(LeosPermission.CAN_DELETE);
        supportPermission.add(LeosPermission.CAN_COMMENT);
        supportPermission.add(LeosPermission.CAN_SHARE);
        supportPermission.add(LeosPermission.CAN_PRINT_LW);
        permissionMap.put(LeosAuthority.SUPPORT, supportPermission);
        
        final Set<LeosPermission> adminPermission = new HashSet<>();
        adminPermission.add(LeosPermission.CAN_READ);
        adminPermission.add(LeosPermission.CAN_UPDATE);
        adminPermission.add(LeosPermission.CAN_DELETE);
        adminPermission.add(LeosPermission.CAN_COMMENT);
        adminPermission.add(LeosPermission.CAN_SHARE);
        adminPermission.add(LeosPermission.CAN_PRINT_LW);
        permissionMap.put(LeosAuthority.ADMIN, adminPermission);
    }

    static Set<LeosPermission> getPermissions(LeosAuthority authority) {
        return permissionMap.get(authority);
    }
}
