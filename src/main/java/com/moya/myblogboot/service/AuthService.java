package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.Role;
import com.moya.myblogboot.domain.token.RefreshToken;
import com.moya.myblogboot.domain.token.Token;
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
    public String memberLogin(String username, String password) {
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

        return newToken.getAccess_token();
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
    public String reissuingAccessToken(String username) {
        // Refresh Token 찾는다.
        RefreshToken findRefreshToken = getFindRefreshToken(username);
        // Refresh Token 검증.
        tokenIsExpired(findRefreshToken.getToken());
        // Access Token 재발급
        return JwtUtil.reissuingToken(username, findRefreshToken.getTokenRole(), secret, accessTokenExpiration);
    }

    private RefreshToken getFindRefreshToken(String username) {
        return tokenRepository.findOne(username).orElseThrow(()
                -> new InvalidateTokenException("존재하지 않는 토큰입니다."));
    }

    private boolean tokenIsExpired(String token) {
        return JwtUtil.isExpired(token, secret);
    }
    private Token createToken(String username, String roleName) {
        return JwtUtil.createToken(username, roleName, secret, accessTokenExpiration, refreshTokenExpiration);
    }

    @Transactional
    public String logout(String username) {
        RefreshToken findRefreshToken = getFindRefreshToken(username);
        if (!tokenIsExpired(findRefreshToken.getToken())) {
            // JWT token 만료 시키기
        }
        tokenRepository.delete(findRefreshToken);
        return "로그아웃 완료";
    }

    // 토큰 재발급
    public String getAccessTokenFromRefreshToken(){
        String newToken = "";
            // 1. Access Token이 만료되었을 경우 해당 코드를 실행한다. if accessTokenIsExpired -> ()
            // 2. Refresh Token을 DB에서 찾는다. username으로.
            // 3. 찾아온 Refresh Token을 검증한다.
            // 4. Refresh Token으로 Access Token 재발급.
            // 5. Refresh Token이 만료되었으면, 강제 로그아웃 반환.
        return newToken;
    }
}
