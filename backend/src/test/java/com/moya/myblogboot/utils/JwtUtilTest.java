package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "12345678901234567890123456789012";
    private static final long ACCESS_TOKEN_EXPIRATION = 600_000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 1_209_600_000L;

    @Test
    @DisplayName("Access token and refresh token cannot pass each other's validation path")
    void validateTokenType() {
        Token token = createToken();

        assertThatCode(() -> JwtUtil.validateAccessToken(token.getAccess_token(), SECRET))
                .doesNotThrowAnyException();
        assertThatCode(() -> JwtUtil.validateRefreshToken(token.getRefresh_token(), SECRET))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JwtUtil.validateAccessToken(token.getRefresh_token(), SECRET))
                .isInstanceOf(InvalidateTokenException.class);
        assertThatThrownBy(() -> JwtUtil.validateRefreshToken(token.getAccess_token(), SECRET))
                .isInstanceOf(InvalidateTokenException.class);
    }

    @Test
    @DisplayName("Reissued token from refresh token has access token type")
    void reissuingTokenCreatesAccessToken() {
        Token token = createToken();
        RefreshTokenClaims tokenInfo = JwtUtil.parseRefreshToken(token.getRefresh_token(), SECRET);

        String reissuedAccessToken = JwtUtil.buildAccess(tokenInfo.memberPrimaryKey(), tokenInfo.role(),
                ACCESS_TOKEN_EXPIRATION, SECRET);

        assertThatCode(() -> JwtUtil.validateAccessToken(reissuedAccessToken, SECRET))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JwtUtil.validateRefreshToken(reissuedAccessToken, SECRET))
                .isInstanceOf(InvalidateTokenException.class);
    }

    @Test
    @DisplayName("Refresh token has jti and familyId claims")
    void refreshTokenClaims() {
        String refreshToken = JwtUtil.buildRefresh(1L, "ROLE_ADMIN", "jti-1", "family-1",
                REFRESH_TOKEN_EXPIRATION, SECRET);

        RefreshTokenClaims claims = JwtUtil.parseRefreshToken(refreshToken, SECRET);

        org.assertj.core.api.Assertions.assertThat(claims.jti()).isEqualTo("jti-1");
        org.assertj.core.api.Assertions.assertThat(claims.familyId()).isEqualTo("family-1");
    }

    private Token createToken() {
        Admin admin = Admin.builder()
                .username("admin")
                .password("password")
                .build();
        ReflectionTestUtils.setField(admin, "id", 1L);
        return JwtUtil.createToken(admin, ACCESS_TOKEN_EXPIRATION, REFRESH_TOKEN_EXPIRATION, SECRET);
    }
}
