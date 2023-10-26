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
        Token token = authService.memberLogin(loginReqDto.getUsername(), loginReqDto.getPassword());
        return ResponseEntity.ok().body(token);
    }

    // 로그아웃
    @PostMapping("/api/v1/logout")
    public ResponseEntity<?> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getPrincipal().toString();
        String role =  authentication.getAuthorities().iterator().next().getAuthority();
        TokenUserType userType = role.equals("ADMIN") ? TokenUserType.ADMIN : TokenUserType.GUEST;
        try {
            String result = authService.logout(username, userType);
            return ResponseEntity.ok().body(result);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }


    // 토큰 검증
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<?> accessTokenValidation (HttpServletRequest request) {
        // Http Header에서 Token 정보를 가져온다.
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        try {
            // Token 유효성 검사.
            return ResponseEntity.ok().body(authService.tokenIsExpired(token));
        } catch (ExpiredTokenException | NoResultException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    // Access Token 재발급
    @PostMapping("/api/v1/reissuing-token")
    public ResponseEntity<?> reissuingAccessToken (@RequestBody @Valid String refresh_token){
        String token = refresh_token.split(" ")[1];
        try{
            String newAccessToken = authService.reissuingAccessToken(token);
            return ResponseEntity.status(HttpStatus.CREATED).body(newAccessToken);
        }catch (ExpiredTokenException | NoResultException e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }
}
