package com.moya.myblogboot.service;

import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.domain.token.ReissuedToken;

public interface AuthService {

    Token adminLogin(LoginReqDto loginReqDto);

    ReissuedToken reissuingAccessToken(String refreshToken);

    TokenInfo getTokenInfo(String token);

    boolean isTokenValid(String token);
}
