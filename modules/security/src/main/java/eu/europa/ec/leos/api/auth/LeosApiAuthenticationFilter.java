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
package eu.europa.ec.leos.api.auth;

import eu.europa.ec.leos.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class LeosApiAuthenticationFilter extends AbstractAuthenticationProcessingFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(LeosApiAuthenticationFilter.class);
    private static final String AUTHORIZATION = "Authorization";
    private TokenService tokenService;
    
    public LeosApiAuthenticationFilter(String filterProcessorUrl, TokenService tokenService) {
        super(filterProcessorUrl);
        this.tokenService = tokenService;
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException {
        JwtAuthenticationToken authRequest = new JwtAuthenticationToken();
        if (request.getHeader(AUTHORIZATION) != null && request.getHeader(AUTHORIZATION).startsWith("Bearer ")) {
            if (tokenService.validateAccessToken(request.getHeader(AUTHORIZATION).substring(7))) {
                authRequest.setAuthenticated(true);
            } else {
                LOG.warn("Authorization failed! Wrong accessToken");
            }
        } else {
            LOG.warn("Authorization failed! Wrong Headers: '{}' is missing or contains a wrong value", AUTHORIZATION);
        }
        
        return getAuthenticationManager().authenticate(authRequest);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        super.successfulAuthentication(request, response, chain, authResult);
        chain.doFilter(request, response);
    }
}