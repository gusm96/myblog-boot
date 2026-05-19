package com.moya.myblogboot.domain.token;

public record TokenMetaResponse(
        String tokenType,
        long expiresIn
) {
}
