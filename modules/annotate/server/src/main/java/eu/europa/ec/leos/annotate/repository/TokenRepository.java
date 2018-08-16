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
package eu.europa.ec.leos.annotate.repository;

import eu.europa.ec.leos.annotate.model.entity.Token;
import eu.europa.ec.leos.annotate.model.entity.User;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * the repository for all {@link Token} objects granted to users
 */
public interface TokenRepository extends CrudRepository<Token, Long> {

    /**
     * search for an access token
     * 
     * @param accessToken the access token that should be assigned to a user
     * 
     * @return the found {@link Token} object, or {@literal null}
     */
    Token findByAccessToken(String accessToken);

    /**
     * search for a refresh token
     * 
     * @param refreshToken the refresh token that should be assigned to any user
     * 
     * @return the found {@link Token} object, or {@literal null}
     */
    Token findByRefreshToken(String refreshToken);

    /**
     * search for a user based on his id
     * 
     * @param id the user's id
     * @return the found {@link Token} objects
     */
    List<Token> findByUserId(Long id);

    /**
     * search for {@link Token} objects associated to a given user and whose access and refresh tokens expired before a certain time
     * 
     * @param user the user whose tokens are searched
     * @param accessTokenExpiration the expiration date/time for the access token
     * @param refreshTokenExpiration the expiration date/time for the refresh token
     * 
     * @return found {@link Token} objects matching all criteria
     */
    List<Token> findByUserAndAccessTokenExpiresLessThanEqualAndRefreshTokenExpiresLessThanEqual(User user, 
            LocalDateTime accessTokenExpiration,
            LocalDateTime refreshTokenExpiration);
}
