package com.moya.myblogboot.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtSecretValidatorTest {

    @Test
    @DisplayName("jwt.secret이 32바이트 미만이면 기동 검증에 실패한다")
    void rejectsShortSecret() {
        JwtSecretValidator validator = new JwtSecretValidator();
        ReflectionTestUtils.setField(validator, "secret", "1234567890123456789012345678901");

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt.secret must be >= 32 bytes");
    }

    @Test
    @DisplayName("jwt.secret이 32바이트 이상이면 통과한다")
    void acceptsValidSecret() {
        JwtSecretValidator validator = new JwtSecretValidator();
        ReflectionTestUtils.setField(validator, "secret", "12345678901234567890123456789012");

        assertThatCode(validator::validate).doesNotThrowAnyException();
    }
}
