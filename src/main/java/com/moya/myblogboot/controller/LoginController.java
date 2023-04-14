package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.Admin;
import com.moya.myblogboot.service.LoginService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService service;
    @GetMapping("/login/admin")
    public String getAdminLoginPage() {
        return "admin/loginForm";
    }

    @PostMapping("/login/admin")
    public String adminLogin(HttpSession session, Admin admin) {

        return service.adminLogin(session, admin);
    }

    @GetMapping("/logout/admin")
    public String adminLogout(HttpSession session){
        session.invalidate();
        return "redirect:/";
    }
}
