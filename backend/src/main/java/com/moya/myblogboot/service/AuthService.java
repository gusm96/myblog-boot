package com.moya.myblogboot.service;

import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;

public interface AuthService {

    Token adminLogin(LoginReqDto loginReqDto);

    String reissuingAccessToken(String refreshToken);

    TokenInfo getTokenInfo(String token);

    boolean tokenIsExpired(String token);
}
