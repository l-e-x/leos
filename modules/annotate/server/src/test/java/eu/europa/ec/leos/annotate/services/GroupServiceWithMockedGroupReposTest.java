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
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.repository.GroupRepository;
import eu.europa.ec.leos.annotate.services.exceptions.GroupAlreadyExistingException;
import eu.europa.ec.leos.annotate.services.impl.GroupServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class GroupServiceWithMockedGroupReposTest {

    /**
     * Test cases on the GroupService; executed using mocked GroupRepository to simulate desired internal behavior 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // the groupRepository used inside the GroupService is mocked
    @Mock
    private GroupRepository groupRepos;

    @InjectMocks
    private GroupServiceImpl groupService;

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
     * test that an expected exception is given when persisting a Group internally throws an unexpected exception
     */
    @Test(expected = GroupAlreadyExistingException.class)
    public void testSavingGroupThrowsExceptionInternally() throws GroupAlreadyExistingException {

        final String login = "evil";

        final Group evilGroup = new Group(login, true);

        Mockito.when(groupRepos.save(evilGroup)).thenThrow(new RuntimeException());

        groupService.createGroup(login, true);
    }
}
