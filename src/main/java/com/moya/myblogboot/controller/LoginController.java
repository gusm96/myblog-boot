package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.LoginReq;
import com.moya.myblogboot.domain.TokenResponse;
import com.moya.myblogboot.service.LoginService;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    @PostMapping("/api/v1/login/admin")
    public ResponseEntity<String> adminLogin(@RequestBody @Valid LoginReq loginReq) {
        // Token 발급
        String token = "";
       try {
           token = loginService.adminLogin(loginReq.getUsername(), loginReq.getPassword());
           return ResponseEntity.ok().body(token);
       }catch (IllegalArgumentException e){
           return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("아이디 또는 비밀번호가 올바르지 않습니다");
       }
    }
}
