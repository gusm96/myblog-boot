package com.moya.myblogboot.utils;

import com.moya.myblogboot.configuration.JwtProperties;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.domain.token.RefreshTokenClaims;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String FAMILY_ID_CLAIM = "familyId";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final long accessTokenExpiration;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.issuer = jwtProperties.issuer();
        this.audience = jwtProperties.audience();
        this.accessTokenExpiration = jwtProperties.accessTokenExpiration();
    }

    public String createAccessToken(Long memberPrimaryKey, String role) {
        return jwtBuild(memberPrimaryKey, role, accessTokenExpiration, ACCESS_TOKEN_TYPE,
                UUID.randomUUID().toString(), null);
    }

    public String createRefreshToken(Long memberPrimaryKey, String role, String jti, String familyId,
                                     long ttlMillis) {
        return jwtBuild(memberPrimaryKey, role, ttlMillis, REFRESH_TOKEN_TYPE, jti, familyId);
    }

    public AccessTokenClaims parseAccessToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);
        Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
        String role = claims.get("role", String.class);
        return new AccessTokenClaims(memberPrimaryKey, role);
    }

    public RefreshTokenClaims parseRefreshToken(String token) {
        Claims claims = parseClaims(token);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);
        Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
        String role = claims.get("role", String.class);
        String jti = claims.getId();
        String familyId = claims.get(FAMILY_ID_CLAIM, String.class);
        if (jti == null || jti.isBlank() || familyId == null || familyId.isBlank()) {
            throw new InvalidateTokenException();
        }
        return new RefreshTokenClaims(memberPrimaryKey, role, jti, familyId);
    }

    public void validateAccessToken(String token) {
        validateTokenType(parseClaims(token), ACCESS_TOKEN_TYPE);
    }

    public void validateRefreshToken(String token) {
        validateTokenType(parseClaims(token), REFRESH_TOKEN_TYPE);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .clockSkewSeconds(30)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidateTokenException();
        }
    }

    private void validateTokenType(Claims claims, String expectedTokenType) {
        String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
        if (!expectedTokenType.equals(tokenType)) {
            throw new InvalidateTokenException();
        }
    }

    private String jwtBuild(Long memberPrimaryKey, String role, long expirationMs, String tokenType,
                            String jti, String familyId) {
        long now = System.currentTimeMillis();
        JwtBuilder builder = Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
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
                .issuedAt(new Date(now))
                .notBefore(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(key)
                .compact();
    }
}
