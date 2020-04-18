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

import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
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

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class AuthenticationServiceWithMockedTokenReposTest {

    /**
     * Test cases on the AuthenticationService; executed using mocked TokenRepository to simulate desired internal behavior 
     */

    // the TokenRepository used inside the AuthenticationService is mocked
    @Mock
    private TokenRepository tokenRepos;

    @InjectMocks
    private AuthenticationServiceImpl authService;

    @Before
    public void setupTests() {

        MockitoAnnotations.initMocks(this);
    }

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test that no exception is received when the TokenRepository internally throws an exception
     */
    @Test
    public void testTokenCleanupDoesNotFailWhenInternalExceptionOccurs() {

        final User user = new User();

        Mockito.when(tokenRepos.findByUserAndAccessTokenExpiresLessThanEqualAndRefreshTokenExpiresLessThanEqual(
                Mockito.any(User.class), Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class)))
                .thenThrow(new RuntimeException());

        // cleanup routine should return false, but not throw an exception
        Assert.assertFalse(authService.cleanupExpiredUserTokens(user));
    }

}
