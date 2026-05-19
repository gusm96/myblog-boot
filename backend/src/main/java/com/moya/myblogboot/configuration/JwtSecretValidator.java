package com.moya.myblogboot.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtSecretValidator {

    private static final int HS256_MIN_BYTES = 32;

    @Value("${jwt.secret}")
    private String secret;

    @PostConstruct
    void validate() {
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < HS256_MIN_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret must be >= 32 bytes for HS256 (current: " + bytes + ")");
        }
    }
}
