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
package eu.europa.ec.leos.model.user;

public class User {

    private Long id;

    private String login;

    private String name;

    private String dg;

    private String email;

    public User(Long id, String login, String name, String dg, String email) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.dg = dg;
        this.email = email;
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

    public String getDg() {
        return dg;
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

    protected void setDg(String dg) {
        this.dg = dg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("id=").append(id);
        sb.append(", login='").append(login).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", dg='").append(dg).append('\'');
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
