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
package eu.europa.ec.leos.annotate;

import eu.europa.ec.leos.annotate.model.UserDetails;
import eu.europa.ec.leos.annotate.services.impl.UserDetailsCache;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class UserDetailsCacheTest {

    /**
     * tests for checking proper working of our cache of user details 
     */

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private UserDetailsCache userCache;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void cleanDatabaseBeforeTests() {

        userCache.clear();
    }

    @After
    public void cleanDatabaseAfterTests() {
        userCache.clear();
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * test basic cache functionality
     */
    @Test
    public void testUserDetailsCache() {

        final String LOGIN = "userlogin";

        Assert.assertEquals(0, userCache.size());
        Assert.assertNull(userCache.getCachedUserDetails(LOGIN));

        final UserDetails details = new UserDetails(LOGIN, (long) 47, "Santa", "Clause", "DIGIT", "santa@clause.europa.eu", null);
        userCache.cache(LOGIN, details);
        Assert.assertEquals(1, userCache.size());

        final UserDetails cachedItem = userCache.getCachedUserDetails(LOGIN);
        Assert.assertEquals(details, cachedItem);

        userCache.clear();
        Assert.assertNull(userCache.getCachedUserDetails(LOGIN));
        Assert.assertEquals(0, userCache.size());
    }

    /**
     * test work refusal when parameters are missing
     */
    @Test
    public void testCacheWithoutName() {

        // verify asking for invalid name returns null
        Assert.assertNull(userCache.getCachedUserDetails(""));

        // verify that no exception is thrown when trying to cache invalid value
        userCache.cache("", null);
    }

    /**
     * test that the cache cleanup works
     */
    @Test
    public void testUserDetailsCacheCleanup() {

        final String LOGIN = "someuser";

        final UserDetails details = new UserDetails(LOGIN, (long) 47, "Santa", "Clause", "DIGIT", "santa@clause.europa.eu", null);
        userCache.cache(LOGIN, details);
        Assert.assertNotNull(userCache.getCachedUserDetails(LOGIN));

        // set next cleanup time invalid -> no cleanup yet, item still present
        userCache.setNextCacheCleanupTime(null);
        Assert.assertNotNull(userCache.getCachedUserDetails(LOGIN));

        // set next cleanup to be in the past -> should trigger upon next access
        userCache.setNextCacheCleanupTime(LocalDateTime.now().minusMinutes(5));

        // -> cache empty
        Assert.assertNull(userCache.getCachedUserDetails(LOGIN));
        Assert.assertEquals(0, userCache.size());
    }
}
