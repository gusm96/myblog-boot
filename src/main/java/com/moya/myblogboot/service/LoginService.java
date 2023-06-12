package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoginService {

    private final AdminRepository adminRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    // 토큰 만료 시간
    private Long expiredMs = 1000 * 60 * 60L;
    public String adminLogin(String admin_name, String admin_pw) throws UsernameNotFoundException {
        Admin admin = adminRepository.findById(admin_name).orElseThrow(() ->
                new UsernameNotFoundException("등록된 관리자 계정이 아닙니다.")
        );
        String token ="";
        if (admin_pw.equals(admin.getAdmin_pw())){
            // 비밀번호가 같다면 토큰을 받아온다.
            token = JwtUtil.createToken(admin.getAdmin_name(), secretKey, expiredMs);
        }
        return token;
    }
    @Transactional
    public void createAdmin() {
        Admin admin = Admin.builder()
                .admin_name("moya")
                .admin_pw("moya1343")
                .nickname("Moyada")
                .build();
        adminRepository.save(admin);
    }
}
