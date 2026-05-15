package com.moya.myblogboot.repository.implementation;

import com.moya.myblogboot.repository.RefreshTokenRedisRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.moya.myblogboot.domain.keys.RedisKey.REFRESH_FAMILY_KEY;
import static com.moya.myblogboot.domain.keys.RedisKey.REFRESH_ROTATION_RESPONSE_KEY;
import static com.moya.myblogboot.domain.keys.RedisKey.REFRESH_TOKEN_KEY;

@Repository
public class RefreshTokenRedisRepositoryImpl implements RefreshTokenRedisRepository {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<String> rotateScript;

    public RefreshTokenRedisRepositoryImpl(
            @Qualifier("refreshTokenRedisTemplate") StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rotateScript = new DefaultRedisScript<>();
        this.rotateScript.setLocation(new ClassPathResource("scripts/refresh-token-rotate.lua"));
        this.rotateScript.setResultType(String.class);
    }

    @Override
    public void saveInitialToken(String familyId, String jti, Long adminId, Instant now,
                                 Instant absoluteExpiry, Duration familyTtl, Duration refreshTtl) {
        String familyKey = familyKey(familyId);
        String tokenKey = tokenKey(familyId, jti);

        redisTemplate.opsForHash().putAll(familyKey, Map.of(
                "adminId", String.valueOf(adminId),
                "familyCreatedAt", now.toString(),
                "absoluteExpiry", absoluteExpiry.toString(),
                "revoked", "false",
                "revokedAt", "",
                "reason", ""
        ));
        redisTemplate.expire(familyKey, familyTtl);

        redisTemplate.opsForHash().putAll(tokenKey, Map.of(
                "status", "ACTIVE",
                "issuedAt", now.toString(),
                "parentJti", "",
                "nextJti", ""
        ));
        redisTemplate.expire(tokenKey, refreshTtl);
    }

    @Override
    public Optional<Instant> findAbsoluteExpiry(String familyId) {
        Object value = redisTemplate.opsForHash().get(familyKey(familyId), "absoluteExpiry");
        return value == null ? Optional.empty() : Optional.of(Instant.parse(value.toString()));
    }

    @Override
    public String rotate(String familyId, String oldJti, String newJti, Instant now,
                         Duration refreshTtl, Duration graceWindow, String rotationResponseJson) {
        List<String> keys = List.of(
                familyKey(familyId),
                tokenKey(familyId, oldJti),
                tokenKey(familyId, newJti),
                rotationResponseKey(familyId, oldJti)
        );
        return redisTemplate.execute(
                rotateScript,
                keys,
                newJti,
                now.toString(),
                String.valueOf(refreshTtl.toSeconds()),
                String.valueOf(graceWindow.toSeconds()),
                rotationResponseJson,
                oldJti
        );
    }

    @Override
    public void revokeFamily(String familyId, Instant now, String reason) {
        redisTemplate.opsForHash().putAll(familyKey(familyId), Map.of(
                "revoked", "true",
                "revokedAt", now.toString(),
                "reason", reason
        ));
    }

    @Override
    public Optional<String> findFamilyReason(String familyId) {
        Object value = redisTemplate.opsForHash().get(familyKey(familyId), "reason");
        return value == null || value.toString().isBlank() ? Optional.empty() : Optional.of(value.toString());
    }

    private String familyKey(String familyId) {
        return String.format(REFRESH_FAMILY_KEY, familyId);
    }

    private String tokenKey(String familyId, String jti) {
        return String.format(REFRESH_TOKEN_KEY, familyId, jti);
    }

    private String rotationResponseKey(String familyId, String jti) {
        return String.format(REFRESH_ROTATION_RESPONSE_KEY, familyId, jti);
    }
}
