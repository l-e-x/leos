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
package eu.europa.ec.leos.annotate.model.entity;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "TOKENS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ACCESS_TOKEN"}),
        @UniqueConstraint(columnNames = {"REFRESH_TOKEN"})
})
public class Token {

    /**
     * Class representing a set of access and refresh tokens given to a user 
     */

    // -------------------------------------
    // column definitions
    // -------------------------------------

    @Id
    @Column(name = "ID", nullable = false)
    @GenericGenerator(name = "tokensSequenceGenerator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "TOKENS_SEQ"),
            @Parameter(name = "increment_size", value = "1")
    })
    @GeneratedValue(generator = "tokensSequenceGenerator")
    private long id;

    // user ID column, filled by hibernate
    @Column(name = "USER_ID", insertable = false, updatable = false, nullable = false)
    private long userId;

    // associate user, mapped by hibernate using USERS.USER_ID column
    @OneToOne
    @JoinColumn(name = "USER_ID")
    private User user;

    // access token granted to the user
    @Column(name = "ACCESS_TOKEN", nullable = false, unique = true)
    private String accessToken;

    // date/time of expiration of the access token
    @Column(name = "ACCESS_TOKEN_EXPIRES", nullable = false)
    private LocalDateTime accessTokenExpires;

    // the access token's TTL in seconds - property contained here for easier access to this value
    @Transient
    private int accessTokenLifetimeSeconds;

    // refresh token granted to the user
    @Column(name = "REFRESH_TOKEN", nullable = false, unique = true)
    private String refreshToken;

    // date/time of expiration of the refresh token
    @Column(name = "REFRESH_TOKEN_EXPIRES", nullable = false)
    private LocalDateTime refreshTokenExpires;

    // the refresh token's TTL in seconds - property contained here for easier access to this value
    @Transient
    private int refreshTokenLifetimeSeconds;

    // -----------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------

    public Token() {
        // parameterless constructor required by JPA
    }

    public Token(User user, String accessToken, LocalDateTime accessTokenExpiration, String refreshToken, LocalDateTime refreshTokenExpiration) {

        this.user = user;
        this.userId = user.getId();
        this.accessToken = accessToken;
        this.accessTokenExpires = accessTokenExpiration;
        this.refreshToken = refreshToken;
        this.refreshTokenExpires = refreshTokenExpiration;
    }

    // -----------------------------------------------------------
    // Useful getters & setters (exceeding POJO)
    // -----------------------------------------------------------

    public void setAccessToken(String accessToken, int lifetimeSeconds) {
        setAccessToken(accessToken);
        setAccessTokenLifetimeSeconds(lifetimeSeconds);

        setAccessTokenExpires(LocalDateTime.now().plusSeconds(lifetimeSeconds));
    }

    public void setRefreshToken(String refreshToken, int lifetimeSeconds) {
        setRefreshToken(refreshToken);
        setRefreshTokenLifetimeSeconds(lifetimeSeconds);

        setRefreshTokenExpires(LocalDateTime.now().plusSeconds(lifetimeSeconds));
    }

    public boolean isAccessTokenExpired() {
        return this.accessTokenExpires.isBefore(LocalDateTime.now());
    }

    public boolean isRefreshTokenExpired() {
        return this.refreshTokenExpires.isBefore(LocalDateTime.now());
    }

    // -----------------------------------------------------------
    // Getters & setters
    // -----------------------------------------------------------
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public LocalDateTime getAccessTokenExpires() {
        return accessTokenExpires;
    }

    public void setAccessTokenExpires(LocalDateTime accessTokenExpires) {
        this.accessTokenExpires = accessTokenExpires;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public LocalDateTime getRefreshTokenExpires() {
        return refreshTokenExpires;
    }

    public void setRefreshTokenExpires(LocalDateTime refreshTokenExpires) {
        this.refreshTokenExpires = refreshTokenExpires;
    }

    public int getAccessTokenLifetimeSeconds() {
        return accessTokenLifetimeSeconds;
    }

    public void setAccessTokenLifetimeSeconds(int ttl) {
        this.accessTokenLifetimeSeconds = ttl;
    }

    public int getRefreshTokenLifetimeSeconds() {
        return refreshTokenLifetimeSeconds;
    }

    public void setRefreshTokenLifetimeSeconds(int ttl) {
        this.refreshTokenLifetimeSeconds = ttl;
    }

    // -------------------------------------
    // equals and hashCode
    // -------------------------------------

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, user, accessToken, accessTokenExpires, refreshToken, refreshTokenExpires);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Token other = (Token) obj;
        return Objects.equals(this.id, other.id) &&
                Objects.equals(this.userId, other.userId) &&
                Objects.equals(this.user, other.user) &&
                Objects.equals(this.accessToken, other.accessToken) &&
                Objects.equals(this.accessTokenExpires, other.accessTokenExpires) &&
                Objects.equals(this.refreshToken, other.refreshToken) &&
                Objects.equals(this.refreshTokenExpires, other.refreshTokenExpires);
    }

}
