package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    public static TokenInfo getTokenInfo(String token, String secret) {
        validateToken(token, secret);
        Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
        String role = claims.get("role", String.class);
        return TokenInfo.builder()
                .memberPrimaryKey(memberPrimaryKey)
                .role(role)
                .build();
    }

    // Token 만료
    public static void validateToken(String token ,String secret) {
        try {
            Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
            Date expiration = claims.getExpiration();
            if (expiration != null && expiration.before(new Date())) {
                throw new ExpiredTokenException("토큰이 만료되었습니다");
            }
        } catch (ExpiredJwtException e) {
            throw new ExpiredTokenException("토큰이 만료되었습니다");
        } catch (SignatureException e) {
            throw new SignatureException("유효하지 않은 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new MalformedJwtException("잘못된 토큰 유형입니다");
        }
    }
    // Token 생성
    public static Token createToken (Member member, Long accessTokenExpiration, Long refreshTokenExpiration, String secret){
        Claims claims = Jwts.claims();
        claims.put("memberPrimaryKey", member.getId());
        claims.put("role", member.getRole());
        String accessToken = jwtBuild(claims, accessTokenExpiration, secret);
        String refreshToken = jwtBuild(claims,refreshTokenExpiration, secret);

        return Token.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .build();
    }

    private static String jwtBuild(Claims claims, Long expiration, String secret) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date((System.currentTimeMillis())))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    // Access Token 재발급
    public static String reissuingToken(TokenInfo tokenInfo, Long accessTokenExpiration, String secret) {
        Claims claims = Jwts.claims();
        claims.put("memberPrimaryKey", tokenInfo.getMemberPrimaryKey());
        claims.put("role", tokenInfo.getRole());
        return jwtBuild(claims, accessTokenExpiration, secret);
    }


}

