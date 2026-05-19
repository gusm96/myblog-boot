package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.login.LoginAttemptResult;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.exception.custom.TooManyLoginAttemptsException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.LoginAttemptService;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final AuthCredentialVerifier authCredentialVerifier;

    @Value("${jwt.secret}")
    private String secret;

    @Override
    public Token adminLogin(LoginReqDto loginReqDto, String clientIp) {
        loginAttemptService.assertNotLocked(loginReqDto.getUsername(), clientIp);
        Admin admin;
        try {
            admin = authCredentialVerifier.verify(loginReqDto);
        } catch (UnauthorizedException e) {
            LoginAttemptResult result = loginAttemptService.onFailure(loginReqDto.getUsername(), clientIp);
            if (result.locked()) {
                throw new TooManyLoginAttemptsException(result.retryAfterSeconds());
            }
            loginAttemptService.applyProgressiveDelay(result.count());
            throw e;
        }
        loginAttemptService.onSuccess(loginReqDto.getUsername(), clientIp);
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
