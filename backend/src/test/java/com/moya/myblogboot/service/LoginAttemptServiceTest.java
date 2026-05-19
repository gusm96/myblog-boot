package com.moya.myblogboot.service;

import com.moya.myblogboot.AbstractContainerBaseTest;
import com.moya.myblogboot.RedisTestCleaner;
import com.moya.myblogboot.domain.login.LoginAttemptResult;
import com.moya.myblogboot.exception.custom.TooManyLoginAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "security.login-attempt.window-ms=500",
        "security.login-attempt.base-delay-ms=1",
        "security.login-attempt.max-delay-ms=2"
})
@ActiveProfiles("test")
class LoginAttemptServiceTest extends AbstractContainerBaseTest {

    @Autowired
    private LoginAttemptService loginAttemptService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void beforeEach() {
        RedisTestCleaner.deleteLoginAttemptKeys(stringRedisTemplate);
    }

    @Test
    @DisplayName("5번째 로그인 실패부터 잠금 처리한다")
    void lockOnFifthFailure() {
        String username = "testuser";
        String clientIp = "127.0.0.1";

        for (int i = 1; i <= 4; i++) {
            LoginAttemptResult result = loginAttemptService.onFailure(username, clientIp);
            assertThat(result.count()).isEqualTo(i);
            assertThat(result.locked()).isFalse();
        }

        LoginAttemptResult result = loginAttemptService.onFailure(username, clientIp);

        assertThat(result.count()).isEqualTo(5);
        assertThat(result.locked()).isTrue();
        assertThat(result.retryAfterSeconds()).isBetween(1L, 60L);
        assertThatThrownBy(() -> loginAttemptService.assertNotLocked(username, clientIp))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    @DisplayName("실패 카운터 TTL은 마지막 실패 기준으로 갱신된다")
    void slidingWindowRefreshesTtl() throws Exception {
        String username = "slidingUser";
        String clientIp = "127.0.0.2";
        String failKey = "login:{acct:" + sha256Hex(username) + "}:fail";

        loginAttemptService.onFailure(username, clientIp);
        Thread.sleep(250);
        loginAttemptService.onFailure(username, clientIp);

        Long ttlMs = stringRedisTemplate.getExpire(failKey, TimeUnit.MILLISECONDS);
        assertThat(ttlMs).isNotNull();
        assertThat(ttlMs).isGreaterThan(350L);
    }

    @Test
    @DisplayName("로그인 성공 시 계정과 IP 카운터 및 잠금 키를 삭제한다")
    void successResetsCountersAndLocks() {
        String username = "resetUser";
        String clientIp = "127.0.0.3";

        for (int i = 0; i < 5; i++) {
            loginAttemptService.onFailure(username, clientIp);
        }
        loginAttemptService.onSuccess(username, clientIp);

        assertThat(stringRedisTemplate.hasKey("login:{acct:" + sha256Hex(username) + "}:fail")).isFalse();
        assertThat(stringRedisTemplate.hasKey("login:{acct:" + sha256Hex(username) + "}:lock")).isFalse();
        assertThat(stringRedisTemplate.hasKey("login:{ip:" + sha256Hex(clientIp) + "}:fail")).isFalse();
        assertThat(stringRedisTemplate.hasKey("login:{ip:" + sha256Hex(clientIp) + "}:lock")).isFalse();
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
