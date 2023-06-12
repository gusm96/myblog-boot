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
    @GetMapping("/create/admin")
    public ResponseEntity<String> createAdmin(){
        loginService.createAdmin();
        return ResponseEntity.ok().body("어드민 계정 생성 완료.");
    }
    @PostMapping("/login/admin")
    public ResponseEntity<TokenResponse> adminLogin(@RequestBody LoginReq loginReq) {
        // Token 발급
        String token = loginService.adminLogin(loginReq.getAdmin_name(), loginReq.getAdmin_pw());
        return ResponseEntity.ok().body(new TokenResponse(token, "bearer"));
    }
}
