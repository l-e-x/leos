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
package eu.europa.ec.leos.repositories.user;

import eu.europa.ec.leos.model.BaseEntity;
import eu.europa.ec.leos.model.user.User;
import eu.europa.ec.leos.test.support.repositories.AbstractLeosRepositoryTest;
import eu.europa.ec.leos.test.support.repositories.ColumnAndValueCondition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static eu.europa.ec.leos.test.support.model.ModelHelper.buildUser;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

public class UserRepositoryTest extends AbstractLeosRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Before
    public void setup() throws Exception {
        User leosUser = buildUser(1L, "leosUsr", "A leos user");
        insertDbHelper.insertUser(leosUser);
    }

    @Test
    public void test_findByLoginAndState_should_findOneResult() throws Exception {
        // Test that the user inserted on setup is correctly inserted
        // This is a demo for the count rows usage
        int rows = readerDbHelper.countRows("LEOS_USER",
                new ColumnAndValueCondition("USR_LOGIN", "=", "leosUsr"),
                new ColumnAndValueCondition("USR_STATE", "=", "A"));
        assertThat(rows, is(1));
    }

    @Test
    public void test_findByLoginAndState_should_returnValidaUser_when_userIsActive() throws Exception {

        User user2 = buildUser(2L, "otherUsr", "Another leos user");
        insertDbHelper.insertUser(user2);

        //DO THE ACTUAL TEST CALL
        User actualUser = userRepository.findByLoginAndState("otherUsr", User.State.A);

        assertThat(actualUser, is(notNullValue()));
        assertThat(actualUser.getId(), is(user2.getId()));
    }

    @Test
    public void test_findByLoginAndState_should_returnValidaUser_when_userIsPassive() throws Exception {

        User user2 = buildUser(2L, "otherUsr", "Another leos user");
        user2.setState(BaseEntity.State.P);
        insertDbHelper.insertUser(user2);

        //DO THE ACTUAL TEST CALL
        User actualUser = userRepository.findByLoginAndState("otherUsr", User.State.P);

        assertThat(actualUser, is(notNullValue()));
        assertThat(actualUser.getId(), is(user2.getId()));
    }

    @Test
    public void test_findByLoginAndState_should_returnNull_when_userNotFound() throws Exception {

        //DO THE ACTUAL TEST CALL
        User actualUser = userRepository.findByLoginAndState("leosUsr", User.State.P);

        assertThat(actualUser, is(nullValue()));
    }

    @Test
    public void test_findAll() throws Exception {
        User user2 = buildUser(2L, "leosUsr2", "A leos user2");
        insertDbHelper.insertUser(user2);
        List<User> list = userRepository.findAll();
        assertThat(list.size(), is(2));
        User user3 = buildUser(3L, "leosUsr3", "A leos user3");
        insertDbHelper.insertUser(user3);
        list = userRepository.findAll();
        assertThat(list.size(), is(3));
    }

}
