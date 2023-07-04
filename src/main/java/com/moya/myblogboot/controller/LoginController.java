package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.LoginReq;
import com.moya.myblogboot.service.LoginService;
import jakarta.persistence.NoResultException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> loginConfirmation(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorization.split(" ")[1];
        return ResponseEntity.ok().body(loginService.tokenIsExpired(token));
    }
    @PostMapping("/api/v1/login/admin")
    public ResponseEntity<String> adminLogin(@RequestBody @Valid LoginReq loginReq) {
        try {
            String token = loginService.adminLogin(loginReq.getUsername(), loginReq.getPassword());
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "아이디 또는 비밀번호를 확인하세요.");
        } catch (IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 확인하세요.");
        }
    }

}
