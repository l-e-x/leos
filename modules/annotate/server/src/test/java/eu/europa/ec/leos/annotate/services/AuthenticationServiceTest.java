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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.repository.UserRepository;
import eu.europa.ec.leos.annotate.services.exceptions.AccessTokenExpiredException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotStoreTokenException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class AuthenticationServiceTest {

    /**
     * These tests focus on token functionality
     */

    private static final String AUTH = "Authorization";
    
    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private UserRepository userRepos;

    @Autowired
    private TokenRepository tokenRepos;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------

    @Before
    public void setUp() {
        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void tearDown() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
    * Test of getUserLogin method, of class AuthenticationServiceImpl.
    */
    @Test
    public void testGetUserLogin_HttpServletRequest() throws Exception {

        final String accessToken = "mytoken";
        final String userLogin = "demo3";

        final User user = new User(userLogin);
        userRepos.save(user);
        tokenRepos.save(new Token(user, "authority", accessToken, LocalDateTime.now().plusMinutes(2), "r", LocalDateTime.now()));

        final MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader(AUTH, "Bearer " + accessToken);

        Assert.assertEquals(userLogin, authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: token is expired
     */
    @Test(expected = AccessTokenExpiredException.class)
    public void testGetUserLogin_HttpServletRequest_TokenExpired() throws Exception {

        final String accessToken = "mytoken";
        final String userLogin = "demo3";

        final User user = new User(userLogin);
        userRepos.save(user);
        tokenRepos.save(new Token(user, "myauth", accessToken, LocalDateTime.now().minusMinutes(2), "r", LocalDateTime.now()));

        final MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader(AUTH, "Bearer " + accessToken);

        authService.getUserLogin(mockHttpRequest);
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: behavior when "Authorization" header is missing
     */
    @Test
    public void testGetUserLogin_HttpServletRequest_WithoutAuthorizationHeader() throws Exception {

        final HttpServletRequest mockHttpRequest = new MockHttpServletRequest(); // no header!

        Assert.assertNull(authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: behavior when "Authorization" header is incomplete (token missing)
     */
    @Test
    public void testGetUserLogin_HttpServletRequest_AuthorizationHeaderWithoutToken() throws Exception {

        final MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader(AUTH, "Bearer ");// header without token

        Assert.assertNull(authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: behavior when "Authorization" header has unexpected format
     */
    @Test
    public void testGetUserLogin_HttpServletRequest_AuthorizationHeaderUnexpected() throws Exception {

        final MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader(AUTH, "nobearer mytoken"); // header doesn't start with "Bearer"

        Assert.assertNull(authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test generation of tokens and persistence in database
     */
    @Test
    public void testTokensAreGenerated() throws CannotStoreTokenException {

        final String authority = "authority";

        final User user = new User("login");
        userRepos.save(user);

        // check before: no tokens
        Assert.assertEquals(0, tokenRepos.findByUserId(user.getId()).size());

        // generate token
        final Token newTokens = authService.generateAndSaveTokensForUser(new UserInformation(user, authority));
        Assert.assertNotNull(newTokens);

        // check: tokens and authority should have been set
        Assert.assertFalse(StringUtils.isEmpty(newTokens.getAccessToken()));
        Assert.assertFalse(StringUtils.isEmpty(newTokens.getRefreshToken()));
        Assert.assertNotNull(newTokens.getAccessTokenExpires());
        Assert.assertNotNull(newTokens.getRefreshTokenExpires());
        Assert.assertEquals(authority, newTokens.getAuthority());

        // check: token contained in DB
        Assert.assertEquals(1, tokenRepos.findByUserId(user.getId()).size());

        // call token generation again
        authService.generateAndSaveTokensForUser(new UserInformation(user, authority));

        // check: another token contained in DB
        Assert.assertEquals(2, tokenRepos.findByUserId(user.getId()).size());
    }

    /**
     * Test that tokens are not generated when no user is given
     */
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test(expected = CannotStoreTokenException.class)
    public void testTokensCannotBeGeneratedWithoutUser() throws Exception {

        final User user = null;
        authService.generateAndSaveTokensForUser(new UserInformation(user, "theauthority"));
    }

    /**
     * Test that tokens are not generated when no authority is given
     */
    @Test(expected = CannotStoreTokenException.class)
    public void testTokensCannotBeGeneratedWithoutAuthority() throws Exception {

        authService.generateAndSaveTokensForUser(new UserInformation(new User("unknownUser"), ""));
    }

    /**
     * Test that tokens are not saved when given user is not contained in database
     */
    @Test(expected = CannotStoreTokenException.class)
    public void testTokensCannotBeGeneratedForUnknownUser() throws Exception {

        authService.generateAndSaveTokensForUser(new UserInformation(new User("unknownUser"), "someauthority"));
    }

    // test that asking for user by invalid access token returns {@literal null}
    @Test
    public void testFindUserByAccessToken_Null() {

        Assert.assertNull(authService.findUserByAccessToken(""));
    }

    // test that asking for user by invalid refresh token returns {@literal null}
    @Test
    public void testFindUserByRefreshToken_Null() {

        Assert.assertNull(authService.findUserByRefreshToken(""));
    }

    // test that asking for user by unknown access token returns a {@literal null} token
    @Test
    public void testFindUserByAccessToken_UnknownToken() {

        final UserInformation userInfo = authService.findUserByAccessToken("whateveraccesstoken");
        Assert.assertNotNull(userInfo);
        Assert.assertNull(userInfo.getCurrentToken());
        Assert.assertNull(userInfo.getUser());
        Assert.assertTrue(StringUtils.isEmpty(userInfo.getLogin()));
    }

    // test that asking for user by unknown refresh token returns {@literal null}
    @Test
    public void testFindUserByRefreshToken_UnknownToken() {

        final UserInformation userInfo = authService.findUserByRefreshToken("whateverrefreshtoken");
        Assert.assertNotNull(userInfo);
        Assert.assertNull(userInfo.getCurrentToken());
        Assert.assertNull(userInfo.getUser());
        Assert.assertTrue(StringUtils.isEmpty(userInfo.getLogin()));
    }

    // test that asking for user by expired access token returns the expired token
    @Test
    public void testFindUserByAccessToken_ExpiredToken() {

        final String accessToken = "acc€$$";

        final User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, "anyauthority",
                accessToken, LocalDateTime.now().minusMinutes(5), // token expired 5 minutes ago
                "r", LocalDateTime.now()));

        final UserInformation foundUser = authService.findUserByAccessToken(accessToken);
        Assert.assertNotNull(foundUser);
        Assert.assertNotNull(foundUser.getCurrentToken());
        Assert.assertTrue(foundUser.getCurrentToken().isAccessTokenExpired());
        Assert.assertEquals("anyauthority", foundUser.getCurrentToken().getAuthority());
    }

    // test that asking for user by expired refresh token returns the expired token
    @Test
    public void testFindUserByRefreshToken_ExpiredToken() {

        final String refreshToken = "refre$h";

        final User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, "otherauth", "acc", LocalDateTime.now(), refreshToken,
                LocalDateTime.now().minusMinutes(5))); // token expired 5 minutes ago

        final UserInformation foundUser = authService.findUserByRefreshToken(refreshToken);
        Assert.assertNotNull(foundUser);
        Assert.assertNotNull(foundUser.getCurrentToken());
        Assert.assertTrue(foundUser.getCurrentToken().isRefreshTokenExpired());
    }

    // test that successfully asking for user by access token returns the correct {@link User}
    @Test
    public void testFindUserByAccessToken() {

        final String accessToken = "acc€$$";

        final User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, "secondauth", accessToken, LocalDateTime.now().plusMinutes(5), "r", LocalDateTime.now()));

        final UserInformation foundUser = authService.findUserByAccessToken(accessToken);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser.getUser());
        Assert.assertNotNull(foundUser.getCurrentToken());
        Assert.assertEquals(accessToken, foundUser.getCurrentToken().getAccessToken());
        Assert.assertFalse(foundUser.getCurrentToken().isAccessTokenExpired());
    }

    // test that successfully asking for user by refresh token returns {@link User}
    @Test
    public void testFindUserByRefreshToken() {

        final String refreshToken = "r€fre$h";

        final User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, "thirdauth", "access", LocalDateTime.now(), refreshToken, LocalDateTime.now().plusMinutes(5)));

        final UserInformation foundUser = authService.findUserByRefreshToken(refreshToken);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser.getUser());
        Assert.assertNotNull(foundUser.getCurrentToken());
        Assert.assertEquals(refreshToken, foundUser.getCurrentToken().getRefreshToken());
        Assert.assertFalse(foundUser.getCurrentToken().isRefreshTokenExpired());
    }

    // test that cleaning tokens for invalid user does not work
    @Test
    public void testCleaningExpiredTokensForInvalidUser() {

        Assert.assertFalse(authService.cleanupExpiredUserTokens(null));
    }

    // test that cleaning tokens for does not remove valid tokens
    @Test
    public void testCleaningExpiredTokensKeepsValidTokens() {

        final LocalDateTime expired = LocalDateTime.now().minusSeconds(30);
        final LocalDateTime notExpired = LocalDateTime.now().plusSeconds(30);

        // store tokens:
        // - access token expired, refresh token not expired
        // - access token not expired, refresh token expired
        // - access token not expired, refresh token not expired
        // -> none to clean!
        User user = new User("login");
        user = userRepos.save(user);

        tokenRepos.save(new Token(user, "highestauth", "acc1", expired, "ref1", notExpired));
        tokenRepos.save(new Token(user, "highestauth", "acc2", notExpired, "ref2", expired));
        tokenRepos.save(new Token(user, "highestauth", "acc3", notExpired, "ref3", notExpired));

        // nothing to clean -> false expected
        Assert.assertFalse(authService.cleanupExpiredUserTokens(user));
    }

    // test that only tokens of given user are cleaned
    @Test
    public void testCleaningExpiredTokensOfGivenUserOnly() {

        final LocalDateTime expired = LocalDateTime.now().minusSeconds(30);

        // store tokens for three different users: access token expired, refresh token expired
        User user1 = new User("login1");
        user1 = userRepos.save(user1);
        User user2 = new User("login2");
        user2 = userRepos.save(user2);
        User user3 = new User("login3");
        user3 = userRepos.save(user3);

        tokenRepos.save(new Token(user1, "auth", "acc1", expired, "ref1", expired));
        tokenRepos.save(new Token(user2, "auth", "acc2", expired, "ref2", expired));
        tokenRepos.save(new Token(user2, "otherauth", "acc22", expired, "ref22", expired)); // second user has two expired tokens, from different authorities
        tokenRepos.save(new Token(user3, "auth", "acc3", expired, "ref3", expired));

        // try cleaning for different users step by step, verify number of remaining tokens before and after
        Assert.assertEquals(4, tokenRepos.count());

        // clean for first user -> one should vanish
        Assert.assertTrue(authService.cleanupExpiredUserTokens(user1));
        Assert.assertEquals(3, tokenRepos.count());

        // clean for second user -> two should vanish
        Assert.assertTrue(authService.cleanupExpiredUserTokens(user2));
        Assert.assertEquals(1, tokenRepos.count());

        // clean again for second user -> none should vanish
        Assert.assertFalse(authService.cleanupExpiredUserTokens(user2));
        Assert.assertEquals(1, tokenRepos.count());

        // clean for third user -> last one should vanish
        Assert.assertTrue(authService.cleanupExpiredUserTokens(user3));
        Assert.assertEquals(0, tokenRepos.count());
    }
}
