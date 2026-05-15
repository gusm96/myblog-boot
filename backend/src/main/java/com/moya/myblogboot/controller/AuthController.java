package com.moya.myblogboot.controller;

import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.ReissuedToken;
import com.moya.myblogboot.exception.custom.ExpiredRefreshTokenException;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.RefreshTokenService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.moya.myblogboot.constants.CookieName.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @PostMapping("/api/v1/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginReqDto loginReqDto,
                                        HttpServletResponse response) {
        Token newToken = authService.adminLogin(loginReqDto);
        addRefreshTokenCookie(response, newToken.getRefresh_token());
        return ResponseEntity.ok().body(newToken.getAccess_token());
    }

    @RequestMapping(value = "/api/v1/logout", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie != null
                && refreshTokenCookie.getValue() != null
                && !refreshTokenCookie.getValue().isEmpty()) {
            refreshTokenService.revokeOnLogout(refreshTokenCookie.getValue());
            CookieUtil.deleteCookie(response, refreshTokenCookie);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/api/v1/token-role")
    public ResponseEntity<String> getRoleFromToken(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.getTokenInfo(getToken(request)).getRole());
    }

    @RequestMapping(value = "/api/v1/reissuing-token", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<String> reissuingAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie == null || refreshTokenCookie.getValue().isEmpty())
            throw new InvalidateTokenException();
        try {
            ReissuedToken reissuedToken = authService.reissuingAccessToken(refreshTokenCookie.getValue());
            addRefreshTokenCookie(response, reissuedToken.refreshToken());
            return ResponseEntity.ok().body(reissuedToken.accessToken());
        } catch (InvalidateTokenException | ExpiredRefreshTokenException e) {
            CookieUtil.deleteCookie(response, refreshTokenCookie);
            throw e;
        }
    }

    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> tokenValidate(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.isTokenValid(getToken(request)));
    }

    private static String getToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.toLowerCase().startsWith("bearer "))
            throw new InvalidateTokenException();
        return authorization.substring(7);
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addCookie(CookieUtil.addCookie(REFRESH_TOKEN_COOKIE, refreshToken,
                Math.toIntExact(refreshTokenExpiration / 1000)));
    }
}
