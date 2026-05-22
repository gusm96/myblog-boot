package com.moya.myblogboot.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.moya.myblogboot.support.TokenTestSupport.AUDIENCE;
import static com.moya.myblogboot.support.TokenTestSupport.ISSUER;
import static com.moya.myblogboot.support.TokenTestSupport.SECRET;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    @DisplayName("jwt.secret이 32바이트 미만이면 기동 검증에 실패한다")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> properties("1234567890123456789012345678901", ISSUER, AUDIENCE,
                600_000L, 1_209_600_000L, 2_592_000_000L, 10_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret must be >= 32 bytes");
    }

    @Test
    @DisplayName("issuer와 audience는 blank일 수 없다")
    void rejectsBlankIssuerOrAudience() {
        assertThatThrownBy(() -> properties(SECRET, " ", AUDIENCE, 600_000L, 1_209_600_000L,
                2_592_000_000L, 10_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.issuer must not be blank");
        assertThatThrownBy(() -> properties(SECRET, ISSUER, " ", 600_000L, 1_209_600_000L,
                2_592_000_000L, 10_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.audience must not be blank");
    }

    @Test
    @DisplayName("만료 설정은 양수이고 grace window는 음수일 수 없다")
    void rejectsInvalidDurations() {
        assertThatThrownBy(() -> properties(SECRET, ISSUER, AUDIENCE, 0L, 1_209_600_000L,
                2_592_000_000L, 10_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be positive");
        assertThatThrownBy(() -> properties(SECRET, ISSUER, AUDIENCE, 600_000L, 1_209_600_000L,
                2_592_000_000L, -1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.grace-window-ms must not be negative");
    }

    @Test
    @DisplayName("유효한 JWT 설정이면 통과한다")
    void acceptsValidProperties() {
        assertThatCode(() -> properties(SECRET, ISSUER, AUDIENCE, 600_000L, 1_209_600_000L,
                2_592_000_000L, 10_000L))
                .doesNotThrowAnyException();
    }

    private JwtProperties properties(String secret, String issuer, String audience, long accessTokenExpiration,
                                     long refreshTokenExpiration, long absoluteLifetime, long graceWindowMs) {
        return new JwtProperties(secret, issuer, audience, accessTokenExpiration, refreshTokenExpiration,
                absoluteLifetime, graceWindowMs);
    }
}
