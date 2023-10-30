package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.member.LoginReqDto;
import com.moya.myblogboot.domain.member.MemberJoinReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.domain.token.TokenUserType;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.service.AuthService;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원 가입
    @PostMapping("/api/v1/join")
    public ResponseEntity<?> join(@RequestBody @Valid MemberJoinReqDto memberJoinReqDto){
        authService.memberJoin(memberJoinReqDto);
        return ResponseEntity.ok("회원가입이 성공적으로 완료되었습니다.");
    }

    // 로그인
    @PostMapping("/api/v1/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginReqDto loginReqDto) {
        String accessToken = authService.memberLogin(loginReqDto.getUsername(), loginReqDto.getPassword());
        return ResponseEntity.ok().body(accessToken);
    }

    // 로그아웃
    @PostMapping("/api/v1/logout")
    public ResponseEntity<?> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getPrincipal().toString();
        return ResponseEntity.ok().body(authService.logout(username));
    }


    // 토큰 검증
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<?> accessTokenValidation (HttpServletRequest request) {
        // Http Header에서 Token 정보를 가져온다.
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        try {
            // Token 유효성 검사.
            return ResponseEntity.ok().body("으악");
        } catch (ExpiredTokenException | NoResultException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    // Access Token 재발급
    @PostMapping("/api/v1/reissuing-token")
    public ResponseEntity<?> reissuingAccessToken (@RequestBody @Valid String username){
        String newToken = authService.reissuingAccessToken(username);
        return ResponseEntity.ok().body(newToken);
    }
}
