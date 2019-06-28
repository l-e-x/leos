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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.controllers.AuthApiController;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.entity.AuthClient;
import eu.europa.ec.leos.annotate.model.entity.Group;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.model.entity.UserGroup;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.UserService;
import eu.europa.ec.leos.annotate.services.impl.AuthenticationServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class GetTokenWithUserServiceMockTest {

 // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private AuthClientRepository authClientRepos;

    @Autowired
    private WebApplicationContext wac;

    // we are mocking the user service to achieve desired behavior
    @Mock
    private UserService userService;

    @Autowired
    private AuthenticationServiceImpl authService;

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests()  {

        MockitoAnnotations.initMocks(this);

        TestDbHelper.cleanupRepositories(this);
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * internal exceptions during /token request, expected HTTP 400 and failure response
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testGetToken_UserServiceThrowsExceptionsWhenCreatingUser() throws Exception {

        final String login = "demo";
        final String clientId = "theclient";

        // preparation: save a user and assign it to a group
        final User theUser = new User(login);
        userRepos.save(theUser);

        final UserGroup membership = new UserGroup();
        membership.setUserId(theUser.getId());
        membership.setGroupId(defaultGroup.getId());
        userGroupRepos.save(membership);

        // register client
        authClientRepos.save(new AuthClient("desc", "thesecret", clientId, "auth"));

        // mock configuration: pretend creating user fails
        Mockito.when(userService.createUserIfNotExists(Mockito.anyString())).thenThrow(new RuntimeException());

        // send token retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=" + authService.createToken(login, clientId));

        // create our own ApiController and hand over required (partly mocked) services
        final MockHttpServletResponse mockResp = new MockHttpServletResponse();
        final AuthApiController controller = new AuthApiController(userService, authService);
        final ResponseEntity<Object> result = controller.getToken(builder.buildRequest(this.wac.getServletContext()), mockResp);

        // expected: Http 400
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCodeValue());

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = (JsonAuthenticationFailure) result.getBody();
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * internal exceptions during /token request, expected HTTP 400 and failure response
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testGetToken_UserServiceThrowsExceptionsWhenRetrievingUserDetails() throws Exception {

        final String login = "demo";
        final String clientId = "theclient";

        // preparation: save a user and assign it to a group
        final User theUser = new User(login);
        userRepos.save(theUser);

        final UserGroup membership = new UserGroup();
        membership.setUserId(theUser.getId());
        membership.setGroupId(defaultGroup.getId());
        userGroupRepos.save(membership);

        // register client
        authClientRepos.save(new AuthClient("desc", "thesecret", clientId, "auth"));

        // mock configuration: pretend fetching user details fails 
        Mockito.when(userService.getUserDetailsFromUserRepo(Mockito.anyString())).thenThrow(new RuntimeException());

        // send token retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=" + authService.createToken(login, clientId));

        // create our own ApiController and hand over required (partly mocked) services
        final MockHttpServletResponse mockResp = new MockHttpServletResponse();
        final AuthApiController controller = new AuthApiController(userService, authService);
        final ResponseEntity<Object> result = controller.getToken(builder.buildRequest(this.wac.getServletContext()), mockResp);

        // expected: Http 400
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCodeValue());

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = (JsonAuthenticationFailure) result.getBody();
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }
}
