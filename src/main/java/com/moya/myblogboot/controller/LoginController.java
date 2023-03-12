package com.moya.myblogboot.controller;

import com.moya.myblogboot.domain.AdminDTO;
import com.moya.myblogboot.service.LoginService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    LoginService service;
    @GetMapping("/admin")
    public String getAdminLoginPage() {
        return "admin/loginForm";
    }

    @PostMapping("/admin")
    public String postAdminLoginPage(HttpSession session, AdminDTO admin) {
        return service.adminLogin(session, admin);
    }
}
