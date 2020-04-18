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
package eu.europa.ec.leos.annotate.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import eu.europa.ec.leos.annotate.model.UserInformation;
import eu.europa.ec.leos.annotate.model.entity.AuthClient;
import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import eu.europa.ec.leos.annotate.repository.AuthClientRepository;
import eu.europa.ec.leos.annotate.repository.TokenRepository;
import eu.europa.ec.leos.annotate.services.AuthenticationServiceWithTestFunctions;
import eu.europa.ec.leos.annotate.services.UUIDGeneratorService;
import eu.europa.ec.leos.annotate.services.exceptions.AccessTokenExpiredException;
import eu.europa.ec.leos.annotate.services.exceptions.CannotStoreTokenException;
import eu.europa.ec.leos.annotate.services.exceptions.TokenFromUnknownClientException;
import eu.europa.ec.leos.annotate.services.exceptions.TokenInvalidForClientAuthorityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service containing all user authentication functionality
 */
@Service
public class AuthenticationServiceImpl implements AuthenticationServiceWithTestFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private static final Pattern SUBJECT_PATTERN = Pattern.compile("acct:(.+)@(.+)");

    // internal list of all clients, enables easy access to algorithms, JWTVerifier and configuration data
    private List<RegisteredClient> clients;

    // -------------------------------------
    // Required services and repositories
    // -------------------------------------

    @Autowired
    private final AuthClientRepository authClientRepos;

    @Autowired
    private final TokenRepository tokenRepository;

    @Autowired
    private UUIDGeneratorService uuidService;

    @Value("${token.access.lifetime}")
    private int lifetimeAccessToken;

    @Value("${token.refresh.lifetime}")
    private int lifetimeRefreshToken;

    @Autowired
    private AuthenticatedUserStore authUserStore;

    // -------------------------------------
    // Constructor
    // -------------------------------------
    @Autowired
    public AuthenticationServiceImpl(final AuthClientRepository authClientRepos, final TokenRepository tokenRepos) {

        this.authClientRepos = authClientRepos;
        this.tokenRepository = tokenRepos;
    }

    /**
     * creation of a JWT token
     * NOTE: currently used for tests only
     * 
     * @param userId the user login to be encoded in the JWT token
     * @param clientId the ID of the client to use (client ID, not DB table ID!)
     * @return the JWT token as string
     */
    @Override
    public String createToken(final String userId, final String clientId) {

        final int EXPIRE_IN_MIN = 50000; // large value for simplifying testing

        final Date now = Calendar.getInstance().getTime();
        final Calendar expires = Calendar.getInstance();
        expires.add(Calendar.MINUTE, EXPIRE_IN_MIN);
        final Date expiresAt = expires.getTime();

        // read clients from DB, then search the one having the given client ID
        refreshClientList();
        final RegisteredClient registeredClient = this.clients.stream().filter(regClient -> regClient.getClient().getClientId().equals(clientId))
                .findFirst()
                .get();

        // any more claims we need to verify?
        return JWT.create()
                .withIssuer(registeredClient.getClient().getClientId())
                .withSubject(String.format("acct:%s@%s", userId, registeredClient.getClient().getAuthorities()))
                .withIssuedAt(now)
                .withNotBefore(now)
                .withExpiresAt(expiresAt)
                .sign(registeredClient.getAlgorithm());
    }

    /**
     * extract the user login from a given token
     * 
     * @param token the token, as String
     * @return extracted user login
     * 
     * @throws TokenFromUnknownClientException this exception is thrown when none of the registered clients can decode the token
     * @throws TokenInvalidForClientAuthorityException this exception is thrown when the decoding client may not authenticate the authority issuing the token
     */
    @Override
    public UserInformation getUserLoginFromToken(final String token) throws TokenFromUnknownClientException, TokenInvalidForClientAuthorityException {

        try {
            final AtomicReference<AuthClient> clientThatDecodedRef = new AtomicReference<AuthClient>();
            final DecodedJWT jwt = tryDecoding(token, clientThatDecodedRef);
            if (jwt == null) {
                throw new TokenFromUnknownClientException(String.format("Received token '%s' could not be decoded with registered clients", token));
            }
            final String subject = jwt.getSubject();
            final Matcher match = SUBJECT_PATTERN.matcher(subject);
            if (!match.matches()) {
                return null;
            }

            final AuthClient client = clientThatDecodedRef.get();

            // check authority - client does not have any? -> token accepted
            if (client.getAuthoritiesList() == null) {
                LOG.debug("Client may authenticate all authorities -> pass extracted user");
                return new UserInformation(match.group(1), match.group(2));
            }

            if (client.getAuthoritiesList().contains(match.group(2))) {
                LOG.debug("Client may authenticate authority '{}' -> pass extracted user", match.group(2));
                return new UserInformation(match.group(1), match.group(2));
            }
            LOG.info("Client {} may not authenticate authority '{}' -> token ignored", client.getId(), match.group(2));
            throw new TokenInvalidForClientAuthorityException(String.format("Client %s may not authenticate authority '%s'", client.getId(), match.group(2)));

        } catch (Exception e) {
            LOG.error("Received exception during token verification", e);
            throw e;
        }
    }

    /**
     * loop over registered clients and see if any can be used for decoding the received token
     * 
     * @param token token to be decoded
     * @return returns a decoded token, or {@literal null} if no registered client can decode it
     */
    private DecodedJWT tryDecoding(final String token, final AtomicReference<AuthClient> successfulClientByRef) {

        refreshClientList();

        for (final RegisteredClient registeredClient : this.clients) {

            try {
                final DecodedJWT decoded = registeredClient.getVerifier().verify(token);
                LOG.info("Verified received token using client {}", registeredClient.getClient().getId());
                successfulClientByRef.set(registeredClient.getClient());
                return decoded;
            } catch (JWTVerificationException verifExc) {
                LOG.info("Received token could not be verified for client {}", registeredClient.getClient().getId());
            }
        }

        // when reaching this point, no client could verify the token!
        return null;
    }

    public String getDomainName(final String url) {
        try {
            final URI uri = new URI(url);
            final String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException ue) {
            return url;
        }
    }

    /**
     * Extracts the access token from the given request and returns the user name
     * 
     * It checks against the database to ensure that the user is authenticated
     * 
     * @param request the incoming HttpServletRequest containing the 'Authorization' header
     * 
     * @return the proper user name or {@literal null} if the user is not authenticated / an invalid or no header is presented
     * 
     * @throws AccessTokenExpiredException this exception is thrown when the provided access token is known, but has expired
     */
    @Override
    public String getUserLogin(final HttpServletRequest request) throws AccessTokenExpiredException {

        final String authenticationHeader = request.getHeader("Authorization");

        final String bearer = "Bearer ";

        if (StringUtils.isEmpty(authenticationHeader) || !authenticationHeader.startsWith(bearer)) {
            authUserStore.clear();
            return null;
        }

        final String accessToken = authenticationHeader.substring(bearer.length());

        final UserInformation info = findUserByAccessToken(accessToken);
        authUserStore.setUserInfo(info);

        if (info == null) {
            return null;
        }

        final Token foundToken = info.getCurrentToken();
        if (foundToken != null && foundToken.isAccessTokenExpired()) {
            throw new AccessTokenExpiredException();
        }

        return info.getLogin();
    }

    /**
     * have access and refresh tokens generated for a user and stored directly
     * 
     * @param userInfo the {@link UserInformation} containing user for which tokens are to be generated
     * @return updated {@link User} object with new tokens set
     * 
     * @throws CannotStoreTokenException exception thrown when storing the token fails
     */
    @Override
    public Token generateAndSaveTokensForUser(final UserInformation userInfo) throws CannotStoreTokenException {

        // store access token / refresh token in database
        final Token newUserToken = storeTokensForUser(userInfo.getUser(), userInfo.getAuthority(), uuidService.generateUrlSafeUUID(),
                uuidService.generateUrlSafeUUID());

        cleanupExpiredUserTokens(userInfo.getUser());
        return newUserToken;
    }

    /**
     * find the user owning a certain refresh token
     *
     * @param refreshToken the refresh token previously given to a user
     * 
     * @return found {@Link UserInformation}, or {@literal null}
     */
    @Override
    public UserInformation findUserByRefreshToken(final String refreshToken) {

        if (StringUtils.isEmpty(refreshToken)) {
            LOG.error("Cannot search for user by empty refresh token");
            return null;
        }

        final Token foundToken = tokenRepository.findByRefreshToken(refreshToken);
        final UserInformation result = new UserInformation(foundToken);
        if (foundToken == null) {
            LOG.debug("Refresh token '{}' not found in database", refreshToken);
            return result;
        }

        // note: due to DB constraints, user must be valid if token is valid
        LOG.debug("Found refresh token '{}', belongs to user '{}'", refreshToken, foundToken.getUser().getLogin());

        if (foundToken.isRefreshTokenExpired()) {
            LOG.info("Found refresh token '{}' is already expired", refreshToken);
        }
        return result;
    }

    /**
     * find the user owning a certain access token
     *
     * @param accessToken the access token previously given to a user
     * 
     * @return found {@link UserInformation}, or {@literal null}
     */
    @Override
    public UserInformation findUserByAccessToken(final String accessToken) {

        if (StringUtils.isEmpty(accessToken)) {
            LOG.error("Cannot search for user by empty access token");
            return null;
        }

        final Token foundToken = tokenRepository.findByAccessToken(accessToken);
        final UserInformation result = new UserInformation(foundToken);
        if (foundToken == null) {
            LOG.debug("Access token '{}' not found in database", accessToken);
            return result;
        }

        // note: due to DB constraints, user must be valid if token is valid
        LOG.debug("Found access token '{}', belongs to user '{}'", accessToken, foundToken.getUser().getLogin());

        if (foundToken.isAccessTokenExpired()) {
            LOG.debug("Found access token '{}' is already expired", accessToken);
        }
        return result;
    }

    /**
     * persistence of a set of access and refresh token for a user 
     * note: creates the user if it does not exist yet
     * 
     * @param user the {@link User} for whom to store tokens
     * @param authority the authority to which the token is bound
     * @param accessToken the access token to be stored
     * @param refreshToken the refresh token to be stored
     * 
     * @return {@link Token} with updated properties
     *
     * @throws CannotStoreTokenException exception thrown when storing the token fails or when required data is missing
     */
    private Token storeTokensForUser(final User user, final String authority, final String accessToken, final String refreshToken) throws CannotStoreTokenException {

        if (user == null) {
            throw new CannotStoreTokenException("No user available");
        }
        if (StringUtils.isEmpty(authority)) {
            throw new CannotStoreTokenException("Authority for which the token is issued is missing!");
        }
        if (StringUtils.isEmpty(accessToken) || StringUtils.isEmpty(refreshToken)) {
            throw new CannotStoreTokenException("Access and/or refresh token to be stored is/are missing!");
        }

        final Token newToken = new Token();
        newToken.setAccessToken(accessToken, lifetimeAccessToken);
        newToken.setRefreshToken(refreshToken, lifetimeRefreshToken);
        newToken.setUser(user);
        newToken.setAuthority(authority);

        try {
            tokenRepository.save(newToken);
        } catch (Exception e) {
            LOG.error("Error storing tokens for user", e);
            throw new CannotStoreTokenException("Error saving tokens", e);
        }

        return newToken;
    }

    /**
     * cleanup procedure for removing tokens that have already expired - they only consume space and no longer serve any purpose
     * 
     * @param user the {@link User} whose tokens are to be cleaned up
     * 
     * @return {@literal true} if something was cleaned; {@literal false} if there was nothing to clean or an error occured 
     */
    @Override
    public boolean cleanupExpiredUserTokens(final User user) {

        if (user == null) {
            LOG.warn("Received invalid user for database token cleanup");
            return false;
        }

        boolean cleanedSomething = false;

        try {
            final List<Token> expiredAccessTokens = tokenRepository.findByUserAndAccessTokenExpiresLessThanEqualAndRefreshTokenExpiresLessThanEqual(user,
                    LocalDateTime.now(), LocalDateTime.now());
            if (!expiredAccessTokens.isEmpty()) {
                LOG.debug("Discovered {} expired access tokens for user '{}'; delete them", expiredAccessTokens.size(), user.getLogin());
                tokenRepository.delete(expiredAccessTokens);
                cleanedSomething = true;
            }
        } catch (Exception e) {
            LOG.error("Unexpected error upon cleaning expired access tokens", e);
        }

        return cleanedSomething;
    }

    /**
     * read the database content and (re)initialise the internal list of clients
     */
    private void refreshClientList() {

        clients = new ArrayList<RegisteredClient>();

        final List<AuthClient> clientsInDb = (List<AuthClient>) authClientRepos.findAll();
        for (final AuthClient cl : clientsInDb) {

            try {
                final Algorithm alg = Algorithm.HMAC256(cl.getSecret());

                // any more things to verify (i.e. claims)?
                final JWTVerifier jwtVerifier = JWT.require(alg)
                        .withIssuer(cl.getClientId())
                        .acceptLeeway(0) // no grace period for timing issues, fail immediately
                        .acceptNotBefore(0)
                        .acceptIssuedAt(0)
                        .acceptExpiresAt(0)
                        .build(); // Reusable verifier instance

                clients.add(new RegisteredClient(cl, alg, jwtVerifier));
            } catch (IllegalArgumentException | UnsupportedEncodingException e) {
                // note: initialisation failure of one client should not block other clients as well - therefore we don't throw the exception!
                LOG.error("Could not initialize JWT verification for client " + cl.getId(), e);
            }
        }
    }

    /**
     * internal class to ease access from a given configured client to its algorithm and token verifier
     */
    private static class RegisteredClient {

        private final AuthClient client;
        private final Algorithm algorithm;
        private final JWTVerifier verifier;

        // -------------------------------------
        // Constructor
        // -------------------------------------
        public RegisteredClient(final AuthClient client, final Algorithm algorithm, final JWTVerifier verifier) {
            this.client = client;
            this.algorithm = algorithm;
            this.verifier = verifier;
        }

        // -------------------------------------
        // Getter
        // -------------------------------------
        public AuthClient getClient() {
            return client;
        }

        public Algorithm getAlgorithm() {
            return algorithm;
        }

        public JWTVerifier getVerifier() {
            return verifier;
        }
    }

}
