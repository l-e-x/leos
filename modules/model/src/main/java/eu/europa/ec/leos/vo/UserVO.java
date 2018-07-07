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
package eu.europa.ec.leos.vo;

import eu.europa.ec.leos.model.user.User;

/* to send data to Client side*/
public class UserVO {
    private String id;
    private String name;
    private String dg;

    public UserVO() {
    }

    public UserVO(User user) {
        this.id=user.getLogin();
        this.name=user.getName();
        this.dg=user.getDepartment().getDepartmentId();
    }

    public UserVO(String id, String name, String dg) {
        this.id = id;
        this.name = name;
        this.dg = dg;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDg() {
        return dg;
    }

    public void setDg(String dg) {
        this.dg = dg;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserVO)) return false;
        if (getId()==null) return false;

        UserVO userVO = (UserVO) o;

        return getId().equals(userVO.getId());

    }

    @Override public int hashCode() {
        return (getId()==null)
                ? 0
                : getId().hashCode();

    }
}
