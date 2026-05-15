package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String FAMILY_ID_CLAIM = "familyId";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static TokenInfo getTokenInfo(String token, String secret) {
        AccessTokenClaims claims = parseAccessToken(token, secret);
        return TokenInfo.builder()
                .memberPrimaryKey(claims.memberPrimaryKey())
                .role(claims.role())
                .build();
    }

    public static AccessTokenClaims parseAccessToken(String token, String secret) {
        try {
            Claims claims = parseClaims(token, secret);
            validateTokenType(claims, ACCESS_TOKEN_TYPE);
            Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
            String role = claims.get("role", String.class);
            return new AccessTokenClaims(memberPrimaryKey, role);
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        }
    }

    public static RefreshTokenClaims parseRefreshToken(String token, String secret) {
        try {
            Claims claims = parseClaims(token, secret);
            validateTokenType(claims, REFRESH_TOKEN_TYPE);
            Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
            String role = claims.get("role", String.class);
            String jti = claims.getId();
            String familyId = claims.get(FAMILY_ID_CLAIM, String.class);
            if (jti == null || jti.isBlank() || familyId == null || familyId.isBlank()) {
                throw new InvalidateTokenException();
            }
            return new RefreshTokenClaims(memberPrimaryKey, role, jti, familyId);
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        }
    }

    public static void validateAccessToken(String token, String secret) {
        try {
            validateTokenType(parseClaims(token, secret), ACCESS_TOKEN_TYPE);
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        }
    }

    public static void validateRefreshToken(String token, String secret) {
        try {
            validateTokenType(parseClaims(token, secret), REFRESH_TOKEN_TYPE);
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        }
    }

    private static void validateTokenType(Claims claims, String expectedTokenType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedTokenType.equals(tokenType)) {
            throw new InvalidateTokenException();
        }
    }

    private static Claims parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static Token createToken(Admin admin, Long accessTokenExpiration,
                                    Long refreshTokenExpiration, String secret) {
        String accessToken = buildAccess(admin.getId(), "ROLE_ADMIN", accessTokenExpiration, secret);
        String refreshToken = buildRefresh(admin.getId(), "ROLE_ADMIN", UUID.randomUUID().toString(),
                UUID.randomUUID().toString(), refreshTokenExpiration, secret);
        return Token.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .build();
    }

    public static String buildAccess(Long memberPrimaryKey, String role, Long expirationMs, String secret) {
        return jwtBuild(memberPrimaryKey, role, expirationMs, ACCESS_TOKEN_TYPE, null, null, secret);
    }

    public static String buildRefresh(Long memberPrimaryKey, String role, String jti, String familyId,
                                      long expirationMs, String secret) {
        return jwtBuild(memberPrimaryKey, role, expirationMs, REFRESH_TOKEN_TYPE, jti, familyId, secret);
    }

    private static String jwtBuild(Long memberPrimaryKey, String role, Long expiration, String tokenType,
                                   String jti, String familyId, String secret) {
        JwtBuilder builder = Jwts.builder()
                .claim("memberPrimaryKey", memberPrimaryKey)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, tokenType);
        if (jti != null) {
            builder.id(jti);
        }
        if (familyId != null) {
            builder.claim(FAMILY_ID_CLAIM, familyId);
        }
        return builder
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(secret))
                .compact();
    }

    public static String reissuingToken(TokenInfo tokenInfo, Long accessTokenExpiration, String secret) {
        return buildAccess(tokenInfo.getMemberPrimaryKey(), tokenInfo.getRole(), accessTokenExpiration, secret);
    }
}
