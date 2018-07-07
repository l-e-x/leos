/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.user;

import eu.europa.ec.leos.model.content.LeosObjectProperties;
import eu.europa.ec.leos.model.user.Permission;
import eu.europa.ec.leos.model.user.Role;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    @Override
    public List<Permission> getPermissions (User user, LeosObjectProperties leosObjectProperties) {
        return getPermissions(getRoles(user, leosObjectProperties));
    }

    @Override
    public boolean hasPermission(User user, LeosObjectProperties leosObjectProperties, Permission permission) {
        for (Permission perm : getPermissions(user, leosObjectProperties)) {
            if (perm.equals(permission))
                return true;
        }
        return false;
    }

    private List<Role> getRoles (User user, LeosObjectProperties leosObjectProperties) {
        // handle user == null and leosDocument == null
        List<Role> list = new ArrayList<>();
        if (user==null || leosObjectProperties==null) {
            return list;
        }
        if (user.getLogin()!=null && (leosObjectProperties.getAuthor()!=null || leosObjectProperties.getContributors().size()>0)) {
            if (user.getLogin().equals(leosObjectProperties.getAuthor().getId())) {
                list.add(Role.AUTHOR);
            }
            for (UserVO contributor : leosObjectProperties.getContributors()) {
                if (user.getLogin().equals(contributor.getId())) {
                    list.add(Role.CONTRIBUTOR);
                    break;
                }
            }
        }
        return list;
    }

    private List<Permission> getPermissions (List<Role> roles) {
        List<Permission> list = new ArrayList<>();
        for (Role role : roles) {
            list.addAll(role.getPermissions());
        }
        return list;
    }

}
