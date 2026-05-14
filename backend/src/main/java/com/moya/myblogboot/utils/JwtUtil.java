package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.admin.Admin;
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

@Slf4j
@Component
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static TokenInfo getTokenInfo(String token, String secret) {
        Claims claims = parseClaims(token, secret);
        Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
        String role = claims.get("role", String.class);
        return TokenInfo.builder()
                .memberPrimaryKey(memberPrimaryKey)
                .role(role)
                .build();
    }

    public static void validateAccessToken(String token, String secret) {
        validateTokenType(token, secret, ACCESS_TOKEN_TYPE);
    }

    public static void validateRefreshToken(String token, String secret) {
        validateTokenType(token, secret, REFRESH_TOKEN_TYPE);
    }

    private static void validateTokenType(String token, String secret, String expectedTokenType) {
        try {
            Claims claims = parseClaims(token, secret);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            if (!expectedTokenType.equals(tokenType)) {
                throw new InvalidateTokenException();
            }
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException();
        } catch (SecurityException e) {
            throw new SecurityException("Invalid token.", e);
        } catch (MalformedJwtException e) {
            throw new MalformedJwtException("Malformed token.", e);
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
        String accessToken = jwtBuild(admin.getId(), "ROLE_ADMIN", accessTokenExpiration, ACCESS_TOKEN_TYPE, secret);
        String refreshToken = jwtBuild(admin.getId(), "ROLE_ADMIN", refreshTokenExpiration, REFRESH_TOKEN_TYPE, secret);
        return Token.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .build();
    }

    private static String jwtBuild(Long memberPrimaryKey, String role, Long expiration, String tokenType, String secret) {
        return Jwts.builder()
                .claim("memberPrimaryKey", memberPrimaryKey)
                .claim("role", role)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(secret))
                .compact();
    }

    public static String reissuingToken(TokenInfo tokenInfo, Long accessTokenExpiration, String secret) {
        return jwtBuild(tokenInfo.getMemberPrimaryKey(), tokenInfo.getRole(), accessTokenExpiration, ACCESS_TOKEN_TYPE, secret);
    }
}
