package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.*;
import com.moya.myblogboot.exception.BusinessException;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.DuplicateException;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    // 회원 가입
    @Override
    @Transactional
    public void memberJoin(MemberJoinReqDto memberJoinReqDto) {
        // 아이디 중복 체크
        validateUsername(memberJoinReqDto.getUsername());
        // Member Entity 생성
        Member newMember = memberJoinReqDto.toEntity(passwordEncoder);
        // Member Persist
        memberRepository.save(newMember);
    }

    @Override
    public Token memberLogin(MemberLoginReqDto memberLoginReqDto) {
        // username 으로 회원 찾기
        Member findMember = memberRepository.findByUsername(memberLoginReqDto.getUsername()).orElseThrow(()
                -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        // password 비교
        if (!passwordEncoder.matches(memberLoginReqDto.getPassword(), findMember.getPassword()))
            throw new UnauthorizedException(ErrorCode.INVALID_PASSWORD);
        // Token 생성
        return createToken(findMember);
    }

    @Override
    public Member retrieve(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(()
                -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public String reissuingAccessToken(String refreshToken) {
        // Refresh Token 검증.
        try {
            JwtUtil.validateToken(refreshToken, secret);
        } catch (ExpiredTokenException e) {
            // RefreshToken 만료시 Data 삭제.
            throw new ExpiredRefreshTokenException();
        }
        // Access Token 재발급
        return JwtUtil.reissuingToken(getTokenInfo(refreshToken), accessTokenExpiration, secret);
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        tokenIsExpired(token);
        return JwtUtil.getTokenInfo(token, secret);
    }

    @Override
    public boolean tokenIsExpired(String token) {
        try {
            JwtUtil.validateToken(token, secret); // 토큰 검증
            return true;
        } catch (BusinessException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // 회원 아이디 유효성 검사.
    private void validateUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_USERNAME);
        }
    }

    private Token createToken(Member member) {
        return JwtUtil.createToken(member, accessTokenExpiration, refreshTokenExpiration, secret);
    }

}
