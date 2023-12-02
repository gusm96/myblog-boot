package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.member.PwStrengthCheckReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.exception.custom.InvalidateTokenException;
import com.moya.myblogboot.service.AuthService;
import com.moya.myblogboot.service.PasswordStrengthCheck;
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

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordStrengthCheck passwordStrengthCheck;

    // 회원 가입
    @PostMapping("/api/v1/join")
    public ResponseEntity<String> join(@RequestBody @Valid MemberJoinReqDto memberJoinReqDto) {
        return ResponseEntity.ok().body(authService.memberJoin(memberJoinReqDto));
    }

    // 로그인
    @PostMapping("/api/v1/login")
    public ResponseEntity<String> login(@RequestBody @Valid LoginReqDto loginReqDto, HttpServletResponse response) {
        Token newToken = authService.memberLogin(loginReqDto);
        Cookie refreshTokenCookie = CookieUtil.addCookie("refresh_token", newToken.getRefresh_token());
        response.addCookie(refreshTokenCookie);
        return ResponseEntity.ok().body(newToken.getAccess_token());
    }

    // 비밀번호 강도 확인
    @PostMapping("/api/v1/password-strength-check")
    public ResponseEntity<String> checkPasswordStrength(@RequestBody @Valid PwStrengthCheckReqDto pwStrengthCheckReqDto) {
        return ResponseEntity.ok().body(passwordStrengthCheck.strengthCheck(pwStrengthCheckReqDto.getPassword()).getLabel());
    }

    // 로그아웃
    @GetMapping("/api/v1/logout")
    public ResponseEntity<HttpStatus> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token");
        if (refreshTokenCookie.getValue() != "" || refreshTokenCookie.getValue() != null)
            CookieUtil.deleteCookie(response, refreshTokenCookie);
        return ResponseEntity.ok().body(HttpStatus.OK);
    }

    // 토큰 권한 조회
    @GetMapping("/api/v1/token-role")
    public ResponseEntity<String> getTokenFromRole(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.getTokenInfo(
                        request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1])
                .getRole());
    }

    // Access Token 재발급
    @GetMapping("/api/v1/reissuing-token")
    public ResponseEntity<String> reissuingAccessToken(HttpServletRequest request) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token");
        if (refreshTokenCookie == null)
            throw new InvalidateTokenException("토큰이 존재하지 않습니다.");
        return ResponseEntity.ok().body(authService.reissuingAccessToken(refreshTokenCookie.getValue()));
    }

    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> tokenValidate(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        return ResponseEntity.ok().body(authService.tokenIsExpired(token));
    }
}