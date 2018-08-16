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
package eu.europa.ec.leos.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TokenGeneratorTest extends LeosTest{

    @InjectMocks
    private TokenGenerator tokenGenerator = new JwtTokenGenerator();
    
    @Test
    public void Test_getSecurityTicket() throws Exception {
        //Setup
        String issuer = "testIssuer";
        String secret = "testSecret";

        ReflectionTestUtils.setField(tokenGenerator, "authority", "annotate");
        ReflectionTestUtils.setField(tokenGenerator,"clientId", issuer);
        ReflectionTestUtils.setField(tokenGenerator,"secret", secret);

        //Actual Call
        String ticket = tokenGenerator.getSecurityToken("login", "https://test.com/abc");

        //verification
        assertNotNull(ticket);
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256(secret))
                .build();
        //FIXME: enable when python server is removed
        //assertEquals(verifier.verify(ticket).getAudience().get(0), "test.com");
        assertEquals(verifier.verify(ticket).getIssuer(), issuer);
        assertEquals(verifier.verify(ticket).getSubject(), "acct:login@annotate");
    }
}