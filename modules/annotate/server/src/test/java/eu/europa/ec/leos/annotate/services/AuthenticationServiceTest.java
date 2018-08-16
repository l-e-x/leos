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
package eu.europa.ec.leos.annotate.services;

import eu.europa.ec.leos.annotate.helper.TestDbHelper;
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
import java.util.concurrent.atomic.AtomicReference;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
@ActiveProfiles("test")
public class AuthenticationServiceTest {

    /**
     * These tests focus on token functionality
     */

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------

    @Before
    public void setUp() throws Exception {
        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void tearDown() throws Exception {
        TestDbHelper.cleanupRepositories(this);
    }

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
    // Tests
    // -------------------------------------

    /**
    * Test of getUserLogin method, of class AuthenticationServiceImpl.
    */
    @Test
    public void testGetUserLogin_HttpServletRequest() throws Exception {

        final String accessToken = "mytoken", userLogin = "demo3";

        User user = new User(userLogin);
        userRepos.save(user);
        tokenRepos.save(new Token(user, accessToken, LocalDateTime.now().plusMinutes(2), "r", LocalDateTime.now()));

        MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader("Authorization", "Bearer " + accessToken);

        Assert.assertEquals(userLogin, authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: token is expired
     */
    @Test
    public void testGetUserLogin_HttpServletRequest_TokenExpired() throws Exception {

        final String accessToken = "mytoken", userLogin = "demo3";

        User user = new User(userLogin);
        userRepos.save(user);
        tokenRepos.save(new Token(user, accessToken, LocalDateTime.now().minusMinutes(2), "r", LocalDateTime.now()));

        MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader("Authorization", "Bearer " + accessToken);

        try {
            authService.getUserLogin(mockHttpRequest);
            Assert.fail("Expected exception not thrown");
        } catch (AccessTokenExpiredException atee) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception - should be AccessTokenExpiredException");
        }
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: behavior when "Authorization" header is missing
     */
    @Test
    public void testGetUserLogin_HttpServletRequest_WithoutAuthorizationHeader() throws Exception {

        HttpServletRequest mockHttpRequest = new MockHttpServletRequest(); // no header!

        Assert.assertNull(authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test of getUserLogin method, of class AuthenticationServiceImpl: behavior when "Authorization" header has unexpected format
     */
    @Test
    public void testGetUserLogin_HttpServletRequest_AuthorizationHeaderUnexpected() throws Exception {

        MockHttpServletRequest mockHttpRequest = new MockHttpServletRequest();
        mockHttpRequest.addHeader("Authorization", "nobearer mytoken"); // header doesn't start with "Bearer"

        Assert.assertNull(authService.getUserLogin(mockHttpRequest));
    }

    /**
     * Test generation of tokens and persistence in database
     */
    @Test
    public void testTokensAreGenerated() throws CannotStoreTokenException {

        User user = new User("login");
        userRepos.save(user);

        // check before: no tokens
        Assert.assertEquals(0, tokenRepos.findByUserId(user.getId()).size());

        // generate token
        Token newTokens = authService.generateAndSaveTokensForUser(user);
        Assert.assertNotNull(newTokens);

        // check: tokens should have been set
        Assert.assertFalse(StringUtils.isEmpty(newTokens.getAccessToken()));
        Assert.assertFalse(StringUtils.isEmpty(newTokens.getRefreshToken()));
        Assert.assertNotNull(newTokens.getAccessTokenExpires());
        Assert.assertNotNull(newTokens.getRefreshTokenExpires());

        // check: token contained in DB
        Assert.assertEquals(1, tokenRepos.findByUserId(user.getId()).size());

        // call token generation again
        authService.generateAndSaveTokensForUser(user);

        // check: another token contained in DB
        Assert.assertEquals(2, tokenRepos.findByUserId(user.getId()).size());
    }

    /**
     * Test that tokens are not generated when no user is given
     */
    @Test
    public void testTokensCannotBeGeneratedWithoutUser() {

        try {
            authService.generateAndSaveTokensForUser(null);
            Assert.fail("Token generation should throw an error since no user is given; did not.");
        } catch (CannotStoreTokenException cste) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception " + e.getMessage());
        }
    }

    /**
     * Test that tokens are not saved when given user is not contained in database
     */
    @Test
    public void testTokensCannotBeGeneratedForUnknownUser() {

        try {
            authService.generateAndSaveTokensForUser(new User("unknownUser"));
            Assert.fail("Token generation should throw an error since no user is given; did not.");
        } catch (CannotStoreTokenException cste) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception " + e.getMessage());
        }
    }

    // test that asking for user by invalid access token returns {@literal null}
    @Test
    public void testFindUserByAccessToken_Null() {

        Assert.assertNull(authService.findUserByAccessToken("", new AtomicReference<Token>()));
    }

    // test that asking for user by invalid refresh token returns {@literal null}
    @Test
    public void testFindUserByRefreshToken_Null() {

        Assert.assertNull(authService.findUserByRefreshToken("", new AtomicReference<Token>()));
    }

    // test that asking for user by unknown access token returns {@literal null}
    @Test
    public void testFindUserByAccessToken_UnknownToken() {

        Assert.assertNull(authService.findUserByAccessToken("whateveraccesstoken", new AtomicReference<Token>()));
    }

    // test that asking for user by unknown refresh token returns {@literal null}
    @Test
    public void testFindUserByRefreshToken_UnknownToken() {

        Assert.assertNull(authService.findUserByRefreshToken("whateverrefreshtoken", new AtomicReference<Token>()));
    }

    // test that asking for user by expired access token returns {@literal null}
    @Test
    public void testFindUserByAccessToken_ExpiredToken() {

        final String accessToken = "acc€$$";

        User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, accessToken, LocalDateTime.now().minusMinutes(5), "r", LocalDateTime.now())); // token expired 5 minutes ago

        AtomicReference<Token> foundToken = new AtomicReference<Token>();
        User foundUser = authService.findUserByAccessToken(accessToken, foundToken);
        Assert.assertNull(foundUser);
        Assert.assertNotNull(foundToken.get());
        Assert.assertTrue(foundToken.get().isAccessTokenExpired());
    }

    // test that asking for user by expired refresh token returns {@literal null}
    @Test
    public void testFindUserByRefreshToken_ExpiredToken() {

        final String refreshToken = "refre$h";

        User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, "acc", LocalDateTime.now(), refreshToken, LocalDateTime.now().minusMinutes(5))); // token expired 5 minutes ago

