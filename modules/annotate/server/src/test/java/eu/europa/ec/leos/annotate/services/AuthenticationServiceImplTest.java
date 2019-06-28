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
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.AuthClient;
import eu.europa.ec.leos.annotate.repository.AuthClientRepository;
import eu.europa.ec.leos.annotate.services.exceptions.TokenFromUnknownClientException;
import eu.europa.ec.leos.annotate.services.exceptions.TokenInvalidForClientAuthorityException;
import eu.europa.ec.leos.annotate.services.impl.AuthenticationServiceImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
@SuppressWarnings("PMD.TooManyMethods")
public class AuthenticationServiceImplTest {

    /**
     * These tests mainly focus on finding the correct AuthClient object from the database and its functionality
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private AuthenticationServiceImpl authenticationService;

    @Autowired
    private AuthClientRepository authClientRepos;

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

    // test that token creation actually creates something
    @Test
    public void test_getUserLogin() throws Exception {

        final String clientId = "client";
        final String login = "demo2";
        final String authority = "ecas";

        authClientRepos.save(new AuthClient("description", "d4$ecre1", clientId, authority));

        final String token1 = authenticationService.createToken(login, clientId);
        Assert.assertNotNull("token should not be null", token1);

        final UserInformation info = authenticationService.getUserLoginFromToken(token1);
        Assert.assertNotNull(info);
        Assert.assertEquals(login, info.getLogin());
        Assert.assertEquals(authority, info.getAuthority());
    }

    // test extraction of domain name from given URL
    @Test
    public void Test_getDomainName() {
        final String domain = authenticationService.getDomainName("https://D02DI1321646DIT.net1.cec.eu.int:9099/");
        Assert.assertEquals(domain, "D02DI1321646DIT.net1.cec.eu.int");
    }

    // test extraction of domain name from given URL
    @Test
    public void Test_getDomainNameWithoutWww() {
        final String domain = authenticationService.getDomainName("https://www.test.eu.int:9099/");
        Assert.assertEquals(domain, "test.eu.int");
    }

    // test extraction of domain name from given invalid URL
    @Test
    public void Test_getDomainName_invalidUrl() {

        final String invalidUrl = "xyz://inval.^id";

        final String domain = authenticationService.getDomainName(invalidUrl);
        Assert.assertEquals(domain, invalidUrl); // invalid URL is returned when inner exception occurs
    }

    // test retrieval of user login from a given token
    @Test
    public void Test_getUserLogin_Ok() throws TokenFromUnknownClientException, TokenInvalidForClientAuthorityException {

        // setup
        final String clientId = "client123";
        final String authority = "myotherauthority";

        final AuthClient theClient = new AuthClient("description", "secret", clientId, authority);
        authClientRepos.save(theClient);

        final String login = "johndoe";
        final String token = authenticationService.createToken(login, clientId);

        // modify the authority of the client
        theClient.setAuthorities("myauthority;" + authority);

        // Actual call
        final UserInformation result = authenticationService.getUserLoginFromToken(token);

        // verify
        Assert.assertEquals(login, result.getLogin());
        Assert.assertEquals(authority, result.getAuthority());
    }

    // test retrieval of user login from given token when more than one client is registered
    @Test
    public void Test_getUserLogin_twoClients_Ok() throws TokenFromUnknownClientException, TokenInvalidForClientAuthorityException {

        // setup
        final String clientId = "clientId123";
        final String authority = "myauthority";

        authClientRepos.save(new AuthClient("authdescrip", "anothersecret", "anotherissuer", "anotherauthority"));
        authClientRepos.save(new AuthClient("authclientdesc", "secret", clientId, authority));

        final String login = "thelogin";
        final String token = authenticationService.createToken(login, clientId);

        // Actual call
        final UserInformation result = authenticationService.getUserLoginFromToken(token);

        // verify
        Assert.assertEquals(login, result.getLogin());
        Assert.assertEquals(authority, result.getAuthority());
    }

    // test retrieval of user login from given token, but fails as no registered client matches
    @Test(expected = TokenFromUnknownClientException.class)
    public void Test_getUserLogin_noMatchingClient() throws TokenFromUnknownClientException, TokenInvalidForClientAuthorityException {

        // setup: save two clients
        authClientRepos.save(new AuthClient("authdesc", "secret1", "clientId1", null));
        authClientRepos.save(new AuthClient("clientdesc", "secret2", "clientId2", "someauth"));

        // actual call - should fail!
        authenticationService.getUserLoginFromToken("whatevertoken");
    }

    // test retrieval of user login from given token, but fails as no clients are registered at all
    @Test(expected = TokenFromUnknownClientException.class)
    public void Test_getUserLogin_noClients() throws TokenFromUnknownClientException, TokenInvalidForClientAuthorityException {

        authenticationService.getUserLoginFromToken("whatevertoken");
    }

    // test that token can be decoded by a client, but the client may not authenticate the authority of the encoded user
    @Test(expected = TokenInvalidForClientAuthorityException.class)
    public void test_getUserLogin_wrongAuthority() throws Exception {

        // setup: save client
        final AuthClient theClient = new AuthClient("desc", "secret2", "clientId2", "someauth");
        authClientRepos.save(theClient);

        // generate a token, but then modify the client's authority
        final String token = authenticationService.createToken("someuser", theClient.getClientId());

        theClient.setAuthorities("anotherauth");
        authClientRepos.save(theClient);

        // actual call - should fail!
        authenticationService.getUserLoginFromToken(token);
    }

    // test that token can be decoded by a client; client has no authority set and may authenticate any authority's user
    @Test
    public void test_getUserLogin_authorityUndefined() throws Exception {

        final String userId = "someuser";
        final String authority = "someauth";

        // setup: save client
        final AuthClient theClient = new AuthClient("desc", "secret2", "clientId2", authority);
        authClientRepos.save(theClient);

        // generate a token, but then modify the client's authority
        final String token = authenticationService.createToken(userId, theClient.getClientId());

        theClient.setAuthorities(null);
        authClientRepos.save(theClient);

        // actual call - should pass, since token can be decoded and no authority restriction applies!
        final UserInformation info = authenticationService.getUserLoginFromToken(token);
        Assert.assertEquals(userId, info.getLogin());
        Assert.assertEquals(authority, info.getAuthority());
    }

    /*@Test
    public void helpCreateToken() throws Exception {
    
        // NOTE:
        // this function is not intended for testing, but to help creating a token that can be used for the TEST environment
        // this means we reproduce the database AUTHCLIENTS content needed
        AuthClient testAuthClient = new AuthClient("the client", "AnnotateIssuedSecret", "AnnotateIssuedClientId", null);
        authClientRepos.save(testAuthClient);
        
        String username = "john";
        String result = authenticationService.createToken(username, testAuthClient.getClientId());
        
        System.out.println("New token is: " + result);
    }*/
}