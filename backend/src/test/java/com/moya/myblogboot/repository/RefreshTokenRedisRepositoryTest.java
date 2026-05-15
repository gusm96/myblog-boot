package com.moya.myblogboot.repository;

import com.moya.myblogboot.AbstractContainerBaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.moya.myblogboot.domain.keys.RedisKey.REFRESH_FAMILY_KEY;
import static com.moya.myblogboot.domain.keys.RedisKey.REFRESH_TOKEN_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRedisRepositoryTest extends AbstractContainerBaseTest {

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;
    @Autowired
    @Qualifier("refreshTokenRedisTemplate")
    private StringRedisTemplate redisTemplate;

    @Test
    @DisplayName("ACTIVE refresh token rotates atomically")
    void rotateActiveToken() {
        String familyId = UUID.randomUUID().toString();
        String oldJti = UUID.randomUUID().toString();
        String newJti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        refreshTokenRedisRepository.saveInitialToken(
                familyId,
                oldJti,
                1L,
                now,
                now.plus(Duration.ofDays(30)),
                Duration.ofDays(60),
                Duration.ofDays(14)
        );

        String result = refreshTokenRedisRepository.rotate(
                familyId,
                oldJti,
                newJti,
                now.plusSeconds(1),
                Duration.ofDays(14),
                Duration.ofSeconds(10),
                "{\"accessToken\":\"access\",\"refreshToken\":\"refresh\"}"
        );

        assertThat(result).isEqualTo("OK");
        assertThat(redisTemplate.opsForHash().get(tokenKey(familyId, oldJti), "status")).isEqualTo("ROTATED");
        assertThat(redisTemplate.opsForHash().get(tokenKey(familyId, oldJti), "nextJti")).isEqualTo(newJti);
        assertThat(redisTemplate.opsForHash().get(tokenKey(familyId, newJti), "status")).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("ROTATED refresh token returns cached rotation response during grace window")
    void rotatedTokenReturnsGraceResponse() {
        String familyId = UUID.randomUUID().toString();
        String oldJti = UUID.randomUUID().toString();
        String newJti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        String responseJson = "{\"accessToken\":\"access\",\"refreshToken\":\"refresh\"}";
        refreshTokenRedisRepository.saveInitialToken(
                familyId,
                oldJti,
                1L,
                now,
                now.plus(Duration.ofDays(30)),
                Duration.ofDays(60),
                Duration.ofDays(14)
        );

        refreshTokenRedisRepository.rotate(familyId, oldJti, newJti, now.plusSeconds(1),
                Duration.ofDays(14), Duration.ofSeconds(10), responseJson);
        String result = refreshTokenRedisRepository.rotate(familyId, oldJti, UUID.randomUUID().toString(),
                now.plusSeconds(2), Duration.ofDays(14), Duration.ofSeconds(10), "ignored");

        assertThat(result).isEqualTo("GRACE:" + responseJson);
        assertThat(redisTemplate.opsForHash().get(familyKey(familyId), "revoked")).isEqualTo("false");
    }

    @Test
    @DisplayName("Missing token record revokes family")
    void missingTokenRecordRevokesFamily() {
        String familyId = UUID.randomUUID().toString();
        String oldJti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        refreshTokenRedisRepository.saveInitialToken(
                familyId,
                UUID.randomUUID().toString(),
                1L,
                now,
                now.plus(Duration.ofDays(30)),
                Duration.ofDays(60),
                Duration.ofDays(14)
        );

        String result = refreshTokenRedisRepository.rotate(familyId, oldJti, UUID.randomUUID().toString(),
                now.plusSeconds(1), Duration.ofDays(14), Duration.ofSeconds(10), "{}");

        assertThat(result).isEqualTo("NOT_FOUND");
        assertThat(redisTemplate.opsForHash().get(familyKey(familyId), "revoked")).isEqualTo("true");
        assertThat(redisTemplate.opsForHash().get(familyKey(familyId), "reason")).isEqualTo("REUSE_DETECTED");
    }

    private String familyKey(String familyId) {
        return String.format(REFRESH_FAMILY_KEY, familyId);
    }

    private String tokenKey(String familyId, String jti) {
        return String.format(REFRESH_TOKEN_KEY, familyId, jti);
    }
}
