package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.admin.AdminReqDto;
import com.moya.myblogboot.domain.token.TokenUserType;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.service.LoginService;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;



@RestController
@RequiredArgsConstructor
public class AdminController {

    private final LoginService loginService;

    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<String> adminLoginConfirmation(HttpServletRequest request) {
        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
        try {
            if (loginService.accessTokenIsExpired(accessToken)) {
                String authenticatedUsername = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
                String newAccessToken = loginService.reissuingAccessToken(authenticatedUsername, TokenUserType.ADMIN);
                return ResponseEntity.ok().body(newAccessToken);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 만료되었습니다.");
            }
        } catch (ExpiredTokenException | NoResultException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/api/v1/login/admin")
    public ResponseEntity<String> adminLogin(@RequestBody @Valid AdminReqDto adminReqDto) {
        try {
            String token = loginService.adminLogin(adminReqDto.getUsername(), adminReqDto.getPassword());
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/api/v1/logout/admin")
    public ResponseEntity<Boolean> amdinLogout() {
        boolean result = false;
        // 유효검사는 X Client에서 미리 할것.
        // 유효한다 가정하여 accessToken 강제 expire
        // 성공시 결과 반환
        return ResponseEntity.ok().body(result);
    }
}
