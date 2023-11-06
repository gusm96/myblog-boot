package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.TokenReqDto;
import com.moya.myblogboot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원 가입
    @PostMapping("/api/v1/join")
    public ResponseEntity<?> join(@RequestBody @Valid MemberJoinReqDto memberJoinReqDto){
        return ResponseEntity.ok().body(authService.memberJoin(memberJoinReqDto));
    }

    // 로그인
    @PostMapping("/api/v1/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReqDto loginReqDto) {
        return ResponseEntity.ok().body(authService.memberLogin(loginReqDto));
    }

    // 로그아웃
    @GetMapping("/api/v1/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        return ResponseEntity.ok().body(authService.logout(accessToken));
    }

    // 토큰 검증
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<?> accessTokenValidation (HttpServletRequest request) {
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        return ResponseEntity.ok().body(authService.validateToken(accessToken));

    }
    // 토큰 권한 조회
    @GetMapping("/api/v1/token-role")
    public ResponseEntity<?> getTokenFromRole(HttpServletRequest request){
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        return ResponseEntity.ok().body(authService.validateTokenAndExtractRole(accessToken));
    }
    // Access Token 재발급
    @GetMapping("/api/v1/reissuing-token")
    public ResponseEntity<?> reissuingAccessToken (@CookieValue("refresh_token_key") String refreshTokenKey){
        System.out.println("test");
        System.out.println(refreshTokenKey);
        String newToken = authService.reissuingAccessToken(Long.parseLong(refreshTokenKey));
        return ResponseEntity.ok().body(newToken);
    }
}