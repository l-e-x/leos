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
package eu.europa.ec.leos.web.model;

import eu.europa.ec.leos.domain.common.LeosAuthority;

public class CollaboratorVO {
    private UserVO user;
    private LeosAuthority leosAuthority;

    public CollaboratorVO(UserVO user, LeosAuthority leosAuthority) {
        this.user = user;
        this.leosAuthority = leosAuthority;
    }

    public LeosAuthority getLeosAuthority() {
        return leosAuthority;
    }

    public void setLeosAuthority(LeosAuthority leosAuthority) {
        this.leosAuthority = leosAuthority;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}
