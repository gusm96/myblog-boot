package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.handler.AdminLoginFailHandler;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AdminRepository adminRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    private Long expiredMs = 1000 * 60 * 60L;

    @Transactional
    public String adminLogin(Admin admin) {
        Optional<Admin> result = adminRepository.findById(admin.getId());
        String token ="";
        if (admin.getPw().equals(result.get().getPw())){
            // 비밀번호가 같다면 토큰을 받아온다.
            token = JwtUtil.createToken(result.get().getName(), secretKey, expiredMs);
        }
        return token;
    }
}
