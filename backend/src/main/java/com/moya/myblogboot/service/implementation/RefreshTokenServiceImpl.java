package com.moya.myblogboot.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.repository.RefreshTokenRedisRepository;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String REUSE_DETECTED = "REUSE_DETECTED";
    private static final String LOGOUT = "LOGOUT";

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    @Value("${jwt.absolute-lifetime}")
    private Long absoluteLifetime;
    @Value("${jwt.grace-window-ms}")
    private Long graceWindowMs;

    @Override
    public IssuedToken issueOnLogin(Admin admin) {
        Instant now = Instant.now();
        Instant absoluteExpiry = now.plusMillis(absoluteLifetime);
        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        String accessToken = JwtUtil.buildAccess(admin.getId(), ROLE_ADMIN, accessTokenExpiration, secret);
        String refreshToken = JwtUtil.buildRefresh(admin.getId(), ROLE_ADMIN, jti, familyId,
                refreshTokenExpiration, secret);

        refreshTokenRedisRepository.saveInitialToken(
                familyId,
                jti,
                admin.getId(),
                now,
                absoluteExpiry,
                Duration.ofMillis(absoluteLifetime).plus(Duration.ofDays(30)),
                Duration.ofMillis(refreshTokenExpiration)
        );
        log.info("Admin login: id={}, family={}", admin.getId(), familyId);
        return new IssuedToken(accessToken, refreshToken);
    }

    @Override
    public ReissuedToken rotate(String presentedRefreshToken) {
        RefreshTokenClaims claims = parsePresentedRefreshToken(presentedRefreshToken);
        Instant now = Instant.now();
        Instant absoluteExpiry = refreshTokenRedisRepository.findAbsoluteExpiry(claims.familyId())
                .orElseThrow(InvalidateTokenException::new);
        Duration refreshTtl = remainingRefreshTtl(now, absoluteExpiry);
        String newJti = UUID.randomUUID().toString();
        String newAccess = JwtUtil.buildAccess(claims.memberPrimaryKey(), claims.role(),
                accessTokenExpiration, secret);
        String newRefresh = JwtUtil.buildRefresh(claims.memberPrimaryKey(), claims.role(), newJti,
                claims.familyId(), refreshTtl.toMillis(), secret);
        ReissuedToken reissuedToken = new ReissuedToken(newAccess, newRefresh);
        String rotationResponseJson = toJson(reissuedToken);

        String result = refreshTokenRedisRepository.rotate(
                claims.familyId(),
                claims.jti(),
                newJti,
                now,
                refreshTtl,
                Duration.ofMillis(graceWindowMs),
                rotationResponseJson
        );

        return parseRotationResult(result, claims.familyId(), reissuedToken);
    }

    @Override
    public void revokeOnLogout(String presentedRefreshToken) {
        try {
            RefreshTokenClaims claims = JwtUtil.parseRefreshToken(presentedRefreshToken, secret);
            refreshTokenRedisRepository.revokeFamily(claims.familyId(), Instant.now(), LOGOUT);
            log.info("Admin logout: family={}", claims.familyId());
        } catch (ExpiredTokenException | InvalidateTokenException e) {
            return;
        }
    }

    private RefreshTokenClaims parsePresentedRefreshToken(String presentedRefreshToken) {
        try {
            return JwtUtil.parseRefreshToken(presentedRefreshToken, secret);
        } catch (ExpiredTokenException e) {
            throw new ExpiredRefreshTokenException();
        }
    }

    private Duration remainingRefreshTtl(Instant now, Instant absoluteExpiry) {
        Duration absoluteRemaining = Duration.between(now, absoluteExpiry);
        if (absoluteRemaining.isZero() || absoluteRemaining.isNegative()) {
            throw new ExpiredRefreshTokenException();
        }
        Duration refreshLifetime = Duration.ofMillis(refreshTokenExpiration);
        return absoluteRemaining.compareTo(refreshLifetime) < 0 ? absoluteRemaining : refreshLifetime;
    }

    private ReissuedToken parseRotationResult(String result, String familyId, ReissuedToken reissuedToken) {
        if ("OK".equals(result)) {
            return reissuedToken;
        }
        if (result != null && result.startsWith("GRACE:")) {
            return fromJson(result.substring("GRACE:".length()));
        }
        if ("REUSE_DETECTED".equals(result)) {
            log.warn("REFRESH_REUSE_DETECTED: family={}", familyId);
            throw new InvalidateTokenException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }
        if ("ABSOLUTE_EXPIRED".equals(result)) {
            throw new ExpiredRefreshTokenException();
        }
        throw new InvalidateTokenException();
    }

    private String toJson(ReissuedToken reissuedToken) {
        try {
            return objectMapper.writeValueAsString(reissuedToken);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rotation response", e);
        }
    }

    private ReissuedToken fromJson(String json) {
        try {
            return objectMapper.readValue(json, ReissuedToken.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize rotation response", e);
        }
    }
}
