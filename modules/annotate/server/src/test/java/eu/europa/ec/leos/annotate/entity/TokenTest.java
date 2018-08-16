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
package eu.europa.ec.leos.annotate.entity;

import eu.europa.ec.leos.annotate.model.entity.Token;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class TokenTest {

    /**
     * This class contains tests for functions operating on {@link Token} entities
     */

    // -------------------------------------
    // Tests
    // -------------------------------------

    // check that access token's expiration time is computed correctly
    @Test
    public void testAccessTokenExpirationSetCorrectly() {

        final String access = "acc";
        final int lifetime = 5;

        Token t = new Token();
        t.setAccessToken(access, lifetime); // use the "combined" function

        Assert.assertEquals(access, t.getAccessToken());
        Assert.assertEquals(lifetime, t.getAccessTokenLifetimeSeconds());

        LocalDateTime latestExpiration = LocalDateTime.now().plusSeconds(lifetime);

        // note: as our reference date is computed after the token's date was computed, it should be (slightly) after the token's expiration or even identical
        Assert.assertTrue(t.getAccessTokenExpires().isBefore(latestExpiration) || t.getAccessTokenExpires().equals(latestExpiration));
    }

    // check that refresh token's expiration time is computed correctly
    @Test
    public void testRefreshTokenExpirationSetCorrectly() {

        final String refresh = "re";
        final int lifetime = 100;

        Token t = new Token();
        t.setRefreshToken(refresh, lifetime); // use the "combined" function

        Assert.assertEquals(refresh, t.getRefreshToken());
        Assert.assertEquals(lifetime, t.getRefreshTokenLifetimeSeconds());

        LocalDateTime latestExpiration = LocalDateTime.now().plusSeconds(lifetime);

        // note: as our reference date is computed after the token's date was computed, it should be (slightly) after the token's expiration or even identical
        Assert.assertTrue(t.getRefreshTokenExpires().isBefore(latestExpiration) || t.getRefreshTokenExpires().equals(latestExpiration));
    }

    // check that the access token is correctly considered expired
    @Test
    public void testAccessTokenExpired() {

        Token t = new Token();
        t.setAccessToken("access", 1);

        // now set the expiration date backwards by 2 seconds -> should then be a past date
        t.setAccessTokenExpires(LocalDateTime.now().minusSeconds(2));

        // verify: must be expired
        Assert.assertTrue(t.isAccessTokenExpired());
    }

    // check that the refresh token is correctly considered expired
    @Test
    public void testRefreshTokenExpired() {

        Token t = new Token();
        t.setAccessToken("refresh", 1);

        // now set the expiration date backwards by 2 seconds -> should then be a past date
        t.setRefreshTokenExpires(LocalDateTime.now().minusSeconds(2));

        // verify: must be expired
        Assert.assertTrue(t.isRefreshTokenExpired());
    }
}
