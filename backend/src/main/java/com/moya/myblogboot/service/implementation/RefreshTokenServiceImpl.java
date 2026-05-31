package com.moya.myblogboot.service.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moya.myblogboot.configuration.JwtProperties;
import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.domain.token.Role;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.repository.RefreshTokenRedisRepository;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String REUSE_DETECTED = "REUSE_DETECTED";
    private static final String LOGOUT = "LOGOUT";

    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public IssuedToken issueOnLogin(Admin admin) {
        Instant now = Instant.now();
        Instant absoluteExpiry = now.plusMillis(jwtProperties.absoluteLifetime());
        String familyId = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();
        String role = Role.ADMIN.getAuthority();
        String accessToken = jwtTokenProvider.createAccessToken(admin.getId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId(), role, jti, familyId,
                jwtProperties.refreshTokenExpiration());

        refreshTokenRedisRepository.saveInitialToken(
                familyId,
                jti,
                admin.getId(),
                now,
                absoluteExpiry,
                Duration.ofMillis(jwtProperties.absoluteLifetime()).plus(Duration.ofDays(30)),
                Duration.ofMillis(jwtProperties.refreshTokenExpiration())
        );
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
        String newAccess = jwtTokenProvider.createAccessToken(claims.memberPrimaryKey(), claims.role());
        String newRefresh = jwtTokenProvider.createRefreshToken(claims.memberPrimaryKey(), claims.role(), newJti,
                claims.familyId(), refreshTtl.toMillis());
        ReissuedToken reissuedToken = new ReissuedToken(newAccess, newRefresh);
        String rotationResponseJson = toJson(reissuedToken);

        String result = refreshTokenRedisRepository.rotate(
                claims.familyId(),
                claims.jti(),
                newJti,
                now,
                refreshTtl,
                Duration.ofMillis(jwtProperties.graceWindowMs()),
                rotationResponseJson
        );

        return parseRotationResult(result, claims.familyId(), reissuedToken);
    }

    @Override
    public void revokeOnLogout(String presentedRefreshToken) {
        try {
            RefreshTokenClaims claims = jwtTokenProvider.parseRefreshToken(presentedRefreshToken);
            refreshTokenRedisRepository.revokeFamily(claims.familyId(), Instant.now(), LOGOUT);
        } catch (ExpiredTokenException | InvalidateTokenException e) {
            return;
        }
    }

    private RefreshTokenClaims parsePresentedRefreshToken(String presentedRefreshToken) {
        try {
            return jwtTokenProvider.parseRefreshToken(presentedRefreshToken);
        } catch (ExpiredTokenException e) {
            throw new ExpiredRefreshTokenException();
        }
    }

    private Duration remainingRefreshTtl(Instant now, Instant absoluteExpiry) {
        Duration absoluteRemaining = Duration.between(now, absoluteExpiry);
        if (absoluteRemaining.isZero() || absoluteRemaining.isNegative()) {
            throw new ExpiredRefreshTokenException();
        }
        Duration refreshLifetime = Duration.ofMillis(jwtProperties.refreshTokenExpiration());
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
