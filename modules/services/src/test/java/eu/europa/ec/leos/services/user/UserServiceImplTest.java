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
package eu.europa.ec.leos.services.user;

import eu.europa.ec.leos.integration.UsersProvider;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import static org.junit.Assert.*;

public class UserServiceImplTest extends LeosTest {

    @Mock
    private UsersProvider usersClient;
    
    @InjectMocks
    private UserServiceImpl userServiceImpl;

    @Test
    public void test_findBylogin() {
        String user1FirstName = "John";
        String user1LastName = "SMITH";
        String user1Login = "smithj";
        String user1Mail = "smithj@test.com";

        String entity = "Entity";

        String userId = "smithj";
        List<String> roles= new ArrayList<String>();
        roles.add("ADMIN");
        User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entity, user1Mail,roles);

        when(usersClient.getUserByLogin(userId)).thenReturn(user1);

        User result = userServiceImpl.getUser(userId);
        assertNotNull(result);
        assertEquals(result.getLogin(),userId);
        assertEquals(result.getId(), Long.valueOf(1));
        assertEquals(result.getName(), user1LastName + " " + user1FirstName);
        assertEquals(result.getEntity(), entity);
    }

    @Test
    public void test_searchUsersByKey() {
        String user1FirstName = "John";
        String user1LastName = "SMITH";
        String user1Login = "smithj";
        List<String> user1Roles= new ArrayList<String>();
        user1Roles.add("ADMIN");
        String user1Mail = "smithj@test.com";

        String user2FirstName = "Peter";
        String user2LastName = "SURRY";
        String user2Login = "surryp";
        String user2Mail = "surryp@test.com";
        List<String> user2Roles= new ArrayList<String>();
        user1Roles.add("ADMIN");

        String entity = "Entity";

        String searchKey = "smith";
        
        User user1 = new User(1l, user1Login, user1LastName + " " + user1FirstName, entity, user1Mail,user1Roles);
        User user2 = new User(0l, user2Login, user2LastName + " " + user2FirstName, entity, user2Mail,user2Roles);

        String key = searchKey;
        List<User> users = new ArrayList();

        users.add(user1);
        users.add(user2);

        when(usersClient.searchUsers(key)).thenReturn(users);

        List<User> results = userServiceImpl.searchUsersByKey(key);
        assertNotNull(results);
        assertThat(results.size(), is(2));
        
        assertEquals(results.get(0).getLogin(),user1Login);
        assertEquals(results.get(0).getId(), Long.valueOf(1));
        assertEquals(results.get(0).getName(), user1LastName + " " + user1FirstName);
        assertEquals(results.get(0).getEntity(), entity);
        assertEquals(results.get(1).getLogin(),user2Login);
        assertEquals(results.get(1).getId(), Long.valueOf(0));
        assertEquals(results.get(1).getName(), user2LastName + " " + user2FirstName);
        assertEquals(results.get(1).getEntity(), entity);
    }
}
