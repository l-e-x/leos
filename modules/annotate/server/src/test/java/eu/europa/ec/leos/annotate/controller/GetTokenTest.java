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
import eu.europa.ec.leos.annotate.helper.SerialisationHelper;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.model.UserEntity;
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
import java.util.concurrent.ConcurrentHashMap;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class GetTokenTest {

    private final static String LOGIN1 = "demouser"; // note: choose a user name not contained in our dummy UD-repo!
    private static final String TOKEN_URL = "/api/token?grant_type=jwt-bearer&assertion=";
    
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

    private Group defaultGroup;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        TestDbHelper.cleanupRepositories(this);

        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(this.wac);
        this.mockMvc = builder.build();
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * try to authenticate using token, with user being registered in UD-repo; expected HTTP 200 and tokens
     */
    @Test
    public void testGetAccessTokenSuccessful() throws Exception {

        final String clientId = "clientId12";
        final String authority = "authority";

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // register client
        authClientRepos.save(new AuthClient("descript", "thesecret", clientId, authority));

        // mock the RestTemplate and inject it into the UserService
        final RestTemplate restOperations = Mockito.mock(RestTemplate.class);
        userService.setRestTemplate(restOperations);

        final Map<String, String> params = new ConcurrentHashMap<String, String>();
        params.put("userId", LOGIN1);

        // prepare Mockito to return the desired user details
        final UserEntity entity = new UserEntity("8", "DIGIT", "DIGIT");
        final UserDetails details = new UserDetails(LOGIN1, (long) 45, "Santa", "Clause", Arrays.asList(entity), "santa@clause.europa.eu", null);
        Mockito.when(restOperations.getForObject(repositoryUrl, UserDetails.class, params)).thenReturn(details);

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get(TOKEN_URL + authService.createToken(LOGIN1, clientId));

        // create our own ApiController and hand over required (partly mocked) services
        final MockHttpServletResponse mockResp = new MockHttpServletResponse();
        final AuthApiController controller = new AuthApiController(userService, authService);
        final ResponseEntity<Object> result = controller.getToken(builder.buildRequest(this.wac.getServletContext()), mockResp);

        // expected: Http 200
        Assert.assertEquals(HttpStatus.OK, result.getStatusCode());

        // check that a JsonTokenResponse was returned
        final JsonTokenResponse jsResponse = (JsonTokenResponse) result.getBody();
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getAccess_token()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getRefresh_token()));

        // check that there is a token stored in the database, which is assigned to the user and authority given
        final List<Token> allTokens = (List<Token>) tokenRepos.findAll();
        Assert.assertNotNull(allTokens);
        Assert.assertEquals(1, allTokens.size());

        final Token theToken = allTokens.get(0);
        Assert.assertEquals(LOGIN1, theToken.getUser().getLogin());
        Assert.assertEquals(authority, theToken.getAuthority());
    }

    /**
     * try to authenticate using token, but user is not registered in UD-repo; expected HTTP 400 and failure notice
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetAccessToken_UserUnknownInUdRepo() throws Exception {

        final String clientId = "cl1€nt";

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // register calling client
        authClientRepos.save(new AuthClient("desc", "secret1234", clientId, "authority"));

        // create user in default group
        final User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get(TOKEN_URL + authService.createToken(LOGIN1, clientId));
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getUnknownUserResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try to receive a refresh token; expected HTTP 200 and new tokens
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetRefreshTokenSuccessful() throws Exception {

        final String refreshToken = UUID.randomUUID().toString();
        final String authority = "authority";

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create user in default group
        final User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));
        tokenRepos.save(new Token(user, authority, "acc", LocalDateTime.now(), refreshToken, LocalDateTime.now().plusMinutes(2)));

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=refresh_token&refresh_token=" + refreshToken);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isOk());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonTokenResponse jsResponse = SerialisationHelper.deserializeJsonTokenResponse(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getAccess_token()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getRefresh_token()));

        // verify that the new tokens are saved for the user
        final Token foundToken = tokenRepos.findByRefreshToken(jsResponse.getRefresh_token());
        Assert.assertEquals(jsResponse.getAccess_token(), foundToken.getAccessToken());
        Assert.assertEquals(user.getId().longValue(), foundToken.getUserId());
        Assert.assertEquals(authority, foundToken.getAuthority()); // new refresh token is issued for the same authority
    }

    /**
     * try to receive a refresh token, but the token is not registered for any user; expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetRefreshToken_RefreshTokenUnknown() throws Exception {

        final String refreshToken = UUID.randomUUID().toString();

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create user in default group, but do not save a refresh token for him
        final User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=refresh_token&refresh_token=" + refreshToken);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getInvalidRefreshTokenResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try to receive a refresh token, but the token registered for the user is expired already; expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetRefreshToken_RefreshTokenExpired() throws Exception {

        final String refreshToken = UUID.randomUUID().toString();

        // set default group here to be SpotBugs-conformant
        defaultGroup = TestDbHelper.insertDefaultGroup(groupRepos);

        // create user in default group, and save a refresh token for him that is already expired
        final User user = new User(LOGIN1);
        userRepos.save(user);
        userGroupRepos.save(new UserGroup(user.getId(), defaultGroup.getId()));

        tokenRepos.save(new Token(user, "auth", "acc", LocalDateTime.now(), refreshToken,
                LocalDateTime.now().minusMinutes(2))); // expired

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=refresh_token&refresh_token=" + refreshToken);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getRefreshTokenExpiredResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but using unsupported grant_type; expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetToken_UnsupportedGrantType() throws Exception {

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=somegrant");
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getUnsupportedGrantTypeResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but without specifying grant_type; expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetToken_NoGrantType() throws Exception {

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get("/api/token");
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getInvalidRequestResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but JWT token cannot be decoded by any registered client; expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetToken_UnknownClient() throws Exception {

        final String tempClientId = "tempClient";

        // register client to create token, then unregister the client again
        authClientRepos.save(new AuthClient("desc", "thesecret", tempClientId, "auth"));
        final String tokenToUse = authService.createToken(LOGIN1, tempClientId);
        authClientRepos.deleteAll();

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get(TOKEN_URL + tokenToUse);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getUnknownClientResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

    /**
     * try a /token request, but JWT token is issued for an authority that the registered client may not authenticate; expected HTTP 400
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    @Test
    public void testGetToken_ClientNotAuthorizedForAuthority() throws Exception {

        final String tempClientId = "tempClient";

        // register client to create token, then change the authorities that the registered client may authenticate
        AuthClient theClient = new AuthClient("desc", "thesecret", tempClientId, "auth");
        theClient = authClientRepos.save(theClient);
        final String tokenToUse = authService.createToken(LOGIN1, tempClientId);

        theClient.setAuthorities("anotherAuthority");
        authClientRepos.save(theClient);

        // send token request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get(TOKEN_URL + tokenToUse);
        final ResultActions result = this.mockMvc.perform(builder);

        // expected: Http 400
        result.andExpect(MockMvcResultMatchers.status().isBadRequest());

        final MvcResult resultContent = result.andReturn();
        final String responseString = resultContent.getResponse().getContentAsString();

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = SerialisationHelper.deserializeJsonAuthenticationFailure(responseString);
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getTokenInvalidForClientAuthorityResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }
}
