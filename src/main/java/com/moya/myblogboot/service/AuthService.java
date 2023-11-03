package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.Role;
import com.moya.myblogboot.domain.token.RefreshToken;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.domain.token.TokenResDto;
import com.moya.myblogboot.exception.DuplicateUsernameException;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.exception.InvalidateTokenException;
import com.moya.myblogboot.repository.MemberRepository;
import com.moya.myblogboot.repository.RoleRepository;
import com.moya.myblogboot.repository.TokenRepository;
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
    public String memberJoin (MemberJoinReqDto memberJoinReqDto){
        // 아이디 중복 체크
        validateUsername(memberJoinReqDto.getUsername());
        // 비밀번호 암호화
        memberJoinReqDto.passwordEncode(passwordEncoder.encode(memberJoinReqDto.getPassword()));
        // Dto 객체 Entity화
        Member newMember = memberJoinReqDto.toEntity();
        // 일반호원 권한 등록
        addNormalRoleToMember(newMember);
        // 회원 Persist
        memberRepository.save(newMember);
        return "회원가입을 축하드립니다.";
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
    public TokenResDto memberLogin(LoginReqDto loginReqDto) {
        // username으로 회원 찾기
        Member findMember = memberRepository.findOne(loginReqDto.getUsername()).orElseThrow(()
                -> new UsernameNotFoundException("존재하지 않는 아이디 입니다."));
        // password 비교
        if (!passwordEncoder.matches(loginReqDto.getPassword(), findMember.getPassword()))
            throw new BadCredentialsException("비밀번호가 일치하지 않습니다..");

        String role = findMember.getRole().getRoleName();
        // Token 생성
        Token newToken = createToken(findMember.getUsername(), role);
        // DB에 RefreshToken 저장
        Long refreshTokenIdx = saveRefreshToken(newToken.getRefresh_token(), findMember.getUsername(), role);
        // Response용 Dto로 생성하여
        TokenResDto tokenResDto = TokenResDto.builder()
                .access_token(newToken.getAccess_token())
                .refresh_token_idx(refreshTokenIdx)
                .build();
        return tokenResDto;
    }

    // Refresh Token 저장
    @Transactional
    public Long saveRefreshToken(String token,String username, String roleName){
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .username(username)
                .tokenRole(roleName)
                .build();
        return tokenRepository.save(refreshToken);
    }
    public String reissuingAccessToken(Long refresh_token_idx) {
        // Refresh Token 찾는다.
        RefreshToken findRefreshToken = getFindRefreshTokenByIndex(refresh_token_idx);
        // Refresh Token 검증.
        tokenIsExpired(findRefreshToken.getToken());
        // Access Token 재발급
        return JwtUtil.reissuingToken(findRefreshToken.getUsername(), findRefreshToken.getTokenRole(), secret, accessTokenExpiration);
    }

    private RefreshToken getFindRefreshTokenByIndex(Long refresh_token_idx) {
        return tokenRepository.findRefreshTokenByIndex(refresh_token_idx).orElseThrow(()
                -> new InvalidateTokenException("존재하지 않는 토큰입니다."));
    }
    private RefreshToken getFindRefreshTokenByUsername(String username) {
        return tokenRepository.findRefreshTokenByUsername(username).orElseThrow(()
                -> new InvalidateTokenException("존재하지 않는 토큰입니다."));
    }

    private void  tokenIsExpired(String token) {
        if(JwtUtil.isExpired(token, secret))
            throw new ExpiredTokenException("만료된 토큰입니다.");
    }

    private Token createToken(String username, String roleName) {
        return JwtUtil.createToken(username, roleName, secret, accessTokenExpiration, refreshTokenExpiration);
    }

    @Transactional
    public String logout(String accessToken) {
        TokenInfo tokenInfo = getTokenInfo(accessToken);
        RefreshToken findRefreshToken = getFindRefreshTokenByUsername(tokenInfo.getUsername());
        tokenRepository.delete(findRefreshToken);
        return "로그아웃 완료";
    }

    private TokenInfo getTokenInfo(String accessToken) {
        return JwtUtil.getTokenInfo(accessToken, secret);
    }

    public String validateTokenAndExtractRole(String accessToken) {
        // 토큰 유효성 검증
        tokenIsExpired(accessToken);
        return getTokenInfo(accessToken).getRole();
    }

    public boolean validateToken(String token) {
        tokenIsExpired(token);
        return true;
    }
}
