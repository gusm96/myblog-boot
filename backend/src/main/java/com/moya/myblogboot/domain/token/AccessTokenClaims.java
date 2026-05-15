package com.moya.myblogboot.domain.token;

public record AccessTokenClaims(Long memberPrimaryKey, String role) {
}
