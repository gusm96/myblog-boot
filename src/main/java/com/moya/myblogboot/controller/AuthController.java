package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.member.MemberLoginReqDto;
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
    public ResponseEntity<String> login(@RequestBody @Valid MemberLoginReqDto memberLoginReqDto, HttpServletResponse response) {
        Token newToken = authService.memberLogin(memberLoginReqDto);

        // Http Only Cookie에 Refersh Token 저장.
        Cookie refreshTokenCookie = CookieUtil.addCookie("refresh_token", newToken.getRefresh_token());
        // Cookie Response
        response.addCookie(refreshTokenCookie);
        return ResponseEntity.ok().body(newToken.getAccess_token());
    }

    // 로그아웃
    @GetMapping("/api/v1/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Cookie 에서 Refresh Token 찾는다.
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token");
        if (refreshTokenCookie != null && refreshTokenCookie.getValue() != null && !refreshTokenCookie.getValue().equals(""))
            CookieUtil.deleteCookie(response, refreshTokenCookie); // Refresh Token 삭제 -> Refresh Token의 정보 소멸
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 토큰 권한 조회
    @GetMapping("/api/v1/token-role")
    public ResponseEntity<String> getRoleFromToken(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.getTokenInfo(getToken(request)).getRole());}


    // Access Token 재발급
    @GetMapping("/api/v1/reissuing-token")
    public ResponseEntity<String> reissuingAccessToken(HttpServletRequest request) {
        Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token");
        if (refreshTokenCookie == null || refreshTokenCookie.getValue().equals(""))
            throw new InvalidateTokenException("토큰이 존재하지 않습니다.");
        return ResponseEntity.ok().body(authService.reissuingAccessToken(refreshTokenCookie.getValue()));
    }

    // 토큰 검증
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> tokenValidate(HttpServletRequest request) {
        return ResponseEntity.ok().body(authService.tokenIsExpired(getToken(request)));
    }

    // 비밀번호 강도 확인
    @PostMapping("/api/v1/password-strength-check")
    public ResponseEntity<String> checkPasswordStrength(@RequestBody @Valid PwStrengthCheckReqDto pwStrengthCheckReqDto) {
        return ResponseEntity.ok().body(passwordStrengthCheck.strengthCheck(pwStrengthCheckReqDto.getPassword()).getLabel());
    }

    private static String getToken(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
    }
}