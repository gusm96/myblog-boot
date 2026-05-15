package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.ErrorCode;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
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
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Token adminLogin(LoginReqDto loginReqDto) {
        Admin admin = adminRepository.findByUsername(loginReqDto.getUsername())
                .orElseThrow(() -> {
                    log.warn("Admin login failed: username not found");
                    return new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
                });
        if (!passwordEncoder.matches(loginReqDto.getPassword(), admin.getPassword())) {
            log.warn("Admin login failed: invalid password");
            throw new UnauthorizedException(ErrorCode.INVALID_CREDENTIALS);
        }
        IssuedToken issuedToken = refreshTokenService.issueOnLogin(admin);
        return Token.builder()
                .access_token(issuedToken.accessToken())
                .refresh_token(issuedToken.refreshToken())
                .build();
    }

    @Override
    public ReissuedToken reissuingAccessToken(String refreshToken) {
        return refreshTokenService.rotate(refreshToken);
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        AccessTokenClaims claims = JwtUtil.parseAccessToken(token, secret);
        return TokenInfo.builder()
                .memberPrimaryKey(claims.memberPrimaryKey())
                .role(claims.role())
                .build();
    }

    @Override
    public boolean isTokenValid(String token) {
        try {
            JwtUtil.validateAccessToken(token, secret);
            return true;
        } catch (ExpiredTokenException | InvalidateTokenException | JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
