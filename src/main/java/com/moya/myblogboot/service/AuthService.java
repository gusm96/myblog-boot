package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;

public interface AuthService {

    String memberJoin(MemberJoinReqDto memberJoinReqDto);

    Token memberLogin(MemberLoginReqDto memberLoginReqDto);

    Member retrieve(Long memberId);

    String reissuingAccessToken(String refreshToken);

    TokenInfo getTokenInfo(String token);

    boolean tokenIsExpired(String token);
}
