package com.moya.myblogboot.service;

import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.domain.token.ReissuedToken;

public interface AuthService {

    default Token adminLogin(LoginReqDto loginReqDto) {
        return adminLogin(loginReqDto, "unknown");
    }

    Token adminLogin(LoginReqDto loginReqDto, String clientIp);

    ReissuedToken reissuingAccessToken(String refreshToken);

    TokenInfo getTokenInfo(String token);

    boolean isTokenValid(String token);
}
