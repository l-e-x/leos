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

import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.services.exceptions.CannotStoreTokenException;
import eu.europa.ec.leos.annotate.services.impl.AuthenticationServiceImpl;
import org.junit.Assert;
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
public class AuthenticationServiceWithMockedUuidServiceTest {

    /**
     * Test cases on the AuthenticationService; executed using mocked UUIDGeneratorService to simulate desired internal behavior 
     */

    @Before
    public void setupTests() {

        MockitoAnnotations.initMocks(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // the UUIDGeneratorService used inside the AuthenticationService is mocked
    @Mock
    private UUIDGeneratorService uuidService;

    @InjectMocks
    private AuthenticationServiceImpl authService;

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test that an expected exception is given when invalid access token is tried to be persisted
     */
    @Test
    public void testSavingEmptyAccessTokenThrowsException() {

        Mockito.when(uuidService.generateUrlSafeUUID())
            .thenReturn("")          // first call provides invalid access token
            .thenReturn("refresh");  // second is ok

        try {
            authService.generateAndSaveTokensForUser(new UserInformation(new User("someuser"), "auth"));
            Assert.fail("Expected exception from AuthenticationService not received");
        } catch(CannotStoreTokenException cste) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception from AuthenticationService");
        }
    }
    
    /**
     * test that an expected exception is given when invalid refresh token is tried to be persisted
     */
    @Test
    public void testSavingEmptyRefreshTokenThrowsException() {

        Mockito.when(uuidService.generateUrlSafeUUID())
            .thenReturn("acce$$")          // first call is ok 
            .thenReturn("");  // second provides invalid access token

        try {
            authService.generateAndSaveTokensForUser(new UserInformation(new User("someuser"), "auth"));
            Assert.fail("Expected exception from AuthenticationService not received");
        } catch(CannotStoreTokenException cste) {
            // OK
        } catch (Exception e) {
            Assert.fail("Received unexpected exception from AuthenticationService");
        }
    }
}
