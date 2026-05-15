package com.moya.myblogboot.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRedisRepository {

    void saveInitialToken(String familyId, String jti, Long adminId, Instant now,
                          Instant absoluteExpiry, Duration familyTtl, Duration refreshTtl);

    Optional<Instant> findAbsoluteExpiry(String familyId);

    String rotate(String familyId, String oldJti, String newJti, Instant now,
                  Duration refreshTtl, Duration graceWindow, String rotationResponseJson);

    void revokeFamily(String familyId, Instant now, String reason);

    Optional<String> findFamilyReason(String familyId);
}
