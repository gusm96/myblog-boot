package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        String issuer,
        String audience,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        long absoluteLifetime,
        long graceWindowMs
) {
    private static final int HS256_MIN_BYTES = 32;

    public JwtProperties {
        int bytes = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < HS256_MIN_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be >= 32 bytes for HS256 (current: " + bytes + ")");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException("jwt.issuer must not be blank");
        }
        if (audience == null || audience.isBlank()) {
            throw new IllegalStateException("jwt.audience must not be blank");
        }
        if (accessTokenExpiration <= 0 || refreshTokenExpiration <= 0 || absoluteLifetime <= 0) {
            throw new IllegalStateException(
                    "jwt access/refresh expiration and absolute-lifetime must be positive");
        }
        if (graceWindowMs < 0) {
            throw new IllegalStateException("jwt.grace-window-ms must not be negative");
        }
    }
}
