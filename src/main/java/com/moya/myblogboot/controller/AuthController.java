package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.TokenResDto;
import com.moya.myblogboot.exception.InvalidateTokenException;
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
    public ResponseEntity<?> login(@RequestBody @Valid LoginReqDto loginReqDto, HttpServletResponse response) {
        TokenResDto tokenResDto = authService.memberLogin(loginReqDto);
        Cookie refreshTokenKeyCookie = CookieUtil.addCookie("refresh_token_key", tokenResDto.getRefresh_token_key().toString());
        response.addCookie(refreshTokenKeyCookie);
        return ResponseEntity.ok().body(tokenResDto.getAccess_token());
    }

    // 로그아웃
    @GetMapping("/api/v1/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
        if(refreshTokenCookie == null)
            throw new InvalidateTokenException("토큰이 존재하지 않습니다.");
        authService.logout(Long.parseLong(refreshTokenCookie.getValue()));
        CookieUtil.deleteCookie(response, refreshTokenCookie);
        return ResponseEntity.ok().body(HttpStatus.OK);
    }

    // 토큰 권한 조회
    @GetMapping("/api/v1/token-role")
    public ResponseEntity<?> getTokenFromRole(HttpServletRequest request){
        return ResponseEntity.ok().body(authService.getTokenInfo(
                request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1])
                .getRole());
    }

    // Access Token 재발급
    @GetMapping("/api/v1/reissuing-token")
    public ResponseEntity<?> reissuingAccessToken (HttpServletRequest request){
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
        if(refreshTokenCookie == null)
            throw new InvalidateTokenException("토큰이 존재하지 않습니다.");
        return ResponseEntity.ok().body(authService.reissuingAccessToken(Long.parseLong(refreshTokenCookie.getValue())));
    }
}