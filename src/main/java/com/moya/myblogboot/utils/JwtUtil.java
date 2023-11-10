package com.moya.myblogboot.utils;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

@Slf4j
public class JwtUtil {

    public static TokenInfo getTokenInfo(String token, String secret) {
        Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        Long memberPrimaryKey = claims.get("memberPrimaryKey", Long.class);
        String role = claims.get("role", String.class);
        return TokenInfo.builder()
                .memberPrimaryKey(memberPrimaryKey)
                .role(role)
                .build();
    }

    // Token 만료
    public static boolean isExpired(String token, String secret) {
        try{
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token)
                    .getBody().getExpiration().before(new Date());
            return false;
        }catch (ExpiredJwtException e){
            return true;
        }
    }
    // Token 생성
    public static Token createToken (Member member, String secret, Long accessTokenExpiration, Long refreshTokenExpiration){
        Claims claims = Jwts.claims();
        // JWT에 사용자의 많은 정보를 담으면 보안상 위험하다.
        claims.put("memberPrimaryKey", member.getId());
        claims.put("role", member.getRole());
        String accessToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date((System.currentTimeMillis())))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
        String refreshToken = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date((System.currentTimeMillis())))
                .setExpiration(new Date(System.currentTimeMillis() +refreshTokenExpiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        return Token.builder()
                .access_token(accessToken)
                .refresh_token(refreshToken)
                .build();
    }

    // Access Token 재발급
    public static String reissuingToken(TokenInfo tokenInfo, String secret, Long accessTokenExpiration) {
        Claims claims = Jwts.claims();
        claims.put("memberPrimaryKey", tokenInfo.getMemberPrimaryKey());
        claims.put("role", tokenInfo.getRole());
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date((System.currentTimeMillis())))
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }


}

