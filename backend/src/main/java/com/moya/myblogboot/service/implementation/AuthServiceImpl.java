package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.EntityNotFoundException;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Override
    public Token adminLogin(LoginReqDto loginReqDto) {
        Admin admin = adminRepository.findByUsername(loginReqDto.getUsername())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.MEMBER_NOT_FOUND));
        if (!passwordEncoder.matches(loginReqDto.getPassword(), admin.getPassword()))
            throw new UnauthorizedException(ErrorCode.INVALID_PASSWORD);
        return JwtUtil.createToken(admin, accessTokenExpiration, refreshTokenExpiration, secret);
    }

    @Override
    public String reissuingAccessToken(String refreshToken) {
        try {
            JwtUtil.validateToken(refreshToken, secret);
        } catch (ExpiredTokenException e) {
            throw new ExpiredRefreshTokenException();
        }
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
            JwtUtil.validateToken(token, secret);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