        AtomicReference<Token> foundToken = new AtomicReference<Token>();
        User foundUser = authService.findUserByRefreshToken(refreshToken, foundToken);
        Assert.assertNull(foundUser);
        Assert.assertNotNull(foundToken.get());
        Assert.assertTrue(foundToken.get().isRefreshTokenExpired());
    }

    // test that successfully asking for user by access token returns {@link User}
    @Test
    public void testFindUserByAccessToken() {

        final String accessToken = "acc€$$";

        User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, accessToken, LocalDateTime.now().plusMinutes(5), "r", LocalDateTime.now()));

        AtomicReference<Token> foundToken = new AtomicReference<Token>();
        User foundUser = authService.findUserByAccessToken(accessToken, foundToken);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser);
        Assert.assertNotNull(foundToken.get());
        Assert.assertEquals(accessToken, foundToken.get().getAccessToken());
        Assert.assertFalse(foundToken.get().isAccessTokenExpired());
    }

    // test that successfully asking for user by refresh token returns {@link User}
    @Test
    public void testFindUserByRefreshToken() {

        final String refreshToken = "r€fre$h";

        User user = new User("me");
        userRepos.save(user);
        tokenRepos.save(new Token(user, "access", LocalDateTime.now(), refreshToken, LocalDateTime.now().plusMinutes(5)));

        AtomicReference<Token> foundToken = new AtomicReference<Token>();
        User foundUser = authService.findUserByRefreshToken(refreshToken, foundToken);
        Assert.assertNotNull(foundUser);
        Assert.assertEquals(user, foundUser);
        Assert.assertNotNull(foundToken);
        Assert.assertEquals(refreshToken, foundToken.get().getRefreshToken());
        Assert.assertFalse(foundToken.get().isRefreshTokenExpired());
    }

    // test that cleaning tokens for invalid user does not work
    @Test
    public void testCleaningExpiredTokensForInvalidUser() {

        Assert.assertFalse(authService.cleanupExpiredUserTokens(null));
    }

    // test that cleaning tokens for does not remove valid tokens
    @Test
    public void testCleaningExpiredTokensKeepsValidTokens() {

        LocalDateTime expired = LocalDateTime.now().minusSeconds(30);
        LocalDateTime notExpired = LocalDateTime.now().plusSeconds(30);

        // store tokens:
        // - access token expired, refresh token not expired
        // - access token not expired, refresh token expired
        // - access token not expired, refresh token not expired
        // -> none to clean!
        User user = new User("login");
        user = userRepos.save(user);

        tokenRepos.save(new Token(user, "acc1", expired, "ref1", notExpired));
        tokenRepos.save(new Token(user, "acc2", notExpired, "ref2", expired));
        tokenRepos.save(new Token(user, "acc3", notExpired, "ref3", notExpired));

        // nothing to clean -> false expected
        Assert.assertFalse(authService.cleanupExpiredUserTokens(user));
    }

    // test that only tokens of given user are cleaned
    @Test
    public void testCleaningExpiredTokensOfGivenUserOnly() {

        LocalDateTime expired = LocalDateTime.now().minusSeconds(30);

        // store tokens for three different users: access token expired, refresh token expired
        User user1 = new User("login1");
        user1 = userRepos.save(user1);
        User user2 = new User("login2");
        user2 = userRepos.save(user2);
        User user3 = new User("login3");
        user3 = userRepos.save(user3);

        tokenRepos.save(new Token(user1, "acc1", expired, "ref1", expired));
        tokenRepos.save(new Token(user2, "acc2", expired, "ref2", expired));
        tokenRepos.save(new Token(user2, "acc22", expired, "ref22", expired)); // second user has two expired tokens
        tokenRepos.save(new Token(user3, "acc3", expired, "ref3", expired));

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
