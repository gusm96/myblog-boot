package com.moya.myblogboot.domain.token;

public record RefreshTokenClaims(Long memberPrimaryKey, String role, String jti, String familyId) {
}
