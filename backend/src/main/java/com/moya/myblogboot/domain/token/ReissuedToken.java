package com.moya.myblogboot.domain.token;

public record ReissuedToken(String accessToken, String refreshToken) {
}
