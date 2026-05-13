package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
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
        TokenInfo tokenInfo = JwtUtil.getTokenInfo(token.getRefresh_token(), SECRET);

        String reissuedAccessToken = JwtUtil.reissuingToken(tokenInfo, ACCESS_TOKEN_EXPIRATION, SECRET);

        assertThatCode(() -> JwtUtil.validateAccessToken(reissuedAccessToken, SECRET))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> JwtUtil.validateRefreshToken(reissuedAccessToken, SECRET))
                .isInstanceOf(InvalidateTokenException.class);
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
