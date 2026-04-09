package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;

public interface AuthService {

    Token adminLogin(MemberLoginReqDto loginReqDto);

    String reissuingAccessToken(String refreshToken);

    TokenInfo getTokenInfo(String token);

    boolean tokenIsExpired(String token);
}
