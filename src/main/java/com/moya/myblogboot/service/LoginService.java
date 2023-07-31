package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.guest.Guest;
import com.moya.myblogboot.domain.guest.GuestReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenUserType;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.GuestRepository;
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
public class LoginService {

    private final AdminRepository adminRepository;
    private final GuestRepository guestRepository;
    private final  PasswordEncoder passwordEncoder;

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
        return createToken(findAdmin.getAdmin_name(), TokenUserType.ADMIN);
    }

    // 게스트 로그인
    public Token guestLogin(GuestReqDto guestReqDto) {
        Guest findGuest = guestRepository.findByName(guestReqDto.getUsername()).orElseThrow(() ->
                new NoResultException("아이디 또는 비밀번호를 확인하세요."));
        if (!passwordEncoder.matches(guestReqDto.getPassword(), findGuest.getPassword())) {
            throw new BadCredentialsException("아이디 또는 비밀번호를 확인하세요.");
        }
        return createToken(findGuest.getUsername(), TokenUserType.GUEST);
    }
    private Token createToken(String username, TokenUserType tokenUserType) {
        return JwtUtil.createToken(username, tokenUserType, secret, accessTokenExpiration, refreshTokenExpiration);
    }
}
