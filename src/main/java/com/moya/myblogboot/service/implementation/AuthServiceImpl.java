package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.member.MemberLoginReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.*;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${jwt.secret}")
    private  String secret;

    @Value("${jwt.access-token-expiration}")
    private  Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private  Long refreshTokenExpiration;
    // 회원 가입
    @Override
    @Transactional
    public String memberJoin(MemberJoinReqDto memberJoinReqDto) {
        // 아이디 중복 체크
        validateUsername(memberJoinReqDto.getUsername());
        // Member Entity 생성
        Member newMember = memberJoinReqDto.toEntity(passwordEncoder);
        // Member Persist
        try {
            memberRepository.save(newMember);
            return "회원가입을 성공했습니다.";
        } catch (Exception e) {
            throw new RuntimeException("회원가입 중 오류가 발생했습니다.");
        }
    }

    @Override
    public Token memberLogin(MemberLoginReqDto memberLoginReqDto) {
        // username 으로 회원 찾기
        Member findMember = memberRepository.findByUsername(memberLoginReqDto.getUsername()).orElseThrow(()
                -> new UsernameNotFoundException("존재하지 않는 아이디 입니다."));
        // password 비교
        if (!passwordEncoder.matches(memberLoginReqDto.getPassword(), findMember.getPassword()))
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다.");
        // Token 생성
        return createToken(findMember);
    }

    @Override
    public Member retrieveMemberById(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(()
                -> new EntityNotFoundException("회원이 존재하지 않습니다."));
    }
    @Override
    public String reissuingAccessToken(String refreshToken) {
        // Refresh Token 검증.
        try {
            JwtUtil.validateToken(refreshToken, secret);
        } catch (ExpiredTokenException e) {
            // RefreshToken 만료시 Data 삭제.
            throw new ExpiredRefreshTokenException("토큰이 만료되었습니다.");
        }
        // Access Token 재발급
        return JwtUtil.reissuingToken(getTokenInfo(refreshToken), accessTokenExpiration, secret);
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        return JwtUtil.getTokenInfo(token, secret);
    }

    @Override
    public boolean tokenIsExpired(String token) {
        try {
            JwtUtil.validateToken(token, secret); // 토큰 검증
            return true;
        } catch (ExpiredTokenException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // 회원 아이디 유효성 검사.
    private void validateUsername(String username) {
        if (memberRepository.existsByUsername(username)) {
            throw new DuplicateKeyException("이미 존재하는 회원입니다.");
        }
    }
    private Token createToken(Member member) {
        return JwtUtil.createToken(member, accessTokenExpiration, refreshTokenExpiration, secret);
    }
}
