package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.Role;
import com.moya.myblogboot.domain.token.RefreshToken;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenUserType;
import com.moya.myblogboot.exception.DuplicateUsernameException;
import com.moya.myblogboot.exception.InvalidateTokenException;
import com.moya.myblogboot.repository.*;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${jwt.secret}")
    private  String secret;

    @Value("${jwt.access-token-expiration}")
    private  Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private  Long refreshTokenExpiration;

    // 회원 가입
    @Transactional
    public void memberJoin (MemberJoinReqDto memberJoinReqDto){
        // 아이디 중복 체크
        validateUsername(memberJoinReqDto.getUsername());
        // 비밀번호 암호화
        memberJoinReqDto.passwordEncode(passwordEncoder.encode(memberJoinReqDto.getPassword()));

        Member newMember = memberJoinReqDto.toEntity();
        // 일반호원 권한 등록
        addNormalRoleToMember(newMember);
        // 회원 Persist
        memberRepository.save(newMember);
    }
    // 일반회원 권한 등록
    private void addNormalRoleToMember(Member newMember) {
        Role normalRole = roleRepository.findOne("NORMAL").orElseThrow(() ->
                new NoResultException("존재하지 않는 권한입니다."));
        newMember.addRole(normalRole);
    }

    // 회원 아이디 유효성 검사.
    private void validateUsername(String username) {
        Optional<Member> findMember = memberRepository.findOne(username);
        if (findMember.isPresent()){
            // 아이디 중복
            throw new DuplicateUsernameException("이미 존재하는 회원입니다.");
        }
    }

    // 회원 로그인
    public Token memberLogin(String username, String password) {
        // username으로 회원 찾기
        Member findMember = memberRepository.findOne(username).orElseThrow(()
                -> new UsernameNotFoundException("존재하지 않는 아이디 입니다."));
        // password 비교
        if (!passwordEncoder.matches(password, findMember.getPassword())) {
            // 일치하지 않음
            throw new BadCredentialsException("비밀번호를 확인하세요.");
        }
        Role role = findMember.getRole();
        // Token 생성
        Token newToken = createToken(findMember.getUsername(), role.getRoleName());
        // DB에 RefreshToken 저장
        saveRefreshToken(newToken.getRefresh_token(), findMember.getUsername(), role.getRoleName());
        return newToken;
    }

    //
    // Refresh Token 저장
    @Transactional
    public void saveRefreshToken(String token,String username, String roleName){
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .username(username)
                .tokenRole(roleName)
                .build();
        tokenRepository.save(refreshToken);
    }
    public String reissuingAccessToken(String refresh_token) {
            if(!tokenIsExpired(refresh_token)){
                // DB에서 Token 조회
                RefreshToken findRefreshToken = tokenRepository.findOne(refresh_token).orElseThrow(()
                        -> new InvalidateTokenException("토큰이 유효하지 않습니다.")
                );
                return JwtUtil.reissuingToken(findRefreshToken.getUsername(), findRefreshToken.getTokenRole(), secret, accessTokenExpiration);
            }
        return null;

    }
    public boolean tokenIsExpired(String token) {
        return JwtUtil.isExpired(token, secret);
    }
    private Token createToken(String username, String roleName) {
        return JwtUtil.createToken(username, roleName, secret, accessTokenExpiration, refreshTokenExpiration);
    }

    @Transactional
    public String logout(String usernmae, TokenUserType userType) {
        RefreshToken findToken = tokenRepository.findByNmaeAndUserType(usernmae, userType).orElseThrow(() ->
                new InvalidateTokenException("토큰이 유효하지 않습니다."));
        tokenRepository.delete(findToken);
        return "로그아웃 되었습니다.";
    }
}
