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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.controllers.AuthApiController;
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.entity.*;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.model.web.token.JsonTokenResponse;
import eu.europa.ec.leos.annotate.repository.*;
import eu.europa.ec.leos.annotate.services.AuthenticationServiceWithTestFunctions;
import eu.europa.ec.leos.annotate.services.UserServiceWithTestFunctions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class GetTokenTest {

    private final static String LOGIN1 = "demouser"; // note: choose a user name not contained in our dummy UD-repo!

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() throws Exception {

        TestDbHelper.cleanupRepositories(this);

        DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() throws Exception {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AuthenticationServiceWithTestFunctions authService;

    @Autowired
    private GroupRepository groupRepos;

    @Autowired
    private UserGroupRepository userGroupRepos;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private AuthClientRepository authClientRepos;

    @Autowired
    private TokenRepository tokenRepos;

    @Autowired
    private UserServiceWithTestFunctions userService;

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    // URL to the external user repository
    @Value("${user.repository.url}")
    private String repositoryUrl;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * try to authenticate using token, with user being registered in UD-repo; expected HTTP 200 and tokens
     */
    @Test
    public void testGetAccessTokenSuccessful() throws Exception {

        final String clientId = "clientId12";

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // register client
        authClientRepos.save(new AuthClient("desc", "thesecret", clientId, "auth"));

        // mock the RestTemplate and inject it into the UserService
        RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        userService.setRestTemplate(restOperations);

        Map<String, String> params = new HashMap<String, String>();
        params.put("userId", LOGIN1);

        // prepare Mockito to return the desired user details
        UserDetails details = new UserDetails(LOGIN1, (long) 45, "Santa", "Clause", "DIGIT", "santa@clause.europa.eu", null);
        Mockito.when(restOperations.getForObject(repositoryUrl, UserDetails.class, params)).thenReturn(details);

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=" + authService.createToken(LOGIN1, clientId));

        // create our own ApiController and hand over required (partly mocked) services
        MockHttpServletResponse mockResp = new MockHttpServletResponse();
        AuthApiController controller = new AuthApiController(userService, authService);
        ResponseEntity<Object> result = controller.getToken(builder.buildRequest(this.wac.getServletContext()), mockResp);

        // expected: Http 200
        Assert.assertEquals(HttpStatus.OK, result.getStatusCode());

        // check that a JsonTokenResponse was returned
        JsonTokenResponse jsResponse = (JsonTokenResponse) result.getBody();
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getAccess_token()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getRefresh_token()));
    }

    /**
     * try to authenticate using token, but user is not registered in UD-repo; expected HTTP 400 and failure notice
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetAccessToken_UserUnknownInUdRepo() throws Exception {

        final String clientId = "cl1€nt";

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // register calling client
        authClientRepos.save(new AuthClient("desc", "secret1234", clientId, "authority"));

        // create user in default group
        User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=" + authService.createToken(LOGIN1, clientId));
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getUnknownUserResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try to receive a refresh token; expected HTTP 200 and new tokens
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetRefreshTokenSuccessful() throws Exception {

        final String refreshToken = UUID.randomUUID().toString();

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create user in default group
        User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(user, "acc", LocalDateTime.now(), refreshToken, LocalDateTime.now().plusMinutes(2)));

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=refresh_token&refresh_token=" + refreshToken);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isOk());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonTokenResponse jsResponse = SerialisationHelper.deserializeJsonTokenResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getAccess_token()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getRefresh_token()));

        // verify that the new tokens are saved for the user
        Token foundToken = tokenRepos.findByRefreshToken(jsResponse.getRefresh_token());
        Assert.assertEquals(jsResponse.getAccess_token(), foundToken.getAccessToken());
        Assert.assertEquals(user.getId().longValue(), foundToken.getUserId());
    }

    /**
     * try to receive a refresh token, but the token is not registered for any user; expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetRefreshToken_RefreshTokenUnknown() throws Exception {

        final String refreshToken = UUID.randomUUID().toString();

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create user in default group, but do not save a refresh token for him
        User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=refresh_token&refresh_token=" + refreshToken);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getInvalidRefreshTokenResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }
    
    /**
     * try to receive a refresh token, but the token registered for the user is expired already; expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetRefreshToken_RefreshTokenExpired() throws Exception {

        final String refreshToken = UUID.randomUUID().toString();

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create user in default group, and save a refresh token for him that is already expired
        User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        
        tokenRepos.save(new Token(user, "acc", LocalDateTime.now(), refreshToken, 
                LocalDateTime.now().minusMinutes(2))); // expired 

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=refresh_token&refresh_token=" + refreshToken);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getRefreshTokenExpiredResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but using unsupported grant_type; expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetToken_UnsupportedGrantType() throws Exception {

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=somegrant");
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getUnsupportedGrantTypeResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but without specifying grant_type; expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetToken_NoGrantType() throws Exception {

        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token");
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getInvalidRequestResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but JWT token cannot be decoded by any registered client; expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetToken_UnknownClient() throws Exception {

        final String tempClientId = "tempClient";

        // register client to create token, then unregister the client again
        authClientRepos.save(new AuthClient("desc", "thesecret", tempClientId, "auth"));
        String tokenToUse = authService.createToken(LOGIN1, tempClientId);
        authClientRepos.deleteAll();
        
        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=" + tokenToUse);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse); 
        Assert.assertEquals(JsonAuthenticationFailure.getUnknownClientResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }
    
    /**
     * try a /token request, but JWT token is issued for an authority that the registered client may not authenticate; expected HTTP 400
     */
    @SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", justification = "initialised in before-test setup function called by junit")
    @Test
    public void testGetToken_ClientNotAuthorizedForAuthority() throws Exception {

        final String tempClientId = "tempClient";

        // register client to create token, then change the authorities that the registered client may authenticate
        AuthClient theClient = new AuthClient("desc", "thesecret", tempClientId, "auth");
        theClient = authClientRepos.save(theClient);
        String tokenToUse = authService.createToken(LOGIN1, tempClientId);

        theClient.setAuthorities("anotherAuthority");
        authClientRepos.save(theClient);
        
        // send token request
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=" + tokenToUse);
        ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        MvcResult resultContent = result.andReturn();
        String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getTokenInvalidForClientAuthorityResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }
}
