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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
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

    @Before
    public void setUp() throws Exception {
        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void tearDown() throws Exception {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    // test that token creation actually creates something
    @Test
    public void test_getUserLogin() throws Exception {

        final String clientId = "client";

        authClientRepos.save(new AuthClient("description", "d4$ecre1", clientId, "ecas"));

        String token1 = authenticationService.createToken("demo2", clientId);
        assertNotNull("token should not be null", token1);
        assertEquals("demo2", authenticationService.getUserLoginFromToken(token1));
    }

    // test extraction of domain name from given URL
    @Test
    public void Test_getDomainName() {
        String domain = authenticationService.getDomainName("https://D02DI1321646DIT.net1.cec.eu.int:9099/");
        assertEquals(domain, "D02DI1321646DIT.net1.cec.eu.int");
    }

    // test extraction of domain name from given URL
    @Test
    public void Test_getDomainNameWithoutWww() {
        String domain = authenticationService.getDomainName("https://www.test.eu.int:9099/");
        assertEquals(domain, "test.eu.int");
    }
    
    // test extraction of domain name from given invalid URL
    @Test
    public void Test_getDomainName_invalidUrl() {
        
        final String invalidUrl = "xyz://inval.^id";
        
        String domain = authenticationService.getDomainName(invalidUrl);
        assertEquals(domain, invalidUrl); // invalid URL is returned when inner exception occurs
    }

    // test retrieval of user login from a given token
    @Test
    public void Test_getUserLogin_Ok() throws Exception {

        // setup
        final String clientId = "client123";

        AuthClient theClient = new AuthClient("desc", "secret", clientId, "myotherauthority");
        authClientRepos.save(theClient);

        String login = "johndoe";
        String token = authenticationService.createToken(login, clientId);

        // modify the authority of the client
        theClient.setAuthorities("myauthority;myotherauthority");

        // Actual call
        String result = authenticationService.getUserLoginFromToken(token);

        // verify
        assertEquals(login, result);
    }

    // test retrieval of user login from given token when more than one client is registered
    @Test
    public void Test_getUserLogin_twoClients_Ok() throws Exception {

        // setup
        final String clientId = "clientId123";

        authClientRepos.save(new AuthClient("desc", "anothersecret", "anotherissuer", "anotherauthority"));
        authClientRepos.save(new AuthClient("desc", "secret", clientId, "myauthority"));

        String login = "thelogin";
        String token = authenticationService.createToken(login, clientId);

        // Actual call
        String result = authenticationService.getUserLoginFromToken(token);

        // verify
        Assert.assertEquals(login, result);
    }

    // test retrieval of user login from given token, but fails as no registered client matches
    @Test
    public void Test_getUserLogin_noMatchingClient() throws Exception {

        // setup: save two clients
        authClientRepos.save(new AuthClient("desc", "secret1", "clientId1", null));
        authClientRepos.save(new AuthClient("desc", "secret2", "clientId2", "someauth"));

        // actual call - should fail!
        try {
            authenticationService.getUserLoginFromToken("whatevertoken");
            Assert.fail("Expected exception not received (token not expected to be decoded)");
        } catch (TokenFromUnknownClientException tfuce) {
            // OK!
        } catch (Exception e) {
            Assert.fail("Received unexpected exception!");
        }
    }

    // test retrieval of user login from given token, but fails as no clients are registered at all
    @Test
    public void Test_getUserLogin_noClients() throws Exception {

        try {
            authenticationService.getUserLoginFromToken("whatevertoken");
            Assert.fail("Expected exception not received (token not expected to be decoded)");
        } catch (TokenFromUnknownClientException tfuce) {
            // OK!
        } catch (Exception e) {
            Assert.fail("Received unexpected exception!");
        }
    }

    // test that token can be decoded by a client, but the client may not authenticate the authority of the encoded user
    @Test
    public void test_getUserLogin_wrongAuthority() throws Exception {

        // setup: save client
        AuthClient theClient = new AuthClient("desc", "secret2", "clientId2", "someauth");
        authClientRepos.save(theClient);

        // generate a token, but then modify the client's authority
        String token = authenticationService.createToken("someuser", theClient.getClientId());

        theClient.setAuthorities("anotherauth");
        authClientRepos.save(theClient);

        // actual call - should fail!
        try {
            authenticationService.getUserLoginFromToken(token);
            Assert.fail("Expected exception not received (since decoding client may not authenticate given authority)");
        } catch (TokenInvalidForClientAuthorityException tifcae) {
            // OK!
        } catch (Exception e) {
            Assert.fail("Received unexpected exception!");
        }
    }

    // test that token can be decoded by a client; client has no authority set and may authenticate any authority's user
    @Test
    public void test_getUserLogin_authorityUndefined() throws Exception {

        final String userId = "someuser";

        // setup: save client
        AuthClient theClient = new AuthClient("desc", "secret2", "clientId2", "someauth");
        authClientRepos.save(theClient);

        // generate a token, but then modify the client's authority
        String token = authenticationService.createToken(userId, theClient.getClientId());

        theClient.setAuthorities(null);
        authClientRepos.save(theClient);

        // actual call - should pass, since token can be decoded and no authority restriction applies!
        Assert.assertEquals(userId, authenticationService.getUserLoginFromToken(token));
    }
}