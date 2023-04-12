package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.repository.AdminRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AdminRepository adminRepository;

    @Transactional
    public String adminLogin(HttpSession session, Admin admin) {
        Optional<Admin> result = adminRepository.findById(admin.getId());

        if (admin.getPw().equals(result.get().getPw())){
            session.setAttribute("admin", result.get().getName());
            return " ";
        }
        return "redirect:/login/admin";
    }
}
