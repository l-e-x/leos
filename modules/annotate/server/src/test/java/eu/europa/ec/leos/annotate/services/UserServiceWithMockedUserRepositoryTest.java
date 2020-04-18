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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.exceptions.DefaultGroupNotFoundException;
import eu.europa.ec.leos.annotate.services.exceptions.GroupAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.exceptions.UserAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.impl.UserServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class UserServiceWithMockedUserRepositoryTest {

    /**
     * Test cases on the UserService; executed using mocked UserRepository to simulate desired internal behavior 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // the UserRepository used inside the UserService is mocked
    @Mock
    private UserRepository userRepos;

    @InjectMocks
    private UserServiceImpl userService;

    @Autowired
    private GroupRepository groupRepos;

    // used inside the UserService, needs to be available -> mock!
    @Mock
    private GroupService groupService;

    @Before
    public void setupTests() {

        MockitoAnnotations.initMocks(this);

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() {

        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test that an expected exception is given when persisting a User internally throws an unexpected exception
     */
    @Test(expected = UserAlreadyExistingException.class)
    public void testSavingUserThrowsExceptionInternally() throws UserAlreadyExistingException, DefaultGroupNotFoundException {

        final String login = "evil";

        final User evilUser = new User(login);
        Mockito.when(userRepos.save(evilUser)).thenThrow(new RuntimeException());

        // save default group
        TestDbHelper.insertDefaultGroup(groupRepos);

        userService.createUser(login);
    }

    /**
     * test that adding a user to a new group (based on user's associate entity) does not throw an exception
     * when the service internally encounters an exception while creating the group
     */
    @Test
    public void testAddingUserToEntityGroupThrowsExceptionInternally() throws GroupAlreadyExistingException {

        final String entityName = "entity";
        final String login = "evil";

        final User evilUser = new User(login);
        final UserDetails details = new UserDetails(login, Long.valueOf(8), "first", "last", 
                Arrays.asList(new UserEntity("4", entityName, entityName)), "", null);

        Mockito.when(groupService.findGroupByName(entityName)).thenReturn(null);
        Mockito.when(groupService.createGroup(entityName, false)).thenThrow(new RuntimeException());

        // verify that adding a user to the entity group simply returns false,
        // even though exceptions are thrown internally
        Assert.assertFalse(userService.addUserToEntityGroup(new UserInformation(evilUser, details)));
    }
}
