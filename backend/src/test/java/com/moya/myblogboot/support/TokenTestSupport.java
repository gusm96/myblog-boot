package com.moya.myblogboot.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public final class TokenTestSupport {

    public static final String SECRET = "12345678901234567890123456789012";
    public static final String ISSUER = "myblog";
    public static final String AUDIENCE = "myblog-admin";

    private TokenTestSupport() {
    }

    public static String rawAccessToken(long expMillisFromNow) {
        return rawAccessToken(SECRET, ISSUER, AUDIENCE, expMillisFromNow);
    }

    public static String rawAccessToken(String secret, String issuer, String audience, long expMillisFromNow) {
        return rawToken(secret, issuer, audience, "access", UUID.randomUUID().toString(), null, expMillisFromNow);
    }

    public static String rawRefreshToken(long expMillisFromNow) {
        return rawRefreshToken(SECRET, ISSUER, AUDIENCE, "jti", "family", expMillisFromNow);
    }

    public static String rawRefreshToken(String secret, String issuer, String audience, String jti, String familyId,
                                         long expMillisFromNow) {
        return rawToken(secret, issuer, audience, "refresh", jti, familyId, expMillisFromNow);
    }

    public static SecretKey signingKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private static String rawToken(String secret, String issuer, String audience, String tokenType, String jti,
                                   String familyId, long expMillisFromNow) {
        long now = System.currentTimeMillis();
        var builder = Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .claim("memberPrimaryKey", 1L)
                .claim("role", "ROLE_ADMIN")
                .claim("tokenType", tokenType)
                .issuedAt(new Date(now))
                .notBefore(new Date(now))
                .expiration(new Date(now + expMillisFromNow));
        if (jti != null) {
            builder.id(jti);
        }
        if (familyId != null) {
            builder.claim("familyId", familyId);
        }
        return builder.signWith(signingKey(secret)).compact();
    }
}
