package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.guest.Guest;
import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.domain.token.RefreshToken;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenUserType;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.GuestRepository;
import com.moya.myblogboot.repository.TokenRepository;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AdminRepository adminRepository;
    private final GuestRepository guestRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    @Value("${jwt.secret}")
    private  String secret;

    @Value("${jwt.access-token-expiration}")
    private  Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private  Long refreshTokenExpiration;

    // 관리자 로그인
    public Token adminLogin(String admin_name, String admin_pw)  {
        Admin findAdmin = adminRepository.findById(admin_name).orElseThrow(() ->
                new NoResultException("아이디 또는 비밀번호를 확인하세요."));
        if (!passwordEncoder.matches(admin_pw, findAdmin.getAdmin_pw())) {
            throw new BadCredentialsException("아이디 또는 비밀번호를 확인하세요.");
        }
        Token newToken = createToken(findAdmin.getAdmin_name(), TokenUserType.ADMIN);
        // DB에 Refresh Token 저장
        saveRefreshToken(newToken.getRefresh_token(), findAdmin.getAdmin_name(), TokenUserType.ADMIN);
        return newToken;
    }

    // 게스트 로그인
    public Token guestLogin(GuestReqDto guestReqDto) {
        Guest findGuest = guestRepository.findByName(guestReqDto.getUsername()).orElseThrow(() ->
                new NoResultException("아이디 또는 비밀번호를 확인하세요."));
        if (!passwordEncoder.matches(guestReqDto.getPassword(), findGuest.getPassword())) {
            throw new BadCredentialsException("아이디 또는 비밀번호를 확인하세요.");
        }
        Token newToken = createToken(findGuest.getUsername(), TokenUserType.GUEST);
        // DB에 Refresh Token 저장
        saveRefreshToken(newToken.getRefresh_token(), findGuest.getUsername(), TokenUserType.GUEST);
        return newToken;
    }

    // Refresh Token 저장
    @Transactional
    public void saveRefreshToken(String token,String username, TokenUserType tokenUserType){
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .username(username)
                .tokenUserType(tokenUserType)
                .build();
        System.out.println(refreshToken.getTokenUserType());
        tokenRepository.save(refreshToken);
    }
    public String reissuingAccessToken(String refresh_token) {
            if(!tokenIsExpired(refresh_token)){
                // DB에서 Token 조회
                RefreshToken findRefreshToken = tokenRepository.findOne(refresh_token).orElseThrow(()
                        -> new NoResultException("토큰이 유효하지 않습니다.")
                );
                return JwtUtil.reissuingToken(findRefreshToken.getUsername(), findRefreshToken.getTokenUserType(), secret, accessTokenExpiration);
            }
        return null;

    }
    public boolean tokenIsExpired(String token) {
        return JwtUtil.isExpired(token, secret);
    }
    private Token createToken(String username, TokenUserType tokenUserType) {
        return JwtUtil.createToken(username, tokenUserType, secret, accessTokenExpiration, refreshTokenExpiration);
    }


}
