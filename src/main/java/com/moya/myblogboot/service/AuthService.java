package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;

public interface AuthService {

    String memberJoin(MemberJoinReqDto memberJoinReqDto);

    Token memberLogin(LoginReqDto loginReqDto);

    Member retrieveMemberById(Long memberId);

    String reissuingAccessToken(String refreshToken);

    TokenInfo getTokenInfo(String token);

    boolean tokenIsExpired(String token);
}
