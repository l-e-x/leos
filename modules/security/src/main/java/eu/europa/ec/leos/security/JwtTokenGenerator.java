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
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

/* Default class to generate tokens */
@Component
class JwtTokenGenerator implements TokenGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenGenerator.class);

    // Authority should be made parameter if we need tokens for more than one systems
    @Value("${annotate.authority}")
    private String  authority;

    @Value("${annotate.jwt.issuer.client.id}")
    private String  clientId;

    @Value("${annotate.jwt.issuer.client.secret}")
    private String  secret;
    
    private static final int EXPIRE_IN_MIN = 9;

    @Autowired
    JwtTokenGenerator() {
        LOG.debug("Using jwt security tokens");
    }

    @Override
    public String getSecurityToken(String userLogin, String url) throws Exception {
        Validate.notNull(userLogin, "User login is required for securoty token!!");
        Validate.notNull(url, "URL is required for securoty token!!");
        
        try {
            Date now = Calendar.getInstance().getTime();
            Calendar expires = Calendar.getInstance();
            expires.add(Calendar.MINUTE, EXPIRE_IN_MIN);
            Date expiresAt = expires.getTime();

            Algorithm algorithm = Algorithm.HMAC256(secret);
            String token = JWT.create()
                    .withIssuer(clientId)
                    .withSubject(String.format("acct:%s@%s", userLogin, authority))
                    .withAudience(getDomainName(url))
                    .withIssuedAt(now)
                    .withNotBefore(now)
                    .withExpiresAt(expiresAt)
                    .sign(algorithm);

            return token;
        } catch (UnsupportedEncodingException exception) {
            //UTF-8 encoding not supported
            throw exception;
        } catch (JWTCreationException exception) {
            //Invalid Signing configuration / Couldn't convert Claims.
            throw exception;
        }
    }

    private String getDomainName(String url) {
        try {
            //FIXME: remove override when python server is removed
            //URI uri = new URI(url);
            URI uri = new URI("http://D02DI1321646DIT.net1.cec.eu.int:9099/");
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException ue) {
            return url;
        }
    }
}
