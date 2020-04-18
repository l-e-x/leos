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
package eu.europa.ec.leos.annotate.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@ActiveProfiles("test")
public class UserInformationTest {

    /**
     * Class testing propagation of assignment within {@link UserInformation} class
     */

    // test that login and authority don't suffice to set further properties
    @Test
    public void testLoginAuthority() {

        final String login = "mylogin";
        final String authority = "theauth";

        final UserInformation userinfo = new UserInformation(login, authority);

        // verify
        Assert.assertEquals(login, userinfo.getLogin());
        Assert.assertEquals(authority, userinfo.getAuthority());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getCurrentToken());
        Assert.assertNull(userinfo.getUser());
        Assert.assertNull(userinfo.getUserDetails());
    }

    // test that token contains most information to set further properties
    @Test
    public void testToken() {

        final String login = "mylogin";
        final String authority = "auth";
        final User user = new User(login);
        final Token token = new Token(user, authority, "@cc€$$", LocalDateTime.now(), "r€fre$h", LocalDateTime.now());

        final UserInformation userinfo = new UserInformation(token);

        // verify
        Assert.assertEquals(login, userinfo.getLogin());
        Assert.assertEquals(user, userinfo.getUser());
        Assert.assertEquals(login, userinfo.getUser().getLogin());
        Assert.assertEquals(authority, userinfo.getAuthority());
        Assert.assertEquals(token, userinfo.getCurrentToken());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getUserDetails());
    }

    // test that null token does not contain further info
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test
    public void testTokenNull() {

        final Token token = null;

        final UserInformation userinfo = new UserInformation(token);

        // verify
        Assert.assertNull(userinfo.getLogin());
        Assert.assertNull(userinfo.getUser());
        Assert.assertNull(userinfo.getAuthority());
        Assert.assertEquals(token, userinfo.getCurrentToken());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getUserDetails());
    }

    // test that user and authority don't suffice to set further properties
    @Test
    public void testUserAuthority() {

        final String login = "login";
        final String authority = "auth";
        final User user = new User(login);

        final UserInformation userinfo = new UserInformation(user, authority);

        // verify
        Assert.assertEquals(login, userinfo.getLogin());
        Assert.assertEquals(user, userinfo.getUser());
        Assert.assertEquals(login, userinfo.getUser().getLogin());
        Assert.assertEquals(authority, userinfo.getAuthority());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getCurrentToken());
        Assert.assertNull(userinfo.getUserDetails());
    }

    // test that a {@literal null} user and authority don't suffice to set further properties
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test
    public void testUserNullAuthority() {

        final String authority = "auth";
        final User user = null;

        final UserInformation userinfo = new UserInformation(user, authority);

        // verify
        Assert.assertEquals(user, userinfo.getUser());
        Assert.assertEquals(authority, userinfo.getAuthority());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getLogin());
        Assert.assertNull(userinfo.getCurrentToken());
        Assert.assertNull(userinfo.getUserDetails());
    }

    // test that user and user details don't suffice to set further properties
    @Test
    public void testUserUserdetails() {

        final String login = "login";
        final User user = new User(login);
        final UserDetails details = new UserDetails(login, Long.valueOf(1), "first", "last", null, "", null);

        final UserInformation userinfo = new UserInformation(user, details);

        // verify
        Assert.assertEquals(login, userinfo.getLogin());
        Assert.assertEquals(user, userinfo.getUser());
        Assert.assertEquals(login, userinfo.getUser().getLogin());
        Assert.assertNotNull(userinfo.getUserDetails());
        Assert.assertEquals(login, userinfo.getUserDetails().getLogin());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getAuthority());
        Assert.assertNull(userinfo.getCurrentToken());
    }

    // test that only user details don't suffice to set further properties
    @SuppressFBWarnings(value = SpotBugsAnnotations.KnownNullValue, justification = SpotBugsAnnotations.KnownNullValueReason)
    @Test
    public void testUserNullUserdetails() {

        final String login = "login";
        final User user = null;
        final UserDetails details = new UserDetails(login, Long.valueOf(1), "first", "last", null, "", null);

        final UserInformation userinfo = new UserInformation(user, details);

        // verify
        Assert.assertEquals(login, userinfo.getLogin());
        Assert.assertNotNull(userinfo.getUserDetails());
        Assert.assertEquals(login, userinfo.getUserDetails().getLogin());

        // other properties cannot be deducted
        Assert.assertNull(userinfo.getUser());
        Assert.assertNull(userinfo.getAuthority());
        Assert.assertNull(userinfo.getCurrentToken());
    }
}
