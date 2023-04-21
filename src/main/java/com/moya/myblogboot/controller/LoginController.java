package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.LoginReq;
import com.moya.myblogboot.domain.TokenResponse;
import com.moya.myblogboot.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login/admin")
    public ResponseEntity<TokenResponse> adminLogin (@RequestBody LoginReq loginReq){
        // Token 발급
        String token = loginService.adminLogin(loginReq.getAdmin_id(), loginReq.getAdmin_pw());
        return ResponseEntity.ok().body(new TokenResponse(token, "bearer"));
    }
    // @GetMapping("/api/v1/logout")
    // Token 만료 시키기 Front 에선 localStorage 에서 Token 삭제

}
