package com.moya.myblogboot.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "security.login-attempt")
public record LoginAttemptProperties(
        long windowMs,
        long baseDelayMs,
        long maxDelayMs,
        boolean trustedProxy,
        List<LockStage> lockStages
) {
    public record LockStage(
            int failureCount,
            long lockMs
    ) {
    }
}
