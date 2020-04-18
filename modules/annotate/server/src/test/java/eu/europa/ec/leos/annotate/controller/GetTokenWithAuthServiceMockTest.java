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
package eu.europa.ec.leos.annotate.controller;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import eu.europa.ec.leos.annotate.controllers.AuthApiController;
import eu.europa.ec.leos.annotate.helper.SpotBugsAnnotations;
import eu.europa.ec.leos.annotate.helper.TestDbHelper;
import eu.europa.ec.leos.annotate.model.web.token.JsonAuthenticationFailure;
import eu.europa.ec.leos.annotate.services.AuthenticationService;
import eu.europa.ec.leos.annotate.services.UserService;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.config.name=anot")
@WebAppConfiguration
@ActiveProfiles("test")
public class GetTokenWithAuthServiceMockTest {

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------
    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserService userService;

    // we are mocking the authentication service to achieve desired behavior
    @Mock
    private AuthenticationService authService;

    // -------------------------------------
    // Cleanup of database content
    // -------------------------------------
    @Before
    public void setupTests() {

        MockitoAnnotations.initMocks(this);

        TestDbHelper.cleanupRepositories(this);
    }

    @After
    public void cleanDatabaseAfterTests() {
        TestDbHelper.cleanupRepositories(this);
    }

    // -------------------------------------
    // Tests
    // -------------------------------------

    /**
     * internal exception during /token request, expected HTTP 400 and failure response
     */
    @Test
    @SuppressFBWarnings(value = SpotBugsAnnotations.FieldNotInitialized, justification = SpotBugsAnnotations.FieldNotInitializedReason)
    public void testGetToken_AuthServiceThrowsExceptionWhenResolvingLogin() throws Exception {

        // mock configuration: pretend creating user fails
        Mockito.when(authService.getUserLoginFromToken(Mockito.anyString())).thenThrow(new RuntimeException());

        // send token retrieval request
        final MockHttpServletRequestBuilder builder = MockMvcRequestBuilders
                .get("/api/token?grant_type=jwt-bearer&assertion=xyz");// + authService.createToken(login, clientId));

        // create our own ApiController and hand over required (partly mocked) services
        final MockHttpServletResponse mockResp = new MockHttpServletResponse();
        final AuthApiController controller = new AuthApiController(userService, authService);
        final ResponseEntity<Object> result = controller.getToken(builder.buildRequest(this.wac.getServletContext()), mockResp);

        // expected: Http 400
        Assert.assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatusCodeValue());

        // check that a JsonAuthenticationFailure was returned
        final JsonAuthenticationFailure jsResponse = (JsonAuthenticationFailure) result.getBody();
        Assert.assertNotNull(jsResponse);
        Assert.assertEquals(JsonAuthenticationFailure.getUnknownUserResult(), jsResponse);
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError()));
        Assert.assertFalse(StringUtils.isEmpty(jsResponse.getError_description()));
    }

}
