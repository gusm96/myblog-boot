package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

    private final AdminRepository adminRepository;
    private final  PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String secretKey;

    // 토큰 만료 시간
    @Value("${jwt.expiration}")
    private Long expiredMs;

    public String adminLogin(String admin_name, String admin_pw)  {
        Admin admin = adminRepository.findById(admin_name).orElseThrow(() ->
                new NoResultException("아이디 또는 비밀번호를 확인하세요")
        );
        if (!passwordEncoder.matches(admin_pw, admin.getAdmin_pw())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호를 확인하세요.");
        }
        return JwtUtil.createToken(admin.getAdmin_name(), secretKey, expiredMs);
    }

    public Boolean tokenIsExpired(String token) {
        return !JwtUtil.isExpired(token, secretKey);
    }
}
