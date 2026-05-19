package com.moya.myblogboot.controller;

import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.TokenMetaResponse;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.exception.custom.ExpiredTokenException;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.ClientIpResolver;
import com.moya.myblogboot.utils.CookieFactory;
import com.moya.myblogboot.utils.CookieUtil;
import com.moya.myblogboot.utils.TokenResolver;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.moya.myblogboot.constants.CookieName.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final ClientIpResolver clientIpResolver;
    private final CookieFactory cookieFactory;
    private final TokenResolver tokenResolver;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @PostMapping("/api/v1/login")
    public ResponseEntity<TokenMetaResponse> login(@RequestBody @Valid LoginReqDto loginReqDto,
                                                   HttpServletRequest request,
                                                   HttpServletResponse response) {
        Token newToken = authService.adminLogin(loginReqDto, clientIpResolver.resolve(request));
        addAuthCookies(response, newToken.getAccess_token(), newToken.getRefresh_token());
        return ResponseEntity.ok().body(tokenMetaResponse());
    }

    @PostMapping("/api/v1/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        jakarta.servlet.http.Cookie refreshTokenCookie = CookieUtil.findCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie != null
                && refreshTokenCookie.getValue() != null
                && !refreshTokenCookie.getValue().isEmpty()) {
            refreshTokenService.revokeOnLogout(refreshTokenCookie.getValue());
        }
        cookieFactory.expireAuthCookies(response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/api/v1/token-role")
    public ResponseEntity<String> getRoleFromToken(HttpServletRequest request) {
        String token = tokenResolver.resolve(request);
        if (token == null) {
            throw new InvalidateTokenException();
        }
        try {
            return ResponseEntity.ok().body(authService.getTokenInfo(token).getRole());
        } catch (ExpiredTokenException | InvalidateTokenException | JwtException | IllegalArgumentException e) {
            throw new InvalidateTokenException();
        }
    }

    @PostMapping("/api/v1/reissuing-token")
    public ResponseEntity<TokenMetaResponse> reissuingAccessToken(HttpServletRequest request, HttpServletResponse response) {
        jakarta.servlet.http.Cookie refreshTokenCookie = CookieUtil.findCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie == null || refreshTokenCookie.getValue().isEmpty()) {
            cookieFactory.expireAuthCookies(response);
            throw new InvalidateTokenException();
        }
        try {
            ReissuedToken reissuedToken = authService.reissuingAccessToken(refreshTokenCookie.getValue());
            addAuthCookies(response, reissuedToken.accessToken(), reissuedToken.refreshToken());
            return ResponseEntity.ok().body(tokenMetaResponse());
        } catch (InvalidateTokenException | ExpiredRefreshTokenException e) {
            cookieFactory.expireAuthCookies(response);
            throw e;
        }
    }

    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> tokenValidate(HttpServletRequest request) {
        String token = tokenResolver.resolve(request);
        return ResponseEntity.ok().body(token != null && authService.isTokenValid(token));
    }

    private void addAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addCookie(cookieFactory.accessTokenCookie(accessToken, Math.toIntExact(accessTokenExpiration / 1000)));
        response.addCookie(cookieFactory.refreshTokenCookie(refreshToken, Math.toIntExact(refreshTokenExpiration / 1000)));
    }

    private TokenMetaResponse tokenMetaResponse() {
        return new TokenMetaResponse("Bearer", accessTokenExpiration / 1000);
    }
}
