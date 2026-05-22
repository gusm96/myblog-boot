package com.moya.myblogboot.utils;

import com.moya.myblogboot.configuration.JwtProperties;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.domain.token.Role;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.moya.myblogboot.support.TokenTestSupport.AUDIENCE;
import static com.moya.myblogboot.support.TokenTestSupport.ISSUER;
import static com.moya.myblogboot.support.TokenTestSupport.SECRET;
import static com.moya.myblogboot.support.TokenTestSupport.rawAccessToken;
import static com.moya.myblogboot.support.TokenTestSupport.signingKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties());

    @Test
    @DisplayName("Access token and refresh token cannot pass each other's validation path")
    void validateTokenType() {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "ROLE_ADMIN", "jti-1", "family-1",
                1_209_600_000L);

        assertThatCode(() -> jwtTokenProvider.validateAccessToken(accessToken))
                .doesNotThrowAnyException();
        assertThatCode(() -> jwtTokenProvider.validateRefreshToken(refreshToken))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(refreshToken))
                .isInstanceOf(InvalidateTokenException.class);
        assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(accessToken))
                .isInstanceOf(InvalidateTokenException.class);
    }

    @Test
    @DisplayName("Access and refresh tokens include expected standard claims")
    void standardClaims() {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "ROLE_ADMIN");
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "ROLE_ADMIN", "jti-1", "family-1",
                1_209_600_000L);

        Claims accessClaims = parseRaw(accessToken);
        Claims refreshClaims = parseRaw(refreshToken);

        assertThat(accessClaims.getId()).isNotBlank();
        assertThat(accessClaims.getIssuer()).isEqualTo(ISSUER);
        assertThat(accessClaims.getAudience()).contains(AUDIENCE);
        assertThat(accessClaims.getNotBefore()).isNotNull();
        assertThat(refreshClaims.getId()).isEqualTo("jti-1");
        assertThat(refreshClaims.get("familyId", String.class)).isEqualTo("family-1");
        assertThat(refreshClaims.getIssuer()).isEqualTo(ISSUER);
        assertThat(refreshClaims.getAudience()).contains(AUDIENCE);
    }

    @Test
    @DisplayName("Refresh token has jti and familyId claims")
    void refreshTokenClaims() {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L, "ROLE_ADMIN", "jti-1", "family-1",
                1_209_600_000L);

        RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(refreshToken);

        assertThat(claims.jti()).isEqualTo("jti-1");
        assertThat(claims.familyId()).isEqualTo("family-1");
    }

    @Test
    @DisplayName("Wrong issuer or audience is invalid")
    void wrongIssuerOrAudience() {
        String wrongIssuer = rawAccessToken(SECRET, "other-issuer", AUDIENCE, 60_000L);
        String wrongAudience = rawAccessToken(SECRET, ISSUER, "other-audience", 60_000L);

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(wrongIssuer))
                .isInstanceOf(InvalidateTokenException.class);
        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(wrongAudience))
                .isInstanceOf(InvalidateTokenException.class);
    }

    @Test
    @DisplayName("Expired token is mapped to ExpiredTokenException")
    void expiredToken() {
        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(rawAccessToken(-60_000L)))
                .isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    @DisplayName("Malformed token and missing required claims are invalid")
    void invalidTokens() {
        String missingIssuer = rawAccessToken(SECRET, null, AUDIENCE, 60_000L);

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken("invalid-token"))
                .isInstanceOf(InvalidateTokenException.class);
        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(missingIssuer))
                .isInstanceOf(InvalidateTokenException.class);
    }

    @Test
    @DisplayName("Role authority is stable")
    void roleAuthority() {
        assertThat(Role.ADMIN.getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    private Claims parseRaw(String token) {
        return Jwts.parser()
                .verifyWith(signingKey(SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtProperties jwtProperties() {
        return new JwtProperties(SECRET, ISSUER, AUDIENCE, 600_000L, 1_209_600_000L,
                2_592_000_000L, 10_000L);
    }
}
