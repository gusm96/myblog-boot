package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
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

    private static SecretKey getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Token 정보
    public static TokenInfo getTokenInfo(String token, String secret) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
        String role = claims.get("role", String.class);
        return TokenInfo.builder()
                .memberPrimaryKey(memberPrimaryKey)
                .role(role)
                .build();
    }

    // Token 검증
    public static void validateToken(String token, String secret) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey(secret))
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("토큰이 만료되었습니다");
        } catch (SecurityException e) {
            throw new SecurityException("유효하지 않은 토큰입니다.", e);
        } catch (MalformedJwtException e) {
            throw new MalformedJwtException("잘못된 토큰 유형입니다", e);
        }
    }

    // Token 생성
    public static Token createToken(Member member, Long accessTokenExpiration, Long refreshTokenExpiration, String secret) {
        String accessToken = jwtBuild(member.getId(), member.getRole().name(), accessTokenExpiration, secret);
        String refreshToken = jwtBuild(member.getId(), member.getRole().name(), refreshTokenExpiration, secret);

        return Token.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .build();
    }

    // JWT builder
    private static String jwtBuild(Long memberPrimaryKey, String role, Long expiration, String secret) {
        return Jwts.builder()
                .claim("memberPrimaryKey", memberPrimaryKey)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(secret))
                .compact();
    }

    // Access Token 재발급
    public static String reissuingToken(TokenInfo tokenInfo, Long accessTokenExpiration, String secret) {
        return jwtBuild(tokenInfo.getMemberPrimaryKey(), tokenInfo.getRole(), accessTokenExpiration, secret);
    }
}
