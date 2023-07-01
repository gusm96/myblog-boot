package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.utils.JwtUtil;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

    private final AdminRepository adminRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    // 토큰 만료 시간
    private Long expiredMs = 1000 * 60 * 60L;
    public String adminLogin(String admin_name, String admin_pw)  {
        String token ="";
        Admin admin = adminRepository.findById(admin_name).orElseThrow(() ->
               new NoResultException("해당하는 관리자를 찾을 수 없습니다.")
        );
        if (admin_pw.equals(admin.getAdmin_pw())){
            // 추후 bcrypt를 사용하여 passwordEncoder.matches()로 수정
            // 비밀번호가 같다면 토큰을 받아온다.
            token = JwtUtil.createToken(admin.getAdmin_name(), secretKey, expiredMs);
        } // 비밀번호 틀리면 Exception 처리
        else{
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        return token;
    }

    public Boolean tokenIsExpired(String token) {
        return !JwtUtil.isExpired(token, secretKey);
    }

}
