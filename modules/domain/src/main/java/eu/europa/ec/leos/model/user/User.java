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
package eu.europa.ec.leos.model.user;

import java.util.List;

public class User {

    private Long id;

    private String login;

    private String name;

    private String entity;

    private String email;
    
    private List<String> roles;

	public User(Long id, String login, String name, String entity, String email,List<String> roles) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.entity = entity;
        this.email = email;
        this.roles = roles;
    }

    public Long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getName() {
        return name;
    }

    public String getEntity() {
        return entity;
    }

    public String getEmail() {
        return email;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    //Ideally it should not be possible to change the login once created.
    private void setLogin(String login) {
        this.login = login;
    }

    protected void setName(String name) {
        this.name = name;
    }

    protected void setEntity(String entity) {
        this.entity = entity;
    }
    public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("id=").append(id);
        sb.append(", login='").append(login).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", entity='").append(entity).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        if (getLogin() == null) return false;

        User user = (User) o;

        return getLogin().equals(user.getLogin());
    }

    @Override
    public int hashCode() {
        //Reason for using login: In OS release, we set Id as 0 for all users. so login is better contender
        return ((getLogin() == null) ? 0 : getLogin().hashCode());
    }
}
