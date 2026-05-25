package com.moya.myblogboot.service.implementation;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.token.AccessTokenClaims;
import com.moya.myblogboot.domain.token.IssuedToken;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.login.LoginAttemptResult;
import com.moya.myblogboot.domain.token.TokenInfo;
import com.moya.myblogboot.exception.custom.TooManyLoginAttemptsException;
import com.moya.myblogboot.exception.custom.UnauthorizedException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.LoginAttemptService;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final AuthCredentialVerifier authCredentialVerifier;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public IssuedToken adminLogin(LoginReqDto loginReqDto, String clientIp) {
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
        return refreshTokenService.issueOnLogin(admin);
    }

    @Override
    public ReissuedToken reissuingAccessToken(String refreshToken) {
        return refreshTokenService.rotate(refreshToken);
    }

    @Override
    public TokenInfo getTokenInfo(String token) {
        AccessTokenClaims claims = jwtTokenProvider.parseAccessToken(token);
        return TokenInfo.builder()
                .memberPrimaryKey(claims.memberPrimaryKey())
                .role(claims.role())
                .build();
    }
}
