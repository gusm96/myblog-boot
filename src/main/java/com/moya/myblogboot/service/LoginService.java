package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.utils.JwtUtil;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpSession;
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
    public String adminLogin(HttpSession session, Admin admin) {
        Optional<Admin> result = adminRepository.findById(admin.getId());

        if (admin.getPw().equals(result.get().getPw())){
            String token = JwtUtil.createToken(result.get().getName(), secretKey, expiredMs);
            System.out.println("token = " + token);
            session.setAttribute("admin", result.get().getIdx());
            return " ";
        }
        return "redirect:/login/admin";
    }
}
