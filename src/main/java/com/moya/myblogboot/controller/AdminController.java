package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.admin.AdminReqDto;
import com.moya.myblogboot.domain.token.Token;
import com.moya.myblogboot.exception.ExpiredTokenException;
import com.moya.myblogboot.service.LoginService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;


@RestController
@RequiredArgsConstructor
public class AdminController {

    private final LoginService loginService;

    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> adminLoginConfirmation() {
        boolean result = false;
        try {
            String authorizedAdminName = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
            if (authorizedAdminName != null && !authorizedAdminName.isEmpty()) {
                result = true;
            }
            return ResponseEntity.ok().body(result);
        }catch (ExpiredTokenException e){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
        }
    }

    @PostMapping("/api/v1/login/admin")
    public ResponseEntity<Token> adminLogin(@RequestBody @Valid AdminReqDto adminReqDto) {

        try {
            Token token = loginService.adminLogin(adminReqDto.getUsername(), adminReqDto.getPassword());
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }


    }

    @GetMapping("/api/v1/logout/admin")
    public ResponseEntity<Boolean> amdinLogout(){
        boolean result = false;
        // 유효검사는 X Client에서 미리 할것.
        // 유효한다 가정하여 accessToken 강제 expire
        // 성공시 결과 반환
        return ResponseEntity.ok().body(result);
    }
}
