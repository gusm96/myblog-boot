package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.domain.TokenResponse;
import com.moya.myblogboot.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    @PostMapping("/avi/v1/login/admin")
    public ResponseEntity<TokenResponse> adminLogin (@RequestBody() Admin admin){
        // Token 발급
        String token = loginService.adminLogin(admin);
        return ResponseEntity.ok().body(new TokenResponse(token, "bearer"));
    }
}
