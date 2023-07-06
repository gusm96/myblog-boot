package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.AdminReqDto;
import com.moya.myblogboot.service.LoginService;
import jakarta.persistence.NoResultException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    @GetMapping("/api/v1/token-validation")
    public ResponseEntity<Boolean> loginConfirmation( ) {
        boolean result = false;
        String authorizedAdminName = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        if(authorizedAdminName != null && !authorizedAdminName.isEmpty()){
            result = true;
        }
        return ResponseEntity.ok().body(result);
    }
    @PostMapping("/api/v1/login/admin")
    public ResponseEntity<String> adminLogin(@RequestBody @Valid AdminReqDto adminReqDto) {
        try {
            String token = loginService.adminLogin(adminReqDto.getUsername(), adminReqDto.getPassword());
            return ResponseEntity.ok().body(token);
        } catch (NoResultException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "아이디 또는 비밀번호를 확인하세요.");
        } catch (IllegalArgumentException e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "아이디 또는 비밀번호를 확인하세요.");
        }
    }

}
