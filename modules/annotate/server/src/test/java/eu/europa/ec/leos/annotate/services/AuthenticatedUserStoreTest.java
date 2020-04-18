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
import eu.europa.ec.leos.annotate.services.impl.AuthenticatedUserStore;
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
public class AuthenticatedUserStoreTest {

    /**
     * simple tests for the authenticated user's thread store 
     */

 // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private AuthenticatedUserStore authUserStore;

    // -------------------------------------
    // Cleanup of cached content
    // -------------------------------------
    @Before
    public void setUp() throws Exception {
        authUserStore.clear();
    }

    @After
    public void tearDown() throws Exception {
        authUserStore.clear();
    }

    // -------------------------------------
    // Tests
    // -------------------------------------
    @Test
    public void testAuthenticatedUserStore() throws Exception {

        // initially: empty
        Assert.assertNull(authUserStore.getUserInfo());

        // store a user
        final UserInformation authenticated = new UserInformation("login", "authority");
        authUserStore.setUserInfo(authenticated);

        // verify the stored user can be retrieved again
        Assert.assertEquals(authenticated, authUserStore.getUserInfo());
        
        // call the clear method and check that no user is available afterwards
        authUserStore.clear();
        Assert.assertNull(authUserStore.getUserInfo());
    }
}
