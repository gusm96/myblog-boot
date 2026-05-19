package com.moya.myblogboot.domain.login;

public record LoginAttemptResult(
        int count,
        long retryAfterSeconds,
        boolean locked
) {
}
