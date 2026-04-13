package com.moya.myblogboot.controller;

import com.moya.myblogboot.dto.auth.LoginReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.utils.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.moya.myblogboot.constants.CookieName.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/v1/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginReqDto loginReqDto,
                                        HttpServletResponse response) {
        Token newToken = authService.adminLogin(loginReqDto);
        response.addCookie(CookieUtil.addCookie(REFRESH_TOKEN_COOKIE, newToken.getRefresh_token()));
        return ResponseEntity.ok().body(newToken.getAccess_token());
    }

    @GetMapping("/api/v1/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie != null
                && refreshTokenCookie.getValue() != null
                && !refreshTokenCookie.getValue().isEmpty()) {
            CookieUtil.deleteCookie(response, refreshTokenCookie);
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/api/v1/token-role")
    public ResponseEntity<String> getRoleFromToken(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.getTokenInfo(getToken(request)).getRole());
    }

    @GetMapping("/api/v1/reissuing-token")
    public ResponseEntity<String> reissuingAccessToken(HttpServletRequest request) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, REFRESH_TOKEN_COOKIE);
        if (refreshTokenCookie == null || refreshTokenCookie.getValue().isEmpty())
            throw new InvalidateTokenException();
        return ResponseEntity.ok().body(authService.reissuingAccessToken(refreshTokenCookie.getValue()));
    }

    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> tokenValidate(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.tokenIsExpired(getToken(request)));
    }

    private static String getToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.toLowerCase().startsWith("bearer "))
            throw new InvalidateTokenException();
        return authorization.substring(7);
    }
}
